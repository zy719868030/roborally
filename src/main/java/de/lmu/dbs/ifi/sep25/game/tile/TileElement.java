package de.lmu.dbs.ifi.sep25.game.tile;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class TileElement {
    public abstract void applyEffect(Robot robot, Board board);
}
