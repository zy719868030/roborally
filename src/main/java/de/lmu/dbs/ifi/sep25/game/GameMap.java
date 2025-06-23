package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import java.util.List;
import java.util.Map;

public interface GameMap {
        List<BoardElement> getElements(); // Returns all board elements
        Map<String, Board.SubBoard> getSubBoards(); // Matches Board’s subBoards
        Position getAntennaPosition();
}

