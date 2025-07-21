package de.lmu.dbs.ifi.sep25.card;

import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Marker interface for cards that can be registered in a robot’s program register.
 * <p>
 * RegisterCard implementations define actions or effects that are executed
 * on a {@link Robot} during the register phase of a turn.
 * </p>
 */
public interface RegisterCard {

    /**
     * Executes this card’s effect on the given robot and its controlling player.
     * <p>
     * Implementations should perform any necessary state changes, logging,
     * or interactions with the game board or player resources.
     * </p>
     *
     * @param robot  the {@link Robot} instance on which to apply this card
     * @param player the {@link Player} who controls the robot
     */
    void execute(Robot robot, Player player);
}
