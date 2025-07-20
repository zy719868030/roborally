package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.*;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

/**
 * Represents a pit element on the RoboRally game board.
 * 
 * <p>Pits are deadly hazards that cause robots to fall and trigger an immediate restart process.
 * When a robot moves onto a pit, it immediately falls in and must be rebooted at a designated
 * restart location on the board.</p>
 *
 */
public class Pit extends BoardElement {
    
    /** The unique identifier of the board this pit belongs to */
    private String boardId;
    
    /** Flag indicating whether this pit is placed on a game board */
    private boolean isOnBoard;

    /**
     * Constructs a default pit with no position or board association.
     * 
     * <p>This constructor creates a pit that is not associated with any board
     * and has no specific position. It is typically used for creating template
     * pit objects or during deserialization.</p>
     */
    public Pit() {
        super();
        this.isOnBoard = false;
        this.boardId = "";
    }

    /*public Pit(Position position) {
        super(position);
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Pit(Position position, String boardId) {
        super(position, boardId);
        this.setBoardId(boardId);
    }
    */

    /**
     * Constructs a pit with a specific position and board identifier.
     * 
     * <p>This constructor creates a pit at the specified position on the given board.
     * The pit will be marked as being on a board if the boardId is not empty.</p>
     * 
     * @param position the position of the pit on the game board
     * @param boardId the unique identifier of the board this pit belongs to
     * @throws IllegalArgumentException if position or boardId is null
     */
    public Pit(Position position, String boardId) {
        super(position, boardId);
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Checks whether this pit is currently placed on a game board.
     * 
     * @return true if the pit is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this pit is placed on a game board.
     * 
     * @param onBoard true to mark the pit as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this pit belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this pit and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the pit is marked as being on a board.
     * This method automatically manages the isOnBoard flag based on the boardId value.</p>
     * 
     * @param boardId the unique identifier of the board this pit belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Activates the pit effect when a robot enters the pit.
     * 
     * <p>This method is called when a robot moves onto the pit's position. It logs
     * the fall event and prepares for the restart process. The actual restart
     * is handled by the applyEffect method which calls the reboot process.</p>
     * 
     * @param robot the robot that entered the pit
     * @throws IllegalArgumentException if robot is null
     */
    @Override
    public void activate(Robot robot) {
        System.out.println("Robot " + robot.getRobotID() + " fell into a pit at " + position + "!");
    }

    /**
     * Checks whether a robot can pass through this pit.
     * 
     * <p>Robots can move onto pit positions, but doing so will trigger the fall
     * effect. The pit itself does not block movement, but the movement will
     * result in the robot falling and needing to restart.</p>
     * 
     * @param robot the robot attempting to pass through the pit
     * @return true - robots can move onto pit positions (triggers fall)
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "Pit" identifying this element type
     */
    @Override
    public String getType() {
        return "Pit";
    }

    /**
     * Applies the complete pit effect to a robot that has entered the pit.
     *
     * @param robot the robot that entered the pit
     * @param board the game board containing the pit
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
        rebootRobot(robot, board);
    }

    /**
     * Initiates the restart process for a robot that has fallen into the pit.
     * 
     * <p>This method delegates the restart process to the board's fall handling
     * system. The board will determine the appropriate restart location and
     * handle any associated penalties or damage that should be applied to
     * the fallen robot.</p>
     *
     * @param robot the robot that needs to be restarted
     * @param board the game board that will handle the restart process
     */
    private void rebootRobot(Robot robot, Board board) {
        board.handleFall(robot);
    }

    /**
     * Returns a string representation of this pit.
     * 
     * <p>The string includes the pit's position and board association for easy
     * identification and debugging purposes.</p>
     *
     * @return a detailed string describing the pit's properties
     */
    @Override
    public String toString() {
        return "Pit at " + position + (isOnBoard ? " on board " + boardId : " not on any board");
    }

    /**
     * Converts this Pit object to a FieldPit representation for network communication.
     * 
     * <p>This method is used for serializing the pit information when sending
     * game state updates to clients. The resulting FieldPit object contains
     * the board ID in a format suitable for network transmission.</p>
     * 
     * @return a new FieldPit object representing this pit's network data
     */
    @Override
    public MessageDefinitions.FieldPit toField() {
        return new MessageDefinitions.FieldPit(boardId);
    }
}