package de.lmu.dbs.ifi.sep25.card.ProgrammingCard;

import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A standard programming card that performs basic robot actions such as movement, rotation,
 * power-up, and repeating the previous action.
 * <p>
 * Supports action types: "move", "backup", "turnLeft", "turnRight", "uTurn", "powerUp", and "again".
 * </p>
 */
public class RegularPro extends ProgrammingCard {
    /** Logger for application-level events and errors. */
    private final static Logger appLogger = LogManager.getLogger(RegularPro.class);

    /** The distance to move when executing move or backup actions. */
    private int distance;

    /**
     * Constructs a new RegularPro card with the given description, action type, and distance.
     *
     * @param description descriptive text for this card
     * @param actionType  the action keyword to perform (e.g., "move", "turnLeft")
     * @param distance    the number of squares to move (positive for forward, negative for backup)
     */
    public RegularPro(String description, String actionType, int distance) {
        super(description, actionType);
        this.distance = distance;
    }

    /**
     * Returns the configured movement distance for this card.
     *
     * @return the distance in board squares
     */
    public int getDistance() {
        return distance;
    }

    /**
     * Executes the programming action on the specified robot.
     * <p>
     * If the robot is powered down or the board is null, logs an error and aborts.
     * Supported actions:
     * <ul>
     *   <li>"move": move forward by {@link #distance} squares</li>
     *   <li>"backup": move backward by {@link #distance} squares</li>
     *   <li>"turnLeft": rotate left</li>
     *   <li>"turnRight": rotate right</li>
     *   <li>"uTurn": rotate 180 degrees</li>
     *   <li>"powerUp": grant the player 1 energy</li>
     *   <li>"again": repeat the last non-Again register action</li>
     * </ul>
     * </p>
     *
     * @param robot  the robot performing the action
     * @param player the player controlling the robot
     */
    @Override
    public void execute(Robot robot, Player player) {
        if (!canExecute(robot)) {
            appLogger.error("Robot {} cannot execute regular programmed action: {}", robot.getRobotID(), actionType);
            return;
        }

        Board board = robot.getBoard();
        if (board == null) {
            appLogger.error("Robot {} cannot execute regular programmed action: {} - board is null", robot.getRobotID(), actionType);
            return;
        }

//        appLogger.debug("Executing regular programmed action: {} for robot {}", actionType, robot.getRobotID());

        switch (actionType.toLowerCase()) {
            case "move" -> {
                Position oldPos = robot.getPosition();
                Direction facing = robot.getDirection();
                appLogger.info("=== CARD EXECUTION === Robot {} executing MoveI/II/III: moving {} steps forward from {} facing {}",
                        robot.getRobotID(), distance, oldPos, facing.getName());

                robot.applyMove(board, distance);

                Position newPos = robot.getPosition();
                appLogger.info("=== CARD RESULT === Robot {} moved from {} to {} (delta: dx={}, dy={})",
                        robot.getRobotID(), oldPos, newPos,
                        newPos.x() - oldPos.x(), newPos.y() - oldPos.y());
            }

            case "backup" -> {
                Position oldPos = robot.getPosition();
                Direction facing = robot.getDirection();
                appLogger.info("=== CARD EXECUTION === Robot {} executing BackUp: moving {} steps backward from {} facing {}",
                        robot.getRobotID(), distance, oldPos, facing.getName());

                robot.applyMove(board, -distance);

                Position newPos = robot.getPosition();
                appLogger.info("=== CARD RESULT === Robot {} moved from {} to {} (delta: dx={}, dy={})",
                        robot.getRobotID(), oldPos, newPos,
                        newPos.x() - oldPos.x(), newPos.y() - oldPos.y());
            }

            case "turnleft" -> {
                Direction oldDir = robot.getDirection();
                appLogger.info("=== CARD EXECUTION === Robot {} executing TurnLeft: rotating from {} to counterclockwise",
                        robot.getRobotID(), oldDir.getName());

                robot.turnLeft();

                Direction newDir = robot.getDirection();
                appLogger.info("=== CARD RESULT === Robot {} rotated from {} to {} at position {}",
                        robot.getRobotID(), oldDir.getName(), newDir.getName(), robot.getPosition());
            }

            case "turnright" -> {
                Direction oldDir = robot.getDirection();
                appLogger.info("=== CARD EXECUTION === Robot {} executing TurnRight: rotating from {} clockwise",
                        robot.getRobotID(), oldDir.getName());

                robot.turnRight();

                Direction newDir = robot.getDirection();
                appLogger.info("=== CARD RESULT === Robot {} rotated from {} to {} at position {}",
                        robot.getRobotID(), oldDir.getName(), newDir.getName(), robot.getPosition());
            }

            case "powerup" -> {
                appLogger.info("=== CARD EXECUTION === Robot {} executing PowerUp: gaining 1 energy",
                        robot.getRobotID());

                player.addEnergy(1, "Power Up");

                appLogger.info("=== CARD RESULT === Robot {} gained 1 energy",
                        robot.getRobotID());
            }

            case "again" -> {
                appLogger.info("=== CARD EXECUTION === Robot {} executing Again card",
                        robot.getRobotID());

                executeAgain(robot);

                appLogger.info("=== CARD RESULT === Robot {} completed Again execution",
                        robot.getRobotID());
            }

            case "uturn" -> {
                Direction oldDir = robot.getDirection();
                appLogger.info("=== CARD EXECUTION === Robot {} executing UTurn: rotating from {} 180 degrees",
                        robot.getRobotID(), oldDir.getName());

                robot.turnAround();

                Direction newDir = robot.getDirection();
                appLogger.info("=== CARD RESULT === Robot {} rotated from {} to {} at position {}",
                        robot.getRobotID(), oldDir.getName(), newDir.getName(), robot.getPosition());

            }
        }
    }

    /**
     * Repeats the most recent non-Again card executed by this robot’s player.
     * Logs errors if no valid previous action is found or on execution failure.
     *
     * @param robot the robot for which to repeat the previous action
     */
    private void executeAgain(Robot robot) {
        final Player player = findPlayerByRobot(robot);
        if (player == null) {
            appLogger.error("Cannot find player for robot {}", robot.getRobotID());
            return;
        }

        final RegisterCard prevCard = findPreviousNonAgainCard(player, Game.getInstance().getCurrentRegister());
        if (prevCard == null) {
            appLogger.error("No valid previous card to repeat for Again.");
            return;
        }

        String prevCardName = CardFactory.getCardName(prevCard);
        appLogger.info("=== AGAIN EXECUTION === Robot {} executing Again: Repeating {} from register {}",
                robot.getRobotID(), prevCardName, Game.getInstance().getCurrentRegister() - 1);

        try {
            // 记录执行前状态
            Position oldPos = robot.getPosition();
            Direction oldDir = robot.getDirection();

            // Directly execute the previous card
            prevCard.execute(robot, player);

            // 记录执行后状态
            Position newPos = robot.getPosition();
            Direction newDir = robot.getDirection();

            appLogger.info("=== AGAIN RESULT === Robot {} repeated {} - Position: {} -> {}, Direction: {} -> {}",
                    robot.getRobotID(), prevCardName, oldPos, newPos, oldDir.getName(), newDir.getName());
        } catch (Exception e) {
            appLogger.error("Error executing Again card for Robot {}: {}", robot.getRobotID(), e.getMessage(), e);
        }
    }

    /**
     * Finds the first non-Again programming card in earlier registers for the given player.
     *
     * @param player          the player whose history to search
     * @param currentRegister the index of the current register slot
     * @return the first non-Again RegisterCard, or {@code null} if none found
     */
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

    /**
     * Helper method to find Player by robot
     * @param robot finding the owner of this robot.
     * @return the corresponding player of the given robot.
     */
    private Player findPlayerByRobot(Robot robot) {
        Game game = Game.getInstance();
        for (Player player : game.getPlayers()) {
            if (player.getRobot() == robot) {
                return player;
            }
        }
        return null;
    }

    /**
     * Determines whether this card may be executed by the given robot.
     * Cards cannot execute if the robot is powered down.
     *
     * @param robot the robot to check
     * @return {@code true} if execution is permitted, {@code false} otherwise
     */
    public boolean canExecute(Robot robot) {
        return !robot.isPoweredDown();
    }

    /**
     * Creates and returns a deep copy of this RegularPro card.
     *
     * @return a new RegularPro instance with identical description, actionType, and distance
     */
    @Override
    public RegularPro clone() {
        return new RegularPro(this.description, this.actionType, this.distance);
    }
}