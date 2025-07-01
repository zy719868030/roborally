package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
import java.util.*;

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

    private final String protocol = "Version 0.1";
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
    private int phase = 0;
    private final List<String> hand = new ArrayList<>();
    private final List<BodyPlayerAdded> pendingPlayers = new ArrayList<>();
    private int currentRegister = 0;
    private BodyMovement rebootPosition;
    private int rebootingInProgress = -1;
    private final Map<Integer, Integer> energy = new HashMap<>();
    private final Map<Integer, Integer> checkpointsReached = new HashMap<>();

    private List<List<List<MessageDefinitions.Field>>> currentGameMap;

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
                System.out.println("[RECEIVED] " + json); // Test

                try {
                    String messageType = JsonUtil.parseUnknown(json).messageType();
                    if (ID != null && !messageType.equalsIgnoreCase("Alive")) {

                        ThreadContext.put("clientId", ID.toString());
                        clientLogger.info("Received " + messageType + ": " + gsonPretty.toJson(JsonUtil.parseUnknown(json)));
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
                        case "TimerEnded" -> handleBodyTimerEnded(json);
                        case "TimerStarted" -> handleBodyTimerStarted(json);
                        case "CardsYouGotNow" -> handleBodyCardsYouGotNow(json);
                        case "CurrentCards" -> handleBodyCurrentCards(json);
                        case "ReplaceCard" -> handleBodyReplaceCard(json);
                        case "Movement" -> handleBodyMovement(json);
                        case "PlayerTurning" -> handleBodyPlayerTurning(json);
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
//            e.printStackTrace(); DEBUG
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
        appLogger.info("Connected using protocol: {} " + protocol);

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
        if (ID != null)
            throw new IllegalStateException("Client ID already initialized.");
        this.ID = msg.messageBody().clientID();
        clientLogger.info("Your client ID: {}" + getID());
        usernames.put(ID, "(me)");// identify the local player in user lists
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
                    loginCtrl.loginSuccess(finalUsername, finalFigure);
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
    private void handleBodyPlayerRenamed(String json) {//@SEBAS
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
                controller.showMapSelection(availableMaps);
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
        String selectedMap = message.messageBody().map();

        clientLogger.info("[MapSelected] Map selected: " + selectedMap);

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
                controller.showWelcomeDialog();



                // Szene wechseln
                Stage stage = ControllerRegistry.getPrimaryStage();
                if (stage != null) {
                    Scene scene = new Scene(root);
                    scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
                    stage.setScene(scene);
                    stage.show();
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
        errorLogger.error("[Error] Vom Server erhalten:{} ",errorText);

        if (errorText.contains("Figure already selected")) {
            javafx.application.Platform.runLater(() -> {
                LoginController loginCtrl = ControllerRegistry.getLoginController();
                if (loginCtrl != null) {
                    loginCtrl.displayFigureAlreadyTaken();
                }
            });
            return; // nicht schließen!
        }

        closeAll(); // bei anderen Fehlern
    }

    /****/
    private void handleBodyCardPlayed(String json) {
        Message<BodyCardPlayed> message = JsonUtil.parseMessage(json, BodyCardPlayed.class);
        BodyCardPlayed body = message.messageBody();
        int clientID = body.clientID();
        String card = body.card();
        String playerName = usernames.getByKeyOrDefault(clientID, "Spieler" + clientID);
        String logMessage = playerName + "hat Karte gespielt" + card;
        appLogger.info("[GAME] {}", logMessage);

        Platform.runLater(() -> {
            GameController gameCtrl = ControllerRegistry.getGameController();
            if (gameCtrl != null) {
                gameCtrl.appendChatMessage("[GAME]" + logMessage);
                gameCtrl.showPlayedCard(clientID, card);
            } else {
                appLogger.warn("[WARN] GameController ist null in handleBodyCardPlayed");
            }
        });
    }


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
     * Verarbeitet die Nachricht, welcher Spieler gerade am Zug ist.
     *
     * @param json JSON-String mit der Client-ID des aktiven Spielers
     */
    private void handleBodyCurrentPlayer(String json) {//@SEBAS
        Message<MessageDefinitions.BodyCurrentPlayer> message =
                JsonUtil.parseMessage(json, MessageDefinitions.BodyCurrentPlayer.class);

        int currentClientID = message.messageBody().clientID();

        Platform.runLater(() -> {
            GameController controller = waitForGameController();
            if (controller != null) {
                controller.markCurrentPlayer(currentClientID);
            } else {
                errorLogger.error("[WARN] GameController ist null nach Warten in handleBodyCurrentPlayer");
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
            String phaseName = switch (phaseID) {
                case 0 -> "Aufbauphase";
                case 1 -> "Upgradephase";
                case 2 -> "Programmierphase";
                case 3 -> "Aktivierungsphase";
                default -> "Unbekannt";
            };
            controller.updatePhase(phaseName);
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
                controller.displayStartingPoint(x, y, clientID, direction);
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
            }        //TODO fx display deck size | optional: animation Raneem

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
        boolean filled = body.filled();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.handleCardSelection(clientID, filled); // implement in GameController
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
        Message<BodyCardSelected> message = JsonUtil.parseMessage(json, BodyCardSelected.class);
        BodyCardSelected body = message.messageBody();

        if (Objects.equals(body.clientID(), ID)) {
            if (firstReadyRegistry) {
                sendMessage(new Message<>(new BodyTimerStarted()));
            }
            firstReadyRegistry = false;
        }

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.markPlayerReady(body.clientID());
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodySelectionFinished");
            }
        });
    }

    private void handleBodyTimerStarted(String json) {
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
                for (ActiveCard card : activeCards) {
                    int clientID = card.clientID();
                    String cardName = card.card();

                    controller.showActiveCard(clientID, cardName); // Visually show
                    controller.animateRobotAction(clientID, cardName); // Basic arrow
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
     * Handles the processing of body movement data received in a JSON string.
     * Parses the JSON input to extract body movement details, updates the server's
     * state accordingly, and manages robot positioning or movement animations.
     *
     * @param json The JSON string containing body movement information, including
     *             client ID, coordinates, and other relevant data.
     */
    /**
     * Handles the movement of a robot on the board.
     * Updates the UI with the new position of the robot.
     *
     * @param json JSON string containing the new coordinates and client ID
     */
    public void handleBodyMovement(String json) {
        Message<BodyMovement> message = JsonUtil.parseMessage(json, BodyMovement.class);
        BodyMovement body = message.messageBody();
        // Server server = Server.getInstance();

        int clientID = body.clientID();
        int x = body.x();
        int y = body.y();

        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.moveRobotTo(clientID, x, y);
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyMovement");
            }
        });

        //TODO display:
        // maybe clear board of robot, set robot at new position?
        // maybe use sendMessageSelf(new Message<>(new BodyAnimaton("Movement"))
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
        rebootingInProgress = body.clientID(); // mark that a reboot is pending

        // If it's this client's reboot, send the direction back
        if (body.clientID().equals(ID)) {
            String rebootDirection = "top"; // Placeholder direction (can be improved via GUI selection later)

            // Send reboot direction back to server
            sendMessage(new Message<>(new BodyRebootDirection(rebootDirection)));
        }

        // Optional: display something in the GUI
        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showReboot(body.clientID());
            }
        });

        //TODO reboot direction selection
        // display rotation, best directly in/during selection
        // Sollte die Nachricht zur Ausrichtung nicht bis zum Ende der aktuellen Runde
        // angekommen sein, wird die Standardausrichtung verwendet.
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


        // Send the previously saved reboot movement
        if (rebootPosition != null) {
            sendMessageSelf(new Message<>(rebootPosition));
            rebootPosition = null;
        }

        // Optional: display a reboot animation
        Platform.runLater(() -> {
            GameController controller = ControllerRegistry.getGameController();
            if (controller != null) {
                controller.showRebootDirection(direction);
            }
        });

        // Optionally notify via animation message
        sendMessageSelf(new Message<>(new BodyAnimation("Reboot")));
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
            } else {
                errorLogger.error("[WARN] GameController is null in handleBodyEnergy");
            }
            //TODO optional: play energy animation depending on: id -> source
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
                controller.showGameResult(isWinner);
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
            appLogger.info("[SENDING] " + msg);
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
    private void closeAll() {
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

}
