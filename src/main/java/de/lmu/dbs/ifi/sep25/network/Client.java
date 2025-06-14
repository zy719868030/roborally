package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.ui.ControllerRegistry;
import de.lmu.dbs.ifi.sep25.ui.LobbyController;
import de.lmu.dbs.ifi.sep25.ui.LoginController;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.Scanner;

public class Client {
    // 1. Constants / configuration
    private final Gson gson = new Gson();
    private final String protocol = "Version 0.1";
    private final ConcurrentBidirectionalMap<Integer, String> usernames = new ConcurrentBidirectionalMap<>();

    // 2. Main identity/data
    private int figure;
    private Integer ID;
    private volatile boolean isAI = false;

    // 3. Networking / I/O
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;

    /**
     * Establishes a connection to a server and initializes the necessary input and output streams
     * for communication. The method also starts a thread that listens for messages from the server.
     *
     * @param host the server hostname or IP address to connect to
     * @param port the port number on the server to connect to
     */
    public void start(String host, int port) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("[SERVER] Connected to server.");

            // Start listening thread
            new Thread(this::listenForMessages).start();

            // CLI chat input (optional, usually used for testing)
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String userInput = scanner.nextLine();
                if (userInput.trim().isEmpty()) continue;
                String[] command = userInput.trim().split(" ", 3);
                if (userInput.equalsIgnoreCase("/help") || userInput.equalsIgnoreCase("/h") || userInput.equalsIgnoreCase("/commands") || userInput.equalsIgnoreCase("/cmds")) {
                    System.out.println("Available commands:");
                    System.out.println("/help                   - Show this help message");
                    System.out.println("/whisper username msg   - Send a private message to a client");
                    System.out.println("/exit                   - Exit the client");
                    // Add more commands as needed
                } else if (userInput.equalsIgnoreCase("/exit")) {
                    closeAll();
                } else if ((command[0].equalsIgnoreCase("/w") || command[0].equalsIgnoreCase("/whisper")) && command.length == 3) {
                    if (usernames.containsValue(command[1])) {
                        sendMessage(new Message<>(new BodySendChat(userInput, Integer.valueOf(command[1]))));
                    } else {
                        System.err.println("User not found");
                    }
                } else {
                    sendMessage(gson.toJson(new Message<>(new BodySendChat(userInput, -1))));
                }
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            closeAll();
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
                    System.out.println("[DEBUG] Received: " + json); // important
                    String messageType = JsonUtil.parseUnknown(json).messageType();
                    switch (messageType) {
                        case "HelloClient" -> handleBodyHelloClient(json);
                        case "Alive" -> handleBodyAlive(json);
                        case "Welcome" -> handleBodyWelcome(json);
                        case "PlayerAdded" -> handleBodyPlayerAdded(json);
                        case "PlayerStatus" -> handleBodyPlayerStatus(json);
                        case "SelectMap" -> handleBodySelectMap(json);
                        case "MapSelected" -> handleBodyMapSelected(json);
                        case "GameStarted" -> handleBodyGameStarted(json);
                        case "ReceivedChat" -> handleBodyReceivedChat(json);
                        case "Error" -> handleBodyError(json);
                        //                    case "PlayCard" -> handleBodyPlayCard(json); TODO add to game logic: sends to server card was played
                        case "CardPlayed" -> handleBodyCardPlayed(json);
                        case "CurrentPlayer" -> handleBodyCurrentPlayer(json);
                        //                    case "ActivePhase" -> handleBodyActivePhase(json);
                        case "StartingPointTaken" -> handleBodyStartingPointTaken(json);
                        //                    case "YourCards" -> handleBodyYourCards(json);
                        //                    case "NotYourCards" -> handleBodyNotYourCards(json);
                        //                    case "ShuffleCoding" -> handleBodyShuffleCoding(json);
                        //                    case "SelectedCard" -> handleBodySelectedCard(json);
                        //                    case "CardSelected" -> handleBodyCardSelected(json);
                        //                    case "SelectionFinished" -> handleBodySelectionFinished(json);
                        //                    case "TimerStarted" -> handleBodyTimerStarted(json);
                        //                    case "TimerEnded" -> handleBodyTimerEnded(json);
                        //                    case "CardsYouGotNow" -> handleBodyCardsYouGotNow(json);
                        //                    case "CurrentCards" -> handleBodyCurrentCards(json);
                        //                    case "ReplaceCard" -> handleBodyReplaceCard(json);
                        //                    case "Movement" -> handleBodyMovement(json);
                        //                    case "PlayerTurning" -> handleBodyPlayerTurning(json);
                        //                    case "Animation" -> handleBodyAnimation(json);
                        //                    case "Reboot" -> handleBodyReboot(json);
                        //                    case "RebootDirection" -> handleBodyRebootDirection(json);
                        //                    case "Energy" -> handleBodyEnergy(json);
                        //                    case "CheckPointReached" -> handleBodyCheckPointReached(json);
                        //                    case "GameFinished" -> handleBodyGameFinished(json);
                        default -> throw new IllegalArgumentException("Unknown messageType: " + messageType);
                    }
                } catch (Exception e) {
                    System.err.println("[ERROR] Error handling message: " + e.getMessage());
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

        sendMessage(gson.toJson(new Message<>(new BodyHelloServer("Edle Eisbecher", isAI, this.protocol))));
    }

    /**
     * Handles the "BodyAlive" message received from the server.
     * This method writes the provided JSON string directly to the server output stream.
     *
     * @param json the JSON string containing the serialized BodyAlive message
     */
    private void handleBodyAlive(String json) {
        //test
        System.out.println("[DEBUG] Alive empfangen und beantwortet");
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
        System.out.println("[SERVER] Your client ID: " + msg.messageBody().clientID());
    }

    /**
     * Handles a "BodyPlayerAdded" message received from the server.
     * <p>
     * This method processes a JSON string representing a {@code BodyPlayerAdded} message,
     * deserializing it to extract the player's proposed username, client ID, and figure.
     * If a conflict is detected in the proposed username (i.e., it already exists in the
     * system), the method generates a unique username by appending a numeric suffix.
     * Finally, the resolved username is added to the {@code usernames} map along with
     * the client ID, and the player's figure is stored for further use.
     *
     * @param json the JSON string containing the serialized {@code BodyPlayerAdded} message
     */
    private void handleBodyPlayerAdded(String json) {
        Message<BodyPlayerAdded> message = JsonUtil.parseMessage(json, BodyPlayerAdded.class);
        BodyPlayerAdded body = message.messageBody();
        String proposedName = body.name();
        String username;

        if (usernames.containsValue(proposedName)) {
            int i = 1;
            do {
                username = proposedName + " #" + i;
                i++;
            } while (usernames.containsValue(username));
        } else {
            username = proposedName;
        }

        usernames.put(body.clientID(), username);
        figure = body.figure();

        // FINAL variables for lambda use
        final String finalUsername = username;
        final int finalFigure = figure;
        final int clientID = body.clientID();

        javafx.application.Platform.runLater(() -> {
            LobbyController lobbyCtrl = ControllerRegistry.getLobbyController();
            if (lobbyCtrl != null) {
                boolean isReady = false;
                lobbyCtrl.addPlayer(clientID, finalUsername, finalFigure, isReady);
            }

            if (clientID == getID()) {
                LoginController loginCtrl = ControllerRegistry.getLoginController();
                if (loginCtrl != null) {
                    loginCtrl.loginSuccess(finalUsername, finalFigure);
                }
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
        String selection = "Dizzy Highway"; // TODO: Replace with actual map selection from GUI

        sendMessage(gson.toJson(new Message<>(new BodyMapSelected(selection))));
    }

    /****/
    private void handleBodyMapSelected(String json) {
        //TODO implement fx display of selected map
    }

    /****/
    private void handleBodyGameStarted(String json) {
        Message<BodyGameStarted> message = JsonUtil.parseMessage(json, BodyGameStarted.class);
        BodyGameStarted body = message.messageBody();
        List<List<List<BoardElement>>> board = body.gameMap();

        // TODO: Display game board in GUI
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
        //TODO implement java fx code: display played card to client
    }

    /****/
    private void handleBodyCurrentPlayer(String json) {
        // TODO
        //  - check if equals sent id
        //  - check for game phase
        // set player turn maybe?
    }



    private void handleBodyStartingPointTaken(String json) {
        //TODO fx display robot
    }


    /**
     * Sends a text message through the output stream to the connected server or client.
     * This method attempts to write the message using a PrintWriter instance.
     * If an error occurs during the process, it logs the failure message to the error stream.
     *
     * @param message the text message to be sent
     */
    public void sendMessage(String message) {
        try {
            writer.println(message);
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
}
