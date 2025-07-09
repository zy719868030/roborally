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
        Game game = Game.getInstance();
        org.apache.logging.log4j.Logger logger = org.apache.logging.log4j.LogManager.getLogger(getClass());

        Player player = game.getPlayers().stream()
                .filter(p -> p.getRobot() == robot)
                .findFirst()
                .orElse(null);

        if (player != null) {
            List<RegisterCard> registers = player.getRegister();
            int currentCardIndex = -1;
            for (int i = 0; i < registers.size(); i++) {
                RegisterCard card = registers.get(i);
                if (card == this) {
                    currentCardIndex = i;
                    break;
                }
            }

            if (currentCardIndex > 0) {
                RegisterCard previousCard = player.getRegisterCard(currentCardIndex - 1);

                if (previousCard != null) {
                    // Check if the previous card is a damage card
                    if (previousCard instanceof DamageCard) {
                        logger.info("Robot {} executes Again card - previous card was a damage card, drawing new card",
                                robot.getRobotID());
                        RegisterCard newCard = player.drawCard();
                        logger.info("Robot {} drew {} from the deck to replace damage card",
                                robot.getRobotID(), CardFactory.getCardName(newCard));
                        newCard.execute(robot, player);
                    } else {
                        logger.info("Robot {} executes Again card - repeating previous action from register {}",
                                robot.getRobotID(), (currentCardIndex - 1));
                        previousCard.execute(robot, player);
                    }
                } else {
                    logger.warn("Robot {} executes Again card but no card found in previous register {}",
                            robot.getRobotID(), (currentCardIndex - 1));
                }
            } else if (currentCardIndex == 0) {
                logger.warn("Robot {} attempted to execute Again card in the first register (index=0)", robot.getRobotID());
            } else {
                logger.warn("Robot {} executes Again card but failed to determine its position in register", robot.getRobotID());
            }
        } else {
            logger.error("Robot {} executes Again card but no player found for this robot", robot.getRobotID());
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