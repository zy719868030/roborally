package de.lmu.dbs.ifi.sep25.card;

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

    public abstract void execute(Robot robot);

    @Override
    public abstract Card clone();
}