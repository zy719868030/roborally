package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class ProgrammingCard extends Card implements RegisterCard{
    protected String actionType;

    public ProgrammingCard(String description, String actionType) {
        super(description, CardType.PROGRAMMING);
        this.actionType = actionType;
    }

    public String getActionType() {
        return actionType;
    }

    @Override
    public abstract void execute(Robot robot);

    @Override
    public abstract ProgrammingCard clone();

    //Default priority, subclasses can override
    public int getPriority() {
        return 0;
    }

    // Check whether the card can be executed in the current state.
    public boolean canExecute(Robot robot) {
        return !robot.isPoweredDown();
    }
}