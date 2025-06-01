package de.lmu.dbs.ifi.sep25.game.tile;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Represents a floor tile in the game that serves as a basic passive element.
 * The Floor class is implemented as a singleton, meaning only one instance
 * of this class can exist during the runtime of the application.
 * <p>
 * The floor tile does not apply any effect on a robot or the board. It is primarily
 * used as a base tile in the game where no specific action or behavior is required.
 */
@SuppressWarnings("unused")
public class Floor extends TileElement {
    private static final Floor INSTANCE = new Floor();

    private Floor() {
    }

    public static Floor getInstance() {
        return INSTANCE;
    }

    @Override
    public void applyEffect(Robot robot, Board board) {
        // No effect
    }
}
