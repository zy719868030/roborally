package lmu.de;

import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import lmu.de.MessageDefinitons.*;

import com.google.gson.Gson;

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
            Scanner scanner = new Scanner(System.in);
            while (scanner.hasNextLine()) {
                String userInput = scanner.nextLine();
                // Example: Send chat message
                Message<BodySendChat> msg = new Message<>(new BodySendChat(userInput, -1));
                String json = gson.toJson(msg);
                writer.println(json);
            }

        } catch (IOException e) {
            System.err.println("Connection error: " + e.getMessage());
            closeEverything();
        }
    }

    private void listenForMessages() {
        try {
            String json;
            while ((json = reader.readLine()) != null) {
                JsonObject obj = gson.fromJson(json, JsonObject.class);
                String messageType = obj.get("messageType").getAsString();

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
            System.err.println("Disconnected from server.");
            closeEverything();
        }
    }

    private void handleBodyHelloClient(String json) {
        Message<BodyHelloClient> message = gson.fromJson(json, new TypeToken<Message<BodyHelloClient>>() {}.getType());

        String protocol = message.messageBody().protocol();
        System.out.println("Connected to server using protocol: " + protocol);

        writer.println(gson.toJson(new Message<BodyHelloServer>(new BodyHelloServer("Edle Eisbecher", false, this.protocol))));
    }

    private void handleBodyWelcome(String json) {
        Message<BodyWelcome> msg = gson.fromJson(json, new TypeToken<Message<BodyWelcome>>(){}.getType());
        System.out.println("Your client ID: " + msg.messageBody().clientID());
    }

    private void closeEverything() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("Error closing client: " + e.getMessage());
        }
    }
    public static void main(String[] args) {
        new Client().start("localhost", 12345);
    }
}
