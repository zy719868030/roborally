package de.lmu.dbs.ifi.sep25.network;

import de.lmu.dbs.ifi.sep25.game.Game;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Represents the game server that manages client connections, lobby management,
 * game sessions, and communication with clients.
 * <p>
 * This class is implemented as a singleton.
 */
public class Server {

    // 0. Singleton instance and Loggers
    private static Server instance;
    private static final Logger heartbeatLogger = LogManager.getLogger("heartbeatLogger");
    private static final Logger appLogger = org.apache.logging.log4j.LogManager.getLogger(Server.class);

    // 1. Constants / configuration
    private final String protocol = "Version 1.0";

    // 2. Core data / state
    private final AtomicInteger clientIDCounter = new AtomicInteger(1);
    private final ConcurrentBidirectionalMap<ClientHandler, Integer> clients = new ConcurrentBidirectionalMap<>();
    private final ConcurrentMap<ClientHandler, Boolean> isAI = new ConcurrentHashMap<>();
    private final ConcurrentBidirectionalMap<ClientHandler, Integer> figures = new ConcurrentBidirectionalMap<>();
    private final List<Integer> availableFigures = Collections.synchronizedList(new ArrayList<>());
    private final Lobby lobby = new Lobby();
    //    private final List<Message<BodyPlayerAdded>> connectedPlayerHistory = new CopyOnWriteArrayList<>(); TODO @sebas bitte integrieren/nutzen
    private final Set<ClientHandler> readyOrder = Collections.synchronizedSet(new LinkedHashSet<>());
    private final List<ClientHandler> snapshotReadyOrder = Collections.synchronizedList(new ArrayList<>());
    private final List<String> availableMaps = List.of(
            "Dizzy Highway",
            "Extra Crispy",
            "Lost Bearings",
            "Death Trap"
    );
    private final Map<ClientHandler, String> names = new ConcurrentHashMap<>();

    // 3. Networking / I/O
    private final ServerSocket serverSocket;

    // 4. State flags
    private volatile boolean running = true;
    private final int minPlayer;
    private volatile boolean mapSelectionOngoing = false;
    private final AtomicBoolean timerStarted = new AtomicBoolean(false);

    // 5. Game logic
    private Game game;
    private final List<Integer> readyRegister = new ArrayList<>();


    /**
     * Constructs a new {@code Server} instance that listens on the specified port and requires
     * a minimum number of players before starting the game.
     *
     * @param port      the TCP port number on which the server will listen for incoming connections
     * @param minPlayer the minimum number of players required to start the game
     * @throws IOException if the server socket cannot be opened on the specified port
     */
    private Server(int port, int minPlayer) throws IOException {
        this.minPlayer = minPlayer;
        this.serverSocket = new ServerSocket(port);
        for (int i = 0; i < 6; i++) {
            availableFigures.add(i);
        }
    }

    /**
     * Returns the singleton {@code Server} instance, creating it if necessary.
     *
     * @param port      the port number for the server socket to listen on
     * @param minPlayer the minimum number of players required for the game
     * @return the singleton instance of the {@code Server}
     * @throws IOException if an I/O error occurs during server initialization
     */
    public static synchronized Server getInstance(int port, int minPlayer) throws IOException {
        if (instance == null) {
            instance = new Server(port, minPlayer);
        }
        return instance;
    }

    /**
     * Returns the existing singleton {@code Server} instance.
     *
     * @return the current server instance
     * @throws IllegalStateException if the server has not been initialized yet
     */
    public static Server getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Server has not been initialized yet.");
        }
        return instance;
    }

    /**
     * Starts the server, listening for client connections and handling them in separate threads.
     * Also initializes the heartbeat mechanism to monitor client connectivity.
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

                handler.sendMessage(new Message<>(new BodyHelloClient(protocol)));

                int newClientID = clientIDCounter.getAndIncrement();
                // broadcastMessage(new Message<>(new BodyReceivedChat("New client connected with ID " + newClientID, 0, false)));
                clients.put(handler, newClientID);
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Server error: " + e.getMessage());
            }
        }
    }

    /**
     * Broadcasts a message to all connected clients. If a client cannot receive the message,
     * it will be removed from the clients list, and an error response will be sent to that client
     * before closing its connection.
     *
     * @param message the message to be broadcasted to all connected clients
     */
    public void broadcastMessage(Message<?> message) {
        try {
            clients.keySet().removeIf(handler -> {
                try {
                    handler.sendMessage(message);
                    return false;
                } catch (Exception e) {
                    System.err.println("Removing client due to send failure: " + e.getMessage());
                    handler.sendMessage(new Message<>(new BodyError("Failed to send message.")));
                    handler.closeAll();
                    return true;
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to serialize and broadcast message: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all connected clients. If a client cannot receive the message,
     * it will be removed from the clients list, and an error response will be sent to that client
     * before closing its connection.
     *
     * @param message the message to be broadcasted to all connected clients
     */
    public void broadcastMessage(String message) {
        try {
            clients.keySet().removeIf(handler -> {
                try {
                    handler.sendMessage(message);
                    return false;
                } catch (Exception e) {
                    System.err.println("Removing client due to send failure: " + e.getMessage());
                    handler.sendMessage(new Message<>(new BodyError("Failed to send message.")));
                    handler.closeAll();
                    return true;
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to serialize and broadcast message: " + e.getMessage());
        }
    }

    /**
     * Broadcasts a message to all connected clients except the specified client handler.
     * If a client cannot receive the message, it will be removed from the clients list, and an
     * error response will be sent to that client before closing its connection.
     *
     * @param message the message to be broadcasted to the clients
     * @param exclude the client handler to exclude from receiving the message
     */
    public void broadcastMessage(Message<?> message, ClientHandler exclude) {
        try {
            clients.keySet().stream()
                    .filter(handler -> handler != exclude)
                    .forEach(handler -> {
                        try {
                            handler.sendMessage(message);
                        } catch (Exception e) {
                            System.err.println("Failed to send message to client: " + e.getMessage());
                            handler.sendMessage(new Message<>(new BodyError("Failed to send message.")));
                            handler.closeAll();
                            clients.removeByKey(handler);
                        }
                    });
        } catch (Exception e) {
            System.err.println("Failed to serialize and broadcast message: " + e.getMessage());
        }
    }


    /**
     * Stops the server by closing the server socket and terminating the accept loop.
     */
    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server socket: " + e.getMessage());
        }
    }

    /**
     * Removes a client handler from the server's management structures,
     * releases associated resources, and notifies other clients.
     *
     * @param clientHandler the client handler to remove
     */
    public void removeClientHandler(ClientHandler clientHandler) {
        clients.removeByKey(clientHandler);
        isAI.remove(clientHandler);
        releaseFigure(figures.getByKey(clientHandler));
        lobby.remove(clientHandler);
        unmarkReady(clientHandler);
        if (clientHandler.isMapSelecting()) {
            setMapSelectionOngoing(false);
            getFirstReadyClient().sendMessage(new Message<>(new BodySelectMap(availableMaps)));
        }
        broadcastMessage(new Message<>(new BodyReceivedChat("Client disconnected.", 0, false)));
        System.out.println("Client disconnected.");
    }

    /**
     * Initializes a periodic heartbeat task that checks the connectivity of all clients every 5 seconds.
     * Disconnects clients that fail to respond to the heartbeat.
     */
    private void initHeartbeat() {
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        heartbeatLogger.info("\n========== HEARTBEAT CYCLE ==========");

        scheduler.scheduleAtFixedRate(() -> {
            // Reset alive flag in checkLiveness and alive check client
            for (ClientHandler client : clients.keySet()) {
                heartbeatLogger.info("\n--- Checking client: " + clients.getByKey(client) + " ---");

                client.checkLiveness(); // sets alive = false and sends BodyAlive

                heartbeatLogger.info("Alive set to false.");
                heartbeatLogger.info("Sent Alive message to client " + clients.getByKey(client));
            }

            // After a short delay, check if anyone failed to respond
            scheduler.schedule(() -> {
                for (ClientHandler client : clients.keySet()) {
                    heartbeatLogger.info("\n>>> Checking response from client: {}", clients.getByKey(client));
                    heartbeatLogger.info("Alive status: {}", client.isAlive());

                    if (!client.isAlive()) {
                        System.err.println("Client did not respond to Alive. Disconnecting...");
                        heartbeatLogger.info("Client with ID {} did not respond to Alive. Disconnecting.", clients.getByKey(client));
                        client.sendMessage(new Message<>(new BodyError("Client did not respond to Alive.")));
                        client.closeAll();
                    }
                }

            }, 4, TimeUnit.SECONDS); // ← Delay before checking response

        }, 0, 5, TimeUnit.SECONDS); // Repeat full cycle every 5 seconds
    }

    /**
     * Returns the bidirectional map of clients and their assigned unique IDs.
     *
     * @return a map linking {@link ClientHandler} instances to their client IDs
     */
    public ConcurrentBidirectionalMap<ClientHandler, Integer> getClients() {
        return clients;
    }

    /**
     * Returns the map indicating which clients are AI-controlled.
     *
     * @return a map linking {@link ClientHandler} instances to a Boolean indicating AI status
     */
    public ConcurrentMap<ClientHandler, Boolean> getIsAI() {
        return isAI;
    }

    /**
     * Returns the protocol version string used by the server.
     *
     * @return the protocol version string
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * Attempts to assign a figure to a client if it is available.
     *
     * @param figure  the figure number to assign
     * @param handler the client handler to assign the figure to
     * @return {@code true} if the figure was assigned successfully; {@code false} if the figure is already taken
     */
    public boolean assignFigure(Integer figure, ClientHandler handler) {
        synchronized (availableFigures) {
            if (!availableFigures.contains(figure)) {
                handler.sendMessage(new Message<>(new BodyError("Figure already selected. Please select another figure.")));
                return false;
            }
            availableFigures.remove(figure);
            figures.put(handler, figure);
            return true;
        }
    }

    /**
     * Releases a figure, making it available again, and removes it from any client assignment.
     *
     * @param figure the figure number to release
     */
    public void releaseFigure(Integer figure) {
        synchronized (availableFigures) {
            if (!availableFigures.contains(figure)) {
                availableFigures.add(figure);
            }

            try {
                figures.removeByValue(figure);
            } catch (Exception e) {
                System.err.println("Failed to release figure: " + e.getMessage());
            }
        }
    }

    /**
     * Generates a unique name by appending a numeric suffix to the given base name.
     * Ensures the generated name does not conflict with existing names managed by the server.
     * The method is thread-safe to handle concurrent generation requests.
     *
     * @param baseName the base string to use for generating the unique name
     * @return a unique name based on the provided base name
     */
    public synchronized String generateUniqueName(String baseName) {
        List<String> currentNames = new ArrayList<>(names.values());

        int maxSuffix = 0;
        for (String name : currentNames) {
            if (name.equals(baseName)) {
                for (Map.Entry<ClientHandler, String> entry : names.entrySet()) {
                    if (entry.getValue().equals(baseName)) {
                        String newName = baseName + "#1";
                        entry.setValue(newName);
                        broadcastMessage(new Message<>(new BodyPlayerRenamed(
                                clients.getByKey(entry.getKey()), newName
                        )));
                        maxSuffix = Math.max(maxSuffix, 1);
                        break;
                    }
                }
            } else if (name.startsWith(baseName + "#")) {
                try {
                    int suffix = Integer.parseInt(name.substring(baseName.length() + 1));
                    maxSuffix = Math.max(maxSuffix, suffix);
                } catch (NumberFormatException ignored) {
                }
            }
        }

        if (maxSuffix == 0) {
            return baseName;
        }

        return baseName + "#" + (maxSuffix + 1);
    }


    /**
     * Returns the client handler assigned to the given figure number.
     *
     * @param figure the figure number
     * @return the associated client handler, or {@code null} if none is assigned
     */
    public ClientHandler getHandlerForFigure(Integer figure) {
        return figures.getByValueOrDefault(figure, null);
    }

    /**
     * Adds a client handler to the lobby.
     * If the lobby meets the minimum player count and all players are AI,
     * a random map is selected and the game is started.
     *
     * @param handler the client handler to add
     */
    public void addToLobby(ClientHandler handler) {
        lobby.add(handler);
        if (lobby.size() >= minPlayer && lobby.allAreAi(isAI)) {
            Random random = new Random();
            String map = availableMaps.get(random.nextInt(availableMaps.size()));
            newGame(map);
            broadcastMessage(new Message<>(new BodyMapSelected(map)));
        }
    }


    /**
     * Marks the specified client as ready.
     * If all clients in the lobby are ready and the game is initialized, the game is started.
     *
     * @param handler the client handler to mark as ready
     */
    public synchronized void markReady(ClientHandler handler) {
        readyOrder.add(handler);
        appLogger.info("Added client {} to ready order. ({} clients in queue now.) {}", handler.getMyID(), readyOrder.size(), readyOrder.stream().map(ClientHandler::getMyID).toList());

//        appLogger.info("Snapshot of ready order: {}", readyOrder);
        snapshotReadyOrder.clear();
        snapshotReadyOrder.addAll(readyOrder);

        if (lobby.size() >= minPlayer && lobby.allReady() && game != null) {
            startGame();
        }
    }

    /**
     * Removes the specified client handler from the ready list.
     *
     * @param handler the client handler to unmark as ready
     */
    public synchronized void unmarkReady(ClientHandler handler) {
        if (readyOrder.contains(handler)) {
            readyOrder.remove(handler);
            appLogger.info("Removed client {} from ready order. ({} clients in queue now.)", handler.getMyID(), readyOrder.size());
        } else {
            appLogger.info("Client {} was not in ready order.", handler.getMyID());
        }

//        appLogger.info("Snapshot of ready order: {}", readyOrder);
        snapshotReadyOrder.clear();
        snapshotReadyOrder.addAll(readyOrder);
    }
    /**
     * Retrieves the first client that is marked as ready from the ready order
     * set and removes it from the set.
     *
     * @return the first {@link ClientHandler} in the ready order set
     * @throws IllegalStateException if no clients are marked as ready
     */
    public synchronized ClientHandler getFirstReadyClient() {
            Iterator<ClientHandler> iterator = readyOrder.iterator();
            if (!iterator.hasNext()) {
                appLogger.error("No clients marked as ready. Cannot get first ready client. Returned null.");
        }
        // update snapshot
//        appLogger.info("Snapshot of ready order: {}", readyOrder);
        snapshotReadyOrder.clear();
        snapshotReadyOrder.addAll(readyOrder);

        ClientHandler first = iterator.next();
        iterator.remove();
        return first;
    }

    /**
     * Checks whether there are no clients marked as ready.
     *
     * @return {@code true} if no clients are ready; {@code false} otherwise
     */
    public synchronized boolean readyOrderIsEmpty() {
        return readyOrder.isEmpty();
    }

    /**
     * Returns an immutable snapshot of the current ready order of clients.
     * <p>
     * This snapshot reflects the state of the ready order at the moment before the last
     * {@code getFirstReadyClient()} call removed the first client from the queue.
     * It preserves the insertion order and does not reflect later changes to the actual queue.
     *
     * @return an unmodifiable list representing the ready order of clients at snapshot time
     */
    public synchronized List<ClientHandler> getSnapshotReadyOrder() {
        appLogger.info("Snapshot of ready order: {}", snapshotReadyOrder.stream().map(ClientHandler::getMyID).toList());
        return List.copyOf(snapshotReadyOrder);
    }

    /**
     * Returns a list of available map names.
     *
     * @return an unmodifiable list of map names available for selection
     */
    public List<String> getAvailableMaps() {
        return List.copyOf(availableMaps);
    }

    /**
     * Retrieves the positions of all robots associated with the connected clients.
     *
     * @return a list of {@link Position} objects representing the positions of all robots
     */
    public List<Position> getRobotPositions() {
        return clients.keySet().stream().map(handler -> handler.getPlayer().getRobot().getPosition()).toList();
    }

    /**
     * Creates a new game session with the specified map and adds all connected players to it.
     * Starts the game if all players in the lobby are ready.
     *
     * @param mapName the name of the map to use for the new game
     */
    public void newGame(String mapName) {
        System.out.println("Creating new game with map " + mapName + "...");
        this.game = Game.getInstance(mapName);
        for (ClientHandler client : clients.keySet()) {
            client.setGame(this.game); // Set the game instance for each client handler to avoid crashes
            game.addPlayer(client.getPlayer());
        }

        if (lobby.allReady()) {
            startGame();
        }
    }

    /**
     * Starts the game and broadcasts a game start message to all players.
     */
    public void startGame() {
        System.out.println("Starting game...");
        resetReadyRegister();
        broadcastMessage(game.getBoard().getSerializedBoardAsMessage());

        game.startGameLoop();
    }

    /**
     * Resets the ready register by clearing its current contents and repopulating it with updated client states.
     * The method retrieves all clients from the lobby, maps them to their respective identifiers,
     * and adds these identifiers to the ready register.
     */
    private void resetReadyRegister() {
        readyRegister.clear();
        readyRegister.addAll(getLobby().getClients().stream().map(clients::getByKey).toList());
    }

    /**
     * Removes the specified client ID from the ready register.
     * This method is used to update the list of clients marked as ready
     * by removing a specific client's identifier.
     *
     * @param clientID the unique identifier of the client to be removed from the ready register
     */
    public void markReadyRegister(Integer clientID) {
        readyRegister.remove(clientID);
        game.checkAndAdvanceFromProgrammingPhase();
    }

    /**
     * Retrieves a copy of the list of ready client IDs and resets the ready register for the next round.
     *
     * @return a copy of the current ready register containing client IDs marked as ready
     */
    public List<Integer> getReadyRegister() {
        List<Integer> copy = new ArrayList<>(readyRegister);
        resetReadyRegister();
        return copy;
    }

    /**
     * Returns the current game instance.
     *
     * @return the active {@code Game} instance managed by the server
     */
    public Game getGame() {
        return game;
    }

    /**
     * Retrieves the current lobby instance managed by the server.
     *
     * @return the {@code Lobby} instance that manages client handlers and their states
     */
    public Lobby getLobby() {
        return lobby;

    }

    /**
     * Returns the mapping of client handlers to their corresponding names.
     *
     * @return a map where keys are {@link ClientHandler} instances and values are the associated names
     */
    public Map<ClientHandler, String> getNames() {
        return names;
    }

    /**
     * Retrieves the mapping of client handlers to their assigned unique figure numbers.
     *
     * @return a bidirectional map linking ClientHandler instances to their respective figure numbers
     */
    public ConcurrentBidirectionalMap<ClientHandler, Integer> getFigures() {
        return figures;
    }

    public synchronized boolean isMapSelectionOngoing() {
        return mapSelectionOngoing;
    }

    public synchronized void setMapSelectionOngoing(boolean mapSelectionOngoing) {
        this.mapSelectionOngoing = mapSelectionOngoing;
    }

    public boolean getTimerStarted() {
        return timerStarted.get();
    }

    public void setTimerStarted(boolean timerStarted) {
        this.timerStarted.set(timerStarted);
    }
}
