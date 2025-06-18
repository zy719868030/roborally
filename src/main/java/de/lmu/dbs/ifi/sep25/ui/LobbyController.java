package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySendChat;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySetStatus;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;

import java.util.List;
import java.util.Optional;

public class LobbyController {
    @FXML private ComboBox<PlayerEntry> recipientBox;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private ListView<PlayerEntry> playerList;
    @FXML private Button readyButton, notReadyButton;
    @FXML private Label statusLabel;
    @FXML private Label mapLabel;
    @FXML private HBox mapSelectionBox;
    @FXML private ComboBox<String> mapChoiceBox;
    @FXML private Button selectMapButton;

    private Parent root;


    private final ObservableList<PlayerEntry> players = FXCollections.observableArrayList();


    @FXML
    public void initialize() {
        ControllerRegistry.setLobbyController(this);
        playerList.setItems(players);


        playerList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(PlayerEntry player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) {
                    setText(null);
                } else {
                    setText(player.toString());
                }
            }
        });
        //Combobox conf
        PlayerEntry alleOption = new PlayerEntry(-1,"Alle",-1,false);
        recipientBox.getItems().add(alleOption);
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


        recipientBox.getSelectionModel().clearSelection();
        chatInput.setOnAction(e -> handleSendChat());
        updateReadyButtons(false);

        //Add yourself to LobbyList
        Client client = ClientSingleton.getInstance();
        if (client != null) {
            client.flushPendingPlayers();
        }
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
        int myID = ClientSingleton.getInstance().getID();
        boolean isMe = (clientID == myID);

        String displayName = isMe ? name + " (du)" : name;

        PlayerEntry player = new PlayerEntry(clientID, displayName, figure, ready);

        // Vermeide Duplikate
        boolean alreadyExists = players.stream()
                .anyMatch(p -> p.getClientID() == clientID);
        if (!alreadyExists) {
            players.add(player);
        }

        // Vermeide Duplikate in ComboBox
        if (!isMe) {
            recipientBox.getItems().add(player);
        }

        System.out.println("[DEBUG] Spieler hinzugefügt: " + displayName);
    }
    public void showMapSelection(List<String> maps) {
        if (mapChoiceBox == null || mapSelectionBox == null) {
            System.err.println("[ERROR] mapChoiceBox oder mapSelectionBox ist null in showMapSelection!");
            return;
        }

        if (maps == null || maps.isEmpty()) {
            System.err.println("[WARN] showMapSelection mit leerer Map-Liste aufgerufen.");
            return;
        }

        mapChoiceBox.getItems().setAll(maps);
        mapChoiceBox.getSelectionModel().selectFirst();
        mapSelectionBox.setVisible(true);
        mapSelectionBox.setManaged(true);
    }

    @FXML
    private void handleMapSelection() {
        String selectedMap = mapChoiceBox.getValue();
        if (selectedMap != null && !selectedMap.isBlank()) {
            ClientSingleton.getInstance().sendMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyMapSelected(selectedMap)
                    )
            );
            // Optional: UI wieder verstecken
            mapSelectionBox.setVisible(false);
            mapSelectionBox.setManaged(false);
        }
    }
    //Methode zum Verstecken, wenn Spieler z. B. unready wird
    public void hideMapSelection() {
        mapSelectionBox.setVisible(false);
        mapSelectionBox.setManaged(false);
    }
    public void setMapLabel(String text) {
        mapLabel.setText(text);
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
        int recipientID = (selected == null|| selected.getClientID() == -1) ? -1 : selected.getClientID();

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
            String displayName = player.getName();
            players.add(player); // ListView

            if (player.getClientID() != myID) {
                recipientBox.getItems().add(player);
            }
        }

        playerList.refresh();
    }


    //new


    public void displaySelectedMap(String mapName) {
        // Diese Methode zeigt den ausgewählten Kartennamen in der Benutzeroberfläche an.
        // Zum Beispiel kann hier ein Label aktualisiert werden, das den Namen der Karte zeigt:
         mapLabel.setText("Ausgewählte Karte: " + mapName);
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
        readyButton.setManaged(!isReady);
        notReadyButton.setVisible(isReady);
        notReadyButton.setManaged(!isReady);
    }
    public void setRoot(Parent root) {
        this.root = root;
    }
    public Parent getRoot() {
        return root;
    }

    public void setSelectedMap(String mapName) {
        mapLabel.setText("Gewählte Karte: " + mapName);
    }
}
