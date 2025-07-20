package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.List;


/**
 * Represents a reboot/restart point element on the RoboRally game board.
 * 
 * <p>Reboot points serve as designated restart locations for robots that have fallen
 * off the board or been destroyed by pits. They provide a safe haven for robots to
 * return to the game and continue their mission.</p>
 *
 */
public class Reboot extends BoardElement {
    
    /** The unique identifier of the board this reboot point belongs to */
    private String boardId;
    
    /** Flag indicating whether this reboot point is placed on a game board */
    private boolean isOnBoard;
    
    // boardName for protocol's isOnBoard attribute
    private String boardName;

//    public Reboot() {
//        super(""); // Calls BoardElement() constructor
//        this.isOnBoard = false;
//        this.boardId = "";
//        //this.boardName = "";
//        // Private constructor for singleton
//
//    }
//    public Reboot(Position position) {
//        super(position, "");
//        this.isOnBoard = false;
//        this.boardId = "";
//    }

    /*
    public Reboot(Position position, String boardId) {
        super(position, boardId);
        this.setBoardId(boardId);
        this.boardName = boardId;
    }
    */

//    public Reboot(Position position, String boardId) {
//        super(position, boardId); // Calls BoardElement(Position, String)
//        this.boardId = boardId;
//        this.isOnBoard = !boardId.isEmpty();
//    }

    /**
     * Constructs a reboot point with position, direction, and board identifier.
     * 
     * <p>This constructor creates a reboot point at the specified position with a
     * specific directional orientation. The direction determines which way robots
     * will face when they restart at this location.</p>
     * 
     * @param position the position of the reboot point on the game board
     * @param direction the direction robots will face when restarting at this point
     * @param boardId the unique identifier of the board this reboot point belongs to
     * @throws IllegalArgumentException if position, direction, or boardId is null
     */
    public Reboot(Position position, Direction direction, String boardId) {
        super(position, direction, boardId);
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Checks whether this reboot point is currently placed on a game board.
     * 
     * @return true if the reboot point is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this reboot point is placed on a game board.
     * 
     * @param onBoard true to mark the reboot point as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this reboot point belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this reboot point and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the reboot point is marked as being on a board.
     * This method automatically manages the isOnBoard flag based on the boardId value.</p>
     * 
     * @param boardId the unique identifier of the board this reboot point belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    // Added: Setter for boardName
    //public void setBoardName(String boardName) { this.boardName = boardName; }

    // Added: Override getBoardName
    //@Override
    //public String getBoardName() {        return boardName;    }

    /**
     * Activates the reboot effect on a robot.
     * 
     * <p>This method is called when a robot is on the reboot point's position.
     * It applies reboot damage to the robot using the board's unified reboot
     * damage system. This damage represents the cost of restarting and helps
     * balance the game by penalizing robots that have fallen or been destroyed.</p>
     * 
     * @param robot the robot that is on the reboot point's position
     * @throws IllegalArgumentException if robot is null
     */
    @Override
    public void activate(Robot robot) {
        //WORM card-triggered rebirth uses a unified method
        Board board = robot.getBoard();
        if (board != null) {
            board.addRebootDamage(robot);
        }
        // Causes two points of SPAM damage when restarting.
//        robot.takeDamage(2);
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "Reboot" identifying this element type
     */
    @Override
    public String getType() {
        return "Reboot";
    }

    /**
     * Checks whether a robot can pass through this reboot point.
     * 
     * <p>Robots can freely move through reboot point positions. The reboot point
     * itself does not block movement, but will apply reboot effects during the
     * activation phase if a robot is on its position.</p>
     * 
     * @param robot the robot attempting to pass through the reboot point
     * @return true - robots can pass through reboot point positions
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Applies the complete reboot effect to a robot.
     * 
     * <p>This method handles the full reboot process:</p>
     * <ol>
     *   <li>Activates the reboot effect and applies damage</li>
     *   <li>Retrieves the designated reboot position from the board</li>
     *   <li>Moves the robot to the reboot position</li>
     *   <li>Updates the robot's position on the board</li>
     * </ol>
     * 
     * <p>The reboot process provides a safe restart mechanism for robots that
     * have fallen off the board or been destroyed by pits, allowing them to
     * continue playing the game.</p>
     * 
     * @param robot the robot to be rebooted
     * @param board the game board containing the reboot point
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
        Position rebootPosition = board.getRebootPosition();
        if (rebootPosition != null) {
            // Set robot position to restart point
            robot.setPosition(rebootPosition);
            board.updateRobotPosition(robot, rebootPosition);
            // Reset robot programming (cancel remaining registers for current round)
//            robot.cancelProgramming();
            System.out.println("Robot " + robot.getRobotID() + " has been rebooted at " + rebootPosition);
        } else {
            System.err.println("Error: No reboot position found on the board!");
        }
    }

    /**
     * Returns a string representation of this reboot point.
     * 
     * <p>The string includes the reboot point's position and board association
     * for easy identification and debugging purposes.</p>
     * 
     * @return a detailed string describing the reboot point's properties
     */
    @Override
    public String toString() {
            return "Reboot point at " + (position != null ? position.toString() : "unspecified position") +
                    (isOnBoard ? " on board " + boardId : " not on any board");
    }

    /**
     * Converts this Reboot object to a FieldRestartPoint representation for network communication.
     * 
     * <p>This method is used for serializing the reboot point information when sending
     * game state updates to clients. The resulting FieldRestartPoint object contains
     * the board ID and direction in a format suitable for network transmission.</p>
     * 
     * @return a new FieldRestartPoint object representing this reboot point's network data
     */
    @Override
    public MessageDefinitions.FieldRestartPoint toField() {
        return new MessageDefinitions.FieldRestartPoint(boardId, List.of(getDirection().toString()));
    }
}

