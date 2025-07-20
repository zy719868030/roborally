package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a wall element on the RoboRally game board.
 * 
 * <p>Walls are solid barriers that prevent robots from moving through them in specific
 * directions. They can have multiple orientations and block movement from various
 * directions, creating complex navigation challenges for players.</p>
 *
 */
public class Wall extends BoardElement {
    
    /** List of directions that are blocked by this wall */
    private List<Direction> blockedDirections;
    
    /** The unique identifier of the board this wall belongs to */
    private String boardId;
    
    /** Flag indicating whether this wall is placed on a game board */
    private boolean isOnBoard;

    /**
     * Constructs a default wall with no position or blocking directions.
     * 
     * <p>This constructor creates a wall that is not associated with any board
     * and has no blocking directions. It is typically used for creating template
     * wall objects or during deserialization.</p>
     */
    public Wall() {
        super();
        this.blockedDirections = new ArrayList<>();
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructs a wall with position and board identifier.
     * 
     * <p>This constructor creates a wall at the specified position with no blocking
     * directions. The wall will be marked as being on a board if the boardId is not empty.</p>
     * 
     * @param position the position of the wall on the game board
     * @param boardId the unique identifier of the board this wall belongs to
     * @throws IllegalArgumentException if position or boardId is null
     */
    public Wall(Position position, String boardId) {
        super(position, boardId);
        this.blockedDirections = new ArrayList<>();
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructs a wall with position, single blocking direction, and board identifier.
     * 
     * <p>This constructor creates a wall that blocks movement in one specific direction.
     * The wall will prevent robots from moving in the specified direction.</p>
     * 
     * @param position the position of the wall on the game board
     * @param blockedDirection the direction that is blocked by this wall
     * @param boardId the unique identifier of the board this wall belongs to
     * @throws IllegalArgumentException if position, blockedDirection, or boardId is null
     */
    public Wall(Position position, Direction blockedDirection, String boardId) {
        super(position, boardId);
        this.blockedDirections = new ArrayList<>();
        this.blockedDirections.add(blockedDirection);
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructs a wall with position, multiple blocking directions, and board identifier.
     * 
     * <p>This constructor creates a wall that blocks movement in multiple directions.
     * The wall will prevent robots from moving in any of the specified directions,
     * creating a more complex barrier.</p>
     * 
     * @param position the position of the wall on the game board
     * @param blockedDirections list of directions that are blocked by this wall
     * @param boardId the unique identifier of the board this wall belongs to
     * @throws IllegalArgumentException if position, blockedDirections, or boardId is null
     */
    public Wall(Position position, List<Direction> blockedDirections, String boardId) {
        super(position, boardId);
        this.blockedDirections = new ArrayList<>(blockedDirections);
        this.isOnBoard = false;
        this.boardId = "";
    }
    /*
    public Wall(Position position, List<Direction> blockedDirections, String boardId) {
        super(position);
        this.blockedDirections = new ArrayList<>(blockedDirections);
        this.setBoardId(boardId);
    }

   */

    /**
     * Checks whether this wall is currently placed on a game board.
     * 
     * @return true if the wall is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this wall is placed on a game board.
     * 
     * @param onBoard true to mark the wall as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this wall belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this wall and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the wall is marked as being on a board.
     * This method automatically manages the isOnBoard flag based on the boardId value.</p>
     * 
     * @param boardId the unique identifier of the board this wall belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Adds a direction to the list of blocked directions.
     * 
     * <p>If the direction is not already in the blocked directions list, it will be added.
     * This allows dynamic configuration of which directions the wall should block.</p>
     * 
     * @param direction the direction to add to the blocked directions
     * @throws IllegalArgumentException if direction is null
     */
    public void addBlockedDirection(Direction direction) {
        if (!blockedDirections.contains(direction)) {
            blockedDirections.add(direction);
        }
    }

    /**
     * Gets a copy of the list of blocked directions for this wall.
     * 
     * <p>The returned list contains all directions that are blocked by this wall.
     * Robots cannot move through the wall in any of these directions.</p>
     * 
     * @return a new ArrayList containing the blocked directions
     */
    public List<Direction> getBlockedDirections() {
        return new ArrayList<>(blockedDirections);
    }

    /**
     * Checks whether a specific direction is blocked by this wall.
     * 
     * <p>This method determines if robots can move in the specified direction
     * through this wall by checking if the direction is in the blocked directions list.</p>
     * 
     * @param direction the direction to check for blocking
     * @return true if the direction is blocked, false if movement is allowed
     */
    public boolean isDirectionBlocked(Direction direction) {
        return blockedDirections.contains(direction);
    }

    /**
     * Activates the wall effect on a robot.
     * 
     * <p>Walls do not have any immediate activation effects on robots. This method
     * is intentionally empty as walls are primarily used to block movement rather
     * than trigger effects when robots are on their position.</p>
     * 
     * @param robot the robot that is on the wall's position
     */
    @Override
    public void activate(Robot robot) {
        // Walls do not trigger effects when robots enter them.
        // The blocking effect of walls should be handled in the movement logic.
    }

    /**
     * Checks whether a robot can pass through this wall.
     * 
     * <p>This method always returns true because walls do not block robots from
     * being on their position. The actual blocking logic is handled by the
     * canPassThroughFromDirection method, which checks if the wall blocks
     * movement from a specific direction.</p>
     * 
     * @param robot the robot attempting to pass through the wall
     * @return true - robots can be on wall positions, blocking is handled elsewhere
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        // Walls on a cell block entry from specific directions
        // Robot can stand on a cell with walls, but cannot enter if the wall blocks that direction
        // Since this method doesn't know from which direction the robot is coming,
        // it should always return true. The actual blocking logic should be in canPassThroughFromDirection
        return true;
    }

    /**
     * Checks whether a robot can pass through this wall from a specific direction.
     * 
     * <p>This method determines if a robot can move through the wall when approaching
     * from the specified direction. It checks if the direction is in the blocked
     * directions list.</p>
     * 
     * @param fromDirection the direction from which the robot attempts to pass through
     * @return true if the robot can pass through from this direction, false if blocked
     */
    public boolean canPassThroughFromDirection(Direction fromDirection) {
        return !isDirectionBlocked(fromDirection);
    }

    /**
     * Checks whether a robot can exit this cell in a specific direction.
     * 
     * <p>This method is used to check if a wall on the current cell blocks movement
     * out of the cell in the specified direction. It checks if the direction is
     * in the blocked directions list.</p>
     * 
     * @param toDirection the direction in which the robot attempts to exit this cell
     * @return true if the robot can exit in this direction, false if blocked
     */
    public boolean canExitToDirection(Direction toDirection) {
        // If the wall blocks the direction to which the robot is exiting, it cannot pass
        return !isDirectionBlocked(toDirection);
    }

    /**
     * Gets the type identifier for this board element.
     *
     * @return the string "Wall" identifying this element type
     */
    @Override
    public String getType() {
        return "Wall";
    }

    /**
     * Returns a string representation of this wall.
     * 
     * <p>The string includes the wall's position, board association, and all
     * blocked directions for easy identification and debugging purposes.</p>
     *
     * @return a detailed string describing the wall's properties
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Wall at ").append(position);

        if (isOnBoard) {
            sb.append(" on board ").append(boardId);
        } else {
            sb.append(" not on any board");
        }

        sb.append(", blocking directions: ");

        for (Direction dir : blockedDirections) {
            sb.append(dir.getName()).append(", ");
        }

        if (!blockedDirections.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    /**
     * Converts this Wall object to a FieldWall representation for network communication.
     * 
     * <p>This method is used for serializing the wall information when sending
     * game state updates to clients. The resulting FieldWall object contains
     * the board ID and a list of blocked directions as strings in a format
     * suitable for network transmission.</p>
     * 
     * @return a new FieldWall object representing this wall's network data
     */
    @Override
    public MessageDefinitions.FieldWall toField() {
        return new MessageDefinitions.FieldWall(boardId, blockedDirections.stream().map(Direction::toString).toList());
    }

}
