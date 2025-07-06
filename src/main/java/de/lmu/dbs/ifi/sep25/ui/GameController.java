package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.function.Consumer;

import static java.util.Map.entry;

public class GameController {
    // 0. Logger
    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");
    private static final Logger appLogger = LogManager.getLogger(GameController.class);
    private static final Logger errorLogger = LogManager.getLogger(GameController.class);

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
    private HBox playedCardsBox;

    @FXML
    private HBox handCardBox;
    @FXML
    private HBox registerBox;
    private static final int TILE_SIZE = 60;

    private final Map<String, Image> tileImages = new HashMap<>();
    private final Map<Integer, Position> robotPositions = new HashMap<>();
    private Parent root;
    @FXML
    private Label timerLabel;

    private Timeline countdownTimer;
    private int secondsLeft = 30;
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(GameController.class);
    @FXML
    private HBox discardPileBox;
    @FXML
    private Label phaseLabel;
    @FXML
    private Button confirmSelectionButton;


    private int currentPlayerID = -1;
    private final Map<Integer, Integer> clientToRobotID = new HashMap<>();
    /**
     * Stores the direction of the robot of each clientID as a lowercase string.
     **/
    private final Map<Integer, String> robotDirections = new HashMap<>();

    /**
     * Keeps track of the current register cards (null = empty)
     **/
    private final String[] registerState = new String[5];
    private final Set<Integer> manuallyClearedSlots = new HashSet<>();


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
        clientToRobotID.put(clientID, figure);

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
                    pane.setStyle("-fx-border-color: yellow; -fx-border-width: 2px;");
                    if (pane.getOnMouseClicked() == null) {
                        pane.setOnMouseClicked(_ -> handleStartPointClick(pane));
                    }
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

        // Nachricht senden
        var msg = new MessageDefinitions.Message<>(new MessageDefinitions.BodySetStartingPoint(x, y,
                switch (ClientSingleton.getInstance().getSelectedMap()) {
                    case "Heavy Merge Area", "Death Trap" -> "left";
                    case "Pilgrimage", "Gear Stripper" -> "top";
                    default -> "right";
                }));
        ClientSingleton.getInstance().sendMessage(msg);

        // Deaktiviere alle Startfelder (nur einmalige Auswahl zulassen)
        for (javafx.scene.Node node : gameBoardPane.getChildren()) {
            if (node instanceof StackPane pane) {
                pane.setOnMouseClicked(null);
                pane.setStyle("-fx-border-color: transparent;"); // entferne ggf. gelbe Rahmen
            }
        }

        // Nur den gewählten grün markieren
        selectedPane.setStyle("-fx-border-color: green; -fx-border-width: 2px;");

        appendChatMessage("[INFO] Startposition gewählt bei (" + x + ", " + y + ")");
        showInstructionDialog();
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

    public void updatePhase(String phaseName) {
        boolean isSetupPhase = phaseName.toLowerCase().contains("aufbau");
        boolean isProgrammingPhase = phaseName.toLowerCase().contains("programm");
        boolean isActivationPhase = phaseName.toLowerCase().contains("aktivierung");
        boolean isGameOverPhase = phaseName.toLowerCase().contains("ende") || phaseName.toLowerCase().contains("spielende");
        logger.info("Update game stage: {}, isProgrammingPhase={}", phaseName, isProgrammingPhase);
        // Label aktualisieren
        if (phaseLabel != null) {
            phaseLabel.setText("Phase: " + phaseName);
        }

        // Update availability of click start position depending on phase
        for (javafx.scene.Node node : gameBoardPane.getChildren()) {
            // Update the availability of the starting position click based on the stage
            if (node instanceof StackPane pane && pane.getOnMouseClicked() != null) {
                if (!isSetupPhase) {
                    // Disable clicks outside the setup phase.
                    pane.setOnMouseClicked(null);
                    pane.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");
                }
            }
        }
        // Handkarten nur in Programmierphase aktiv
        handCardBox.setDisable(!isProgrammingPhase);
        logger.info(isProgrammingPhase ? "Enable hand card area" : "No-touch zone");

        if (isProgrammingPhase) {
            handCardBox.setDisable(false);
            handCardBox.setVisible(true);
            handCardBox.setManaged(true);
            logger.info("Hand card area enabled");
        } else {
            handCardBox.setDisable(true);
            logger.info("No handball zone");
        }

        // DiscardPile nur in Aktivierungsphase sichtbar
        discardPileBox.setVisible(isActivationPhase);
        discardPileBox.setManaged(isActivationPhase);

        if (confirmSelectionButton != null) {
            updateConfirmButtonVisibility();

            //confirmSelectionButton.setVisible(isProgrammingPhase);
            // confirmSelectionButton.setManaged(isProgrammingPhase);
        }


//        // Timer nur in Programmierphase starten
//        if (isProgrammingPhase) {
//            startCountdown();
//        } else {
//            hideCountdown();
//        }

        // Chat sperren, wenn Spiel vorbei ist
        chatInput.setDisable(isGameOverPhase);
        recipientBox.setDisable(isGameOverPhase);

        // Beispiel: iconMenu (z. B. Buttons oder Aktionsleiste)
        iconMenu.setDisable(isGameOverPhase || isSetupPhase);

        // ChatBox bei Spielende ausblenden
        if (isGameOverPhase) {
            chatBox.setVisible(false);
            chatBox.setManaged(false);
        }

        //appendChatMessage("[INFO] Aktuelle Phase: " + phaseName);

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
        boolean currentlyVisible = chatBox.isVisible();
        chatBox.setVisible(!currentlyVisible);
        chatBox.setManaged(!currentlyVisible);
        iconMenu.setVisible(currentlyVisible);
        iconMenu.setManaged(currentlyVisible);
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
        // Verwende Platzhalter, wenn Bild fehlt
        String imagePath = "/assets/cards/" + cardName.toLowerCase() + ".png";
        Image cardImage;
        try {
            cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
        } catch (Exception e) {
            cardImage = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover.png")));
        }

        ImageView cardView = new ImageView(cardImage);
        cardView.setFitWidth(60);
        cardView.setFitHeight(90);
        cardView.setPreserveRatio(true);
        cardView.setSmooth(true);

        playedCardsBox.getChildren().add(cardView);
        if (playedCardsBox.getChildren().size() > 5) {
            playedCardsBox.getChildren().remove(0);
        }
    }

    /**
     * Hebt den Spieler hervor, der aktuell am Zug ist.
     *
     * @param clientID Die Client-ID des Spielers
     */
    public void markCurrentPlayer(int clientID) {
        int phaseID = getCurrentPhaseID();
        appLogger.info("[DEBUG] markCurrentPlayer aufgerufen mit clientID = {}", clientID);

        if (phaseID == 2) {
            appendChatMessage("[INFO] Programmieren...");
            statusLabel.setText("Spiel läuft...");
        }
        appLogger.info("Aktueller Spieler ist: {}", clientID);

        for (PlayerEntry entry : recipientBox.getItems()) {
            if (entry.getClientID() == clientID) {
                String playerName = entry.getName();

                // Es ist dein eigener Zug
                if (clientID == ClientSingleton.getInstance().getID()) {
                    appendChatMessage("[INFO] Du bist am Zug!");

                    if (currentPhaseID == 0) {
                        appendChatMessage("Bitte wähle deine Startposition durch Klick auf ein gelbes Feld.");
                    }
                } else {
                    appendChatMessage("[INFO] " + playerName + " ist am Zug.");
                    statusLabel.setText(playerName + " ist am Zug.");
                }
                recipientBox.getSelectionModel().select(entry);

                this.currentPlayerID = clientID;
                return;
            }
        }

        // Falls der Spieler nicht gefunden wurde
        if (clientID == ClientSingleton.getInstance().getID()) {
            appendChatMessage("[INFO] Du bist am Zug.");

            if (currentPhaseID == 0) {
                appendChatMessage("Bitte wähle deine Startposition durch Klick auf ein gelbes Feld.");
            }

            statusLabel.setText("Du bist am Zug.");
        } else {
            String name = getPlayerNameById(clientID);
            appendChatMessage("[INFO] " + name + " ist am Zug.");
            statusLabel.setText(name + " ist am Zug.");
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
            robotView.setUserData("robot");//verbessern

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


        // 1. Rebuild register UI (DO keep this)
        registerBox.getChildren().clear();

        for (int i = 0; i < 5; i++) {
            Label numberLabel = new Label(String.valueOf(i + 1));
            numberLabel.setStyle("-fx-font-size: 18px; -fx-background-color: darkorange; -fx-text-fill: white; -fx-padding: 6px; -fx-background-radius: 30px;");
            numberLabel.setMaxSize(30, 30);

            StackPane slot = new StackPane();
            slot.setPrefSize(60, 90);
            slot.setStyle("-fx-border-color: gray; -fx-background-color: lightgray;");
            setupRegisterSlot(slot);

            Tooltip tooltip = new Tooltip("Bitte hier eine Programmierkarte ablegen");
            Tooltip.install(slot, tooltip);

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
            String base = tagged.split("#")[0];       // e.g. "MoveII#1" → "MoveII"
            ImageView view = createClickableCard(base);
            view.setUserData(tagged);                 // userData = "MoveII#1"
            handCardBox.getChildren().add(view);
        }

        updateConfirmButtonVisibility();
    }

    private ImageView createClickableCard(String cardName) {
        String imagePath = "/assets/cards/" + cardName.toLowerCase() + ".png";
        Image img;
        try {
            img = new Image(getClass().getResourceAsStream(imagePath));
            if (img.isError()) {
                logger.error("Error loading card image: {}", imagePath);
                img = new Image(getClass().getResourceAsStream("/assets/cover.png"));
            }
        } catch (Exception e) {
            logger.error("Abnormal loading of card images: {}", e.getMessage());
            img = new Image(getClass().getResourceAsStream("/assets/cover.png"));
        }

        ImageView view = new ImageView(img);
        view.setFitWidth(60);
        view.setFitHeight(90);
        view.setPreserveRatio(true);
        view.setSmooth(true);

        view.setStyle("-fx-cursor: hand;");
        view.setOnMouseEntered(e -> view.setEffect(new DropShadow()));
        view.setOnMouseExited(e -> view.setEffect(null));

        view.setOnMouseClicked(event -> {
            String raw = view.getUserData().toString(); // e.g. "TurnRight#1"
            String actualCardName = raw.contains("#") ? raw.split("#")[0] : raw;

            logger.info("Card clicked: {}", actualCardName);

            int nextEmptySlot = findNextEmptyRegisterSlot();
            if (nextEmptySlot != -1) {
                appLogger.info("Place the card {} into the storage slot {}.", actualCardName, nextEmptySlot);

                // Only update logical state
                registerState[nextEmptySlot] = actualCardName;

                // Let server & displayHandCards() handle UI

                var body = new MessageDefinitions.BodySelectedCard(actualCardName, nextEmptySlot);
                var msg = new MessageDefinitions.Message<>(body);
                ClientSingleton.getInstance().sendMessage(msg);

                appLogger.info("Card selection message sent");
                appendChatMessage("[INFO] Card: " + actualCardName + " has been selected for slot " + (nextEmptySlot + 1));
            } else {
                appendChatMessage("[WARNUNG] Alle Registerspeicher sind bereits belegt.");
                appLogger.warn("No available storage slots");
            }
            updateConfirmButtonVisibility();


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

    private void placeCardInRegisterSlot(String cardName, int slotIndex) {
        if (slotIndex >= 0 && slotIndex < registerBox.getChildren().size()) {
            Node node = registerBox.getChildren().get(slotIndex);
            if (node instanceof VBox vbox) {
                Node slotNode = vbox.getChildren().get(1);
                if (slotNode instanceof StackPane pane) {
                    pane.getChildren().clear();

                    String imagePath = "/assets/cards/" + cardName.toLowerCase() + ".png";
                    Image img;
                    try {
                        img = new Image(getClass().getResourceAsStream(imagePath));
                    } catch (Exception e) {
                        img = new Image(getClass().getResourceAsStream("/assets/cover.png"));
                    }

                    ImageView cardView = new ImageView(img);
                    cardView.setFitWidth(60);
                    cardView.setFitHeight(90);
                    cardView.setPreserveRatio(true);
                    cardView.setSmooth(true);
                    cardView.setUserData(cardName);
                    cardView.setOpacity(0.7);

                    pane.getChildren().add(cardView);
                    // remember what card is in slot
                    registerState[slotIndex] = cardName;
                    appLogger.info("Card {} placed in register slot {}", cardName, slotIndex);
                }
            }
        }
    }

    private void setupRegisterSlot(StackPane pane) {
        pane.setOnDragOver(null);
        pane.setOnDragDropped(null);
        pane.setOnMouseClicked(event -> {
            if (!pane.getChildren().isEmpty()) {
                Node node = pane.getChildren().getFirst();
                if (node instanceof ImageView) {
                    int slotIndex = -1;
                    for (int i = 0; i < registerBox.getChildren().size(); i++) {
                        Node boxNode = registerBox.getChildren().get(i);
                        if (boxNode instanceof VBox vbox) {
                            Node slotNode = vbox.getChildren().get(1);
                            if (slotNode == pane) {
                                slotIndex = i;
                                break;
                            }
                        }
                    }

                    if (slotIndex != -1) {
                        manuallyClearedSlots.add(slotIndex); // for differentiation between server changes and client changes
                        pane.getChildren().clear();
                        registerState[slotIndex] = null;

                        appendChatMessage("[INFO] Karte aus Register " + (slotIndex + 1) + " entfernt.");
                        appLogger.info("Card removed from register slot {}", slotIndex);
                    }

                }
            }
            event.consume();
        });
    }

    /**
     * Bestätigt die ausgewählten Karten und sendet sie an den Server.
     */
    @FXML
    private void handleConfirmSelection() {
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

        // Sende "fertig" Nachricht an Server
        var finishedBody = new MessageDefinitions.BodySelectionFinished(clientID);
        var finishedMsg = new MessageDefinitions.Message<>(finishedBody);
        ClientSingleton.getInstance().sendMessage(finishedMsg);

        appendChatMessage("[INFO] Auswahl wurde erfolgreich gesendet.");
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
            Image back = new Image(Objects.requireNonNull(getClass().getResourceAsStream("/assets/cover.png")));

            ImageView cardBack = new ImageView(back);
            cardBack.setFitWidth(60);
            cardBack.setFitHeight(90);
            cardBack.setPreserveRatio(true);
            cardBack.setSmooth(true);

            handCardBox.getChildren().add(cardBack);
        }

        appendChatMessage("Spieler #" + clientID + " hat " + count + " Karten erhalten.");
    }

    /**
     * Zeigt eine optionale Animation oder Nachricht an, dass das Deck gemischt wurde.
     */
    public void showShuffleAnimation() {
        appendChatMessage("[INFO] Das Programmierdeck wurde neu gemischt.");
        // Optional: UI-Animation hier einbauen @Raneem
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
            appendChatMessage("[INFO] " + playerName + " hat eine Karte ausgewählt.");
            return;
        }
        // Self && !filled so self and emptied
        if (!filled) {
            if (manuallyClearedSlots.remove(register)) {
                appLogger.debug("Ignoring register {} deselection - user triggered.", register);
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
            registerState[register] = null;

            // No need to update the hand manually — will be handled by displayHandCards
            appendChatMessage("[INFO] Deine Karte aus Register " + (register + 1) + " wurde entfernt.");
            appLogger.info("Card removed from register slot {} by server (isSelf).", register);
        }
    }


    private String getPlayerNameById(int id) {
        for (PlayerEntry entry : recipientBox.getItems()) {
            if (entry.getClientID() == id) {
                return entry.getName();
            }
        }
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
        timerLabel.setText("Zeit: 30s");
        timerLabel.setVisible(true);// Zeige das Label beim Start
        timerLabel.setManaged(true);


        if (countdownTimer != null) countdownTimer.stop();

        countdownTimer = new Timeline(
                new KeyFrame(Duration.seconds(1), event -> {
                    secondsLeft--;
                    timerLabel.setText("Zeit: " + secondsLeft + "s");
                    if (secondsLeft <= 0) {
                        countdownTimer.stop();
                        timerLabel.setText("Zeit abgelaufen!");
                        hideCountdown(); //ausblenden nach Ablauf
                    }
                })
        );
        countdownTimer.setCycleCount(30);
        countdownTimer.play();
    }

    public void hideCountdown() {
        timerLabel.setVisible(false);
        timerLabel.setManaged(false);
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

        hideCountdown();
        appendChatMessage("[TIMER] Zeit ist abgelaufen.");

        if (slowPlayers != null && !slowPlayers.isEmpty()) {
            appendChatMessage("Folgende Spieler waren zu langsam: " + slowPlayers);

            for (PlayerEntry entry : recipientBox.getItems()) {
                if (slowPlayers.contains(entry.getClientID())) {
                    // Spieler visuell markieren (z. B. ✖ vor den Namen setzen)
                    entry.setName("✖ " + entry.getName());
                }
            }

            // Optional: Auswahl zurücksetzen, falls vorheriger Eintrag jetzt verändert wurde
            recipientBox.getSelectionModel().clearSelection();
        }
    }

    /**
     * Zeigt die vom Spieler bestätigten Karten, z. B. im Register oder Kartenbereich.
     *
     * @param cards Liste der Kartennamen (z. B. ["move_1", "turn_left", ...])
     */
    public void displayConfirmedCards(List<String> cards) {
        handCardBox.getChildren().clear();

        for (String card : cards) {
            String imagePath = "/assets/cards/" + card.toLowerCase() + ".png";

            Image cardImage;
            try {
                cardImage = new Image(getClass().getResourceAsStream(imagePath));
            } catch (Exception e) {
                cardImage = new Image(getClass().getResourceAsStream("/assets/cover.png"));
            }

            ImageView cardView = new ImageView(cardImage);
            cardView.setFitWidth(60);
            cardView.setFitHeight(90);
            cardView.setPreserveRatio(true);
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

            // Falls col/row null (z. B. bei nicht gesetztem Index), skip
            if (col != null && row != null && col == x && row == y) {
                return (StackPane) node;
            }
        }

        errorLogger.warn("Kein StackPane gefunden bei Koordinaten ({}, {})", x, y);
        return null;
    }


    /**
     * Ersetzt eine Karte in einem bestimmten Register (z. B. durch Schaden).
     *
     * @param clientID ID des Spielers
     * @param register Register-Slot (0–4)
     * @param newCard  Name der neuen Karte
     */
    public void replaceCardInRegister(int clientID, int register, String newCard) {
        // Placeholder: Anzeige im Chat (später grafisch auf dem Spielfeld zeigen)
        appendChatMessage("[INFO] Spieler " + clientID + " ersetzt Karte in Register " + register + " durch: " + newCard);

        // TODO (optional): Animation oder visuelles Update für Register-Karte
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
            appLogger.info("looking up for clientID {}", clientID);
            appLogger.info("clientToRobotID: {}", clientToRobotID.toString());
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

            appendChatMessage("[BEWEGUNG] Spieler " + clientID + " wurde nach (" + x + ", " + y + ") bewegt.");
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
        Position pos = robotPositions.get(clientID);
        if (pos == null) {
            appendChatMessage("[FEHLER] Position für Spieler " + clientID + " nicht gefunden.");
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

                    appendChatMessage("[DREHUNG] Spieler " + clientID + " dreht sich " + rotation + ".");
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
     * @param type Typ der Animation (z. B. "Movement", "Clockwise", "Checkpoint")
     */
    public void playAnimation(String type) {
        appendChatMessage("[ANIMATION] " + type + " ausgeführt.");

        // Platzhalter: Optional kleine visuelle Effekte
        // z. B. Blinken des Spielfelds, Richtungspfeil, etc.
        // TODO: Ersetze durch echte Animationen in späterem Schritt
    }

    /**
     * Zeigt eine visuelle Nachricht oder Platzhalter an, dass ein Spieler rebootet wurde.
     *
     * @param clientID ID des Spielers
     */
    public void showReboot(int clientID) {
        // Optional: Spielername ermitteln – hier als Platzhalter
        String playerName = "Spieler " + clientID;

        // Nachricht im Chat anzeigen
        appendChatMessage("[REBOOT] " + playerName + " wurde rebootet.");

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
     * @param source   Quelle der Energie (z. B. "EnergySpace", "Laser")
     */
    public void showEnergyChange(int clientID, int energy, String source) {
        String playerName = "Spieler " + clientID;
        appendChatMessage("[ENERGIE] " + playerName + " hat jetzt " + energy + " ⚡ (Quelle: " + source + ")");

        // Optional: weitere Anzeige z. B. Energiebalken, Symbol-Update etc.
    }

    /**
     * Zeigt an, dass ein Spieler einen Checkpoint erreicht hat.
     *
     * @param clientID         Die ID des Spielers
     * @param checkpointNumber Die Nummer des erreichten Checkpoints
     */
    public void showCheckpointReached(int clientID, int checkpointNumber) {
        String playerName = "Spieler " + clientID;
        appendChatMessage("[ZIEL] " + playerName + " hat Checkpoint #" + checkpointNumber + " erreicht! 🏁");

        // Optional: UI-Markierung im Spielfeld oder Spieleranzeige
    }

    /**
     * Zeigt das Spielende an und informiert den Benutzer, ob er gewonnen oder verloren hat.
     *
     * @param isWinner true, wenn der Spieler gewonnen hat; false sonst
     */
    public void showGameResult(boolean isWinner) {
        String resultText = isWinner ? "🎉 Glückwunsch, du hast GEWONNEN! 🎉"
                : "☠️ Leider verloren. Versuch es erneut! ☠️";
        appendChatMessage("[SPIELENDE] " + resultText);

        // Optional: Fenster, Animation oder Szenewechsel
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Spiel beendet");
        alert.setHeaderText(null);
        alert.setContentText(resultText);
        alert.showAndWait();
    }


    public void showInstructionDialog() {
        Label title = new Label("Nächster Schritt");
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
//DAMAGE CARDS

    /**
     * Zeigt dem Spieler die gezogenen Schadenskarten an.
     *
     * <p>Diese Methode wird aufgerufen, wenn der Server dem Spieler automatisch Schadenskarten zuweist.
     * Sie zeigt die entsprechenden Kartengrafiken im Handkartenbereich an.</p>
     *
     * @param cards Liste der Schadenskarten (z. B. ["spam", "worm", "trojan_horse"])
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
            view.setFitWidth(60);
            view.setFitHeight(90);
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
            view.setFitWidth(60);
            view.setFitHeight(90);
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
                    // Desactiva más clics
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

        // 🟠 Hover-Effekt
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

        // 🖱️ Klick-Ereignis → fügt Karte ins nächste freie Registerfeld ein
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
        appendChatMessage("[INFO] " + playerName + " fell off the chessboard!");

    }

    /**
     * Display an error message dialog box.
     *
     * @param title   Dialog box title.
     * @param message Error message.
     */
    public void displayErrorAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    /**
     * Highlight the register slot to indicate an error.
     *
     * @param registerSlot The index of the register slot to be highlighted (0-4).
     */
    public void highlightRegisterSlot(int registerSlot) {
        if (registerSlot < 0 || registerSlot >= 5 || registerBox == null) {
            return;
        }

        Platform.runLater(() -> {
            if (registerBox.getChildren().size() > registerSlot) {
                Node node = registerBox.getChildren().get(registerSlot);
                if (node instanceof VBox vbox && vbox.getChildren().size() > 1) {
                    Node slotNode = vbox.getChildren().get(1);
                    if (slotNode instanceof StackPane pane) {
                        String originalStyle = pane.getStyle();
                        pane.setStyle(originalStyle + "; -fx-border-color: red; -fx-border-width: 3px; -fx-effect: dropshadow(gaussian, #ff0000, 10, 0.5, 0, 0);");
                        PauseTransition pause = new PauseTransition(Duration.seconds(2));
                        pause.setOnFinished(e -> pane.setStyle(originalStyle));
                        pause.play();
                    }
                }
            }
        });
    }

    private int currentPhaseID = -1;

    public void setCurrentPhaseID(int phaseID) {
        this.currentPhaseID = phaseID;
    }

    public int getCurrentPhaseID() {
        return currentPhaseID;
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


}



