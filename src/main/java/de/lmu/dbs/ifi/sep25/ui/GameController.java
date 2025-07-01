package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Map.entry;

public class GameController {
    // 0. Logger
    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");
    private static final Logger appLogger = LogManager.getLogger(GameController.class);

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
    private static final int TILE_SIZE = 60;

    private final Map<String, Image> tileImages = new HashMap<>();
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
                                imageKey = "wall_laser_" + laser.count() + "_" + (laser.isActive() ? "on" : "off");
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
                    appLogger.info("Adding conveyor tile {} with rotation {}", tileName, rotation);
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
                        System.err.println("Fehlendes Bild: " + key);
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
            System.err.println("Fehlendes Bild: " + key);
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
    private void handleStartPointClick(StackPane pane) {
        Integer x = GridPane.getColumnIndex(pane);
        Integer y = GridPane.getRowIndex(pane);
        if (x == null || y == null) return;

        var msg = new MessageDefinitions.Message<>(new MessageDefinitions.BodySetStartingPoint(x, y, "up"));
        ClientSingleton.getInstance().sendMessage(msg);
        System.out.printf("[DEBUG] Starting position selected at (%d, %d)%n", x, y);

        pane.setOnMouseClicked(null);
        pane.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
        appendChatMessage("[INFO] Starting position selected at (" + x + ", " + y + ")");
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

        System.err.println("无法确定传送带类型，使用默认直线传送带");
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
        boolean isSetupPhase = "Aufbauphase".equals(phaseName);
        // Label aktualisieren
        if (phaseLabel != null) {
            phaseLabel.setText("Phase: " + phaseName);
        }

        // Update availability of click start position depending on phase
        for (javafx.scene.Node node : gameBoardPane.getChildren()) {
            if (node instanceof StackPane pane) {
                // Update the availability of the starting position click based on the stage
                if (pane.getOnMouseClicked() != null) {
                    if (!isSetupPhase) {
                        // Disable clicks outside the setup phase.
                        pane.setOnMouseClicked(null);
                        pane.setStyle("-fx-border-color: gray; -fx-border-width: 2px;");
                    }
                }
            }
        }
        // Info in Chat
        appendChatMessage("[INFO] Current phase: " + phaseName);
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
        System.out.println("[INFO] Aktueller Spieler ist: " + clientID);

        // Spielername herausfinden (aus der ComboBox)
        for (PlayerEntry entry : recipientBox.getItems()) {
            if (entry.getClientID() == clientID) {
                String playerName = entry.getName();

                // Nachricht im Chat anzeigen
                appendChatMessage("[INFO] " + playerName + " ist am Zug.");

                // Status-Label oben aktualisieren
                statusLabel.setText(playerName + " ist am Zug.");

                // Optional: Spieler in ComboBox markieren
                recipientBox.getSelectionModel().select(entry);

                return;
            }
        }

        // Falls der Spieler nicht gefunden wurde
        appendChatMessage("[INFO] Spieler mit ID " + clientID + " ist am Zug.");
        statusLabel.setText("Spieler " + clientID + " ist am Zug.");
    }


    /**
     * Zeigt die Startposition eines Roboters im Spielfeld an.
     *
     * @param x         X-Koordinate auf dem Spielfeld
     * @param y         Y-Koordinate auf dem Spielfeld
     * @param clientID  Die Client-ID des Spielers
     * @param direction Die Ausrichtung des Roboters (z. B. "right", "left", "up", "down")
     */
    public void displayStartingPoint(int x, int y, int clientID, String direction) {
        try {
            String imagePath = "/assets/robot_" + clientID + ".png";
            Image robotImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            ImageView robotView = new ImageView(robotImg);
            robotView.setFitWidth(40);
            robotView.setFitHeight(40);
            robotView.setPreserveRatio(true);

            // Drehe Bild je nach Richtung
            switch (direction.toLowerCase()) {
                case "right" -> robotView.setRotate(0);
                case "down" -> robotView.setRotate(90);
                case "left" -> robotView.setRotate(180);
                case "up" -> robotView.setRotate(270);
            }

            // Setze Roboter auf das Spielfeld (Grid)
            StackPane tile = getTileAt(x, y);
            tile.getChildren().add(robotView);

        } catch (Exception e) {
            System.err.println("[FEHLER] Roboter konnte nicht angezeigt werden an (" + x + "," + y + ")");
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
     * @param cardNames Liste der Kartennamen (z. B. "MoveI", "TurnLeft", ...)
     */
    public void displayHandCards(List<String> cardNames) {
        if (cardNames == null || cardNames.isEmpty()) {
            System.err.println("[INFO] Spieler hat keine Karten erhalten.");
            return;
        }

        // Beispiel-Container,  im FXML: <HBox fx:id="handCardBox" />
        HBox handCardBox = (HBox) root.lookup("#handCardBox");
        if (handCardBox == null) {
            System.err.println("[FEHLER] Kein Container für Handkarten gefunden (fx:id=handCardBox).");
            return;
        }

        handCardBox.getChildren().clear();

        for (String name : cardNames) {
            String imagePath = "/assets/cards/" + name.toLowerCase() + ".png";

            Image img;
            try {
                img = new Image(getClass().getResourceAsStream(imagePath));
            } catch (Exception e) {
                System.err.println("[WARNUNG] Bild nicht gefunden für Karte: " + name + " → verwende Platzhalter.");
                img = new Image(getClass().getResourceAsStream("/assets/cover.png"));
            }

            ImageView view = new ImageView(img);
            view.setFitWidth(60);
            view.setFitHeight(90);
            view.setPreserveRatio(true);
            view.setSmooth(true);

            handCardBox.getChildren().add(view);
        }
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
    public void handleCardSelection(int clientID, boolean filled) {
        String message = filled
                ? "Spieler " + clientID + " hat sein Programm fertiggestellt."
                : "Spieler " + clientID + " hat eine Karte ausgewählt.";
        appendChatMessage("[INFO] " + message);
    }

    /**
     * Hebt hervor, dass ein Spieler sein Programm abgeschlossen hat.
     *
     * @param clientID Die ID des Spielers
     */
    public void markPlayerReady(int clientID) {
        appendChatMessage("[INFO] Spieler " + clientID + " ist bereit.");
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
     *
     * <p>Funktionsweise:</p>
     * <ul>
     *   <li>Zeigt eine Nachricht im Chat an, dass die Zeit abgelaufen ist.</li>
     *   <li>Listet die IDs der Spieler auf, die zu langsam waren, falls vorhanden.</li>
     *   <li>Markiert diese Spieler visuell in der Empfänger-ComboBox (z. B. durch ein "✖" vor ihrem Namen).</li>
     *   <li>Setzt die Auswahl in der ComboBox zurück, falls ein markierter Spieler ausgewählt war.</li>
     * </ul>
     *
     * @param slowPlayers Eine Liste von Spieler-IDs, die zu langsam waren. Kann null oder leer sein.
     */
    public void showTimerEnded(List<Integer> slowPlayers) {
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
        for (javafx.scene.Node node : gameBoardPane.getChildren()) {
            if (GridPane.getColumnIndex(node) == x && GridPane.getRowIndex(node) == y) {
                return (StackPane) node;
            }
        }
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
 * Aktualisiert die Position eines Roboters auf dem Spielfeld.
 *
 * @param clientID ID des Spielers/Roboters
 * @param x        Neue X-Position
 * @param y        Neue Y-Position
 */
    /**
     * Bewegt den Roboter eines Spielers auf das Feld (x, y).
     *
     * @param clientID Die ID des Spielers
     * @param x        Die Zielspalte
     * @param y        Die Zielzeile
     */
    public void moveRobotTo(int clientID, int x, int y) {
        try {
            String imagePath = "/assets/robot_" + clientID + ".png";
            Image robotImg = new Image(Objects.requireNonNull(getClass().getResourceAsStream(imagePath)));
            ImageView robot = new ImageView(new Image(getClass().getResourceAsStream("/assets/cover.png")));

            ImageView robotView = new ImageView(robotImg);
            robotView.setFitWidth(40);
            robotView.setFitHeight(40);
            robotView.setPreserveRatio(true);

            // Bestehende Roboter entfernen (optional)
            StackPane cell = getCellAt(x, y);
            if (cell != null) {
                cell.getChildren().removeIf(n -> n instanceof ImageView && ((ImageView) n).getImage().getUrl().contains("robot"));
                cell.getChildren().add(robot);
            }

            appendChatMessage("[BEWEGUNG] Spieler " + clientID + " wurde nach (" + x + ", " + y + ") bewegt.");
        } catch (Exception e) {
            System.err.println("[FEHLER] Roboterbild konnte nicht geladen werden für Spieler " + clientID);
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
        // Beispiel: aktuelle Roboterposition (vereinfachtes Beispiel)
        int x = 5; // TODO: echte Roboterposition verwenden
        int y = 5;

        StackPane cell = getCellAt(x, y);
        if (cell != null) {
            for (javafx.scene.Node node : cell.getChildren()) {
                if (node instanceof ImageView img && img.getImage().getUrl().contains("robot")) {
                    double currentRotation = img.getRotate();
                    img.setRotate(rotation.equals("clockwise") ? currentRotation + 90 : currentRotation - 90);
                    appendChatMessage("[DREHUNG] Spieler " + clientID + " dreht sich " + rotation + ".");
                    break;
                }
            }
        }
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
            case "up" -> "↑";
            case "down" -> "↓";
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

}

