package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a conveyor belt element on the game board.
 * The conveyor belt moves robots standing on it during the activation phase.
 * There are two types of conveyor belts: green conveyor belts (single arrow) move one space, and blue conveyor belts
 * (double arrow) move two spaces.
 * Conveyor belts can also be rotating, which will rotate robots as they move.
 */
public class Belts extends BoardElement {
    private static final Logger appLogger = org.apache.logging.log4j.LogManager.getLogger(Belts.class);


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


    public Belts(Position position, Direction outDirection, Direction inDirection, BeltSpeed speed, String boardId) {
        this(position, outDirection, List.of(inDirection), speed, boardId);
    }

    /**
     * Constructs a new Belts instance representing a conveyor belt with the specified properties.
     *
     * @param position     the position of the conveyor belt on the board, cannot be null.
     * @param outDirection the primary direction where the conveyor belt exits, cannot be null.
     * @param inDirections a list of directions where the conveyor belt receives input, cannot be null.
     * @param speed        the speed of the belt, determining how many tiles the robot moves (SLOW or FAST), cannot be null.
     * @param boardId      the identifier of the board to which this belt belongs.
     * @throws IllegalArgumentException if any of the required parameters (position, outDirection, inDirections, or speed) are null.
     */
    public Belts(Position position, Direction outDirection, List<Direction> inDirections, BeltSpeed speed, String boardId) {
        super(position, outDirection, boardId);

        if (position == null || outDirection == null || inDirections == null || speed == null) {
            throw new IllegalArgumentException("Position, outDirection, inDirections, and speed cannot be null.");
        }
        this.speed = speed;
        this.color = (speed == BeltSpeed.SLOW) ? BeltColor.GREEN : BeltColor.BLUE;
        this.outDirections = List.of(outDirection);
        this.inDirections = new ArrayList<>(inDirections);
        // If the export direction and import direction are different, it is a rotating conveyor belt.
        this.isRotating = !inDirections.isEmpty() &&
                !outDirections.getFirst().equals(inDirections.getFirst().turnAround());
        this.isOnBoard = false;
        this.boardId = boardId;
        //this.boardId = "";
        this.setBoardId(boardId);
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
        moveRobotOnBelt(robot, board, speed.getValue());
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
        Direction outDir = outDirections.getFirst();
        for (Direction inDir : inDirections) {
            if (robotDirection == inDir.turnAround() && !robotDirection.equals(outDir)) {
                robot.setDirection(outDir);
                appLogger.info("Robot {} rotated from {} to {} on rotating conveyor belt.", robot.getRobotID(), robot.getDirection().getName(), outDir.getName());
                return;
            }
        }
    }

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

        for (int i = 0; i < steps; i++) {
            Position nextPos = currentPos.move(outDir);

            if (!board.isValidPosition(nextPos)) {
                rebootRobot(robot, board);
                return;
            }

            if (board.getRobotAt(nextPos) != null) {
                return;
            }

            // Wall barrier inspection
            List<BoardElement> currentElements = board.getElements(currentPos.x(), currentPos.y());
            for (BoardElement element : currentElements) {
                if (element instanceof Wall wall) {
                    if (!wall.canExitToDirection(outDir)) {
                        return;
                    }
                }
            }
            List<BoardElement> targetElements = board.getElements(nextPos.x(), nextPos.y());
            for (BoardElement element : targetElements) {
                if (element instanceof Wall wall) {
                    if (!wall.canPassThroughFromDirection(outDir.turnAround())) {
                        return;
                    }
                }
            }

            Belts nextBelt = findBeltAt(board, nextPos);
            if (nextBelt != null && isValidBeltMovement(outDir, nextBelt)) {
                robot.setPosition(nextPos);
                board.updateRobotPosition(robot, nextPos);
                currentPos = nextPos;
                outDir = nextBelt.getMainOutDirection();
            } else {
                robot.setPosition(nextPos);
                board.updateRobotPosition(robot, nextPos);
                currentPos = nextPos;
                break; // Stop belt movement
            }
        }

        // After belt movement is done, apply effects like gear rotation
//        board.applyEffects(robot, currentPos.x(), currentPos.y());
    }

    /**
     * Search for a conveyor belt at a specified location.
     *
     * @param board    Game board.
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
     * @param outDir   The exit direction of the current conveyor belt.
     * @param nextBelt The next conveyor belt.
     * @return Returns true if the movement is valid, otherwise returns false.
     */
    private boolean isValidBeltMovement(Direction outDir, Belts nextBelt) {
        Direction moveFrom = outDir.turnAround();
        return nextBelt.getInDirections().contains(moveFrom);
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

            System.out.println("Robot " + robot.getRobotID() + " has been rebooted at " + rebootPosition);
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

    /**
     * Converts the conveyor belt instance into a FieldConveyorBelt representation.
     *
     * @return A {@code MessageDefinitions.FieldConveyorBelt} object containing the conveyor belt's board ID, speed,
     * and the concatenated list of exit and entry direction strings.
     */
    @Override
    public MessageDefinitions.FieldConveyorBelt toField() {
        return new MessageDefinitions.FieldConveyorBelt(boardId, speed.getValue(), Stream.concat(
                Stream.of(outDirections.getFirst().toString()),
                inDirections.stream().map(Direction::toString)
        ).collect(Collectors.toList()));
    }

}