package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class PathfindingBotStrategy implements BotStrategy {
    private static final AtomicInteger botCount = new AtomicInteger(1);

    @Override
    public String chooseName() {
        return "smartBot #" + botCount.getAndIncrement();
    }

    @Override
    public List<String> chooseRegisterCards(List<String> hand, Object boardState) {
        // TODO: Implement real pathfinding logic!
        // For now, fallback to random or simple logic:
        return new RandomBotStrategy().chooseRegisterCards(hand, boardState);
    }

    @Override
    public Position chooseStartingPoint(List<Position> available) {
        // TODO: Pick best starting point (e.g., closest to checkpoint)
        return available.getFirst();
    }

    @Override
    public String chooseRebootDirection(Object boardState) {
        // TODO: Decide best direction based on position/goals
        return "top";
    }

    @Override
    public List<String> chooseDamageCards(int count, List<String> availablePiles) {
        //TODO
        return new ArrayList<>();
    }
}