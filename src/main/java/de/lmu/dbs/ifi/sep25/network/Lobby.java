package de.lmu.dbs.ifi.sep25.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
/**
 * Represents a game lobby that manages connected clients.
 * It supports adding and removing clients, checking readiness,
 * and determining AI-only status.
 */
public class Lobby {

    /**
     * Thread-safe list of clients currently in the lobby.
     */
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    /**
     * Attempts to add a new client to the lobby.
     * If the lobby has reached its capacity (6 clients), the client is rejected with an error message.
     *
     * @param handler The client handler to add.
     * @return {@code true} if the client was added; {@code false} if the lobby is full.
     */
    public boolean add(ClientHandler handler) {
        if (clients.size() >= 6){
            handler.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Lobby is full")));
            return false;
        }
        clients.add(handler);
        return true;
    }

    /**
     * Removes a client from the lobby.
     *
     * @param handler The client handler to remove.
     */
    public void remove(ClientHandler handler) {
        clients.remove(handler);
    }

    /**
     * Returns the number of clients currently in the lobby.
     *
     * @return The number of connected clients.
     */
    public int size() {
        return clients.size();
    }

    /**
     * Checks whether all clients are ready.
     * A client is considered ready if their associated player exists and is marked as ready.
     *
     * @return {@code true} if all clients are ready; {@code false} otherwise.
     */
    public boolean allReady() {
        return !clients.isEmpty() &&
                clients.stream().allMatch(h -> h.getPlayer() != null && h.getPlayer().isReady());
    }

    /**
     * Determines whether all connected clients are AI-controlled.
     *
     * @param isAI A concurrent map associating clients with their AI status.
     * @return {@code true} if all clients are AI; {@code false} otherwise.
     */
    public boolean allAreAi(ConcurrentMap<ClientHandler, Boolean> isAI) {
        for (ClientHandler client : clients)
            if (!Boolean.TRUE.equals(isAI.get(client)))
                return false;
        return true;
    }

    /**
     * Returns a copy of the current client list.
     *
     * @return A new list containing all clients in the lobby.
     */
    public List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }
}
