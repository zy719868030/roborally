package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the gear element on the game board.
 * The gear rotates the robot standing on it during the activation phase.
 * There are two types of gears: clockwise rotation (green) and counterclockwise rotation (red).
 */
public class Gear extends BoardElement {

    public enum RotationDirection {
        CLOCKWISE,
        COUNTERCLOCKWISE
    }

    public enum GearColor {
        // Red gear rotates counterclockwise; Green gear rotates clockwise.
        RED,
        GREEN
    }

    private RotationDirection rotationDirection;
    private GearColor color;
    private String boardId;
    private boolean isOnBoard;

    public Gear() {
        super();
        this.rotationDirection = RotationDirection.CLOCKWISE;
        this.color = GearColor.GREEN;
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Gear(Position position, String boardId) {
        super(position, boardId);
        this.rotationDirection = RotationDirection.CLOCKWISE;
        this.color = GearColor.GREEN;
        this.isOnBoard = false;
        this.boardId = "";
    }


    /**
     * Constructor with position and rotation direction parameters
     *
     * @param position Gear position
     * @param rotationDirection Gear rotation direction
     */
    public Gear(Position position, RotationDirection rotationDirection, String boardId) {
        super(position, boardId);
        this.rotationDirection = rotationDirection;
        // Set colours based on rotation direction
        this.color = (rotationDirection == RotationDirection.CLOCKWISE) ? GearColor.GREEN : GearColor.RED;
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructor with position and colour parameters
     *
     * @param position Gear position
     * @param color Gear colour
     */
    public Gear(Position position, GearColor color, String boardId) {
        super(position, boardId);
        this.color = color;
        this.isOnBoard = false;
        this.boardId = "";
        // Set the rotation direction based on the colour
        this.rotationDirection = (color == GearColor.GREEN) ? RotationDirection.CLOCKWISE :
                RotationDirection.COUNTERCLOCKWISE;
    }

    /*
    public Gear(Position position, RotationDirection rotationDirection, String boardId) {
        super(position, boardId);
        this.rotationDirection = rotationDirection;
        // Set colours based on rotation direction
        this.color = (rotationDirection == RotationDirection.CLOCKWISE) ? GearColor.GREEN : GearColor.RED;
        this.setBoardId(boardId);
    }*/

    public RotationDirection getRotationDirection() {
        return rotationDirection;
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
     * Set the rotation direction of the gear.
     * Update the colour of the gear at the same time.
     *
     * @param rotationDirection New rotation direction.
     */
    public void setRotationDirection(RotationDirection rotationDirection) {
        this.rotationDirection = rotationDirection;
        this.color = (rotationDirection == RotationDirection.CLOCKWISE) ? GearColor.GREEN : GearColor.RED;
    }

    public GearColor getColor() {
        return color;
    }

    /**
     * Set the colour of the gear.
     * At the same time, update the rotation direction of the gear.
     *
     * @param color New colour.
     */
    public void setColor(GearColor color) {
        this.color = color;
        // Update rotation direction based on colour
        this.rotationDirection = (color == GearColor.GREEN) ? RotationDirection.CLOCKWISE :
                RotationDirection.COUNTERCLOCKWISE;
    }

    /**
     * When the robot is on the gear, the gear rotates the robot.
     *
     * @param robot The robot on the gear.
     */
    @Override
    public void activate(Robot robot) {
        // Rotate the robot according to the rotation direction of the gear.
        if (rotationDirection == RotationDirection.CLOCKWISE) {
            robot.turnRight();
            System.out.println("Green gear at " + position + " rotated Robot " + robot.getId() + " clockwise");
        } else {
            robot.turnLeft();
            System.out.println("Red gear at " + position + " rotated Robot " + robot.getId() + " counterclockwise");
        }
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "Gear";
    }

    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
    }

    @Override
    public String toString() {
        return (color == GearColor.GREEN ? "Green" : "Red") +
                " gear at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                ", rotation: " + (rotationDirection == RotationDirection.CLOCKWISE ? "clockwise" : "counterclockwise");
    }

//    /**
//     * Convert the RotationDirection enumeration to the direction string required by the protocol.
//     * @param direction RotationDirection enumeration value.
//     * @return The direction string used by the protocol: “clockwise” or “counterclockwise”.
//     */
//    private String rotationDirectionToString(RotationDirection direction) {
//        return direction == RotationDirection.CLOCKWISE ? "clockwise" : "counterclockwise";
//    }
//
//    /**
//     * Convert the direction string in the protocol to a RotationDirection enumeration.
//     * @param dirString Direction string in the protocol: “clockwise” or “counterclockwise”.
//     * @return Corresponding RotationDirection enumeration value.
//     */
//    private RotationDirection stringToRotationDirection(String dirString) {
//        return "clockwise".equals(dirString) ? RotationDirection.CLOCKWISE : RotationDirection.COUNTERCLOCKWISE;
//    }

//    /**
//     * Serialize to protocol format
//     * @return Map that complies with the protocol
//     */
//    public Map<String, Object> serialize() {
//        Map<String, Object> result = new HashMap<>();
//        result.put("type", "Gear");
//        result.put("isOnBoard", boardId);
//
//        List<String> orientations = new ArrayList<>();
//        orientations.add(rotationDirectionToString(rotationDirection));
//        result.put("orientations", orientations);
//
//        return result;
//    }

//    /**
//     * Create a gear instance from the protocol representation.
//     * @param position Position
//     * @param orientation Rotation direction string: “clockwise” or “counterclockwise”
//     * @param boardId Board ID
//     */
//    public Gear(Position position, String orientation, String boardId) {
//        super(position, boardId);
//        this.rotationDirection = stringToRotationDirection(orientation);
//        this.color = (rotationDirection == RotationDirection.CLOCKWISE) ? GearColor.GREEN : GearColor.RED;
//        this.isOnBoard = true;
//        this.boardId = boardId;
//    }

}
