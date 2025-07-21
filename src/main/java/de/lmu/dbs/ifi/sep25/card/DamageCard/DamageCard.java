package de.lmu.dbs.ifi.sep25.card.DamageCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.*;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Reboot;

import java.util.List;

/**
 * Represents a damage card that is executed during the robot’s register phase.
 * There are four types of damage cards, each with a distinct in-game effect.
 */
public class DamageCard extends Card implements RegisterCard{
    /**
     * The different kinds of damage that can be inflicted by a DamageCard.
     */
    public enum DamageType {
        /** Simple damage with no additional effect. */
        SPAM,
        /** Forces the robot to reboot immediately. */
        WORM,
        /** Spreads additional SPAM cards to nearby robots. */
        VIRUS,
        /** Grants extra SPAM cards to the robot immediately. */
        TROJAN_HORSE
    }

    /** The type of this damage card. */
    private DamageType damageType;

    /**
     * Constructs a new DamageCard with the given description and damage type.
     *
     * @param description a textual description of the card’s effect
     * @param damageType  the type of damage this card represents
     */
    public DamageCard(String description, DamageType damageType) {
        super(description, CardType.DAMAGE);
        this.damageType = damageType;
    }


    /**
     * Returns the type of this damage card.
     *
     * @return the damage type of this card
     */
    public DamageType getDamageType() {
        return damageType;
    }

    /**
     * Executes the effect of this damage card on the specified robot (and its player).
     * <ul>
     *   <li><b>SPAM</b>: Logs simple damage, no extra effect.</li>
     *   <li><b>WORM</b>: Logs and forces an immediate reboot via the board’s Reboot element.</li>
     *   <li><b>VIRUS</b>: Logs and calls {@link #spreadVirus(Robot)} to give SPAM cards to nearby robots.</li>
     *   <li><b>TROJAN_HORSE</b>: Logs and grants the robot two additional SPAM damage cards.</li>
     * </ul>
     * After executing the effect, this card is removed and replaced in the robot’s damage deck.
     *
     * @param robot  the robot on which to execute this damage card
     * @param player the player who owns the robot (currently unused, reserved for UI/notifications)
     */
    @Override
    public void execute(Robot robot, Player player) {
        switch (damageType) {
            case SPAM:
                // SPAM Card: Simple damage, no additional effects.
                System.out.println("Robot " + robot.getRobotID() + " executes SPAM damage card");
                break;

            case WORM:
                // WORM card: Restart the robot immediately.
                System.out.println("Robot " + robot.getRobotID() + " executes WORM - must reboot!");

                Reboot rebootElement = Game.getInstance().getBoard().getReboot();
                Board currentBoard = robot.getBoard();
                rebootElement.applyEffect(robot, currentBoard);
                break;

            case VIRUS:
                // VIRUS card: Send SPAM cards to all robots within a 6-square range.
                System.out.println("Robot " + robot.getRobotID() + " executes VIRUS - spreading damage!");
                spreadVirus(robot);
                break;

            case TROJAN_HORSE:
                // TROJAN HORSE Card: Immediately obtain two SPAM Damage Cards.
                System.out.println("Robot " + robot.getRobotID() + " executes TROJAN HORSE - taking extra damage!");
                robot.addDamageCard(new DamageCard("SPAM", DamageType.SPAM).getDamageType());
                robot.addDamageCard(new DamageCard("SPAM", DamageType.SPAM).getDamageType());
                break;
        }
        // After the damage card is executed, remove it from the deck and draw a new programming card.
        robot.replaceDamageCard();
    }

    /**
     * Helper method to implement the VIRUS effect:
     * gives a SPAM damage card to every other robot within a 6-square radius.
     *
     * @param robot the source robot from which the virus spreads
     */
    private void spreadVirus(Robot robot) {
        Board board = robot.getBoard();
        if (board != null) {
            Position virusSource = robot.getPosition();
            List<Robot> nearbyRobots = board.getRobotsInRange(virusSource, 6);
            for (Robot targetRobot : nearbyRobots) {
                if (targetRobot != robot) {
                    targetRobot.addDamageCard(new DamageCard("SPAM", DamageType.SPAM).getDamageType());
                    System.out.println("Robot " + targetRobot.getRobotID() + " infected by virus!");
                }
            }
        }
    }

    /**
     * Creates and returns a copy of this DamageCard.
     * Note: if new fields are added to this class, update this method accordingly.
     *
     * @return a new DamageCard instance with the same description and damage type
     */
    @Override
    public DamageCard clone() {
        return new DamageCard(this.description, this.damageType);
    }

    /**
     * Returns a string representation of this damage card.
     * The format is “<damageType>: <description>”.
     *
     * @return formatted string for this damage card
     */
    @Override
    public String toString() {
        return damageType.name() + ": " + description;
    }
}