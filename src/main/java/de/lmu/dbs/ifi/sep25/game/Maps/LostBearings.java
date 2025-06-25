package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LostBearings implements GameMap {
    private final List<BoardElement> elements = new ArrayList<>();
    private final Map<String, Board.SubBoard> subBoards = new HashMap<>();
    private final Position antennaPosition;

    public LostBearings() {
        // Initialize sub-boards
        subBoards.put("StartA", new Board.SubBoard("StartA", 0, 2)); // Reuse StartA from Dizzy Highway (y=0–2)
        subBoards.put("1A", new Board.SubBoard("1A", 3, 12));   // New 1A sub-board (y=3–12)

        // Initialize antenna (example position, adjust as needed)
        antennaPosition = new Position(4, 3);

        // Add StartA elements (copied from Dizzy Highway’s StartA setup)
        // Example: reuse antenna, belts, etc. from Dizzy Highway’s StartA
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

        for (int x = 2; x <= 8; x++) {
            elements.add(new Belts(new Position(x, 4), Direction.EAST, Belts.BeltSpeed.SLOW, "1A"));
        }

        elements.add(new CheckPoints(new Position(5, 6), 1, "1A")); // Example checkpoint
        // Add more elements (lasers, pits, etc.) based on Lost Bearings specs
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
