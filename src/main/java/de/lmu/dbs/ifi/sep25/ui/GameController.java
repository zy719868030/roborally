package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameController {
    @FXML private GridPane gameBoardPane;
    @FXML private TextArea chatArea;
    @FXML private TextField chatInput;
    @FXML private Label statusLabel;
    @FXML private ComboBox<PlayerEntry> recipientBox;
    private static final int TILE_SIZE = 60;

    private final Map<String, Image> tileImages = new HashMap<>();
    private Parent root;

    @FXML
    public void initialize() {
        loadTileImages();

        chatInput.setOnAction(e -> handleSendChat());
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
            case "Gear" ->
                    ((MessageDefinitions.FieldGear) element).orientations().getFirst().equalsIgnoreCase("clockwise") ? "Gear_green.png" : "Gear_red.png";
            //TODO add "Pit"
            case "Energy-Space" -> "energyspace.png";
            case "Wall" -> wallDirectionToFile((MessageDefinitions.FieldWall) element);
            case "Laser" ->
                    "laser_" + ((MessageDefinitions.FieldLaser) element).orientations().getFirst().toLowerCase() + ".png";
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

    public void setRoot(Parent root) {
        this.root = root;
    }

    public Parent getRoot() {
        return root;
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
            client.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySendChat(msg, recipientID)));
        } else {
            System.err.println("[ERROR] ClientSingleton is null in handleSendChat");
        }


        String prefix = (recipientID == -1) ? "Du" : "Du → " + selected.getName();
        appendChatMessage(prefix + ": " + msg);
        chatInput.clear();
    }

    public void appendChatMessage(String message) {
        chatArea.appendText(message + "\n");
    }
    public void updatePhase(String phaseName) {
    }

    public void setInitialPlayerStats(Map<Integer, Integer> energy, Map<Integer, Integer> checkpointsReached) {
    }
}
