package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a gear element on the RoboRally game board.
 * 
 * <p>Gears are mechanical elements that automatically rotate robots during the activation phase.
 * They come in two colors with corresponding rotation directions, providing strategic movement
 * control and challenges for players.</p>
 *
 */
public class Gear extends BoardElement {
    
    /** Logger for gear rotation events */
    private static final Logger logger = Logger.getLogger(Gear.class.getName());

    /**
     * Enumeration representing the rotation direction of gears.
     * 
     * <p>The rotation direction determines how robots are turned when they stand on the gear
     * during the activation phase. This affects the robot's facing direction and subsequent
     * movement capabilities.</p>
     * 
     * @author Edle Eisbecher Team
     * @version 1.0
     */
    public enum RotationDirection {
        /** Clockwise rotation (90 degrees to the right) */
        CLOCKWISE,
        /** Counterclockwise rotation (90 degrees to the left) */
        COUNTERCLOCKWISE;

        /**
         * Returns a string representation of the rotation direction.
         * 
         * @return "clockwise" for CLOCKWISE, "counterclockwise" for COUNTERCLOCKWISE
         */
        @Override
        public String toString() {
            return switch (this) {
                case CLOCKWISE -> "clockwise";
                case COUNTERCLOCKWISE -> "counterclockwise";
            };
        }
    }

    /**
     * Enumeration representing the color of gears.
     * 
     * <p>The gear color is directly linked to its rotation direction and provides
     * visual distinction between different types of gears on the game board.</p>
     * 
     * @author Edle Eisbecher Team
     * @version 1.0
     */
    public enum GearColor {
        /** Red gear that rotates robots counterclockwise */
        RED,
        /** Green gear that rotates robots clockwise */
        GREEN
    }

    /** The rotation direction of this gear (clockwise or counterclockwise) */
    private RotationDirection rotationDirection;
    
    /** The color of this gear (red or green) */
    private GearColor color;
    
    /** The unique identifier of the board this gear belongs to */
    private String boardId;
    
    /** Flag indicating whether this gear is placed on a game board */
    private boolean isOnBoard;

    /**
     * Constructs a gear with position and rotation direction parameters.
     * 
     * <p>This constructor creates a gear with a specific rotation direction. The color
     * is automatically determined based on the rotation direction: clockwise gears
     * are green, counterclockwise gears are red.</p>
     * 
     * @param position the position of the gear on the game board
     * @param rotationDirection the rotation direction of the gear
     * @param boardId the unique identifier of the board this gear belongs to
     * @throws IllegalArgumentException if position, rotationDirection, or boardId is null
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
     * Constructs a gear with position and color parameters.
     * 
     * <p>This constructor creates a gear with a specific color. The rotation direction
     * is automatically determined based on the color: green gears rotate clockwise,
     * red gears rotate counterclockwise.</p>
     * 
     * @param position the position of the gear on the game board
     * @param color the color of the gear
     * @param boardId the unique identifier of the board this gear belongs to
     * @throws IllegalArgumentException if position, color, or boardId is null
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

    /**
     * Gets the rotation direction of this gear.
     * 
     * @return the rotation direction (CLOCKWISE or COUNTERCLOCKWISE)
     */
    public RotationDirection getRotationDirection() {
        return rotationDirection;
    }

    /**
     * Checks whether this gear is currently placed on a game board.
     * 
     * @return true if the gear is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this gear is placed on a game board.
     * 
     * @param onBoard true to mark the gear as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this gear belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this gear and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the gear is marked as being on a board.</p>
     * 
     * @param boardId the unique identifier of the board this gear belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Sets the rotation direction of the gear and updates the color accordingly.
     * 
     * <p>When the rotation direction is changed, the color is automatically updated
     * to maintain consistency: clockwise gears become green, counterclockwise gears
     * become red.</p>
     * 
     * @param rotationDirection the new rotation direction for the gear
     * @throws IllegalArgumentException if rotationDirection is null
     */
    public void setRotationDirection(RotationDirection rotationDirection) {
        this.rotationDirection = rotationDirection;
        this.color = (rotationDirection == RotationDirection.CLOCKWISE) ? GearColor.GREEN : GearColor.RED;
    }

    /**
     * Gets the color of this gear.
     * 
     * @return the gear color (RED or GREEN)
     */
    public GearColor getColor() {
        return color;
    }

    /**
     * Sets the color of the gear and updates the rotation direction accordingly.
     * 
     * <p>When the color is changed, the rotation direction is automatically updated
     * to maintain consistency: green gears rotate clockwise, red gears rotate
     * counterclockwise.</p>
     * 
     * @param color the new color for the gear
     * @throws IllegalArgumentException if color is null
     */
    public void setColor(GearColor color) {
        this.color = color;
        // Update rotation direction based on colour
        this.rotationDirection = (color == GearColor.GREEN) ? RotationDirection.CLOCKWISE :
                RotationDirection.COUNTERCLOCKWISE;
    }

    /**
     * Activates the gear effect on a robot, causing it to rotate.
     * 
     * <p>When a robot stands on a gear during the activation phase, this method
     * rotates the robot according to the gear's rotation direction. The rotation
     * changes the robot's facing direction by 90 degrees, which affects subsequent
     * movement and actions.</p>
     * 
     * <p>The rotation is logged for debugging and game state tracking purposes.</p>
     * 
     * @param robot the robot standing on the gear
     * @throws IllegalArgumentException if robot is null
     */
    @Override
    public void activate(Robot robot) {
        // Rotate the robot according to the rotation direction of the gear.
        if (rotationDirection == RotationDirection.CLOCKWISE) {
            robot.turnRight();
            logger.info("Green gear at " + position + " rotated Robot " + robot.getClientID() + " clockwise");
        } else {
            robot.turnLeft();
            logger.info("Red gear at " + position + " rotated Robot " + robot.getClientID() + " counterclockwise");
        }
    }

    /**
     * Checks whether a robot can pass through this gear.
     * 
     * <p>Robots can freely move through gears. The gear itself does not block movement,
     * but will rotate robots when they are on it during the activation phase.</p>
     * 
     * @param robot the robot attempting to pass through the gear
     * @return true - robots can pass through gears
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "Gear" identifying this element type
     */
    @Override
    public String getType() {
        return "Gear";
    }

    /**
     * Applies the gear effect to a robot during the activation phase.
     * 
     * <p>This method delegates to the activate method to perform the actual rotation.
     * Gears have their effect applied during the activation phase when robots are
     * on their position.</p>
     * 
     * @param robot the robot on the gear
     * @param board the game board containing the gear
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
    }

    /**
     * Returns a string representation of this gear.
     * 
     * <p>The string includes the gear's color, position, board association,
     * and rotation direction for easy identification and debugging.</p>
     * 
     * @return a detailed string describing the gear's properties
     */
    @Override
    public String toString() {
        return (color == GearColor.GREEN ? "Green" : "Red") +
                " gear at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                ", rotation: " + (rotationDirection == RotationDirection.CLOCKWISE ? "clockwise" : "counterclockwise");
    }

    /**
     * Converts this Gear object to a FieldGear representation for network communication.
     * 
     * <p>This method is used for serializing the gear information when sending
     * game state updates to clients. The resulting FieldGear object contains
     * the board ID and rotation direction in a format suitable for network transmission.</p>
     * 
     * @return a new FieldGear object representing this gear's network data
     */
    @Override
    public MessageDefinitions.FieldGear toField() {
        return new MessageDefinitions.FieldGear(boardId, List.of(rotationDirection.toString()));
    }
}
