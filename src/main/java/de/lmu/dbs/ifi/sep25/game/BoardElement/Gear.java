package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.List;

/**
 * Represents the gear element on the game board.
 * The gear rotates the robot standing on it during the activation phase.
 * There are two types of gears: clockwise rotation (green) and counterclockwise rotation (red).
 */
public class Gear extends BoardElement {

    public enum RotationDirection {
        CLOCKWISE,
        COUNTERCLOCKWISE;

        @Override
        public String toString() {
            return switch (this) {
                case CLOCKWISE -> "clockwise";
                case COUNTERCLOCKWISE -> "counterclockwise";
            };
        }
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

    /**
     * Constructor with position and rotation direction parameters
     *
     * @param position          Gear position
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
     * @param color    Gear colour
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
            System.out.println("Green gear at " + position + " rotated Robot " + robot.getRobotID() + " clockwise");
        } else {
            robot.turnLeft();
            System.out.println("Red gear at " + position + " rotated Robot " + robot.getRobotID() + " counterclockwise");
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

    /**
     * Converts the current Gear object into a FieldGear representation.
     * The FieldGear object encapsulates the board ID and the rotation direction of the gear.
     *
     * @return a MessageDefinitions.FieldGear object containing the board ID and rotation direction of the gear.
     */
    @Override
    public MessageDefinitions.FieldGear toField() {
        return new MessageDefinitions.FieldGear(boardId, List.of(rotationDirection.toString()));
    }
}
