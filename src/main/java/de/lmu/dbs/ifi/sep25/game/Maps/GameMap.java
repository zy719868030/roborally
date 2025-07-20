package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**
 * Abstract base class for defining custom game maps in RoboRally.
 * Each map consists of one or more sub-boards, a list of board elements, and an antenna position.
 * Subclasses must implement {@code initializeSubBoards()} and {@code initializeElements()}.
 */
public abstract class GameMap {

    /**
     * The list of all elements (walls, belts, pits, etc.) that make up the map.
     */
    protected final List<BoardElement> elements = new ArrayList<>();

    /**
     * A mapping from board ID to their respective sub-board definitions.
     */
    protected final Map<String, Board.SubBoard> subBoards = new HashMap<>();

    /**
     * The location of the antenna used for signal-related game mechanics.
     */
    protected final Position antennaPosition;

    /**
     * Constructs a new {@code GameMap} with a given antenna position.
     *
     * @param antennaPosition The position of the antenna on the board.
     */
    public GameMap(Position antennaPosition) {
        this.antennaPosition = antennaPosition;
    }

    /**
     * Factory method that loads a specific map implementation based on the map name.
     *
     * @param mapName The name of the map to load.
     * @return A {@link GameMap} instance corresponding to the specified name.
     * @throws IllegalArgumentException if the map name is unknown.
     */
    public static GameMap load(String mapName) {
        return switch (mapName) {
            case "Dizzy Highway" -> new DizzyHighway();
            case "Extra Crispy" -> new ExtraCrispy();
            case "Lost Bearings" -> new LostBearings();
            case "Death Trap" -> new DeathTrap();
            default -> throw new IllegalArgumentException("Unknown map name: " + mapName);
        };
    }

    /**
     * Returns a list of all board elements on this map.
     *
     * @return A copy of the list of board elements.
     */
    public List<BoardElement> getElements() {
        return new ArrayList<>(elements);
    }

    /**
     * Returns a map of sub-board definitions used in this game map.
     *
     * @return A copy of the sub-board mapping.
     */
    public Map<String, Board.SubBoard> getSubBoards() {
        return new HashMap<>(subBoards);
    }

    /**
     * Returns the antenna position used by this game map.
     *
     * @return The antenna's position.
     */
    public Position getAntennaPosition() {
        return antennaPosition;
    }

    /**
     * Initializes the StartA sub-board with antenna, walls, belts and start positions,
     * depending on the specified orientation (EAST or WEST).
     *
     * @param boardId   The identifier of the sub-board (usually "StartA").
     * @param direction The direction the antenna and start points are facing.
     * @throws IllegalArgumentException If the board ID or direction is unsupported.
     */
    public void initializeBoard(String boardId, Direction direction) {
        switch (direction) {
            case EAST -> {
                switch (boardId) {
                    case "StartA" -> {
                        // Add directional walls
                        elements.add(new Wall(new Position(1, 2), Direction.NORTH, "StartA"));
                        elements.add(new Wall(new Position(2, 4), Direction.EAST, "StartA"));
                        elements.add(new Wall(new Position(2, 5), Direction.EAST, "StartA"));
                        elements.add(new Wall(new Position(1, 7), Direction.SOUTH, "StartA"));

                        // Add antenna and start points
                        elements.add(new Antenna(antennaPosition, Direction.EAST, "StartA"));

                        // Add slow belts
                        elements.add(new Belts(new Position(2, 9), Direction.EAST, Direction.WEST, Belts.BeltSpeed.SLOW, "StartA"));
                        elements.add(new Belts(new Position(2, 0), Direction.EAST, Direction.WEST, Belts.BeltSpeed.SLOW, "StartA"));
                    }
                    default -> throw new IllegalArgumentException("Unknown/Unimplemented board ID: " + boardId);
                }
            }
            case WEST -> {
                switch (boardId) {
                    case "StartA" -> {
                        elements.add(new Wall(new Position(11, 2), Direction.NORTH, "StartA"));
                        elements.add(new Wall(new Position(11, 7), Direction.SOUTH, "StartA"));
                        elements.add(new Wall(new Position(10, 4), Direction.WEST, "StartA"));
                        elements.add(new Wall(new Position(10, 5), Direction.WEST, "StartA"));

                        elements.add(new Antenna(antennaPosition, Direction.WEST, "StartA"));
                        List.of(
                                new Position(11, 1), new Position(12, 3), new Position(11, 4),
                                new Position(11, 5), new Position(11, 8), new Position(12, 6)
                        ).forEach(pos -> elements.add(new StartPoint(pos, Direction.WEST, "StartA")));

                        elements.add(new Belts(new Position(10, 9), Direction.WEST, Direction.EAST, Belts.BeltSpeed.SLOW, "StartA"));
                        elements.add(new Belts(new Position(10, 0), Direction.WEST, Direction.EAST, Belts.BeltSpeed.SLOW, "StartA"));
                    }
                    default -> throw new IllegalArgumentException("Unknown/Unimplemented board ID: " + boardId);
                }
            }
        }
    }
    /**
     * Must be implemented by subclasses to define sub-board metadata (IDs and y-coordinate bounds).
     */
    abstract protected void initializeSubBoards();
    /**
     * Must be implemented by subclasses to populate the board with its game elements (walls, belts, etc.).
     */
    abstract protected void initializeElements();
}
