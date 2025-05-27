package de.lmu.dbs.ifi.sep25;

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
 * Main class to start the application
 */
public class App extends Application {

    /**
     * {@code true} - the view is created with fxml code<p>
     * {@code false} - the view is created with java code ({@link DemoView})
     */
    private static final boolean CREATE_VIEW_FROM_FXML = false;

    public static void main(String[] args) {
        System.out.println("Execution order");
        launch(args);
    }

    @Override
    public void init() {
        // do some preparation
        System.out.println("1: init()");

        // enable hot reloading for CSS changes
        CSSFX.start();
    }

    @Override
    public void start(Stage primaryStage) throws IOException {
        // initialize the GUI
        System.out.println("2: start()");

        // setting stage title and icon
        primaryStage.setTitle("Demo");
        primaryStage.getIcons().add(loadIcon());

        // loading scene into primaryStage
        Scene scene = loadScene();
        primaryStage.setScene(scene);

        // showing the stage on screen
        primaryStage.show();
    }

    @Override
    public void stop() {
        // do some manual cleanup
        System.out.println("3: stop()");
    }

    private Scene loadScene() throws IOException {
        Parent root;
        if (CREATE_VIEW_FROM_FXML) {
            root = FXMLLoader.load(Objects.requireNonNull(getClass().getResource("/demo.fxml")));
        } else {
            root = new DemoView();
        }
        return new Scene(root);
    }

    private Image loadIcon() {
        URL url = getClass().getResource("/demo-icon.png");
        return new Image(Objects.requireNonNull(url).toString());
    }
}

