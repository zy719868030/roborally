package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.ui.bot.pathfinding.BoardGraph;
import de.lmu.dbs.ifi.sep25.ui.bot.pathfinding.Pathfinder;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class PathfindingBotStrategy implements BotStrategy {
    private static final AtomicInteger botCount = new AtomicInteger(1);

    @Override
    public String chooseName() {
        return "BFSBot #" + botCount.getAndIncrement();
    }

    @Override
    public List<String> chooseRegisterCards(List<String> hand, BotGameState state) {
        RobotInfo me = state.me();
        List<Position> checkpoints = state.checkpoints();
        List<List<List<MessageDefinitions.Field>>> map = state.boardMap();

        if (me == null || checkpoints == null || checkpoints.isEmpty() || map == null) {
            // Fallback to random if info missing
            return new RandomBotStrategy().chooseRegisterCards(hand, state);
        }

        // Build graph once
        BoardGraph graph = new BoardGraph(map);

        // Compute path to next checkpoint
        Position start = me.position();
        int nextCheckpointIdx = Math.min(me.checkpointsReached(), checkpoints.size() - 1);
        Position goal = checkpoints.get(nextCheckpointIdx);

        List<Direction> path = Pathfinder.dijkstra(graph, start, goal);

        List<String> registers = new ArrayList<>(5);
        Direction myDir = me.direction();
        int pathIdx = 0;

        for (int reg = 0; reg < 5; reg++) {
            final int registerIdx = reg;
            String nextCard;

            if (pathIdx < path.size()) {
                Direction needed = path.get(pathIdx);

                if (myDir == needed) {
                    nextCard = hand.stream()
                            .filter(card -> (!"Again".equals(card)) && card.startsWith("Move"))
                            .findFirst()
                            .orElseGet(() ->
                                    registerIdx == 0
                                            ? hand.stream().filter(card -> !"Again".equals(card)).findFirst().orElse(hand.getFirst())
                                            : hand.get(new Random().nextInt(hand.size()))
                            );
                    pathIdx++;
                } else {
                    nextCard = hand.stream()
                            .filter(card -> (!"Again".equals(card)) &&
                                    (card.startsWith("Turn") || card.equals("UTurn")))
                            .findFirst()
                            .orElseGet(() ->
                                    registerIdx == 0
                                            ? hand.stream().filter(card -> !"Again".equals(card)).findFirst().orElse(hand.getFirst())
                                            : hand.get(new Random().nextInt(hand.size()))
                            );
                    myDir = simulateTurn(myDir, needed);
                }
            } else {
                if (registerIdx == 0) {
                    nextCard = hand.stream().filter(card -> !"Again".equals(card)).findFirst().orElse(hand.getFirst());
                } else {
                    nextCard = hand.get(new Random().nextInt(hand.size()));
                }
            }
            registers.add(nextCard);
        }

        return registers;
    }

    /**
     * Simulates the direction you would face after playing a turn card to face 'targetDir' from 'currentDir'.
     * This logic assumes you always pick the minimal turn needed.
     */
    private Direction simulateTurn(Direction currentDir, Direction targetDir) {
        int diff = (targetDir.ordinal() - currentDir.ordinal() + 4) % 4;
        if (diff == 1) { // right turn
            return currentDir.turnRight();
        } else if (diff == 3) { // left turn
            return currentDir.turnLeft();
        } else if (diff == 2) { // UTurn
            return currentDir.turnRight().turnRight();
        } else { // already facing, or can't turn
            return targetDir;
        }
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