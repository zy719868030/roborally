package de.lmu.dbs.ifi.sep25.ui;

import javafx.beans.property.*;

/**
 * Represents a player in the lobby with JavaFX data binding support.
 *
 * <p>
 * This class uses JavaFX properties (StringProperty, IntegerProperty, BooleanProperty)
 * to automatically reflect value changes in the GUI. This is necessary to meet the
 * requirements of Milestone III (data binding).
 * </p>
 */
public class PlayerEntry {
    // JavaFX-Property für die eindeutige Client-ID
    private final IntegerProperty clientID = new SimpleIntegerProperty();

    // JavaFX-Property für den Spielernamen
    private final StringProperty name = new SimpleStringProperty();

    // JavaFX-Property für die gewählte Spielfigur
    private final IntegerProperty figure = new SimpleIntegerProperty();

    // JavaFX-Property für den Bereitschaftsstatus
    private final BooleanProperty ready = new SimpleBooleanProperty();

    /**
     * Constructor to initialize a player object.
     *
     * @param clientID the unique ID of the client
     * @param name     the player's name
     * @param figure   the selected game figure (0–5)
     * @param ready    the readiness status
     */
    public PlayerEntry(int clientID, String name, int figure, boolean ready) {
        this.clientID.set(clientID);
        this.name.set(name);
        this.figure.set(figure);
        this.ready.set(ready);
    }

    /**
     * Returns the client ID.
     *
     * @return the client ID
     */
    public int getClientID() {
        return clientID.get();
    }

    /**
     * Property für die Client-ID, nützlich für Bindings.
     *
     * @return die Client-ID als Property
     */
    public IntegerProperty clientIDProperty() {
        return clientID;
    }

    /**
     * Returns the name of the player.
     *
     * @return the player's name
     */
    public String getName() {
        return name.get();
    }

    /**
     * Property for the player's name.
     *
     * @return the property for the name
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Returns the selected game figure.
     *
     * @return the figure number
     */
    public int getFigure() {
        return figure.get();
    }

    /**
     * Property for the game figure.
     *
     * @return the property for the figure
     */
    public IntegerProperty figureProperty() {
        return figure;
    }

    /**
     * Returns whether the player is ready.
     *
     * @return {@code true} if ready
     */
    public boolean isReady() {
        return ready.get();
    }

    /**
     * Sets the ready status.
     *
     * @param ready {@code true} if ready
     */
    public void setReady(boolean ready) {
        this.ready.set(ready);
    }

    /**
     * Property for the ready status.
     *
     * @return the property representing "ready"
     */
    public BooleanProperty readyProperty() {
        return ready;
    }

    /**
     * Returns a textual representation of the player for display in the ListView.
     *
     * @return e.g. "Figure 3 – Anna [ready]"
     */
    @Override
    public String toString() {
        return String.format("Figur %d – %s [%s]", getFigure(), getName(), isReady() ? "bereit" : "nicht bereit");
    }
    /**
     * Returns a short string representation of the player, including their name and client ID.
     *
     * @return A string in the format "Name #ID".
     */
    public String toShortString() {
        return getName() + " #" + getClientID();
    }


    /**
     * Sets the player's name to the specified value.
     *
     * @param name The new name of the player.
     */
    public void setName(String name) { //@SEBAS
        this.name.set(name);
    }
}