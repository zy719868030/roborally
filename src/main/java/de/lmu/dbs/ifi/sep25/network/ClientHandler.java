package de.lmu.dbs.ifi.sep25.network;

import de.lmu.dbs.ifi.sep25.network.MessageDefinitons.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable{

    // 1. Constants / configuration
    private final Gson gson = new Gson();

    // 2. Networking / I/O
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;

    // 3. State flags
    private volatile boolean alive = true;


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
    }

    /**
     * Executes the main logic for handling incoming messages from a client.
     * This method reads JSON-formatted messages from the client's input stream,
     * determines the message type, and delegates processing to the appropriate handler method.
     *
     * The method operates as follows:
     * - Reads JSON messages from the input stream using the `reader` object.
     * - Parses each message to extract the `messageType` field.
     * - Based on the `messageType`, forwards the message to specific handler methods
     *   (e.g., `handleBodyAlive`, `handleBodySendChat`, or `handleBodyReceivedChat`, among others).
     *
     * If an unrecognized message type is encountered, it throws an `IllegalArgumentException`
     * with a description of the unknown type.
     *
     * The method runs continuously within a loop until the input stream is closed or
     * an exception occurs. Any `IOException` during execution is caught and logged using
     * `printStackTrace` for debugging purposes.
     *
     * This method is invoked automatically when the thread associated with the
     * `ClientHandler` instance is executed.
     */
    @Override
    public void run() {
        try{
            String json;
            while ((json = reader.readLine()) != null) {
                String messageType = gson.fromJson(json, JsonObject.class).get("messageType").getAsString();
                switch (messageType) {
                    case "Alive" -> handleBodyAlive();
                    case "HelloServer" -> handleBodyHelloServer(json);
//                    case "PlayerValues" -> handleBodyPlayerValues(json);
//                    case "PlayerAdded" -> handleBodyPlayerAdded(json);
//                    case "SetStatus" -> handleBodySetStatus(json);
//                    case "PlayerStatus" -> handleBodyPlayerStatus(json);
//                    case "SelectMap" -> handleBodySelectMap(json);
//                    case "MapSelected" -> handleBodyMapSelected(json);
//                    case "GameStarted" -> handleBodyGameStarted(json);
                    case "SendChat" -> handleBodySendChat(json);
                    case "ReceivedChat" -> handleBodyReceivedChat(json);
                    case "Error" -> handleBodyError(json);
//                    case "PlayCard" -> handleBodyPlayCard(json);
//                    case "CardPlayed" -> handleBodyCardPlayed(json);
//                    case "CurrentPlayer" -> handleBodyCurrentPlayer(json);
//                    case "ActivePhase" -> handleBodyActivePhase(json);
//                    case "SetStartingPoint" -> handleBodySetStartingPoint(json);
//                    case "StartingPointTaken" -> handleBodyStartingPointTaken(json);
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
            }
        } catch (IOException e) {
            e.printStackTrace();
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
     *
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
        if (!body.protocol().equalsIgnoreCase(Server.getInstance().getProtocol())){
            sendMessage(new Message<>(new BodyError("Connection refused, protocol mismatch: " + body.protocol() + " != " + Server.getInstance().getProtocol())));
            closeAll();
        }
        Server.getInstance().getIsAI().put(this, body.isAI());
    }

    /**
     * Handles the processing of a "BodySendChat" message. This method parses the incoming JSON
     * to extract the message details and determines if the chat message should be broadcast
     * to all clients or sent to a specific recipient.
     *
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
        Integer from = Server.getInstance().getClients().getByKey(this);
        if (body.to() == -1)
            Server.getInstance().broadcastMessage(new Message<>(new BodyReceivedChat(body.message(), from, false)));
        else
            Server.getInstance().getClients().getByValue(body.to()).sendMessage(new Message<>(new BodyReceivedChat(body.message().split(" ", 3)[2], from, true)));
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
     * Validates the liveness state of the client connection and sends a "BodyAlive" message
     * to the client to indicate activity. This method performs the following actions:
     *
     * - Sets the {@code alive} flag to {@code false}, potentially signaling that the client's
     *   connection or activity needs to be verified.
     * - Sends a message containing a {@link BodyAlive} object to the client. The message is
     *   serialized using the {@link Message} wrapper and dispatched using the
     *   {@link #sendMessage(Message)} method.
     *
     * This method may be used for periodic health checks to ensure that the client is responsive
     * and able to receive and process messages properly.
     */
    public void checkLiveness(){
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
     * and sends it to the client. The method utilizes a JSON library to perform
     * serialization and delegates the actual transmission to the overloaded
     * {@link #sendMessage(String)} method.
     *
     * If serialization or transmission fails, the exception is caught and an
     * error message is printed to the error output stream.
     *
     * @param msg the {@link Message} object to be serialized and sent
     */
    public void sendMessage(Message<?> msg){
        try {
            String json = gson.toJson(msg);
            sendMessage(json);
        } catch (Exception e) {
            System.err.println("Failed to serialize and send message: " + e.getMessage());
        }
    }

    /**
     * Closes all resources associated with the client handler, including the client
     * socket, input/output streams, and the handler itself. This method ensures the
     * proper cleanup of resources to avoid potential resource leaks.
     *
     * The following actions are performed in sequence:
     * - Removes this client handler from the server's list of active client handlers.
     * - Closes the input stream (`reader`) if it is not null.
     * - Closes the output stream (`writer`) if it is not null.
     * - Closes the socket connection if it is not already closed.
     * - Sets the `alive` state of the client handler to `false` to indicate it is no longer active.
     * - Logs a message confirming the closure of the client connection.
     *
     * If an `IOException` occurs during the closing of any resource, it is handled and
     * an error message is logged.
     */
    public void closeAll() {
        try {
            Server.getInstance().removeClientHandler(this);
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
    public boolean isAlive(){
        return alive;
    }

}
