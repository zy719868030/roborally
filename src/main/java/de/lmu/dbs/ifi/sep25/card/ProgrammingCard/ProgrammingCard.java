package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class ProgrammingCard extends Card {
    protected String actionType;

    public ProgrammingCard(String description, String actionType) {
        super(description, CardType.PROGRAMMING);
        this.actionType = actionType;
    }

    public String getActionType() {
        return actionType;
    }

    @Override
    public abstract ProgrammingCard clone();
}