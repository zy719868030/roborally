package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.Board.*;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import java.util.*;

public class DeathTrap implements GameMap {
    private final List<BoardElement> elements = new ArrayList<>();
    private final Map<String, Board.SubBoard> subBoards = new HashMap<>();
    private final Position antennaPosition;

    public DeathTrap() {
        // Initialize sub-boards
        subBoards.put("StartA", new Board.SubBoard("StartA", 0, 2)); // y=0–2 (same as DizzyHighway)
        subBoards.put("DT1", new Board.SubBoard("DT1", 3, 12)); // y=3–12

        // Initialize antenna position (matches Antenna element in StartA)
        antennaPosition = new Position(5, 0, "StartA");

        // Add StartA elements (identical to DizzyHighway and LostBearings)
        elements.add(new Antenna(new Position(5, 0), Direction.EAST, "StartA"));
        elements.add(new Wall(new Position(7, 1), Direction.NORTH, "StartA"));
        elements.add(new Wall(new Position(5, 2), Direction.EAST, "StartA"));
        elements.add(new Wall(new Position(4, 2), Direction.EAST, "StartA"));
        elements.add(new Wall(new Position(2, 1), Direction.SOUTH, "StartA"));
        Position[] startPositions = {
                new Position(8, 1), new Position(6, 0), new Position(5, 1),
                new Position(4, 1), new Position(3, 0), new Position(1, 1)
        };
        for (Position pos : startPositions) {
            elements.add(new StartPoint(pos, Direction.EAST, "StartA"));
        }

        // Add DT1 elements (y=3–12)
        for (int y = 3; y <= 7; y++) {
            elements.add(new Belts(new Position(6, y), Direction.WEST, Belts.BeltSpeed.SLOW, "DT1"));
        }
        List<Direction> outDirs = new ArrayList<>();
        outDirs.add(Direction.SOUTH);
        List<Direction> inDirs = new ArrayList<>();
        inDirs.add(Direction.EAST);
        elements.add(new Belts(new Position(5, 7), outDirs, inDirs, Belts.BeltSpeed.SLOW, "DT1"));
        elements.add(new CheckPoints(new Position(6, 8), 1, "DT1"));
        elements.add(new Laser(new Position(7, 6), Direction.NORTH, 1, "DT1"));
        elements.add(new Pit(new Position(5, 9), "DT1"));
        elements.add(new EnergySpace(new Position(6, 10), "DT1"));
        elements.add(new Gear(new Position(7, 5), Gear.RotationDirection.CLOCKWISE, "DT1"));
        Reboot reboot = Reboot.getInstance();
        reboot.setPosition(new Position(6, 11));
        reboot.setBoardId("DT1");
        elements.add(reboot);
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