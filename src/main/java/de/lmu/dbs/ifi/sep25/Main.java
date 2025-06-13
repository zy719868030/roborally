package de.lmu.dbs.ifi.sep25;

import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.Server;
import de.lmu.dbs.ifi.sep25.ui.HelloApplication;
import javafx.application.Application;

public class Main {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--server")) {
            try {
                System.out.println("[SYSTEM] Launching server...");
                Server.getInstance(12345, 2).start();
            } catch (Exception e) {
                System.err.println("[ERROR] Server failed to start: " + e.getMessage());
            }
        } else {
            System.out.println("[SYSTEM] Launching client...");
            startClientInBackground();
            Application.launch(HelloApplication.class, args);
        }
    }

    private static void startClientInBackground() {
        Thread clientThread = new Thread(() -> {
            Client client = new Client();
            ClientSingleton.set(client);
            client.start("localhost", 12345);
        });
        clientThread.setDaemon(true);
        clientThread.start();
    }
}