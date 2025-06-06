package de.lmu.dbs.ifi.sep25.ui;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class HelloApplication extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/de/lmu/dbs/ifi/sep25/ui/LoginView.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 400, 300);
        stage.setTitle("RoboRally – Login");
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        // Start the client connection in a separate thread before launching the GUI
        startClientConnection();

        // Launch the JavaFX application
        launch(args);
    }

    // Method to initialize the client and start the connection to the server
    private static void startClientConnection() {
        Client client = new Client(); // Create a new Client instance
        ClientSingleton.set(client);  // Set the client in the singleton for global access

        // Start the client in a separate thread to prevent blocking the UI
        new Thread(() -> {
            client.start("localhost", 12345); // Connect to the server
        }).start();
    }
}