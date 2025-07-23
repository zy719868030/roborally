package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.utils.ErrorDialogUtil;
import javafx.animation.*;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;


/**
 * Controller for the login UI.
 * <p>
 * Processes user input for name and figure,
 * sends login messages to the server, and switches to the lobby view on success.
 */
public class LoginController {
    private static final Logger logger = LogManager.getLogger(LoginController.class);
    private final ToggleGroup figureToggleGroup = new ToggleGroup();
    private final Set<Integer> takenFigures = new HashSet<>();
    public String cachedName;
    public int cachedFigure;
    @FXML
    private TextField hostField;
    @FXML
    private TextField portField;
    /**
     * Input field for the player name.
     */
    @FXML
    private TextField nameField;
    /**
     * Selection field for the figure (index 0–5).
     */
    @FXML
    private ComboBox<Integer> figureBox;
    @FXML
    private javafx.scene.control.Button loginButton;
    @FXML
    private Label warningLabel;
    @FXML
    private HBox figureGallery;
    @FXML
    private ScrollPane figureScrollPane;
    @FXML
    private Button scrollLeftButton, scrollRightButton;
    /**
     * Property for binding the player name.
     */
    private final StringProperty playerName = new SimpleStringProperty();
    /**
     * Property for binding the selected figure.
     */
    private final ObjectProperty<Integer> selectedFigure = new SimpleObjectProperty<>();
    private Stage stage;
    private boolean figureTakenWarningShown = false;

    @FXML
    /**
     * Initializes the login UI.
     * <p>
     * Loads the available robot figures into the gallery, sets up event handlers for selection
     * and scroll buttons, and binds the input fields for player name and figure.
     * Ensures that warnings are hidden when a valid selection is made.
     * Registers the controller in the ControllerRegistry.
     */
    private void initialize() {
        int robotCount = 6;

        // Clear any previous state (important for reusability)
        figureGallery.getChildren().clear();
        figureToggleGroup.getToggles().clear();

        // Load robot figures into the gallery
        for (int i = 0; i < robotCount; i++) {
            ToggleButton button = new ToggleButton();
            button.setToggleGroup(figureToggleGroup);
            button.setUserData(i);
            button.getStyleClass().add("robot-choice");

            String path = String.format("/assets/robots/robot_%02d_left.png", i);
            Image image = new Image(getClass().getResourceAsStream(path));
            ImageView iv = new ImageView(image);
            iv.setFitWidth(80);
            iv.setFitHeight(80);
            iv.setPreserveRatio(true);
            button.setGraphic(iv);

            figureGallery.getChildren().add(button);
        }

        // Initialize carousel with all behavior (scroll, drag, select)
        CarouselManager carouselManager = new CarouselManager(
                figureScrollPane, figureGallery, figureToggleGroup, robotCount
        );

        // Buttons to select left/right robot
        scrollLeftButton.setOnAction(_ -> carouselManager.selectPrevious());
        scrollRightButton.setOnAction(_ -> carouselManager.selectNext());

        // Selection state (update selectedFigure when user clicks robot)
        figureToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                selectedFigure.set((Integer) newVal.getUserData());
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            }
        });

        // Player name bidirectional binding
        nameField.textProperty().bindBidirectional(playerName);
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.isBlank() && selectedFigure.get() != null) {
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            }
        });

        // Hide warning when selection is valid
        selectedFigure.addListener((obs, oldVal, newVal) -> {
            if (newVal != null && nameField.getText() != null && !nameField.getText().isBlank()) {
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            }
        });

        ControllerRegistry.setLoginController(this);
    }

    /**
     * Called when the user clicks "Login".
     * Validates the inputs for name and figure, uses default values for host and port,
     * connects to the server, and stores the user data.
     * Shows appropriate warnings on errors and disables the login button during processing.
     *
     * @param event The associated ActionEvent.
     */
    @FXML
    private void handleLogin(ActionEvent event) {

        // Deaktiviere Login-Button, damit man nicht mehrfach klickt
        ((javafx.scene.Node) event.getSource()).setDisable(true);
        String name = nameField.getText();
        Integer figure = selectedFigure.get();
        // Falls Host oder Port leer → Standardwerte nutzen
        String host = (hostField.getText() == null || hostField.getText().isBlank()) ? "localhost" : hostField.getText();
        String portText = (portField.getText() == null || portField.getText().isBlank()) ? "12345" : portField.getText();
        logger.info("Login attempt as '{}' with figure {}", name, figure);

        if ((name == null || name.isBlank()) && figure == null) {
            displaySelectionError("Bitte Namen & Figur auswählen", event);
            return;
        } else if (name == null || name.isBlank()) {
            displaySelectionError("Bitte einen Namen auswählen", event);
            return;
        } else if (figure == null) {
            displaySelectionError("Bitte eine Figur auswählen", event);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showAlert("Ungültiger Port. Bitte eine gültige Zahl eingeben.");
            return;
        }

        try {
            Client client;
            client = new Client();
            client.start(host, port); // HelloServer wird automatisch nach HelloClient gesendet
            ClientSingleton.setInstance(client);

            cachedName = name;
            cachedFigure = figure;

        } catch (Exception e) {
            showAlert("Verbindung fehlgeschlagen: " + e.getMessage());
            ((Node) event.getSource()).setDisable(false);

        }
    }

    /**
     * Shows an error message if the selection is invalid.
     * Sets the warning label visible, plays a shake animation for the input field,
     * and re-enables the button.
     *
     * @param errorText The error text to display.
     * @param event     The associated ActionEvent.
     */
    private void displaySelectionError(String errorText, ActionEvent event) {
        warningLabel.setText(errorText);
        warningLabel.setVisible(true);
        warningLabel.setManaged(true);
        shakeNode(nameField.getParent());
        ((Node) event.getSource()).setDisable(false);
    }

    @FXML
    public void onBotStartButton(ActionEvent event) {
        // Get the Stage from the event (the window the button is in)
        Stage owner = (Stage) ((Node) event.getSource()).getScene().getWindow();
        showBotPopup(owner);
    }

    private void showBotPopup(Stage owner) {
        // Popup root
        VBox content = new VBox(22);
        content.setAlignment(Pos.CENTER);
        content.setStyle("-fx-background-color: rgba(20, 20, 30, 0.95);"
                + "-fx-padding: 32 36;"
                + "-fx-background-radius: 18; "
                + "-fx-effect: dropshadow(gaussian, #00ffd0, 16, 0.7, 0, 0);");

        Label title = new Label("Bot-Schwierigkeit wählen");
        title.getStyleClass().add("status-label");
        title.setStyle("-fx-font-size: 21px; -fx-font-weight: bold; -fx-text-fill: #00ffd0;");

        // Dropdown styled as map selection
        ComboBox<String> botTypeBox = new ComboBox<>();
        botTypeBox.getItems().addAll("Random Bot", "Pathfinding Bot");
        botTypeBox.setPromptText("Bot auswählen...");
        botTypeBox.getStyleClass().add("combo-recipient");
        botTypeBox.setPrefWidth(210);

        // Button row (like info popup)
        HBox btnBox = new HBox(18);
        btnBox.setAlignment(Pos.CENTER);

        Button startBtn = new Button("Bot starten");
        startBtn.getStyleClass().add("senden-button");
        startBtn.setStyle("-fx-font-size: 20px; -fx-background-radius: 10px;");
        startBtn.setPrefWidth(170);
        startBtn.setCursor(Cursor.HAND);

        // Shrink effect on hover
        startBtn.setOnMouseEntered(e -> startBtn.setStyle(
                "-fx-font-size: 17px; -fx-background-radius: 10px;"
        ));
        startBtn.setOnMouseExited(e -> startBtn.setStyle(
                "-fx-font-size: 20px; -fx-background-radius: 10px;"
        ));

        Button cancelBtn = new Button("Abbrechen");
        cancelBtn.getStyleClass().add("not-ready-button");
        cancelBtn.setPrefWidth(120);
        cancelBtn.setCursor(Cursor.HAND);

        btnBox.getChildren().addAll(startBtn, cancelBtn);

        content.getChildren().addAll(title, botTypeBox, btnBox);

        // Create a draggable undecorated Stage
        Stage popup = new Stage(StageStyle.TRANSPARENT);
        popup.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) popup.initOwner(owner);

        // Draggable logic
        final double[] dragOffset = new double[2];
        content.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                dragOffset[0] = e.getSceneX();
                dragOffset[1] = e.getSceneY();
            }
        });
        content.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.PRIMARY) {
                popup.setX(e.getScreenX() - dragOffset[0]);
                popup.setY(e.getScreenY() - dragOffset[1]);
            }
        });

        // Animate in
        content.setOpacity(0);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(300), content);
        scaleIn.setFromX(0.84); scaleIn.setFromY(0.84);
        scaleIn.setToX(1.0); scaleIn.setToY(1.0);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(280), content);
        fadeIn.setFromValue(0); fadeIn.setToValue(1);
        ParallelTransition anim = new ParallelTransition(scaleIn, fadeIn);
        anim.play();

        // Setup scene with transparent background, hand cursor on combo, rounded corners
        Scene scene = new Scene(content);
        scene.setFill(Color.TRANSPARENT);
        botTypeBox.setCursor(Cursor.HAND);
        content.setStyle(content.getStyle() + "-fx-cursor: default;");
        popup.setScene(scene);

        // Remove window borders
        popup.initStyle(StageStyle.TRANSPARENT);

        // Button logic
        startBtn.setOnAction(ev -> {
            String botType = botTypeBox.getValue();
            if (botType == null) {
                botTypeBox.setStyle("-fx-border-color: #ff4444; -fx-border-width: 2px;");
                return;
            }
            // LAUNCH BOT here (with ProcessBuilder or other, as discussed)
            launchBot(botType);
            popup.close();
        });
        cancelBtn.setOnAction(ev -> popup.close());

        popup.show();
    }

    // Sample bot launch method
    private void launchBot(String botType) {
        // Map type to cmdline arg
        String typeArg = botType.equals("Pathfinding Bot") ? "-smart" : "-random";
        String host = (hostField.getText() == null || hostField.getText().isBlank()) ? "localhost" : hostField.getText();
        String port = (portField.getText() == null || portField.getText().isBlank()) ? "12345" : portField.getText();
        try {
            new ProcessBuilder("java", "-jar", "roborally.jar", "--bot", typeArg, host, port)
                    .start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Called after a successful login and loads the lobby UI.
     * <p>
     * Loads the FXML of the lobby, initializes the corresponding controller,
     * sets the scene, and shows the lobby window.
     * Shows an error message if loading fails.
     */
    public void loginSuccess() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/lmu/dbs/ifi/sep25/LobbyView.fxml"));
            Parent root = loader.load();

            LobbyController controller = loader.getController();
            controller.setRoot(root);
            ControllerRegistry.setLobbyController(controller);

            Stage stage = (Stage) nameField.getScene().getWindow();
            ControllerRegistry.setPrimaryStage(stage);
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Lobby");
            stage.setWidth(1200);
            stage.setHeight(900);
            stage.setMinWidth(1200);
            stage.setMinHeight(900);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fehler beim Laden der Lobby.");
        }
    }

    /**
     * Displays a styled error alert dialog on the login screen with the given title and message.
     * The dialog is always shown on the JavaFX application thread and blocks input until dismissed.
     *
     * @param title   The title text of the error alert (e.g. highlighted in red).
     * @param message The error message to display.
     */
    public void displayErrorAlert(String title, String message) {
        ErrorDialogUtil.showError(title, message);
    }

    /**
     * Called by the client if the selected figure is already taken.
     * Shows a warning and re-enables the input.
     */
    public void displayFigureAlreadyTaken() {
        ErrorDialogUtil.showError(
                "Diese Spielfigur wurde bereits gewählt",
                "Bitte wähle eine andere Figur aus.",
                () -> {
                    // Custom logic after closing the error dialog
                    Toggle selectedToggle = figureToggleGroup.getSelectedToggle();
                    if (selectedToggle != null) selectedToggle.setSelected(false);

                    selectedFigure.set(null);
                    loginButton.setDisable(false);
                    nameField.setDisable(false);
                    figureTakenWarningShown = false;

                    takenFigures.add(cachedFigure);
                    for (Toggle toggle : figureToggleGroup.getToggles()) {
                        int figID = (int) toggle.getUserData();
                        ToggleButton button = (ToggleButton) toggle;
                        if (takenFigures.contains(figID)) {
                            button.setDisable(true);
                            button.setOpacity(0.4);
                        }
                    }

                    cachedFigure = -1;
                    new Timeline(new KeyFrame(Duration.seconds(2), _ -> loginButton.setDisable(false))).play();
                }
        );
    }

    /**
     * Returns whether the warning for an already selected figure was shown.
     *
     * @return true if the warning was shown, false otherwise
     */
    public boolean isFigureTakenWarningShown() {
        return figureTakenWarningShown;
    }

    /**
     * Sets the status whether the warning for an already selected figure was shown.
     *
     * @param shown true if the warning should be shown, false otherwise
     */
    public void setFigureTakenWarningShown(boolean shown) {
        this.figureTakenWarningShown = shown;
    }

    /**
     * Shows an error message in a dialog window.
     *
     * @param text The text to display.
     */
    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    /**
     * Sets the current stage object for the controller.
     *
     * @param stage The stage window to set.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Plays a shake animation for the given node element.
     * <p>
     * The method moves the element horizontally several times to create a "shake" effect.
     *
     * @param node The node element to animate.
     */
    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(-10);
        tt.setToX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }
}

