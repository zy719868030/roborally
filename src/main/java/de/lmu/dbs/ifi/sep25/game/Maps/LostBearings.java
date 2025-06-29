package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class LostBearings extends GameMap {
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(ExtraCrispy.class);
    private final String boardId = "1A";

    public LostBearings() {
        super(new Position(0, 0));
        elements.add(new Antenna(antennaPosition, Direction.EAST, "StartA"));

        initializeSubBoards();
        initializeBoard("StartA", Direction.EAST);
        initializeElements();
    }

    protected void initializeSubBoards() {
        subBoards.put("StartA", new Board.SubBoard("StartA", 0, 2));
        subBoards.put(boardId, new Board.SubBoard(boardId, 3, 12));
    }

    protected void initializeElements() {
        // 1A
        // Reboot
        elements.add(new Reboot(new Position(0, 0), Direction.EAST, "StartA"));

        // Wall
        elements.add(new Wall(new Position(6, 3), Direction.WEST, boardId));
        elements.add(new Wall(new Position(6, 6), Direction.WEST, boardId));
        elements.add(new Wall(new Position(9, 3), Direction.EAST, boardId));
        elements.add(new Wall(new Position(9, 6), Direction.EAST, boardId));


        // ConveyorBelts
        elements.add(new Belts(new Position(5, 3), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(5, 6), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(10, 3), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.FAST, boardId));
        elements.add(new Belts(new Position(10, 6), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.FAST, boardId));

        List.of(new Position(8, 1), new Position(9, 1), new Position(3, 8), new Position(12, 8))
                .forEach(pos -> elements.add(new Belts(pos, Direction.EAST, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId)));
        List.of(new Position(6, 1), new Position(7, 1), new Position(3, 1), new Position(12, 1))
                .forEach(pos -> elements.add(new Belts(pos, Direction.WEST, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId)));

        elements.add(new Belts(new Position(4, 8), Direction.SOUTH, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(4, 9), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.SLOW, boardId));

        elements.add(new Belts(new Position(4, 0), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(4, 1), Direction.WEST, List.of(Direction.NORTH), Belts.BeltSpeed.SLOW, boardId));

        elements.add(new Belts(new Position(11, 0), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(11, 1), Direction.NORTH, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId));

        elements.add(new Belts(new Position(11, 8), Direction.EAST, List.of(Direction.SOUTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(11, 9), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.SLOW, boardId));

        // Pit
        List.of(new Position(6, 2), new Position(9, 2),
                        new Position(6, 7), new Position(9, 7))
                .forEach(pos -> elements.add(new Pit(pos, boardId)));

        // EnergySpace
        List.of(new Position(5, 2), new Position(10, 2),
                        new Position(5, 7), new Position(10, 7),
                        new Position(7, 4), new Position(8, 5))
                .forEach(pos -> elements.add(new EnergySpace(pos, boardId)));

        // Laser
        elements.add(new Laser(new Position(9, 3), Direction.EAST, 1, boardId));
        elements.add(new Laser(new Position(6, 6), Direction.WEST, 1, boardId));

        // Gear
        List.of(new Position(5, 5), new Position(7, 5), new Position(10, 4))
                .forEach(pos -> elements.add(new Gear(pos, Gear.RotationDirection.CLOCKWISE, boardId)));
        List.of(new Position(5, 4), new Position(8, 4), new Position(10, 5))
                .forEach(pos -> elements.add(new Gear(pos, Gear.RotationDirection.COUNTERCLOCKWISE, boardId)));

        // Checkpoint
        final Position[] checkpointPositions = {new Position(11, 4), new Position(4, 5), new Position(8, 2), new Position(8, 7)};
        for (int i = 1; i <= 4; i++)
            elements.add(new CheckPoints(checkpointPositions[i - 1], i, boardId));

    }
}
