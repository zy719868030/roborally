package de.lmu.dbs.ifi.sep25;

import de.lmu.dbs.ifi.sep25.network.Server;
import de.lmu.dbs.ifi.sep25.ui.HelloApplication;
import de.lmu.dbs.ifi.sep25.ui.bot.BotClient;
import de.lmu.dbs.ifi.sep25.ui.bot.BotStrategy;
import de.lmu.dbs.ifi.sep25.ui.bot.PathfindingBotStrategy;
import de.lmu.dbs.ifi.sep25.ui.bot.RandomBotStrategy;
import javafx.application.Application;

import java.io.IOException;

/**
 * Entry point for the RoboRally application.
 * <p>
 * <b>Usage:</b>
 * <pre>
 * java -jar YourGame.jar --server [-port &lt;port&gt;] [-minplayer &lt;players&gt;]
 * java -jar YourGame.jar --client
 * java -jar YourGame.jar --bot -random [host] [port]
 * java -jar YourGame.jar --bot -smart  [host] [port]
 * </pre>
 * <ul>
 *   <li><b>--server [-port &lt;port&gt;] [-minplayer &lt;players&gt;]</b>:
 *     Starts the game server.
 *     <ul>
 *       <li><b>-port &lt;port&gt;</b>: (Optional) TCP port to use (default: 12345).</li>
 *       <li><b>-minplayer &lt;players&gt;</b>: (Optional) Minimum players to start (1–6, default: 2).</li>
 *     </ul>
 *   </li>
 *   <li><b>--client</b>: Starts the JavaFX client application.</li>
 *   <li><b>--bot -random [host] [port]</b>: Starts a headless bot that plays randomly.</li>
 *   <li><b>--bot -smart [host] [port]</b>: Starts a headless bot that uses pathfinding logic.</li>
 * </ul>
 * <p>
 * Example usages:
 * <pre>
 * java -jar YourGame.jar --server
 * java -jar YourGame.jar --server -minplayer 4 -port 24680
 * java -jar YourGame.jar --bot -random
 * java -jar YourGame.jar --bot -smart my.server.com 23456
 * java -jar YourGame.jar --client
 * </pre>
 * <p>
 * Invalid arguments print usage and exit.
 * </p>
 */
public class Main {
    /**
     * Launches the RoboRally server, client, or bot, depending on command-line arguments.
     *
     * @param args Command-line arguments:
     *             <ul>
     *                 <li><b>--server</b> to launch the server (optional: <b>-port &lt;port&gt;</b> and/or <b>-minplayer &lt;players&gt;</b>).</li>
     *                 <li><b>--client</b> to launch the GUI client.</li>
     *                 <li><b>--bot -random [host] [port]</b> to launch a random-move bot.</li>
     *                 <li><b>--bot -smart [host] [port]</b> to launch a pathfinding bot.</li>
     *             </ul>
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("--server")) {
            try {
                if (Server.getInstance() != null) {
                    System.err.println("[ERROR] Server is already running.");
                    System.exit(1);
                }

                // Default values
                int port = 12345;
                int minPlayer = 2;

                // Parse args (order-independent)
                for (int i = 1; i < args.length; i++) {
                    switch (args[i].toLowerCase()) {
                        case "-port":
                            if (i + 1 < args.length) {
                                port = Integer.parseInt(args[++i]);
                            } else {
                                System.err.println("[ERROR] -port requires a value.");
                                System.exit(1);
                            }
                            break;
                        case "-minplayer":
                            if (i + 1 < args.length) {
                                minPlayer = Integer.parseInt(args[++i]);
                            } else {
                                System.err.println("[ERROR] -minplayer requires a value.");
                                System.exit(1);
                            }
                            break;
                    }
                }

                // Validity check
                if (minPlayer < 1 || minPlayer > 6) {
                    System.err.println("[ERROR] Invalid minimum player count: " + minPlayer);
                    System.exit(1);
                }

                System.out.println("[SYSTEM] Launching server with port " + port + " and minimum player count " + minPlayer + " ...");
                Server.getInstance(port, minPlayer).start();

            } catch (Exception e) {
                System.err.println("[ERROR] Server failed to start: " + e.getMessage());
                System.exit(1);
            }
        } else if (args.length > 1 && args[0].equalsIgnoreCase("--bot")) {
            String botType = args[1].toLowerCase();
            BotStrategy strategy;
            switch (botType) {
                case "-random" -> strategy = new RandomBotStrategy();
                case "-smart" -> strategy = new PathfindingBotStrategy();
                default -> {
                    System.err.println("Unknown bot type: " + botType);
                    return;
                }
            }
            BotClient bot = new BotClient(strategy);
            // Optionally allow host/port as further args
            String host = (args.length > 2) ? args[2] : "localhost";
            int port = (args.length > 3) ? Integer.parseInt(args[3]) : 12345;
            try {
                bot.start(host, port);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (args.length > 0 && args[0].equalsIgnoreCase("--client")) {
            System.out.println("[SYSTEM] Launching client...");
            Application.launch(HelloApplication.class, args);
        } else {
            System.out.println("Usage:");
            System.out.println("  java -jar YourGame.jar --bot -random [host] [port]");
            System.out.println("  java -jar YourGame.jar --bot -smart  [host] [port]");
            System.out.println("  java -jar YourGame.jar --client");
        }
    }
}
