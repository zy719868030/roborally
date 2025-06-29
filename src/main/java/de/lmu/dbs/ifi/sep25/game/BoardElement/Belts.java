package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

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

//    public Belts(String boardId) {
//        super(boardId);
//        this.speed = BeltSpeed.SLOW;
//        this.color = BeltColor.GREEN;
//        this.outDirections = new ArrayList<>();
//        this.outDirections.add(Direction.NORTH);
//        this.inDirections = new ArrayList<>();
//        this.isRotating = false;
//        this.isOnBoard = false;
//        this.boardId = boardId; // Set boardId to boardName
//    }
//
//    public Belts(Position position, String boardId) {
//        super(position, boardId);
//        this.speed = BeltSpeed.SLOW;
//        this.color = BeltColor.GREEN;
//        this.outDirections = new ArrayList<>();
//        this.outDirections.add(Direction.NORTH);
//        this.inDirections = new ArrayList<>();
//        this.isRotating = false;
//        this.isOnBoard = false;
//        this.boardId = boardId;
//    }
//
//
//    public Belts(Position position, Direction outDirection, BeltSpeed speed, String boardID) {
//        super(position, outDirection, boardID);
//        this.speed = speed;
//        this.color = (speed == BeltSpeed.SLOW) ? BeltColor.GREEN : BeltColor.BLUE;
//        this.outDirections = new ArrayList<>();
//        this.outDirections.add(outDirection);
//        this.inDirections = new ArrayList<>();
//        this.isRotating = false;
//        this.isOnBoard = false;
//        this.boardId = boardID;
//        this.setBoardId(boardId); // Updates isOnBoard
//
//    }
    /*
    public Belts(Position position, Direction outDirection, BeltSpeed speed, String boardName) {
        super(position, !outDirections.isEmpty() ? outDirections.get(0) : null, boardName);
        this.speed = speed;
        this.color = (speed == BeltSpeed.SLOW) ? BeltColor.GREEN : BeltColor.BLUE;
        this.outDirections = new ArrayList<>();
        this.outDirections.add(outDirection);
        this.inDirections = new ArrayList<>();
        //this.isRotating = false;
        //this.setBoardId(boardId);

        this.boardId = boardName; // Added: Set boardId to boardName
        this.setBoardName(boardName);
    }

     */

    public Belts(Position position, Direction outDirection, Direction inDirection, BeltSpeed speed, String boardId) {
        this(position, outDirection, List.of(inDirection), speed, boardId);
    }


    /**
     * Constructs a new Belts instance representing a conveyor belt with the specified properties.
     *
     * @param position the position of the conveyor belt on the board, cannot be null.
     * @param outDirection the primary direction where the conveyor belt exits, cannot be null.
     * @param inDirections a list of directions where the conveyor belt receives input, cannot be null.
     * @param speed the speed of the belt, determining how many tiles the robot moves (SLOW or FAST), cannot be null.
     * @param boardId the identifier of the board to which this belt belongs.
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

//    /**
//     * Convert the Direction enumeration to the direction string required by the protocol.
//     * @param direction Direction enumeration value.
//     * @return Direction string used by the protocol: “top”, “bottom”, ‘right’, “left”.
//     */
//    private String directionToString(Direction direction) {
//        return switch (direction) {
//            case NORTH -> "top";
//            case SOUTH -> "bottom";
//            case EAST -> "right";
//            case WEST -> "left";
//        };
//    }

//    /**
//     * Convert the direction string in the protocol to a Direction enumeration.
//     * @param dirString Direction string in the protocol: “top”, “bottom”, ‘right’, “left”
//     * @return Corresponding Direction enumeration value.
//     */
//    private Direction stringToDirection(String dirString) {
//        return switch (dirString) {
//            case "top" -> Direction.NORTH;
//            case "bottom" -> Direction.SOUTH;
//            case "right" -> Direction.EAST;
//            case "left" -> Direction.WEST;
//            default -> throw new IllegalArgumentException("Invalid direction string: " + dirString);
//        };
//    }

//    /**
//     * Get the list of directions for serialization, in a format that complies with the protocol requirements.
//     * The first direction is the outbound direction, followed by the inbound direction.
//     * @return List of direction strings.
//     */
//    public List<String> getOrientationsForProtocol() {
//        List<String> orientations = new ArrayList<>();
//        if (!outDirections.isEmpty()) {
//            orientations.add(directionToString(outDirections.get(0)));
//        }
//
//        for (Direction inDir : inDirections) {
//            orientations.add(directionToString(inDir));
//        }
//
//        return orientations;
//    }

//    /**
//     * Create a conveyor belt instance from the protocol representation.
//     * @param position Position.
//     * @param orientations List of orientations, with the first being the outflow direction and the rest being the inflow directions.
//     * @param speed Speed (1 = green belt, 2 = blue belt).
//     * @param boardId Board ID.
//     */
//    public Belts(Position position, List<String> orientations, int speed, String boardId) {
//        super(position, boardId);
//
//        this.speed = speed == 1 ? BeltSpeed.SLOW : BeltSpeed.FAST;
//        this.color = speed == 1 ? BeltColor.GREEN : BeltColor.BLUE;
//
//        this.outDirections = new ArrayList<>();
//        this.inDirections = new ArrayList<>();
//
//        if (!orientations.isEmpty()) {
//            this.outDirections.add(stringToDirection(orientations.get(0)));
//
//            for (int i = 1; i < orientations.size(); i++) {
//                this.inDirections.add(stringToDirection(orientations.get(i)));
//            }
//        }
//
//        // Determine whether it is a rotating conveyor belt
//        this.isRotating = !outDirections.isEmpty() && !inDirections.isEmpty() &&
//                !outDirections.get(0).equals(inDirections.get(0).turnAround());
//
//        this.isOnBoard = true;
//        this.boardId = boardId;
//    }

//    /**
//     * Get the serialized representation of the conveyor belt.
//     * @return Serialized Map representation.
//     */
//    public Map<String, Object> serialize() {
//        Map<String, Object> result = new HashMap<>();
//        result.put("type", "ConveyorBelt");
//        result.put("isOnBoard", boardId);
//        result.put("speed", getSpeedForProtocol());
//        result.put("orientations", getOrientationsForProtocol());
//        return result;
//    }
//
//    /**
//     * Get the speed value used for the protocol.
//     * @return 1 for green belt, 2 for blue belt.
//     */
//    public int getSpeedForProtocol() {
//        return speed.getValue();
//    }

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

    /**
     * Converts the conveyor belt instance into a FieldConveyorBelt representation.
     *
     * @return A {@code MessageDefinitions.FieldConveyorBelt} object containing the conveyor belt's board ID, speed,
     *         and the concatenated list of exit and entry direction strings.
     */
    @Override
    public MessageDefinitions.FieldConveyorBelt toField() {
        return new MessageDefinitions.FieldConveyorBelt(boardId, speed.getValue(), Stream.concat(
                Stream.of(outDirections.getFirst().toString()),
                inDirections.stream().map(Direction::toString)
        ).collect(Collectors.toList()));
    }

}