package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySendChat;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySetStatus;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class LobbyController {
    @FXML private ComboBox<PlayerEntry> recipientBox;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private ListView<PlayerEntry> playerList;
    @FXML private Button readyButton, notReadyButton;
    @FXML private Label statusLabel;

    private final ObservableList<PlayerEntry> players = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        ControllerRegistry.setLobbyController(this);
        playerList.setItems(players);

        // ComboBox: Darstellung und Zellen setzen
        recipientBox.setPromptText("An...");
        recipientBox.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(PlayerEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? "Alle" : item.getName());
            }
        });
        recipientBox.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(PlayerEntry item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : item.getName());
            }
        });

        // Optional: Alle als Standardauswahl
        recipientBox.getSelectionModel().clearSelection();

        chatInput.setOnAction(e -> handleSendChat());
        updateReadyButtons(false); // Spieler ist anfangs nicht bereit

    }

    @FXML
    private void handleReady() {
        sendReadyStatus(true);
        statusLabel.setText("Du bist bereit.");
        updateReadyButtons(true);
    }

    @FXML
    private void handleNotReady() {
        sendReadyStatus(false);
        statusLabel.setText("Du bist nicht bereit.");
        updateReadyButtons(false);
    }

    private void sendReadyStatus(boolean ready) {
        Message<BodySetStatus> msg = new Message<>(new BodySetStatus(ready));
        var client = ClientSingleton.getInstance();

        if (client != null) {
            client.sendMessage(msg);
        } else {
            System.err.println("[ERROR] ClientSingleton is null in sendReadyStatus");
        }


}

    public void addPlayer(int clientID, String name, int figure, boolean ready) {
        PlayerEntry player = new PlayerEntry(clientID, name, figure, ready);
        players.add(player);
        int myID = ClientSingleton.getInstance().getID();
        if (clientID != myID) {
            recipientBox.getItems().add(player); // nur andere Spieler
        }
        playerList.refresh();
    }


    public void updatePlayerStatus(int clientID, boolean ready) {
        for (PlayerEntry player : players) {
            if (player.getClientID() == clientID) {
                player.setReady(ready);
                playerList.refresh();
                break;
            }
        }
    }

    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText();
        if (msg == null || msg.trim().isEmpty()) return;

        PlayerEntry selected = recipientBox.getSelectionModel().getSelectedItem();
        int recipientID = (selected != null) ? selected.getClientID() : -1;

        System.out.println("[DEBUG] Sende Chatnachricht an " + (recipientID == -1 ? "ALLE" : recipientID) + ": " + msg);

        var client = ClientSingleton.getInstance();
        if (client != null) {
            client.sendMessage(new Message<>(new BodySendChat(msg, recipientID)));
        } else {
            System.err.println("[ERROR] ClientSingleton is null in handleSendChat");
        }



        String prefix = (recipientID == -1) ? "Du" : "Du → " + selected.getName();
        appendChatMessage(prefix + ": " + msg);
        chatInput.clear();
    }
    @FXML
    public void updatePlayerList(java.util.List<PlayerEntry> newPlayers) {
        players.clear();
        recipientBox.getItems().clear();

        var client = ClientSingleton.getInstance();
        if (client == null) {
            System.err.println("[ERROR] ClientSingleton is null in addPlayer");
            return;
        }
        int myID = client.getID();

        for (PlayerEntry player : newPlayers) {
            players.add(player); // ListView

            if (player.getClientID() != myID) {
                recipientBox.getItems().add(player); //
            }
        }

        playerList.refresh();
    }
    @FXML
    private void handleClearRecipient() {
        recipientBox.getSelectionModel().clearSelection();
        System.out.println("[DEBUG] Chat-Empfänger zurückgesetzt auf 'Alle'");
    }


    public void appendChatMessage(String message) {
        chatArea.appendText(message + "\n");
    }
    private void updateReadyButtons(boolean isReady) {
        readyButton.setVisible(!isReady);
        notReadyButton.setVisible(isReady);
    }

}
