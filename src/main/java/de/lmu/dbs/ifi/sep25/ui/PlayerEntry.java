package de.lmu.dbs.ifi.sep25.ui;

public class PlayerEntry {
    private final int clientID;
    private final String name;
    private final int figure;
    private boolean ready;

    public PlayerEntry(int clientID, String name, int figure, boolean ready) {
        this.clientID = clientID;
        this.name = name;
        this.figure = figure;
        this.ready = ready;
    }

    public int getClientID() {
        return clientID;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

    @Override
    public String toString() {
        return String.format("Figur %d – %s [%s]", figure, name, ready ? "bereit" : "nicht bereit");
    }
}
