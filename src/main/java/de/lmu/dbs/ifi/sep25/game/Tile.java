package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Tile {
    private final List<BoardElement> elements = new ArrayList<>();

    public Tile() {

    }

    public Tile(List<BoardElement> elements) {
        this.elements.addAll(elements);
    }

    public void addElement(BoardElement element) {
        elements.add(element);
    }

    public void removeElement(BoardElement element) {
        elements.remove(element);
    }

    public List<BoardElement> getElements() {
        return elements;
    }

    public boolean contains(BoardElement element) {
        return !elements.stream().filter(e -> e.getClass().equals(element.getClass())).toList().isEmpty();
    }

    public void applyEffects(Robot robot, Board board) {
        for (BoardElement element : elements) {
            element.activate(robot);
        }
    }

    public boolean isEmpty() {
        return elements.isEmpty();
    }
}