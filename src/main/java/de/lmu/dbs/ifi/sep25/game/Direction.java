package de.lmu.dbs.ifi.sep25.game;

/**
 * The Direction enum represents possible directions on a grid-based system.
 * Each direction has a name and associated delta values (dx, dy) representing
 * the change in X and Y coordinates when moving in that direction.
 */
public enum Direction {
    NORTH("North", 0, -1),
    EAST("East", 1, 0),
    SOUTH("South", 0, 1),
    WEST("West", -1, 0);

    private final int dx;
    private final int dy;
    private final String name;

    Direction(String name, int dx, int dy) {
        this.name = name;
        this.dx = dx;
        this.dy = dy;
    }

    public String getName() {
        return name;
    }

    public Direction turnLeft() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
        };
    }

    public Direction turnRight() {
        return switch (this) {
            case NORTH -> WEST;
            case EAST -> NORTH;
            case SOUTH -> EAST;
            case WEST -> SOUTH;
        };
    }

    public Direction turnAround() {
        return switch (this) {
            case NORTH -> SOUTH;
            case EAST -> WEST;
            case SOUTH -> NORTH;
            case WEST -> EAST;
        };
    }

    public int getDeltaX() {
        return dx;
    }

    public int getDeltaY() {
        return dy;
    }
}
