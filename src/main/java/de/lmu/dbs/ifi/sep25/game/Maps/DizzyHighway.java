package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DizzyHighway implements GameMap {
    private final List<BoardElement> elements = new ArrayList<>();
    private Position antennaPosition;
    private final Map<String, Board.SubBoard> subBoards = new HashMap<>();

    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(DizzyHighway.class);

    public DizzyHighway() {
        initializeSubBoards();
        initializeElements();
    }

    private void initializeSubBoards() {
        subBoards.put("StartA", new Board.SubBoard("StartA", 0, 2));
        subBoards.put("5B", new Board.SubBoard("5B", 3, 12));
    }

    private void initializeElements() {
        // Start A (y: 0–2)
        elements.add(new Wall(new Position(1, 2), Direction.NORTH, "StartA"));
        elements.add(new Wall(new Position(2, 4), Direction.EAST, "StartA"));
        elements.add(new Wall(new Position(2, 5), Direction.EAST, "StartA"));
        elements.add(new Wall(new Position(1, 7), Direction.SOUTH, "StartA"));

        antennaPosition = new Position(0, 4);
        elements.add(new Antenna(antennaPosition, Direction.EAST, "StartA"));

        List.of(new Position(1, 1), new Position(0, 3), new Position(1, 4),
                new Position(1, 5), new Position(0, 6), new Position(1, 8)
        ).forEach(pos -> elements.add(new StartPoint(pos, Direction.EAST, "StartA")));

        elements.add(new Belts(new Position(2, 9), Direction.EAST, Direction.WEST, Belts.BeltSpeed.SLOW, "StartA"));
        elements.add(new Belts(new Position(2, 0), Direction.EAST, Direction.WEST, Belts.BeltSpeed.SLOW, "StartA"));

        // 5B (y: 3–12)
        // Belts
        elements.add(new Belts(new Position(3, 7), Direction.EAST, Direction.WEST, Belts.BeltSpeed.FAST, "5B"));
        elements.add(new Belts(new Position(10, 9), Direction.NORTH, Direction.SOUTH, Belts.BeltSpeed.FAST, "5B"));
        elements.add(new Belts(new Position(12, 2), Direction.WEST, Direction.EAST, Belts.BeltSpeed.FAST, "5B"));
        elements.add(new Belts(new Position(5, 0), Direction.SOUTH, Direction.NORTH, Belts.BeltSpeed.FAST, "5B"));

        // Column 4 belts
        for (int y = 0; y <= 8; y++) {
            switch (y) {
                case 1 ->
                        elements.add(new Belts(new Position(4, y), Direction.SOUTH, List.of(Direction.NORTH, Direction.EAST), Belts.BeltSpeed.FAST, "5B"));
                case 7 ->
                        elements.add(new Belts(new Position(4, y), Direction.SOUTH, List.of(Direction.NORTH, Direction.WEST), Belts.BeltSpeed.FAST, "5B"));
                case 8 ->
                        elements.add(new Belts(new Position(4, y), Direction.EAST, List.of(Direction.NORTH, Direction.WEST), Belts.BeltSpeed.FAST, "5B"));
                default ->
                        elements.add(new Belts(new Position(4, y), Direction.SOUTH, Direction.NORTH, Belts.BeltSpeed.FAST, "5B"));
            }
        }

        // Row 1 belts
        for (int x = 3; x <= 12; x++) {
            if (x != 4) {
                if (x == 10) {
                    elements.add(new Belts(new Position(x, 8), Direction.EAST, List.of(Direction.SOUTH, Direction.WEST), Belts.BeltSpeed.FAST, "5B"));
                } else if (x == 11) {
                    elements.add(new Belts(new Position(x, 8), Direction.NORTH, List.of(Direction.SOUTH, Direction.WEST), Belts.BeltSpeed.FAST, "5B"));
                } else {
                    elements.add(new Belts(new Position(x, 8), Direction.EAST, Direction.WEST, Belts.BeltSpeed.FAST, "5B"));
                }
            }
        }

        // Column 11 belts
        for (int y = 1; y <= 9; y++) {
            switch (y) {
                case 8 ->
                        elements.add(new Belts(new Position(11, y), Direction.NORTH, List.of(Direction.SOUTH, Direction.WEST), Belts.BeltSpeed.FAST, "5B"));
                case 2 ->
                        elements.add(new Belts(new Position(11, y), Direction.NORTH, List.of(Direction.SOUTH, Direction.EAST), Belts.BeltSpeed.FAST, "5B"));
                case 1 ->
                        elements.add(new Belts(new Position(11, y), Direction.WEST, List.of(Direction.SOUTH, Direction.EAST), Belts.BeltSpeed.FAST, "5B"));
                default ->
                        elements.add(new Belts(new Position(11, y), Direction.NORTH, Direction.SOUTH, Belts.BeltSpeed.FAST, "5B"));
            }
        }

        // Row 8 belts
        for (int x = 5; x <= 12; x++) {
            switch (x) {
                case 5 ->
                        elements.add(new Belts(new Position(x, 1), Direction.SOUTH, List.of(Direction.NORTH, Direction.EAST), Belts.BeltSpeed.FAST, "5B"));
                case 6 ->
                        elements.add(new Belts(new Position(x, 1), Direction.WEST, List.of(Direction.NORTH, Direction.EAST), Belts.BeltSpeed.FAST, "5B"));
                case 11 ->
                        elements.add(new Belts(new Position(x, 1), Direction.WEST, List.of(Direction.SOUTH, Direction.EAST), Belts.BeltSpeed.FAST, "5B"));
                default ->
                        elements.add(new Belts(new Position(x, 1), Direction.WEST, Direction.EAST, Belts.BeltSpeed.FAST, "5B"));
            }
        }

        // Energy Spaces
        List.of(new Position(3, 9), new Position(10, 7), new Position(12, 0),
                        new Position(5, 2), new Position(7, 5), new Position(8, 4))
                .forEach(pos -> elements.add(new EnergySpace(pos, "5B")));

        // Checkpoints
        logger.info("Adding Checkpoints to 5B");
        CheckPoints cp = new CheckPoints(new Position(12, 3), 1, "5B");
        elements.add(cp);
        logger.info("Added Checkpoints to 5B: " + cp);

        // Reboot
        Reboot reboot = Reboot.getInstance();
        reboot.setPosition(new Position(7, 3));
        reboot.setDirection(Direction.SOUTH);
        reboot.setBoardId("5B");
        elements.add(reboot);

        // Walls
        elements.add(new Wall(new Position(6, 4), Direction.SOUTH, "5B"));
        elements.add(new Wall(new Position(7, 6), Direction.EAST, "5B"));
        elements.add(new Wall(new Position(8, 3), Direction.WEST, "5B"));
        elements.add(new Wall(new Position(9, 5), Direction.NORTH, "5B"));

        elements.add(new Wall(new Position(6, 3), Direction.NORTH, "5B"));
        elements.add(new Wall(new Position(6, 6), Direction.WEST, "5B"));
        elements.add(new Wall(new Position(9, 3), Direction.EAST, "5B"));
        elements.add(new Wall(new Position(9, 6), Direction.SOUTH, "5B"));

        // Lasers
        elements.add(new Laser(new Position(6, 4), Direction.SOUTH, 1, "5B"));
        elements.add(new Laser(new Position(7, 6), Direction.EAST, 1, "5B"));
        elements.add(new Laser(new Position(8, 3), Direction.WEST, 1, "5B"));
        elements.add(new Laser(new Position(9, 5), Direction.NORTH, 1, "5B"));
    }

    @Override
    public List<BoardElement> getElements() {
        return new ArrayList<>(elements);
    }

    @Override
    public Map<String, Board.SubBoard> getSubBoards() {
        return new HashMap<>(subBoards);
    }

    @Override
    public Position getAntennaPosition() {
        return antennaPosition;
    }
}