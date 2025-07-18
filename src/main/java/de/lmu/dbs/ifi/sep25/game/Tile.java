package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class Tile {
    //private final List<BoardElement> elements = new ArrayList<>();
    private final List<BoardElement> elements;

    public Tile() {
        this.elements = new ArrayList<>();
    }

    public Tile(List<BoardElement> elements) {

        //this.elements.addAll(elements);
        this.elements = new ArrayList<>(elements);
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
        if (board.hasRobotFallen(robot)) return;
        for (BoardElement element : elements) {
//            element.activate(robot);
            element.applyEffect(robot, board);
            if (board.hasRobotFallen(robot)) break;
        }
    }

    public boolean isEmpty() {

        return elements.isEmpty();
    }

    // Serialize to List<BoardElement> for protocol compliance
    public List<BoardElement> toSerializableList() {
        return new ArrayList<>(elements);
    }
}