package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A bot strategy that makes random decisions for all required game actions.
 * <p>
 * This strategy selects cards, starting points, reboot directions, and damage cards randomly
 * from the available options. It maintains internal state to ensure that each starting position
 * is only tried once per selection phase.
 * </p>
 */
public class RandomBotStrategy implements BotStrategy {
    private final Random rnd = new Random();
    private Queue<Position> shuffledStartQueue = null;
    private static final AtomicInteger botCount = new AtomicInteger(1);

    /**
     * Called when the server requests the bot to select a player name and a figure (robot) for the game.
     * <p>
     * This method ensures the bot picks a unique name (generated only once) and tries to select an available figure.
     * It shuffles all possible figure IDs and attempts each one in random order, sending its choice to the server.
     * If a figure is already taken, the method will be called again to try the next available figure until success.
     * </p>
     * <p>
     * This prevents duplicate figure attempts and guarantees protocol compliance for automated clients.
     * </p>
     */
    @Override
    public String chooseName() {
        return "randomBot #" + botCount.getAndIncrement();
    }

    /**
     * Randomly selects a starting position from the available options,
     * ensuring each is tried only once per selection round by shuffling and tracking with a queue.
     *
     * @param available the list of available starting positions
     * @return the next randomly chosen starting {@link Position}, or {@code null} if none remain
     */
    @Override
    public Position chooseStartingPoint(List<Position> available) {
        if (shuffledStartQueue == null || shuffledStartQueue.isEmpty()) {
            List<Position> shuffled = new ArrayList<>(available);
            Collections.shuffle(shuffled, rnd);
            shuffledStartQueue = new LinkedList<>(shuffled);
        }
        return shuffledStartQueue.poll();
    }

    /**
     * Randomly selects up to five cards for the programming registers, avoiding "again" as the first card.
     *
     * @param hand       the list of cards currently in hand
     * @param boardState the current board state (unused in this strategy)
     * @return a list of up to five randomly selected cards for the registers
     */
    @Override
    public List<String> chooseRegisterCards(List<String> hand, Object boardState) {
        List<String> copy = new ArrayList<>(hand);
        do {
            Collections.shuffle(copy, rnd);
        } while (copy.getFirst().equalsIgnoreCase("again"));
        Collections.shuffle(copy, rnd);
        return copy.subList(0, Math.min(5, copy.size()));
    }

    /**
     * Randomly selects a direction for the robot to face after rebooting.
     *
     * @param boardState the current board state (unused in this strategy)
     * @return the chosen reboot direction as a string ("top", "bottom", "left", or "right")
     */
    @Override
    public String chooseRebootDirection(Object boardState) {
        final String[] dirs = {"top", "bottom", "left", "right"};
        return dirs[rnd.nextInt(dirs.length)];
    }

    /**
     * Randomly selects the required number of damage cards from the available piles.
     *
     * @param count          the number of damage cards to pick
     * @param availablePiles the list of available damage piles or card types
     * @return a list of randomly selected damage card names or identifiers
     */
    @Override
    public List<String> chooseDamageCards(int count, List<String> availablePiles) {
        List<String> copy = new ArrayList<>(availablePiles);
        Collections.shuffle(copy, rnd);
        return copy.subList(0, Math.min(count, copy.size()));
    }
}
