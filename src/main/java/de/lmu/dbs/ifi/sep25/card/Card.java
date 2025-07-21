package de.lmu.dbs.ifi.sep25.card;

import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Abstract base class for all card types in the SEP25 game.
 * <p>
 * A Card has a textual description and a {@link CardType}, and defines
 * an abstract {@link #execute(Robot, Player)} method to apply its effect
 * during gameplay. Subclasses must implement cloning to allow safe copying.
 * </p>
 */
public abstract class Card implements Cloneable {

    /**
     * Enumeration of the different categories of cards.
     */
    public enum CardType {
        /** Cards that encode robot programming actions. */
        PROGRAMMING,
        /** Cards that grant permanent upgrades to robots. */
        UPGRADE,
        /** Cards representing damage effects applied to robots. */
        DAMAGE
    }

    /** Human‐readable description of what this card does. */
    protected String description;
    /** The category/type of this card. */
    protected CardType type;

    /**
     * Constructs a new Card with the given description and type.
     *
     * @param description textual description of the card’s effect
     * @param type        the {@link CardType} category of this card
     */
    public Card(String description, CardType type) {
        this.description = description;
        this.type = type;
    }

    /**
     * Returns the description of this card.
     *
     * @return the card’s description string
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the category/type of this card.
     *
     * @return the {@link CardType} of this card
     */
    public CardType getType() {
        return type;
    }

    /**
     * Executes the effect of this card on the specified robot and player.
     * <p>
     * Concrete subclasses should implement this to apply their unique
     * behavior during the register or game phases.
     * </p>
     *
     * @param robot  the {@link Robot} instance on which to apply the card
     * @param player the {@link Player} who controls the robot
     */
    public abstract void execute(Robot robot, Player player );

    /**
     * Creates and returns a deep copy of this card.
     * <p>
     * Subclasses must override to clone any additional fields.
     * </p>
     *
     * @return a new {@link Card} instance with identical state
     */
    @Override
    public abstract Card clone();

    /**
     * Returns a string representation of this card.
     * By default, returns its description.
     *
     * @return the description of the card
     */
    @Override
    public String toString() {
        return description;
    }
}