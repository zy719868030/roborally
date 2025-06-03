package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.game.Board;

public class Reboot extends TileElement {
    private static Reboot instance = null;

    private Reboot() {
        super("reboot");
        // Private constructor for singleton
    }

    public static Reboot getInstance() {
        if (instance == null) {
            instance = new Reboot();
        }
        return instance;
    }

    @Override
    public void applyEffect(Robot robot, Board board) {
        // Stub: Future reboot logic (e.g., reset robot position, damage)
    }
}
