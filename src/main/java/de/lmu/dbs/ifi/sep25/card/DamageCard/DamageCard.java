package de.lmu.dbs.ifi.sep25.card.DamageCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Reboot;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.List;

public class DamageCard extends Card implements RegisterCard{
    public enum DamageType {
        SPAM,
        WORM,
        VIRUS,
        TROJAN_HORSE
    }

    private DamageType damageType;

    public DamageCard(String description, DamageType damageType) {
        super(description, CardType.DAMAGE);
        this.damageType = damageType;
    }

    public DamageType getDamageType() {
        return damageType;
    }

    // Execute when damage is programmed in the register
    @Override
    public void execute(Robot robot, Player player) {
        switch (damageType) {
            case SPAM:
                // SPAM Card: Simple damage, no additional effects.
                System.out.println("Robot " + robot.getId() + " executes SPAM damage card");
                break;

            case WORM:
                // WORM card: Restart the robot immediately.
                System.out.println("Robot " + robot.getId() + " executes WORM - must reboot!");

                Reboot rebootElement = Reboot.getInstance();
                Board currentBoard = robot.getBoard();
                rebootElement.applyEffect(robot, currentBoard);
                break;

            case VIRUS:
                // VIRUS card: Send SPAM cards to all robots within a 6-square range.
                System.out.println("Robot " + robot.getId() + " executes VIRUS - spreading damage!");
                spreadVirus(robot);
                break;

            case TROJAN_HORSE:
                // TROJAN HORSE Card: Immediately obtain two SPAM Damage Cards.
                System.out.println("Robot " + robot.getId() + " executes TROJAN HORSE - taking extra damage!");
                robot.addDamageCard(new DamageCard("SPAM", DamageType.SPAM).getDamageType());
                robot.addDamageCard(new DamageCard("SPAM", DamageType.SPAM).getDamageType());
                break;
        }
        // After the damage card is executed, remove it from the deck and draw a new programming card.
        robot.replaceDamageCard();
    }

    // The spread of VIRUS cards
    private void spreadVirus(Robot robot) {
        Board board = robot.getBoard();
        if (board != null) {
            Position virusSource = robot.getPosition();
            List<Robot> nearbyRobots = board.getRobotsInRange(virusSource, 6);
            for (Robot targetRobot : nearbyRobots) {
                if (targetRobot != robot) {
                    targetRobot.addDamageCard(new DamageCard("SPAM", DamageType.SPAM).getDamageType());
                    System.out.println("Robot " + targetRobot.getId() + " infected by virus!");
                }
            }
        }
    }

    @Override
    public DamageCard clone() {
        return new DamageCard(this.description, this.damageType);
    }

    @Override
    public String toString() {
        return damageType.name() + ": " + description;
    }
}