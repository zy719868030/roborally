package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    // 0. Singleton instance
    private static Server instance;

    // 1. Constants / configuration
    private final String protocol = "Version 0.1";
    private final Gson gson = new Gson();

    // 2. Core data / state
    private final AtomicInteger clientIDCounter = new AtomicInteger(1);
    private final ConcurrentBidirectionalMap<ClientHandler, Integer> clients = new ConcurrentBidirectionalMap<>();
    private final ConcurrentMap<ClientHandler, Boolean> isAI = new ConcurrentHashMap<>();

    // 3. Networking / I/O
    private final ServerSocket serverSocket;

    // 4. State flags
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
     * Starts the server and begins listening for client connections.
     * <p>
     * This method performs the following actions:
     * - Prints messages to indicate the server has started and is waiting for clients.
     * - Initializes a heartbeat mechanism to monitor client connectivity by calling {@link #initHeartbeat()}.
     * - Enters a loop where it:
     * - Accepts incoming client socket connections.
     * - Logs a message when a new client connects.
     * - Creates a new {@link ClientHandler} for the client and starts it in its own thread.
     * - Sends a "HelloClient" message to the newly connected client to acknowledge the connection.
     * - Assigns a unique ID to the client using an atomic counter.
     * - Broadcasts a message to all clients announcing the new client connection.
     * - Maps the client handler to its assigned ID for future reference.
     * - Sends a "Welcome" message to the new client, including its unique ID.
     * <p>
     * If an {@link IOException} occurs while accepting client connections, the error message is logged,
     * provided the server is in a running state.
     * <p>
     * This method assumes that the server socket and associated fields
     * have already been properly initialized. It is designed to run until the server
     * is stopped or an error forces termination.
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

                // Send HelloClient
                handler.sendMessage(gson.toJson(new Message<>(new BodyHelloClient(protocol))));

                int newClientID = clientIDCounter.getAndIncrement();
                broadcastMessage(new Message<>(new BodyReceivedChat("New client connected with ID " + newClientID, 0, false)));
                clients.put(handler, newClientID);

                // Send Welcome with an assigned client ID
                handler.sendMessage(gson.toJson(new Message<>(new BodyWelcome(newClientID))));

            }
        } catch (IOException e) {
            if (running)
                System.err.println("Server error: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a JSON message to all connected clients.
     * <p>
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
                handler.sendMessage(gson.toJson(new Message<>(new BodyError("Failed to send message."))));
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
            broadcastMessage(gson.toJson(message));
        } catch (Exception e) {
            System.err.println("Failed to serialize and broadcast message: " + e.getMessage());
        }
    }

    /**
     * Stops the server by closing the server socket and halting the execution loop.
     * <p>
     * This method sets the server's running state to {@code false}, signaling
     * that the server should stop accepting new client connections and perform
     * a clean shutdown. It then attempts to close the {@code serverSocket},
     * releasing the associated network resources. If an {@link IOException}
     * occurs while closing the socket, the error is logged to the standard error stream.
     * <p>
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
     * Removes the specified client handler from the server's client management structures.
     * <p>
     * This method performs the following actions:
     * - Removes the client handler from the client list.
     * - Updates the server's tracking map for AI and non-AI clients by removing the specified client handler.
     * - Broadcasts a message to all connected clients indicating that the client has disconnected.
     * - Logs a message to the console about the disconnection.
     *
     * @param clientHandler the client handler to be removed from the server's list of managed clients
     */
    public void removeClientHandler(ClientHandler clientHandler) {
        clients.removeByKey(clientHandler);
        isAI.remove(clientHandler);
        broadcastMessage(gson.toJson(new Message<>(new BodyReceivedChat("Client disconnected.", 0, false))));
        System.out.println("Client disconnected.");
    }

    /**
     * Initializes and starts the server's heartbeat mechanism to monitor
     * the connectivity of all connected client handlers.
     * <p>
     * This method schedules a recurring task that executes every 5 seconds.
     * On each execution, it iterates through the currently connected clients
     * and performs the following actions:
     * - If a client fails the liveness check (`isAlive` returns false),
     * an error message is logged, and the client is disconnected by
     * invoking `closeAll`.
     * - If a client is alive, its liveness status is updated using
     * the `checkLiveness` method.
     * <p>
     * The heartbeat mechanism ensures that non-responsive clients
     * are detected and removed in a timely manner.
     */
    private void initHeartbeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        scheduler.scheduleAtFixedRate(() -> {
            for (ClientHandler client : clients.keySet()) {
                if (!client.isAlive()) {
                    System.err.println("Client did not respond to Alive. Disconnecting...");
                    client.sendMessage(gson.toJson(new Message<>(new BodyError("Client did not respond to Alive."))));
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
     * Retrieves the map indicating whether each connected client is AI or not.
     *
     * @return a ConcurrentMap where the keys are ClientHandler instances representing connected clients,
     * and the values are Booleans indicating whether the respective client is an AI (true) or not (false).
     */
    public ConcurrentMap<ClientHandler, Boolean> getIsAI() {
        return isAI;
    }

    /**
     * Retrieves the protocol used by the server.
     *
     * @return a string indicating the protocol used by the server
     */
    public String getProtocol() {
        return protocol;
    }


    /**
     * The main entry point for the application. It starts the server and then stops it.
     *
     * @param args command-line arguments passed to the application
     * @throws IOException if an I/O error occurs while starting or stopping the server
     */
    public static void main(String[] args) throws IOException {
        getInstance(12345).start();
    }
}
