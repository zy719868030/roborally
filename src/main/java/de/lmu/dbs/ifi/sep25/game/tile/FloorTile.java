package de.lmu.dbs.ifi.sep25.game.tile;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Represents a floor tile in the game that serves as a basic passive element.
 * The FloorTile class is implemented as a singleton, meaning only one instance
 * of this class can exist during the runtime of the application.
 * <p>
 * The floor tile does not apply any effect on a robot or the board. It is primarily
 * used as a base tile in the game where no specific action or behavior is required.
 */
@SuppressWarnings("unused")
public class FloorTile extends TileElement {
    private static final FloorTile INSTANCE = new FloorTile();

    private FloorTile() {
    }

    public static FloorTile getInstance() {
        return INSTANCE;
    }

    @Override
    public void applyEffect(Robot robot, Board board) {
        // No effect
    }
}
