package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a conveyor belt element on the game board.
 * The conveyor belt moves robots standing on it during the activation phase.
 * There are two types of conveyor belts: green conveyor belts (single arrow) move one space, and blue conveyor belts
 * (double arrow) move two spaces.
 * Conveyor belts can also be rotating, which will rotate robots as they move.
 */
public class Belts extends BoardElement {

    // Conveyor belt speed: Green conveyor belt moves one square; blue conveyor belt moves two squares.
    public enum BeltSpeed {
        SLOW(1),
        FAST(2);

        private final int value;

        BeltSpeed(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum BeltColor {
        GREEN,
        BLUE
    }

    private BeltSpeed speed;
    private BeltColor color;
    private List<Direction> outDirections;
    private List<Direction> inDirections;
    private boolean isRotating;
    private String boardId;
    private boolean isOnBoard;

    public Belts() {
        super();
        this.speed = BeltSpeed.SLOW;
        this.color = BeltColor.GREEN;
        this.outDirections = new ArrayList<>();
        this.outDirections.add(Direction.NORTH);
        this.inDirections = new ArrayList<>();
        this.isRotating = false;
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Belts(Position position) {
        super(position);
        this.speed = BeltSpeed.SLOW;
        this.color = BeltColor.GREEN;
        this.outDirections = new ArrayList<>();
        this.outDirections.add(Direction.NORTH);
        this.inDirections = new ArrayList<>();
        this.isRotating = false;
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Belts(Position position, Direction outDirection, BeltSpeed speed) {
        super(position);
        this.speed = speed;
        this.color = (speed == BeltSpeed.SLOW) ? BeltColor.GREEN : BeltColor.BLUE;
        this.outDirections = new ArrayList<>();
        this.outDirections.add(outDirection);
        this.inDirections = new ArrayList<>();
        this.isRotating = false;
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Belts(Position position, Direction outDirection, BeltSpeed speed, String boardId) {
        super(position);
        this.speed = speed;
        this.color = (speed == BeltSpeed.SLOW) ? BeltColor.GREEN : BeltColor.BLUE;
        this.outDirections = new ArrayList<>();
        this.outDirections.add(outDirection);
        this.inDirections = new ArrayList<>();
        this.isRotating = false;
        this.setBoardId(boardId);
    }

    /**
     * Constructor with position, exit direction list, entry direction list, and speed parameters.
     * Used to create a rotating conveyor belt.
     *
     * @param position The position of the conveyor belt.
     * @param outDirections The exit direction list of the conveyor belt.
     * @param inDirections The entry direction list of the conveyor belt.
     * @param speed The speed of the conveyor belt.
     */
    public Belts(Position position, List<Direction> outDirections, List<Direction> inDirections, BeltSpeed speed) {
        super(position);
        this.speed = speed;
        this.color = (speed == BeltSpeed.SLOW) ? BeltColor.GREEN : BeltColor.BLUE;
        this.outDirections = new ArrayList<>(outDirections);
        this.inDirections = new ArrayList<>(inDirections);
        // If the export direction and import direction are different, it is a rotating conveyor belt.
        this.isRotating = !outDirections.isEmpty() && !inDirections.isEmpty() &&
                !outDirections.get(0).equals(inDirections.get(0).turnAround());
        this.isOnBoard = false;
        this.boardId = "";
    }

    public BeltSpeed getSpeed() {
        return speed;
    }

    public void setSpeed(BeltSpeed speed) {
        this.speed = speed;
        this.color = (speed == BeltSpeed.SLOW) ? BeltColor.GREEN : BeltColor.BLUE;
    }

    public BeltColor getColor() {
        return color;
    }

    public void addOutDirection(Direction direction) {
        if (!outDirections.contains(direction)) {
            outDirections.add(direction);
        }
    }

    public List<Direction> getOutDirections() {
        return new ArrayList<>(outDirections);
    }

    /**
     * Get the main exit direction of the conveyor belt.
     *
     * @return The main exit direction of the conveyor belt. If there is no exit direction, return null.
     */
    public Direction getMainOutDirection() {
        return outDirections.isEmpty() ? null : outDirections.get(0);
    }

    public void addInDirection(Direction direction) {
        if (!inDirections.contains(direction)) {
            inDirections.add(direction);
        }
    }

    public List<Direction> getInDirections() {
        return new ArrayList<>(inDirections);
    }

    public boolean isRotating() {
        return isRotating;
    }

    public void setRotating(boolean rotating) {
        this.isRotating = rotating;
    }

    public boolean isOnBoard() {
        return isOnBoard;
    }

    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    @Override
    public void activate(Robot robot) {
        // The conveyor belt does not trigger an effect when the robot enters.
        // The conveyor belt's movement effect should be handled in the applyEffect method.
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "ConveyorBelt";
    }

    /**
     * Apply conveyor belt effect to robot
     * Conveyor belt will move robot standing on it during activation phase
     *
     * @param robot Robot standing on conveyor belt
     * @param board Game board
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
        // If the conveyor belt has no exit direction, no action is taken.
        if (outDirections.isEmpty()) {
            return;
        }


        // If it is a rotating conveyor belt, rotate the robot first.
        Position currentPos = robot.getPosition();
        if (isRotating) {
            rotateRobot(robot);
        }
        // Move the robot according to the speed of the conveyor belt.
        moveRobotOnBelt(robot, board, speed.ordinal() + 1);
    }

    /**
     * Rotate robot
     * The rotating conveyor belt rotates the robot according to the direction of the entrance and exit.
     *
     * @param robot Robot to be rotated
     */
    private void rotateRobot(Robot robot) {
        Direction robotDirection = robot.getDirection();
        // If there is no entrance or exit direction, do not perform rotation.
        if (inDirections.isEmpty() || outDirections.isEmpty()) {
            return;
        }

        // If the robot enters from the entrance direction, adjust the direction to match the exit direction.
        Direction outDir = outDirections.get(0);
        for (Direction inDir : inDirections) {
            if (robotDirection == inDir.turnAround()) {
                robot.setDirection(outDir);
                System.out.println("Robot " + robot.getId() + " rotated from " +
                        robotDirection.getName() + " to " + outDir.getName() +
                        " on rotating conveyor belt.");
                return;
            }
        }
    }

    /**
     * Calculate the number of right turns required to rotate from one direction to another.
     *
     * @param from Starting direction
     * @param to Target direction
     * @return Number of right turns required (0-3)
     */

    /**
     * Move the robot on the conveyor belt
     *
     * @param robot The robot to be moved
     * @param board The game board
     * @param steps The number of steps to move
     */
    private void moveRobotOnBelt(Robot robot, Board board, int steps) {
        Position currentPos = robot.getPosition();
        Direction outDir = getMainOutDirection();

        // Check if the next position is valid and move the robot
        for (int i = 0; i < steps; i++) {
            Position nextPos = currentPos.move(outDir);
            // Robot fell off the game board and needs to be restarted.
            if (!board.isValidPosition(nextPos)) {
                System.out.println("Robot " + robot.getId() + " would be pushed off the board by conveyor belt! " +
                        "Robot falls and reboots.");
                rebootRobot(robot, board);
                return;
            }

            // Check if the next position is valid. If there is a robot at the target position, stop moving.
            Robot targetRobot = board.getRobotAt(nextPos);
            if (targetRobot != null) {
                System.out.println("Robot " + robot.getId() + " is blocked by Robot " + targetRobot.getId() +
                        " on conveyor belt.");
                return;
            }

            // Check if there is a conveyor belt at the next location.If the direction does not match, stop moving.
            Belts nextBelt = findBeltAt(board, nextPos);
            if (nextBelt != null) {
                if (isValidBeltMovement(outDir, nextBelt)) {
                    robot.setPosition(nextPos);
                    board.updateRobotPosition(robot, nextPos);

                    currentPos = nextPos;
                    outDir = nextBelt.getMainOutDirection();

                    if (nextBelt.isRotating()) {
                        nextBelt.rotateRobot(robot);
                    }
                } else {
                    System.out.println("Robot " + robot.getId() + " cannot continue on conveyor belt due to " +
                            "direction mismatch.");
                    return;
                }
            } else {
                robot.setPosition(nextPos);
                board.updateRobotPosition(robot, nextPos);

                // Apply the board element effect to the next position
                board.applyEffects(robot, nextPos.x(), nextPos.y());

                return;
            }
        }
    }


    /**
     * Search for a conveyor belt at a specified location.
     *
     * @param board Game board.
     * @param position Location to search.
     * @return Found conveyor belt. If none found, return null.
     */
    private Belts findBeltAt(Board board, Position position) {
        for (BoardElement element : board.getElements(position.x(), position.y())) {
            if (element instanceof Belts) {
                return (Belts) element;
            }
        }
        return null;
    }

    /**
     * Check whether the conveyor belt movement is valid.
     * The condition for valid movement is: the exit direction matches the entrance direction of the next conveyor belt.
     *
     * @param outDir The exit direction of the current conveyor belt.
     * @param nextBelt The next conveyor belt.
     * @return Returns true if the movement is valid, otherwise returns false.
     */
    private boolean isValidBeltMovement(Direction outDir, Belts nextBelt) {
        for (Direction inDir : nextBelt.getInDirections()) {
            if (outDir.equals(inDir)) {
                return true;
            }
        }
        return nextBelt.getInDirections().isEmpty();
    }

    /**
     * Restart the robot that dropped out of the game board.
     *
     * @param robot Robot that needs to be restarted.
     * @param board Game board.
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append((color == BeltColor.GREEN ? "Green" : "Blue") + " conveyor belt at " + position);

        if (isOnBoard) {
            sb.append(" on board ").append(boardId);
        } else {
            sb.append(" not on any board");
        }

        if (!outDirections.isEmpty()) {
            sb.append(", out directions: ");
            for (Direction dir : outDirections) {
                sb.append(dir.getName()).append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

        if (!inDirections.isEmpty()) {
            sb.append(", in directions: ");
            for (Direction dir : inDirections) {
                sb.append(dir.getName()).append(", ");
            }
            sb.setLength(sb.length() - 2);
        }

        if (isRotating) {
            sb.append(", rotating");
        }

        return sb.toString();
    }
}