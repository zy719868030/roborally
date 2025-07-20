package de.lmu.dbs.ifi.sep25.ui;

import fr.brouillard.oss.cssfx.CSSFX;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import java.io.IOException;
import java.net.URL;
import java.util.Objects;
/**
 * Main JavaFX application class responsible for initializing and launching the GUI.
 * <p>
 * It supports switching between FXML-based and programmatic view creation via the
 * {@code CREATE_VIEW_FROM_FXML} flag and enables CSS hot-reloading using {@code CSSFX}.
 */
public class App extends Application {

    /**
     * {@code true} - the view is created using FXML ({@code demo.fxml}) <br>
     * {@code false} - the view is created programmatically using {@link DemoView}.
     */
    private static final boolean CREATE_VIEW_FROM_FXML = false;

    /**
     * Launches the JavaFX application.
     *
     * @param args command-line arguments passed to the application
     */
    public static void main(String[] args) {
        System.out.println("Execution order");
        launch(args);
    }

    /**
     * Initializes the application before the UI is shown.
     * <p>
     * This method is called once before {@link #start(Stage)} and is used to perform setup tasks,
     * such as enabling CSS hot-reloading via {@code CSSFX}.
     */
    @Override
    public void init() {
        System.out.println("1: init()");
        CSSFX.start(); // Enable CSS hot reload
    }

    /**
     * Starts the JavaFX application by setting up the stage and scene.
     *
     * @param primaryStage the primary stage for this application
     * @throws IOException if the FXML or other resources cannot be loaded
     */
    @Override
    public void start(Stage primaryStage) throws IOException {
        System.out.println("2: start()");

        primaryStage.setTitle("Demo");
        primaryStage.getIcons().add(loadIcon());

        Scene scene = loadScene();
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    /**
     * Stops the application and performs cleanup.
     * <p>
     * Called when the application is about to shut down.
     */
    @Override
    public void stop() {
        System.out.println("3: stop()");
    }

    /**
     * Loads the JavaFX {@link Scene}, either from FXML or programmatically.
     *
     * @return the constructed {@link Scene}
     * @throws IOException if loading from FXML fails
     */
    private Scene loadScene() throws IOException {
        Parent root;
        if (CREATE_VIEW_FROM_FXML) {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/demo.fxml")));
        } else {
            root = new DemoView();
        }
        return new Scene(root);
    }

    /**
     * Loads the application icon from the resources.
     *
     * @return the loaded {@link Image}
     */
    private Image loadIcon() {
        URL url = getClass().getResource("/demo-icon.png");
        return new Image(Objects.requireNonNull(url).toString());
    }
}
