package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Abstract base class for all programming cards that robots can register and execute.
 * Each ProgrammingCard encapsulates an action type, a description, and execution logic.
 */
public abstract class ProgrammingCard extends Card implements RegisterCard{
    /** String identifier for the type of action this card represents (e.g., "MOVE_FORWARD"). */
    protected String actionType;

    /**
     * Constructs a new ProgrammingCard.
     *
     * @param description textual description of what the card does
     * @param actionType  identifier for the specific action this card triggers
     */
    public ProgrammingCard(String description, String actionType) {
        super(description, CardType.PROGRAMMING);
        this.actionType = actionType;
    }

    /**
     * Returns the action type of this programming card.
     *
     * @return the action type string
     */
    public String getActionType() {
        return actionType;
    }

    /**
     * Executes the effect of this programming card on the specified robot.
     * Subclasses must implement the concrete behavior.
     *
     * @param robot  the robot that will perform the action
     * @param player the player who owns the robot
     */
    @Override
    public abstract void execute(Robot robot, Player player);

    /**
     * Creates and returns a deep copy of this programming card.
     * Subclasses must override to return an instance of their concrete type.
     *
     * @return a cloned copy of this ProgrammingCard
     */
    @Override
    public abstract ProgrammingCard clone();

    /**
     * Returns this card's priority for sequencing during the register phase.
     * Higher values execute earlier. Subclasses may override to assign specific priorities.
     *
     * @return the execution priority, default is 0
     */
    public int getPriority() {
        return 0;
    }

    /**
     * Determines whether this card can currently be executed by the given robot.
     * By default, cards cannot execute if the robot is powered down.
     * Subclasses may add further preconditions.
     *
     * @param robot the robot whose state is being checked
     * @return {@code true} if execution is allowed, {@code false} otherwise
     */
    public boolean canExecute(Robot robot) {
        return !robot.isPoweredDown();
    }
}