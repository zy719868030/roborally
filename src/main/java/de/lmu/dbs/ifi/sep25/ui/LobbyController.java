package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySendChat;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySetStatus;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LobbyController {
    //GUI components for chat
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField chatInput;


    @FXML
    private ListView<PlayerEntry> playerList;

    @FXML
    private Button readyButton, notReadyButton;

    @FXML
    private Label statusLabel;

    private final ObservableList<PlayerEntry> players = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        playerList.setItems(players);
    }

    @FXML
    private void handleReady() {
        sendReadyStatus(true);
        statusLabel.setText("Du bist bereit.");
    }

    @FXML
    private void handleNotReady() {
        sendReadyStatus(false);
        statusLabel.setText("Du bist nicht bereit.");
    }

    private void sendReadyStatus(boolean ready) {
        Message<BodySetStatus> msg = new Message<>(new BodySetStatus(ready));
        ClientSingleton.getInstance().sendMessage(msg);
    }


    public void addPlayer(int clientID, String name, int figure, boolean ready) {
        players.add(new PlayerEntry(clientID, name, figure, ready));
    }


    public void updatePlayerStatus(int clientID, boolean ready) {
        for (PlayerEntry player : players) {
            if (player.getClientID() == clientID) {
                player.setReady(ready);
                playerList.refresh(); // GUI aktualisieren
                break;
            }
        }
    }

    //sends message to Server
    @FXML
    private void handleSendChat () {
        String msg = chatInput.getText();
        if (msg == null || msg.trim().isEmpty()) return;

        ClientSingleton.getInstance().sendMessage(
                new Message<>(new BodySendChat(msg, -1))
        );

        chatInput.clear();

    }
    public void appendChatMessage(String message) {
        chatArea.appendText(message + "\n");
    }

}


