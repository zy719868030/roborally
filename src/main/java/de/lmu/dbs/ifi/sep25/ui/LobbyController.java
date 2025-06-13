package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
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

        // Optional: Alle als Standardauswahl (oder Auswahl leer lassen)
        recipientBox.getSelectionModel().clearSelection();

        chatInput.setOnAction(e -> handleSendChat());

        // Initiale Button-Zustände
        readyButton.setDisable(false);
        notReadyButton.setDisable(true);
    }

    @FXML
    private void handleReady() {
        sendReadyStatus(true);
    }

    @FXML
    private void handleNotReady() {
        sendReadyStatus(false);
    }


    private void sendReadyStatus(boolean ready) {
        var client = ClientSingleton.getInstance();

        if (client == null) {
            System.err.println("[ERROR] ClientSingleton is null in sendReadyStatus");
            return;
        }

        Message<BodySetStatus> msg = new Message<>(new BodySetStatus(ready));
        client.sendMessage(msg);

        // GUI-Buttons je nach Status anpassen
        readyButton.setDisable(ready);        // Wenn bereit, "Bereit" ausgrauen
        notReadyButton.setDisable(!ready);    // Wenn bereit, "Nicht bereit" aktiv
        statusLabel.setText(ready ? "Du bist bereit." : "Du bist nicht bereit.");
    }


    public void addPlayer(int clientID, String name, int figure, boolean ready) {
        int myID = ClientSingleton.getInstance().getID();


        if (clientID == myID) {
            name = name + " (Du)";
        }

        PlayerEntry player = new PlayerEntry(clientID, name, figure, ready);
        players.add(player);

        if (clientID != myID) {
            recipientBox.getItems().add(player); // Nur andere Spieler als Empfänger
        }
    }



    public void updatePlayerStatus(int clientID, boolean ready) {
        Client client = ClientSingleton.getInstance();
        if (client == null) {
            System.err.println("[ERROR] ClientSingleton is null in updatePlayerStatus");
            return;
        }

        int myID = client.getID();

        for (PlayerEntry player : players) {
            if (player.getClientID() == clientID) {
                player.setReady(ready);

                // Wenn ich selbst gemeint bin, Buttons anpassen
                if (clientID == myID) {
                    updateOwnButtons(ready);
                }

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
            players.add(player);
            if (player.getClientID() != myID) {
                recipientBox.getItems().add(player);
            }
        }

        playerList.refresh();
    }


    public void appendChatMessage(String message) {
        chatArea.appendText(message + "\n");
    }
    public void updateOwnButtons(boolean isReady) {
        readyButton.setDisable(isReady);
        notReadyButton.setDisable(!isReady);
    }
}
