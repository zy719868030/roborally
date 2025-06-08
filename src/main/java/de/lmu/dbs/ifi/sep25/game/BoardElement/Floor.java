package de.lmu.dbs.ifi.sep25.game.BoardElement;

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
public class Floor extends BoardElement {
    private static final Floor INSTANCE = new Floor();

    private Floor() {

    }

    public static Floor getInstance() {

        return INSTANCE;
    }

    @Override
    public void activate(Robot robot) {
            // No effect
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "Floor";
    }

    public void applyEffect(Robot robot, Board board) {
        // No effect
        activate(robot);
    }

    @Override
    public String toString() {
        return "Floor at " + (position != null ? position.toString() : "unspecified position");
    }
}

