package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.*;


/**
 * Represents a push element on the game board.
 * Pushes the robot when activated during a specific register round.
 */
public class PushPanel extends BoardElement {
    private List<Integer> activeRegisters;
    private int currentRegister;
    private String boardId;
    private boolean isOnBoard;

//    public PushPanel() {
//        super();
//        this.activeRegisters = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public PushPanel(Position position, String boardId) {
//        super(position, boardId);
//        this.activeRegisters = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public PushPanel(Position position, Direction direction) {
//        super(position, direction);
//        this.activeRegisters = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public PushPanel(Position position, Direction direction, List<Integer> activeRegisters) {
//        super(position, direction);
//        this.activeRegisters = new ArrayList<>(activeRegisters);
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }

    public PushPanel(Position position, Direction direction, List<Integer> activeRegisters, String boardId) {
        super(position, direction);
        this.activeRegisters = new ArrayList<>(activeRegisters);
        this.currentRegister = 0;
        this.setBoardId(boardId);
    }

    public List<Integer> getActiveRegisters() {
        return new ArrayList<>(activeRegisters);
    }

    public void setActiveRegisters(List<Integer> activeRegisters) {
        this.activeRegisters = new ArrayList<>(activeRegisters);
    }

    public void addActiveRegister(int register) {
        if (!activeRegisters.contains(register)) {
            activeRegisters.add(register);
        }
    }

    public void removeActiveRegister(int register) {
        activeRegisters.remove(Integer.valueOf(register));
    }

    public void setCurrentRegister(int register) {
        this.currentRegister = register;
    }

    public boolean isActiveInCurrentRegister() {
        return activeRegisters.contains(currentRegister);
    }

    @Override
    public void activate(Robot robot) {
        // The push plate will not trigger an effect when the robot enters.
        // The push plate's pushing effect should be handled in the applyEffect method.
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "PushPanel";
    }

    public boolean isOnBoard() {
        return isOnBoard;
    }

    public void setOnBoard(boolean isOnBoard) {
        this.isOnBoard = isOnBoard;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Apply the push effect to the robot.
     * If the push is activated in the current register round, it will push the robot.
     *
     * @param robot The robot located on the push.
     * @param board The game board.
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);

        if (isActiveInCurrentRegister()) {
            pushRobot(robot, board);
        }
    }

    /**
     * Push the robot to the next square in the direction of the push plate.
     *
     * @param robot The robot to be pushed.
     * @param board The game board.
     */
    private void pushRobot(Robot robot, Board board) {
        if (direction == null) {
            System.err.println("Error: Push panel direction is not set!");
            return;
        }

        // Calculate the position after the push
        Position currentPos = robot.getPosition();
        Position targetPos = currentPos.move(direction);


        // Check if the target location is valid
        if (!board.isValidPosition(targetPos)) {
            System.out.println("Robot " + robot.getId() + " would be pushed off the board! Robot falls and reboots.");
            // The robot has fallen off the game board and needs to be restarted.
            rebootRobot(robot, board);
            return;
        }

        // Check if there are robots at the target location
        Robot targetRobot = board.getRobotAt(targetPos);
        if (targetRobot != null) {
            //There is a robot at the target location. Try to push it.
            pushRobotChain(robot, targetRobot, board, direction);
        } else {
            //There are no robots at the target location, so move directly there.
            moveRobot(robot, targetPos, board);
        }

        System.out.println("Push panel at " + position + " pushed Robot " + robot.getId() + " to " + targetPos);
    }

    /**
     * Recursively push a series of robots.
     *
     * @param sourceRobot The source robot to be pushed.
     * @param targetRobot The robot at the target location.
     * @param board The game board.
     * @param pushDirection The direction of the push.
     */
    private void pushRobotChain(Robot sourceRobot, Robot targetRobot, Board board, Direction pushDirection) {
        // Calculate the next position
        Position targetPos = targetRobot.getPosition();
        Position nextPos = targetPos.move(pushDirection);

        // Check if the next position is valid
        if (!board.isValidPosition(nextPos)) {
            System.out.println("Robot " + targetRobot.getId() + " would be pushed off the board! Robot falls and reboots.");
            // The target robot has fallen off the game board and needs to be restarted.
            rebootRobot(targetRobot, board);

            // Now the source robot can move to the target position.
            moveRobot(sourceRobot, targetPos, board);
            return;
        }

        // Check if there is a robot at the next position
        Robot nextRobot = board.getRobotAt(nextPos);
        if (nextRobot != null) {
            //There is also a robot at the next location, recursively pushing
            pushRobotChain(targetRobot, nextRobot, board, pushDirection);
        } else {
            moveRobot(targetRobot, nextPos, board);
        }
        moveRobot(sourceRobot, targetPos, board);
    }

    /**
     * Move the robot to the specified position.
     *
     * @param robot The robot to be moved.
     * @param targetPos The target position.
     * @param board The game board.
     */
    private void moveRobot(Robot robot, Position targetPos, Board board) {
        robot.setPosition(targetPos);
        board.updateRobotPosition(robot, targetPos);

        //Apply the board element effect to the target location.
        //Note: This may cause recursive calls if the target location also has a push plate.
        board.applyEffects(robot, targetPos.x(), targetPos.y());
    }


    /**
     * Restart the robot that has dropped out of the game board.
     *
     * @param robot The robot that needs to be restarted.
     * @param board The game board.
     */
    private void rebootRobot(Robot robot, Board board) {
        robot.takeDamage(2);
        robot.cancelProgramming();

        Position rebootPosition = board.getRebootPosition();
        if (rebootPosition != null) {
            robot.setPosition(rebootPosition);
            board.updateRobotPosition(robot, rebootPosition);

            System.out.println("Robot " + robot.getId() + " has been rebooted at " + rebootPosition);
        } else {
            System.err.println("Error: No reboot position found on the board!");
        }
    }

    /**
     * Get the string representation of the pushboard.
     *
     * @return The string representation of the pushboard, including position, direction, and activated register round.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Push Panel at ").append(position);

        if (isOnBoard) {
            sb.append(" on board ").append(boardId);
        } else {
            sb.append(" not on any board");
        }

        if (direction != null) {
            sb.append(", pushing direction: ").append(direction.getName());
        }

        sb.append(", active in registers: ");
        for (int register : activeRegisters) {
            // +1 Convert to register number 1-5
            sb.append(register + 1).append(", ");
        }

        if (!activeRegisters.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    /**
     * Converts the PushPanel into its corresponding field representation.
     *
     * @return A MessageDefinitions.FieldPushPanel object that represents the push panel,
     *         including its board identifier, direction, and active register configurations.
     */
    @Override
    public MessageDefinitions.FieldPushPanel toField() {
        return new MessageDefinitions.FieldPushPanel(boardId, List.of(direction.toString()), activeRegisters);
    }

//    /**
//     * Convert the Direction enumeration to the direction string required by the protocol.
//     */
//    private String directionToString(Direction direction) {
//        if (direction == null) return null;
//        return switch (direction) {
//            case NORTH -> "top";
//            case SOUTH -> "bottom";
//            case EAST -> "right";
//            case WEST -> "left";
//        };
//    }


//    /**
//     * Serialize to protocol format
//     * @return Map that complies with the protocol
//     */
//    public Map<String, Object> serialize() {
//        Map<String, Object> result = new HashMap<>();
//        result.put("type", "PushPanel");
//        result.put("isOnBoard", boardId);
//
//        // Add direction
//        if (direction != null) {
//            List<String> orientations = new ArrayList<>();
//            orientations.add(directionToString(direction));
//            result.put("orientations", orientations);
//        }
//
//        // Add activated registers
//        result.put("registers", activeRegisters);
//
//        return result;
//    }

}
