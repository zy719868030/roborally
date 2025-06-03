package de.lmu.dbs.ifi.sep25.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@SuppressWarnings("unused")
public class Lobby {
    private final List<ClientHandler> clients = new CopyOnWriteArrayList<>();

    public boolean add(ClientHandler handler) {
        if (clients.size() >= 6){
            handler.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Lobby is full")));
            return false;
        }
        clients.add(handler);
        return true;
    }

    public void remove(ClientHandler handler) {
        clients.remove(handler);
    }

    public boolean allReady() {
        return !clients.isEmpty() &&
                clients.stream().allMatch(h -> h.getPlayer() != null && h.getPlayer().isReady());
    }

    public List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }

}
