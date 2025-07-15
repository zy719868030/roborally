package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodyPlayerValues;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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
import java.util.Random;
import java.util.Set;


/**
 * Controller für die Login-Oberfläche.
 * <p>
 * Verarbeitet Benutzereingaben für Name und Spielfigur,
 * sendet Login-Nachrichten an den Server und wechselt bei Erfolg zur Lobby-Ansicht.
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
     * Eingabefeld für den Spielernamen.
     */
    @FXML
    private TextField nameField;
    /**
     * Auswahlfeld für die Spielfigur (Index 0–5).
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
     * Property zur Bindung des Spielernamens.
     */
    private final StringProperty playerName = new SimpleStringProperty();
    /**
     * Property zur Bindung der ausgewählten Spielfigur.
     */
    private final ObjectProperty<Integer> selectedFigure = new SimpleObjectProperty<>();
    private Stage stage;
    private boolean figureTakenWarningShown = false;

    @FXML
    /**
     * Initialisiert die Login-Oberfläche.
     * <p>
     * Lädt die verfügbaren Roboterfiguren in die Galerie, setzt die Event-Handler für die Auswahl
     * und Scroll-Buttons, und bindet die Eingabefelder für den Spielernamen und die Spielfigur.
     * Stellt sicher, dass Warnhinweise bei gültiger Auswahl ausgeblendet werden.
     * Registriert den Controller im ControllerRegistry.
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
     * Wird aufgerufen, wenn der Nutzer auf "Login" klickt.
     * Validiert die Eingaben für Name und Spielfigur, verwendet Standardwerte für Host und Port,
     * stellt die Verbindung zum Server her und speichert die Nutzerdaten.
     * Zeigt bei Fehlern entsprechende Warnungen an und deaktiviert den Login-Button während der Verarbeitung.
     *
     * @param event Das zugehörige ActionEvent.
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
     * Zeigt eine Fehlermeldung an, wenn die Auswahl ungültig ist.
     * Setzt den Warnhinweis sichtbar, spielt eine Shake-Animation für das Eingabefeld
     * und aktiviert den Button wieder.
     *
     * @param errorText Der anzuzeigende Fehlertext.
     * @param event     Das zugehörige ActionEvent.
     */
    private void displaySelectionError(String errorText, ActionEvent event) {
        warningLabel.setText(errorText);
        warningLabel.setVisible(true);
        warningLabel.setManaged(true);
        shakeNode(nameField.getParent());
        ((Node) event.getSource()).setDisable(false);
    }


    /**
     * Wird aufgerufen, wenn der Nutzer auf "Bot-Login" klickt.
     * <p>
     * Deaktiviert die Eingabefelder, wählt einen zufälligen Namen und eine zufällige Spielfigur für den Bot,
     * verwendet Standardwerte für Host und Port, stellt die Verbindung zum Server her und sendet die Bot- und Spielerinformationen.
     * Zeigt bei Fehlern entsprechende Warnungen an und aktiviert die Eingabefelder wieder.
     *
     * @param event Das zugehörige ActionEvent.
     */
    @FXML
    private void handleBotLogin(ActionEvent event) {
        // Eingabefelder deaktivieren, damit der Nutzer nichts mehr ändern kann
        nameField.setDisable(true);
        figureBox.setDisable(true);
        hostField.setDisable(true);
        portField.setDisable(true);

        // Zufälligen Namen und Figur für den Bot wählen
        String name = "KI-Spieler";
        int figure = new Random().nextInt(6); // Zufällige Figur (0–5)

        // Host und Port auslesen oder Standard setzen
        String host = (hostField.getText() == null || hostField.getText().isBlank()) ? "localhost" : hostField.getText();
        String portText = (portField.getText() == null || portField.getText().isBlank()) ? "12345" : portField.getText();

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showAlert("Ungültiger Port. Bitte eine gültige Zahl eingeben.");
            return;
        }

        try {
            // Client erstellen und starten
            Client client = new Client();
            client.start(host, port);
            ClientSingleton.setInstance(client);

            // Bot als KI markieren (AI = true)
            Message<MessageDefinitions.BodyHelloServer> hello = new Message<>(new MessageDefinitions.BodyHelloServer("Edle Eisbecher", true, "Version 0.1"));
            client.sendMessage(hello);

            // Spielerinformationen (Name, Figur) an den Server senden
            client.sendMessage(new Message<>(new BodyPlayerValues(name, figure)));

        } catch (Exception e) {
            showAlert("Verbindung fehlgeschlagen: " + e.getMessage());

            // Falls Verbindung fehlschlägt, Felder wieder aktivieren
            nameField.setDisable(false);
            figureBox.setDisable(false);
            hostField.setDisable(false);
            portField.setDisable(false);
        }
    }


    /**
     * Wird nach erfolgreichem Login aufgerufen und lädt die Lobby-Oberfläche.
     * <p>
     * Lädt das FXML der Lobby, initialisiert den zugehörigen Controller,
     * setzt die Szene und zeigt das Lobby-Fenster an.
     * Bei Fehlern wird eine entsprechende Fehlermeldung angezeigt.
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
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fehler beim Laden der Lobby.");
        }
    }

    /**
     * Wird vom Client aufgerufen, wenn die gewählte Figur schon belegt ist.
     * Zeigt einen Warnhinweis an und reaktiviert die Eingabe.
     */
    public void displayFigureAlreadyTaken() {

        Platform.runLater(() -> {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.TRANSPARENT);

            Label titleLabel = new Label("Diese Spielfigur wurde bereits gewählt");
            titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #b00020;");

            Label messageLabel = new Label("Bitte wähle eine andere Figur aus.");
            messageLabel.setWrapText(true);
            messageLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: white;");
            messageLabel.setMaxWidth(300);

            Button okButton = new Button("OK");
            okButton.setDefaultButton(true);
            okButton.setStyle("""
                        -fx-background-color: #e0e0e0;
                        -fx-text-fill: black;
                        -fx-font-size: 13px;
                        -fx-padding: 6 14 6 14;
                        -fx-background-radius: 6;
                        -fx-border-radius: 6;
                        -fx-cursor: hand;
                    """);

            okButton.setOnMouseEntered(e -> {
                okButton.setScaleX(1.1);
                okButton.setScaleY(1.1);
            });

            okButton.setOnMouseExited(e -> {
                okButton.setScaleX(1.0);
                okButton.setScaleY(1.0);
            });

            okButton.setOnAction(_ -> dialog.close());

            VBox layout = new VBox(12, titleLabel, messageLabel, okButton);
            layout.setAlignment(Pos.CENTER);
            layout.setPadding(new Insets(20));
            layout.setStyle("""
                        -fx-background-color: #1a1a1a;
                        -fx-background-radius: 12;
                        -fx-border-radius: 12;
                        -fx-border-color: #b00020;
                        -fx-border-width: 2;
                    """);

            Scene scene = new Scene(layout);
            scene.setFill(Color.TRANSPARENT);

            scene.setOnKeyPressed(event -> {
                switch (event.getCode()) {
                    case ESCAPE, SPACE, ENTER -> dialog.close();
                }
            });

            dialog.setScene(scene);
            dialog.setResizable(false);
            dialog.show();
            scene.getRoot().requestFocus();
        });

        // Logic for restoring UI state after error
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

    /**
     * Gibt zurück, ob die Warnung für eine bereits gewählte Spielfigur angezeigt wurde.
     *
     * @return true, wenn die Warnung angezeigt wurde, sonst false
     */
    public boolean isFigureTakenWarningShown() {
        return figureTakenWarningShown;
    }

    /**
     * Setzt den Status, ob die Warnung für eine bereits gewählte Spielfigur angezeigt wurde.
     *
     * @param shown true, wenn die Warnung angezeigt werden soll, sonst false
     */
    public void setFigureTakenWarningShown(boolean shown) {
        this.figureTakenWarningShown = shown;
    }

    /**
     * Zeigt eine Fehlernachricht in einem Dialogfenster an.
     *
     * @param text Der anzuzeigende Text.
     */
    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }

    /**
     * Setzt das aktuelle Stage-Objekt für den Controller.
     *
     * @param stage Das zu setzende Stage-Fenster.
     */
    public void setStage(Stage stage) {
        this.stage = stage;
    }

    /**
     * Spielt eine Shake-Animation für das angegebene Node-Element ab.
     * <p>
     * Die Methode verschiebt das Element mehrfach horizontal, um einen "Shake"-Effekt zu erzeugen.
     *
     * @param node Das zu animierende Node-Element.
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

