package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class RandomBotStrategy implements BotStrategy {
    private final Random rnd = new Random();

    @Override
    public List<String> chooseRegisterCards(List<String> hand, Object boardState) {
        List<String> copy = new ArrayList<>(hand);
        do {
            Collections.shuffle(copy, rnd);
        } while (copy.getFirst().equalsIgnoreCase("again"));
        Collections.shuffle(copy, rnd);
        return copy.subList(0, Math.min(5, copy.size()));
    }

    @Override
    public Position chooseStartingPoint(List<Position> available) {
        return available.get(rnd.nextInt(available.size()));
    }

    @Override
    public String chooseRebootDirection(Object boardState) {
        String[] dirs = {"top", "bottom", "left", "right"};
        return dirs[rnd.nextInt(dirs.length)];
    }
}