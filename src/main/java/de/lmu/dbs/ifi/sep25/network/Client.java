package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.ui.ControllerRegistry;
import de.lmu.dbs.ifi.sep25.ui.GameController;
import de.lmu.dbs.ifi.sep25.ui.LobbyController;
import de.lmu.dbs.ifi.sep25.ui.LoginController;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;

public class Client {
    // 1. Constants / configuration
    private final Gson gson = new Gson();
    private final String protocol = "Version 0.1";
    private final ConcurrentBidirectionalMap<Integer, String> usernames = new ConcurrentBidirectionalMap<>();

    // 2. Main identity/data
    private Integer ID;
    private volatile boolean isAI = false;
    private volatile boolean firstReadyRegistry = true;

    // 3. Networking / I/O
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    // 4. Game state
    private int phase = -1;
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
                try {
                    System.out.println("[DEBUG] Received: " + json);
                    String messageType = JsonUtil.parseUnknown(json).messageType();
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
        System.out.println("[SERVER] Connected using protocol: " + protocol);

        sendMessage(new Message<>(new BodyHelloServer("Edle Eisbecher", isAI, this.protocol)));
    }

    /**
     * Handles the "BodyAlive" message received from the server.
     * This method writes the provided JSON string directly to the server output stream.
     *
     * @param json the JSON string containing the serialized BodyAlive message
     */
    private void handleBodyAlive(String json) {
        System.out.println("[DEBUG] Alive empfangen und beantwortet"); //TEST
        sendMessage(json);
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
        System.out.println("[SERVER] Your client ID: " + getID());
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
            System.err.println("[ERROR] Server hat keine Karten geschickt oder Body war leer.");
            return;
        }

        Platform.runLater(() -> {
            LobbyController controller = ControllerRegistry.getLobbyController();
            if (controller != null) {
                controller.showMapSelection(availableMaps);
            } else {
                System.err.println("[ERROR] LobbyController ist null in handleBodySelectMap");
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

        System.out.println("[SERVER] Map selected: " + selectedMap);

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
            System.err.println("[ERROR] Empfangenes boardMap ist null oder leer!");
            return;
        }

        // Energie und Checkpoints initialisieren
        for (Integer id : Server.getInstance().getClients().valueSet()) {
            energy.put(id, body.energy());
            checkpointsReached.put(id, 0);
        }

        // Lokale Map speichern (z.B. in ClientSingleton oder deiner eigenen Struktur)
        this.setCurrentGameMap(boardMap);

        System.out.println("[DEBUG] handleBodyGameStarted aufgerufen");
        System.out.println("Map-Größe: " + boardMap.size() + " × " + boardMap.get(0).size());

        // Szenewechsel zur GameView
        Platform.runLater(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/lmu/dbs/ifi/sep25/GameView.fxml"));
                Parent root = loader.load();
                GameController controller = loader.getController();

                // Optional: Im Controller-Registry speichern
                ControllerRegistry.setGameController(controller);

                controller.drawBoard(boardMap);
                controller.setInitialPlayerStats(energy, checkpointsReached);

                // Szene wechseln
                Stage stage = (Stage) ControllerRegistry.getLobbyController().getRoot().getScene().getWindow();
                stage.setScene(new Scene(root));
                stage.show();

            } catch (IOException e) {
                System.err.println("[ERROR] Fehler beim Laden der GameView: " + e.getMessage());
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
                LobbyController controller = ControllerRegistry.getLobbyController();
                if (controller != null) {
                    controller.appendChatMessage(fullMessage);
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
        System.err.println("Error: " + errorText);

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
        //TODO fx display played card to client Sebas
    }

    /****/
    private void handleBodyCurrentPlayer(String json) {
        // TODO @Lukas
        //  - check if equals sent id
        //  - check for game phase
        // set player turn maybe?
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

    /****/
    private void handleBodyStartingPointTaken(String json) {
        //TODO fx display robot
    }

    /****/
    private void handleBodyYourCards(String json) {
        Message<BodyYourCards> message = JsonUtil.parseMessage(json, BodyYourCards.class);
        BodyYourCards body = message.messageBody();

        hand.addAll(body.cardsInHand());
        broadcastMessage(new Message<>(new BodyYourCards(body.cardsInHand())), Server.getInstance().getClients().getByValue(ID));


        //TODO fx display hand
    }

    /****/
    private void handleBodyNotYourCards(String json) {
        //TODO fx display other hands Sebas
    }

    /****/
    private void handleBodyShuffleCoding(String json) {
        //TODO fx display deck size | optional: animation Raneem
    }

    /****/
    private void handleBodyCardSelected(String json) {

        //TODO fx display card selection Sebas
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

        if (body.clientID().equals(ID) && body.filled()) {
            if (firstReadyRegistry) {
                sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyTimerStarted()));
            }
            firstReadyRegistry = false;
        }

        //TODO fx display selection finished
    }

    /****/
    private void handleBodyTimerEnded(String json) {
        //TODO fx display timer ended
    }

    /****/
    private void handleBodyCardsYouGotNow(String json) {
        //TODO fx display cards to register
    }

    /****/
    private void handleBodyCurrentCards(String json) {
        Message<BodyCurrentCards> message = JsonUtil.parseMessage(json, BodyCurrentCards.class);
        BodyCurrentCards body = message.messageBody();

        //TODO display robot animations

        currentRegister++;
    }

    /****/
    private void handleBodyReplaceCard(String json) {
        Message<BodyReplaceCard> message = JsonUtil.parseMessage(json, BodyReplaceCard.class);
        BodyReplaceCard body = message.messageBody();

        //TODO display replaced card in register
    }

    /**
     * Handles the processing of body movement data received in a JSON string.
     * Parses the JSON input to extract body movement details, updates the server's
     * state accordingly, and manages robot positioning or movement animations.
     *
     * @param json The JSON string containing body movement information, including
     *             client ID, coordinates, and other relevant data.
     */
    public void handleBodyMovement(String json) {
        Message<BodyMovement> message = JsonUtil.parseMessage(json, BodyMovement.class);
        BodyMovement body = message.messageBody();
        Server server = Server.getInstance();

        if (rebootingInProgress == body.clientID()) {
            rebootPosition = body;
            rebootingInProgress = -1;
        } else {
            final int newX = body.x();
            final int newY = body.y();
            final int robotID = server.getFigures().getByKey(server.getClients().getByValue(body.clientID()));

            //TODO display:
            // maybe clear board of robot, set robot at new position?
            // maybe use sendMessageSelf(new Message<>(new BodyAnimaton("Movement"))

        }
    }

    /****/
    public void handleBodyPlayerTurning(String json) {
        Message<BodyPlayerTurning> message = JsonUtil.parseMessage(json, BodyPlayerTurning.class);
        BodyPlayerTurning body = message.messageBody();
        Server server = Server.getInstance();
        final int robotID = server.getFigures().getByKey(server.getClients().getByValue(body.clientID()));

        //TODO display robot turning
        // maybe use sendMessageSelf(new Message<>(new BodyAnimaton("Turning"))

        switch (body.rotation()) {
//            case "clockwise" ->
//            case "counterclockwise" ->
            default -> System.err.println("Unknown rotation: " + body.rotation());
        }
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
    public void handleBodyAnimation(String json) {
        AnimationType animation = AnimationType.fromString(JsonUtil.parseMessage(json, BodyAnimation.class).messageBody().type());

        switch (animation) {
//            case MOVEMENT ->
            default -> sendMessageSelf(new Message<>(new BodyError("Animation error: " + animation.asString())));
        }

        //TODO display animations

    }

    /**
     * Handles the body reboot process by processing the provided JSON message.
     * Parses the JSON message to extract the body reboot information and determines
     * the reboot direction, sending a response message if the client ID matches.
     *
     * @param json the JSON string containing the body reboot information. The JSON
     *             is expected to represent a message with a body reboot payload.
     */
    public void handleBodyReboot(String json) {
        Message<BodyReboot> message = JsonUtil.parseMessage(json, BodyReboot.class);
        BodyReboot body = message.messageBody();
        rebootingInProgress = body.clientID();

        if (body.clientID().equals(ID)) {
            String rebootDirection = "top"; //default

            //TODO reboot direction selection
            // display rotation, best directly in/during selection
            // Sollte die Nachricht zur Ausrichtung nicht bis zum Ende der aktuellen Runde
            // angekommen sein, wird die Standardausrichtung verwendet.

            sendMessage(new Message<>(new BodyRebootDirection(rebootDirection)));
        }
    }

    /****/
    public void handleBodyRebootDirection(String json) {
        final String direction = JsonUtil.parseMessage(json, BodyRebootDirection.class).messageBody().direction();

        //TODO turn robot in correct direction ui logic
        // can also add cases to handleBodyTurnRobot

        sendMessageSelf(new Message<>(rebootPosition));
        rebootPosition = null;

        //TODO display reboot animation with already correct rotation

        sendMessageSelf(new Message<>(new BodyAnimation("Reboot")));

    }

    /****/
    public void handleBodyEnergy(String json) {
        Message<BodyEnergy> message = JsonUtil.parseMessage(json, BodyEnergy.class);
        BodyEnergy body = message.messageBody();

        energy.put(body.clientID(), body.count());

        //TODO optional: play energy animation depending on: id -> source

        //TODO update energy counter

    }

    /****/
    public void handleBodyCheckPointReached(String json) {
        BodyCheckPointReached body = JsonUtil.parseMessage(json, BodyCheckPointReached.class).messageBody();
        checkpointsReached.put(body.clientID(), body.number());

        //TODO display a option to see which checkpoints are reached by whom

    }

    /**
     * Handles the game finished event by processing the body of the message and determining
     * whether the client has won or lost.
     *
     * @param json the JSON string containing the game finished message, which includes details
     *             about the client ID and the game outcome
     */
    public void handleBodyGameFinished(String json) {
        BodyGameFinished body = JsonUtil.parseMessage(json, BodyGameFinished.class).messageBody();

        //TODO display win/lose

        if (Objects.equals(body.clientID(), ID)) {
            //play win screen
        } else {
            //play lose screen
        }
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
            writer.println(msg);
            writer.flush();
        } catch (Exception e) {
            System.err.println("Failed to send message: " + e.getMessage());
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
            System.err.println("Failed to serialize and send message: " + e.getMessage());
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
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
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

}
