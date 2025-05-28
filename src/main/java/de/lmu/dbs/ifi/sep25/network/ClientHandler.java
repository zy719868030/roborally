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

    private final Socket socket;
    private final Gson gson = new Gson();
    private final BufferedReader reader;
    private final PrintWriter writer;
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
     * Executes the main loop for handling incoming client messages. Reads JSON strings
     * from the input stream, parses the message type, and delegates handling of the message
     * to the appropriate method based on its type.
     *
     * The recognized message types are:
     * - "SendChat": Processes an outgoing chat message from the client.
     * - "ReceivedChat": Passes a received chat message to the client.
     *
     * For unknown message types, an {@link IllegalArgumentException} is thrown.
     *
     * This method continuously runs until the input stream is exhausted or an I/O error occurs.
     *
     * Exceptions:
     * - Catches {@link IOException} during stream reading and prints the stack trace.
     */
    @Override
    public void run() {
        try{
            String json;
            while ((json = reader.readLine()) != null) {
                String messageType = gson.fromJson(json, JsonObject.class).get("messageType").getAsString();
                //TODO implement the handle methods.
                switch (messageType) {
                    case "Alive" -> handleBodyAlive(json);
//                    case "HelloServer" -> handleBodyHelloServer(json);
//                    case "PlayerValues" -> handleBodyPlayerValues(json);
//                    case "PlayerAdded" -> handleBodyPlayerAdded(json);
//                    case "SetStatus" -> handleBodySetStatus(json);
//                    case "PlayerStatus" -> handleBodyPlayerStatus(json);
//                    case "SelectMap" -> handleBodySelectMap(json);
//                    case "MapSelected" -> handleBodyMapSelected(json);
//                    case "GameStarted" -> handleBodyGameStarted(json);
                    case "SendChat" -> handleBodySendChat(json);
                    case "ReceivedChat" -> handleBodyReceivedChat(json);
//                    case "Error" -> handleBodyError(json);
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
     * Handles the processing of a "BodyAlive" message.
     * This method sets the internal alive state to true, indicating that the client
     * connection is active. The provided JSON may optionally contain additional details
     * related to the "BodyAlive" message, but it is not explicitly processed in this method.
     *
     * @param json the JSON string representing the "BodyAlive" message
     *             received from the client
     */
    private void handleBodyAlive(String json) {
        alive = true;
    }

    /**
     * Handles the processing of a "SendChat" message body. This method parses the incoming JSON,
     * resolves the sender and recipient, and then either broadcasts the message or sends it
     * directly to the intended recipient based on the message details.
     *
     * @param json the raw JSON string representing a "SendChat" message containing
     *             the message content and recipient details
     */
    private void handleBodySendChat(String json) {
        Message<BodySendChat> message = JsonUtil.parseMessage(json, BodySendChat.class);
        BodySendChat body = message.messageBody();
        Integer from = Server.getInstance().getClients().getByKey(this);
        if (body.to() == -1)
            Server.getInstance().broadcastMessage(new Message<BodyReceivedChat>(new BodyReceivedChat(body.message(), from, false)));
        else
            Server.getInstance().getClients().getByValue(body.to()).sendMessage(new Message<BodyReceivedChat>(new BodyReceivedChat(body.message(), from, true)));
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
        sendMessage(new Message<BodyAlive>(new BodyAlive()));
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
     * Closes all resources associated with the client connection and removes the client
     * handler from the server's list of active clients. This includes closing the reader,
     * writer, and socket, along with any other operations required for cleanup.
     *
     * This method performs the following steps safely:
     * - Removes the client handler from the server using {@code Server.getInstance().removeClientHandler(this)}.
     * - Closes the input stream (reader) if it is not already closed.
     * - Closes the output stream (writer) if it is not already closed.
     * - Closes the client socket if it is open.
     * - Logs a message to indicate successful closure.
     *
     * In the event of an exception (e.g., {@link IOException}) during resource cleanup,
     * an error message is logged to the console.
     *
     * This method is designed to be idempotent, meaning it can be safely called multiple
     * times without causing additional issues.
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
