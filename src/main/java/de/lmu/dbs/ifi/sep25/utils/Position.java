package de.lmu.dbs.ifi.sep25.utils;

public record Position(int x, int y) {

    // Move by delta
    public Position move(int dx, int dy) {
        return new Position(x + dx, y + dy);
    }

    // Move in a given direction
    public Position move(Direction direction) {
        return new Position(x + direction.getDeltaX(), y + direction.getDeltaY());
    }

    // Calculate Manhattan distance to another position
    public int distanceTo(Position other) {
        return Math.abs(x - other.x) + Math.abs(y - other.y);
    }

}
