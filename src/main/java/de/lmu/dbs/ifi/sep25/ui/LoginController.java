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
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.Random;

import javafx.stage.StageStyle;
import javafx.util.Duration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
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
    public void initialize() {
        ControllerRegistry.setLoginController(this);

        for (int i = 0; i <= 5; i++) {
            figureBox.getItems().add(i);
        }

        nameField.textProperty().bindBidirectional(playerName);
        figureBox.valueProperty().bindBidirectional(selectedFigure);

        nameField.textProperty().addListener((obs, oldText, newText) -> {
            if (!newText.isBlank() && figureBox.getValue() != null) {
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
                    }
                });

        figureBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !nameField.getText().isBlank()) {
                warningLabel.setVisible(false);
                warningLabel.setManaged(false);
            }
        });
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
        Integer figure = figureBox.getValue();
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
            //client.sendMessage(new Message<>(new BodyPlayerValues(name, figure)));

        } catch (Exception e) {
            showAlert("Verbindung fehlgeschlagen: " + e.getMessage());
            //loginButton.setDisable(false);

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

        // Auswahlfelder wieder aktivieren
        nameField.setDisable(false);
        figureBox.setDisable(false);
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

