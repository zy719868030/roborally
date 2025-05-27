package lmu.de;

import com.google.gson.Gson;
import lmu.de.MessageDefinitons.*;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class Server {

    //TODO list for server class
    // -
    //

    private static Server instance;

    private final ServerSocket serverSocket;
    private final Map<ClientHandler, Integer> clients = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final AtomicInteger clientIDCounter = new AtomicInteger(1);
    private volatile boolean running = true;


    /**Private constructor enables Singleton**/
    private Server(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    /**Singleton implementation of Server class**/
    public static synchronized Server getInstance(int port) throws IOException {
        return instance == null ? instance = new Server(port) : instance;
    }

    public static Server getInstance() {
        if (instance == null) {
            throw new IllegalStateException("Server has not been initialized yet.");
        }
        return instance;
    }

    /**Starts the server. Waits for new clients to connect**/
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

    /**Broadcasts message to all currently connected clients**/
    public void broadcastMessage(String message) {
        Iterator<ClientHandler> iterator = clients.keySet().iterator();
        while (iterator.hasNext()) {
            ClientHandler clientHandler = iterator.next();
            try {
                clientHandler.sendMessage(message);
            } catch (IOException e) {
                System.err.println("Failed to send message to client. Removing client.");
                iterator.remove();
            }
        }
    }

    /**Closes the server socket**/
    public void stop() {
        running = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            System.err.println("Error closing server: " + e.getMessage());
        }
    }

    /**Removes client(handler) from clients' list
     * @param clientHandler
     * **/
    public void removeClientHandler(ClientHandler clientHandler) {
        clients.remove(clientHandler);
    }

    /**
     * Returns a snapshot list of all connected client handlers.
     * The list is a copy and is safe to iterate over.
     */
    public List<ClientHandler> getClients() {
        return new ArrayList<>(clients.keySet());
    }

    /**ONLY FOR TESTING**/
    public static void main(String[] args) throws IOException {
        getInstance().start();
        getInstance().stop();
    }
}
