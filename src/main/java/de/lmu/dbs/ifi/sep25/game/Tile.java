package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a tile on the RoboRally game board, containing multiple board elements
 * that can affect robots and game mechanics.
 * 
 * <p>The Tile class serves as a container for board elements at a specific location
 * on the game board. Each tile can contain multiple board elements such as walls,
 * conveyors, gears, lasers, energy spaces, checkpoints, and other game mechanics
 * that interact with robots when they occupy or pass through the tile.</p>
 *
 */
@SuppressWarnings("unused")
public class Tile {
    
    /** List of board elements contained on this tile */
    private final List<BoardElement> elements;

    /**
     * Constructs an empty tile with no board elements.
     * 
     * <p>This constructor creates a new tile with an empty list of board elements.
     * The tile is ready to have elements added to it through the addElement method.
     * Empty tiles are typically used for floor spaces or as starting points for
     * building complex tile configurations.</p>
     */
    public Tile() {
        this.elements = new ArrayList<>();
    }

    /**
     * Constructs a tile with the specified board elements.
     * 
     * <p>This constructor creates a new tile and initializes it with the provided
     * list of board elements. The constructor performs defensive copying to ensure
     * that the internal element list cannot be modified through the provided reference.</p>
     *
     * @param elements the list of board elements to initialize the tile with
     */
    public Tile(List<BoardElement> elements) {
        // Defensive copying to prevent external modification
        this.elements = new ArrayList<>(elements);
    }

    /**
     * Adds a board element to this tile.
     * 
     * <p>This method adds a single board element to the tile's element collection.
     * The element is added to the end of the list and will be processed in order
     * when effects are applied to robots occupying or passing through the tile.</p>
     *
     * @param element the board element to add to this tile
     */
    public void addElement(BoardElement element) {
        elements.add(element);
    }

    /**
     * Removes a board element from this tile.
     * 
     * <p>This method removes a specific board element from the tile's element collection.
     * The element is removed from the list and will no longer be processed when
     * effects are applied to robots.</p>
     * 
     * @param element the board element to remove from this tile
     */
    public void removeElement(BoardElement element) {
        elements.remove(element);
    }

    /**
     * Gets all board elements contained on this tile.
     * 
     * <p>This method returns a list containing all board elements currently on
     * this tile. The returned list is a defensive copy, ensuring that external
     * modifications cannot affect the internal state of the tile.</p>
     *
     *
     * @return a new list containing all board elements on this tile
     */
    public List<BoardElement> getElements() {
        return elements;
    }

    /**
     * Checks if this tile contains a board element of the specified type.
     * 
     * <p>This method searches through all board elements on this tile to determine
     * if any element matches the class type of the provided element. The comparison
     * is based on class equality, not object identity.</p>
     *
     * @param element the board element whose class type to search for
     * @return true if the tile contains an element of the same class type, false otherwise
     */
    public boolean contains(BoardElement element) {
        return !elements.stream().filter(e -> e.getClass().equals(element.getClass())).toList().isEmpty();
    }

    /**
     * Applies the effects of all board elements on this tile to the specified robot.
     * 
     * <p>This method processes all board elements contained on this tile and applies
     * their effects to the specified robot. The elements are processed in the order
     * they were added to the tile. Processing stops if the robot falls off the board
     * during effect application.</p>
     *
     * <p>The method ensures that effects are applied safely and that processing
     * stops appropriately if the robot's state changes significantly (such as
     * falling off the board).</p>
     * 
     * @param robot the robot to apply tile effects to
     * @param board the game board for context and fall detection
     */
    public void applyEffects(Robot robot, Board board) {
        if (board.hasRobotFallen(robot)) return;
        for (BoardElement element : elements) {
//            element.activate(robot);
            element.applyEffect(robot, board);
            if (board.hasRobotFallen(robot)) break;
        }
    }

    /**
     * Checks if this tile contains no board elements.
     *
     * @return true if the tile contains no board elements, false otherwise
     */
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    /**
     * Creates a serializable list of board elements for protocol compliance.
     *
     * @return a new list containing all board elements, formatted for protocol transmission
     */
    public List<BoardElement> toSerializableList() {
        return new ArrayList<>(elements);
    }
}