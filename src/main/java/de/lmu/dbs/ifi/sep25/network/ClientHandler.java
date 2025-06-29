package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Game;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.utils.FieldDeserializer;
import de.lmu.dbs.ifi.sep25.utils.FieldSerializer;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


/**
 * The ClientHandler class is responsible for managing communication
 * between the server and a specific connected client. Each instance
 * of this class runs on its own thread and listens for incoming
 * messages from the client, processes them, and sends responses or
 * broadcasts as appropriate.
 * <p>
 * The class supports a range of message types, each of which is handled
 * by a specific method. It ensures the integrity of communication by
 * adhering to the server protocol and allows clients to perform
 * operations such as sending messages, setting their status, and more.
 * <p>
 * This class is intended to be used within the context of a server that
 * supports multiple connected clients and provides mechanisms for
 * broadcasting messages, client lifecycle management, and error handling.
 */
public class ClientHandler implements Runnable {

    // 1. Constants / configuration
    private final Gson gson = new GsonBuilder()
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldDeserializer())
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldSerializer())
            .create();

    private Integer myID;

    // 2. Networking / I/O
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final Server server;

    // 3. State flags
    private volatile boolean alive = true;
    private volatile boolean mapSelecting = false;
    private CountDownLatch placementLatch;

    // 4. Game connection
    private Player player;
    private Game game;


    /**
     * Constructs a new ClientHandler to handle communication with a specific
     * client socket. Initializes input and output streams for data exchange.
     *
     * @param socket the client socket representing the connection
     *               between the server and client
     * @throws IOException if an I/O error occurs when creating input
     *                     or output stream objects
     */
    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        this.reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
        this.writer = new PrintWriter(socket.getOutputStream(), true);
        this.server = Server.getInstance();
    }

    public void setGame(Game game) {
        System.out.println("[DEBUG] setGame() aufgerufen für ClientHandler ID: " + myID);
        this.game = game;
    }



    /**
     * Executes the main logic for handling incoming messages from a client.
     * This method reads JSON-formatted messages from the client's input stream,
     * determines the message type, and delegates processing to the appropriate handler method.
     * <p>
     * The method operates as follows:
     * - Reads JSON messages from the input stream using the `reader` object.
     * - Parses each message to extract the `messageType` field.
     * - Based on the `messageType`, forwards the message to specific handler methods
     * (e.g., `handleBodyAlive`, `handleBodySendChat`, or `handleBodyReceivedChat`, among others).
     * <p>
     * If an unrecognized message type is encountered, it throws an `IllegalArgumentException`
     * with a description of the unknown type.
     * <p>
     * The method runs continuously within a loop until the input stream is closed or
     * an exception occurs. Any `IOException` during execution is caught and logged using
     * `printStackTrace` for debugging purposes.
     * <p>
     * This method is invoked automatically when the thread associated with the
     * `ClientHandler` instance is executed.
     */
    @Override
    public void run() {
        try {
            String json;
            while ((json = reader.readLine()) != null) {
                String messageType = JsonUtil.parseUnknown(json).messageType();
                switch (messageType) {
                    case "Alive" -> handleBodyAlive();
                    case "HelloServer" -> handleBodyHelloServer(json);
                    case "PlayerValues" -> handleBodyPlayerValues(json);
                    case "SetStatus" -> handleBodySetStatus(json);
                    case "MapSelected" -> handleBodyMapSelected(json);
                    case "SendChat" -> handleBodySendChat(json);
                    case "ReceivedChat" -> handleBodyReceivedChat(json);
                    case "Error" -> handleBodyError(json);
                    case "PlayCard" -> handleBodyPlayCard(json);
                    case "SetStartingPoint" -> handleBodySetStartingPoint(json);
                    case "SelectedCard" -> handleBodySelectedCard(json);
                    case "TimerStarted" -> handleBodyTimerStarted();
                    case "RebootDirection" -> handleBodyRebootDirection(json);
                    default -> throw new IllegalArgumentException("Unknown messageType: " + messageType);
                }
            }
        } catch (IOException e) {
            sendMessage(new Message<>(new BodyError("Client connection failed or closed unexpectedly: " + e.getMessage())));
            closeAll();
        }
    }

    /**
     * Handles the processing of errors related to message bodies. This method is used to
     * forward the error message, represented as a JSON string, for further transmission
     * or handling via the {@link #sendMessage(String)} method.
     *
     * @param json the JSON string representing the error details of the message body
     */
    private void handleBodyError(String json) {
        sendMessage(json);
        closeAll();
    }

    /**
     * Handles the "BodyAlive" operation by updating the state of the client handler
     * to mark the connection as active. This is typically invoked upon receiving a
     * "BodyAlive" message from the client, serving as a heartbeat to ensure that
     * the connection remains valid and responsive.
     * <p>
     * This method sets the {@code alive} flag to {@code true}, indicating that the
     * client is active and reachable. It does not return a value and operates
     * directly on the {@code alive} field of the {@code ClientHandler} instance.
     */
    private void handleBodyAlive() {
        alive = true;
    }


    /**
     * Handles the processing of a "HelloServer" message body. This method parses the incoming JSON,
     * validates the protocol against the server's protocol, and updates the AI status for the client
     * connection. If the protocol does not match the server's protocol, the connection is refused,
     * an error message is sent, and the connection is terminated.
     *
     * @param json the raw JSON string representing a "BodyHelloServer" message containing
     *             protocol details and AI status
     */
    private void handleBodyHelloServer(String json) {
        Message<BodyHelloServer> message = JsonUtil.parseMessage(json, BodyHelloServer.class);
        BodyHelloServer body = message.messageBody();
        if (!body.protocol().equalsIgnoreCase(server.getProtocol())) {
            sendMessage(new Message<>(new BodyError("Connection refused, protocol mismatch: " + body.protocol() + " != " + server.getProtocol())));
            closeAll();
        }
        server.getIsAI().put(this, body.isAI());
    }

    /**
     * Processes incoming JSON data representing player values and updates the server state
     * based on the extracted information. If the player's figure assignment is successful,
     * the player is added to the server lobby, and a broadcast message is sent to notify
     * all clients about the new player.
     *
     * @param json the JSON string representing a "BodyPlayerValues" message, containing
     *             the player's name and selected figure
     */
    private void handleBodyPlayerValues(String json) {
        Message<BodyPlayerValues> message = JsonUtil.parseMessage(json, BodyPlayerValues.class);
        BodyPlayerValues body = message.messageBody();

        if (server.assignFigure(body.figure(), this)) {
            final String name = server.generateUniqueName(body.name());
            this.player = new Player(name, body.figure(), this);
            this.myID = server.getClients().getByKey(this);

            server.getNames().put(this, name);
            server.addToLobby(this);

            sendMessage(new Message<>(new BodyWelcome(myID)));

            broadcastMessage(new Message<>(new BodyPlayerAdded(myID, name, body.figure())));

            // notify all
            for (ClientHandler other : server.getLobby().getClients()) {
                if (other == this) continue;
                Integer otherID = server.getClients().getByKey(other);
                String otherName = server.getNames().get(other);
                Integer otherFigure = server.getFigures().getByKey(other);

                if (otherID != null && otherName != null && otherFigure != null) {
                    sendMessage(new Message<>(new BodyPlayerAdded(otherID, otherName, otherFigure)));
                }
            }

        } else {
            sendMessage(new Message<>(new BodyError("Figure already selected.")));
        }
    }

    /**
     * Handles the processing of a "BodySetStatus" message. This method parses the incoming
     * JSON to determine whether the associated player is ready and updates the server
     * and client states accordingly. It also manages readiness logic for AI players and
     * broadcasts status updates to all clients.
     *
     * @param json the raw JSON string representing a "BodySetStatus" message containing
     *             the readiness status of the player
     */
    private void handleBodySetStatus(String json) {
        Message<BodySetStatus> message = JsonUtil.parseMessage(json, BodySetStatus.class);
        boolean ready = message.messageBody().ready();

        if (player == null) {
            System.err.println("[ERROR] Player is null in handleBodySetStatus (clientID: " + myID + ")");
            sendMessage(new Message<>(new BodyError("Cannot change ready state: Player not initialized.")));
            return;
        }

        player.setReady(ready);
        broadcastMessage(new Message<>(new BodyPlayerStatus(myID, ready)));

        if (!Boolean.TRUE.equals(server.getIsAI().get(this))) {
            if (ready) {
                if (server.readyIsEmpty() && server.getGame() == null && !server.isMapSelectionOngoing()) {
                    server.setMapSelectionOngoing(true);
                    setMapSelecting(true);
                    sendMessage(new Message<>(new BodySelectMap(server.getAvailableMaps())));
                }
                server.markReady(this);
            } else
                server.unmarkReady(this);
        }
    }

    /**
     * Handles the processing of a "BodyMapSelected" message. This method parses the incoming
     * JSON string into a {@link Message} object containing a {@link BodyMapSelected} message body.
     * It then broadcasts the selected map information to all connected clients.
     *
     * @param json the JSON string representing a "BodyMapSelected" message containing
     *             the selected map details
     */
    private void handleBodyMapSelected(String json) {
        Message<BodyMapSelected> message = JsonUtil.parseMessage(json, BodyMapSelected.class);
        String map = message.messageBody().map();

        broadcastMessage(new Message<>(new BodyMapSelected(map)));
        server.setMapSelectionOngoing(false);
        setMapSelecting(false);
        server.newGame(map);
    }

    /**
     * Handles the processing of a "BodySendChat" message. This method parses the incoming JSON
     * to extract the message details and determines if the chat message should be broadcast
     * to all clients or sent to a specific recipient.
     * <p>
     * The following actions are performed:
     * - Parses the JSON string into a Message object containing a BodySendChat instance.
     * - Retrieves the sender's ID from the server's client mapping.
     * - If the target is set to `-1`, broadcasts the message to all clients.
     * - Otherwise, sends the message to the specified recipient.
     *
     * @param json the raw JSON string representing a "BodySendChat" message
     */
    private void handleBodySendChat(String json) {
        Message<BodySendChat> message = JsonUtil.parseMessage(json, BodySendChat.class);
        BodySendChat body = message.messageBody();

        if (myID == null) {
            sendMessage(new Message<>(new BodyError("Sender ID not found.")));
            return;
        }

        if (body.to() == -1) {
            server.broadcastMessage(
                    new Message<>(new BodyReceivedChat(body.message(), myID, false))
            );
        } else {
            ClientHandler recipient = server.getClients().getByValue(body.to());
            if (recipient != null) {
                recipient.sendMessage(
                        new Message<>(new BodyReceivedChat(body.message(), myID, true))
                );
            } else {
                sendMessage(new Message<>(new BodyError("Recipient not found.")));
            }
        }
    }

    /**
     * Handles the processing of a received chat message in JSON format.
     * Delegates the JSON string to the {@link #sendMessage(String)} method for further transmission.
     *
     * @param json the JSON string representing the received chat message
     */
    private void handleBodyReceivedChat(String json) {
        this.sendMessage(json);
    }

    /**
     * Handles the processing of a "BodyPlayCard" message. This method parses the incoming
     * JSON string into a {@link Message} object containing a {@link BodyPlayCard} message body.
     * It then broadcasts a message indicating that a card has been played to all connected
     * clients.
     *
     * @param json the JSON string representing a "BodyPlayCard" message which contains
     *             the details of the played card
     */
    private void handleBodyPlayCard(String json) {
        Message<BodyPlayCard> message = JsonUtil.parseMessage(json, BodyPlayCard.class);
        broadcastMessage(new Message<>(new BodyCardPlayed(myID, message.messageBody().card())));
    }

    /**
     * Handles the process of setting a robot's starting position on the game board.
     * Validates the provided starting point coordinates and updates the robot's position if valid.
     * Sends appropriate messages based on the validation result.
     *
     * @param json the JSON string containing data to set the starting position,
     *             parsed into a {@code BodySetStartingPoint} object which provides the coordinates (x, y).
     */
    private void handleBodySetStartingPoint(String json) {
        BodySetStartingPoint body = JsonUtil.parseMessage(json, BodySetStartingPoint.class).messageBody();

        if (!game.setPlayerStartingPosition(player, body.x(), body.y())) {
            //TODO maybe send a message for reselection
        } else if (placementLatch != null) {
            placementLatch.countDown();
        }
    }

    /**
     * Handles the processing of a body-selected card event. Calls chooseCard in player.
     *
     * @param json the JSON string representation of the BodySelectedCard object
     */
    private void handleBodySelectedCard(String json) {
        BodySelectedCard body = JsonUtil.parseMessage(json, BodySelectedCard.class).messageBody();
        //notify is in player class
        player.chooseCard(body.card(), body.register());
    }

    /**
     * Handles the start of the body timer by scheduling a task to execute
     * after a fixed delay of 30 seconds. When the timer ends, it retrieves
     * the list of ready players from the server and broadcasts a
     * BodyTimerEnded message containing this list.
     * <p>
     * This method uses a single-threaded scheduled executor service to perform
     * the delayed task execution. The task is responsible for broadcasting
     * a message via the method `broadcastMessage`.
     */
    private void handleBodyTimerStarted() {
        final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            List<Integer> readyRegister = server.getReadyRegister();
            broadcastMessage(new Message<>(new BodyTimerEnded(readyRegister)));
            //TODO @Lukas add code to call random selection for remaining players in readyRegister

        }, 30, TimeUnit.SECONDS);
    }

    /**
     * Handles the body reboot direction specified in the given JSON message.
     * Parses the direction from the message, updates the robot's direction,
     * and broadcasts the new direction information.
     *
     * @param json the JSON message containing the body reboot direction
     */
    private void handleBodyRebootDirection(String json) {
        String direction = JsonUtil.parseMessage(json, BodyRebootDirection.class).messageBody().direction();
        player.getRobot().setDirection(Direction.fromString(direction));
        broadcastMessage(new Message<>(new BodyRebootDirection(direction)), this);
    }


    // -------------

    /**
     * Sets the placement latch to the specified CountDownLatch instance.
     *
     * @param latch the CountDownLatch instance to be assigned to the placement latch
     */
    public void setPlacementLatch(CountDownLatch latch) {
        this.placementLatch = latch;
    }

    /**
     * Marks the entity associated with this instance as ready to register
     * in the server and broadcasts a message indicating completion of the
     * selection process.
     * <p>
     * This method performs the following actions:
     * 1. Marks the entity identified by `myID` as ready for registration
     * using the server instance.
     * 2. Broadcasts a message to notify other entities about the completion
     * of the selection process for this instance.
     * <p>
     * Utilizes the `server` instance for registration marking and the
     * `broadcastMessage` method to send out a notification message.
     */
    public void setReadyRegister() {
        server.markReadyRegister(myID);
        broadcastMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySelectionFinished(myID)));
    }

    /**
     * Validates the liveness state of the client connection and sends a "BodyAlive" message
     * to the client to indicate activity. This method performs the following actions:
     * <p>
     * - Sets the {@code alive} flag to {@code false}, potentially signaling that the client's
     * connection or activity needs to be verified.
     * - Sends a message containing a {@link BodyAlive} object to the client. The message is
     * serialized using the {@link Message} wrapper and dispatched using the
     * {@link #sendMessage(Message)} method.
     * <p>
     * This method may be used for periodic health checks to ensure that the client is responsive
     * and able to receive and process messages properly.
     */
    public void checkLiveness() {
        alive = false;
        sendMessage(new Message<>(new BodyAlive()));
    }

    /**
     * Sends a message to the client through the established connection.
     * This method writes the provided message to the output stream
     * and ensures it is flushed for immediate transmission.
     *
     * @param message the text message to be sent to the connected client
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
     * Serializes a given {@link Message} object to its JSON string representation
     * and sends it to the client. The method uses a JSON library to perform
     * serialization and delegates the actual transmission to the overloaded
     * {@link #sendMessage(String)} method.
     * <p>
     * If serialization or transmission fails, the exception is caught and an
     * error message is printed to the error output stream.
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
     * Sends the given message to all connected clients via the server.
     *
     * @param msg the message to be broadcasted to all connected clients
     */
    public void broadcastMessage(Message<?> msg) {
        server.broadcastMessage(msg);
    }

    /**
     * Sends the specified message to all connected clients except the excluded client.
     *
     * @param msg     the message to be broadcasted to connected clients
     * @param exclude the client handler to be excluded from receiving the message
     */
    public void broadcastMessage(Message<?> msg, ClientHandler exclude) {
        server.broadcastMessage(msg, exclude);
    }

    /**
     * Closes all resources associated with the client connection managed by this
     * {@code ClientHandler}. This includes removing the client handler from the
     * server's management, closing the input/output streams, and terminating
     * the socket connection, if applicable.
     *
     * <p>This method performs the following actions:
     * <ul>
     *   <li>Removes the current client handler instance from the server's client handler list.</li>
     *   <li>Closes the input stream reader if it is not null.</li>
     *   <li>Closes the output stream writer if it is not null.</li>
     *   <li>Closes the socket connection if it is open and not already closed.</li>
     *   <li>Sets the {@code alive} flag to {@code false}, marking the client handler as inactive.</li>
     *   <li>Outputs a log statement indicating the connection closure.</li>
     * </ul>
     *
     * <p>Any {@code IOException} that occurs during the resource cleanup process is caught and
     * logged to the error stream. The connection cleanup process continues for other resources
     * even if an exception occurs.
     */
    public void closeAll() {
        try {
            server.removeClientHandler(this);
            if (reader != null)
                reader.close();
            if (writer != null)
                writer.close();
            if (socket != null && !socket.isClosed())
                socket.close();
            alive = false;
            System.out.println("Closed connection for client handler.");
        } catch (IOException e) {
            System.err.println("Error closing resources for client: " + e.getMessage());
        }
    }

    /**
     * Checks whether the client connection is currently active.
     *
     * @return {@code true} if the client connection is alive, {@code false} otherwise
     */
    public boolean isAlive() {
        return alive;
    }

    /**
     * Retrieves the {@link Player} associated with this client handler.
     *
     * @return the {@link Player} object representing the player linked to the client connection
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Retrieves the ID associated with the current instance.
     *
     * @return the ID of the current instance
     */
    public int getMyID() {
        return myID;
    }

    public boolean isMapSelecting() {
        return mapSelecting;
    }

    public void setMapSelecting(boolean mapSelecting) {
        this.mapSelecting = mapSelecting;
    }
}

