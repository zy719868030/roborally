package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.tile.TileElement;

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