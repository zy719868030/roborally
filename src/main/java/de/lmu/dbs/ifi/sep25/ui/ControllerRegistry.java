package de.lmu.dbs.ifi.sep25.ui;
import javafx.stage.Stage;
/**
 * A central registry class to store and retrieve JavaFX controller instances and the primary stage.
 * <p>
 * This allows easy access to controllers across different parts of the application without passing references.
 */
public class ControllerRegistry {
    private static LobbyController lobbyController;
    private static LoginController loginController;
    private static GameController gameController;
    private static Stage primaryStage;

    /**
     * Sets the {@link LobbyController} instance.
     *
     * @param controller the LobbyController to register
     */
    public static void setLobbyController(LobbyController controller) {
        lobbyController = controller;
    }

    /**
     * Returns the registered {@link LobbyController} instance.
     *
     * @return the LobbyController
     */
    public static LobbyController getLobbyController() {
        return lobbyController;
    }

    /**
     * Sets the {@link LoginController} instance.
     *
     * @param controller the LoginController to register
     */
    public static void setLoginController(LoginController controller) {
        loginController = controller;
    }

    /**
     * Returns the registered {@link LoginController} instance.
     *
     * @return the LoginController
     */
    public static LoginController getLoginController() {
        return loginController;
    }

    /**
     * Sets the {@link GameController} instance.
     *
     * @param controller the GameController to register
     */
    public static void setGameController(GameController controller) {
        gameController = controller;
    }

    /**
     * Returns the registered {@link GameController} instance.
     *
     * @return the GameController
     */
    public static GameController getGameController() {
        return gameController;
    }

    /**
     * Sets the primary {@link Stage} of the JavaFX application.
     *
     * @param stage the primary Stage to register
     */
    public static void setPrimaryStage(Stage stage) {
        primaryStage = stage;
    }

    /**
     * Returns the registered primary {@link Stage}.
     *
     * @return the primary Stage
     */
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
}
