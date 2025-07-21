package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import org.apache.logging.log4j.Logger;

import java.util.List;
/**
 * Represents the "ExtraCrispy" game map configuration for the RoboRally game.
 * This map corresponds to board "4A" and includes pits, belts, lasers,
 * gears, walls, energy spaces, and multiple checkpoints.
 */
public class ExtraCrispy extends GameMap {

    /**
     * Logger instance for debugging and tracking map initialization.
     */
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(ExtraCrispy.class);

    /**
     * Identifier for the board used in this map.
     */
    private final String boardId = "4A";

    /**
     * Constructs the ExtraCrispy map by initializing sub-boards,
     * the main board orientation, and placing board elements.
     */
    public ExtraCrispy() {
        super(new Position(0, 4));

        initializeSubBoards();
        initializeBoard("StartA", Direction.EAST);
        initializeElements();
    }

    /**
     * Defines the sub-boards used in this map.
     * Includes the "StartA" entry zone and the "4A" playable zone.
     */
    @Override
    protected void initializeSubBoards() {
        subBoards.put("StartA", new Board.SubBoard("StartA", 0, 2));
        subBoards.put(boardId, new Board.SubBoard(boardId, 3, 12));
    }

    /**
     * Adds all board elements such as pits, belts, lasers,
     * walls, gears, energy spaces, and checkpoints to the map.
     */
    @Override
    protected void initializeElements() {
        // Reboot point
        elements.add(new Reboot(new Position(0, 0), Direction.EAST, "StartA"));

        // Pits in board 4A
        List.of(new Position(6, 2), new Position(6, 3),
                        new Position(9, 2), new Position(9, 3),
                        new Position(6, 6), new Position(6, 7),
                        new Position(9, 6), new Position(9, 7))
                .forEach(pos -> elements.add(new Pit(pos, boardId)));

        // Energy spaces on specific tiles
        List.of(new Position(3, 4), new Position(3, 9), new Position(7, 5),
                        new Position(8, 0), new Position(11, 4))
                .forEach(pos -> elements.add(new EnergySpace(pos, boardId)));

        // Walls in various directions
        List.of(new Position(3, 4), new Position(5, 2),
                        new Position(10, 2), new Position(12, 4))
                .forEach(pos -> elements.add(new Wall(pos, Direction.NORTH, boardId)));

        List.of(new Position(11, 0), new Position(9, 4), new Position(8, 3),
                        new Position(8, 6), new Position(6, 9))
                .forEach(pos -> elements.add(new Wall(pos, Direction.EAST, boardId)));

        List.of(new Position(3, 5), new Position(5, 7), new Position(10, 7),
                        new Position(12, 5))
                .forEach(pos -> elements.add(new Wall(pos, Direction.SOUTH, boardId)));

        List.of(new Position(4, 9), new Position(6, 5), new Position(7, 3),
                        new Position(7, 6), new Position(9, 0))
                .forEach(pos -> elements.add(new Wall(pos, Direction.WEST, boardId)));

        // Conveyor belts
        for (int y = 0; y <= 2; y++)
            elements.add(new Belts(new Position(4, y), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(4, 3), Direction.EAST, List.of(Direction.NORTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(5, 3), Direction.EAST, List.of(Direction.WEST), Belts.BeltSpeed.FAST, boardId));

        for (int y = 7; y <= 9; y++)
            elements.add(new Belts(new Position(11, y), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(11, 6), Direction.WEST, List.of(Direction.SOUTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(10, 6), Direction.WEST, List.of(Direction.EAST), Belts.BeltSpeed.FAST, boardId));

        elements.add(new Belts(new Position(11, 1), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(11, 2), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(11, 3), Direction.WEST, List.of(Direction.NORTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(10, 3), Direction.WEST, List.of(Direction.EAST), Belts.BeltSpeed.FAST, boardId));

        elements.add(new Belts(new Position(4, 8), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(4, 7), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(4, 6), Direction.EAST, List.of(Direction.SOUTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(5, 6), Direction.EAST, List.of(Direction.WEST), Belts.BeltSpeed.FAST, boardId));

        elements.add(new Belts(new Position(8, 8), Direction.EAST, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(9, 8), Direction.NORTH, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId));

        elements.add(new Belts(new Position(6, 1), Direction.SOUTH, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(7, 1), Direction.WEST, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId));

        // Lasers in all directions
        elements.add(new Laser(new Position(4, 9), Direction.WEST, 1, boardId));
        elements.add(new Laser(new Position(6, 9), Direction.EAST, 1, boardId));
        elements.add(new Laser(new Position(9, 0), Direction.WEST, 1, boardId));
        elements.add(new Laser(new Position(11, 0), Direction.EAST, 1, boardId));
        elements.add(new Laser(new Position(7, 3), Direction.WEST, 1, boardId));
        elements.add(new Laser(new Position(8, 3), Direction.EAST, 1, boardId));
        elements.add(new Laser(new Position(7, 6), Direction.WEST, 1, boardId));
        elements.add(new Laser(new Position(8, 6), Direction.EAST, 1, boardId));

        elements.add(new Laser(new Position(5, 2), Direction.NORTH, 1, boardId));
        elements.add(new Laser(new Position(10, 2), Direction.NORTH, 1, boardId));
        elements.add(new Laser(new Position(5, 7), Direction.SOUTH, 1, boardId));
        elements.add(new Laser(new Position(10, 7), Direction.SOUTH, 1, boardId));

        // Gears
        elements.add(new Gear(new Position(6, 4), Gear.RotationDirection.CLOCKWISE, boardId));
        elements.add(new Gear(new Position(9, 5), Gear.RotationDirection.COUNTERCLOCKWISE, boardId));

        // Checkpoints (1–4)
        final Position[] checkpointPositions = {
                new Position(10, 2),
                new Position(5, 7),
                new Position(10, 7),
                new Position(5, 2)
        };
        for (int i = 1; i <= 4; i++)
            elements.add(new CheckPoints(checkpointPositions[i - 1], i, boardId));
    }
}
