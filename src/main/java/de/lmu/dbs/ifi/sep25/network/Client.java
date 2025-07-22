package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.ui.ControllerRegistry;
import de.lmu.dbs.ifi.sep25.ui.GameController;
import de.lmu.dbs.ifi.sep25.ui.LobbyController;
import de.lmu.dbs.ifi.sep25.ui.LoginController;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;
import de.lmu.dbs.ifi.sep25.utils.FieldDeserializer;
import de.lmu.dbs.ifi.sep25.utils.FieldSerializer;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.ThreadContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Client {
    // 0. Logging
    private static final Logger clientLogger = LogManager.getLogger("PerClientLogger");
    private static final Logger appLogger = LogManager.getLogger(Client.class);
    private static final Logger errorLogger = LogManager.getLogger("ErrorLogger");
    private static final Logger heartbeatLogger = LogManager.getLogger("heartbeatLogger");

    // 1. Constants / configuration
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldDeserializer())
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldSerializer())
            .create();

    private final Gson gsonPretty = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final String protocol = "Version 1.0";
    private final ConcurrentBidirectionalMap<Integer, String> usernames = new ConcurrentBidirectionalMap<>();

    // 2. Main identity/data
    private Integer ID;
    private final boolean isAI = false;
    private volatile boolean firstReadyRegistry = true;

    // 3. Networking / I/O
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // 4. Game state
    private final List<String> hand = new ArrayList<>();
    private final List<BodyPlayerAdded> pendingPlayers = new ArrayList<>();
    private int currentRegister = 0;
    private BodyMovement rebootPosition;
    private int rebootingInProgress = -1;
    private final Map<Integer, Integer> energy = new HashMap<>();
    private final Map<Integer, Integer> checkpointsReached = new HashMap<>();

    private List<List<List<MessageDefinitions.Field>>> currentGameMap;

    private String selectedMap;

    public void setCurrentGameMap(List<List<List<MessageDefinitions.Field>>> map) {
        this.currentGameMap = map;
    }

    public List<List<List<MessageDefinitions.Field>>> getCurrentGameMap() {
        return currentGameMap;
    }

    /**
     * Establishes a connection to a server and initializes the necessary input and output streams
     * for communication. The method also starts a thread that listens for messages from the server.
     *
     * @param host the server hostname or IP address to connect to
     * @param port the port number on the server to connect to
     */
    public void start(String host, int port) throws IOException {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("[SERVER] Connected to server.");

            // Start listening thread
            new Thread(this::listenForMessages).start();

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            closeAll();
            throw e;
        }
    }

    /**
     * Continuously listens for and processes incoming messages from the server.
     * <p>
     * This method operates in a loop, reading messages from the server using the `reader` input stream.
     * Each message is expected to be in JSON format, containing a `messageType` field that indicates the
     * type of message being sent. Based on the `messageType`, a corresponding handler method is invoked
     * to process the message. If an unsupported or unknown message type is encountered, an
     * {@link IllegalArgumentException} is thrown.
     * <p>
     * The processing includes handling specific types of server messages such as "HelloClient", "Alive",
     * "Welcome", and "ReceivedChat". Additional message types can be enabled by uncommenting the relevant
     * cases in the switch block. The method gracefully handles disconnections or errors by catching
     * {@link IOException}, printing a disconnection message, and calling the {@code closeAll()} method
     * to clean up resources.
     * <p>
     * This method is designed to operate in its own thread, ensuring the client can continuously listen
     * for server messages while performing other tasks.
     * <p>
     * If an error occurs while reading from the input stream, or if the connection to the server is lost,
     * the method exits the listening loop and releases all allocated resources.
     */
    private void listenForMessages() {
        try {
            String json;
            while ((json = reader.readLine()) != null) {
                try {
                    String messageType = JsonUtil.parseUnknown(json).messageType();
                    if (ID != null && !messageType.equalsIgnoreCase("Alive")) {
                        ThreadContext.put("clientId", ID.toString());
                        clientLogger.info("[RECEIVED] {}: {}", messageType, gsonPretty.toJson(JsonUtil.parseUnknown(json)));
                        ThreadContext.clearAll();
                    }
                    switch (messageType) {
                        case "HelloClient" -> handleBodyHelloClient(json);
                        case "Alive" -> handleBodyAlive(json);
                        case "Welcome" -> handleBodyWelcome(json);
                        case "PlayerAdded" -> handleBodyPlayerAdded(json);
                        case "PlayerRenamed" -> handleBodyPlayerRenamed(json);//@SEBAS
                        case "PlayerStatus" -> handleBodyPlayerStatus(json);
                        case "SelectMap" -> handleBodySelectMap(json);
                        case "MapSelected" -> handleBodyMapSelected(json);
                        case "GameStarted" -> handleBodyGameStarted(json);
                        case "ReceivedChat" -> handleBodyReceivedChat(json);
                        case "Error" -> handleBodyError(json);
                        case "CardPlayed" -> handleBodyCardPlayed(json);
                        case "CurrentPlayer" -> handleBodyCurrentPlayer(json);
                        case "ActivePhase" -> handleBodyActivePhase(json);
                        case "StartingPointTaken" -> handleBodyStartingPointTaken(json);
                        case "YourCards" -> handleBodyYourCards(json);
                        case "NotYourCards" -> handleBodyNotYourCards(json);
                        case "ShuffleCoding" -> handleBodyShuffleCoding(json);
                        case "CardSelected" -> handleBodyCardSelected(json);
                        case "SelectionFinished" -> handleBodySelectionFinished(json);
                        case "TimerStarted" -> handleBodyTimerStarted();
                        case "TimerEnded" -> handleBodyTimerEnded(json);
                        case "CardsYouGotNow" -> handleBodyCardsYouGotNow(json);
                        case "CurrentCards" -> handleBodyCurrentCards(json);
                        case "ReplaceCard" -> handleBodyReplaceCard(json);
                        case "Movement" -> handleBodyMovement(json);
                        case "PlayerTurning" -> handleBodyPlayerTurning(json);
                        case "DrawDamage" -> handleBodyDrawDamage(json);
                        case "PickDamage" -> handleBodyPickDamage(json);
                        case "Animation" -> handleBodyAnimation(json);
                        case "Reboot" -> handleBodyReboot(json);
                        case "RebootDirection" -> handleBodyRebootDirection(json);
                        case "Energy" -> handleBodyEnergy(json);
                        case "CheckPointReached" -> handleBodyCheckPointReached(json);
                        case "GameFinished" -> handleBodyGameFinished(json);
                        default -> throw new IllegalArgumentException("Unknown messageType: " + messageType);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Error handling message: " + e.getMessage());
                    System.err.println("[ERROR] Message: " + json);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("Disconnected from server.");
            System.err.println("Message: " + e.getMessage());
            e.printStackTrace();
            closeAll();
        }
    }

    /**
     * Handles the BodyHelloClient message received from the server.
     * This method processes the JSON message, extracts connection protocol information,
     * and sends a response back to the server using a BodyHelloServer message.
     *
     * @param json the JSON string containing the serialized BodyHelloClient message
     */
    private void handleBodyHelloClient(String json) {
        Message<BodyHelloClient> message = JsonUtil.parseMessage(json, BodyHelloClient.class);
        String protocol = message.messageBody().protocol();
        appLogger.info("Connected using protocol: {} (Received HelloClient)", protocol);

        sendMessage(new Message<>(new BodyHelloServer("Edle Eisbecher", isAI, this.protocol)));
    }

    /**
     * Handles the "BodyAlive" message received from the server.
     * This method writes the provided JSON string directly to the server output stream.
     *
     * @param json the JSON string containing the serialized BodyAlive message
     */
    private void handleBodyAlive(String json) {
        heartbeatLogger.info("Alive received from server: {}, {}", ID, JsonUtil.parseMessage(json, BodyAlive.class));

//        System.out.println("[DEBUG] handleBodyAlive called at " + System.currentTimeMillis());
        if (socket.isClosed())
            System.out.println("[DEBUG] socket closed? " + socket.isClosed());
        sendMessage(new Message<>(new BodyAlive()));
//        System.out.println("[DEBUG] handleBodyAlive finished. Alive message sent.");

        heartbeatLogger.info("Client {} sent Alive response.", ID);
    }

    /**
     * Handles the "Welcome" message received from the server.
     * This method parses the incoming JSON string to extract a message of type {@code BodyWelcome}.
     * It then retrieves and prints the client ID assigned by the server from the message body.
     *
     * @param json the JSON string containing the serialized {@code BodyWelcome} message
     */
    private void handleBodyWelcome(String json) {
        Message<BodyWelcome> msg = JsonUtil.parseMessage(json, BodyWelcome.class);
        clientLogger.info("[RECEIVED] Welcome: {}", gsonPretty.toJson(msg));
        if (ID != null)
            throw new IllegalStateException("Client ID already initialized.");
        this.ID = msg.messageBody().clientID();
        usernames.put(ID, "(me)");

        //  PlayerValues only after Welcome
        LoginController loginCtrl = ControllerRegistry.getLoginController();
        if (loginCtrl != null && loginCtrl.cachedName != null) {
            String name = loginCtrl.cachedName;
            int figure = loginCtrl.cachedFigure;
            sendMessage(new Message<>(new BodyPlayerValues(name, figure)));
        }
    }

    /**
     * Handles the "BodyPlayerAdded" message received from the server.
     * This method processes a JSON string representing the {@code BodyPlayerAdded} message,
     * updates the client's internal state with the new player's details,
     * and updates the appropriate GUI components through the {@code LobbyController} and {@code LoginController}.
     *
     * @param json the JSON string containing the serialized {@code BodyPlayerAdded} message
     */
    private void handleBodyPlayerAdded(String json) {
        Message<BodyPlayerAdded> message = JsonUtil.parseMessage(json, BodyPlayerAdded.class);
        BodyPlayerAdded body = message.messageBody();
        String username = body.name();

        usernames.put(body.clientID(), username);

        boolean isMe = body.clientID().equals(getID());
        if (isMe) {
            usernames.put(ID, username);
        }

        //  FINAL variables for lambda use
        final String finalUsername = username;
        final int finalFigure = body.figure();
        final int clientID = body.clientID();

        javafx.application.Platform.runLater(() -> {
            LobbyController lobbyCtrl = ControllerRegistry.getLobbyController();
            if (lobbyCtrl != null) {
                boolean isReady = false;
                lobbyCtrl.addPlayer(clientID, finalUsername, finalFigure, isReady);

            } else {
                synchronized (pendingPlayers) {
                    pendingPlayers.add(body);
                }

            }

            if (isMe) {
                LoginController loginCtrl = ControllerRegistry.getLoginController();
                if (loginCtrl != null) {
                    loginCtrl.loginSuccess();
                }
            }
        });
    }

    /**
     * If the LobbyController is not yet initialized (e.g., UI not ready),
     * the received player information is temporarily stored in the pendingPlayers list.
     * This allows the application to process and display the player data later,
     * once the lobby UI is available and ready to render the list of players.
     * The block is synchronized to ensure thread safety, as this method might be
     * accessed from different threads (e.g., the network listener thread).
     */
    public void flushPendingPlayers() {
        javafx.application.Platform.runLater(() -> {
            LobbyController lobbyCtrl = ControllerRegistry.getLobbyController();
            if (lobbyCtrl == null) return;

            synchronized (pendingPlayers) {
                for (BodyPlayerAdded body : pendingPlayers) {
                    int clientID = body.clientID();
                    String name = body.name();
                    int figure = body.figure();
                    boolean isMe = (clientID == this.ID);
                    String finalName = name;

                    lobbyCtrl.addPlayer(clientID, finalName, figure, false);
                }
                pendingPlayers.clear();
            }
        });
    }

    /**
     * Handles the event of a player being renamed by parsing the provided JSON message
     * and updating the player name in the lobby controller.
     *
     * @param json A JSON string representing the message containing player renaming information,
     *             including the client's ID and the new name.
     */
    private void handleBodyPlayerRenamed(String json) {
        Message<BodyPlayerRenamed> msg = JsonUtil.parseMessage(json, BodyPlayerRenamed.class);
        int clientID = msg.messageBody().clientID();
        String newName = msg.messageBody().newName();

        javafx.application.Platform.runLater(() -> {
            LobbyController ctrl = ControllerRegistry.getLobbyController();
            if (ctrl != null) {
                ctrl.renamePlayer(clientID, newName);
            }
        });
    }

    /**
     * Processes a JSON string representing a "BodyPlayerStatus" message and updates the
     * corresponding player's ready status in the lobby GUI.
     *
     * @param json the JSON string containing the serialized {@code BodyPlayerStatus} message
     */
    private void handleBodyPlayerStatus(String json) {
        Message<BodyPlayerStatus> message = JsonUtil.parseMessage(json, BodyPlayerStatus.class);
        BodyPlayerStatus body = message.messageBody();

        int clientID = body.clientID();
        boolean ready = body.ready();
        // JavaFX-Thread für GUI-Update
        javafx.application.Platform.runLater(() -> {
            LobbyController controller = ControllerRegistry.getLobbyController();
            if (controller != null) {
                controller.updatePlayerStatus(clientID, ready);
            }
        });
    }

    /**
     * Handles the "BodySelectMap" message received from the server.
     * This method processes a JSON string representing a {@code BodySelectMap} message, deserializing it
     * to extract the list of available maps from the message body. It includes preparation for selecting
     * a map from the list, where further logic can be implemented to handle the selection process.
     * Finally, it creates a {@code BodyMapSelected} message with the chosen map and sends it to the server.
     *
     * @param json the JSON string containing the serialized {@code BodySelectMap} message
     */
    private void handleBodySelectMap(String json) {
        Message<BodySelectMap> message = JsonUtil.parseMessage(json, BodySelectMap.class);
        List<String> availableMaps = message.messageBody().availableMaps();

        if (availableMaps == null || availableMaps.isEmpty()) {
            errorLogger.error("[SelectMap] Server hat keine Karten geschickt oder Body war leer.");
            return;
        }

        Platform.runLater(() -> {
            LobbyController controller = ControllerRegistry.getLobbyController();
            if (controller != null) {
                int selectorID = message.messageBody().selectorID();
                int myID = ClientSingleton.getInstance().getID();
                if (selectorID == myID) {
                    controller.showMapSelection(message.messageBody().availableMaps());
                } else {
                    controller.hideMapSelection();
                }
            } else {
                errorLogger.error("[SelectMap] LobbyController ist null in handleBodySelectMap");
            }
        });
    }


    /**
     * Handles the selected body map event provided in JSON format.
     * Parses the JSON message, retrieves the selected map,
     * and updates the LobbyController with the corresponding map information.
     *
     * @param json a JSON string representing the selected body map event,
     *             expected to contain the necessary data to identify the selected map.
     */
    private void handleBodyMapSelected(String json) {
        Message<MessageDefinitions.BodyMapSelected> message =
                JsonUtil.parseMessage(json, MessageDefinitions.BodyMapSelected.class);
        selectedMap = message.messageBody().map();

        Platform.runLater(() -> {
            LobbyController lobbyCtrl = ControllerRegistry.getLobbyController();
            if (lobbyCtrl != null) {
                lobbyCtrl.setMapLabel("Ausgewählte Karte: " + selectedMap);
            }
        });
    }

    /**
     * Handles the event when the body of a game started message is received.
     * This method processes the incoming JSON message, updates the server-side energy map,
     * and triggers the display of the game field on the user interface.
     *
     * @param json The JSON string representing the game started message.
     */
    private void handleBodyGameStarted(String json) {
        Message<BodyGameStarted> message = JsonUtil.parseMessage(json, BodyGameStarted.class);
        BodyGameStarted body = message.messageBody();

        // BoardMap vom Server holen
        List<List<List<Field>>> boardMap = body.gameMap();
        if (boardMap == null || boardMap.isEmpty()) {
            errorLogger.error("[GameStarted] Empfangenes boardMap ist null oder leer!");
            return;
        }

        appLogger.info("[CLIENT DEBUG] BoardMap size: {}x{}", boardMap.size(), boardMap.get(0).size());
        for (int x = 0; x < boardMap.size(); x++) {
            for (int y = 0; y < boardMap.get(x).size(); y++) {
                List<Field> fields = boardMap.get(x).get(y);
                if (fields != null) {
                    for (Field field : fields) {
                        if ("Energy-Space".equals(field.type())) {
                            MessageDefinitions.FieldEnergySpace es = (MessageDefinitions.FieldEnergySpace) field;
//                            appLogger.info("[CLIENT DEBUG] Found EnergySpace at ({},{}) with count: {}", x, y, es.count());
                        }
                    }
                }
            }
        }

        // Energie und Checkpoints initialisieren
        for (Integer playerId : usernames.keySet()) {
            energy.put(playerId, body.energy());
            checkpointsReached.put(playerId, 0);
        }

        // Lokale Map speichern (z.B. in ClientSingleton oder deiner eigenen Struktur)
        this.setCurrentGameMap(boardMap);

//        System.out.println("[DEBUG] handleBodyGameStarted aufgerufen");
//        System.out.println("Map-Größe: " + boardMap.size() + " × " + boardMap.getFirst().size());

        // Szenewechsel zur GameView
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/lmu/dbs/ifi/sep25/GameView.fxml"));
                Parent root = loader.load();
                GameController controller = loader.getController();
                controller.setRoot(root);
                // Im Controller-Registry speichern
                ControllerRegistry.setGameController(controller);

                controller.drawBoard(boardMap);
                controller.setInitialPlayerStats(energy, checkpointsReached);


                // Szene wechseln
                Stage stage = ControllerRegistry.getPrimaryStage();
                if (stage != null) {
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                    stage.setScene(scene);
                    stage.show();
                    stage.setResizable(true);
                    stage.setWidth(1400);
                    stage.setHeight(1000);
                    stage.setMinWidth(600);
                    stage.setMinHeight(400);
                    stage.setTitle("Robo Rally Game");
                    controller.setPlayersFromLobby(ControllerRegistry.getLobbyController().getPlayers());

                } else {
                    errorLogger.error("[ERROR] Kein gültiges Fenster (Stage) gefunden!");
                }


            } catch (IOException e) {
                errorLogger.error("[GameStarted] Fehler beim Laden der GameView: ", e);
                e.printStackTrace();
            }
        });
    }

    /**
     * Handles a "BodyReceivedChat" message from the server.
     *
     * <p>This method processes an incoming JSON string representing a chat message
     * by deserializing it into a {@code BodyReceivedChat} object. Based on the
     * message details, it displays the appropriate chat content in the console:
     * <ul>
     *   <li>Private messages are displayed as whispers.</li>
     *   <li>Messages from the server (identified by a {@code from} value of 0)
     *       are prefixed with “[SERVER]”.</li>
     *   <li>Public messages from other users display their usernames, resolved
     *       with {@code usernames.getByKeyOrDefault}, or their raw ID if no match exists.</li>
     * </ul>
     *
     * <p>Messages sent by the current user (identified by {@code ID}) are ignored.
     *
     * @param json the JSON string containing the serialized {@code BodyReceivedChat} message
     */
    private void handleBodyReceivedChat(String json) {
        BodyReceivedChat body = JsonUtil.parseMessage(json, BodyReceivedChat.class).messageBody();
        if (!body.from().equals(ID)) {
            String sender;
            if (body.from().equals(0)) {
                sender = "[SERVER]";
            } else if (body.isPrivate()) {
                sender = usernames.getByKeyOrDefault(body.from(), body.from().toString()) + " (private)";
            } else {
                sender = usernames.getByKeyOrDefault(body.from(), body.from().toString());
            }

            String fullMessage = sender + ": " + body.message();

            javafx.application.Platform.runLater(() -> {
                GameController gameController = ControllerRegistry.getGameController();
                LobbyController lobbyController = ControllerRegistry.getLobbyController();
                if (gameController != null && gameController.getRoot().isVisible()) {
                    gameController.appendChatMessage(fullMessage);
                } else if (lobbyController != null) {
                    lobbyController.appendChatMessage(fullMessage);
                }

            });
        }
    }

    /**
     * Handles an error response message received from the server.
     *
     * <p>This method processes a JSON string representing a {@code BodyError} message,
     * extracts the error details, and logs the error description to the
     * standard error stream. After handling the error, it releases all
     * associated resources by invoking the {@code closeAll} method.
     *
     * @param json the JSON string containing the serialized {@code BodyError} message
     */
    private void handleBodyError(String json) {
        Message<BodyError> msg = JsonUtil.parseMessage(json, BodyError.class);
        String errorText = msg.messageBody().error();
        errorLogger.error("[RECEIVED]  ERROR  {}", errorText);

        Platform.runLater(() -> {
            if (errorText.contains("Robot already taken.")) {
                LoginController loginCtrl = ControllerRegistry.getLoginController();
                if (loginCtrl != null && !loginCtrl.isFigureTakenWarningShown()) {
                    loginCtrl.setFigureTakenWarningShown(true);
                    loginCtrl.displayFigureAlreadyTaken();
                }
            } else if (errorText.contains("Again card cannot be played in the first register")) {
                GameController gameCtrl = ControllerRegistry.getGameController();
                if (gameCtrl != null) {
                    gameCtrl.appendGameLog(errorText, "error");
                    gameCtrl.displayErrorAlert("Card placement error",
                            "The card Again cannot be placed in the first register position!\n" +
                                    "Please select the 2nd to 5th register positions.");
                    gameCtrl.highlightRegisterSlot(0);
                }
            } else if (errorText.contains("Card") || errorText.contains("register") || errorText.contains("hand")) {
                GameController gameCtrl = ControllerRegistry.getGameController();
                if (gameCtrl != null) {
                    gameCtrl.appendGameLog(errorText, "error");
                    gameCtrl.displayErrorAlert("Card operation error", errorText);
                }
            } else if (errorText.toLowerCase().contains("starting position")) {
                GameController gameCtrl = ControllerRegistry.getGameController();
                if (gameCtrl != null) {
                    gameCtrl.deselectStartingPosition();
                    gameCtrl.appendGameLog(errorText, "error");
                    gameCtrl.displayErrorAlert("Starting position selection error", errorText);
                }
            } else {

                GameController gameCtrl = ControllerRegistry.getGameController();
                if (gameCtrl != null) {
                    gameCtrl.appendGameLog(errorText, "error");
                    gameCtrl.displayErrorAlert("Server Error", errorText);
                }
            }
        });
    }

    /**
     * Handles a BodyCardPlayed message received from the server.
     * <p>
     * Parses the incoming JSON message, extracts the player and card information,
     * logs the event, and updates the game UI to display the played card and a chat message.
     * This method ensures that UI updates are performed on the JavaFX application thread.
     *
     * @param json the JSON string containing the BodyCardPlayed message
     */
    private void handleBodyCardPlayed(String json) {
        Message<BodyCardPlayed> message = JsonUtil.parseMessage(json, BodyCardPlayed.class);
        BodyCardPlayed body = message.messageBody();
        int clientID = body.clientID();
        String card = body.card();
        String playerName = usernames.getByKeyOrDefault(clientID, null);
//        String playerName = usernames.getByKeyOrDefault(clientID, "Spieler" + clientID);
        String logMessage = "Spieler " + playerName + " hat Karte " + card + " gespielt.";
        appLogger.info("[GAME] {}", logMessage);

        Platform.runLater(() -> {
            GameController gameCtrl = ControllerRegistry.getGameController();
            if (gameCtrl != null) {
                gameCtrl.appendGameLog(logMessage, "info");
                gameCtrl.showPlayedCard(clientID, card);
            } else {
                appLogger.warn("[WARN] GameController ist null in handleBodyCardPlayed");
            }
        });
    }
    /**
     * Waits for the GameController instance to become available.
     * <p>
     * Tries to retrieve the GameController from the ControllerRegistry, retrying up to
     * {@code maxRetries} times with a delay between attempts. If the controller is not
     * available after all retries, returns {@code null}.
     *
     * @return the GameController instance if available, or {@code null} if not found after retries
     */
    private GameController waitForGameController() {
        int maxRetries = 10;
        int delayMillis = 100;

        for (int i = 0; i < maxRetries; i++) {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                return controller;
            }

            try {
                Thread.sleep(delayMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return null;
    }

    /**
     * Processes the server message indicating which player is currently active.
     * Calls the GameController to visually highlight the current player.
     *
     * @param json JSON string containing the client ID of the active player
     */
    private void handleBodyCurrentPlayer(String json) {
        Message<MessageDefinitions.BodyCurrentPlayer> message =
                JsonUtil.parseMessage(json, MessageDefinitions.BodyCurrentPlayer.class);

        int currentClientID = message.messageBody().clientID();

        Platform.runLater(() -> {
            GameController controller = waitForGameController();
            if (controller != null) {
                int phase = controller.getCurrentPhaseID();

                if (phase == 2) {
                    appLogger.warn(" not markCurrentPlayer in Programmierphase (Phase 2)");
                    return;
                }

                controller.markCurrentPlayer(currentClientID);
            }
        });
    }

    /**
     * Handles the "BodyActivePhase" message received from the server.
     * This method processes a JSON string representing a {@code BodyActivePhase} message,
     * extracts the phase information from the message body, and updates the client's internal state.
     *
     * @param json the JSON string containing the serialized {@code BodyActivePhase} message
     */
    private void handleBodyActivePhase(String json) {
        Message<BodyActivePhase> message = JsonUtil.parseMessage(json, BodyActivePhase.class);
        int phaseID = message.messageBody().phase();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.updatePhase(phaseID);
            }
        });
    }


    /**
     * Handles the "StartingPointTaken" message from the server, indicating that a player
     * has selected and occupied a starting position on the board.
     *
     * @param json the JSON string containing the serialized {@code BodyStartingPointTaken} message
     */
    private void handleBodyStartingPointTaken(String json) {
        Message<MessageDefinitions.BodyStartingPointTaken> message =
                JsonUtil.parseMessage(json, MessageDefinitions.BodyStartingPointTaken.class);
        MessageDefinitions.BodyStartingPointTaken body = message.messageBody();

        int x = body.x();
        int y = body.y();
        int clientID = body.clientID();
        String direction = body.direction();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                if (clientID == ID) {
                    controller.deactivateStartPointClick();
                }
                controller.displayStartingPoint(x, y, clientID, direction);
                controller.setRobotPosition(clientID, new Position(x, y));
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyStartingPointTaken");
            }
        });
    }

    /**
     * Handles the list of cards the player receives from the server.
     * Updates the local hand and displays it in the GUI.
     *
     * @param json JSON string containing the list of cards
     */
    private void handleBodyYourCards(String json) {
        Message<BodyYourCards> message = JsonUtil.parseMessage(json, BodyYourCards.class);
        BodyYourCards body = message.messageBody();

        hand.clear();  // Clear old cards
        hand.addAll(body.cardsInHand());

        Platform.runLater(() -> {
            GameController controller = waitForGameController();
            if (controller != null) {
                controller.displayHandCards(body.cardsInHand());
            } else {
                errorLogger.error("[FEHLER] GameController ist null nach Warten in handleBodyYourCards");
            }
        });
    }

    /**
     * Handles a message indicating how many cards another player has received.
     * This does not include the content of the cards, only the count,
     * and is used to visually show hidden cards (e.g., card backs).
     *
     * @param json JSON string containing the client ID and number of cards
     */
    private void handleBodyNotYourCards(String json) {
        Message<BodyNotYourCards> message = JsonUtil.parseMessage(json, BodyNotYourCards.class);
        BodyNotYourCards body = message.messageBody();

        int clientID = body.clientID();
        int count = body.cardsInHand();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.displayHiddenCardsForPlayer(clientID, count);
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyNotYourCards");
            }
        });
    }

    /**
     * Handles a message indicating that the programming deck was shuffled.
     * Can be used to trigger a visual animation or log event.
     *
     * @param json JSON string with no additional body information
     */
    private void handleBodyShuffleCoding(String json) {
        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showShuffleAnimation(); // Optional UI effect
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyShuffleCoding");
            }

        });
    }

    /**
     * Handles a message indicating that a card has been selected for a register.
     * Can be used to highlight selected cards or update UI state.
     *
     * @param json JSON string containing the card selection info
     */
    private void handleBodyCardSelected(String json) {
        Message<BodyCardSelected> message = JsonUtil.parseMessage(json, BodyCardSelected.class);
        BodyCardSelected body = message.messageBody();

        int clientID = body.clientID();
        int register = body.register();
        boolean filled = body.filled();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
//                appLogger.info("Calling handleCardSelection for clientID: {} register: {} filled: {}", clientID, register, filled);
                controller.handleCardSelection(clientID, register, filled); // implement in GameController
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyCardSelected");
            }
        });
    }

    /**
     * Handles the "BodySelectionFinished" message received from the server.
     * This method processes a JSON string representing a {@code BodyCardSelected} message.
     * It checks if the client ID matches the received message and verifies if the selection is complete.
     * If the selection is complete and this is the first time the ready state is being registered,
     * a {@code BodyTimerStarted} message is sent to the server.
     *
     * @param json the JSON string containing the serialized {@code BodyCardSelected} message
     */
    private void handleBodySelectionFinished(String json) {
        Message<BodySelectionFinished> message = JsonUtil.parseMessage(json, BodySelectionFinished.class);
        BodySelectionFinished body = message.messageBody();
        int clientID = body.clientID();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.markPlayerReady(clientID);
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodySelectionFinished");
            }
        });
    }


    private void handleBodyTimerStarted() {
        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.startCountdown();
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyTimerStarted");
            }
        });
    }

    /**
     * Handles the message indicating that the programming timer has ended.
     * Optionally highlights players who did not complete selection in time.
     *
     * @param json JSON string containing a list of slow players (optional)
     */
    private void handleBodyTimerEnded(String json) {
        Message<BodyTimerEnded> message = JsonUtil.parseMessage(json, BodyTimerEnded.class);
        BodyTimerEnded body = message.messageBody();

        List<Integer> clientIDs = body.clientIDs();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showTimerEnded(clientIDs);
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyTimerEnded");
            }
        });
    }

    /**
     * Handles the cards the server confirms the player now has registered.
     * These are the cards already selected and locked for execution.
     *
     * @param json JSON string containing the list of selected cards
     */
    private void handleBodyCardsYouGotNow(String json) {
        Message<BodyCardsYouGotNow> message = JsonUtil.parseMessage(json, BodyCardsYouGotNow.class);
        BodyCardsYouGotNow body = message.messageBody();

        List<String> cards = body.cards();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.displayConfirmedCards(cards); //
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyCardsYouGotNow");
            }
        });
    }

    /**
     * Handles the list of active cards for all players.
     * Displays the card names and triggers basic animations.
     *
     * @param json JSON string containing active cards
     */
    private void handleBodyCurrentCards(String json) {
        Message<BodyCurrentCards> message = JsonUtil.parseMessage(json, BodyCurrentCards.class);
        List<ActiveCard> activeCards = message.messageBody().activeCards();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                Map<Integer, List<String>> registersByClient = new HashMap<>();

                for (ActiveCard card : activeCards) {
                    int clientID = card.clientID();
                    String cardName = card.card();

                    registersByClient.computeIfAbsent(clientID, k -> new ArrayList<>()).add(cardName);

                    controller.showActiveCard(clientID, cardName);
                }

                for (Map.Entry<Integer, List<String>> entry : registersByClient.entrySet()) {
                    controller.updateOtherPlayerRegister(entry.getKey(), entry.getValue());
                }
            }
        });

        currentRegister++;
    }

    /**
     * Handles a message indicating that a specific card in a register
     * has been replaced (e.g. due to damage or effect).
     *
     * @param json JSON string containing the register number, new card name, and client ID
     */
    private void handleBodyReplaceCard(String json) {
        Message<BodyReplaceCard> message = JsonUtil.parseMessage(json, BodyReplaceCard.class);
        BodyReplaceCard body = message.messageBody();

        int register = body.register();
        String newCard = body.newCard();
        int clientID = body.clientID();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.replaceCardInRegister(clientID, register, newCard);
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyReplaceCard");
            }
        });
    }

    /**
     * Handles the movement of a robot on the board.
     * Updates the UI with the new position of the robot.
     *
     * @param json JSON string containing the new coordinates and client ID
     */
    public void handleBodyMovement(String json) {
//        clientLogger.info("[CLIENT] handleBodyMovement called");
//        clientLogger.info("json = {}", json);
        Message<BodyMovement> message = JsonUtil.parseMessage(json, BodyMovement.class);
        BodyMovement body = message.messageBody();

        int clientID = body.clientID();
        int newX = body.x();
        int newY = body.y();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller == null) {
                errorLogger.error("[WARN] GameController is null in handleBodyMovement");
                return;
            }

            controller.syncPlayerNames();

            if (newX < 0 || newY < 0 ||
                    (currentGameMap != null && (newX >= currentGameMap.size() ||
                            (currentGameMap.get(0) != null && newY >= currentGameMap.get(0).size())))) {
                clientLogger.info("Robot {} fell off the board to point ({}, {})", clientID, newX, newY);
                controller.handleRobotFellOffBoard(clientID);
                return;
            }


            Position oldPos = controller.getRobotPosition(clientID);
//            if (oldPos == null) {
//                errorLogger.warn("[WARN] Keine alte Roboterposition bekannt für Client {}", clientID);
//                return;
//            }

            //  // Special case: Robot restarts from outside the board (reboot situation)
            if (oldPos == null || oldPos.x() == -1 || oldPos.y() == -1) {
                clientLogger.info("Robot {} is being placed/rebooted to ({}, {})", clientID, newX, newY);
                controller.moveRobotTo(clientID, newX, newY);

                // Ensure that the robot is set in the correct direction.
                String direction = controller.getRobotDirection(clientID);
                if (direction != null) {
                    controller.updateRobotDirection(clientID, direction);
                }
                return;
            }


            int dx = newX - oldPos.x();
            int dy = newY - oldPos.y();
            Direction moveDir = null;

            if (dx == 1) moveDir = Direction.EAST;
            else if (dx == -1) moveDir = Direction.WEST;
            else if (dy == 1) moveDir = Direction.SOUTH;
            else if (dy == -1) moveDir = Direction.NORTH;


            if (moveDir == null) {
                errorLogger.warn("[WARN] Ungültige Bewegungsrichtung von ({},{}) nach ({},{})", oldPos.x(), oldPos.y(), newX, newY);
                controller.moveRobotTo(clientID, newX, newY);
                return;
            }

            // Check if the current map range is valid
            if (currentGameMap == null || newX >= currentGameMap.size() || newY >= currentGameMap.get(0).size() ||
                    oldPos.x() >= currentGameMap.size() || oldPos.y() >= currentGameMap.get(0).size()) {
                errorLogger.warn("[WARN] Ungültige Kartenkoordinaten: alt ({},{}) neu ({},{})", oldPos.x(), oldPos.y(), newX, newY);
                controller.moveRobotTo(clientID, newX, newY);
                return;
            }

            List<MessageDefinitions.Field> sourceFields = currentGameMap.get(oldPos.x()).get(oldPos.y());
            List<MessageDefinitions.Field> targetFields = currentGameMap.get(newX).get(newY);

            boolean blocked = false;

            for (MessageDefinitions.Field f : sourceFields) {
                if (f instanceof MessageDefinitions.FieldWall wall &&
                        wall.orientations().contains(moveDir.toString().toLowerCase())) {
                    blocked = true;
                    break;
                }
            }

            for (MessageDefinitions.Field f : targetFields) {
                if (f instanceof MessageDefinitions.FieldWall wall &&
                        wall.orientations().contains(moveDir.turnAround().toString().toLowerCase())) {
                    blocked = true;
                    break;
                }
            }

            if (blocked) {
                controller.appendGameLog("Bewegung durch Wand verhindert.", "warn");
                return;
            }

            controller.moveRobotTo(clientID, newX, newY);
        });
    }


    /**
     * Handles a message indicating that a player's robot should rotate.
     *
     * @param json JSON string containing clientID and rotation direction
     */
    private void handleBodyPlayerTurning(String json) {
        Message<BodyPlayerTurning> message = JsonUtil.parseMessage(json, BodyPlayerTurning.class);
        BodyPlayerTurning body = message.messageBody();

        int clientID = body.clientID();
        String rotation = body.rotation(); // "clockwise" or "counterclockwise"

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.rotateRobot(clientID, rotation);
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyPlayerTurning");
            }
        });
    }

    /**
     * Handles the reception of damage cards from the server.
     *
     * <p>Parses the incoming JSON message and extracts the list of damage cards.
     * Then updates the UI by displaying the received cards to the player.
     *
     * <p>This method is triggered when the server assigns damage cards to the client.
     *
     * @param json the JSON string containing the list of damage cards
     */
    private void handleBodyDrawDamage(String json) {
        Message<BodyDrawDamage> message = JsonUtil.parseMessage(json, BodyDrawDamage.class);
        BodyDrawDamage body = message.messageBody();
        List<String> cards = body.cards();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showDrawnDamageCards(cards);
//                controller.appendChatMessage("[INFO] Du hast " + cards.size() + " Schadenskarten erhalten.");
            } else {
                errorLogger.error("[WARN] GameController ist null in handleBodyDrawDamage");
            }
        });
    }
    /**
     * Handles the server request for the player to select damage cards.
     *
     * <p>Parses the incoming JSON to determine how many damage cards the player must pick
     * and from which available piles. Then displays the selection UI and sends the
     * chosen cards back to the server.
     *
     * @param json the JSON string containing the selection request details
     */
     private void handleBodyPickDamage(String json) {
        Message<BodyPickDamage> message = JsonUtil.parseMessage(json, BodyPickDamage.class);
        BodyPickDamage body = message.messageBody();
        int count = body.count();
        List<String> availablePiles = body.availablePiles();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.promptDamageCardSelection(count, availablePiles, selectedCards -> {
                    if (selectedCards != null && !selectedCards.isEmpty()) {
                        sendMessage(new Message<>(new MessageDefinitions.BodySelectedDamage(selectedCards)));
                        controller.appendGameLog("Du hast folgende Schadenskarten gewählt: " + selectedCards, "info");
                    } else {
                        controller.appendGameLog("Keine Schadenskarten ausgewählt.", "warn");
                    }
                });
            } else {
                errorLogger.error("[WARN] GameController ist null in handleBodyPickDamage");
            }
        });
    }


    /**
     * Represents the various types of animations available within the system.
     * Each animation type corresponds to a specific visual or interactive behavior.
     * This can include player actions, environmental interactions, and system states.
     * <p>
     * The enum provides methods to get a string representation of an animation type
     * and to create an AnimationType instance based on a string input.
     */
    public enum AnimationType {
        MOVEMENT("Movement"),
        CLOCKWISE("Clockwise"),
        COUNTERCLOCKWISE("Counterclockwise"),
        BLUECONVEYORBELT("BlueConveyorBelt"),
        GREENCONVEYORBELT("GreenConveyorBelt"),
        PUSHPANEL("PushPanel"),
        GEAR("Gear"),
        CHECKPOINT("Checkpoint"),
        PLAYERSHOOTING("PlayerShooting"),
        WALLSHOOTING("WallShooting"),
        PLAYERHURT("PlayerHurt"),
        ENERGYSPACE("EnergySpace"),
        ENERGYCONSUMPTION("EnergyConsumption"),
        ANIMATION_NOT_SUPPORTED("AnimationNotSupported");

        //TODO add animation types depending whats need for UI

        private final String asString;

        AnimationType(String asString) {
            this.asString = asString;
        }

        public String asString() {
            return asString;
        }

        public static AnimationType fromString(String str) {
            return switch (str.toLowerCase()) {
                case "movement" -> MOVEMENT;
                case "clockwise" -> CLOCKWISE;
                case "counterclockwise" -> COUNTERCLOCKWISE;
                case "playershooting" -> PLAYERSHOOTING;

                default -> ANIMATION_NOT_SUPPORTED;
            };
        }
    }
    /**
     * Handles the body animation logic based on the provided JSON input.
     * This method processes the input to determine the type of animation
     * and performs appropriate actions or sends an error message in case of issues.
     *
     * @param json the input JSON string containing information about the body animation
     */

    private void handleBodyAnimation(String json) {
        Message<BodyAnimation> message = JsonUtil.parseMessage(json, BodyAnimation.class);
        BodyAnimation body = message.messageBody();

        String type = body.type(); // e.g., "Movement", "Clockwise", "Checkpoint"

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.playAnimation(type);  // Visual feedback in GUI
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyAnimation");
            }
        });
    }

    /**
     * Handles the reboot notification from the server for a specific player.
     * If the client is the one rebooting, it sends back the chosen reboot direction.
     *
     * @param json the JSON string containing the reboot message
     */
    public void handleBodyReboot(String json) {
        Message<BodyReboot> message = JsonUtil.parseMessage(json, BodyReboot.class);
        BodyReboot body = message.messageBody();
        int robotID = body.clientID(); // Note: this is actually robotID, not clientID
        clientLogger.info("Received BodyReboot for robotID: {}, my ID: {}", robotID, ID);
        rebootingInProgress = body.clientID(); // Mark that a reboot is pending

        // Zeige Reboot-Animation/Info im Spiel
        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showReboot(body.clientID());
            }
        });

        // Check if this is MY robot that needs to reboot
        // Need to compare with my clientID, not robotID
        boolean isMyRobot = body.clientID().equals(ID);

        clientLogger.info("Is my robot rebooting? {} (robotID: {}, myID: {})", isMyRobot, robotID, ID);

        // Wenn der eigene Roboter rebooted wird → Richtung auswählen lassen
        if (isMyRobot) {
            clientLogger.info("My robot is rebooting - asking for direction");
            Platform.runLater(() -> {
                GameController controller = ControllerRegistry.getGameController();
                if (controller != null) {
                    controller.askRebootDirection(direction -> {
                        sendMessage(new Message<>(new BodyRebootDirection(direction)));
                    });
                } else {
                    // Fallback falls kein Controller verfügbar
                    clientLogger.warn("GameController not available, using default direction");
                    sendMessage(new Message<>(new BodyRebootDirection("top")));
                }
            });
        }
    }


    /**
     * Handles the server response indicating the direction chosen for a rebooted robot.
     * Sends the saved reboot movement and triggers an optional reboot animation.
     *
     * @param json JSON string containing the reboot direction
     */
    public void handleBodyRebootDirection(String json) {
        Message<BodyRebootDirection> message = JsonUtil.parseMessage(json, BodyRebootDirection.class);
        String direction = message.messageBody().direction();
        clientLogger.info("Received reboot direction confirmation from server: {}", direction);

//        // Send the previously saved reboot movement
//        if (rebootPosition != null) {
//            sendMessageSelf(new Message<>(rebootPosition));
//            rebootPosition = null;
//        }

        // Update local robot direction records
        if (rebootingInProgress != -1) {
            final int currentRebootingClient = rebootingInProgress;
            Platform.runLater(() -> {
                GameController controller = ControllerRegistry.getGameController();
                if (controller != null) {
                    controller.updateRobotDirection(currentRebootingClient, direction);
                    controller.showRebootDirection(direction);
                    controller.appendGameLog("Der Roboter wurde in Richtung " + direction + " neu gestartet.", "info");
                    Position currentPos = controller.getRobotPosition(rebootingInProgress);
                    if (currentPos != null && currentPos.x() >= 0 && currentPos.y() >= 0) {
                        controller.moveRobotTo(rebootingInProgress, currentPos.x(), currentPos.y());
                    }
                }
            });

            // Clear the status of the restart in progress
            rebootingInProgress = -1;
        }

//        // Optional: display a reboot animation
//        Platform.runLater(() -> {
//            GameController controller = ControllerRegistry.getGameController();
//            if (controller != null) {
//                controller.showRebootDirection(direction);
//            }
//        });

        // Optionally notify via animation message
//        sendMessageSelf(new Message<>(new BodyAnimation("Reboot")));
    }

    /**
     * Handles a message indicating an energy change for a player.
     *
     * @param json the JSON string containing the energy update
     */
    private void handleBodyEnergy(String json) {
        Message<BodyEnergy> message = JsonUtil.parseMessage(json, BodyEnergy.class);
        BodyEnergy body = message.messageBody();

        int clientID = body.clientID();
        int count = body.count();  // energy value
        String source = body.source();  // e.g., "EnergySpace", "Laser"

        energy.put(clientID, count);  // update internal tracking

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showEnergyChange(clientID, count, source);
                controller.updateEnergyIfLocal(clientID, count);

            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyEnergy");
            }
        });
    }

    /**
     * Handles a message indicating that a player reached a checkpoint.
     *
     * @param json the JSON string containing the checkpoint information
     */
    private void handleBodyCheckPointReached(String json) {
        Message<BodyCheckPointReached> message = JsonUtil.parseMessage(json, BodyCheckPointReached.class);
        BodyCheckPointReached body = message.messageBody();

        int clientID = body.clientID();
        int number = body.number();  // Checkpoint number

        checkpointsReached.put(clientID, number);

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showCheckpointReached(clientID, number);
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyCheckPointReached");
            }
        });
    }

    /**
     * Handles the game finished event by processing the body of the message and determining
     * whether the client has won or lost.
     *
     * @param json the JSON string containing the game finished message, which includes details
     *             about the client ID and the game outcome
     */
    private void handleBodyGameFinished(String json) {
        Message<BodyGameFinished> message = JsonUtil.parseMessage(json, BodyGameFinished.class);
        BodyGameFinished body = message.messageBody();

        int winnerID = body.clientID();
        boolean isWinner = (winnerID == this.ID);

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showGameResult(isWinner, winnerID);

            } else {
                appLogger.error("[WARN] GameController is null in handleBodyGameFinished");
            }
        });
    }


    //----------------------


    /**
     * Sends a text message through the output stream to the connected server or client.
     * This method attempts to write the message using a PrintWriter instance.
     * If an error occurs during the process, it logs the failure message to the error stream.
     *
     * @param msg the text message to be sent
     */
    public void sendMessage(String msg) {
        try {
            if (!JsonUtil.parseUnknown(msg).messageType().equalsIgnoreCase("Alive"))
                clientLogger.info("[SENDING] {}: {}", JsonUtil.parseUnknown(msg).messageType(), gsonPretty.toJson(JsonUtil.parseUnknown(msg)));

            writer.println(msg);
            writer.flush();
            if (writer.checkError())
                errorLogger.error("[DEBUG] Error writing to server: " + msg);
        } catch (Exception e) {
            errorLogger.error("Failed to send message to client {}: {}", ID, e.getMessage());
        }
    }

    /**
     * Serializes the provided {@link Message} object into a JSON string and sends it
     * through the established connection.
     * This method utilizes the Gson library for serialization and delegates the actual
     * sending to the overloaded {@code sendMessage(String message)} method.
     * If serialization fails, an error message is logged to the standard error stream.
     *
     * @param msg the {@link Message} object to be serialized and sent
     */
    public void sendMessage(Message<?> msg) {
        try {
            String json = gson.toJson(msg);
            sendMessage(json);
        } catch (Exception e) {
            errorLogger.error("Failed to serialize and send message: {}", e.getMessage(), e);
        }
    }

    /**
     * Sends a message to the client itself using the specified message content.
     *
     * @param msg the message to be sent to the client
     */
    public void sendMessageSelf(String msg) {
        Server.getInstance().getClients().getByValue(ID).sendMessage(msg);
    }

    /**
     * Sends a message to the current instance of the client identified by its unique ID.
     *
     * @param msg the message object to be sent to the client
     */
    public void sendMessageSelf(Message<?> msg) {
        Server.getInstance().getClients().getByValue(ID).sendMessage(msg);
    }

    /**
     * Sends the given message to all connected clients via the server.
     *
     * @param msg the message to be broadcasted to all connected clients
     */
    public void broadcastMessage(Message<?> msg) {
        Server.getInstance().broadcastMessage(msg);
    }

    /**
     * Sends the specified message to all connected clients except the excluded client.
     *
     * @param msg     the message to be broadcasted to connected clients
     * @param exclude the client handler to be excluded from receiving the message
     */
    public void broadcastMessage(Message<?> msg, ClientHandler exclude) {
        Server.getInstance().broadcastMessage(msg, exclude);
    }

    /**
     * Releases resources associated with the client connection.
     * <p>
     * This method ensures the proper closure of the input stream `reader`,
     * output stream `writer`, and the socket connection. It first checks if
     * each resource is non-null (or in the case of the socket, not already
     * closed), and closes them in sequence. If an error occurs during this
     * process, an error message is logged to the standard error stream.
     * <p>
     * This method is typically called to clean up resources when the client
     * disconnects or an issue occurs, ensuring no resource leaks.
     */
    public final void closeAll() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
            System.exit(0);
        } catch (IOException e) {
            errorLogger.error("Error closing client: {}", e.getMessage());
        }
    }

    public int getID() {
        return ID != null ? ID : -1;
    }

    /**
     * Returns a list of players that were received from the server
     * before the LobbyController was fully initialized.
     * This is used by the LobbyController to populate the player list
     * once it becomes available.
     *
     * @return list of pending player entries
     */
    public List<BodyPlayerAdded> getPendingPlayers() {
        return pendingPlayers;
    }

    public String getMyName() {
        return usernames.getByKeyOrDefault(ID, "Unbekannt");
    }

    public int getMyFigureFromLobby() {
        for (BodyPlayerAdded player : pendingPlayers) {
            if (player.clientID() == ID) {
                return player.figure();
            }
        }
        return -1; // Default/fallback
    }


    /**
     * Returns the selected map variable, stored in handleMapSelected.
     *
     * @return selected map as string
     **/
    public String getSelectedMap() {
        return selectedMap;
    }

    public ConcurrentBidirectionalMap<Integer, String> getUsernames() {
        return usernames;
    }

    /**
     * Get the client ID of the robot currently being restarted.
     *
     * @return The client ID of the robot being restarted. If none, return -1.
     */
    public int getRebootingInProgress() {
        return rebootingInProgress;
    }

    /**
     * Clear the status of the restart in progress.
     */
    public void clearRebootingInProgress() {
        this.rebootingInProgress = -1;
    }

}
