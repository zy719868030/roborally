package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Represents the starting points in the game.
 * Players can choose to place robots at these starting points at the beginning of the game.
 */
public class StartPoint extends BoardElement {

    // Mark whether this starting point has been occupied
    private boolean occupied;

    // Record the player ID occupying this starting point
    private int playerID;

    /**
     * Create a starting point.
     *
     * @param position Starting point position.
     * @param direction Direction the robot faces when placed at this starting point.
     */
    public StartPoint(Position position, Direction direction) {
        super(position, direction);
        this.occupied = false;
        this.playerID = -1; //-1 indicates unoccupied
    }

    /**
     * Attempt to occupy this starting point.
     *
     * @param player Player ID attempting to occupy.
     * @return Returns true if the occupation is successful, otherwise returns false.
     */
    public boolean occupy(int player) {
        if (!occupied) {
            occupied = true;
            playerID = player;
            return true;
        }
        return false;
    }

    /**
     * Release this starting point
     */
    public void release() {
        occupied = false;
        playerID = -1;
    }

    /**
     * Check whether this starting point has already been occupied.
     *
     * @return Returns true if it has already been occupied, otherwise returns false.
     */
    public boolean isOccupied() {
        return occupied;
    }

    /**
     * Get the player ID occupying this starting point.
     *
     * @return Player ID. If not occupied, return -1.
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * Get the orientation of the robot when it is placed at this starting point.
     *
     * @return The orientation of the robot.
     */
    public Direction getRobotDirection() {
        return direction;
    }

    /**
     * The starting point itself does not activate the robot.
     */
    @Override
    public void activate(Robot robot) {
    }


    /**
     * Check whether the robot can pass through the starting point.
     * If the starting point is occupied, the robot cannot pass through.
     * If the starting point is not occupied, the robot can pass through.
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return !occupied;
    }

    /**
     * Get element type
     */
    @Override
    public String getType() {
        return "StartPoint";
    }


    /**
     * Restart the robot at this starting point.
     *
     * @param robot The robot that needs to be restarted.
     */
    public void rebootRobot(Robot robot) {
        robot.setPosition(this.position);
        robot.setDirection(this.direction);
    }
}
