package de.lmu.dbs.ifi.sep25.game;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private String name;
    private Robot robot;
    private List<Card> register;
    private List<UpgradeCard> upgrades;

    public Player(String name, int startX, int startY) {
        this.name = name;
        this.robot = new Robot(startX, startY, "north");
        this.register = new ArrayList<>(5);
        this.upgrades = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            register.add(null); // 5 slots for program cards
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