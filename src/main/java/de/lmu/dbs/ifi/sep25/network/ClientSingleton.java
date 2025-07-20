package de.lmu.dbs.ifi.sep25.network;

public class ClientSingleton {
    private static Client client;
    /**
     * Sets the singleton instance of the Client.
     *
     * <p>This method assigns the provided Client object as the global singleton instance,
     * allowing it to be accessed throughout the application via {@link #getInstance()}.
     *
     * @param instance the Client instance to set as the singleton
     */
    public static void setInstance(Client instance) {
        client = instance;
    }

    /**
     * Retrieves the singleton instance of the Client.
     *
     * <p>This method returns the globally accessible Client instance previously set
     * by {@link #setInstance(Client)}. If no instance has been set, this method returns {@code null}.
     *
     * @return the singleton Client instance, or {@code null} if not set
     */
    public static Client getInstance() {
        return client;
    }
}

