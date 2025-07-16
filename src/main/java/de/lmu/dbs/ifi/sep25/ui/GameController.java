package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.media.AudioClip;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.StrokeType;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import javafx.scene.shape.Line;
import javafx.animation.FadeTransition;
import javafx.geometry.Point2D;

import static java.util.Map.entry;

public class GameController {
    // 0. Logger
    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");
    private static final Logger appLogger = LogManager.getLogger(GameController.class);
    private static final Logger errorLogger = LogManager.getLogger(GameController.class);
    @FXML
    public HBox energyBox;
    public ImageView gameLogoView;
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
    @FXML
    private Button chatToggleButton;
    @FXML
    private Button chatRestoreButton;
    @FXML
    private Label chatIconLabel;
    @FXML
    private VBox leftSidebar;
    @FXML
    private HBox playedCardsBox;
    @FXML
    private VBox playerStatusBox;

    @FXML
    private HBox handCardBox;
    @FXML
    private Button shuffleVisualButton;


    @FXML
    private HBox registerBox;
    private static final int TILE_SIZE = 60;

    private final Map<String, Image> tileImages = new HashMap<>();
    private final Map<Integer, Position> robotPositions = new HashMap<>();
    private final Map<Integer, String> robotIDToPlayerName = new HashMap<>();
    private Parent root;
    @FXML
    private Label timerLabel;

    private Timeline countdownTimer;
    private int secondsLeft = 30;
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(GameController.class);
    @FXML
    private StackPane discardPileBox;

    @FXML
    private Label phaseLabel;
    @FXML
    private Button confirmSelectionButton;
    private Node draggedCard;
    @FXML
    private StackPane popupContainer;
    @FXML
    private ScrollPane gameBoardScrollPane;
    @FXML
    private StackPane zoomWrapper;
    @FXML
    private HBox turnInfoBox;
    @FXML
    private ImageView playerIcon;


    private int lastEnergy = -1;
    @FXML
    private Label energyIcon;
    @FXML
    private Label energyValue;
    @FXML
    private ProgressBar energyBar;


    private double scaleValue = 1.0;
    private final double SCALE_DELTA = 1.1;
    private final List<String> discardPile = new ArrayList<>();
    private Label discardCounter;
    private Timeline blinkTimeline;


    private int currentPlayerID = -1;
    private final Map<Integer, Integer> clientToRobotID = new HashMap<>();
    // Map: clientID -> List der ausgewählten Registerkarten (Strings)
    private final Map<Integer, List<String>> otherPlayersRegisters = new HashMap<>();

    private int currentPhaseID = -1;

    public void setCurrentPhaseID(int phaseID) {
        this.currentPhaseID = phaseID;
    }


    private final List<String> confirmedCards = new ArrayList<>();
    private final Map<Integer, PauseTransition> activeHighlights = new HashMap<>();
    private final List<StackPane> startPointPanes = new ArrayList<>();
    private final List<String> discardedCards = new ArrayList<>();


    private double dragStartX, dragStartY;
    private TranslateTransition robotFloat;
    private static final int MAX_ENERGY = 10;

    private Circle timerCircle;
    private StackPane timerContainer;


    /**
     * Stores the direction of the robot of each clientID as a lowercase string.
     **/
    private final Map<Integer, String> robotDirections = new HashMap<>();

    /**
     * Keeps track of the current register cards (null = empty)
     **/
    private final String[] registerState = new String[5];
    private final Set<Integer> manuallyClearedSlots = new HashSet<>();
    private final Set<Integer> swapProtectedSlots = new HashSet<>();


    @FXML
    public void initialize() {
        loadTileImages();
        gameBoardScrollPane.setStyle("-fx-background-color: transparent;");
        zoomWrapper.setStyle("-fx-background-color: transparent;");
        zoomWrapper.setOnScroll(event -> {
            if (event.isControlDown()) {
                event.consume();
                double oldScale = scaleValue;
                if (event.getDeltaY() > 0) {
                    scaleValue *= SCALE_DELTA;
                } else {
                    scaleValue /= SCALE_DELTA;
                }

                scaleValue = clamp(scaleValue, 0.5, 2.5);
                zoomWrapper.setScaleX(scaleValue);
                zoomWrapper.setScaleY(scaleValue);

                repositionScrollPane(event.getX(), event.getY(), oldScale);
            }
        });


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
        addHoverAnimation(chatToggleButton);

    }


    private void addHoverAnimation(Button button) {
        button.setOnMouseEntered(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), button);
            tt.setToY(-3);
            tt.play();
        });
        button.setOnMouseExited(e -> {
            TranslateTransition tt = new TranslateTransition(Duration.millis(150), button);
            tt.setToY(0);
            tt.play();
        });
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private void repositionScrollPane(double mouseX, double mouseY, double oldScale) {
        Bounds viewportBounds = gameBoardScrollPane.getViewportBounds();
        Bounds contentBounds = zoomWrapper.getBoundsInParent();

        double posX = (mouseX + gameBoardScrollPane.getHvalue() * (contentBounds.getWidth() - viewportBounds.getWidth())) / oldScale;
        double posY = (mouseY + gameBoardScrollPane.getVvalue() * (contentBounds.getHeight() - viewportBounds.getHeight())) / oldScale;

        double newX = posX * scaleValue;
        double newY = posY * scaleValue;

        double hValue = (newX - viewportBounds.getWidth() / 2) / (contentBounds.getWidth() - viewportBounds.getWidth());
        double vValue = (newY - viewportBounds.getHeight() / 2) / (contentBounds.getHeight() - viewportBounds.getHeight());

        gameBoardScrollPane.setHvalue(clamp(hValue, 0, 1));
        gameBoardScrollPane.setVvalue(clamp(vValue, 0, 1));
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
        clientToRobotID.put(clientID, figure);

        if (name != null && !name.isBlank()) {
            robotIDToPlayerName.put(figure, name);
            ClientSingleton.getInstance().getUsernames().put(clientID, name);
        }

        if (ClientSingleton.getInstance().getID() == clientID)
            return; // Sich selbst nicht hinzufügen

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
        if (elements == null || elements.isEmpty()) return pane;

        addImage(pane, "Floor", 0);

        for (MessageDefinitions.Field element : elements) {
            switch (element.type()) {
                case "Laser" -> {
                } // skip

                case "Wall" -> {
                    MessageDefinitions.FieldWall wall = (MessageDefinitions.FieldWall) element;
                    List<MessageDefinitions.FieldLaser> lasers = elements.stream()
                            .filter(e -> e instanceof MessageDefinitions.FieldLaser)
                            .map(e -> (MessageDefinitions.FieldLaser) e)
                            .toList();

                    for (String dir : wall.orientations()) {
                        String imageKey = "wall_n";
                        for (MessageDefinitions.FieldLaser laser : lasers) {
                            if (laser.orientations().getFirst().equals(dir)) {
                                imageKey = "wall_laser_" + laser.count() + "_off";
                                break;
                            }
                        }
                        addImage(pane, imageKey, convertDirectionToRotation(dir));
                    }
                }

                case "ConveyorBelt" -> {
                    MessageDefinitions.FieldConveyorBelt conveyor = (MessageDefinitions.FieldConveyorBelt) element;
                    String color = (conveyor.speed() == 2) ? "blue" : "green";
                    List<String> dirs = conveyor.orientations();
                    if (dirs == null || dirs.isEmpty()) break;

                    Direction outDir = Direction.fromString(dirs.getFirst());
                    List<Direction> inDirs = dirs.subList(1, dirs.size()).stream().map(Direction::fromString).toList();
                    String tileName = determineConveyorTile(color, outDir, inDirs);
                    double rotation = convertDirectionToRotation(outDir.toString().toLowerCase());
                    switch (tileName.substring(tileName.indexOf('_'))) {
                        case "_conveyor_corner_l" -> rotation += 90;
                        case "_conveyor_corner_r" -> rotation -= 90;
                    }
//                    appLogger.info("Adding conveyor tile {} with rotation {}", tileName, rotation); DEBUG
                    addImage(pane, tileName, rotation);
                }

                case "PushPanel" -> {
                    MessageDefinitions.FieldPushPanel pushPanel = (MessageDefinitions.FieldPushPanel) element;
                    String key = "PushPanel_" + String.join("_", pushPanel.registers().stream().map(String::valueOf).toList());
                    addImage(pane, key, convertDirectionToRotation(pushPanel.orientations().getFirst()));
                }

                case "RestartPoint" -> addImage(pane, "reboot", convertDirectionToRotation(
                        ((MessageDefinitions.FieldRestartPoint) element).orientations().getFirst()));

                case "Antenna" -> addImage(pane, "antenna", convertDirectionToRotation(
                        ((MessageDefinitions.FieldAntenna) element).orientations().getFirst()));

                case "StartPoint" -> {
                    addImage(pane, "black and white gears", 0);

                    Circle yellowGlow = new Circle(15);
                    yellowGlow.setFill(Color.TRANSPARENT); // default = invisible
                    StackPane.setAlignment(yellowGlow, Pos.CENTER);

                    Circle overlay = new Circle(15);
                    overlay.setFill(Color.TRANSPARENT);
                    StackPane.setAlignment(overlay, Pos.CENTER);

                    pane.getChildren().addAll(yellowGlow, overlay);
                    pane.setUserData(overlay); // for green overlay access
                    pane.getProperties().put("yellowGlow", yellowGlow); // store for toggling

                    startPointPanes.add(pane); // for later bulk update
                }
                default -> {
                    String key = getTileKeyForElement(element);
                    if (tileImages.containsKey(key)) {
                        addImage(pane, key, 0);
                    } else {
                        appLogger.warn("Fehlendes Bild: {}", key);

                    }
                }
            }
        }

        pane.requestLayout();
        return pane;
    }

    /**
     * Helper to add an ImageView with rotation.
     */
    private void addImage(StackPane pane, String key, double rotation) {
        Image img = tileImages.get(key);
        if (img == null) {
            appLogger.warn("Fehlendes Bild: {}", key);
            return;
        }
        ImageView view = new ImageView(img);
        view.setFitWidth(TILE_SIZE);
        view.setFitHeight(TILE_SIZE);
        view.setRotate(rotation);
        pane.getChildren().add(view);
    }

    /**
     * Handles clicking a start point tile.
     */
    private void handleStartPointClick(StackPane selectedPane) {
        Integer x = GridPane.getColumnIndex(selectedPane);
        Integer y = GridPane.getRowIndex(selectedPane);
        if (x == null || y == null) return;

        var msg = new MessageDefinitions.Message<>(new MessageDefinitions.BodySetStartingPoint(x, y,
                switch (ClientSingleton.getInstance().getSelectedMap()) {
                    case "Heavy Merge Area", "Death Trap" -> "left";
                    case "Pilgrimage", "Gear Stripper" -> "top";
                    default -> "right";
                }));
        ClientSingleton.getInstance().sendMessage(msg);

        // Visually mark this tile as selected (solid green)
        Circle overlay = (Circle) selectedPane.getUserData();
        overlay.setFill(Color.rgb(39, 174, 96, 0.6)); // solid green

        appendChatMessage("[INFO] Startposition gewählt bei (" + x + ", " + y + ")");
    }

    public void deselectStartingPosition() {
        for (Node node : gameBoardPane.getChildren()) {
            if (node instanceof StackPane pane) {
                Circle overlay = (Circle) pane.getUserData();
                if (overlay != null) overlay.setFill(Color.TRANSPARENT);
            }
        }
    }

    /**
     * Deactivates the startpointselection. Is called when startingpoint selection is confirmed by server.
     **/
    public void deactivateStartPointClick() {
        for (Node node : gameBoardPane.getChildren()) {
            if (node instanceof StackPane pane) {
                // Disable interaction
                pane.setOnMouseClicked(null);
                pane.setOnMouseEntered(null);
                pane.setOnMouseExited(null);

                // Reset green overlay
                Circle overlay = (Circle) pane.getUserData();
                if (overlay != null) {
                    overlay.setFill(Color.TRANSPARENT);
                }

                // Reset yellow glow
                Circle yellowGlow = (Circle) pane.getProperties().get("yellowGlow");
                if (yellowGlow != null) {
                    yellowGlow.setFill(Color.TRANSPARENT);
                }
            }
        }
    }

    /**
     * Determines which conveyor tile to use.
     */
    private String determineConveyorTile(String color, Direction out, List<Direction> in) {
        if (in.isEmpty()) return color + "_conveyor_belt_straight";

        if (in.size() == 1) {
            Direction inDir = in.getFirst();
            if (inDir.turnAround().equals(out)) return color + "_conveyor_belt_straight";

            return switch (getRelativeTurn(inDir, out)) {
                case 1 -> color + "_conveyor_corner_l";
                default -> color + "_conveyor_corner_r";
            };
        }

        boolean isMerge = in.stream().allMatch(d -> d.turnAround().equals(out));
        if (isMerge) return color + "_conveyor_merge";

        Direction nonOpposite = in.stream().filter(d -> !d.turnAround().equals(out)).findFirst().orElse(null);
        if (nonOpposite != null) {
            return getRelativeTurn(out, nonOpposite) == 1
                    ? color + "_conveyor_split_right"
                    : color + "_conveyor_split_left";
        }

        System.err.println("Unable to determine conveyor type, using default straight conveyor");
        return color + "_conveyor_belt_straight";
    }

    /**
     * Returns relative turn.
     */
    private int getRelativeTurn(Direction from, Direction to) {
        int delta = (to.ordinal() - from.ordinal() + 4) % 4;

        return switch (delta) {
            case 1 -> 1;   // Rechtsdrehung
            case 3 -> -1;  // Linksdrehung
            default -> 0;  // Gerade oder U-Turn (ignoriert U-Turn als Sonderfall)
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
                entry("wall_laser_2_off", "wall_laser_2_off.png"),
                entry("wall_laser_3_on", "wall_laser_3.png"),
                entry("wall_laser_3_off", "wall_laser_3_off.png"),
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
                entry("checkpoint5", "checkpoint5.png"),
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
                entry("blue_conveyor_corner_r", "blue_conveyor_corner_r.png"),
                entry("blue_conveyor_corner_l", "blue_conveyor_corner_l.png"),
                entry("blue_conveyor_merge", "blue_conveyor_merge_triple.png"),
                entry("blue_conveyor_split_left", "blue_conveyor_split_left.png"),
                entry("blue_conveyor_split_right", "blue_conveyor_split_right.png"),
                entry("green_conveyor_corner_r", "green_conveyor_corner_r.png"),
                entry("green_conveyor_corner_l", "green_conveyor_corner_l.png")


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
            case "Pit":
                return "Pit";
            case "Energy-Space":
                MessageDefinitions.FieldEnergySpace es = (MessageDefinitions.FieldEnergySpace) element;
                Integer count = es.getCount();
                return count != null && count > 0 ? "energyspace_green" : "energyspace_red";
            case "CheckPoint":
                return "checkpoint" + ((MessageDefinitions.FieldCheckPoint) element).count().toString();
            case "Gear":
                return ((MessageDefinitions.FieldGear) element).orientations().getFirst().equalsIgnoreCase("clockwise") ? "Gear_Green" : "Gear_Red";
            default:
                return "Unknown type received: " + element.type();
        }
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

        messageLogger.info("→ Chat gesendet an {}: {}", (recipientID == -1 ? "ALLE" : recipientID), msg);
        var client = ClientSingleton.getInstance();
        if (client != null) {
            client.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySendChat(msg, recipientID)));
        } else {
            messageLogger.error("ClientSingleton is null in handleSendChat");
        }


        String prefix = (recipientID == -1) ? "Du" : "Du → " + selected.getName();
        appendChatMessage(prefix + ": " + msg);
        chatInput.clear();
    }

    public void appendChatMessage(String message) {
        chatArea.appendText(message + "\n");
    }

    public int getCurrentPhaseID() {
        return currentPhaseID;
    }

    public void updatePhase(int phaseID) {
        final int previousPhase = getCurrentPhaseID();

        logger.info("Update game phase from {} to {}", previousPhase, phaseID);

        // Update internal state
        this.currentPhaseID = phaseID;
        updateMiniRobotInPhase(phaseID == 0, phaseID == 2, phaseID == 3);

        // Phase label
        if (phaseLabel != null) {
            phaseLabel.setText("Phase: " + switch (phaseID) {
                case 0 -> "Aufbauphase";
                case 1 -> "Upgradephase";
                case 2 -> "Programmierphase";
                case 3 -> "Aktivierungsphase";
                default -> "Unbekannt";
            });
        }

        // Reset all startpoint interactivity unless Setup
        for (Node node : gameBoardPane.getChildren()) {
            if (node instanceof StackPane pane && pane.getOnMouseClicked() != null) {
                if (phaseID != 0) {
                    pane.setOnMouseClicked(null);
                    pane.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");
                }
            }
        }

        // Phase-specific UI updates
        switch (phaseID) {
            case 0 -> {
                setPhaseLabel("🛠️ Phase: Aufbau", "phase-setup");
                showLogoTransition();
                if (playerStatusBox != null) {
                    playerStatusBox.setVisible(false);

                    // playedCardsBox.setVisible(false);
                    playerStatusBox.setManaged(false);
                    playerStatusBox.getChildren().removeIf(node -> !(node instanceof Label));
                }
                if (energyBox != null) {
                    energyBox.setVisible(false);
                    energyBox.setManaged(false);
                }
                if (confirmSelectionButton != null) {
                    confirmSelectionButton.setVisible(false);
                    confirmSelectionButton.setManaged(false);
                }
                if (discardPileBox != null) {
                    discardPileBox.setVisible(false);
                    discardPileBox.setManaged(false);
                }


            }

            case 1 -> { // Upgrade phase UNIMPLEMENTED

            }

            case 2 -> { // Programming phase
                setPhaseLabel("🤖 Phase: Programmierung", "phase-programming");
                if (playerStatusBox != null) {
                    playerStatusBox.setVisible(false);

                    playedCardsBox.setVisible(false);
                    playerStatusBox.setManaged(false);
                    playerStatusBox.getChildren().removeIf(node -> !(node instanceof Label));

                }

                if (energyBox != null) {
                    energyBox.setVisible(false);
                    energyBox.setManaged(false);
                }

                statusLabel.setText("🌟 Spiel läuft...");
                statusLabel.getStyleClass().setAll("dynamic-status");

                // Ensure conflicting styles are removed
                statusLabel.getStyleClass().removeAll("status-other-turn", "another-old-style");

                if (!statusLabel.getStyleClass().contains("dynamic-status")) {
                    statusLabel.getStyleClass().add("dynamic-status");
                }

                Platform.runLater(() -> {
                    clearHandUI();
                    clearRegisterUI();
                });

                handCardBox.setDisable(false);
                handCardBox.setVisible(true);
                handCardBox.setManaged(true);
                showDragAndDropInfoPopup();

                if (confirmSelectionButton != null) {
                    updateConfirmButtonVisibility();
                }
                if (shuffleVisualButton != null) {
                    shuffleVisualButton.setVisible(true);
                    shuffleVisualButton.setManaged(true);
                }

            }

            case 3 -> { // Activation phase
                setPhaseLabel("⚡ Phase: Aktivierung", "phase-activation");

                if (shuffleVisualButton != null) {
                    shuffleVisualButton.setVisible(false);
                    shuffleVisualButton.setManaged(false);
                }


                if (playerStatusBox != null) {
                    playerStatusBox.setVisible(true);
                    playerStatusBox.setManaged(true);
                }


                if (energyBox != null) {
                    energyBox.setVisible(true);
                    energyBox.setManaged(true);
                }
                statusLabel.setText("Aktivierungsphase");
                statusLabel.getStyleClass().setAll("dynamic-status");

                handCardBox.setDisable(true);
                handCardBox.setVisible(false);
                handCardBox.setManaged(false);

                if (confirmSelectionButton != null) {
                    confirmSelectionButton.setVisible(false);
                    confirmSelectionButton.setManaged(false);
                }

                fadeOutCountdown();
                stopCountdownSound();

                Platform.runLater(this::clearHandUI);

                if (!confirmedCards.isEmpty()) {
                    displayConfirmedCards(confirmedCards);
                }
                Timeline activationTimeline = new Timeline();

                for (int i = 0; i < registerBox.getChildren().size(); i++) {
                    int index = i;
                    KeyFrame frame = new KeyFrame(Duration.seconds(index * 1.5), e -> {
                        highlightRegisterCard(index);
                    });
                    activationTimeline.getKeyFrames().add(frame);
                }

                activationTimeline.play();


                activationPhaseActive = true;
                refreshPlayerStatusUI();

                handCardBox.setDisable(true);
                handCardBox.setVisible(false);
                handCardBox.setManaged(false);

                if (confirmSelectionButton != null) {
                    confirmSelectionButton.setVisible(false);
                    confirmSelectionButton.setManaged(false);
                }
            }

            default -> throw new IllegalStateException("Unexpected value: " + phaseID);
        }
    }

    private void clearHandUI() {
        handCardBox.getChildren().clear();
    }

    private void clearRegisterUI() {
        registerBox.getChildren().forEach(node -> {
            if (node instanceof VBox vbox) {
                Node slot = vbox.getChildren().get(1);
                if (slot instanceof StackPane stack) {
                    stack.getChildren().clear();
                }
            }
        });
        Arrays.fill(registerState, null); // reset internal state
    }
    private void highlightRegisterCard(int index) {
        if (index < 0 || index >= registerBox.getChildren().size()) return;

        Node cardNode = registerBox.getChildren().get(index);
        if (!cardNode.getStyleClass().contains("card-active")) {
            cardNode.getStyleClass().add("card-active");
        }

        PauseTransition pause = new PauseTransition(Duration.seconds(2));
        pause.setOnFinished(e -> cardNode.getStyleClass().remove("card-active"));
        pause.play();
    }


    private void setPhaseLabel(String phaseText, String cssClass) {
        phaseLabel.setText(phaseText);

        phaseLabel.getStyleClass().removeAll("phase-setup", "phase-programming", "phase-activation");
        if (!phaseLabel.getStyleClass().contains(cssClass)) {
            phaseLabel.getStyleClass().add(cssClass);
        }

        FadeTransition fade = new FadeTransition(Duration.millis(400), phaseLabel);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();
    }


    public void updateDiscardPile(List<String> discardedCards) {
        discardPileBox.getChildren().clear();

        for (String card : discardedCards) {
            ImageView cardImage = new ImageView(new Image(getClass().getResourceAsStream("/cards/" + card + ".png")));
            cardImage.setFitWidth(60);
            cardImage.setFitHeight(90);
            discardPileBox.getChildren().add(cardImage);
        }
    }

    /**
     * Zeigt oder versteckt das Chat-Fenster.
     * Blendet das Icon-Menü entsprechend ein oder aus.
     */

    @FXML
    private void toggleChatBox() {
        if (chatBox.isVisible()) {
            slideOut(chatBox);
            chatIconLabel.setText("▲");

        } else {
            slideIn(chatBox);
            chatIconLabel.setText("▼");

        }
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

    public void setInitialPlayerStats(Map<Integer, Integer> energy, Map<Integer, Integer> checkpointsReached) {
    }

    /**
     * Zeigt eine gespielte Karte visuell im Kartenbereich an.
     *
     * @param clientID Die ID des Spielers, der die Karte gespielt hat.
     * @param cardName Der Name der gespielten Karte (z. B. "move_1", "turn_right").
     */
    public void showPlayedCard(int clientID, String cardName) {
        String imagePath = "/assets/cards/" + cardName.toLowerCase() + ".png";
        Image cardImage;
        try {
            cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        } catch (Exception e) {
            cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover_card.png")));
        }

        ImageView cardView = new ImageView(cardImage);
        cardView.setFitWidth(60);
        cardView.setFitHeight(90);
        cardView.setPreserveRatio(false);
        cardView.setSmooth(true);

        playedCardsBox.getChildren().add(cardView);
        if (playedCardsBox.getChildren().size() > 5) {
            playedCardsBox.getChildren().remove(0);
        }
    }


    public void markCurrentPlayer(int clientID) {
        int phaseID = getCurrentPhaseID();

        if (phaseID == 2) {
            appendChatMessage("[INFO] Programmieren...");
            statusLabel.setText("Spiel läuft...");
            return;

        }
        this.currentPlayerID = clientID; // Immer setzen
        int myID = ClientSingleton.getInstance().getID();

        appLogger.info("[DEBUG] markCurrentPlayer aufgerufen mit clientID = {}", clientID);
        appLogger.info("ClientID (ich): {}, aktueller Spieler: {}", myID, clientID);
        appLogger.info("PHASE ID AKTUELL: {}", phaseID);
        boolean isMyTurn = (myID == clientID);

        if (isMyTurn) {
            appendChatMessage("[INFO] Du bist am Zug!");
            statusLabel.setText("Du bist am Zug.");
            statusLabel.getStyleClass().removeAll("status-other-turn");
            if (!statusLabel.getStyleClass().contains("dynamic-status")) {
                statusLabel.getStyleClass().add("dynamic-status");
            }
            playWavingAnimation(statusLabel);
            // Imagen Robot)
            showMiniRobot(myID);


            switch (phaseID) {
                case 0 -> {
                    appendChatMessage("Bitte wähle deine Startposition durch Klick auf ein gelbes Feld.");

                    startPointPanes.forEach(pane -> {
                        Circle yellow = (Circle) pane.getProperties().get("yellowGlow");
                        if (yellow != null)
                            yellow.setFill(new RadialGradient(
                                    0, 0, 0.5, 0.5, 1.0, true, CycleMethod.NO_CYCLE,
                                    new Stop(0.0, Color.rgb(255, 223, 0, 0.4)),
                                    new Stop(1.0, Color.TRANSPARENT)
                            ));

                        Circle overlay = (Circle) pane.getUserData();
                        pane.setOnMouseClicked(e -> handleStartPointClick(pane));
                        pane.setOnMouseEntered(e -> {
                            if (overlay.getFill().equals(Color.TRANSPARENT)) {
                                overlay.setFill(Color.rgb(46, 204, 113, 0.3));
                            }
                        });
                        pane.setOnMouseExited(e -> {
                            if (overlay.getFill().equals(Color.rgb(46, 204, 113, 0.3))) {
                                overlay.setFill(Color.TRANSPARENT);
                            }
                        });
                    });
                }

                case 1 -> {

                }

                case 2 -> {

                }
            }
        } else {
            String name = getPlayerNameById(clientID);
            appendChatMessage("[INFO] Spieler " + name + " ist am Zug.");
            statusLabel.setText(name + " ist am Zug.");

            statusLabel.getStyleClass().removeAll("dynamic-status");
            if (!statusLabel.getStyleClass().contains("status-other-turn")) {
                statusLabel.getStyleClass().add("status-other-turn");
            }
            stopWavingAnimation(statusLabel);
            playerIcon.setImage(null);
            //playerIcon.setVisible(false);
        }


        for (StackPane sp : startPointPanes) {
            sp.setDisable(!isMyTurn);
        }

        for (PlayerEntry entry : recipientBox.getItems()) {
            if (entry.getClientID() == clientID) {
                recipientBox.getSelectionModel().select(entry);
                break;
            }
        }
    }

    private void showMiniRobot(int clientID) {
        try {
            int robotID = clientToRobotID.getOrDefault(clientID, 0);
            String imagePath = "/assets/robots/robot_0" + robotID + "_right.png";
            Image image = new Image(getClass().getResourceAsStream(imagePath));
            playerIcon.setImage(image);
            playerIcon.setVisible(true);
            applyRobotGlow(playerIcon, robotID);
        } catch (Exception e) {
            appLogger.warn("Fehler beim Anzeigen des Mini-Roboters: {}", e.getMessage());
            playerIcon.setVisible(false);
        }
    }


    /**
     * Zeigt die Startposition eines Roboters im Spielfeld an.
     *
     * @param x         X-Koordinate auf dem Spielfeld
     * @param y         Y-Koordinate auf dem Spielfeld
     * @param clientID  Die Client-ID des Spielers
     * @param direction Die Ausrichtung des Roboters (z. B. "right", "left", "top", "botom")
     */
    public void displayStartingPoint(int x, int y, int clientID, String direction) {
        int robotID = clientToRobotID.get(clientID);
        try {
            robotDirections.put(clientID, direction);
            String imagePath = "/assets/robots/robot_0" + robotID + "_" + direction.toLowerCase() + ".png";
            Image robotImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            ImageView robotView = new ImageView(robotImg);
            robotView.setFitWidth(40);
            robotView.setFitHeight(40);
            robotView.setPreserveRatio(true);
            robotView.setUserData("robot");

            // Setze Roboter auf das Spielfeld (Grid)
            StackPane tile = getTileAt(x, y);
            tile.getChildren().add(robotView);

            robotPositions.put(clientID, new Position(x, y));

        } catch (Exception e) {
            appLogger.error("Roboter konnte nicht angezeigt werden an ({}, {})", x, y);
            e.printStackTrace();
        }
    }

    /**
     * Holt das StackPane an der gegebenen Spielfeldposition.
     *
     * @param x X-Koordinate
     * @param y Y-Koordinate
     * @return Das StackPane an der Position oder neu erstellt
     */
    private StackPane getTileAt(int x, int y) {
        for (javafx.scene.Node node : gameBoardPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == x && GridPane.getRowIndex(node) == y && node instanceof StackPane pane) {
                return pane;
            }
        }
        StackPane fallback = new StackPane();
        gameBoardPane.add(fallback, x, y);
        return fallback;
    }


    /**
     * Zeigt die Handkarten des Spielers in der Benutzeroberfläche an.
     *
     * <p>Diese Methode:
     * <ul>
     *   <li>Prüft, ob Karten vorhanden sind.</li>
     *   <li>Leert und initialisiert die Registerfelder (Slots 1–5).</li>
     *   <li>Zeigt die Karten in der Handkarten-Box an.</li>
     *   <li>Fügt jeder Karte ein Klick-Ereignis hinzu, um sie ins Register zu verschieben.</li>
     */
    public void displayHandCards(List<String> cardNames) {
        if (cardNames == null || cardNames.isEmpty()) {
            logger.error("Spieler hat keine Karten erhalten..");
            return;
        }

        if (handCardBox == null || registerBox == null) {
            appLogger.error("handCardBox oder registerBox ist null.");
            return;
        }


        registerBox.getChildren().clear();

        for (int i = 0; i < 5; i++) {
            Label numberLabel = new Label(String.valueOf(i + 1));
            numberLabel.getStyleClass().add("slot-number");

            StackPane slot = new StackPane();
            slot.setPrefSize(60, 90);
            slot.getStyleClass().add("register-slot");

            setupRegisterSlot(slot);

            Tooltip tooltip = new Tooltip("Bitte hier eine Programmierkarte ablegen");
            Tooltip.install(slot, tooltip);

            final int slotIndex = i;
            slot.setOnDragOver(event -> {
                if (event.getGestureSource() != slot && event.getDragboard().hasString()) {
                    String draggedCardName = event.getDragboard().getString().toLowerCase();
                    if (slotIndex == 0 && draggedCardName.contains("again")) {
                        event.consume();
                    } else {
                        event.acceptTransferModes(TransferMode.MOVE);
                    }
                }
            });

            VBox slotWithLabel = new VBox(5, numberLabel, slot);
            slotWithLabel.setAlignment(Pos.CENTER);
            registerBox.getChildren().add(slotWithLabel);

            if (registerState[i] != null) {
                placeCardInRegisterSlot(registerState[i], i);
            }
        }



// 2. Filter used cards
        List<String> adjustedHand = new ArrayList<>();
        Map<String, Integer> cardInstanceCounter = new HashMap<>();

        for (String card : cardNames) {
            int instance = cardInstanceCounter.getOrDefault(card, 0);
            cardInstanceCounter.put(card, instance + 1);
            adjustedHand.add(card + "#" + instance);
        }

// 3. Show remaining hand cards
        handCardBox.getChildren().clear();

        for (String tagged : adjustedHand) {
            String base = tagged.split("#")[0];
            ImageView view = createClickableCard(base);
            view.setUserData(tagged);
            view.setOpacity(0);

            handCardBox.getChildren().add(view);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(200), view);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        }

        updateConfirmButtonVisibility();
        shuffleHandCards();
    }


    private ImageView createClickableCard(String cardName) {
        String imagePath = "/assets/cards/" + cardName + ".png";
        Image img;
        try {
            img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        } catch (Exception e) {
            logger.error("Image not found: {}", imagePath);
            img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover_card.png")));
        }

        ImageView view = new ImageView(img);
        view.setFitWidth(60);
        view.setFitHeight(90);
        view.setPreserveRatio(false);
        view.setSmooth(true);
        view.setUserData(cardName);
        view.setStyle("-fx-cursor: hand;");

        view.setOnMouseEntered(e -> {
            view.setScaleX(1.2);
            view.setScaleY(1.2);
            view.setTranslateY(-10);
            view.setEffect(new DropShadow());
        });

        view.setOnMouseExited(e -> {
            view.setScaleX(1.0);
            view.setScaleY(1.0);
            view.setTranslateY(0);
            view.setEffect(null);
        });

        view.setOnMousePressed(event -> {
            dragStartX = event.getSceneX();
            dragStartY = event.getSceneY();
        });

        view.setOnMouseReleased(event -> {
            double deltaX = Math.abs(event.getSceneX() - dragStartX);
            double deltaY = Math.abs(event.getSceneY() - dragStartY);

            // Only handle click if not dragged
            if (deltaX < 5 && deltaY < 5 && draggedCard == null) {
                int slot = findNextEmptyRegisterSlot();
                if (slot != -1) {
                    if (placeCardInRegisterSlot(cardName, slot)) {
                        handCardBox.getChildren().remove(view);
                        appendChatMessage("[INFO] Karte " + cardName + " wurde ins Register " + (slot + 1) + " gelegt.");

                    }
                } else {
                    appendChatMessage("[WARN] Kein freier Register-Slot verfügbar.");
                }
            }
        });

        view.setOnDragDetected(event -> {
            Dragboard db = view.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(cardName + "#" + System.nanoTime());
            db.setContent(content);
            db.setDragView(view.getImage());
            draggedCard = view;
            event.consume();
        });

        view.setOnDragDone(event -> {
            draggedCard = null;
        });

        return view;
    }

    private int findNextEmptyRegisterSlot() {
        for (int i = 0; i < registerState.length; i++) {
            if (registerState[i] == null) {
                appLogger.info("Found empty register slot at index {}", i);
                return i;
            }
        }
        return -1;
    }

    /**
     * Ermittelt den Slot-Index basierend auf dem StackPane in der UI.
     * Wird beim Drag & Drop verwendet.
     */
    private int findRegisterSlotIndex(StackPane pane) {
        for (int i = 0; i < registerBox.getChildren().size(); i++) {
            Node node = registerBox.getChildren().get(i);
            if (node instanceof VBox vbox && vbox.getChildren().get(1) == pane) {
                return i;
            }
        }
        return -1;
    }

    private boolean placeCardInRegisterSlot(String cardName, int slotIndex) {
        return placeCardInRegisterSlot(cardName, slotIndex, true, true);
    }

    private boolean placeCardInRegisterSlot(String cardName, int slotIndex, boolean returnToHand) {
        return placeCardInRegisterSlot(cardName, slotIndex, returnToHand, true);
    }

    private boolean placeCardInRegisterSlot(String cardName, int slotIndex, boolean returnToHand, boolean sendUpdate) {
        if (slotIndex < 0 || slotIndex >= registerBox.getChildren().size()) return false;

        if (slotIndex == 0 && cardName.toLowerCase().contains("again")) {
            highlightRegisterSlot(0);
            displayErrorAlert(
                    "Card placement error",
                    "The card \"Again\" cannot be placed in the first register position!\n" +
                            "Please select the 2nd to 5th register positions."
            );
            return false;
        }

        Node node = registerBox.getChildren().get(slotIndex);
        if (!(node instanceof VBox vbox)) return false;
        Node slotNode = vbox.getChildren().get(1);
        if (!(slotNode instanceof StackPane pane)) return false;

        String previousCard = registerState[slotIndex];
        if (previousCard != null && returnToHand) {
            ImageView returnCard = createClickableCard(previousCard);
            handCardBox.getChildren().add(returnCard);
        }

        // Clear and place new card
        pane.getChildren().clear();

        String imagePath = "/assets/cards/" + cardName + ".png";
        Image img;
        try {
            img = new Image(getClass().getResourceAsStream(imagePath));
        } catch (Exception e) {
            img = new Image(getClass().getResourceAsStream("/assets/cover_card.png"));
        }

        ImageView cardView = new ImageView(img);
        cardView.setFitWidth(80);
        cardView.setFitHeight(120);
        cardView.setPreserveRatio(true);
        cardView.setSmooth(true);
        cardView.setOpacity(0.7);
        cardView.setUserData(cardName);

        // Enable drag from register
        cardView.setOnDragDetected(event -> {
            Dragboard db = cardView.startDragAndDrop(TransferMode.MOVE);
            ClipboardContent content = new ClipboardContent();
            content.putString(cardName + "#" + System.nanoTime());
            db.setContent(content);
            db.setDragView(cardView.getImage());
            draggedCard = cardView;
            event.consume();
        });

        // Enable click-to-remove
        cardView.setOnMouseClicked(event -> {
            if (findNextEmptyRegisterSlot() != -1) {
                pane.getChildren().clear();
                registerState[slotIndex] = null;

                handCardBox.getChildren().add(createClickableCard(cardName));
                manuallyClearedSlots.add(slotIndex);

                sendCardSelectionUpdate(null, slotIndex);

                appendChatMessage("[INFO] Karte " + cardName + " wurde aus Register " + (slotIndex + 1) + " entfernt.");
            }
        });

        pane.getChildren().add(cardView);
        registerState[slotIndex] = cardName;

        if (sendUpdate) {
            sendCardSelectionUpdate(cardName, slotIndex);
        }

        appLogger.info("Card {} placed in register slot {}", cardName, slotIndex);

        return true;
    }

    /**
     * Initialisiert einen Register-Slot zur Annahme von Karten per Drag & Drop.
     */
    private void setupRegisterSlot(StackPane pane) {
        pane.setOnDragOver(event -> {
            if (event.getGestureSource() != pane && event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.MOVE);
            }
            event.consume();
        });

        pane.setOnDragDropped(event -> {
            Dragboard db = event.getDragboard();
            boolean success = false;

            if (db.hasString() && draggedCard instanceof ImageView) {
                String rawCardName = db.getString();
                String actualCardName = rawCardName.contains("#")
                        ? rawCardName.split("#")[0]
                        : rawCardName;

                int targetIndex = findRegisterSlotIndex(pane);
                if (targetIndex != -1) {
                    if (draggedCard != null) {
                        for (int i = 0; i < registerBox.getChildren().size(); i++) {
                            StackPane slot = getRegisterPane(i);
                            if (slot != null && slot.getChildren().contains(draggedCard) && i == targetIndex) {
                                // Card dragged back to same slot — ignore
                                event.setDropCompleted(false);
                                draggedCard = null;
                                event.consume();
                                return;
                            }
                        }
                    }

                    int sourceIndex = -1;
                    for (int i = 0; i < registerBox.getChildren().size(); i++) {
                        StackPane slot = getRegisterPane(i);
                        if (slot != null && slot.getChildren().contains(draggedCard)) {
                            sourceIndex = i;
                            break;
                        }
                    }

                    String existingCardInTarget = registerState[targetIndex];

                    if (sourceIndex != -1 && existingCardInTarget != null) {
                        success = swapRegisterCards(sourceIndex, targetIndex);
                    } else {
                        // Standard case: either hand → register or register → empty slot
                        if (sourceIndex != -1) {
                            StackPane oldPane = getRegisterPane(sourceIndex);
                            if (oldPane != null) oldPane.getChildren().clear();
                            registerState[sourceIndex] = null;
                            sendCardSelectionUpdate(null, sourceIndex);
                        }

                        boolean placed = placeCardInRegisterSlot(actualCardName, targetIndex);
                        if (placed && handCardBox.getChildren().contains(draggedCard)) {
                            handCardBox.getChildren().remove(draggedCard);
                        }
                        success = placed;
                    }
                }
            }

            event.setDropCompleted(success);
            draggedCard = null;
            event.consume();
        });
    }


    private boolean swapRegisterCards(int indexA, int indexB) {
        String cardA = registerState[indexA];
        String cardB = registerState[indexB];

        if (cardA == null || cardB == null) return false;

        // Rule check for "Again"
        if ((indexA == 0 && cardB.toLowerCase().contains("again")) ||
                (indexB == 0 && cardA.toLowerCase().contains("again"))) {

            highlightRegisterSlot(indexA);
            highlightRegisterSlot(indexB);
            displayErrorAlert("Ungültiger Kartentausch",
                    "Die Karte \"Again\" darf sich nicht im ersten Register befinden –\n" +
                            "auch nicht durch Tausch mit einer anderen Karte.");
            return false;
        }

        // Clear visuals + internal state
        StackPane paneA = getRegisterPane(indexA);
        StackPane paneB = getRegisterPane(indexB);
        if (paneA != null) paneA.getChildren().clear();
        if (paneB != null) paneB.getChildren().clear();

        registerState[indexA] = null;
        registerState[indexB] = null;

        // Lock
        swapProtectedSlots.add(indexA);
        swapProtectedSlots.add(indexB);

        // Send deselections BEFORE placing anything
        sendCardSelectionUpdate(null, indexA);
        sendCardSelectionUpdate(null, indexB);

        // Now reassign state
        registerState[indexA] = cardB;
        registerState[indexB] = cardA;

        // Place cards visually WITHOUT sending again
        placeCardInRegisterSlot(cardB, indexA, false, false);
        placeCardInRegisterSlot(cardA, indexB, false, false);

        // Final: send correct updates to server
        sendCardSelectionUpdate(cardB, indexA);
        sendCardSelectionUpdate(cardA, indexB);

        // Unlock
        swapProtectedSlots.remove(indexA);
        swapProtectedSlots.remove(indexB);

        return true;
    }

    private StackPane getRegisterPane(int index) {
        if (index < 0 || index >= registerBox.getChildren().size()) return null;
        Node node = registerBox.getChildren().get(index);
        if (node instanceof VBox vbox) {
            Node slotNode = vbox.getChildren().get(1);
            if (slotNode instanceof StackPane pane) return pane;
        }
        return null;
    }

    private void sendCardSelectionUpdate(String cardName, int registerIndex) {
        ClientSingleton.getInstance().sendMessage(
                new MessageDefinitions.Message<>(new MessageDefinitions.BodySelectedCard(cardName, registerIndex))
        );
        if (cardName == null) {
            appLogger.info("→ Removed card from register {}", registerIndex);
        } else {
            appLogger.info("→ Selected card '{}' for register {}", cardName, registerIndex);
        }
    }

    /**
     * Bestätigt die ausgewählten Karten und sendet sie an den Server.
     */
    @FXML
    private void handleConfirmSelection() {
        appLogger.info("Sending SelectionFinished manually for clientID {}", ClientSingleton.getInstance().getID());

        List<String> selectedCards = registerBox.getChildren().stream()
                .filter(n -> n instanceof VBox)
                .map(n -> ((VBox) n).getChildren().get(1))
                .filter(n -> n instanceof StackPane)
                .map(n -> (StackPane) n)
                .filter(p -> !p.getChildren().isEmpty())
                .map(p -> {
                    Node node = p.getChildren().get(0);
                    if (node instanceof ImageView iv && iv.getUserData() != null) {
                        return iv.getUserData().toString();
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();

        if (selectedCards.size() != 5) {
            appendChatMessage("[WARNUNG] Du musst genau 5 Karten ins Register ziehen.");
            return;
        }

        int clientID = ClientSingleton.getInstance().getID();
        for (int i = 0; i < selectedCards.size(); i++) {
            String cardName = selectedCards.get(i);
            appLogger.info("→ Karte ausgewählt: {} in Slot {}", cardName, i);
        }

        // Sende Nachricht an Server
        var finishedBody = new MessageDefinitions.BodySelectionFinished(clientID);
        var finishedMsg = new MessageDefinitions.Message<>(finishedBody);
        ClientSingleton.getInstance().sendMessage(finishedMsg);
        stopCountdownSound();
        appendChatMessage("[INFO] Auswahl wurde erfolgreich gesendet.");

        confirmSelectionButton.setVisible(false);
        confirmSelectionButton.setManaged(false);
        confirmSelectionButton.setDisable(true);
    }


    /**
     * Zeigt für einen anderen Spieler Kartenrückseiten an.
     *
     * @param clientID Spieler-ID
     * @param count    Anzahl der Karten
     */
    public void displayHiddenCardsForPlayer(int clientID, int count) {
        handCardBox.getChildren().clear();

        for (int i = 0; i < count; i++) {
            Image back = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover_card.png")));

            ImageView cardBack = new ImageView(back);
            cardBack.setFitWidth(60);
            cardBack.setFitHeight(90);
            cardBack.setPreserveRatio(true);
            cardBack.setSmooth(true);

            handCardBox.getChildren().add(cardBack);
        }

        String playerName = getPlayerNameById(clientID);
        // appendChatMessage("Spieler " + playerName + " hat " + count + " Karten erhalten.");
    }

    /**
     * Zeigt eine optionale Animation oder Nachricht an, dass das Deck gemischt wurde.
     */
    public void showShuffleAnimation() {
        appendChatMessage("[INFO] Das Programmierdeck wurde neu gemischt.");
        playShuffleSound();
        shuffleHandCards();

    }

    @FXML
    private void handleShuffleVisual() {
        playShuffleSound();
        List<Node> cards = new ArrayList<>(handCardBox.getChildren());
        Collections.shuffle(cards);
        handCardBox.getChildren().setAll(cards);
    }


    /**
     * Wird aufgerufen, wenn ein Spieler eine Karte für sein Register ausgewählt hat.
     *
     * @param clientID Die ID des Spielers
     * @param filled   Ob das Register des Spielers vollständig ist
     */
    public void handleCardSelection(int clientID, int register, boolean filled) {
        final boolean isSelf = clientID == ClientSingleton.getInstance().getID();
        final String playerName = getPlayerNameById(clientID);

        if (!isSelf) {
            //    appendChatMessage("[INFO] Spieler " + playerName + " hat eine Karte ausgewählt.");
            return;
        }
        // Self && !filled so self and emptied
        if (!filled) {
            if (manuallyClearedSlots.remove(register)) {
                appLogger.debug("Ignoring register {} deselection - user triggered.", register);
                return;
            }

            if (swapProtectedSlots.remove(register)) {
                appLogger.debug("Ignoring register {} deselection - part of swap.", register);
                return;
            }

            // for server forced removal
            if (register >= 0 && register < registerBox.getChildren().size()) {
                Node boxNode = registerBox.getChildren().get(register);
                if (boxNode instanceof VBox vbox) {
                    StackPane pane = (StackPane) vbox.getChildren().get(1);
                    pane.getChildren().clear();
                }
            }

            String removedCardName = registerState[register];
            if (removedCardName != null && !isCardInAnyRegisterSlot(removedCardName)) {
                ImageView cardView = createClickableCard(removedCardName);
                cardView.setUserData(removedCardName + "#" + UUID.randomUUID()); // simulate unique tag
                handCardBox.getChildren().add(cardView);
                appLogger.info("Manually restored card {} to hand after deselection.", removedCardName);
            }

            registerState[register] = null;

            // No need to update the hand manually — will be handled by displayHandCards
            appendChatMessage("[INFO] Deine Karte aus Register " + (register + 1) + " wurde entfernt.");
            appLogger.info("Card removed from register slot {} by server (isSelf).", register);
        }
    }

    private boolean isCardInAnyRegisterSlot(String cardName) {
        for (int i = 0; i < registerBox.getChildren().size(); i++) {
            StackPane pane = getRegisterPane(i);
            if (pane != null && !pane.getChildren().isEmpty()) {
                Node node = pane.getChildren().get(0);
                if (node instanceof ImageView view && view.getUserData() instanceof String data) {
                    if (data.equals(cardName)) return true;
                }
            }
        }
        return false;
    }

    private String getPlayerNameById(int id) {
        // Attempt to retrieve from ClientSingleton (via clientID)
        String name = ClientSingleton.getInstance().getUsernames().getByKeyOrDefault(id, null);
        if (name != null && !name.isBlank()) {
//            logger.info("getPlayerNameById({}): found in usernames as clientID: {}", id, name);
            return name;
        }

        // Try to retrieve from robotIDToPlayerName (via robotID)
        name = robotIDToPlayerName.get(id);
        if (name != null && !name.isBlank()) {
//            logger.info("getPlayerNameById({}): found in robotIDToPlayerName: {}", id, name);
            return name;
        }

        // Try reverse lookup from clientToRobotID to see if it is robotID.
        for (Map.Entry<Integer, Integer> entry : clientToRobotID.entrySet()) {
            if (entry.getValue() == id) {
                int clientID = entry.getKey();
                name = ClientSingleton.getInstance().getUsernames().getByKeyOrDefault(clientID, null);
                if (name != null && !name.isBlank()) {
//                    logger.info("getPlayerNameById({}): found through reverse lookup as robotID -> clientID {}: {}",
//                            id, clientID, name);
                    robotIDToPlayerName.put(id, name);
                    return name;
                }
            }
        }

        // Try to find it in recipientBox
        for (PlayerEntry entry : recipientBox.getItems()) {
            if (entry.getClientID() == id && entry.getName() != null && !entry.getName().isBlank()) {
//                logger.info("getPlayerNameById({}): found in recipientBox as clientID: {}", id, entry.getName());
                ClientSingleton.getInstance().getUsernames().put(id, entry.getName());
                return entry.getName();
            }

            Integer robotID = clientToRobotID.get(entry.getClientID());
            if (robotID != null && robotID == id && entry.getName() != null && !entry.getName().isBlank()) {
//                logger.info("getPlayerNameById({}): found in recipientBox as robotID: {}", id, entry.getName());
                robotIDToPlayerName.put(id, entry.getName());
                return entry.getName();
            }
        }

        // Try to find it in LobbyController.
        LobbyController lobbyController = ControllerRegistry.getLobbyController();
        if (lobbyController != null) {
            for (PlayerEntry entry : lobbyController.getPlayers()) {
                if (entry.getClientID() == id && entry.getName() != null && !entry.getName().isBlank()) {
//                    logger.info("getPlayerNameById({}): found in lobby players as clientID: {}", id, entry.getName());
                    ClientSingleton.getInstance().getUsernames().put(id, entry.getName());
                    return entry.getName();
                }

                if (entry.getFigure() == id && entry.getName() != null && !entry.getName().isBlank()) {
//                    logger.info("getPlayerNameById({}): found in lobby players as robotID/figure: {}", id, entry.getName());
                    robotIDToPlayerName.put(id, entry.getName());
                    return entry.getName();
                }
            }
        }

        logger.warn("getPlayerNameById({}): fallback to id!", id);
//        for (PlayerEntry entry : recipientBox.getItems()) {
//            if (entry.getClientID() == id) {
//                return entry.getName();
//            }
//        }
        return "Spieler " + id;
    }


    /**
     * Hebt hervor, dass ein Spieler sein Programm abgeschlossen hat.
     *
     * @param clientID Die ID des Spielers
     */
    public void markPlayerReady(int clientID) {
        boolean isSelf = clientID == ClientSingleton.getInstance().getID();
        String playerName = getPlayerNameById(clientID);

        if (isSelf) {
            appendChatMessage("[INFO] Du hast dein Programm fertiggestellt.");
        } else {
            appendChatMessage("[INFO] " + playerName + " hat sein Programm fertiggestellt.");
        }
    }


    /**
     * Startet einen Countdown-Timer von 30 Sekunden und aktualisiert dabei ein Label in der Benutzeroberfläche.
     *
     * <p>Die Methode zeigt die verbleibende Zeit im `timerLabel` an und blendet das Label aus,
     * wenn der Countdown abgelaufen ist. Der Timer wird in 1-Sekunden-Intervallen aktualisiert.</p>
     *
     * <p>Funktionsweise:</p>
     * <ul>
     *   <li>Setzt die verbleibende Zeit (`secondsLeft`) auf 30 Sekunden.</li>
     *   <li>Zeigt das `timerLabel` an und aktualisiert es jede Sekunde.</li>
     *   <li>Stoppt einen eventuell laufenden Timer, bevor ein neuer gestartet wird.</li>
     *   <li>Blendet das `timerLabel` aus, wenn die Zeit abgelaufen ist.</li>
     * </ul>
     *
     * <p>Diese Methode wird verwendet, um zeitgesteuerte Aktionen in der Benutzeroberfläche zu ermöglichen.</p>
     */
    public void startCountdown() {

        secondsLeft = 30;

        timerLabel.setText(String.valueOf(secondsLeft));
        timerLabel.setVisible(true);
        timerLabel.setManaged(true);

        updateCountdownColor(secondsLeft);
        animatePulse(timerLabel);

        if (countdownTimer != null) countdownTimer.stop();

        if (timerContainer != null) {
            ((Pane) timerLabel.getParent()).getChildren().remove(timerContainer);
        }

        double radius = 80;
        double strokeWidth = 10;
        double fullLength = 2 * Math.PI * radius;

        timerCircle = new Circle(radius, Color.TRANSPARENT);
        timerCircle.setStroke(Color.web("#00ffd0"));
        timerCircle.setStrokeWidth(strokeWidth);
        timerCircle.setStrokeType(StrokeType.CENTERED);
        timerCircle.getStrokeDashArray().add(fullLength);
        timerCircle.setStrokeDashOffset(0);


        timerContainer = new StackPane(timerCircle, timerLabel);
        timerContainer.setPrefSize(radius * 2 + 20, radius * 2 + 20);


        timerContainer.setLayoutX(500);
        timerContainer.setLayoutY(100);


        zoomWrapper.getChildren().add(timerContainer);
        StackPane.setAlignment(timerContainer, Pos.CENTER);

        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    secondsLeft--;
                    timerLabel.setText(secondsLeft > 0 ? String.valueOf(secondsLeft) : "GO!");

                    double offset = (fullLength * (30 - secondsLeft)) / 30;
                    timerCircle.setStrokeDashOffset(offset);
                    if (secondsLeft > 19) {
                        timerCircle.setStroke(Color.web("#00ffd0"));
                    } else if (secondsLeft > 9) {
                        timerCircle.setStroke(Color.web("#ffcc00"));
                    } else {
                        timerCircle.setStroke(Color.web("#ff4444"));
                    }

                    animatePulse(timerLabel);
                    updateCountdownColor(secondsLeft);

                    if (secondsLeft <= 0) {
                        countdownTimer.stop();
                        stopCountdownSound();

                        if (blinkTimeline != null) blinkTimeline.stop(); //
                        timerLabel.setOpacity(1);
                        timerLabel.setText("Zeit abgelaufen!");
                        fadeOutCountdown();
                        updateCountdownColor(secondsLeft);
                    }

                })
        );
        countdownTimer.setCycleCount(30);
        playCountdownSound();
        countdownTimer.play();

    }


    private void animatePulse(Label label) {
        ScaleTransition zoom = new ScaleTransition(Duration.millis(200), label);
        zoom.setFromX(1.3);
        zoom.setFromY(1.3);
        zoom.setToX(1.0);
        zoom.setToY(1.0);
        zoom.play();
    }
    private void fadeOutCountdown() {
        FadeTransition fade = new FadeTransition(Duration.seconds(1), timerContainer);
        fade.setFromValue(1.0);
        fade.setToValue(0.0);
        fade.setOnFinished(e -> {
            timerLabel.setVisible(false);
            timerLabel.setManaged(false);
            ((Pane) timerLabel.getParent()).getChildren().remove(timerContainer);
        });
        fade.play();
    }
    private void updateCountdownColor(int secondsLeft) {
        if (blinkTimeline != null) {
            blinkTimeline.stop();
            timerLabel.setOpacity(1);
        }

        String styleBase = "-fx-font-size: 60px;" +
                "-fx-font-weight: bold;" +
                "-fx-effect: dropshadow(gaussian, black, 15, 0.7, 0, 0);";

        if (secondsLeft > 19) {
            timerLabel.setStyle("-fx-text-fill: #00ffd0;" + styleBase);
        } else if (secondsLeft > 9) {
            timerLabel.setStyle("-fx-text-fill: #ffcc00;" + styleBase);
        } else {
            timerLabel.setStyle("-fx-text-fill: #ff4444;" + styleBase);

            if (secondsLeft <= 5) {
                timerLabel.setOpacity(1);

            }
        }
    }

    private AudioClip countdownClip;

    private void playCountdownSound() {
        try {
            countdownClip = new AudioClip(getClass().getResource("/audio/timer.wav").toExternalForm());
            countdownClip.setCycleCount(1);
            countdownClip.play();

            PauseTransition stopSound = new PauseTransition(Duration.seconds(20));
            stopSound.setOnFinished(e -> stopCountdownSound());
            stopSound.play();

        } catch (Exception e) {
            System.err.println("Countdown-Sound konnte nicht geladen werden: " + e.getMessage());
        }
    }





    private void stopCountdownSound() {
        if (countdownClip != null && countdownClip.isPlaying()) {
            System.out.println("stop.");
            countdownClip.stop();
        }
    }


    /**
     * Zeigt an, dass der Timer abgelaufen ist, und markiert Spieler, die zu langsam waren.
     *
     * <p>Diese Methode wird aufgerufen, wenn der Countdown-Timer endet. Sie informiert die Benutzer
     * über das Ende des Timers und markiert Spieler, die ihre Aktionen nicht rechtzeitig abgeschlossen haben.</p>
     */
    public void showTimerEnded(List<Integer> slowPlayers) {
        if (!timerLabel.isVisible()) {
            appLogger.info("Timer ended, but not displayed since timer was already hidden.");
            return;
        }

        fadeOutCountdown();
        stopCountdownSound();
        appendChatMessage("[TIMER] Zeit ist abgelaufen.");

        if (slowPlayers != null && !slowPlayers.isEmpty()) {
            List<String> slowNames = new ArrayList<>();
            for (Integer id : slowPlayers) {
                slowNames.add(getPlayerNameById(id));
            }
            appendChatMessage("Folgende Spieler waren zu langsam: " + slowNames);

            for (PlayerEntry entry : recipientBox.getItems()) {
                if (slowPlayers.contains(entry.getClientID())) {
                    entry.setName("✖ " + entry.getName());
                }
            }
            // Optional: Auswahl zurücksetzen, falls vorheriger Eintrag jetzt verändert wurde
            recipientBox.getSelectionModel().clearSelection();
        }
    }

    /**
     * Zeigt die vom Spieler bestätigten Karten im Register oder Kartenbereich.
     *
     * @param cards Liste der Kartennamen
     */
    public void displayConfirmedCards(List<String> cards) {
        handCardBox.getChildren().clear();

        for (String card : cards) {
            String imagePath = "/assets/cards/" + card.toLowerCase() + ".png";

            Image cardImage;
            try {
                cardImage = new Image(getClass().getResourceAsStream(imagePath));
            } catch (Exception e) {
                cardImage = new Image(getClass().getResourceAsStream("/assets/cover_card.png"));
            }

            ImageView cardView = new ImageView(cardImage);
            cardView.setFitWidth(80);
            cardView.setFitHeight(120);
            cardView.setPreserveRatio(false);
            cardView.setSmooth(true);

            handCardBox.getChildren().add(cardView);
        }
    }

    /**
     * Displays the name of a played card below the board.
     *
     * @param clientID ID of the player
     * @param cardName Name of the played card
     */
    public void showActiveCard(int clientID, String cardName) {
        Label label = new Label("Player " + clientID + ": " + cardName);
        label.setStyle("-fx-font-size: 14px; -fx-text-fill: white;");
        handCardBox.getChildren().add(label);
    }

    /**
     * Basic placeholder animation for a robot action.
     *
     * @param clientID ID of the robot
     * @param cardName The card causing the action
     */
    public void animateRobotAction(int clientID, String cardName) {
        int x = 5;
        int y = 5;

        Label arrow = new Label("→");
        arrow.setStyle("-fx-font-size: 30px; -fx-text-fill: red;");

        StackPane cell = getCellAt(x, y);
        if (cell != null) {
            cell.getChildren().add(arrow);

            PauseTransition pause = new PauseTransition(javafx.util.Duration.seconds(1));
            pause.setOnFinished(e -> cell.getChildren().remove(arrow));
            pause.play();
        }
    }

    /**
     * Holt die StackPane für ein Spielfeldfeld bei (x, y).
     *
     * @param x Spaltenindex
     * @param y Zeilenindex
     * @return StackPane der Zelle oder null, wenn nicht gefunden
     */
    private StackPane getCellAt(int x, int y) {
        if (x < 0 || y < 0) {
            errorLogger.error("Ungültiger Zugriff in getCellAt({}, {}): Koordinaten negativ", x, y);
            return null;
        }

        for (Node node : gameBoardPane.getChildren()) {
            Integer col = GridPane.getColumnIndex(node);
            Integer row = GridPane.getRowIndex(node);

            // Falls col/row null bei nicht gesetztem Index), skip
            if (col != null && row != null && col == x && row == y) {
                return (StackPane) node;
            }
        }

        errorLogger.warn("Kein StackPane gefunden bei Koordinaten ({}, {})", x, y);
        return null;
    }


    /**
     * Ersetzt eine Karte in einem bestimmten Register
     *
     * @param clientID ID des Spielers
     * @param register Register-Slot (0–4)
     * @param newCard  Name der neuen Karte
     */
    public void replaceCardInRegister(int clientID, int register, String newCard) {
        appendChatMessage("[INFO] Spieler " + clientID + " ersetzt Karte in Register " + register + " durch: " + newCard);

    }

    /**
     * Bewegt den Roboter eines Spielers auf das Feld (x, y).
     *
     * @param clientID Die ID des Spielers
     * @param x        Die Zielspalte
     * @param y        Die Zielzeile
     */
    public void moveRobotTo(int clientID, int x, int y) {
        messageLogger.info("clientToRobotID = {}", clientToRobotID);
        messageLogger.info("clientID = {}", clientID);

        try {
            Integer robotID = clientToRobotID.get(clientID);
//            appLogger.info("looking up for clientID {}", clientID);
//            appLogger.info("clientToRobotID: {}", clientToRobotID.toString());
            appLogger.info("Robot {} moved to ({}, {})", robotID, x, y);

            final String direction = robotDirections.get(clientID);
            if (direction == null)
                throw new IllegalStateException("Direction for clientID " + clientID + " not found in robotDirections map.");
            final String imagePath = "/assets/robots/robot_0" + robotID + "_" + direction + ".png";
            final Image robotImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            ImageView robotView = new ImageView(robotImg);
            robotView.setFitWidth(40);
            robotView.setFitHeight(40);
            robotView.setPreserveRatio(true);
            robotView.setUserData("robot");

            // REMOVE old robot image at previous position (if any)
            Position oldPos = robotPositions.get(clientID);
            if (oldPos != null) {
                StackPane oldCell = getCellAt(oldPos.x(), oldPos.y());
                if (oldCell != null) {
                    oldCell.getChildren().removeIf(n -> n instanceof ImageView && "robot".equals(n.getUserData()));
                }
            }

            // REMOVE robot image at new position to avoid stacking
            StackPane newCell = getCellAt(x, y);
            if (newCell != null) {
                newCell.getChildren().removeIf(n -> n instanceof ImageView && "robot".equals(n.getUserData()));
                newCell.getChildren().add(robotView);
            }

            // Update tracked position
            robotPositions.put(clientID, new Position(x, y));

            String playerName = getPlayerNameById(clientID);
            appendChatMessage("[BEWEGUNG] Spieler " + playerName + " wurde nach (" + x + ", " + y + ") bewegt.");
        } catch (Exception e) {
            appLogger.error("Roboterbild konnte nicht geladen werden für Spieler {}", clientID);
            e.printStackTrace();
        }
    }

    /**
     * Dreht den Roboter eines Spielers in eine bestimmte Richtung.
     *
     * @param clientID Die ID des Spielers
     * @param rotation "clockwise" oder "counterclockwise"
     */
    public void rotateRobot(int clientID, String rotation) {
        syncPlayerNames();
        Position pos = robotPositions.get(clientID);
        if (pos == null) {
            String playerName = getPlayerNameById(clientID);
            appendChatMessage("[FEHLER] Position für Spieler " + playerName + " nicht gefunden.");
            return;
        }

        StackPane cell = getCellAt(pos.x(), pos.y());
        if (cell != null) {
            for (Node node : cell.getChildren()) {
                if (node instanceof ImageView img && "robot".equals(img.getUserData())) {
                    final String currentDirection = robotDirections.get(clientID);
                    if (currentDirection == null)
                        throw new IllegalStateException("Direction for clientID " + clientID + " not found in robotDirections map. (called in rotateRobot)");

                    final String newDirection = switch (rotation.toLowerCase()) {
                        case "clockwise" -> rotateClockwise(currentDirection);
                        case "counterclockwise" -> rotateCounterClockwise(currentDirection);
                        case "uturn" -> rotateClockwise(rotateClockwise(currentDirection));
                        default -> throw new IllegalArgumentException("Invalid rotation: " + rotation);
                    };

                    robotDirections.put(clientID, newDirection);

                    Integer robotID = clientToRobotID.get(clientID);
                    if (robotID == null)
                        throw new IllegalStateException("Robot ID for clientID " + clientID + " not found in clientToRobotID map.");

                    final String imagePath = "/assets/robots/robot_0" + robotID + "_" + newDirection + ".png";

                    // Remove old image
                    cell.getChildren().remove(node);

                    // Add new image
                    Image robotImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
                    ImageView newRobot = new ImageView(robotImg);
                    newRobot.setFitWidth(40);
                    newRobot.setFitHeight(40);
                    newRobot.setPreserveRatio(true);
                    newRobot.setUserData("robot");
                    cell.getChildren().add(newRobot);

                    //Insert direction indicator
                    Label directionIndicator = new Label(switch (newDirection) {
                        case "top" -> "↑";
                        case "right" -> "→";
                        case "bottom" -> "↓";
                        case "left" -> "←";
                        default -> "?";
                    });

                    directionIndicator.setStyle(
                            "-fx-font-size: 16px; " +
                                    "-fx-font-weight: bold; " +
                                    "-fx-text-fill: #ffcc00; " +
                                    "-fx-background-color: linear-gradient(#303030, #505050); " +
                                    "-fx-background-radius: 5px; " +
                                    "-fx-border-color: #ffcc00; " +
                                    "-fx-border-width: 1px; " +
                                    "-fx-border-radius: 5px; " +
                                    "-fx-padding: 3px 6px; " +
                                    "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.6), 5, 0, 0, 1);"
                    );
                    directionIndicator.setTranslateY(-25);
                    cell.getChildren().add(directionIndicator);
                    ScaleTransition appear = new ScaleTransition(Duration.millis(200), directionIndicator);
                    appear.setFromX(0.5);
                    appear.setFromY(0.5);
                    appear.setToX(1.0);
                    appear.setToY(1.0);
                    appear.play();
                    PauseTransition delay = new PauseTransition(Duration.seconds(1.5));
                    delay.setOnFinished(e -> {
                        FadeTransition fade = new FadeTransition(Duration.seconds(1), directionIndicator);
                        fade.setFromValue(1.0);
                        fade.setToValue(0.0);
                        fade.setOnFinished(event -> cell.getChildren().remove(directionIndicator));
                        fade.play();
                    });
                    delay.play();

                    String playerName = getPlayerNameById(clientID);
                    appendChatMessage("[DREHUNG] Spieler " + playerName + " dreht sich " + rotation + ".");
                    break;
                }
            }
        } else {
            appendChatMessage("[FEHLER] Kein Zellen-StackPane an Position (" + pos.x() + ", " + pos.y() + ") gefunden.");
        }
    }

    private String rotateClockwise(String dir) {
        return switch (dir) {
            case "top" -> "right";
            case "right" -> "bottom";
            case "bottom" -> "left";
            case "left" -> "top";
            default -> dir;
        };
    }

    private String rotateCounterClockwise(String dir) {
        return switch (dir) {
            case "top" -> "left";
            case "left" -> "bottom";
            case "bottom" -> "right";
            case "right" -> "top";
            default -> dir;
        };
    }

    public void setRobotPosition(int clientID, Position position) {
        robotPositions.put(clientID, position);
    }

    public Position getRobotPosition(int clientID) {
        return robotPositions.get(clientID);
    }

    /**
     * Spielt eine einfache Animation basierend auf dem Animationstyp.
     *
     * @param type Typ der Animation ( "Movement", "Clockwise", "Checkpoint")
     */
    public void playAnimation(String type) {
        //appendChatMessage("[ANIMATION] " + type + " ausgeführt.");
        if ("PlayerShooting".equals(type)) {
            int meineClientID = ClientSingleton.getInstance().getID();
            spieleLaserAnimation(meineClientID);
        } else {
            appendChatMessage("[ANIMATION] " + type + " ausgeführt.");
        }
    }


    /**
     * Zeigt eine visuelle Nachricht oder Platzhalter an, dass ein Spieler rebootet wurde.
     *
     * @param clientID ID des Spielers
     */
    public void showReboot(int clientID) {
        // Optional: Spielername ermitteln – hier als Platzhalter
        final String playerName = getPlayerNameById(clientID);

        // Nachricht im Chat anzeigen
        appendChatMessage("[REBOOT] Spieler " + playerName + " wurde rebootet.");

        // TODO: Hier könnte man auch eine Reboot-Animation zeigen
        // oder ein Symbol auf dem Spielfeld darstellen
    }

    /**
     * Zeigt eine visuelle Reboot-Ausrichtung für den Spieler an.
     *
     * @param direction Die vom Spieler gewählte Richtung ("up", "down", "left", "right")
     */
    public void showRebootDirection(String direction) {
        appendChatMessage("[INFO] Reboot-Richtung: " + direction);

        // Optional: Richtungssymbol visuell anzeigen (Platzhalter-Animation)
        Label arrow = new Label(switch (direction.toLowerCase()) {
            case "top" -> "↑";
            case "bottom" -> "↓";
            case "left" -> "←";
            case "right" -> "→";
            default -> "?";
        });

        arrow.setStyle("-fx-font-size: 28px; -fx-text-fill: blue;");
        StackPane centerTile = getCellAt(5, 5); // Beispielposition – später echte Roboterposition verwenden

        if (centerTile != null) {
            centerTile.getChildren().add(arrow);

            // Pfeil nach kurzer Zeit entfernen
            PauseTransition pause = new PauseTransition(Duration.seconds(1.5));
            pause.setOnFinished(e -> centerTile.getChildren().remove(arrow));
            pause.play();
        }
    }

    public void askRebootDirection(Consumer<String> callback) {
        // Öffne ein einfaches Dialog-Fenster mit 4 Buttons oder ChoiceBox
        List<String> directions = List.of("top", "right", "bottom", "left");

        ChoiceDialog<String> dialog = new ChoiceDialog<>("top", directions);
        dialog.setTitle("Roboter neu ausrichten");
        dialog.setHeaderText("Wähle eine neue Ausrichtung für deinen Roboter");
        dialog.setContentText("Ausrichtung:");

        Optional<String> result = dialog.showAndWait();
        if (result.isPresent()) {
            callback.accept(result.get());
        } else {
            // Wenn der Dialog geschlossen wurde oder abgebrochen → Standard
            callback.accept("top");
        }
    }


    /**
     * Zeigt die Energieänderung für einen bestimmten Spieler an.
     *
     * @param clientID Die Client-ID des Spielers
     * @param energy   Neue Energieanzahl
     * @param source   Quelle der Energie ("EnergySpace", "Laser")
     */
    public void showEnergyChange(int clientID, int energy, String source) {
        String playerName = getPlayerNameById(clientID);
        appendChatMessage("[ENERGIE] Spieler " + playerName
                + " hat jetzt " + energy + " ⚡ (Quelle: " + source + ")");
        updateEnergyDisplay(energy);
    }

    /**
     * Zeigt an, dass ein Spieler einen Checkpoint erreicht hat.
     *
     * @param clientID         Die ID des Spielers
     * @param checkpointNumber Die Nummer des erreichten Checkpoints
     */
    public void showCheckpointReached(int clientID, int checkpointNumber) {
        String playerName = getPlayerNameById(clientID);
        appendChatMessage("[ZIEL] Spieler " + playerName + " hat Checkpoint #" + checkpointNumber + " erreicht! 🏁");

        // Optional: UI-Markierung im Spielfeld oder Spieleranzeige
    }

    /**
     * Zeigt eine Sieges- oder Niederlageanzeige mit grünem Hintergrund und Button zum Hauptmenü.
     *
     * @param isWinner       true, wenn der Spieler selbst gewonnen hat
     * @param winnerClientId Client-ID des Gewinner-Spielers
     */
    public void showGameResult(boolean isWinner, int winnerClientId) {
        Platform.runLater(() -> {
            String playerName = getPlayerNameById(winnerClientId);
            String message = isWinner
                    ? " Glückwunsch, " + playerName + " hat das Rennen gemeistert! 🏆"
                    : "🔩 Du hast deine Schrauben verloren. " + playerName + " dominiert das Spielfeld!";

            appendChatMessage("[SPIELENDE] " + message);

            int robotId = clientToRobotID.getOrDefault(winnerClientId, 1);
            String imagePath = "/assets/robots/robot_0" + robotId + "_right.png";

            ImageView robotImage = new ImageView();
            try {
                Image robotImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
                robotImage.setImage(robotImg);
            } catch (Exception e) {
                appLogger.error("Roboterbild konnte nicht geladen werden: " + imagePath);
            }

            robotImage.setFitWidth(180);
            robotImage.setPreserveRatio(true);

            TranslateTransition bounce = new TranslateTransition(Duration.seconds(0.6), robotImage);
            bounce.setByY(-15);
            bounce.setCycleCount(Animation.INDEFINITE);
            bounce.setAutoReverse(true);
            bounce.play();

            Label title = new Label(message);
            title.getStyleClass().add("status-label");
            title.setWrapText(true);
            title.setMaxWidth(600);
            title.setAlignment(Pos.CENTER);

            Button backToMenuBtn = new Button("Zurück zum Hauptmenü");
            backToMenuBtn.getStyleClass().add("senden-button");
            backToMenuBtn.setOnAction(e -> {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/lmu/dbs/ifi/sep25/GameView.fxml"));
                    Parent menuRoot = loader.load();
                    Scene menuScene = new Scene(menuRoot, 800, 600);
                    menuScene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

                    Stage stage = (Stage) root.getScene().getWindow();
                    stage.setScene(menuScene);
                } catch (IOException ex) {
                    appLogger.error("Hauptmenü konnte nicht geladen werden.", ex);
                    displayErrorAlert("Fehler", "Das Hauptmenü konnte nicht geladen werden.");
                }
            });

            VBox layout = new VBox(30, title, robotImage, backToMenuBtn);
            layout.setAlignment(Pos.CENTER);
            layout.getStyleClass().add("vbox");

            StackPane rootPane = new StackPane(layout);

            String backgroundStyle = isWinner
                    ? "-fx-background-color: linear-gradient(to bottom, #c8f7c5, #7ddf78);"
                    : "-fx-background-color: linear-gradient(to bottom, #f8d7da, #f5c6cb);";
            rootPane.setStyle(backgroundStyle);

            Scene scene = new Scene(rootPane, 800, 600);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());

            Stage stage = (Stage) root.getScene().getWindow();
            stage.setScene(scene);

            FadeTransition fadeIn = new FadeTransition(Duration.millis(700), rootPane);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
    }



    public void showInstructionDialog() {
        Label title = new Label("Programmierphase");
        title.setStyle("-fx-text-fill: #00ffd0; -fx-font-size: 20px; -fx-font-weight: bold;");

        Label info = new Label("Ziehe 5 Karten in die Registerfelder, um deinen Roboter zu programmieren.");
        info.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox content = new VBox(15, title, info);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: rgba(20,20,30,0.95); -fx-padding: 30; -fx-background-radius: 12;");

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Programmierphase");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().lookupButton(ButtonType.OK).setStyle(
                "-fx-background-color: #00ffd0; -fx-text-fill: black; -fx-font-weight: bold;");

        dialog.showAndWait();
    }


// === DAMAGE ===

    /**
     * Zeigt dem Spieler die gezogenen Schadenskarten an.
     *
     * <p>Diese Methode wird aufgerufen, wenn der Server dem Spieler automatisch Schadenskarten zuweist.
     * Sie zeigt die entsprechenden Kartengrafiken im Handkartenbereich an.</p>
     *
     * @param cards Liste der Schadenskarten ["spam", "worm", "trojan_horse"]
     */
    public void showDrawnDamageCards(List<String> cards) {
        handCardBox.getChildren().clear();

        for (String card : cards) {
            String path = "/assets/cards/damage_cards/" + card.toLowerCase() + ".png";
            Image img;
            try {
                img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
            } catch (Exception e) {
                appLogger.warn("Fehlendes Schadensbild: {}", card);
                img = new Image(getClass().getResourceAsStream("/assets/cover.png"));
            }

            ImageView view = new ImageView(img);
            view.setFitWidth(80);
            view.setFitHeight(120);
            view.setPreserveRatio(true);
            view.setSmooth(true);

            handCardBox.getChildren().add(view);
        }

        appendChatMessage("[INFO] Du hast " + cards.size() + " Schadenskarten erhalten.");
    }

    public void promptDamageCardSelection(int count, List<String> options, java.util.function.Consumer<List<String>> callback) {
        handCardBox.getChildren().clear();
        registerBox.getChildren().clear();

        List<String> selected = new ArrayList<>();

        for (String name : options) {
            String path = "/assets/cards/damage_cards/" + name.toLowerCase() + ".png";
            Image img;
            try {
                img = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
            } catch (Exception e) {
                img = new Image(getClass().getResourceAsStream("/assets/cover.png"));
            }

            ImageView view = new ImageView(img);
            view.setFitWidth(80);
            view.setFitHeight(120);
            view.setPreserveRatio(true);
            view.setSmooth(true);

            view.setOnMouseClicked(event -> {
                if (selected.contains(name)) {
                    selected.remove(name);
                    view.setStyle("");
                } else if (selected.size() < count) {
                    selected.add(name);
                    view.setStyle("-fx-effect: dropshadow(gaussian, red, 12, 0.5, 0, 0);");
                }

                if (selected.size() == count) {
                    handCardBox.getChildren().forEach(n -> n.setOnMouseClicked(null));

                    PauseTransition wait = new PauseTransition(Duration.millis(500));
                    wait.setOnFinished(e -> callback.accept(selected));
                    wait.play();
                }
            });

            handCardBox.getChildren().add(view);
        }

        appendChatMessage("[INFO] Wähle " + count + " Schadenskarte(n) durch Klick.");
    }

    /**
     * Erstellt eine Karte, die per Klick ins nächste freie Registerfeld gelegt wird.
     * Drag-and-drop ist deaktiviert.
     *
     * @param cardName Name der Karte
     * @return ImageView mit der Karte
     */
    private ImageView createDraggableCard(String cardName) {
        String imagePath = "/assets/cards/" + cardName.toLowerCase() + ".png";
        Image img;
        try {
            img = new Image(getClass().getResourceAsStream(imagePath));
        } catch (Exception e) {
            img = new Image(getClass().getResourceAsStream("/assets/cover.png"));
            logger.warn("Bild für Karte '{}' nicht gefunden, Platzhalter wird verwendet.", cardName);
        }

        ImageView view = new ImageView(img);
        view.setFitWidth(60);
        view.setFitHeight(90);
        view.setPreserveRatio(true);
        view.setSmooth(true);
        view.setUserData(cardName);

        // Hover-Effekt
        DropShadow shadow = new DropShadow();
        shadow.setRadius(10);
        shadow.setColor(javafx.scene.paint.Color.ORANGE);

        view.setOnMouseEntered(e -> {
            view.setScaleX(1.1);
            view.setScaleY(1.1);
            view.setEffect(shadow);
        });

        view.setOnMouseExited(e -> {
            view.setScaleX(1.0);
            view.setScaleY(1.0);
            view.setEffect(null);
        });

        //  Klick-Ereignis → fügt Karte ins nächste freie Registerfeld ein
        view.setOnMouseClicked(e -> {
            logger.info("Karte '{}' wurde angeklickt.", cardName);
            addCardToNextEmptyRegister(cardName);
        });

        return view;
    }

    private void addCardToNextEmptyRegister(String cardName) {
    }

    /**
     * Handle the situation where a robot falls off the board.
     *
     * @param clientID The client ID corresponding to the robot that fell off the board.
     */
    public void handleRobotFellOffBoard(int clientID) {
        Integer robotID = clientToRobotID.getOrDefault(clientID, -1);
        appLogger.info("Robot {} (clientID {}) fell off the board", robotID, clientID);

        Position oldPos = robotPositions.get(clientID);
        if (oldPos != null) {
            StackPane oldCell = getCellAt(oldPos.x(), oldPos.y());
            if (oldCell != null) {
                oldCell.getChildren().removeIf(n -> n instanceof ImageView && "robot".equals(n.getUserData()));
            }
        }

        robotPositions.put(clientID, new Position(-1, -1));

        String playerName = getPlayerNameById(clientID);
        appendChatMessage("[INFO] Spieler " + playerName + " fiel vom Spielbrett herunter!");

    }

    /**
     * Display an error message dialog box.
     *
     * @param title   Dialog box title.
     * @param message Error message.
     */
    public void displayErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);

            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #b00020;");

            Label messageLabel = new Label(message);
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-font-size: 13px;");

            Button okButton = new Button("OK");
            okButton.setDefaultButton(true); // ENTER works
            okButton.setStyle("""
                        -fx-background-color: #e0e0e0;
                        -fx-text-fill: black;
                        -fx-font-size: 13px;
                        -fx-padding: 6 14 6 14;
                        -fx-background-radius: 6;
                        -fx-border-radius: 6;
                        -fx-cursor: hand;
                        -fx-transition: all 0.2s ease-in-out;
                    """);

            okButton.setOnMouseEntered(_ -> {
                okButton.setScaleX(1.1);
                okButton.setScaleY(1.1);
            });

            okButton.setOnMouseExited(_ -> {
                okButton.setScaleX(1.0);
                okButton.setScaleY(1.0);
            });

            okButton.setOnAction(_ -> dialog.close());

            VBox layout = new VBox(12, titleLabel, messageLabel, okButton);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(20));
            layout.setStyle("""
                        -fx-background-color: white;
                        -fx-background-radius: 12;
                        -fx-border-radius: 12;
                        -fx-border-color: #b00020;
                        -fx-border-width: 2;
                    """);

            Scene scene = new Scene(layout);
            scene.setFill(Color.TRANSPARENT);

            // ESC or SPACE to close
            scene.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ESCAPE, SPACE, ENTER -> dialog.close();
                }
            });

            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();
            scene.getRoot().requestFocus(); // ensures key events are registered
        });
    }

    /**
     * Highlight the register slot to indicate an error.
     *
     * @param registerSlot The index of the register slot to be highlighted (0-4).
     */
    public void highlightRegisterSlot(int registerSlot) {
        if (registerSlot < 0 || registerSlot >= 5 || registerBox == null) return;

        Platform.runLater(() -> {
            if (registerBox.getChildren().size() > registerSlot) {
                Node node = registerBox.getChildren().get(registerSlot);
                if (node instanceof VBox vbox && vbox.getChildren().size() > 1) {
                    Node slotNode = vbox.getChildren().get(1);
                    if (slotNode instanceof StackPane pane) {
                        String originalStyle = pane.getStyle();
                        String highlightStyle = originalStyle + "; -fx-border-color: red; -fx-border-width: 3px; " +
                                "-fx-effect: dropshadow(gaussian, #ff0000, 10, 0.5, 0, 0);";

                        pane.setStyle(highlightStyle);

                        // Cancel existing transition if one is running
                        PauseTransition existing = activeHighlights.get(registerSlot);
                        if (existing != null) existing.stop();

                        // Start a new one
                        PauseTransition pause = new PauseTransition(Duration.seconds(2));
                        pause.setOnFinished(e -> {
                            pane.setStyle(originalStyle);
                            activeHighlights.remove(registerSlot);
                        });

                        activeHighlights.put(registerSlot, pause);
                        pause.play();
                    }
                }
            }
        });
    }

    private void updateConfirmButtonVisibility() {
        long filledSlots = registerBox.getChildren().stream()
                .filter(n -> n instanceof VBox)
                .map(n -> ((VBox) n).getChildren().get(1)) // StackPane
                .filter(n -> n instanceof StackPane)
                .map(n -> (StackPane) n)
                .filter(p -> !p.getChildren().isEmpty())
                .count();

        confirmSelectionButton.setVisible(filledSlots == 5);
        confirmSelectionButton.setManaged(filledSlots == 5);
    }

    /**
     * Mischt die Handkarten visuell neu. Die Reihenfolge wird zufällig geändert.
     * Dies beeinflusst nicht die Spielmechanik, da die Karten individuell gewählt werden.
     */
    private void shuffleHandCards() {
        ObservableList<Node> cards = handCardBox.getChildren();

        if (cards.size() <= 1) return;

        List<Node> shuffled = new ArrayList<>(cards);
        Collections.shuffle(shuffled);

        FadeTransition fadeOut = new FadeTransition(Duration.millis(300), handCardBox);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);

        fadeOut.setOnFinished(e -> {
            handCardBox.getChildren().setAll(shuffled);

            for (Node card : shuffled) {
                card.setOpacity(0);
                card.setRotate(0);
                card.setTranslateX(0);
                card.setTranslateY(0);
            }

            // Delay-Effekt
            for (int i = 0; i < shuffled.size(); i++) {
                Node card = shuffled.get(i);
                int delayMs = i * 120;

                PauseTransition delay = new PauseTransition(Duration.millis(delayMs));
                delay.setOnFinished(ev -> {
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(300), card);
                    fadeIn.setFromValue(0);
                    fadeIn.setToValue(1);

                    TranslateTransition jump = new TranslateTransition(Duration.millis(400), card);
                    jump.setFromY(-40 + Math.random() * -20);
                    jump.setToY(0);
                    jump.setInterpolator(Interpolator.EASE_OUT);

                    RotateTransition rotate = new RotateTransition(Duration.millis(400), card);
                    rotate.setFromAngle(-15 + Math.random() * 30);
                    rotate.setToAngle(0);
                    rotate.setInterpolator(Interpolator.EASE_OUT);

                    ParallelTransition fullAnim = new ParallelTransition(fadeIn, jump, rotate);
                    fullAnim.play();
                });

                delay.play();
            }

            handCardBox.setOpacity(1.0);
        });

        playShuffleSound();
        fadeOut.play();

        // Informiere den Spieler
        //appendChatMessage("[INFO] Deine Handkarten wurden gemischt.");
    }
    private void playShuffleSound() {
        try {
            AudioClip clip = new AudioClip(getClass().getResource("/audio/shuffle-cards.wav").toExternalForm());
            clip.play();
        } catch (Exception e) {
            System.err.println("Fehler beim Abspielen des Shuffle-Sounds: " + e.getMessage());
        }
    }


    /**
     * Synchronize the current player's nickname mapping.
     * This method ensures that the latest username mapping is obtained from ClientSingleton and
     * synchronized with the data in recipientBox.
     */
    public void syncPlayerNames() {
        LobbyController lobbyController = ControllerRegistry.getLobbyController();
        if (lobbyController != null) {
            for (PlayerEntry entry : lobbyController.getPlayers()) {
                int clientID = entry.getClientID();
                int figure = entry.getFigure();
                String name = entry.getName();

                if (name != null && !name.isBlank()) {
                    ClientSingleton.getInstance().getUsernames().put(clientID, name);
                    robotIDToPlayerName.put(figure, name);
                    clientToRobotID.put(clientID, figure);
                }
            }
        }

        for (PlayerEntry entry : recipientBox.getItems()) {
            int clientID = entry.getClientID();
            int figure = entry.getFigure();
            String name = entry.getName();

            if (name != null && !name.isBlank()) {
                ClientSingleton.getInstance().getUsernames().put(clientID, name);
                robotIDToPlayerName.put(figure, name);
                clientToRobotID.put(clientID, figure);
            }
        }
    }


    private void showDragAndDropInfoPopup() {
        Platform.runLater(() -> {
            // Label mit Info-Text und Styling
            Label infoLabel = new Label("Ziehe 5 Karten per Drag & Drop in die Registerfelder,\num deinen Roboter zu programmieren.");
            infoLabel.getStyleClass().add("status-label");
            infoLabel.setWrapText(true);
            infoLabel.setMaxWidth(280);
            infoLabel.setStyle(
                    "-fx-background-color: rgba(20, 20, 30, 0.85);" +
                            "-fx-text-fill: #F5A623;" +
                            "-fx-padding: 12 16;" +
                            "-fx-background-radius: 12;" +
                            "-fx-effect: dropshadow(gaussian, #00ffd0, 6, 0.5, 0, 0);"
            );

            // Vorher vorhandene Kinder entfernen
            popupContainer.getChildren().clear();
            popupContainer.getChildren().add(infoLabel);

            // Popup sichtbar machen
            popupContainer.setVisible(true);
            popupContainer.setManaged(true);

            // Nach 15 Sekunden Popup wieder verstecken
            PauseTransition delay = new PauseTransition(Duration.seconds(15));
            delay.setOnFinished(event -> {
                popupContainer.getChildren().clear();
                popupContainer.setVisible(false);
                popupContainer.setManaged(false);
            });
            delay.play();
        });
    }


    private void playWavingAnimation(Label label) {
        TranslateTransition wave = new TranslateTransition(Duration.millis(600), label);
        wave.setFromX(-5);
        wave.setToX(5);
        wave.setCycleCount(Animation.INDEFINITE);
        wave.setAutoReverse(true);
        wave.play();
        label.setUserData(wave);
    }

    private void stopWavingAnimation(Label label) {
        Object userData = label.getUserData();
        if (userData instanceof Animation anim) {
            anim.stop();
            label.setTranslateX(0);
        }
    }


    private void updateMiniRobotInPhase(boolean isSetupPhase, boolean isProgrammingPhase, boolean isActivationPhase) {
        int myID = ClientSingleton.getInstance().getID();

        if (isSetupPhase || isProgrammingPhase || isActivationPhase) {
            try {
                int robotID = clientToRobotID.getOrDefault(myID, 0);
                String imagePath = "/assets/robots/robot_0" + robotID + "_right.png";
                Image image = new Image(getClass().getResourceAsStream(imagePath));
                playerIcon.setImage(image);
                playerIcon.setVisible(true);
                playerIcon.setManaged(true);

                if (isProgrammingPhase) {
                    startMiniRobotFloatAnimation();
                } else {
                    stopMiniRobotFloatAnimation();
                    playerIcon.setTranslateY(0);
                }
            } catch (Exception e) {
                appLogger.warn("Fehler beim Anzeigen des Mini-Roboters: {}", e.getMessage());
                playerIcon.setVisible(false);
            }
        } else {
            playerIcon.setVisible(false);
            playerIcon.setManaged(false);
            stopMiniRobotFloatAnimation();
        }
    }

    private void startMiniRobotFloatAnimation() {
        if (robotFloat != null) robotFloat.stop();

        robotFloat = new TranslateTransition(Duration.seconds(1.3), playerIcon);
        robotFloat.setFromY(0);
        robotFloat.setToY(-6);
        robotFloat.setAutoReverse(true);
        robotFloat.setCycleCount(TranslateTransition.INDEFINITE);
        robotFloat.play();
    }

    private void stopMiniRobotFloatAnimation() {
        if (robotFloat != null) {
            robotFloat.stop();
            robotFloat = null;
        }
    }

    private Color getRobotGlowColor(int robotID) {
        return switch (robotID) {
            case 0 -> Color.RED;
            case 1 -> Color.BLUE;
            case 2 -> Color.LIMEGREEN;
            case 3 -> Color.GOLD;
            case 4 -> Color.MEDIUMPURPLE;
            case 5 -> Color.ORANGE;
            default -> Color.WHITE;
        };
    }

    private void applyRobotGlow(ImageView robotView, int robotID) {
        Color glowColor = getRobotGlowColor(robotID);
        DropShadow glow = new DropShadow();
        glow.setColor(glowColor);
        glow.setRadius(20);
        glow.setSpread(0.4);
        glow.setOffsetX(0);
        glow.setOffsetY(0);
        robotView.setEffect(glow);
    }
    public void updateDiscardPile(String lastCardName) {
        discardPileBox.getChildren().clear();

        if (lastCardName != null) {
            String path = "/assets/cards/" + lastCardName + ".png";
            Image image;
            try {
                image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
            } catch (Exception e) {
                image = new Image(getClass().getResourceAsStream("/assets/cover_card.png"));
            }

            ImageView view = new ImageView(image);
            view.setFitWidth(60);
            view.setFitHeight(90);
            view.setPreserveRatio(true);
            view.setSmooth(true);
            discardPileBox.getChildren().add(view);
        }
    }
    private void animateDiscard(ImageView cardView) {
        TranslateTransition slide = new TranslateTransition(Duration.millis(300), cardView);
        slide.setFromY(-50);
        slide.setToY(0);
        slide.setInterpolator(Interpolator.EASE_OUT);

        FadeTransition fade = new FadeTransition(Duration.millis(300), cardView);
        fade.setFromValue(0);
        fade.setToValue(1);

        ParallelTransition animation = new ParallelTransition(slide, fade);
        animation.play();
    }
    /**
     * Aktualisiert die Registerkarten eines anderen Spielers.
     * Nach dem Update wird die Spielerstatusanzeige aktualisiert, um
     * den neuen Zustand sichtbar zu machen.
     * @param clientID       Die eindeutige ID des Spielers, dessen Register aktualisiert wird.
     * @param registerCards  Eine Liste von Karten (als Strings), die der Spieler für diese Runde programmiert hat.
     */
    public void updateOtherPlayerRegister(int clientID, List<String> registerCards) {
        otherPlayersRegisters.put(clientID, registerCards);
        refreshPlayerStatusUI();
    }
    private boolean activationPhaseActive = false;

    private void refreshPlayerStatusUI() {
        Platform.runLater(() -> {
            playerStatusBox.getChildren().clear();

            for (Map.Entry<Integer, List<String>> entry : otherPlayersRegisters.entrySet()) {
                int clientID = entry.getKey();
                List<String> cards = entry.getValue();
                String playerName = getPlayerNameById(clientID);

                VBox playerBox = new VBox(2);
                playerBox.setStyle("-fx-border-color: #33cccc; -fx-padding: 4; -fx-background-color: #e0f7f7;");

                Label nameLabel = new Label(playerName);
                nameLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #007777;");

                HBox cardsBox = new HBox(3);
                for (String card : cards) {
                    ImageView cardView = createCardBackOrFront(card, activationPhaseActive);
                    cardsBox.getChildren().add(cardView);
                }

                playerBox.getChildren().addAll(nameLabel, cardsBox);
                playerStatusBox.getChildren().add(playerBox);
            }
        });
    }
    private ImageView createCardBackOrFront(String cardName, boolean showFront) {
        Image img;
        try {
            if (showFront) {
                img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cards/" + cardName.toLowerCase() + ".png")));
            } else {
                img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover_card.png")));
            }
        } catch (Exception e) {
            img = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover_card.png")));
        }
        ImageView iv = new ImageView(img);
        iv.setFitWidth(40);
        iv.setFitHeight(60);
        iv.setPreserveRatio(true);
        iv.setSmooth(true);
        return iv;
    }
    public void spieleLaserAnimation(int clientID) {
        Position startPos = robotPositions.get(clientID);
        if (startPos == null) {
            appLogger.warn("Keine Position für Roboter von Client {} gefunden.", clientID);
            return;
        }

        String richtung = robotDirections.get(clientID);
        if (richtung == null) {
            appLogger.warn("Keine Richtung für Roboter von Client {} gefunden.", clientID);
            return;
        }

        // Startpunkt (Pixel-Koordinaten, Mitte der Zelle)
        double startX = startPos.x() * TILE_SIZE + TILE_SIZE / 2.0;
        double startY = startPos.y() * TILE_SIZE + TILE_SIZE / 2.0;

        // Endpunkt des Lasers berechnen (Raycast)
        Point2D endPunkt = berechneLaserEndpunkt(startPos.x(), startPos.y(), richtung);

        Line laserLinie = new Line();
        laserLinie.setStartX(startX);
        laserLinie.setStartY(startY);
        laserLinie.setEndX(startX);
        laserLinie.setEndY(startY);
        laserLinie.setStroke(Color.RED);
        laserLinie.setStrokeWidth(4);
        laserLinie.setOpacity(0.8);
        // DropShadow-Glow hinzufügen
        DropShadow glow = new DropShadow();
        glow.setColor(Color.RED);
        glow.setRadius(10);
        glow.setSpread(0.5);
        laserLinie.setEffect(glow);

        gameBoardPane.getChildren().add(laserLinie);

        // Animation: Linie wächst und wird dann ausgeblendet
        Timeline animation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(laserLinie.endXProperty(), startX),
                        new KeyValue(laserLinie.endYProperty(), startY),
                        new KeyValue(laserLinie.opacityProperty(), 0.8)
                ),
                new KeyFrame(Duration.seconds(0.3),
                        new KeyValue(laserLinie.endXProperty(), endPunkt.getX()),
                        new KeyValue(laserLinie.endYProperty(), endPunkt.getY())
                ),
                new KeyFrame(Duration.seconds(0.8),
                        new KeyValue(laserLinie.opacityProperty(), 0.8)
                ),
                new KeyFrame(Duration.seconds(1.3),
                        new KeyValue(laserLinie.opacityProperty(), 0)
                )
        );

        animation.setOnFinished(e -> gameBoardPane.getChildren().remove(laserLinie));
        animation.play();
    }

    /**
     * Berechnet den Endpunkt des Laserstrahls mit Wänden und Robotern.
     */
    private Point2D berechneLaserEndpunkt(int startX, int startY, String richtung) {
        final int MAX_DISTANZ = 10;

        int dx = 0, dy = 0;
        switch (richtung.toLowerCase()) {
            case "right" -> dx = 1;
            case "left" -> dx = -1;
            case "top" -> dy = -1;
            case "bottom" -> dy = 1;
            default -> {
                appLogger.warn("Ungültige Richtung für Laser: {}", richtung);
                return new Point2D(startX * TILE_SIZE + TILE_SIZE / 2.0, startY * TILE_SIZE + TILE_SIZE / 2.0);
            }
        }

        int x = startX;
        int y = startY;

        for (int i = 0; i < MAX_DISTANZ; i++) {
            // Prüfe Wand vor dem nächsten Feld
            if (hatWand(x, y, dx, dy)) {
                break;
            }

            x += dx;
            y += dy;

            StackPane zelle = getCellAt(x, y);
            if (zelle == null) break;

            if (istRoboterAnPosition(x, y)) {
                break;
            }
        }

        double endX = x * TILE_SIZE + TILE_SIZE / 2.0;
        double endY = y * TILE_SIZE + TILE_SIZE / 2.0;
        return new Point2D(endX, endY);
    }

    /**
     * Prüft, ob zwischen Feld (x,y) und dem Feld in Richtung (dx, dy) eine Wand den Laser blockiert.
     */
    private boolean hatWand(int x, int y, int dx, int dy) {
        // Hole die Board-Elemente an Position (x, y)
        List<MessageDefinitions.Field> elements = getBoardElementsAt(x, y);
        if (elements == null) return false;

        for (MessageDefinitions.Field element : elements) {
            if ("Wall".equals(element.type())) {
                MessageDefinitions.FieldWall wall = (MessageDefinitions.FieldWall) element;
                if (wandBlockiertRichtung(wall, dx, dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Hilfsmethode, um zu prüfen, ob die Wand die Laser-Richtung blockiert.
     */
    private boolean wandBlockiertRichtung(MessageDefinitions.FieldWall wall, int dx, int dy) {
        List<String> orientations = wall.orientations();

        if (dx == 1 && orientations.contains("left")) return true;
        if (dx == -1 && orientations.contains("right")) return true;
        if (dy == 1 && orientations.contains("top")) return true;
        if (dy == -1 && orientations.contains("bottom")) return true;

        return false;
    }

    /**
     * Prüft, ob ein Roboter an der Position (x, y) steht.
     */
    private boolean istRoboterAnPosition(int x, int y) {
        for (Position pos : robotPositions.values()) {
            if (pos.x() == x && pos.y() == y) {
                return true;
            }
        }
        return false;
    }
    private Board board;

    public void setBoard(Board board) {
        this.board = board;
    }
    private List<List<List<MessageDefinitions.Field>>> boardMap;

    public List<MessageDefinitions.Field> getBoardElementsAt(int x, int y) {
        if (boardMap == null || x < 0 || y < 0 || x >= boardMap.size() || y >= boardMap.get(0).size()) {
            return null;
        }
        return boardMap.get(x).get(y);
    }

    /**
     * Zeigt den aktuellen Energiewert in der UI an.
     * <p>
     * Setzt Text, Farbe und Fortschrittsbalken entsprechend dem Wert.
     * Spielt eine Animation bei Änderung (Bounce bei Anstieg, Shake bei Abfall).
     *
     * @param energy Der neue Energiewert.
     */
    public void updateEnergyDisplay(int energy) {
        energyValue.setText(String.valueOf(energy));

        String color;
        if (energy >= 5) {
            color = "limegreen";
        } else if (energy >= 3) {
            color = "orange";
        } else {
            color = "red";
        }

        energyIcon.setStyle("-fx-text-fill: " + color + ";");
        energyValue.setStyle("-fx-text-fill: " + color + ";");
        energyBar.setProgress(energy / (double) MAX_ENERGY);

        //  Animation je nach Änderung
        if (lastEnergy != -1) {
            if (energy > lastEnergy) {
                // GESTEIGERT → Bounce
                ScaleTransition bounce = new ScaleTransition(Duration.millis(300), energyIcon);
                bounce.setFromX(1.0);
                bounce.setFromY(1.0);
                bounce.setToX(1.5);
                bounce.setToY(1.5);
                bounce.setCycleCount(2);
                bounce.setAutoReverse(true);
                bounce.play();
            } else if (energy < lastEnergy) {
                // GESENKT → Shake
                TranslateTransition shake = new TranslateTransition(Duration.millis(80), energyIcon);
                shake.setFromX(-4);
                shake.setToX(4);
                shake.setCycleCount(6);
                shake.setAutoReverse(true);
                shake.play();
            }
        }
        lastEnergy = energy;
    }

    public void setupDiscardPileBox() {
        // Image of the back of a card
        ImageView discardImage = new ImageView(new Image(Objects.requireNonNull(
                getClass().getResourceAsStream("/assets/cover.png"))));
        discardImage.setFitWidth(60);
        discardImage.setFitHeight(90);
        discardImage.setPreserveRatio(true);

        // Counter in the top-right corner
        discardCounter = new Label("0");
        discardCounter.setStyle("-fx-text-fill: white; -fx-font-size: 16; -fx-font-weight: bold;");
        StackPane.setAlignment(discardCounter, Pos.TOP_RIGHT);

        discardPileBox.getChildren().addAll(discardImage, discardCounter);

        // Click to open discard view
        discardPileBox.setOnMouseClicked(e -> showDiscardPile());
    }

    public void addCardToDiscard(String cardName) {
        discardPile.add(cardName);
        updateDiscardCounter();
    }

    private void updateDiscardCounter() {
        discardCounter.setText(String.valueOf(discardPile.size()));
    }

    public void showDiscardPile() {
        Stage stage = new Stage();
        stage.setTitle("Discard Pile");

        FlowPane pane = new FlowPane();
        pane.setPadding(new Insets(10));
        pane.setHgap(10);
        pane.setVgap(10);

        for (String cardName : discardPile) {
            String path = "/assets/cards/" + cardName + ".png";
            Image image;
            try {
                image = new Image(Objects.requireNonNull(getClass().getResourceAsStream(path)));
            } catch (Exception ex) {
                image = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover_card.png")));
            }

            ImageView view = new ImageView(image);
            view.setFitWidth(60);
            view.setFitHeight(90);
            view.setPreserveRatio(true);
            pane.getChildren().add(view);
        }

        ScrollPane scrollPane = new ScrollPane(pane);
        scrollPane.setFitToWidth(true);

        Scene scene = new Scene(scrollPane, 400, 300);
        stage.setScene(scene);
        stage.show();
    }

    /**
     * Zeigt das Spiellogo mit einer einfachen Ein- und Ausblendanimation.
     * <p>
     * Eignet sich für diskrete Hinweise oder einfache Übergänge.
     */
    public void showGameLogoAnimation() {
        gameLogoView.setVisible(true);
        gameLogoView.setManaged(true);

        FadeTransition fadeIn = new FadeTransition(Duration.seconds(1.2), gameLogoView);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.setOnFinished(e -> {
            PauseTransition wait = new PauseTransition(Duration.seconds(1.5));
            wait.setOnFinished(ev -> {
                FadeTransition fadeOut = new FadeTransition(Duration.seconds(1.2), gameLogoView);
                fadeOut.setFromValue(1);
                fadeOut.setToValue(0);
                fadeOut.setOnFinished(event -> {
                    gameLogoView.setVisible(false);
                    gameLogoView.setManaged(false);
                });
                fadeOut.play();
            });
            wait.play();
        });
        fadeIn.play();
    }
    /**
     * Zeigt das Spiellogo mit einer aufwendigen Übergangsanimation.
     * <p>
     * Das Logo erscheint aus dem unteren Bildschirmbereich, skaliert sich hoch
     * und verblasst nach einer kurzen Pause wieder.
     *  für den Spielstart oder Übergänge zwischen Phasen.
     */
    public void showLogoTransition() {
        gameLogoView.setVisible(true);
        gameLogoView.setManaged(true);

        gameLogoView.setOpacity(0);
        gameLogoView.setScaleX(0.2);
        gameLogoView.setScaleY(0.2);
        gameLogoView.setTranslateZ(-300);

        TranslateTransition move = new TranslateTransition(Duration.millis(1200), gameLogoView);
        move.setFromY(200);
        move.setToY(0);

        ScaleTransition scale = new ScaleTransition(Duration.millis(1200), gameLogoView);
        scale.setFromX(0.2);
        scale.setFromY(0.2);
        scale.setToX(1.0);
        scale.setToY(1.0);

        FadeTransition fadeIn = new FadeTransition(Duration.millis(1000), gameLogoView);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);

        PauseTransition hold = new PauseTransition(Duration.seconds(2));

        FadeTransition fadeOut = new FadeTransition(Duration.millis(800), gameLogoView);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(e -> {
            gameLogoView.setVisible(false);
            gameLogoView.setManaged(false);
        });

        SequentialTransition sequence = new SequentialTransition(
                new ParallelTransition(move, scale, fadeIn),
                hold,
                fadeOut
        );

        sequence.play();
    }


    private void clearProgrammingUI() {
    }
    public void animateHandCards(List<String> handCards) {
        handCardBox.getChildren().clear();
        Timeline timeline = new Timeline();

        for (int i = 0; i < handCards.size(); i++) {
            String cardName = handCards.get(i);
            ImageView card = createClickableCard(cardName);
            card.setOpacity(0);
            card.setTranslateY(30);

            handCardBox.getChildren().add(card);

            FadeTransition fade = new FadeTransition(Duration.millis(200), card);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(200), card);
            slide.setFromY(30);
            slide.setToY(0);

            ParallelTransition entry = new ParallelTransition(fade, slide);
            KeyFrame kf = new KeyFrame(Duration.millis(i * 120), e -> entry.play());
            timeline.getKeyFrames().add(kf);
        }

        timeline.play();
    }

}






