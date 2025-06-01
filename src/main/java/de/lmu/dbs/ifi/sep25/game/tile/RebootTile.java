package de.lmu.dbs.ifi.sep25.game.tile;

import de.lmu.dbs.ifi.sep25.game.Robot;

public class RebootTile extends TileElement {
    private static RebootTile instance = null;

    private RebootTile() {
        super("reboot");
        // Private constructor for singleton
    }

    public static RebootTile getInstance() {
        if (instance == null) {
            instance = new RebootTile();
        }
        return instance;
    }

    @Override
    public void applyEffect(Robot robot) {
        // Stub: Future reboot logic (e.g., reset robot position, damage)
    }
}
