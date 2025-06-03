package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.ArrayList;
import java.util.List;

/**
 * The Walls class represents wall elements on the game board.
 * Walls prevent robots from passing through and can have multiple orientations
 * (i.e., walls can block movement in multiple directions).
 */
public class Walls extends BoardElement {
    private List<Direction> blockedDirections;

    public Walls() {
        super();
        this.blockedDirections = new ArrayList<>();
    }

    public Walls(Position position) {
        super(position);
        this.blockedDirections = new ArrayList<>();
    }

    //Constructor with position and blocking direction parameters
    public Walls(Position position, Direction blockedDirection) {
        super(position);
        this.blockedDirections = new ArrayList<>();
        this.blockedDirections.add(blockedDirection);
    }

    //Constructor with position and multiple blocking direction parameters
    public Walls(Position position, List<Direction> blockedDirections) {
        super(position);
        this.blockedDirections = new ArrayList<>(blockedDirections);
    }

    public void addBlockedDirection(Direction direction) {
        if (!blockedDirections.contains(direction)) {
            blockedDirections.add(direction);
        }
    }

    public List<Direction> getBlockedDirections() {
        return new ArrayList<>(blockedDirections);
    }

    public boolean isDirectionBlocked(Direction direction) {
        return blockedDirections.contains(direction);
    }


    /**
     * When the robot enters a grid containing a wall, no special effect is triggered.
     * The main purpose of walls is to block movement, not to trigger effects when entered.
     *
     * @param robot The robot entering this element.
     */
    @Override
    public void activate(Robot robot) {
        // Walls do not trigger effects when robots enter them.
        // The blocking effect of walls should be handled in the movement logic.
    }

    /**
     * Checks whether the robot can pass through the wall from the specified direction.
     * If the robot's movement direction is opposite to the wall's blocking direction, it cannot pass through.
     *
     * @param robot The robot attempting to pass through.
     * @return Returns true if it can pass through, otherwise returns false.
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        Direction robotDirection = robot.getDirection();
        return !isDirectionBlocked(robotDirection.turnAround());
    }

    /**
     * Checks whether the robot can pass through the wall from the specified direction.
     *
     * @param fromDirection The direction from which the robot attempts to pass through.
     * @return Returns true if it can pass through, otherwise returns false.
     */
    public boolean canPassThroughFromDirection(Direction fromDirection) {
        return !isDirectionBlocked(fromDirection);
    }

    /**
     * Get element type
     *
     * @return ‘Wall’ string
     */
    @Override
    public String getType() {
        return "Wall";
    }


    /**
     * Get the string representation of the wall.
     *
     * @return The string representation of the wall, including its position and blocking direction.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Wall at ").append(position).append(", blocking directions: ");

        for (Direction dir : blockedDirections) {
            sb.append(dir.getName()).append(", ");
        }

        if (!blockedDirections.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }
}
