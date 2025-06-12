package de.lmu.dbs.ifi.sep25.ui;

import javafx.beans.property.*;

/**
 * Repräsentiert einen Spieler in der Lobby mit unterstütztem JavaFX-Databinding.
 *
 * <p>
 * Diese Klasse nutzt JavaFX Properties (StringProperty, IntegerProperty, BooleanProperty),
 * um Änderungen an den Werten automatisch in der GUI widerspiegeln zu können. Das
 * ist notwendig, um die Anforderungen aus Milestone III (Databinding) zu erfüllen.
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
     * Konstruktor zur Initialisierung eines Spielerobjekts.
     *
     * @param clientID die eindeutige ID des Clients
     * @param name     der Spielername
     * @param figure   die gewählte Spielfigur (0–5)
     * @param ready    der Bereitschaftsstatus
     */
    public PlayerEntry(int clientID, String name, int figure, boolean ready) {
        this.clientID.set(clientID);
        this.name.set(name);
        this.figure.set(figure);
        this.ready.set(ready);
    }

    /**
     * Gibt die Client-ID zurück.
     *
     * @return die Client-ID
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
     * Gibt den Namen des Spielers zurück.
     *
     * @return der Spielername
     */
    public String getName() {
        return name.get();
    }

    /**
     * Property für den Spielernamen.
     *
     * @return die Property für den Namen
     */
    public StringProperty nameProperty() {
        return name;
    }

    /**
     * Gibt die gewählte Spielfigur zurück.
     *
     * @return die Figur-Nummer
     */
    public int getFigure() {
        return figure.get();
    }

    /**
     * Property für die Spielfigur.
     *
     * @return die Property für die Figur
     */
    public IntegerProperty figureProperty() {
        return figure;
    }

    /**
     * Gibt zurück, ob der Spieler bereit ist.
     *
     * @return {@code true}, wenn bereit
     */
    public boolean isReady() {
        return ready.get();
    }

    /**
     * Setzt den Bereitschaftsstatus.
     *
     * @param ready {@code true}, wenn bereit
     */
    public void setReady(boolean ready) {
        this.ready.set(ready);
    }

    /**
     * Property für den Bereitschaftsstatus.
     *
     * @return die Property für "ready"
     */
    public BooleanProperty readyProperty() {
        return ready;
    }

    /**
     * Gibt eine textuelle Darstellung des Spielers für die Anzeige im ListView zurück.
     *
     * @return z.B. "Figur 3 – Anna [bereit]"
     */
    @Override
    public String toString() {
        return String.format("Figur %d – %s [%s]", getFigure(), getName(), isReady() ? "bereit" : "nicht bereit");
    }
}
