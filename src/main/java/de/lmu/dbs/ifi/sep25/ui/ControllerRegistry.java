package de.lmu.dbs.ifi.sep25.ui;

public class ControllerRegistry {
    private static LobbyController lobbyController;

    public static void setLobbyController(LobbyController controller) {
        lobbyController = controller;
    }

    public static LobbyController getLobbyController() {
        return lobbyController;
    }
}
