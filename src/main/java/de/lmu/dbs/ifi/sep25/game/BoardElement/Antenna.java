package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Represents the priority antenna element on the RoboRally game board.
 * 
 * <p>The antenna serves as a reference point for determining player action priority during
 * the activation phase. It acts as an impassable obstacle and provides a directional
 * reference for resolving ties when multiple robots are at the same distance from the antenna.</p>
 *
 */
public class Antenna extends BoardElement {
    
    /** The unique identifier of the board this antenna belongs to */
    private String boardId;
    
    /** Flag indicating whether this antenna is placed on a game board */
    private boolean isOnBoard;

    /**
     * Constructs a new Antenna at the specified position with the given direction.
     * This constructor creates an antenna that is not associated with any specific board.
     * 
     * @param position the position where the antenna is placed on the game board
     * @param direction the direction the antenna is facing, used for tie resolution
     * @throws IllegalArgumentException if position or direction is null
     */
    public Antenna(Position position, Direction direction) {
        super(position, direction);
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructs a new Antenna at the specified position with the given direction and board ID.
     * This constructor creates an antenna that is associated with a specific game board.
     * 
     * @param position the position where the antenna is placed on the game board
     * @param direction the direction the antenna is facing, used for tie resolution
     * @param boardId the unique identifier of the board this antenna belongs to
     * @throws IllegalArgumentException if position, direction, or boardId is null
     */
    public Antenna(Position position, Direction direction, String boardId) {
        super(position, direction);
        this.setBoardId(boardId);
    }

    /**
     * Checks whether this antenna is currently placed on a game board.
     * 
     * @return true if the antenna is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this antenna is placed on a game board.
     * 
     * @param onBoard true to mark the antenna as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this antenna belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this antenna and updates the on-board status accordingly.
     * If the boardId is not empty, the antenna is marked as being on a board.
     * 
     * @param boardId the unique identifier of the board this antenna belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Activates the antenna effect on a robot.
     * 
     * <p>The antenna itself does not have any activation effect on robots.
     * It only serves as a reference point for determining action priority.</p>
     * 
     * @param robot the robot to activate the antenna effect on (unused)
     */
    @Override
    public void activate(Robot robot) {
        // Antenna has no activation effect
    }

    /**
     * Checks whether a robot can pass through this antenna.
     * 
     * <p>According to RoboRally game rules, robots cannot pass through the antenna.
     * The antenna acts as an impassable obstacle on the game board.</p>
     * 
     * @param robot the robot attempting to pass through the antenna
     * @return false - robots cannot pass through the antenna
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return false;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "Antenna" identifying this element type
     */
    @Override
    public String getType() {
        return "Antenna";
    }

    /**
     * Calculates the Manhattan distance between this antenna and a robot's position.
     * This distance is used to determine the priority order of robots during the activation phase.
     * 
     * <p>The Manhattan distance is calculated as |x1 - x2| + |y1 - y2|, which represents
     * the minimum number of moves required to reach the antenna from the robot's position.</p>
     * 
     * @param robotPosition the position of the robot to calculate distance to
     * @return the Manhattan distance between the antenna and the robot
     * @throws IllegalArgumentException if robotPosition is null
     */
    public int distanceToRobot(Position robotPosition) {
        return position.distanceTo(robotPosition);
    }

    /**
     * Sorts a list of robots that are at the same distance from the antenna by their priority.
     * 
     * <p>When multiple robots are equidistant from the antenna, their priority is determined
     * by their angular position relative to the antenna's direction. The sorting follows
     * a clockwise rotation starting from the antenna's facing direction.</p>
     * 
     * <p>Algorithm:</p>
     * <ol>
     *   <li>Calculate the angle of each robot relative to the antenna's direction</li>
     *   <li>Sort robots by their angles in ascending order (smaller angles first)</li>
     *   <li>Robots with smaller angles have higher priority</li>
     * </ol>
     * 
     * @param robots a list of robots that are at the same distance from the antenna
     * @return a new list containing the robots sorted by priority (highest priority first)
     * @throws IllegalArgumentException if robots is null
     */
    public List<Robot> sortTiedRobotsByPriority(List<Robot> robots) {
        if (robots.size() <= 1) {
            return robots;
        }

        // Get the antenna position as a reference point
        final Position antennaPos = this.position;
        final Direction antennaDir = this.direction;
        List<Robot> sortedRobots = new ArrayList<>(robots);

        // Calculate the angle relative to the antenna and sort in ascending order by angle (smaller angles first).
        sortedRobots.sort(new Comparator<Robot>() {
            @Override
            public int compare(Robot r1, Robot r2) {
                double angle1 = calculateAngle(antennaPos, r1.getPosition(), antennaDir);
                double angle2 = calculateAngle(antennaPos, r2.getPosition(), antennaDir);
                return Double.compare(angle1, angle2);
            }
        });

        return sortedRobots;
    }

    /**
     * Calculates the angle of a point relative to the antenna's position and direction.
     * 
     * <p>This method is used for tie resolution when multiple robots are at the same
     * distance from the antenna. The angle is calculated in radians and represents
     * the position of the point relative to the antenna's facing direction.</p>
     * 
     * <p>The angle calculation:</p>
     * <ul>
     *   <li>Uses the antenna's direction as the reference (0 radians)</li>
     *   <li>Calculates the angle to the point using atan2</li>
     *   <li>Normalizes the result to be between 0 and 2π</li>
     * </ul>
     * 
     * @param center the antenna's position (center point)
     * @param point the position of the robot or point to calculate angle to
     * @param direction the antenna's facing direction
     * @return the angle in radians (0 to 2π), where 0 represents the antenna's direction
     */
    private double calculateAngle(Position center, Position point, Direction direction) {
        int dx = point.x() - center.x();
        int dy = point.y() - center.y();
        double angle = Math.atan2(dy, dx);

        double directionOffset = 0;
        switch (direction) {
            case NORTH: directionOffset = -Math.PI/2; break;
            case EAST:  directionOffset = 0; break;
            case SOUTH: directionOffset = Math.PI/2; break;
            case WEST:  directionOffset = Math.PI; break;
        }

        angle = angle - directionOffset;
        if (angle < 0) {
            angle += 2 * Math.PI;
        }

        return angle;
    }

    /**
     * Returns a string representation of this antenna.
     * 
     * <p>The string includes the antenna's position, board association status,
     * and facing direction.</p>
     * 
     * @return a string describing the antenna's location and properties
     */
    @Override
    public String toString() {
        return "Antenna at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                ", direction: " + direction.getName();
    }

    /**
     * Converts this Antenna object to a FieldAntenna representation for network communication.
     * 
     * <p>This method is used for serializing the antenna information when sending
     * game state updates to clients. The resulting FieldAntenna object contains
     * the board ID and direction information in a format suitable for network transmission.</p>
     * 
     * @return a new FieldAntenna object representing this antenna's network data
     */
    @Override
    public MessageDefinitions.FieldAntenna toField() {
        return new MessageDefinitions.FieldAntenna(boardId, List.of(direction.toString()));
    }

//    /**
//     * Convert the Direction enumeration to the direction string required by the protocol.
//     * @param direction Direction enumeration value.
//     * @return Direction string used by the protocol: "top", "bottom", 'right', "left".
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
//
//    /**
//     * Convert the direction string in the protocol to a Direction enumeration.
//     * @param dirString Direction string in the protocol: "top", "bottom", 'right', "left"
//     * @return Corresponding Direction enumeration value.
//     */
//    private Direction stringToDirection(String dirString) {
//        if (dirString == null) return null;
//        return switch (dirString) {
//            case "top" -> Direction.NORTH;
//            case "bottom" -> Direction.SOUTH;
//            case "right" -> Direction.EAST;
//            case "left" -> Direction.WEST;
//            default -> throw new IllegalArgumentException("Invalid direction string: " + dirString);
//        };
//    }

//    /**
//     * Serialize to protocol format
//     * @return Map that complies with the protocol
//     */
//    public Map<String, Object> serialize() {
//        Map<String, Object> result = new HashMap<>();
//        result.put("type", "Antenna");
//        result.put("isOnBoard", boardId);
//
//        if (direction != null) {
//            List<String> orientations = new ArrayList<>();
//            orientations.add(directionToString(direction));
//            result.put("orientations", orientations);
//        }
//
//        return result;
//    }
//
//    /**
//     * Create an antenna instance from the protocol representation.
//     * @param position Position
//     * @param orientation Direction string: "top", "bottom", 'right', "left"
//     * @param boardId Board ID
//     */
//    public Antenna(Position position, String orientation, String boardId) {
//        super(position, boardId);
//        if (orientation != null) {
//            this.direction = stringToDirection(orientation);
//        }
//        this.isOnBoard = true;
//        this.boardId = boardId;
//    }

}