package de.lmu.dbs.ifi.sep25.ui;


public class ControllerRegistry {
    private static LobbyController lobbyController;
    private static LoginController loginController;

    public static void setLobbyController(LobbyController controller) {
        lobbyController = controller;
    }

    public static LobbyController getLobbyController() {
        return lobbyController;
    }

    public static void setLoginController(LoginController controller) {
        loginController = controller;
    }

    public static LoginController getLoginController() {
        return loginController;

    }
}
