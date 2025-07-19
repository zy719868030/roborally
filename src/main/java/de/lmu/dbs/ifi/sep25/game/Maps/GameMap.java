package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class GameMap {
    protected final List<BoardElement> elements = new ArrayList<>();
    protected final Map<String, Board.SubBoard> subBoards = new HashMap<>();
    protected final Position antennaPosition;

    public GameMap(Position antennaPosition) {
        this.antennaPosition = antennaPosition;
    }
// No longer used. Kept for legacy reference.


    public static GameMap load(String mapName) {
        return switch (mapName) {
            case "Dizzy Highway" -> new DizzyHighway();
            case "Extra Crispy" -> new ExtraCrispy();
            case "Lost Bearings" -> new LostBearings();
            case "Death Trap" -> new DeathTrap();
            default -> throw new IllegalArgumentException("Unknown map name: " + mapName);
        };

    }


    public List<BoardElement> getElements() {
        return new ArrayList<>(elements);
    }

    public Map<String, Board.SubBoard> getSubBoards() {
        return new HashMap<>(subBoards);
    }

    public Position getAntennaPosition() {
        return antennaPosition;
    }

    public void initializeBoard(String boardId, Direction direction) {
        switch (direction) {
            case EAST -> {
                switch (boardId) {
                    case "StartA" -> {
                        // Start A (y: 0–2)
                        elements.add(new Wall(new Position(1, 2), Direction.NORTH, "StartA"));
                        elements.add(new Wall(new Position(2, 4), Direction.EAST, "StartA"));
                        elements.add(new Wall(new Position(2, 5), Direction.EAST, "StartA"));
                        elements.add(new Wall(new Position(1, 7), Direction.SOUTH, "StartA"));

                        elements.add(new Antenna(antennaPosition, Direction.EAST, "StartA"));

                        List.of(new Position(1, 1), new Position(0, 3), new Position(1, 4), new Position(1, 5), new Position(0, 6), new Position(1, 8)).forEach(pos -> elements.add(new StartPoint(pos, Direction.EAST, "StartA")));

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

                        List.of(new Position(11, 1), new Position(12, 3), new Position(11, 4), new Position(11, 5), new Position(11, 8), new Position(12, 6)).forEach(pos -> elements.add(new StartPoint(pos, Direction.WEST, "StartA")));

                        elements.add(new Belts(new Position(10, 9), Direction.WEST, Direction.EAST, Belts.BeltSpeed.SLOW, "StartA"));
                        elements.add(new Belts(new Position(10, 0), Direction.WEST, Direction.EAST, Belts.BeltSpeed.SLOW, "StartA"));
                    }
                    default -> throw new IllegalArgumentException("Unknown/Unimplemented board ID: " + boardId);
                }

            }
        }

    }

    abstract protected void initializeSubBoards();

    abstract protected void initializeElements();
}

