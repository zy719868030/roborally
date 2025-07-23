package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public final static int DEFAULT_DECISION_DELAY_SECONDS = 3;

    /**
     * Constructs a new bot client using the specified strategy.
     *
     * @param strategy the {@link BotStrategy} instance providing decision logic for all actions
     */
    public BotClient(BotStrategy strategy) {
        super(true); // Always constructed as AI
        this.strategy = strategy;
    }

    /**
     * Schedules the given decision logic to run after the specified delay in seconds,
     * simulating human-like reaction time for all bot actions.
     * <p>
     * This method ensures the provided {@code Runnable} is executed on a background thread
     * after the specified number of seconds. Typical usage is to wrap all server-response actions
     * (such as card selection, starting position selection, reboot direction, etc.) for realism.
     * </p>
     *
     * @param delaySeconds   the delay before executing the logic, in seconds
     * @param decisionLogic  the logic to execute after the delay (e.g., sending a move to the server)
     */
    protected void delayedDecision(int delaySeconds, Runnable decisionLogic) {
        scheduler.schedule(decisionLogic, delaySeconds, TimeUnit.SECONDS);
    }

    /**
     * Schedules the given decision logic to run after the default decision delay,
     * simulating human-like reaction time for all bot actions.
     * <p>
     * This is a convenience overload that uses {@link #DEFAULT_DECISION_DELAY_SECONDS} as the delay value.
     * Typical usage is to wrap all server-response actions (such as card selection,
     * starting position selection, reboot direction, etc.) for realism.
     * </p>
     *
     * @param decisionLogic the logic to execute after the delay (e.g., sending a move to the server)
     * @see #delayedDecision(int, Runnable)
     */
    protected void delayedDecision(Runnable decisionLogic) {
        delayedDecision(DEFAULT_DECISION_DELAY_SECONDS, decisionLogic);
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
        delayedDecision(2, () -> {
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
        });
    }

    /**
     * Called when the bot receives its hand of cards and must program registers for the round.
     * The cards are selected using the bot's strategy and sent to the server.
     *
     * @param hand the current list of cards in hand
     */
    @Override
    protected void onYourCards(List<String> hand) {
        delayedDecision(() -> {
            List<String> selected = strategy.chooseRegisterCards(hand, new BotGameState(getMyRobotInfo(), getCheckpoints(), getCurrentGameMap()));
            for (int i = 0; i < selected.size(); i++) {
                String cardName = selected.get(i);
                sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySelectedCard(cardName, i)));
            }
        });
    }

    /**
     * Called when the bot's robot must choose a direction for rebooting.
     * The direction is chosen using the strategy and sent to the server.
     */
    @Override
    protected void onRebootDirectionRequest() {
        delayedDecision(2, () -> {
            String dir = strategy.chooseRebootDirection(getCurrentGameMap());
            sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyRebootDirection(dir)));
        });
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
        delayedDecision(2, () -> {
            List<String> chosen = strategy.chooseDamageCards(count, availablePiles);
            sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySelectedDamage(chosen)));
        });
    }

    /**
     * Releases resources associated with the client connection. Also closes scheduler.
     * <p>
     * This method ensures the proper closure of the input stream `reader`,
     * output stream `writer`, and the socket connection. It first checks if
     * each resource is non-null (or in the case of the socket, not already
     * closed), and closes them in sequence. If an error occurs during this
     * process, an error message is logged to the standard error stream.
     * <p>
     * This method is typically called to clean up resources when the client
     * disconnects or an issue occurs, ensuring no resource leaks.
     */
    @Override
    public void closeAll() {
        super.closeAll();
        scheduler.shutdown();
    }
}