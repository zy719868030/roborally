package de.lmu.dbs.ifi.sep25.game;

/**
 * Represents a position in a 2D coordinate system.
 * This class is implemented as a record, providing immutable x and y coordinates.
 */
@SuppressWarnings("unused")
public record Position(int x, int y, String boardId) {
    public Position(int x, int y){
        this(x,y,null);
    }

    /**
     * Moves the current position by the specified delta values for x and y.
     * This method adds the provided delta values to the current coordinates
     * and returns a new Position object with the updated coordinates.
     *
     * @param dx the change in the x-coordinate
     * @param dy the change in the y-coordinate
     * @return a new Position object representing the updated position
     */
    public Position move(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    /**
     * Moves the position based on the provided direction.
     * The direction determines the change in x and y coordinates, which are added to the
     * current position to calculate the new position.
     *
     * @param direction the direction to move, providing delta x and delta y values
     * @return a new Position object representing the updated position after the move
     */
    public Position move(Direction direction) {
        return new Position(x + direction.getDeltaX(), y + direction.getDeltaY());
    }

    /**
     * Calculates the Manhattan distance between the current position and another given position.
     *
     * @param other the target Position object to which the distance is calculated
     * @return the Manhattan distance as an integer
     */
    public int distanceTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

}
