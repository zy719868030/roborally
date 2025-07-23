package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.List;

public class PathfindingBotStrategy implements BotStrategy {
    @Override
    public List<String> chooseRegisterCards(List<String> hand, Object boardState) {
        // TODO: Implement real pathfinding logic!
        // For now, fallback to random or simple logic:
        return new RandomBotStrategy().chooseRegisterCards(hand, boardState);
    }

    @Override
    public Position chooseStartingPoint(List<Position> available) {
        // TODO: Pick best starting point (e.g., closest to checkpoint)
        return available.get(0);
    }

    @Override
    public String chooseRebootDirection(Object boardState) {
        // TODO: Decide best direction based on position/goals
        return "top";
    }
}