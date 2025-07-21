package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A specialized programming card that performs unique routines such as energy gain,
 * speed boost, sandbox choices, weasel turns, and damage mitigation.
 * <p>
 * The effect is determined by the {@code specialEffect} keyword.
 * </p>
 */
public class SpecialPro extends ProgrammingCard {
    /**
     * Keyword representing the special effect this card triggers
     * (e.g., "energy routine", "speed routine").
     */
    private final String specialEffect;
    /**
     * Logger for warnings and debugging related to special card execution.
     */
    private static final Logger appLogger = LogManager.getLogger(SpecialPro.class);

    /**
     * Constructs a new SpecialPro card with the given description, action type,
     * and special effect keyword.
     *
     * @param description   textual description of the card's purpose
     * @param actionType    generic action type identifier (inherited semantics)
     * @param specialEffect keyword indicating which unique routine to perform
     */
    public SpecialPro(String description, String actionType, String specialEffect) {
        super(description, actionType);
        this.specialEffect = specialEffect;
    }

    /**
     * Returns the special effect keyword of this card.
     *
     * @return the specialEffect string
     */
    public String getSpecialEffect() {
        return specialEffect;
    }

    /**
     * Executes the special routine on the given robot and player.
     * <p>
     * If the robot is powered down, logs a warning and aborts.
     * Supported routines:
     * <ul>
     *   <li>"energy routine": grants 1 energy to the player</li>
     *   <li>"speed routine": moves the robot forward 3 spaces</li>
     *   <li>"sandbox routine": lets the robot perform a chosen basic action (default move 1)</li>
     *   <li>"weasel routine": executes a default left turn</li>
     *   <li>"spam folder": removes one SPAM damage effect (stub logging)</li>
     *   <li>default: logs execution of an unknown effect</li>
     * </ul>
     * </p>
     *
     * @param robot  the robot performing the special routine
     * @param player the player controlling the robot
     */
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

    /**
     * Executes a sandbox routine, allowing a default basic action.
     * <p>
     * In a full implementation, the player would choose the action via UI.
     * This stub defaults to moving 1 space.
     * </p>
     *
     * @param robot the robot performing the sandbox action
     * @param board the board context for movement
     */
    private void executeSandboxChoice(Robot robot, Board board) {
        // TODO In the actual game, players should be allowed to choose actions here.
        // Simplified implementation: randomly select a basic action.
        System.out.println("Robot " + robot.getRobotID() + " executes Sandbox Routine");
        // Move 1 can be executed by default here
        // TODO UI interaction may be required to allow the player to choose.
        robot.applyMove(board, 1);
    }

    /**
     * Executes a weasel routine: a default turn action.
     * <p>
     * In a full implementation, the player would choose the turn direction.
     * This stub defaults to a left turn.
     * </p>
     *
     * @param robot the robot performing the weasel action
     */
    private void executeWeaselChoice(Robot robot) {
        // TODO In the actual game, players should be allowed to choose which direction to turn.
        // Simplified implementation: default left turn.
        System.out.println("Robot " + robot.getRobotID() + " executes Weasel Routine");
        robot.turnLeft();
    }

    /**
     * Determines whether this card may be executed by the given robot.
     * Overridden to ensure powered-down robots cannot run special routines.
     *
     * @param robot the robot to check
     * @return {@code true} if execution is permitted, {@code false} otherwise
     */
    public boolean canExecute(Robot robot) {
        return !robot.isPoweredDown();
    }

    /**
     * Creates and returns a deep copy of this SpecialPro card.
     *
     * @return a new SpecialPro instance with the same description, action type, and special effect
     */
    @Override
    public SpecialPro clone() {
        return new SpecialPro(this.description, this.actionType, this.specialEffect);
    }
}