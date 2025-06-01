package de.lmu.dbs.ifi.sep25.game;

import java.util.ArrayList;
import java.util.List;

public class Tile {
    private Robot robot;
    private int x;
    private int y;
    private List<FabricElement> elements;

    public Tile(int x, int y) {
        this.x = x;
        this.y = y;
        this.elements = new ArrayList<>();
        this.elements.add(new Floor()); // Every tile has a floor by default
        this.robot = null;
    }

    public void applyEffect(Robot robot) {
        // Apply effects of all factory elements
        for (FabricElement element : elements) {
            element.applyEffect(robot);
        }
    }

    public Robot getRobot() {
        return robot;
    }

    public void setRobot(Robot robot) {
        this.robot = robot;
    }

    public List<FabricElement> getElements() {
        return elements;
    }

    public void addElement(FabricElement element) {
        elements.add(element);
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }
}