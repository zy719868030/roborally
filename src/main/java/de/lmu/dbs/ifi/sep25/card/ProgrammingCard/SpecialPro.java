package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Game;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;

public class SpecialPro extends ProgrammingCard {
    private String specialEffect;
    private static final Logger appLogger = LogManager.getLogger(SpecialPro.class);

    public SpecialPro(String description, String actionType, String specialEffect) {
        super(description, actionType);
        this.specialEffect = specialEffect;
    }

    public String getSpecialEffect() {
        return specialEffect;
    }

    @Override
    public void execute(Robot robot, Player player) {
        if (!canExecute(robot)) return;

        Board board = robot.getBoard();

        switch (specialEffect.toLowerCase()) {
            case "again":
                executeAgain(robot, board);
                break;
            case "energy routine":
                player.addEnergy(1, "Power Up");
                System.out.println("Robot " + robot.getRobotID() + " executes Energy Routine - gained 1 energy");
                break;
            case "speed routine":
                if (board != null) {
                    robot.applyMove(board, 3);
                    System.out.println("Robot " + robot.getRobotID() + " executes Speed Routine - moved 3 spaces");
                }
                break;
            case "repeat routine":
                executeAgain(robot, board);
                break;
            case "sandbox routine":
                executeSandboxChoice(robot, board);
                break;
            case "weasel routine":
                executeWeaselChoice(robot);
                break;
            case "spam folder":
                System.out.println("Robot " + robot.getRobotID() + " executes SPAM Folder - removes SPAM damage");
                break;
            default:
                System.out.println("Robot " + robot.getRobotID() + " executes unknown special effect: " + specialEffect);
        }
    }

    // Again card: Repeat the action of the previous register.
    private void executeAgain(Robot robot, Board board) {
        Player player = findPlayerByRobot(robot);
        if (player == null) {
            appLogger.error("Cannot find player for robot {}", robot.getRobotID());
            return;
        }

        Game game = Game.getInstance();
        int currentRegister = game.getCurrentRegister();

        // Find the actual card to execute by recursively looking back
        RegisterCard cardToExecute = findPreviousNonAgainCard(player, currentRegister);

        if (cardToExecute == null) {
            appLogger.warn("No valid card found for Again card execution");
            return;
        }

        // Execute the found card
        appLogger.info("Robot {} executes Again - repeating {}",
                robot.getRobotID(), CardFactory.getCardName(cardToExecute));

        if (cardToExecute instanceof DamageCard) {
            // For damage cards, draw a new card instead
            RegisterCard newCard = player.drawCard();
            appLogger.info("Previous card was damage card, drawing new card: {}",
                    CardFactory.getCardName(newCard));
            if (newCard != null) {
                newCard.execute(robot, player);
            }
        } else {
            // Execute the card normally
            cardToExecute.execute(robot, player);
        }

        // TODO Here, we need to retrieve the card from the previous register and re-execute it.
        // Simplified implementation: Assume that the Robot class has a getPreviousCard() method.
        System.out.println("Robot " + robot.getRobotID() + " executes Again - repeating previous action");
        // TODO: Actual need to access the programming history of the robot
        // Card previousCard = robot.getPreviousCard();
        // if (previousCard != null) {
        //     previousCard.execute(robot);
        // }
    }

    // Recursively find the first non-Again card in previous registers
    private RegisterCard findPreviousNonAgainCard(Player player, int currentRegister) {
        // Start from the previous register
        for (int i = currentRegister - 1; i >= 0; i--) {
            RegisterCard card = player.getRegisterCard(i);
            if (card == null) {
                continue;
            }

            // Check if it's an Again card
            String cardName = CardFactory.getCardName(card);
            if (!"Again".equals(cardName) && !"RepeatRoutine".equals(cardName)) {
                // Found a non-Again card
                return card;
            }
        }

        // No valid card found in previous registers
        return null;
    }

    // Helper method to find player by robot
    private Player findPlayerByRobot(Robot robot) {
        Game game = Game.getInstance();
        for (Player player : game.getPlayers()) {
            if (player.getRobot() == robot) {
                return player;
            }
        }
        return null;
    }

    // Sandbox Routine: Actions that players can choose to perform
    private void executeSandboxChoice(Robot robot, Board board) {
        // TODO In the actual game, players should be allowed to choose actions here.
        // Simplified implementation: randomly select a basic action.
        System.out.println("Robot " + robot.getRobotID() + " executes Sandbox Routine");
        // Move 1 can be executed by default here
        // TODO UI interaction may be required to allow the player to choose.
        robot.applyMove(board, 1);
    }

    // Weasel Routine: Select turning action
    private void executeWeaselChoice(Robot robot) {
        // TODO In the actual game, players should be allowed to choose which direction to turn.
        // Simplified implementation: default left turn.
        System.out.println("Robot " + robot.getRobotID() + " executes Weasel Routine");
        robot.turnLeft();
    }

    public boolean canExecute(Robot robot) {
        return !robot.isPoweredDown();
    }

    @Override
    public SpecialPro clone() {
        return new SpecialPro(this.description, this.actionType, this.specialEffect);
    }
}