package de.lmu.dbs.ifi.sep25.ui;

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
                    // TODO add different corner and passageway wall assets with lasers etc. or find a different solution. Can only display single walls or laser on a single wall rn.
                    for (String dir : ((MessageDefinitions.FieldWall) element).orientations()) {
                        ImageView wallImg = new ImageView(tileImages.get("Wall_N"));
                        wallImg.setFitWidth(TILE_SIZE);
                        wallImg.setFitHeight(TILE_SIZE);

                        wallImg.setRotate(convertDirectionToRotation(dir));

                        pane.getChildren().add(wallImg);
                    }
                }

                case "Laser" -> {
                    MessageDefinitions.FieldLaser laser = (MessageDefinitions.FieldLaser) element;

                    //TODO add the laser off assets
                    ImageView laserImg = new ImageView(tileImages.get(switch (laser.count()) {
                        case 1 -> "Laser_1";
                        case 2 -> "Laser_2";
                        default -> "Laser_3";
                    }));

                    laserImg.setFitWidth(TILE_SIZE);
                    laserImg.setFitHeight(TILE_SIZE);

                    laserImg.setRotate(convertDirectionToRotation(laser.orientations().getFirst()) + 90);

                    pane.getChildren().add(laserImg);
                }

                case "ConveyorBelt" -> {
                    MessageDefinitions.FieldConveyorBelt belt = (MessageDefinitions.FieldConveyorBelt) element;
                    Image image = tileImages.get(belt.speed() == 2 ? "Conveyor_Fast_N" : "Conveyor_Slow_N");

                    //TODO after adding blue_conveyor_intersection, the blue conveyor intersections, add to the check by checking for direction size etc.!
                    ImageView beltImg = new ImageView(image);
                    beltImg.setFitWidth(TILE_SIZE);
                    beltImg.setFitHeight(TILE_SIZE);

                    beltImg.setRotate(convertDirectionToRotation(belt.directions().getFirst()));

                    pane.getChildren().add(beltImg);
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
                entry("Wall_N", "wall_n.png"),
                entry("Gear_Green", "Gear_green.png"),
                entry("Gear_Red", "Gear_red.png"),
                entry("Conveyor_Slow_N", "green_conveyor_belt_n.png"),
                entry("Conveyor_Fast_N", "blue_conveyor_belt_n.png"),
                entry("Laser_1", "Board_Lasers_1.png"),
                entry("Laser_2", "Board_Lasers_2.png"),
                entry("Laser_3", "Board_Lasers_3.png"),
                entry("Conveyor_green_Rotate", "green_conveyor_rotate_n.png"),
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
                entry("energyspace", "energyspace.png"),
                entry("reboot", "reboot.png"),
                entry("antenna", "antenna.png"),
                entry("black and white gears", "b&w_gear.png")
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
        return switch (element.type()) {
            case "Empty" -> "Floor"; // Hintergrund
            case "StartPoint" -> "black and white gears";
            case "Pit" -> "Pit";
            case "Energy-Space" -> "energyspace";
            case "CheckPoint" -> "checkpoint" + ((MessageDefinitions.FieldCheckPoint) element).count().toString();
            case "Gear" ->
                    ((MessageDefinitions.FieldGear) element).orientations().getFirst().equalsIgnoreCase("clockwise") ? "Gear_Green" : "Gear_Red";
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
