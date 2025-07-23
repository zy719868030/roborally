package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.List;

/**
 * Defines the decision-making interface for RoboRally bot strategies.
 * <p>
 * Implementations of this interface provide automated logic for all game actions
 * that require player input, allowing bots to participate as clients in the game.
 * </p>
 */
public interface BotStrategy {

    /**
     * Selects a username.
     **/
    String chooseName();

    /**
     * Selects a starting point from the available positions on the board.
     *
     * @param available a list of available starting positions
     * @return the {@link Position} chosen as the starting point
     */
    Position chooseStartingPoint(List<Position> available);

    /**
     * Decides which cards to place in the programming registers for this round.
     *
     * @param hand       the current list of cards in hand (card names or objects)
     * @param boardState the current state of the board (positions, goals, etc.), if needed for strategy
     * @return a list of card names (or objects) to be placed in registers 0–4, in order
     */
    List<String> chooseRegisterCards(List<String> hand, BotGameState boardState);

    /**
     * Determines which direction the robot should face when rebooting.
     *
     * @param boardState the current state of the board, if needed for decision-making
     * @return the chosen reboot direction (e.g., "top", "right", "left", or "bottom")
     */
    String chooseRebootDirection(Object boardState);

    /**
     * Chooses which damage cards to pick from the available piles when prompted by the server.
     *
     * @param count          the number of damage cards that must be selected
     * @param availablePiles the list of available damage card piles or types to choose from
     * @return a list containing the names or identifiers of the selected damage cards
     */
    List<String> chooseDamageCards(int count, List<String> availablePiles);

}

