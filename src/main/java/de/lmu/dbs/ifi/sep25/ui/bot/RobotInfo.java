package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;

/**
 * Represents the complete and up-to-date state of a robot/player in the RoboRally game.
 * <p>
 * This record encapsulates all information needed for game logic and bot strategies,
 * including the robot's unique client ID, current position, facing direction, number of
 * checkpoints reached, and current energy count.
 * <p>
 * Instances are immutable. To update a robot's state, create a new {@code RobotInfo}
 * object and replace the existing one in your state-tracking map.
 *
 * @param clientID           The unique client ID assigned by the server to this player/robot.
 * @param position           The robot's current {@link Position} (x, y) coordinates on the board.
 * @param direction          The robot's current {@link Direction} (e.g., NORTH, EAST, SOUTH, WEST).
 * @param checkpointsReached The number of checkpoints this robot has reached so far.
 * @param energy             The number of energy cubes currently held by this robot.
 * @param damage             The compound amount of damage received
 */
public record RobotInfo(
        int clientID,
        Position position,
        Direction direction,
        int checkpointsReached,
        int energy,
        int damage
) {
    /**
     * Returns true if the robot is at the specified position.
     */
    public boolean isAt(Position p) {
        return this.position.equals(p);
    }

    /**
     * Returns the Manhattan distance to another position.
     */
    public int distanceTo(Position other) {
        return Math.abs(this.position.x() - other.x()) + Math.abs(this.position.y() - other.y());
    }

    /**
     * Returns true if the robot has reached at least the specified number of checkpoints.
     */
    public boolean hasReachedCheckpoint(int checkpointNum) {
        return this.checkpointsReached >= checkpointNum;
    }

    /**
     * Returns true if the robot currently has any energy cubes.
     */
    public boolean hasEnergy() {
        return this.energy > 0;
    }
}