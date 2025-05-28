package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitons.*;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client {

    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private final Gson gson = new Gson();
    private final String protocol = "Version 0.1";
    private volatile boolean isAI = false;

    /**
     * Establishes a connection to the server and initializes communication streams.
     * This method attempts to connect to the specified server and port, creates a socket,
     * and initializes the input and output streams for communication. It also starts a
     * separate thread to listen for incoming messages from the server. The method allows
     * the user to input messages via the console, which are then sent to the server.
     *
     * If an error occurs during the connection or while sending messages, the resources are
     * properly closed to ensure no leaks.
     *
     * @param host the hostname or IP address of the server to connect to
     * @param port the port number of the server to connect to
     */
    public void start(String host, int port) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            writer = new PrintWriter(socket.getOutputStream(), true);

            System.out.println("Connected to server.");

            // Start listening thread
            new Thread(this::listenForMessages).start();

            //TODO implement fx code to send messages/receive

            // Read user input and send messages
            // Example: Send chat message
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String userInput = scanner.nextLine();
                String[] command = userInput.trim().split(" ", 3);
                Message<BodySendChat> msg;
                //TODO maybe change command[1] to check in server map(username, id) or smth
                //rn only supports /w clientID msg, not /w username msg
                if (command[0].equalsIgnoreCase("/w") || command[0].equalsIgnoreCase("/whisper"))
                    msg = new Message<>(new BodySendChat(userInput, Integer.valueOf(command[1])));
                else
                    msg = new Message<>(new BodySendChat(userInput, -1));
                writer.println(gson.toJson(msg));
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            closeAll();
        }
    }

    /**
     * Listens for incoming messages from the server.
     * Continuously reads JSON-formatted messages from the server, determines the message type,
     * and delegates handling to the appropriate method based on the message type.
     * Supports different message types such as "HelloClient", "Welcome", and "ReceivedChat".
     * For unsupported or unknown message types, an {@code IllegalArgumentException} is thrown.
     *
     * If the connection is lost or an {@code IOException} occurs, the method terminates,
     * logs a disconnection message, and closes all resources using {@code closeAll()}.
     */
    private void listenForMessages() {
        try {
            String json;
            while ((json = reader.readLine()) != null) {
                String messageType = gson.fromJson(json, JsonObject.class).get("messageType").getAsString();
                switch (messageType) {
                    case "HelloClient" -> handleBodyHelloClient(json);
//                    case "Alive" -> handleBodyAlive(json);
                    case "Welcome" -> handleBodyWelcome(json);
//                    case "PlayerValues" -> handleBodyPlayerValues(json);
//                    case "PlayerAdded" -> handleBodyPlayerAdded(json);
//                    case "SetStatus" -> handleBodySetStatus(json);
//                    case "PlayerStatus" -> handleBodyPlayerStatus(json);
//                    case "SelectMap" -> handleBodySelectMap(json);
//                    case "MapSelected" -> handleBodyMapSelected(json);
//                    case "GameStarted" -> handleBodyGameStarted(json);
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
        System.out.println("Connected to server using protocol: " + protocol);

        writer.println(gson.toJson(new Message<BodyHelloServer>(new BodyHelloServer("Edle Eisbecher", isAI, this.protocol))));
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
        System.out.println("Your client ID: " + msg.messageBody().clientID());
    }

    /**
     * Processes a received chat message body parsed from a JSON string. If the message is private,
     * it is displayed with a "whispers to you" notation. Otherwise, it is displayed as a public message.
     *
     * @param json the JSON string containing the serialized BodyReceivedChat message
     */
    private void handleBodyReceivedChat(String json) {
        BodyReceivedChat body = JsonUtil.parseMessage(json, BodyReceivedChat.class).messageBody();
        if (body.isPrivate())
            System.out.println(body.from() + " whispers to you: " + body.message());
        System.out.println(body.from() + ": " + body.message());
    }

    /**
     * Releases resources associated with the client connection.
     *
     * This method ensures the proper closure of the input stream `reader`,
     * output stream `writer`, and the socket connection. It first checks if
     * each resource is non-null (or in the case of the socket, not already
     * closed), and closes them in sequence. If an error occurs during this
     * process, an error message is logged to the standard error stream.
     *
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

    /**
     * The main entry point of the application.
     * This method initiates the client program by creating a new {@link Client} instance
     * and starting a connection with the server using the specified hostname and port.
     *
     * @param args command-line arguments passed to the program. These are not used
     *             in this specific implementation.
     */
    public static void main(String[] args) {
        new Client().start("localhost", 12345);
    }
}
