package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SpecialPro extends ProgrammingCard {
    private final String specialEffect;
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
        if (!canExecute(robot)) {
            appLogger.warn("Robot {} cannot execute special effect: {}", robot.getRobotID(), specialEffect);
            return;
        }

        final Board board = robot.getBoard();

//        appLogger.debug("Executing special programmed action: {} for robot {}", actionType, robot.getRobotID());

        switch (specialEffect.toLowerCase()) {
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
//            case "repeat routine":
//                executeAgain(robot, board);
//                break;
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