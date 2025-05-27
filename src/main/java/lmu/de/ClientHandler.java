package lmu.de;

import lmu.de.MessageDefinitons.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable{

    private final Socket socket;
    private final Gson gson = new Gson();
    private BufferedReader reader;
    private PrintWriter writer;


    /**Constructor
     * @param socket
     * **/
    public ClientHandler(Socket socket) throws IOException {
        this.socket = socket;
        BufferedReader reader = new BufferedReader(new java.io.InputStreamReader(socket.getInputStream()));
        PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
    }


    /**Main method in client handler. Checks different message types and calls corresponding handler method.
     * No deserialization yet. Add more cases with more message types.
     * **/
    @Override
    public void run() {
        try{
            String json;
            while ((json = reader.readLine()) != null) {
                JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
                String messageType = obj.get("messageType").getAsString();

                //TODO implement the handle methods.

                switch (messageType) {
//                    case "Alive" -> handleBodyAlive(json);
//                    case "HelloServer" -> handleBodyHelloServer(json);
//                    case "Welcome" -> handleBodyWelcome(json);
//                    case "PlayerValues" -> handleBodyPlayerValues(json);
//                    case "PlayerAdded" -> handleBodyPlayerAdded(json);
//                    case "SetStatus" -> handleBodySetStatus(json);
//                    case "PlayerStatus" -> handleBodyPlayerStatus(json);
//                    case "SelectMap" -> handleBodySelectMap(json);
//                    case "MapSelected" -> handleBodyMapSelected(json);
//                    case "GameStarted" -> handleBodyGameStarted(json);
//                    case "SendChat" -> handleBodySendChat(json);
//                    case "ReceivedChat" -> handleBodyReceivedChat(json);
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

    /**Sends a message to the corresponding client
     * takes a json as input and sends it as json to client
     * @param message
     * **/
    public void sendMessage(String message) throws IOException {
        writer.println(message);
    }


    /**Properly closes everything and "leaves" server
     * @param reader
     * @param socket
     * @param writer
     * **/
    public void closeAll(Socket socket, BufferedReader reader, PrintWriter writer) {
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
            System.err.println("Error closing client resources: " + e.getMessage());
        }
    }

}
