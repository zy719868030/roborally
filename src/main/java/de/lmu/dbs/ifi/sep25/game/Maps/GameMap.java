package de.lmu.dbs.ifi.sep25.game.Maps;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.List;
import java.util.Map;

public interface GameMap {
        List<BoardElement> getElements(); // Returns all board elements
        Map<String, Board.SubBoard> getSubBoards(); // Matches Board’s subBoards
        Position getAntennaPosition();
}

