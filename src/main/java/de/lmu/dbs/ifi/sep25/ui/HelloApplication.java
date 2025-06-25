package de.lmu.dbs.ifi.sep25.ui;



import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

    /**
     * Startklasse für die JavaFX-Anwendung "RoboRally".
     * <p>
     * Diese Klasse lädt die Login-Oberfläche und stellt vorab eine Verbindung
     * zum Server über einen separaten Netzwerk-Thread her.
     */
    public class HelloApplication extends Application {

        /**
         * Einstiegspunkt für JavaFX. Lädt die Login-Oberfläche (LoginView.fxml) und zeigt das Fenster.
         *
         * @param stage Das Hauptfenster der Anwendung.
         * @throws Exception wenn das FXML nicht geladen werden kann.
         */
        @Override
        public void start(Stage stage) throws Exception {
            // Lade die Login-Oberfläche aus dem FXML-File
            FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/de/lmu/dbs/ifi/sep25/LoginView.fxml"));
            Scene scene = new Scene(fxmlLoader.load(), 400, 300);
            scene.getStylesheets().add(getClass().getResource("/style.css").toExternalForm());
            // Controller holen und Stage setzen
            LoginController controller = fxmlLoader.getController();
            controller.setStage(stage);
            // Setze Fenstertitel und Szene
            stage.setTitle("RoboRally – Login");
            stage.setScene(scene);
            stage.show();
        }


//startClientConnection via MAIN

}
