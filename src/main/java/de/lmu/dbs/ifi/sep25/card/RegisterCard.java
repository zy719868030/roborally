package de.lmu.dbs.ifi.sep25.card;

import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;

public interface RegisterCard {
    void execute(Robot robot, Player player);
}
