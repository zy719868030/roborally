package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;


/**
 * Represents the priority antenna in the game.
 * The priority antenna is used to determine the order of player actions and acts as an obstacle on the game board.
 */
public class Antenna extends BoardElement {
    private String boardId;
    private boolean isOnBoard;

    public Antenna(Position position, Direction direction) {

        super(position, direction);
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Antenna(Position position, Direction direction, String boardId) {
        super(position, direction);
        this.setBoardId(boardId);
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

    /**
     * The antenna itself does not activate the robot in any way.
     * It only serves as a reference point to determine the sequence of actions.
     */
    @Override
    public void activate(Robot robot) {
    }

    /**
     * Check whether the robot can pass through the antenna
     * According to the game rules, the robot cannot pass through the antenna
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return false;
    }

    /**
     * Get element type
     */
    @Override
    public String getType() {
        return "Antenna";
    }


    /**
     * Calculate the distance between the robot and the antenna.
     * Used to determine the priority order in the game.
     *
     * @param robotPosition The position of the robot.
     * @return The Manhattan distance between the robot and the antenna.
     */
    public int distanceToRobot(Position robotPosition) {
        return position.distanceTo(robotPosition);
    }

    /**
     * Sort robots with the same distance.
     * Imagine a straight line extending from the antenna direction, then rotate clockwise.
     * Determine priority based on the order in which the line touches the robots.
     *
     * @param robots List of robots with the same distance.
     * @return List of robots sorted by priority.
     */
    public List<Robot> sortTiedRobotsByPriority(List<Robot> robots) {
        if (robots.size() <= 1) {
            return robots;
        }

        // Get the antenna position as a reference point
        final Position antennaPos = this.position;
        final Direction antennaDir = this.direction;
        List<Robot> sortedRobots = new ArrayList<>(robots);


        //Calculate the angle relative to the antenna and sort in ascending order by angle (smaller angles first).
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
     * Calculate the angle of a point relative to the antenna and antenna direction.
     *
     * @param center Antenna position.
     * @param point Robot position.
     * @param direction Antenna direction.
     * @return Angle (radians).
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

    @Override
    public String toString() {
        return "Antenna at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                ", direction: " + direction.getName();
    }

//    /**
//     * Convert the Direction enumeration to the direction string required by the protocol.
//     * @param direction Direction enumeration value.
//     * @return Direction string used by the protocol: “top”, “bottom”, ‘right’, “left”.
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
//     * @param dirString Direction string in the protocol: “top”, “bottom”, ‘right’, “left”
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
//     * @param orientation Direction string: “top”, “bottom”, ‘right’, “left”
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