package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.game.Board;

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
    public void execute(Robot robot) {
        if (!canExecute(robot)) return;

        Board board = robot.getBoard();

        switch (specialEffect.toLowerCase()) {
            case "again":
                executeAgain(robot, board);
                break;
            case "energy routine":
                robot.addEnergy(1);
                System.out.println("Robot " + robot.getId() + " executes Energy Routine - gained 1 energy");
                break;
            case "speed routine":
                if (board != null) {
                    robot.applyMove(board, 3);
                    System.out.println("Robot " + robot.getId() + " executes Speed Routine - moved 3 spaces");
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
                System.out.println("Robot " + robot.getId() + " executes SPAM Folder - removes SPAM damage");
                break;
            default:
                System.out.println("Robot " + robot.getId() + " executes unknown special effect: " + specialEffect);
        }
    }

    // Again card: Repeat the action of the previous register.
    private void executeAgain(Robot robot, Board board) {
        // TODO Here, we need to retrieve the card from the previous register and re-execute it.
        // Simplified implementation: Assume that the Robot class has a getPreviousCard() method.
        System.out.println("Robot " + robot.getId() + " executes Again - repeating previous action");
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
        System.out.println("Robot " + robot.getId() + " executes Sandbox Routine");
        // Move 1 can be executed by default here
        // TODO UI interaction may be required to allow the player to choose.
        robot.applyMove(board, 1);
    }

    // Weasel Routine: Select turning action
    private void executeWeaselChoice(Robot robot) {
        // TODO In the actual game, players should be allowed to choose which direction to turn.
        // Simplified implementation: default left turn.
        System.out.println("Robot " + robot.getId() + " executes Weasel Routine");
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