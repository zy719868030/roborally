package de.lmu.dbs.ifi.sep25.game.tile;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Tile {
    private final List<TileElement> elements = new ArrayList<>();

    public void addElement(TileElement element) {
        elements.add(element);
    }

    public void removeElement(TileElement element) {
        elements.remove(element);
    }

    public List<TileElement> getElements() {
        return elements;
    }

    public void applyEffects(Robot robot, Board board) {
        for (TileElement element : elements) {
            element.applyEffect(robot, board);
        }
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}