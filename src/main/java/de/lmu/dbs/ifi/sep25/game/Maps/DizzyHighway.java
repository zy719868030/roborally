package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import org.apache.logging.log4j.Logger;

import java.util.List;
/**
 * Represents the "DizzyHighway" game map configuration for the RoboRally game.
 * This class defines the layout and elements of board 5B, including belts,
 * energy spaces, walls, checkpoints, lasers, and a reboot point.
 */
public class DizzyHighway extends GameMap {

    /**
     * Logger instance for debugging and runtime information.
     */
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(DizzyHighway.class);

    /**
     * Identifier for the board used in this map.
     */
    private final String boardId = "5B";

    /**
     * Constructs the DizzyHighway map by initializing sub-boards, the main board,
     * and placing all elements such as belts, lasers, walls, and checkpoints.
     */
    public DizzyHighway() {
        super(new Position(0, 4));

        initializeSubBoards();
        initializeBoard("StartA", Direction.EAST);
        initializeElements();
    }

    /**
     * Initializes the sub-board definitions for the map.
     * Includes the start board and the playable board 5B.
     */
    @Override
    protected void initializeSubBoards() {
        subBoards.put("StartA", new Board.SubBoard("StartA", 0, 2));
        subBoards.put(boardId, new Board.SubBoard(boardId, 3, 12));
    }

    /**
     * Initializes all elements that are part of the DizzyHighway board.
     * Elements include various conveyor belts, energy spaces, a checkpoint,
     * a reboot point, walls, and lasers.
     */
    @Override
    protected void initializeElements() {
        // Belts on fixed positions
        elements.add(new Belts(new Position(3, 7), Direction.EAST, Direction.WEST, Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(10, 9), Direction.NORTH, Direction.SOUTH, Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(12, 2), Direction.WEST, Direction.EAST, Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(5, 0), Direction.SOUTH, Direction.NORTH, Belts.BeltSpeed.FAST, boardId));

        // Conveyor belts in column 4 with custom directions
        for (int y = 0; y <= 8; y++) {
            switch (y) {
                case 1 ->
                        elements.add(new Belts(new Position(4, y), Direction.SOUTH, List.of(Direction.NORTH, Direction.EAST), Belts.BeltSpeed.FAST, boardId));
                case 7 ->
                        elements.add(new Belts(new Position(4, y), Direction.SOUTH, List.of(Direction.NORTH, Direction.WEST), Belts.BeltSpeed.FAST, boardId));
                case 8 ->
                        elements.add(new Belts(new Position(4, y), Direction.EAST, List.of(Direction.NORTH, Direction.WEST), Belts.BeltSpeed.FAST, boardId));
                default ->
                        elements.add(new Belts(new Position(4, y), Direction.SOUTH, Direction.NORTH, Belts.BeltSpeed.FAST, boardId));
            }
        }

        // Conveyor belts in row 8
        for (int x = 3; x <= 11; x++) {
            switch (x) {
                case 4 -> {
                } // skip
                case 10 ->
                        elements.add(new Belts(new Position(x, 8), Direction.EAST, List.of(Direction.SOUTH, Direction.WEST), Belts.BeltSpeed.FAST, boardId));
                case 11 ->
                        elements.add(new Belts(new Position(x, 8), Direction.NORTH, List.of(Direction.SOUTH, Direction.WEST), Belts.BeltSpeed.FAST, boardId));
                default ->
                        elements.add(new Belts(new Position(x, 8), Direction.EAST, Direction.WEST, Belts.BeltSpeed.FAST, boardId));
            }
        }

        // Conveyor belts in column 11
        for (int y = 1; y <= 9; y++) {
            switch (y) {
                case 8 ->
                        elements.add(new Belts(new Position(11, y), Direction.NORTH, List.of(Direction.SOUTH, Direction.WEST), Belts.BeltSpeed.FAST, boardId));
                case 2 ->
                        elements.add(new Belts(new Position(11, y), Direction.NORTH, List.of(Direction.SOUTH, Direction.EAST), Belts.BeltSpeed.FAST, boardId));
                case 1 ->
                        elements.add(new Belts(new Position(11, y), Direction.WEST, List.of(Direction.SOUTH, Direction.EAST), Belts.BeltSpeed.FAST, boardId));
                default ->
                        elements.add(new Belts(new Position(11, y), Direction.NORTH, Direction.SOUTH, Belts.BeltSpeed.FAST, boardId));
            }
        }

        // Conveyor belts in row 1
        for (int x = 5; x <= 12; x++) {
            switch (x) {
                case 5 ->
                        elements.add(new Belts(new Position(x, 1), Direction.WEST, List.of(Direction.NORTH, Direction.EAST), Belts.BeltSpeed.FAST, boardId));
                case 11 ->
                        elements.add(new Belts(new Position(x, 1), Direction.WEST, List.of(Direction.SOUTH, Direction.EAST), Belts.BeltSpeed.FAST, boardId));
                default ->
                        elements.add(new Belts(new Position(x, 1), Direction.WEST, Direction.EAST, Belts.BeltSpeed.FAST, boardId));
            }
        }

        // Energy spaces on specific positions
        List.of(new Position(3, 9), new Position(10, 7), new Position(12, 0),
                        new Position(5, 2), new Position(7, 5), new Position(8, 4))
                .forEach(pos -> elements.add(new EnergySpace(pos, boardId)));

        // Single checkpoint
        CheckPoints cp = new CheckPoints(new Position(12, 3), 1, boardId);
        elements.add(cp);

        // Reboot point
        elements.add(new Reboot(new Position(7, 3), Direction.SOUTH, boardId));

        // Walls around specific tiles
        elements.add(new Wall(new Position(6, 4), Direction.SOUTH, boardId));
        elements.add(new Wall(new Position(7, 6), Direction.EAST, boardId));
        elements.add(new Wall(new Position(8, 3), Direction.WEST, boardId));
        elements.add(new Wall(new Position(9, 5), Direction.NORTH, boardId));

        elements.add(new Wall(new Position(6, 3), Direction.NORTH, boardId));
        elements.add(new Wall(new Position(6, 6), Direction.WEST, boardId));
        elements.add(new Wall(new Position(9, 3), Direction.EAST, boardId));
        elements.add(new Wall(new Position(9, 6), Direction.SOUTH, boardId));

        // Lasers pointing in various directions
        elements.add(new Laser(new Position(6, 4), Direction.SOUTH, 1, boardId));
        elements.add(new Laser(new Position(7, 6), Direction.EAST, 1, boardId));
        elements.add(new Laser(new Position(8, 3), Direction.WEST, 1, boardId));
        elements.add(new Laser(new Position(9, 5), Direction.NORTH, 1, boardId));
    }
}