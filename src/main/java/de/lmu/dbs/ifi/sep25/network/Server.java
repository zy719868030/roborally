package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("unused")
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
    private final ConcurrentBidirectionalMap<ClientHandler, Integer> figures = new ConcurrentBidirectionalMap<>();
    private final Lobby lobby = new Lobby();
    private final Set<ClientHandler> readyOrder = Collections.synchronizedSet(new LinkedHashSet<>());
    private final List<String> availableMaps = new ArrayList<>(List.of("Dizzy Highway")); //TODO add new maps

    // 3. Networking / I/O
    private final ServerSocket serverSocket;

    // 4. State flags
    private volatile boolean running = true;
    private final int MinPlayer;


    /**
     * Constructs a Server instance with the specified port and minimum number of players.
     * Initializes the server socket and sets up the figures map.
     *
     * @param port      the port number on which the server socket will listen for connections
     * @param MinPlayer the minimum number of players required for the server
     * @throws IOException if an I/O error occurs when opening the server socket
     */
    private Server(int port, int MinPlayer) throws IOException {
        this.MinPlayer = MinPlayer;
        this.serverSocket = new ServerSocket(port);
        for (int i = 0; i < 6; i++)
            figures.put(null, i);
    }

    /**
     * Retrieves the singleton instance of the {@code Server} class.
     * If the instance does not yet exist, it is created with the specified
     * port and minimum number of players. Subsequent calls to this method
     * will return the same instance.
     *
     * @param port      the port number on which the server will listen for incoming connections
     * @param MinPlayer the minimum number of players required for the game
     * @return the singleton {@code Server} instance
     * @throws IOException if an error occurs while initializing the {@code Server} instance
     */
    public static synchronized Server getInstance(int port, int MinPlayer) throws IOException {
        return instance == null ? instance = new Server(port, MinPlayer) : instance;
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
     * Removes a client from the server's internal tracking structures and notifies other clients.
     *
     * <p>This method performs the following operations for the given {@code clientHandler}:
     * <ul>
     *   <li>Removes the client from the main client map.</li>
     *   <li>Removes the client from the AI tracking map.</li>
     *   <li>Releases any figure associated with the client.</li>
     *   <li>Removes the client from the lobby.</li>
     *   <li>Marks the client as not ready.</li>
     *   <li>Broadcasts a disconnection message to all connected clients.</li>
     *   <li>Logs the disconnection event to the server console.</li>
     * </ul>
     *
     * @param clientHandler the client handler to be removed
     */
    public void removeClientHandler(ClientHandler clientHandler) {
        clients.removeByKey(clientHandler);
        isAI.remove(clientHandler);
        releaseFigure(figures.getByKey(clientHandler));
        lobby.remove(clientHandler);
        unmarkReady(clientHandler);
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
     * Assigns a figure to the specified client handler if the figure is not already selected.
     *
     * @param figure  the figure being assigned
     * @param handler the client handler to whom the figure is being assigned
     * @return true if the figure is successfully assigned, false if the figure is already selected by another user
     */
    public boolean assignFigure(Integer figure, ClientHandler handler) {
        if (figures.getByValue(figure) != null) {
            figures.put(handler, figure);
            return true;
        }
        handler.sendMessage(new Message<>(new BodyError("Figure already selected. Please select a other figure.")));
        return false;
    }

    /**
     * Releases a figure by associating it with a null key in the figures map.
     *
     * @param figure the figure to be released and placed in the figures map
     */
    public void releaseFigure(Integer figure) {
        figures.put(null, figure);
    }

    /**
     * Retrieves the appropriate ClientHandler associated with the given figure identifier.
     *
     * @param figure the identifier of the figure for which to get the associated handler
     * @return the ClientHandler associated with the given figure, or null if no handler is found
     */
    public ClientHandler getHandlerForFigure(Integer figure) {
        return figures.getByValueOrDefault(figure, null);
    }

    /**
     * Adds the specified client handler to the server's lobby.
     * The lobby is a collection of connected clients managed by the server.
     *
     * @param handler the {@link ClientHandler} instance representing the client to be added to the lobby
     */
    public void addToLobby(ClientHandler handler) {
        lobby.add(handler);
        //TODO depending on how to handle the map selection, check needed for minplayer exceeded -> ai check -> map selection
        // - otherwise have to implement some sort of live function for the map seleciton, since connection can be broken, and no map could be selected.
        // - probably implement a new message CheckDisconnect accessed via gamelogic
    }

    /**
     * Marks the specified client handler as ready by adding it to the server's ready order.
     * This method maintains the order of readiness and avoids duplicates.
     *
     * @param handler the {@link ClientHandler} instance representing the client to be marked as ready
     */
    public synchronized void markReady(ClientHandler handler) {
        readyOrder.add(handler); // adds in order, ignores duplicates
    }

    /**
     * Removes the specified client handler from the list of ready clients.
     *
     * @param handler the client handler to be removed from the ready order list
     */
    public synchronized void unmarkReady(ClientHandler handler) {
        readyOrder.remove(handler);
    }

    /**
     * Retrieves the first ready client from the list of ready clients.
     * If no clients are ready, an IllegalStateException is thrown.
     *
     * @return the first ready ClientHandler from the readyOrder list
     * @throws IllegalStateException if no clients are in the ready state
     */
    public synchronized ClientHandler getFirstReadyClient() {
        return readyOrder.stream().findFirst().orElseThrow(() -> new IllegalStateException("No players are ready."));
    }

    /**
     * Checks if the readyOrder collection is empty.
     *
     * @return true if the readyOrder collection contains no elements, false otherwise.
     */
    public synchronized boolean readyIsEmpty() {
        return readyOrder.isEmpty();
    }

    /**
     * Retrieves a list of available maps.
     *
     * @return an unmodifiable list of available map names.
     */
    public List<String> getAvailableMaps() {
        return List.copyOf(availableMaps);
    }

    /**
     * The main entry point for the application. It starts the server and then stops it.
     *
     * @param args command-line arguments passed to the application
     * @throws IOException if an I/O error occurs while starting or stopping the server
     */
    public static void main(String[] args) throws IOException {
        getInstance(12345, 2).start();
    }
}
