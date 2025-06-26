package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Map.entry;

public class GameController {
    // 0. Logger
    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");

    @FXML
    private GridPane gameBoardPane;
    @FXML
    private TextArea chatArea;
    @FXML
    private TextField chatInput;
    @FXML
    private Label statusLabel;
    @FXML
    private ComboBox<PlayerEntry> recipientBox;
    @FXML
    private VBox chatBox;
    @FXML
    private HBox iconMenu;
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

        final int columns = boardMap.size();
        final int rows = boardMap.getFirst().size();

        for (int x = 0; x < columns; x++) {
            for (int y = 0; y < rows; y++) {
                gameBoardPane.add(createTile(boardMap.get(x).get(y)), x, y);
            }
        }

        gameBoardPane.setVisible(true);
        gameBoardPane.setManaged(true);
        gameBoardPane.setPrefWidth(columns * TILE_SIZE);
        gameBoardPane.setPrefHeight(rows * TILE_SIZE);
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


    /**
     * Converts a given direction String to its corresponding rotation in degrees.
     *
     * @param dir the direction to be converted. Accepted values are "EAST", "SOUTH", "WEST". Any other value defaults to 0 degrees.
     * @return the rotation in degrees corresponding to the given direction. Returns 90 for "EAST", 180 for "SOUTH", 270 for "WEST", and 0 for undefined directions.
     */
    private double convertDirectionToRotation(String dir) {
        return switch (dir) {
            case "right" -> 90;
            case "bottom" -> 180;
            case "left" -> 270;
            default -> 0;
        };
    }

    /**
     * Erstellt ein einzelnes Feld basierend auf BoardElementen
     **/
    private StackPane createTile(List<MessageDefinitions.Field> elements) {
        StackPane pane = new StackPane();

        if (elements == null || elements.isEmpty()) {
            return pane; // Skip when empty pane
        }

        ImageView background = new ImageView(tileImages.get("Floor"));
        background.setFitWidth(TILE_SIZE);
        background.setFitHeight(TILE_SIZE);
        pane.getChildren().add(background);


        for (MessageDefinitions.Field element : elements) {
            switch (element.type()) {
                case "Wall" -> {
                    MessageDefinitions.FieldWall wall = (MessageDefinitions.FieldWall) element;
                    List<String> directions = wall.orientations();

                    // Alle Laser auf dem Feld sammeln
                    List<MessageDefinitions.FieldLaser> laserFields = elements.stream()
                            .filter(e -> e instanceof MessageDefinitions.FieldLaser)
                            .map(e -> (MessageDefinitions.FieldLaser) e)
                            .toList();

                    for (String dir : directions) {
                        // Standardbild
                        String imageKey = "wall_n";

                        for (MessageDefinitions.FieldLaser laser : laserFields) {
                            // Passt Richtung an
                            if (laser.orientations().getFirst().equals(dir)) {
                                int count = laser.count();
                                boolean active = laser.isActive(); // Laser ist an oder aus
                                imageKey = "wall_laser_" + count + "_" + (active ? "on" : "off");
                                break;
                            }
                        }

                        ImageView wallImg = new ImageView(tileImages.get(imageKey));
                        wallImg.setFitWidth(TILE_SIZE);
                        wallImg.setFitHeight(TILE_SIZE);
                        wallImg.setRotate(convertDirectionToRotation(dir));
                        pane.getChildren().add(wallImg);
                    }
                }

                case "ConveyorBelt" -> {
                    MessageDefinitions.FieldConveyorBelt belt = (MessageDefinitions.FieldConveyorBelt) element;
                    String color = belt.speed() == 2 ? "blue" : "green";

                    // Parse Direction enum aus Strings
                    Direction outDir = parseProtocolDirection(belt.directions().getFirst());
                    List<Direction> inDirs = belt.directions().stream()
                            .skip(1)
                            .map(this::parseProtocolDirection)
                            .toList();

                    String imageKey;
                    boolean isSplit = inDirs.stream()
                            .filter(in -> !in.turnAround().equals(outDir))
                            .count() >= 1;

                    if (isSplit) {
                        imageKey = color + "_" + getSplitImageSuffix(outDir, inDirs, color);
                    } else {
                        imageKey = color + "_conveyor_belt_straight";
                    }


                    Image image = tileImages.get(imageKey);
                    ImageView imgView = new ImageView(image);
                    imgView.setFitWidth(TILE_SIZE);
                    imgView.setFitHeight(TILE_SIZE);
                    imgView.setRotate(convertDirectionToRotation(outDir.toString().toLowerCase()));
                    pane.getChildren().add(imgView);
                }



                case "PushPanel" -> {
                    MessageDefinitions.FieldPushPanel pushPanel = (MessageDefinitions.FieldPushPanel) element;
                    String registerKey = pushPanel.registers().stream()
                            .map(String::valueOf)
                            .collect(Collectors.joining("_"));

                    Image baseImage = tileImages.get("PushPanel_" + registerKey);

                    if (baseImage == null) {
                        System.err.println("Fehlendes Bild: PushPanel_" + registerKey);
                        continue;
                    }

                    ImageView ppImg = new ImageView(baseImage);
                    ppImg.setFitWidth(TILE_SIZE);
                    ppImg.setFitHeight(TILE_SIZE);

                    ppImg.setRotate(convertDirectionToRotation(pushPanel.orientations().getFirst()));

                    pane.getChildren().add(ppImg);
                }

                case "RestartPoint" -> {
                    MessageDefinitions.FieldRestartPoint restartPoint = (MessageDefinitions.FieldRestartPoint) element;
                    Image baseImage = tileImages.get("reboot");

                    ImageView image = new ImageView(baseImage);
                    image.setFitWidth(TILE_SIZE);
                    image.setFitHeight(TILE_SIZE);

                    image.setRotate(convertDirectionToRotation(restartPoint.orientations().getFirst()));

                    pane.getChildren().add(image);
                }

                case "Antenna" -> {
                    MessageDefinitions.FieldAntenna antenna = (MessageDefinitions.FieldAntenna) element;
                    Image baseImage = tileImages.get("antenna");

                    ImageView image = new ImageView(baseImage);
                    image.setFitWidth(TILE_SIZE);
                    image.setFitHeight(TILE_SIZE);

                    image.setRotate(convertDirectionToRotation(antenna.orientations().getFirst()));

                    pane.getChildren().add(image);

                }

                default -> {
                    String key = getTileKeyForElement(element);
                    if (tileImages.containsKey(key)) {
                        ImageView overlay = new ImageView(tileImages.get(key));
                        overlay.setFitWidth(TILE_SIZE);
                        overlay.setFitHeight(TILE_SIZE);
                        pane.getChildren().add(overlay);
                    } else {
                        System.err.println("Fehlendes Bild: " + key);
                    }
                }
            }
        }

        pane.requestLayout();
        pane.requestFocus();

        return pane;
    }
    private boolean isOpposite(String a, String b) {
        return (a.equals("top") && b.equals("bottom")) ||
                (a.equals("bottom") && b.equals("top")) ||
                (a.equals("left") && b.equals("right")) ||
                (a.equals("right") && b.equals("left"));
    }

    private int getCornerRotation(String push, String pull) {
        return switch (push + "_" + pull) {
            case "top_right" -> 0;
            case "right_bottom" -> 90;
            case "bottom_left" -> 180;
            case "left_top" -> 270;
            case "right_top" -> 270;
            case "bottom_right" -> 0;
            case "left_bottom" -> 90;
            case "top_left" -> 180;
            default -> 0;
        };
    }

    private String getSplitImageKeyForDirection(String pushDir) {
        // Diese Methode gibt entweder "conveyor_split_left" oder "conveyor_split_right" zurück
        return switch (pushDir) {
            case "top" -> "conveyor_split_right";
            case "right" -> "conveyor_split_left";
            case "bottom" -> "conveyor_split_left";
            case "left" -> "conveyor_split_right";
            default -> "conveyor_split_left";
        };
    }
    private String getSplitImageSuffix(Direction outDir, List<Direction> inDirs, String color)
    {        if (inDirs.size() < 2) return (color + "_conveyor_belt_straight");
        // Standard-Gerade

        // Split: welcher Eingang ist NICHT gegenüber vom Ausgang?
        for (Direction inDir : inDirs) {
            if (!inDir.turnAround().equals(outDir)) {
                int relative = getRelativeTurn(outDir, inDir);
                return (relative == 1) ? "conveyor_split_right" : "conveyor_split_left";
            }
        }
        return "conveyor_belt_straight";
    }
    private Direction parseProtocolDirection(String dirString) {
        return switch (dirString.toLowerCase()) {
            case "top" -> Direction.NORTH;
            case "bottom" -> Direction.SOUTH;
            case "left" -> Direction.WEST;
            case "right" -> Direction.EAST;
            default -> throw new IllegalArgumentException("Ungültige Richtung: " + dirString);
        };
    }

    // 0 = gerade, 1 = rechts, -1 = links
    private int getRelativeTurn(Direction from, Direction to) {
        return switch (from) {
            case NORTH -> switch (to) {
                case EAST -> 1;
                case WEST -> -1;
                default -> 0;
            };
            case EAST -> switch (to) {
                case SOUTH -> 1;
                case NORTH -> -1;
                default -> 0;
            };
            case SOUTH -> switch (to) {
                case WEST -> 1;
                case EAST -> -1;
                default -> 0;
            };
            case WEST -> switch (to) {
                case NORTH -> 1;
                case SOUTH -> -1;
                default -> 0;
            };
        };
    }


    /**
     * Loads the tile images for various game elements and populates them into the `tileImages` map.
     * <p>
     * This method maps specific tile keys to corresponding image file paths stored in the assets folder.
     * It then loads these images and associates them with their respective keys.
     * Each image is retrieved from the resources of the application and added to the `tileImages` map using the key.
     * <p>
     * The mappings contain a variety of tile types such as floors, walls, gears, conveyor belts, lasers, checkpoints,
     * and starting points. This ensures that each tile key corresponds to a visually appropriate image for rendering
     * game elements on the board.
     * <p>
     * Exceptions may occur if an image file is missing or cannot be loaded from the `/assets/` resource directory.
     */
    private void loadTileImages() {
        final Map<String, String> imagePaths = Map.ofEntries(
                entry("Floor", "floor.png"),
                entry("wall_n", "wall_n.png"),
                entry("Gear_Green", "Gear_green.png"),
                entry("Gear_Red", "Gear_red.png"),
                entry("green_conveyor_belt_straight", "green_conveyor_belt_n.png"),
                entry("blue_conveyor_belt_straight", "blue_conveyor_belt_n.png"),
                entry("Laser_1_on", "wall_laser_1_on.png"),
                entry("wall_laser_1_on", "wall_laser_1_on.png"),
                entry("wall_laser_1_off", "wall_laser_1_off.png"),
                entry("wall_laser_2_on", "wall_laser_2.png"),
                entry("wall_laser_3_on", "wall_laser_3.png"),
                entry("Conveyor_green_Rotate", "green_conveyor_corner_r.png"),
                entry("PushPanel_1", "pushpanel_1.png"),
                entry("PushPanel_2", "pushpanel_2.png"),
                entry("PushPanel_3", "pushpanel_3.png"),
                entry("PushPanel_4", "pushpanel_4.png"),
                entry("PushPanel_5", "pushpanel_5.png"),
                entry("PushPanel_1_3_5", "pushpanel_1_3_5.png"),
                entry("PushPanel_2_4", "pushpanel_2_4.png"),
                entry("checkpoint1", "checkpoint1.png"),
                entry("checkpoint2", "checkpoint2.png"),
                entry("checkpoint3", "checkpoint3.png"),
                entry("checkpoint4", "checkpoint4.png"),
                entry("startpoint_1A", "startpoint_1A.png"),
                entry("startpoint_2A", "startpoint_2A.png"),
                entry("startpoint_4A", "startpoint_4A.png"),
                entry("startpoint_5B", "startpoint_5B.png"),
                entry("Pit", "Pit.png"),
                entry("energyspace_green", "energyspace_green.png"),
                entry("energyspace_red", "energyspace_red.png"),
                entry("reboot", "reboot.png"),
                entry("antenna", "antenna.png"),
                entry("black and white gears", "b&w_gear.png"),
                entry("blue_conveyor_corner", "blue_conveyor_corner_r.png"),
                entry("blue_conveyor_merge", "blue_conveyor_merge_triple.png"),
                entry("blue_conveyor_split_left", "blue_conveyor_split_left.png"),
                entry("blue_conveyor_split_right", "blue_conveyor_split_right.png")

        );

        imagePaths.forEach((key, file) ->
                tileImages.put(key, new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/" + file))))
        );
    }

    /**
     * Determines the tile key associated with a given game board field element.
     * This is used to specify the visual or functional representation for the element.
     *
     * @param element the field element for which the corresponding tile key is to be determined.
     *                The element's type determines the resulting key.
     * @return a string representing the tile key corresponding to the input field element.
     * Possible values include "Floor" for an empty field, "black and white gears" for a StartPoint,
     * "Pit" for a pit field, "energyspace" for an Energy-Space, "checkpointX" (with X as the count)
     * for a CheckPoint, "Gear_Green" or "Gear_Red" for gears depending on their orientation,
     * or "unknown" for undefined types.
     */
    private String getTileKeyForElement(MessageDefinitions.Field element) {
        switch (element.type()) {
            case "Empty":
                return "Floor";
            case "StartPoint":
                return "black and white gears";
            case "Pit":
                return "Pit";
            case "Energy-Space" :
                MessageDefinitions.FieldEnergySpace es = (MessageDefinitions.FieldEnergySpace) element;
                Integer count = es.getCount();
                return count != null && count > 0 ? "energyspace_green" : "energyspace_red";
            case "CheckPoint":
                return "checkpoint" + ((MessageDefinitions.FieldCheckPoint) element).count().toString();
            case "Gear":
                return ((MessageDefinitions.FieldGear) element).orientations().getFirst().equalsIgnoreCase("clockwise") ? "Gear_Green" : "Gear_Red";
            default:
                return "unknown";
        }
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

    @FXML
    private void toggleChatBox() {
        boolean currentlyVisible = chatBox.isVisible();
        chatBox.setVisible(!currentlyVisible);
        chatBox.setManaged(!currentlyVisible);
        iconMenu.setVisible(currentlyVisible);
        iconMenu.setManaged(currentlyVisible);
    }

    public void setInitialPlayerStats(Map<Integer, Integer> energy, Map<Integer, Integer> checkpointsReached) {
    }
}
