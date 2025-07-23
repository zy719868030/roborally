package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.List;

public interface BotStrategy {
    /**
     * Decide which cards to place in the registers.
     *
     * @param hand       The current list of cards in hand (card names or objects).
     * @param boardState The current state of the board (positions, goals, etc), if needed.
     * @return A list of card names (or objects) for registers 0-4.
     */
    List<String> chooseRegisterCards(List<String> hand, Object boardState);

    /**
     * Choose a starting point from available positions.
     */
    Position chooseStartingPoint(List<Position> available);

    /**
     * Choose a reboot direction (e.g. "top", "right", etc).
     */
    String chooseRebootDirection(Object boardState);

}

