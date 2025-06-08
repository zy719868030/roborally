package de.lmu.dbs.ifi.sep25.network;

public class ClientSingleton {
    private static Client client;

    public static void set(Client instance) {
        client = instance;
    }

    public static Client getInstance() {
        return client;
    }
}

