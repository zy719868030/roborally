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
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;





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
    @FXML private GridPane gameBoardPane;


    private Parent root;


    private final ObservableList<PlayerEntry> players = FXCollections.observableArrayList();

    private static final int TILE_SIZE = 60;

    @FXML
    public void initialize() {
        ControllerRegistry.setLobbyController(this);
        playerList.setItems(players);
        loadTileImages();



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


    // Zeichnet das Spielfeld anhand der BoardMap
    public void drawBoard(List<List<List<MessageDefinitions.Field>>> boardMap) {
        gameBoardPane.getChildren().clear();

        for (int x = 0; x < boardMap.size(); x++) {
            List<List<MessageDefinitions.Field>> col = boardMap.get(x);
            for (int y = 0; y < col.size(); y++) {
                List<MessageDefinitions.Field> elements = col.get(y);

                StackPane tile = createTile(elements); // ← nutze deine gute Methode
                gameBoardPane.add(tile, x, y);
            }
        }

        gameBoardPane.setVisible(true);
        gameBoardPane.setManaged(true);
    }




    // Erstellt ein einzelnes Feld basierend auf BoardElementen
    private StackPane createTile(List<MessageDefinitions.Field> elements) {
        StackPane pane = new StackPane();

        ImageView background = new ImageView(tileImages.get("Floor"));
        background.setFitWidth(60);
        background.setFitHeight(60);
        pane.getChildren().add(background);

        for (MessageDefinitions.Field element : elements) {
            String key = getTileKeyForElement(element);
            if (tileImages.containsKey(key)) {
                ImageView overlay = new ImageView(tileImages.get(key));
                overlay.setFitWidth(60);
                overlay.setFitHeight(60);
                pane.getChildren().add(overlay);
            }
        }

        return pane;
    }
    private Map<String, Image> tileImages = new HashMap<>();

    private void loadTileImages() {
        tileImages.put("Floor", new Image(getClass().getResourceAsStream("/assets/floor.png")));
        tileImages.put("Wall_N", new Image(getClass().getResourceAsStream("/assets/wall_n.png")));
        tileImages.put("Wall_O", new Image(getClass().getResourceAsStream("/assets/wall_o.png")));
        tileImages.put("Wall_S", new Image(getClass().getResourceAsStream("/assets/wall_s.png")));
        tileImages.put("Gear_Green", new Image(getClass().getResourceAsStream("/assets/Gear_green.png")));
        tileImages.put("Gear_Red", new Image(getClass().getResourceAsStream("/assets/Gear_red.png")));
//        tileImages.put("Conveyor_green_NORTH", new Image(getClass().getResourceAsStream("/assets/green_conveyor_belt.png")));
       // tileImages.put("Conveyor_blue_EAST_rot", new Image(getClass().getResourceAsStream("/assets/conveyor_b_e_rot.png")));


    }
    private String getTileKeyForElement(MessageDefinitions.Field element) {
        String type = element.getType(); // z. B. "Wall", "Laser", "Gear", "Floor", etc.
        return switch (type) {
            case "Empty" -> "floor.png";
            case "StartPoint" -> "startpoint.png";
            case "ConveyorBelt" -> beltToFile((MessageDefinitions.FieldConveyorBelt) element);
            //TODO add "PushPanel" ->
            case "Gear" -> ((MessageDefinitions.FieldGear) element).orientations().getFirst().equalsIgnoreCase("clockwise") ? "Gear_green.png" : "Gear_red.png";
            //TODO add "Pit"
            case "Energy-Space" -> "energyspace.png";
            case "Wall" -> wallDirectionToFile((MessageDefinitions.FieldWall) element);
            case "Laser" -> "laser_" + ((MessageDefinitions.FieldLaser) element).orientations().getFirst().toLowerCase() + ".png";
            case "Antenna" -> "antenne.png";
            case "CheckPoint" -> "checkpoint" + ((MessageDefinitions.FieldCheckpoint) element).count() + ".png";
            case "RestartPoint" -> "reboot.png";
            default -> "unknown.png";
        };

    }

    private String wallDirectionToFile(MessageDefinitions.FieldWall wall) {
        return "wall_" + wall.orientations().getFirst().toLowerCase() + ".png";
        //TODO bitte ändern: mehrere wall orientations möglich!
    }

    private String beltToFile(MessageDefinitions.FieldConveyorBelt belt) {
        return (belt.speed() == 2 ? "belt_fast_" : "belt_slow_")
                + belt.directions().getFirst().toLowerCase() + ".png";
    }

    public void handleGameStarted(List<List<List<MessageDefinitions.Field>>> boardMap) {
        System.out.println("[DEBUG] Game gestartet – Board wird gezeichnet.");
        drawBoard(boardMap);
    }
    public void loadAndDisplayPreviewMap(String mapName) {
        String fileName = mapName.toLowerCase().replace(" ", "_") + ".json";
        try (InputStream is = getClass().getResourceAsStream("/maps/" + fileName)) {
            if (is == null) {
                System.err.println("[ERROR] Map-Datei nicht gefunden: " + mapName);
                return;
            }

            // GSON oder Jackson für Typ: List<List<List<BoardElement>>>
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(MessageDefinitions.Field.class, new FieldDeserializer())
                    .setPrettyPrinting()
                    .create();
            Type mapType = new TypeToken<List<List<List<BoardElement>>>>() {}.getType();
            List<List<List<MessageDefinitions.Field>>> mapData = gson.fromJson(new InputStreamReader(is), mapType);

            drawBoard(mapData); // nutzt deine bestehende Methode
        } catch (IOException e) {
            System.err.println("[ERROR] Fehler beim Laden der Map: " + e.getMessage());
        }
    }



}
