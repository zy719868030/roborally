package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitons.*;
import de.lmu.dbs.ifi.sep25.utils.ConcurrentBidirectionalMap;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
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
     * Starts the server and begins accepting client connections.
     *
     * The method initializes the server's listening loop, continuously accepting
     * incoming client connections as long as the server is running. Each accepted
     * connection is handled by creating a new {@code ClientHandler} instance,
     * which runs on a separate thread. The method performs the following steps:
     *
     * - Accepts new client connections using the {@link ServerSocket#accept()} method.
     * - Assigns each client a unique identifier and stores the client handler in the
     *   server's internal client management structure.
     * - Sends an initial greeting message ("HelloClient") to the connected client.
     * - Sends a welcome message ("Welcome") to the client, including the assigned
     *   unique identifier.
     *
     * If an {@code IOException} is encountered during the process, the server
     * logs the error message to the standard error stream, provided the server
     * is still in the running state. The method will terminate if the server's
     * running status is set to {@code false}.
     *
     * Note: This method is designed to run in a continuous loop and should
     * be called in a condition where the server is intended to remain operational.
     */
    public void start() {
        System.out.println("Server started!");
        System.out.println("Waiting for clients...");
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
