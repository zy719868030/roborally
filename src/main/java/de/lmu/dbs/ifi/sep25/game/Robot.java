package de.lmu.dbs.ifi.sep25.game;

import java.awt.Point;

public class Robot {
    private int positionX;
    private int positionY;
    private String direction;
    private int damage;

    public Robot(int positionX, int positionY, String direction) {
        this.positionX = positionX;
        this.positionY = positionY;
        this.direction = direction; // e.g., "north"
        this.damage = 0;
    }

    public void turnR() {
        switch (direction) {
            case "north": direction = "east"; break;
            case "east": direction = "south"; break;
            case "south": direction = "west"; break;
            case "west": direction = "north"; break;
        }
    }

    public void turnL() {
        switch (direction) {
            case "north": direction = "west"; break;
            case "west": direction = "south"; break;
            case "south": direction = "east"; break;
            case "east": direction = "north"; break;
        }
    }

    public Point getPosition() {
        return new Point(positionX, positionY);
    }

    public void move(int steps) {
        switch (direction) {
            case "north": positionY -= steps; break;
            case "south": positionY += steps; break;
            case "east": positionX += steps; break;
            case "west": positionX -= steps; break;
        }
    }

    public String getDirection() {
        return direction;
    }

    public int getDamage() {
        return damage;
    }
}
