package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySetStatus;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class LobbyController {

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
}
