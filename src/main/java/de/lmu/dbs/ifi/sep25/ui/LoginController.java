package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.BodyPlayerValues;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.Message;

import java.io.IOException;

/**
 * Controller für die Login-Oberfläche.
 *
 * Verarbeitet Benutzereingaben für Name und Spielfigur,
 * sendet Login-Nachrichten an den Server und wechselt bei Erfolg zur Lobby-Ansicht.
 */
public class LoginController {
    @FXML
    private TextField hostField;

    @FXML
    private TextField portField;

    /** Eingabefeld für den Spielernamen. */
    @FXML
    private TextField nameField;

    /** Auswahlfeld für die Spielfigur (Index 0–5). */
    @FXML
    private ComboBox<Integer> figureBox;

    /** Property zur Bindung des Spielernamens. */
    private StringProperty playerName = new SimpleStringProperty();

    /** Property zur Bindung der ausgewählten Spielfigur. */
    private ObjectProperty<Integer> selectedFigure = new SimpleObjectProperty<>();

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
    }

    /**
     * Wird aufgerufen, wenn der Nutzer auf "Login" klickt.
     * Validiert die Eingaben und sendet eine Login-Nachricht an den Server.
     *
     * @param event Das zugehörige ActionEvent (nicht verwendet).
     */
    @FXML
    private void handleLogin(ActionEvent event) {
        String name = nameField.getText();
        Integer figure = figureBox.getValue();
        String host =  hostField.getText();
        String portText = portField.getText();

        if (name == null || name.isBlank() || figure == null) {
            showAlert("Bitte Namen und Spielfigur auswählen.");
            return;
        }
        if (host == null || host.isBlank() || portText == null || portText.isBlank()) {
            showAlert("Bitte IP-Adresse und Port eingeben.");
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
            port = Integer.parseInt(portText);
        } catch (NumberFormatException e) {
            showAlert("Ungültiger Port. Bitte eine gültige Zahl eingeben.");
            return;
        }

        try {
            Client client = new Client();
            client.start(host, port);
            ClientSingleton.setInstance(client);

        } catch (Exception e) {
            showAlert("Verbindung fehlgeschlagen: " + e.getMessage());
            return;
        }

        Message<BodyPlayerValues> msg = new Message<>(new BodyPlayerValues(name, figure));
        ClientSingleton.getInstance().sendMessage(msg);
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
            ControllerRegistry.setLobbyController(controller);

            Stage stage = (Stage) nameField.getScene().getWindow();
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
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Figur vergeben");
        alert.setHeaderText("Diese Spielfigur wurde bereits gewählt");
        alert.setContentText("Bitte wähle eine andere Figur aus.");
        alert.showAndWait();

        // Auswahlfelder wieder aktivieren
        nameField.setDisable(false);
        figureBox.setDisable(false);
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
}
