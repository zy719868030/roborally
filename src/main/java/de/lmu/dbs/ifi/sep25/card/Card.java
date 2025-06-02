package de.lmu.dbs.ifi.sep25.card;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class Card implements Cloneable {
    public enum CardType {
        PROGRAMMING,
        UPGRADE,
        DAMAGE
    }

    protected String description;
    protected CardType type;

    public Card(String description, CardType type) {
        this.description = description;
        this.type = type;
    }

    public String getDescription() {
        return description;
    }

    public CardType getType() {
        return type;
    }


    // Abstract Methods: Define roles for Robot in concrete subclasses.
    public abstract void execute(Robot robot);

    @Override
    // Abstract cloning methods: subclasses need to implement deep copies
    public abstract Card clone();
}