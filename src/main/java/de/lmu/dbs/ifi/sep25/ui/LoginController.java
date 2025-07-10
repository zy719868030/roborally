package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodyPlayerValues;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;
import javafx.animation.TranslateTransition;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.InputStream;
import java.util.Random;

import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import javafx.geometry.Pos;
import javafx.scene.layout.VBox;
import javafx.scene.Node;


/**
 * Controller für die Login-Oberfläche.
 * <p>
 * Verarbeitet Benutzereingaben für Name und Spielfigur,
 * sendet Login-Nachrichten an den Server und wechselt bei Erfolg zur Lobby-Ansicht.
 */
public class LoginController {
    private static final Logger logger = LogManager.getLogger(LoginController.class);
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

    private final ToggleGroup figureToggleGroup = new ToggleGroup();
    //private int selectedFigure = -1;

    /**
     * Property zur Bindung des Spielernamens.
     */
    private StringProperty playerName = new SimpleStringProperty();

    /**
     * Property zur Bindung der ausgewählten Spielfigur.
     */
    private ObjectProperty<Integer> selectedFigure = new SimpleObjectProperty<>();
    private Stage stage;
    private boolean figureTakenWarningShown = false;

    /**
     * Initialisiert die Login-Oberfläche:
     * - registriert den Controller
     * - füllt die Auswahlbox für Spielfiguren
     * - bindet UI-Komponenten an Properties
     */
    @FXML
    private void initialize() {
        // Dynamisch Roboter-Figuren laden
        for (int i = 0; i <= 5; i++) {
            final int figureID = i;

            ToggleButton button = new ToggleButton();
            button.setToggleGroup(figureToggleGroup);
            button.setUserData(figureID);
            button.getStyleClass().add("robot-choice");

            String path = String.format("/assets/robots/robot_%02d_left.png", i);
            InputStream stream = getClass().getResourceAsStream(path);
            if (stream == null) {
                System.err.println("Fehlendes Bild: " + path);
            }
            Image image = new Image(stream);
            ImageView iv = new ImageView(image);
            iv.setFitWidth(80);
            iv.setFitHeight(80);
            iv.setPreserveRatio(true);
            button.setGraphic(iv);

            button.setOnAction(e -> {
                selectedFigure.set(figureID);
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            });

            figureGallery.getChildren().add(button);
        }
        figureToggleGroup.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
            if (newToggle != null) {
                Integer figureID = (Integer) newToggle.getUserData();
                selectedFigure.set(figureID);
            } else {
                selectedFigure.set(null); // keine Auswahl
            }
        });


        // Scroll-Pfeile
        scrollLeftButton.setOnAction(e -> scrollBy(-100));
        scrollRightButton.setOnAction(e -> scrollBy(100));

        // Setze Referenz in Registry
        ControllerRegistry.setLoginController(this);

        // Name binding
        nameField.textProperty().bindBidirectional(playerName);

        // Validation: Name + Figur ausgewählt?
        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if (newText != null && !newText.isBlank() && selectedFigure.get() != null) {
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            }
        });

        selectedFigure.addListener((obs, oldVal, newVal) -> {
            String currentName = nameField.getText();
            if (newVal != null && currentName != null && !currentName.isBlank()) {
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            }
        });
    }

    private void scrollBy(double delta) {
        double current = figureScrollPane.getHvalue();
        double newValue = Math.max(0, Math.min(1, current + delta / figureGallery.getWidth()));
        figureScrollPane.setHvalue(newValue);
    }
    /**
     * Wird aufgerufen, wenn der Nutzer auf "Login" klickt.
     * Validiert die Eingaben und sendet eine Login-Nachricht an den Server.
     *
     * @param event Das zugehörige ActionEvent (nicht verwendet).
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
        logger.info("Login-Versuch mit Name '{}' und Figur {}", name, figure);


        if (name == null || name.isBlank() || figure == null) {
            warningLabel.setText("Bitte Namen und Spielfigur auswählen.");
            warningLabel.setVisible(true);
            warningLabel.setManaged(true);
            shakeNode(nameField.getParent());
            ((Node) event.getSource()).setDisable(false);
           //loginButton.setDisable(false);
            return;
        }

        int port;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showAlert("Ungültiger Port. Bitte eine gültige Zahl eingeben.");
            //loginButton.setDisable(false);
            return;
        }
        try {
            Client client = ClientSingleton.getInstance();
            if (client == null) {
                client = new Client();
                client.start(host, port); // HelloServer wird automatisch nach HelloClient gesendet
                ClientSingleton.setInstance(client);
            }


            cachedName = name;
            cachedFigure = figure;

            client.sendMessage(new Message<>(new BodyPlayerValues(name, figure)));

        } catch (Exception e) {
            showAlert("Verbindung fehlgeschlagen: " + e.getMessage());
            ((Node) event.getSource()).setDisable(false);

        }
    }

    /**
     * Wird aufgerufen, wenn der Nutzer auf "KI beitreten" klickt.
     * Stellt eine Verbindung zum Server her und meldet einen Bot-Spieler automatisch an.
     * Die Eingabefelder werden deaktiviert, um eine manuelle Eingabe zu verhindern.
     *
     * @param event Das zugehörige ActionEvent (nicht verwendet).
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
     * Wird aufgerufen, wenn der Login erfolgreich war.
     * Lädt die Lobby-Ansicht und wechselt dorthin.
     *
     * @param name   Der Benutzername
     * @param figure Die ausgewählte Spielfigur
     */
    public void loginSuccess(String name, int figure) {
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
        logger.warn("Spielfigur bereits vergeben. Zeige Warnfenster an.");

        Label title = new Label("Diese Spielfigur wurde bereits gewählt");
        title.setStyle("-fx-text-fill: #F5A623; -fx-font-size: 18px; -fx-font-weight: bold;");

        Label info = new Label("Bitte wähle eine andere Figur aus.");
        info.setWrapText(true);
        info.setStyle("-fx-text-fill: white; -fx-font-size: 14px;");

        VBox content = new VBox(15, title, info);
        content.setAlignment(Pos.CENTER);
        content.setStyle(
                "-fx-background-color: rgba(20,20,30,0.95); " +
                        "-fx-padding: 30; " +
                        "-fx-background-radius: 12; " +
                        "-fx-effect: dropshadow(gaussian, #F5A623, 10, 0.3, 0, 0);"
        );

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Figur vergeben");
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        okButton.setStyle(
                "-fx-background-color: #F5A623; " +
                        "-fx-text-fill: black; " +
                        "-fx-font-weight: bold; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8;"
        );

        dialog.getDialogPane().setStyle(
                "-fx-background-color: transparent; " +
                        "-fx-border-color: #F5A623; " +
                        "-fx-border-width: 2; " +
                        "-fx-border-radius: 12;"
        );

        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        dialog.showAndWait();

        // Auswahlfelder wieder aktivieren und alle Cached-Werte zurücksetzen
        nameField.setDisable(false);
        selectedFigure.set(null); // Auswahl zurücksetzen
        loginButton.setDisable(false);
        figureTakenWarningShown = false;
    }


    public boolean isFigureTakenWarningShown() {
        return figureTakenWarningShown;
    }

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

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String cachedName;
    public int cachedFigure;

    private void shakeNode(Node node) {
        TranslateTransition tt = new TranslateTransition(Duration.millis(50), node);
        tt.setFromX(-10);
        tt.setToX(10);
        tt.setCycleCount(6);
        tt.setAutoReverse(true);
        tt.play();
    }
}

