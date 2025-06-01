package de.lmu.dbs.ifi.sep25.game;

public class Tile {
    private Robot robot;
    private int x;
    private int y;
    private String type;

    public Tile(int x, int y, String type) {
        this.x = x;
        this.y = y;
        this.type = type; // e.g., "floor"
        this.robot = null;
    }

    public void applyEffect(Robot robot) {
        // Stub: For future tile effects (e.g., conveyor, pit)
    }

    public Robot getRobot() {
        return robot;
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }

    public String getType() {
        return type;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}