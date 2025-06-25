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

        // Initialisiere EmpfängerBox
        PlayerEntry alleOption = new PlayerEntry(-1, "Alle", -1, false);
        recipientBox.getItems().add(alleOption);
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
    public void addPlayer(int clientID, String name, int figure, boolean ready) {
        if (ClientSingleton.getInstance().getID() == clientID) return; // Sich selbst nicht hinzufügen

        boolean exists = recipientBox.getItems().stream()
                .anyMatch(p -> p.getClientID() == clientID);

        if (!exists) {
            recipientBox.getItems().add(new PlayerEntry(clientID, name, figure, ready));
        }
    }


    public void setPlayersFromLobby(List<PlayerEntry> players) {
        for (PlayerEntry p : players) {
            addPlayer(p.getClientID(), p.getName(), p.getFigure(), p.isReady());
        }
    }
    // Erstellt ein einzelnes Feld basierend auf BoardElementen
    private StackPane createTile(List<MessageDefinitions.Field> elements) {
        StackPane pane = new StackPane();

        ImageView background = new ImageView(tileImages.get("Floor"));
        background.setFitWidth(TILE_SIZE);
        background.setFitHeight(TILE_SIZE);
        pane.getChildren().add(background);

        for (MessageDefinitions.Field element : elements) {

            // Wall – 1 Bild, mehrfach rotiert
            if (element instanceof MessageDefinitions.FieldWall wall) {
                for (String dir : wall.orientations()) {
                    ImageView wallImg = new ImageView(tileImages.get("Wall_N"));
                    wallImg.setFitWidth(TILE_SIZE);
                    wallImg.setFitHeight(TILE_SIZE);

                    wallImg.setRotate(switch (dir) {
                        case "EAST" -> 90;
                        case "SOUTH" -> 180;
                        case "WEST" -> 270;
                        default -> 0;
                    });

                    pane.getChildren().add(wallImg);
                }
                continue;
            }

            // Laser – 1 Bild (laser_north), mehrfach rotiert
            if (element instanceof MessageDefinitions.FieldLaser laser) {
                ImageView laserImg = new ImageView(tileImages.get("Laser_N"));
                laserImg.setFitWidth(TILE_SIZE);
                laserImg.setFitHeight(TILE_SIZE);

                String orientation = laser.orientations().getFirst(); // z. B. "EAST"
                laserImg.setRotate(switch (orientation) {
                    case "EAST" -> 90;
                    case "SOUTH" -> 180;
                    case "WEST" -> 270;
                    default -> 0;
                });

                pane.getChildren().add(laserImg);
                continue;
            }

            //ConveyorBelt – 1 Basisbild pro Speed (slow/fast), dann Rotation
            if (element instanceof MessageDefinitions.FieldConveyorBelt belt) {
                String speed = belt.speed() == 2 ? "Fast" : "Slow";
                ImageView beltImg = new ImageView(tileImages.get("Belt_" + speed + "_N"));
                beltImg.setFitWidth(TILE_SIZE);
                beltImg.setFitHeight(TILE_SIZE);

                String dir = belt.directions().getFirst(); // "NORTH", etc.
                beltImg.setRotate(switch (dir) {
                    case "EAST" -> 90;
                    case "SOUTH" -> 180;
                    case "WEST" -> 270;
                    default -> 0;
                });

                pane.getChildren().add(beltImg);
                continue;
            }
            // PushPanel – 1 Bild, je nach Register-Kombination, rotiert nach Richtung
            if (element instanceof MessageDefinitions.FieldPushPanel pp) {
                // Register: z. B. [1, 3, 5] → "1_3_5"
                List<String> registerStrings = pp.registers().stream()
                        .map(String::valueOf)
                        .toList(); // Falls Java <16, .collect(Collectors.toList())
                String registerKey = String.join("_", registerStrings); // z. B. "1_3_5"

                Image baseImage = tileImages.get("PushPanel_" + registerKey);
                if (baseImage != null) {
                    ImageView ppImg = new ImageView(baseImage);
                    ppImg.setFitWidth(TILE_SIZE);
                    ppImg.setFitHeight(TILE_SIZE);

                    String dir = pp.orientations().getFirst(); // NORTH, EAST, ...
                    ppImg.setRotate(switch (dir) {
                        case "EAST" -> 90;
                        case "SOUTH" -> 180;
                        case "WEST" -> 270;
                        default -> 0;
                    });

                    pane.getChildren().add(ppImg);
                }
                continue;
            }


            // Standard-Handling für alle anderen (z. B. Gear, Pit, etc.)
            String key = getTileKeyForElement(element);
            if (tileImages.containsKey(key)) {
                ImageView overlay = new ImageView(tileImages.get(key));
                overlay.setFitWidth(TILE_SIZE);
                overlay.setFitHeight(TILE_SIZE);
                pane.getChildren().add(overlay);
            }
        }

        return pane;
    }


    private void loadTileImages() {
        tileImages.put("Floor", new Image(getClass().getResourceAsStream("/assets/floor.png")));
        tileImages.put("Wall_N", new Image(getClass().getResourceAsStream("/assets/wall_n.png")));
        tileImages.put("Gear_Green", new Image(getClass().getResourceAsStream("/assets/Gear_green.png")));
        tileImages.put("Gear_Red", new Image(getClass().getResourceAsStream("/assets/Gear_red.png")));
        tileImages.put("Conveyor_green_NORTH", new Image(getClass().getResourceAsStream("/assets/green_conveyor_belt_n.png")));
        tileImages.put("Laser_N", new Image(getClass().getResourceAsStream("/assets/laser_n.png")));
        tileImages.put("Conveyor_green_Rotate", new Image(getClass().getResourceAsStream("/assets/green_conveyor_rotate_n.png")));
        tileImages.put("PushPanel_1", new Image(getClass().getResourceAsStream("/assets/pushpanel_1.png")));
        tileImages.put("PushPanel_2", new Image(getClass().getResourceAsStream("/assets/pushpanel_2.png")));
        tileImages.put("PushPanel_3", new Image(getClass().getResourceAsStream("/assets/pushpanel_3.png")));
        tileImages.put("PushPanel_4", new Image(getClass().getResourceAsStream("/assets/pushpanel_4.png")));
        tileImages.put("PushPanel_5", new Image(getClass().getResourceAsStream("/assets/pushpanel_5.png")));
        tileImages.put("PushPanel_1_3_5", new Image(getClass().getResourceAsStream("/assets/pushpanel_1_3_5.png")));
        tileImages.put("PushPanel_2_4", new Image(getClass().getResourceAsStream("/assets/pushpanel_2_4.png")));


    }

    private String getTileKeyForElement(MessageDefinitions.Field element) {
        String type = element.type();

        return switch (type) {
            case "Empty" -> "Floor"; // Hintergrund
            case "StartPoint" -> "startpoint";
            case "Pit" -> "pit";
            case "RestartPoint" -> "reboot";
            case "Energy-Space" -> "energyspace";
            case "CheckPoint" -> "checkpoint" + ((MessageDefinitions.FieldCheckPoint) element).count();
            case "Antenna" -> "antenna";

            case "Gear" -> {
                String dir = ((MessageDefinitions.FieldGear) element).orientations().getFirst().toLowerCase();
                yield dir.equals("clockwise") ? "Gear_Green" : "Gear_Red";
            }

            // Wände, Laser, Belts, PushPanels: Rotation → Sonderbehandlung in createTile()
            case "Wall", "Laser", "ConveyorBelt", "PushPanel" -> null;

            default -> "unknown";
        };
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
