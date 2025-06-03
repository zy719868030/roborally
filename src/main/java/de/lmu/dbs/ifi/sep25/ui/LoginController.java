package de.lmu.dbs.ifi.sep25.ui;

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

public class LoginController {

    @FXML
    private TextField nameField;

    @FXML
    private ComboBox<Integer> figureBox;

    @FXML
    public void initialize() {
        for (int i = 0; i <= 5; i++) {
            figureBox.getItems().add(i);
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String name = nameField.getText();
        Integer figure = figureBox.getValue();

        if (name == null || name.isBlank() || figure == null) {
            showAlert("Bitte Namen und Spielfigur auswählen.");
            return;
        }

        Message<BodyPlayerValues> msg = new Message<>(new BodyPlayerValues(name, figure));
        ClientSingleton.getInstance().sendMessage(msg);
    }

    public void loginSuccess(String name, int figure) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/de/lmu/dbs/ifi/sep25/ui/LobbyView.fxml"));
            Parent root = loader.load();

            LobbyController controller = loader.getController();
            ControllerRegistry.setLobbyController(controller);

            Stage stage = (Stage) nameField.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Lobby");
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Fehler beim Laden der Lobby.");
        }
    }

    private void showAlert(String text) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setHeaderText(null);
        alert.setContentText(text);
        alert.showAndWait();
    }
}
