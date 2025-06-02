package de.lmu.dbs.ifi.sep25.ui;

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

    private ObservableList<PlayerEntry> players = FXCollections.observableArrayList();

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
        String json = String.format("""
            {
              "messageType": "SetStatus",
              "messageBody": {
                "ready": %s
              }
            }
            """, ready);

       // NetworkService.send(json);
    }

    // Diese Methode kann von außen aufgerufen werden, um Spieler hinzuzufügen
    public void addPlayer(int clientID, String name, int figure, boolean ready) {
        players.add(new PlayerEntry(clientID, name, figure, ready));
    }

    // Diese Methode kann genutzt werden, um Statusänderungen zu verarbeiten
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
