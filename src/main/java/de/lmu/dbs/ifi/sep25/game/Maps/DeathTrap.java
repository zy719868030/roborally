package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class DeathTrap extends GameMap {
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(DeathTrap.class);
    private final String boardId = "2A";

    public DeathTrap() {
        super(new Position(12, 5));

        initializeSubBoards();
        initializeBoard("StartA", Direction.WEST);
        initializeElements();
    }

    protected void initializeSubBoards() {
        subBoards.put("StartA", new Board.SubBoard("StartA", 10, 12));
        subBoards.put(boardId, new Board.SubBoard(boardId, 0, 9));
    }

    protected void initializeElements() {
        // Reboot
        elements.add(new Reboot(new Position(12, 9), Direction.WEST, "StartA"));

        // Checkpoint
        final Position[] checkpointPositions = {new Position(1, 7), new Position(4, 4), new Position(7, 8),
                new Position(8, 2), new Position(0, 1)};
        for (int i = 1; i <= 5; i++)
            elements.add(new CheckPoints(checkpointPositions[i - 1], i, boardId));

        // Add 2A elements

        // Pit
        List.of(new Position(1, 2), new Position(3, 2), new Position(7, 1),
                new Position(7, 3), new Position(3, 4), new Position(6, 5),
                new Position(2, 6), new Position(2, 8), new Position(6, 7),
                new Position(8, 7)).forEach(pos -> elements.add(new Pit(pos, boardId)));

        // EnergySpaces
        List.of(new Position(2, 3), new Position(4, 2), new Position(6, 2),
                        new Position(3, 7), new Position(4, 6), new Position(7, 6))
                .forEach(pos -> elements.add(new EnergySpace(pos, boardId)));

        // Walls
        List.of(new Position(1, 1), new Position(7, 2), new Position(6, 4),
                        new Position(4, 5), new Position(6, 8))
                .forEach(pos -> elements.add(new Wall(pos, Direction.NORTH, boardId)));

        List.of(new Position(8, 1), new Position(1, 6), new Position(7, 7))
                .forEach(pos -> elements.add(new Wall(pos, Direction.EAST, boardId)));

        List.of(new Position(3, 1), new Position(4, 3), new Position(3, 5),
                        new Position(5, 5), new Position(2, 7), new Position(8, 8))
                .forEach(pos -> elements.add(new Wall(pos, Direction.SOUTH, boardId)));

        List.of(new Position(2, 2), new Position(1, 8), new Position(8, 3))
                .forEach(pos -> elements.add(new Wall(pos, Direction.WEST, boardId)));

        // ConveyorBelt
        for (int x = 1; x <= 3; x++)
            elements.add(new Belts(new Position(x, 0), Direction.WEST, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId));

        elements.add(new Belts(new Position(4, 0), Direction.WEST, List.of(Direction.SOUTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(4, 1), Direction.NORTH, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(5, 1), Direction.WEST, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId));

        for (int y = 8; y >= 6; y--)
            elements.add(new Belts(new Position(0, y), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(0, 5), Direction.SOUTH, List.of(Direction.EAST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(1, 5), Direction.WEST, List.of(Direction.NORTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(1, 4), Direction.SOUTH, List.of(Direction.NORTH), Belts.BeltSpeed.SLOW, boardId));


        for (int x = 6; x <= 8; x++)
            elements.add(new Belts(new Position(x, 9), Direction.EAST, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(5, 9), Direction.EAST, List.of(Direction.NORTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(5, 8), Direction.SOUTH, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(4, 8), Direction.EAST, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId));

        for (int y = 1; y <= 3; y++)
            elements.add(new Belts(new Position(9, y), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(9, 4), Direction.NORTH, List.of(Direction.WEST), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(8, 4), Direction.EAST, List.of(Direction.SOUTH), Belts.BeltSpeed.SLOW, boardId));
        elements.add(new Belts(new Position(8, 5), Direction.NORTH, List.of(Direction.SOUTH), Belts.BeltSpeed.SLOW, boardId));


        // PushPanel
        elements.add(new PushPanel(new Position(1, 1), Direction.NORTH, List.of(1, 3, 5), boardId));
        elements.add(new PushPanel(new Position(8, 1), Direction.EAST, List.of(1, 3, 5), boardId));
        elements.add(new PushPanel(new Position(4, 3), Direction.SOUTH, List.of(1, 3, 5), boardId));
        elements.add(new PushPanel(new Position(6, 4), Direction.NORTH, List.of(1, 3, 5), boardId));
        elements.add(new PushPanel(new Position(1, 8), Direction.WEST, List.of(1, 3, 5), boardId));
        elements.add(new PushPanel(new Position(8, 8), Direction.SOUTH, List.of(1, 3, 5), boardId));
        elements.add(new PushPanel(new Position(3, 5), Direction.SOUTH, List.of(1, 3, 5), boardId));

        elements.add(new PushPanel(new Position(2, 2), Direction.WEST, List.of(2, 4), boardId));
        elements.add(new PushPanel(new Position(7, 2), Direction.NORTH, List.of(2, 4), boardId));
        elements.add(new PushPanel(new Position(4, 5), Direction.NORTH, List.of(2, 4), boardId));
        elements.add(new PushPanel(new Position(2, 7), Direction.SOUTH, List.of(2, 4), boardId));
        elements.add(new PushPanel(new Position(7, 7), Direction.EAST, List.of(2, 4), boardId));

    }
}