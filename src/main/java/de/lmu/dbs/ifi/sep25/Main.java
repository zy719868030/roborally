package de.lmu.dbs.ifi.sep25;

import de.lmu.dbs.ifi.sep25.network.Server;
import de.lmu.dbs.ifi.sep25.ui.HelloApplication;
import javafx.application.Application;

/**
 * Entry point for the RoboRally application.
 * <p>
 * <b>Usage:</b><br>
 * <code>java -jar EEJar.jar --server [-port &lt;port&gt;] [-minplayer &lt;players&gt;]</code><br>
 * <code>java -jar EEJar.jar --client</code>
 * </p>
 * <ul>
 *   <li><b>--server [-port &lt;port&gt;] [-minplayer &lt;players&gt;]</b>:
 *     Starts the game server. <br>
 *     <ul>
 *         <li><b>-port &lt;port&gt;</b>: (Optional) TCP port to bind the server to (default: 12345).</li>
 *         <li><b>-minplayer &lt;players&gt;</b>: (Optional) Minimum number of players required to start (1–6, default: 2).</li>
 *         <li>Arguments can be provided in any order and are optional.</li>
 *     </ul>
 *   </li>
 *   <li><b>--client</b>:
 *     Starts the JavaFX client application.
 *   </li>
 * </ul>
 * <p>
 * Example usages:<br>
 * <code>java -jar EEJar.jar --server</code><br>
 * <code>java -jar EEJar.jar --server -minplayer 4 -port 24680</code><br>
 * <code>java -jar EEJar.jar --server -port 24680 -minplayer 3</code><br>
 * <code>java -jar EEJar.jar --client</code>
 * </p>
 * <p>
 * Invalid arguments will cause the application to print an error and exit.
 * </p>
 */
public class Main {
    /**
     * Starts the server or client depending on the command line arguments.
     *
     * @param args Command line arguments.
     *             <ul>
     *                 <li><code>--server</code> to launch the server (optional: <code>-port &lt;port&gt;</code> and/or <code>-minplayer &lt;players&gt;</code>).</li>
     *                 <li><code>--client</code> to launch the client UI.</li>
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
        } else if (args.length > 0 && args[0].equalsIgnoreCase("--client")) {
            System.out.println("[SYSTEM] Launching client...");
            Application.launch(HelloApplication.class, args);
        } else {
            System.err.println("[ERROR] Invalid arguments. Please use --server or --client.");
            System.exit(1);
        }
    }
}
