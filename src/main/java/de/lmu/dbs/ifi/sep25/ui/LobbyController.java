package de.lmu.dbs.ifi.sep25.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySendChat;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodySetStatus;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import de.lmu.dbs.ifi.sep25.utils.FieldDeserializer;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





public class LobbyController {
    @FXML
    private ComboBox<PlayerEntry> recipientBox;
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
    @FXML
    private Label mapLabel;
    @FXML
    private VBox mapSelectionBox;
    @FXML
    private ComboBox<String> mapChoiceBox;
    @FXML
    private Button selectMapButton;
    @FXML private ImageView mapPreviewImage;
    @FXML private StackPane mapPreviewContainer;
    @FXML private VBox chatBox;
    @FXML private VBox lobbyBox;
    @FXML private HBox iconMenu;
    @FXML private Button lobbyToggleButton;
    @FXML private Button chatToggleButton;
    @FXML private Button lobbyRestoreButton;
    @FXML private Button chatRestoreButton;



    private final ObservableList<PlayerEntry> players = FXCollections.observableArrayList();
    private Parent root;

    @FXML
    private void toggleLobbyBox() {
        if (lobbyBox.isVisible()) {
            slideOut(lobbyBox);
            lobbyToggleButton.setVisible(false);
            lobbyToggleButton.setManaged(false);
            lobbyRestoreButton.setVisible(true);
            lobbyRestoreButton.setManaged(true);
        } else {
            slideIn(lobbyBox);
            lobbyToggleButton.setVisible(true);
            lobbyToggleButton.setManaged(true);
            lobbyRestoreButton.setVisible(false);
            lobbyRestoreButton.setManaged(false);
        }
        updateIconMenuVisibility();
    }

    @FXML
    private void toggleChatBox() {
        if (chatBox.isVisible()) {
            slideOut(chatBox);
            chatToggleButton.setVisible(false);
            chatToggleButton.setManaged(false);
            chatRestoreButton.setVisible(true);
            chatRestoreButton.setManaged(true);
        } else {
            slideIn(chatBox);
            chatToggleButton.setVisible(true);
            chatToggleButton.setManaged(true);
            chatRestoreButton.setVisible(false);
            chatRestoreButton.setManaged(false);
        }
        updateIconMenuVisibility();
    }

    private void updateIconMenuVisibility() {
        boolean irgendwasMinimiert =
                chatRestoreButton.isVisible() || lobbyRestoreButton.isVisible();

        iconMenu.setVisible(irgendwasMinimiert);
        iconMenu.setManaged(irgendwasMinimiert);



    }






    @FXML
    public void initialize() {
        ControllerRegistry.setLobbyController(this);
        playerList.setItems(players);



        playerList.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(PlayerEntry player, boolean empty) {
                super.updateItem(player, empty);
                if (empty || player == null) {
                    setText(null);
                    setStyle(null);
                    getStyleClass().clear();
                } else {
                    setText(player.toString());
                    getStyleClass().clear();
                    getStyleClass().add("player-cell");
                    if (player.isReady()) {
                        getStyleClass().add("player-cell-ready");
                    } else {
                        getStyleClass().add("player-cell-unready");
                    }
                }
            }
        });

        //Combobox conf
        PlayerEntry alleOption = new PlayerEntry(-1, "Alle", -1, false);
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
        chatBox.setVisible(true);
        chatBox.setManaged(true);
        iconMenu.setVisible(false);
        iconMenu.setManaged(false);


// Listener für Kartenwechsel in der ComboBox
        mapChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldMap, newMap) -> {
            if (newMap != null) {
                mapLabel.setText("Ausgewählte Karte: " + newMap);
                loadMapPreview(newMap); // << NUR Bild laden
            }
        });

        //Add yourself to LobbyList
        Client client = ClientSingleton.getInstance();
        if (client != null) {
            client.flushPendingPlayers();
        }
        mapPreviewImage.fitWidthProperty().bind(mapPreviewContainer.widthProperty());
        mapPreviewImage.fitHeightProperty().bind(mapPreviewContainer.heightProperty());
    }

    private void loadMapPreview(String mapName) {
        String imageFile = "/assets/" + mapName.toLowerCase().replace(" ", "_") + ".png";
        InputStream imageStream = getClass().getResourceAsStream(imageFile);
        if (imageStream != null) {
            mapPreviewImage.setImage(new Image(imageStream));
            mapPreviewImage.setVisible(true);
            mapPreviewImage.setManaged(true);
        } else {
            System.err.println("[WARN] Kein Vorschaubild gefunden für: " + mapName);
            mapPreviewImage.setVisible(false);
            mapPreviewImage.setManaged(false);
        }
    }

    @FXML
    private void handleReady() {
        sendReadyStatus(true);
        statusLabel.setText("Du bist bereit.");
        updateReadyButtons(true);
        if (!lobbyBox.getStyleClass().contains("lobby-box-ready")) {
            lobbyBox.getStyleClass().add("lobby-box-ready");
        }

    }

    @FXML
    private void handleNotReady() {
        sendReadyStatus(false);
        statusLabel.setText("Du bist nicht bereit.");
        updateReadyButtons(false);
        lobbyBox.getStyleClass().remove("lobby-box-ready");

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
                    new Message<>(new MessageDefinitions.BodyMapSelected(selectedMap))
            );


        }
    }


    //Methode zum Verstecken, wenn Spieler z. B. unready wird
    public void hideMapSelection() {
        mapSelectionBox.setVisible(false);
        mapSelectionBox.setManaged(false);
    }

    public void setMapLabel(String text) {
        mapLabel.setText(text);

        // Lade zugehöriges Bild
        String mapName = text.replace("Ausgewählte Karte: ", "").trim();
        String imageFile = "/assets/maps/" + mapName.toLowerCase().replace(" ", "_") + ".png";

        InputStream imageStream = getClass().getResourceAsStream(imageFile);
        if (imageStream != null) {
            mapPreviewImage.setImage(new Image(imageStream));
            mapPreviewImage.setVisible(true);
            mapPreviewImage.setManaged(true);
        } else {
            System.err.println("[WARN] Kein Vorschaubild gefunden für: " + mapName);
            mapPreviewImage.setVisible(false);
            mapPreviewImage.setManaged(false);
        }
    }



    public void updatePlayerStatus(int clientID, boolean ready) {
        for (PlayerEntry player : players) {
            if (player.getClientID() == clientID) {
                player.setReady(ready);
                playerList.refresh();


                if (ClientSingleton.getInstance().getID() == clientID) {
                    boolean ichBinErsterReady = ready && binIchErsterReady(clientID);

                    // Nur der erste "Bereit"-Client darf die Karte auswählen
                    selectMapButton.setDisable(!ichBinErsterReady);
                    mapChoiceBox.setDisable(!ichBinErsterReady);
                }

                break;
            }
        }
    }

    private boolean binIchErsterReady(int clientID) {
        for (PlayerEntry player : players) {
            if (player.isReady() && player.getClientID() != clientID) {
                return false;
            }
        }
        return true;
    }


    @FXML
    private void handleSendChat() {
        String msg = chatInput.getText();
        if (msg == null || msg.trim().isEmpty()) return;

        PlayerEntry selected = recipientBox.getSelectionModel().getSelectedItem();
        int recipientID = (selected == null || selected.getClientID() == -1) ? -1 : selected.getClientID();

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


  /*  public void displaySelectedMap(String mapName) {
        // Diese Methode zeigt den ausgewählten Kartennamen in der Benutzeroberfläche an.
        // Zum Beispiel kann hier ein Label aktualisiert werden, das den Namen der Karte zeigt:
        mapLabel.setText("Ausgewählte Karte: " + mapName);
    } */


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


    public void renamePlayer(int clientID, String newName) { //@SEBAS
        for (PlayerEntry p : players) {
            if (p.getClientID() == clientID) {
                p.setName(newName + (clientID == ClientSingleton.getInstance().getID() ? " (du)" : ""));
                playerList.refresh();
                break;
            }

        }


    }
    public List<PlayerEntry> getPlayers() {
        return players.stream()
                .map(p -> new PlayerEntry(
                        p.getClientID(),
                        p.getName().replace(" (du)", ""), // falls notwendig
                        p.getFigure(),
                        p.isReady()
                ))
                .toList();
    }


    private void slideOut(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), node);
        tt.setToY(50);
        tt.setOnFinished(e -> {
            node.setVisible(false);
            node.setManaged(false);
        });
        tt.play();
    }

    private void slideIn(Node node) {
        node.setVisible(true);
        node.setManaged(true);
        TranslateTransition tt = new TranslateTransition(Duration.millis(200), node);
        tt.setFromY(50);
        tt.setToY(0);
        tt.play();
    }

    public void setRoot(Parent root) {
        this.root = root;
    }

    public Parent getRoot() {
        return root;
    }
}
