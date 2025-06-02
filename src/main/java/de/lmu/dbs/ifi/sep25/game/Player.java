package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private Robot robot;
    private List<Card> register;
    private List<UpgradeCard> upgrades;

    public Player(String name, int startX, int startY) {
        this.name = name;
        this.robot = new Robot(startX, startY, Direction.NORTH);
        this.register = new ArrayList<Card>(5); // Specify Card type
        this.upgrades = new ArrayList<UpgradeCard>(); // Specify UpgradeCard type
        for (int i = 0; i < 5; i++) {
            register.add(null);
        }
    }

    public void chooseCard(Card card, int slot) {
        if (slot >= 0 && slot < 5) {
            register.set(slot, card);
        }
    }

    public Robot getRobot() {
        return robot;
    }

    public List<Card> getRegister() {
        return register;
    }

    public String getName() {
        return name;
    }
}