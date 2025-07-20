package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a floor tile element on the RoboRally game board.
 * 
 * <p>The Floor class is implemented as a singleton pattern per board, meaning only one
 * instance of this class can exist per board during the runtime of the application.
 * Floor tiles serve as the basic foundation elements of the game board.</p>
 *
 */
@SuppressWarnings("unused")
public class Floor extends BoardElement {
    
    /** Map storing singleton instances of Floor for each board */
    private final static Map<String, Floor> INSTANCES = new HashMap<>();
    
    /** The unique identifier of the board this floor tile belongs to */
    private String boardId;
    
    /** Flag indicating whether this floor tile is placed on a game board */
    private boolean isOnBoard;

    /**
     * Private constructor for creating a floor tile instance.
     * 
     * <p>This constructor is private to enforce the singleton pattern. Floor tiles
     * are created through the static factory methods to ensure only one instance
     * exists per board.</p>
     * 
     * @param boardId the unique identifier of the board this floor tile belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    private Floor(String boardId) {
        this.setIsOnBoard(true);
        this.setBoardId(boardId);
    }

    /**
     * Creates a new floor tile instance for the specified board.
     * 
     * <p>This factory method creates a new floor tile if one doesn't already exist
     * for the given board. If a floor tile already exists for the board, the
     * existing instance is returned, maintaining the singleton pattern.</p>
     * 
     * <p>This method is typically used when initializing a new board or when
     * a floor tile is needed for a board that doesn't have one yet.</p>
     * 
     * @param boardId the unique identifier of the board to create a floor tile for
     * @return a floor tile instance for the specified board
     * @throws IllegalArgumentException if boardId is null
     */
    public static Floor createFloor(String boardId) {
        Floor floor = new Floor(boardId);
        INSTANCES.putIfAbsent(boardId, floor);
        return floor;
    }

    /**
     * Gets the existing floor tile instance for the specified board.
     * 
     * <p>This method retrieves the singleton floor tile instance for the given board.
     * If no floor tile exists for the board, an exception is thrown. Use createFloor()
     * to create a new instance if needed.</p>
     * 
     * <p>This method is typically used when accessing an existing floor tile
     * that was previously created for a board.</p>
     * 
     * @param boardId the unique identifier of the board to get the floor tile for
     * @return the existing floor tile instance for the specified board
     * @throws IllegalArgumentException if no floor tile exists for the given boardId
     */
    public static Floor getInstance(String boardId) {
        Floor floor = INSTANCES.getOrDefault(boardId, null);
        if (floor == null)
            throw new IllegalArgumentException("No Floor instance with boardId " + boardId + " exists!");
        return floor;
    }

    /**
     * Checks whether this floor tile is currently placed on a game board.
     * 
     * @return true if the floor tile is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this floor tile is placed on a game board.
     * 
     * @param onBoard true to mark the floor tile as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this floor tile belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this floor tile and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the floor tile is marked as being on a board.
     * This method is used to associate the floor tile with a specific game board.</p>
     * 
     * @param boardId the unique identifier of the board this floor tile belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        // If boardId is not empty, set it to be on the board.
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Activates the floor tile effect on a robot.
     * 
     * <p>Floor tiles have no activation effect when robots enter them. They serve
     * as neutral foundation elements that don't interact with robots.</p>
     * 
     * @param robot the robot to activate the floor tile effect on (unused)
     */
    @Override
    public void activate(Robot robot) {
        // No effect
    }

    /**
     * Checks whether a robot can pass through this floor tile.
     * 
     * <p>Robots can freely move through floor tiles. The floor tile itself
     * does not block movement and serves as a neutral foundation element.</p>
     * 
     * @param robot the robot attempting to pass through the floor tile
     * @return true - robots can pass through floor tiles
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "Floor" identifying this element type
     */
    @Override
    public String getType() {
        return "Floor";
    }

    /**
     * Applies the floor tile effect to a robot during the activation phase.
     * 
     * <p>Floor tiles have no effect on robots during the activation phase. They serve
     * as neutral foundation elements that provide a base for other board elements
     * without adding any special behavior.</p>
     * 
     * @param robot the robot on the floor tile
     * @param board the game board containing the floor tile
     */
    public void applyEffect(Robot robot, Board board) {
        // No effect
        activate(robot);
    }

    /**
     * Returns a string representation of this floor tile.
     * 
     * <p>The string includes the floor tile's position (if set) and board association
     * status for easy identification and debugging.</p>
     * 
     * @return a detailed string describing the floor tile's properties
     */
    @Override
    public String toString() {
        return "Floor at " + (position != null ? position.toString() : "unspecified position")
                + (isOnBoard ? " on board " + boardId : " not on any board");
    }

    /**
     * Converts this Floor object to a FieldEmpty representation for network communication.
     * 
     * <p>This method is used for serializing the floor tile information when sending
     * game state updates to clients. Floor tiles are represented as empty fields
     * in the network protocol since they have no special properties.</p>
     * 
     * <p>The resulting FieldEmpty object contains only the board ID, reflecting
     * the passive nature of floor tiles.</p>
     * 
     * @return a new FieldEmpty object representing this floor tile's network data
     */
    @Override
    public MessageDefinitions.FieldEmpty toField() {
        return new MessageDefinitions.FieldEmpty(boardId);
    }
}

