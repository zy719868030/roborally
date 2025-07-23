package de.lmu.dbs.ifi.sep25.game;

/**
 * Represents the four cardinal directions on a grid-based system in RoboRally.
 * 
 * <p>The Direction enum provides a comprehensive system for handling movement
 * and orientation in the 2D grid-based game world. Each direction is defined
 * with a human-readable name and associated delta values that represent the
 * change in X and Y coordinates when moving in that direction.</p>
 * 
 * <p>Key features of the Direction enum:</p>
 * <ul>
 *   <li><strong>Cardinal Directions:</strong> NORTH, EAST, SOUTH, WEST with precise coordinate deltas</li>
 *   <li><strong>Rotation Operations:</strong> turnLeft(), turnRight(), and turnAround() methods</li>
 *   <li><strong>Coordinate Deltas:</strong> getDeltaX() and getDeltaY() for movement calculations</li>
 *   <li><strong>Protocol Compliance:</strong> String conversion for network communication</li>
 *   <li><strong>Human-Readable Names:</strong> getName() for user interface display</li>
 *   <li><strong>Bidirectional Conversion:</strong> fromString() and toString() for serialization</li>
 * </ul>
 * 
 * <p>The Direction enum is used throughout the game for robot movement, board
 * element orientation, laser firing directions, and various other directional
 * calculations. It ensures consistent and predictable movement behavior across
 * the entire game system.</p>
 * 
 * @author Edle Eisbecher Team
 * @version 1.0
 * @since 1.0
 */
public enum Direction {
    
    /** North direction: moves upward on the grid (dx=0, dy=-1) */
    NORTH("North", 0, -1),
    
    /** East direction: moves rightward on the grid (dx=1, dy=0) */
    EAST("East", 1, 0),
    
    /** South direction: moves downward on the grid (dx=0, dy=1) */
    SOUTH("South", 0, 1),
    
    /** West direction: moves leftward on the grid (dx=-1, dy=0) */
    WEST("West", -1, 0);

    /** The change in X coordinate when moving in this direction */
    private final int dx;
    
    /** The change in Y coordinate when moving in this direction */
    private final int dy;
    
    /** The human-readable name of this direction */
    private final String name;

    /**
     * Constructs a Direction enum value with specified properties.
     * 
     * <p>This constructor initializes a direction with a human-readable name
     * and the delta values that represent the coordinate changes when moving
     * in this direction on a 2D grid.</p>
     * 
     * @param name the human-readable name of the direction
     * @param dx the change in X coordinate when moving in this direction
     * @param dy the change in Y coordinate when moving in this direction
     */
    Direction(String name, int dx, int dy) {
        this.name = name;
        this.dx = dx;
        this.dy = dy;
    }

    /**
     * Gets the human-readable name of this direction.
     * 
     * <p>This method returns the display name of the direction, which is useful
     * for user interfaces, logging, and debugging purposes.</p>
     * 
     * @return the human-readable name of this direction
     */
    public String getName() {
        return name;
    }

    public int getDx() {
        return dx;
    }

    public int getDy() {
        return dy;
    }

    /**
     * Rotates this direction 90 degrees counterclockwise (to the left).
     * 
     * <p>This method returns the direction that is 90 degrees to the left
     * of the current direction. The rotation follows the standard compass
     * pattern: NORTH → WEST → SOUTH → EAST → NORTH.</p>
     * 
     * @return the direction 90 degrees to the left of this direction
     */
    public Direction turnLeft() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
        };
    }

    /**
     * Rotates this direction 90 degrees clockwise (to the right).
     * 
     * <p>This method returns the direction that is 90 degrees to the right
     * of the current direction. The rotation follows the standard compass
     * pattern: NORTH → EAST → SOUTH → WEST → NORTH.</p>
     * 
     * @return the direction 90 degrees to the right of this direction
     */
    public Direction turnRight() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    /**
     * Rotates this direction 180 degrees (turns around).
     * 
     * <p>This method returns the direction that is directly opposite to the
     * current direction. This is equivalent to rotating 180 degrees in either
     * direction: NORTH ↔ SOUTH and EAST ↔ WEST.</p>
     * 
     * @return the direction opposite to this direction
     */
    public Direction turnAround() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    /**
     * Gets the change in X coordinate when moving in this direction.
     * 
     * <p>This method returns the delta X value that represents how much the
     * X coordinate changes when moving one step in this direction. Positive
     * values indicate movement to the right, negative values to the left.</p>
     * 
     * @return the change in X coordinate when moving in this direction
     */
    public int getDeltaX() {
        return dx;
    }

    /**
     * Gets the change in Y coordinate when moving in this direction.
     * 
     * <p>This method returns the delta Y value that represents how much the
     * Y coordinate changes when moving one step in this direction. Positive
     * values indicate movement downward, negative values upward.</p>
     * 
     * @return the change in Y coordinate when moving in this direction
     */
    public int getDeltaY() {
        return dy;
    }

    /**
     * Converts a string representation to a Direction enum value.
     * 
     * <p>This method parses string representations of directions and converts
     * them to the corresponding Direction enum value. It supports protocol-specific
     * string formats used in network communication.</p>
     *
     * 
     * <p>The method is case-insensitive and returns null for unrecognized strings
     * or null input.</p>
     * 
     * @param direction the string representation of the direction
     * @return the corresponding Direction enum value, or null if the string is not recognized
     */
    public static Direction fromString(String direction) {
        if (direction == null) return null;
        return switch (direction.toLowerCase()) {
            case "top" -> NORTH;
            case "right" -> EAST;
            case "bottom" -> SOUTH;
            case "left" -> WEST;
            default -> null;
        };
    }

    /**
     * Converts this Direction enum value to a string representation.
     * 
     * <p>This method returns a protocol-compliant string representation of the
     * direction for network communication and serialization purposes. The returned
     * string follows the protocol specification used in client-server communication.</p>
     *
     * 
     * @return the protocol-compliant string representation of this direction
     */
    @Override
    public String toString() {
        return switch (this) {
            case NORTH -> "top";
            case EAST -> "right";
            case SOUTH -> "bottom";
            case WEST -> "left";
        };
    }
}
