package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExtraCrispy implements GameMap {
    private final List<BoardElement> elements = new ArrayList<>();
    private final Map<String, Board.SubBoard> subBoards = new HashMap<>();
    private final Position antennaPosition;

    public ExtraCrispy() {
        // Initialize sub-boards
        subBoards.put("StartA", new Board.SubBoard("StartA", 0, 2)); // y=0–2 (same as DizzyHighway)
        subBoards.put("EC1", new Board.SubBoard("EC1", 3, 12)); // y=3–12

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

        // Add EC1 elements (y=3–12)
        for (int x = 0; x <= 4; x++) {
            elements.add(new Belts(new Position(x, 4), Direction.EAST, Belts.BeltSpeed.FAST, "EC1"));
        }
        List<Direction> outDirs = new ArrayList<>();
        outDirs.add(Direction.NORTH);
        List<Direction> inDirs = new ArrayList<>();
        inDirs.add(Direction.WEST);
//        elements.add(new Belts(new Position(4, 5), outDirs, inDirs, Belts.BeltSpeed.FAST, "EC1"));
        elements.add(new CheckPoints(new Position(3, 6), 1, "EC1"));
        elements.add(new Laser(new Position(2, 5), Direction.SOUTH, 1, "EC1"));
        elements.add(new Pit(new Position(1, 7), "EC1"));
        elements.add(new EnergySpace(new Position(4, 8), "EC1"));
        elements.add(new Gear(new Position(2, 9), Gear.RotationDirection.COUNTERCLOCKWISE, "EC1"));
        Reboot reboot = Reboot.getInstance();
        reboot.setPosition(new Position(3, 10));
        reboot.setBoardId("EC1");
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