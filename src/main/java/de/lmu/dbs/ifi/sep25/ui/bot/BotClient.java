package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.*;

/**
 * A headless client that participates in a RoboRally game as a bot, using a provided {@link BotStrategy}.
 * <p>
 * This client automatically responds to all game decisions using the chosen strategy and never displays a UI.
 * It is intended for automated gameplay, testing, and AI matches.
 * </p>
 */
public class BotClient extends Client {
    private final BotStrategy strategy;
    private String botName;
    private Queue<Integer> figureQueue = null;

    /**
     * Constructs a new bot client using the specified strategy.
     *
     * @param strategy the {@link BotStrategy} instance providing decision logic for all actions
     */
    public BotClient(BotStrategy strategy) {
        super(true); // Always constructed as AI
        this.strategy = strategy;
    }

    @Override
    protected void onSelectNameAndFigure() {
        if (botName == null) botName = strategy.chooseName();
        if (figureQueue == null || figureQueue.isEmpty()) {
            List<Integer> shuffled = new ArrayList<>(List.of(0, 1, 2, 3, 4, 5));
            Collections.shuffle(shuffled, new Random());
            figureQueue = new LinkedList<>(shuffled);
        }
        Integer nextFigure = figureQueue.poll();
        if (nextFigure != null) {
            sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyPlayerValues(botName, nextFigure)
            ));
        }
    }

    /**
     * Called when the bot must choose a starting position.
     * The position is selected by the strategy and sent to the server, along with a default facing direction for the chosen map.
     *
     * @param available the list of currently available starting positions
     */
    @Override
    protected void onSelectStartingPoint(List<Position> available) {
        Position choice = strategy.chooseStartingPoint(available);
        if (choice != null) {
            sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodySetStartingPoint(
                            choice.x(), choice.y(),
                            switch (getSelectedMap()) {
                                case "Heavy Merge Area", "Death Trap" -> "left";
                                case "Pilgrimage", "Gear Stripper" -> "top";
                                default -> "right";
                            }
                    )));
        }
    }

    /**
     * Called when the bot receives its hand of cards and must program registers for the round.
     * The cards are selected using the bot's strategy and sent to the server.
     *
     * @param hand the current list of cards in hand
     */
    @Override
    protected void onYourCards(List<String> hand) {
        List<String> selected = strategy.chooseRegisterCards(hand, getCurrentGameMap());
        for (int i = 0; i < selected.size(); i++) {
            String cardName = selected.get(i);
            sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySelectedCard(cardName, i)));
        }
    }

    /**
     * Called when the bot's robot must choose a direction for rebooting.
     * The direction is chosen using the strategy and sent to the server.
     */
    @Override
    protected void onRebootDirectionRequest() {
        String dir = strategy.chooseRebootDirection(getCurrentGameMap());
        sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyRebootDirection(dir)));
    }

    /**
     * Called when the bot must select damage cards from the available options.
     * The chosen cards are selected using the strategy and sent to the server.
     *
     * @param count          the number of damage cards to pick
     * @param availablePiles the available piles or card types to choose from
     */
    @Override
    protected void onPickDamage(int count, List<String> availablePiles) {
        List<String> chosen = strategy.chooseDamageCards(count, availablePiles);
        sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySelectedDamage(chosen)));
    }
}