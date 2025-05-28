package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitons.*;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    private static Server instance;

    private final ServerSocket serverSocket;
    private final ConcurrentBidirectionalMap<ClientHandler, Integer> clients = new ConcurrentBidirectionalMap<>();
    private final Gson gson = new Gson();
    private final AtomicInteger clientIDCounter = new AtomicInteger(1);
    private volatile boolean running = true;


    /**
     * Constructs a new instance of the {@code Server} with the specified port number.
     * This initializes the server by creating a {@link ServerSocket} bound to the given port.
     *
     * @param port the port number on which the server will listen for incoming connections
     * @throws IOException if an I/O error occurs when opening the socket
     */
    private Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    /**
     * Retrieves the single instance of the {@code Server}, initializing it if necessary.
     * This method ensures that only one instance of {@code Server} is created (Singleton pattern)
     * for the specified port, allowing it to manage client-server communication.
     *
     * @param port the port number on which the server will listen for incoming connections
     * @return the single instance of the {@code Server}
     * @throws IOException if an error occurs initializing the {@code Server} or opening the socket
     */
    public static synchronized Server getInstance(int port) throws IOException {
        return instance == null ? instance = new Server(port) : instance;
    }

    /**
     * Retrieves the single instance of the {@code Server}.
     * This method enforces the Singleton design pattern, ensuring that only
     * one instance of the {@code Server} is created and accessible throughout
     * the application's lifecycle. If the instance has not been initialized,
     * an {@link IllegalStateException} will be thrown.
     *
     * @return the current {@code Server} instance
     * @throws IllegalStateException if the server instance has not been initialized
     */
    public static Server getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Server has not been initialized yet.");
        }
        return instance;
    }

    /**
     * Starts the server and initializes the process for handling client connections.
     *
     * This method performs the following actions:
     * - Prints status messages to indicate the server is starting and waiting for clients.
     * - Calls the `initHeartbeat` method to start monitoring client connectivity.
     * - Enters into a loop to accept and handle incoming client connections as long
     *   as the server is running.
     * - For each new connection:
     *   - Accepts the client's socket connection.
     *   - Creates a new `ClientHandler` instance for the connection.
     *   - Starts a new thread to manage the interaction with the client.
     *   - Assigns a unique client ID using a shared counter and updates the client map.
     *   - Sends a "HelloClient" message to the client with version information.
     *   - Sends a "Welcome" message to the client with the assigned ID.
     *
     * If an `IOException` occurs while the server is running, an error message is logged.
     * If the server is not running, no further connections are accepted.
     *
     * Note: The connection protocol and additional client handling logic must be implemented where indicated.
     */
    public void start() {
        System.out.println("Server started!");
        System.out.println("Waiting for clients...");
        initHeartbeat();

        try {
            while (running) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected.");

                ClientHandler handler = new ClientHandler(clientSocket);
                new Thread(handler).start();

                //TODO implement connection protocol

                int newClientID = clientIDCounter.getAndIncrement();
                clients.put(handler, newClientID);

                // Send HelloClient
                handler.sendMessage(gson.toJson(new Message<BodyHelloClient>(new BodyHelloClient("Version 0.1"))));

                // Send Welcome with assigned client ID
                handler.sendMessage(gson.toJson(new Message<>(new BodyWelcome(newClientID))));

            }
        } catch (IOException e) {
            if (running)
                System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a JSON message to all connected clients.
     *
     * This method iterates over the set of client handlers and sends the provided JSON message
     * to each client. If an exception occurs during the sending of the message to a particular
     * client, the client handler is removed from the collection of active clients, and any
     * associated resources are closed.
     *
     * @param json the JSON-formatted message to be broadcast to all connected clients
     */
    public void broadcastMessage(String json) {
        clients.keySet().removeIf(handler -> {
            try {
                handler.sendMessage(json);
                return false;
            } catch (Exception e) {
                System.err.println("Removing client due to send failure: " + e.getMessage());
                handler.closeAll();
                return true;
            }
        });
    }

    /**
     * Converts the provided {@link Message} object into a JSON string and broadcasts
     * it to all currently connected clients.
     * If serialization fails, an error message will be printed to the error stream,
     * and the message will not be broadcasted.
     *
     * @param message the message object to be serialized and broadcasted
     */
    public void broadcastMessage(Message<?> message) {
        try {
            String json = gson.toJson(message);
            broadcastMessage(json);
        } catch (Exception e) {
            System.err.println("Failed to serialize and broadcast message: " + e.getMessage());
        }
    }

    /**
     * Stops the server by closing the server socket and halting the execution loop.
     *
     * This method sets the server's running state to {@code false}, signaling
     * that the server should stop accepting new client connections and perform
     * a clean shutdown. It then attempts to close the {@code serverSocket},
     * releasing the associated network resources. If an {@link IOException}
     * occurs while closing the socket, the error is logged to the standard error stream.
     *
     * Any ongoing client communication or background tasks associated with the server
     * may still need to be handled separately to ensure a graceful shutdown.
     */
    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    /**
     * Removes a client handler from the server's client collection.
     * This method is used to disconnect and manage clients actively connected
     * to the server.
     *
     * @param clientHandler the client handler to be removed from the server
     */
    public void removeClientHandler(ClientHandler clientHandler) {
        clients.removeByKey(clientHandler);
    }

    /**
     * Initializes and starts the server's heartbeat mechanism to monitor
     * the connectivity of all connected client handlers.
     *
     * This method schedules a recurring task that executes every 5 seconds.
     * On each execution, it iterates through the currently connected clients
     * and performs the following actions:
     * - If a client fails the liveness check (`isAlive` returns false),
     *   an error message is logged, and the client is disconnected by
     *   invoking `closeAll`.
     * - If a client is alive, its liveness status is updated using
     *   the `checkLiveness` method.
     *
     * The heartbeat mechanism ensures that non-responsive clients
     * are detected and removed in a timely manner.
     */
    private void initHeartbeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            for (ClientHandler client : clients.keySet()) {
                if (!client.isAlive()) {
                    System.err.println("Client did not respond to Alive. Disconnecting...");
                    client.closeAll();
                } else {
                    client.checkLiveness();
                }
            }
        }, 0, 5, TimeUnit.SECONDS);
    }

    /**
     * Retrieves the map of all connected clients and their associated unique IDs.
     *
     * @return a ConcurrentBidirectionalMap where the keys are ClientHandler instances
     * representing connected clients and the values are their unique integer identifiers.
     */
    public ConcurrentBidirectionalMap<ClientHandler, Integer> getClients() {
        return clients;
    }

    /**
     * The main entry point for the application. It starts the server and then stops it.
     *
     * @param args command-line arguments passed to the application
     * @throws IOException if an I/O error occurs while starting or stopping the server
     */
    public static void main(String[] args) throws IOException {
        getInstance().start();
        getInstance().stop();
    }
}
