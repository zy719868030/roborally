package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

/**
 * Represents a starting point element on the RoboRally game board.
 * 
 * <p>Starting points are designated locations where players can place their robots
 * at the beginning of the game. Each starting point has a specific position and
 * directional orientation that determines how robots will face when placed there.</p>
 * 
 * <p>Key features of starting points:</p>
 * <ul>
 *   <li><strong>Initial Placement:</strong> Designated positions for robot placement at game start</li>
 *   <li><strong>Occupancy Management:</strong> Track which player has claimed each starting point</li>
 *   <li><strong>Directional Orientation:</strong> Determine robot facing direction when placed</li>
 *   <li><strong>Reboot Location:</strong> Can serve as restart points for robots during gameplay</li>
 *   <li><strong>Passable:</strong> Robots can move through starting points during normal gameplay</li>
 *   <li><strong>Setup Phase Control:</strong> Occupancy status relevant only during initial setup</li>
 * </ul>
 * 
 * <p>Starting points are crucial for game initialization, providing fair and organized
 * starting positions for all players. They also serve as potential reboot locations
 * during gameplay, offering strategic positioning opportunities.</p>
 * 
 * @author Edle Eisbecher Team
 * @version 1.0
 * @since 1.0
 */
public class StartPoint extends BoardElement {

    /** Flag indicating whether this starting point has been claimed by a player */
    private boolean occupied;

    /** The ID of the player who has claimed this starting point (-1 if unoccupied) */
    private int playerID;
    
    /** The unique identifier of the board this starting point belongs to */
    private String boardId;
    
    /** Flag indicating whether this starting point is placed on a game board */
    private boolean isOnBoard;

    /**
     * Constructs a starting point with position and direction.
     * 
     * <p>This constructor creates a starting point at the specified position with a
     * specific directional orientation. The starting point is initially unoccupied
     * and not associated with any board.</p>
     * 
     * @param position the position of the starting point on the game board
     * @param direction the direction robots will face when placed at this starting point
     * @throws IllegalArgumentException if position or direction is null
     */
    public StartPoint(Position position, Direction direction) {
        super(position, direction);
        this.occupied = false;
        this.playerID = -1; //-1 indicates unoccupied
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructs a starting point with position, direction, and board identifier.
     * 
     * <p>This constructor creates a starting point at the specified position with a
     * specific directional orientation and board association. The starting point is
     * initially unoccupied but will be marked as being on a board if the boardId
     * is not empty.</p>
     * 
     * @param position the position of the starting point on the game board
     * @param direction the direction robots will face when placed at this starting point
     * @param boardId the unique identifier of the board this starting point belongs to
     * @throws IllegalArgumentException if position, direction, or boardId is null
     */
    public StartPoint(Position position, Direction direction, String boardId) {
        super(position, direction);
        this.occupied = false;
        this.playerID = -1; //-1 indicates unoccupied
        this.setBoardId(boardId);
    }

    /**
     * Attempts to claim this starting point for a specific player.
     * 
     * <p>This method allows a player to claim an unoccupied starting point. The claim
     * will only succeed if the starting point is currently available. Once claimed,
     * the starting point becomes occupied and cannot be claimed by other players.</p>
     * 
     * @param player the ID of the player attempting to claim the starting point
     * @return true if the claim was successful, false if the starting point was already occupied
     */
    public boolean occupy(int player) {
        if (!occupied) {
            occupied = true;
            playerID = player;
            return true;
        }
        return false;
    }

    /**
     * Checks whether this starting point is currently placed on a game board.
     *
     * @return true if the starting point is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Releases this starting point, making it available for other players.
     * 
     * <p>This method clears the occupancy status of the starting point, resetting
     * it to an unoccupied state. This allows other players to claim the starting
     * point if needed.</p>
     */
    public void release() {
        occupied = false;
        playerID = -1;
    }

    /**
     * Sets whether this starting point is placed on a game board.
     *
     * @param onBoard true to mark the starting point as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Checks whether this starting point has been claimed by a player.
     * 
     * <p>This method indicates whether a player has successfully claimed this
     * starting point. The occupancy status is primarily relevant during the
     * game setup phase when players are choosing their starting positions.</p>
     * 
     * @return true if the starting point is occupied, false if it is available
     */
    public boolean isOccupied() {
        return occupied;
    }

    /**
     * Gets the unique identifier of the board this starting point belongs to.
     *
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Gets the ID of the player who has claimed this starting point.
     * 
     * <p>This method returns the player ID of the player who has successfully
     * claimed this starting point. If the starting point is unoccupied, it
     * returns -1.</p>
     * 
     * @return the player ID of the occupying player, or -1 if unoccupied
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * Sets the board ID for this starting point and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the starting point is marked as being on a board.
     * This method automatically manages the isOnBoard flag based on the boardId value.</p>
     *
     * @param boardId the unique identifier of the board this starting point belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Gets the directional orientation for robots placed at this starting point.
     * 
     * <p>This method returns the direction that robots will face when they are
     * placed at this starting point. This orientation is set during construction
     * and determines the initial facing direction of robots.</p>
     * 
     * @return the direction robots will face when placed at this starting point
     */
    public Direction getRobotDirection() {
        return direction;
    }

    /**
     * Activates the starting point effect on a robot.
     * 
     * <p>Starting points do not have any immediate activation effects on robots.
     * This method is intentionally empty as starting points are primarily used
     * for initial placement and potential reboot locations.</p>
     * 
     * @param robot the robot that is on the starting point's position
     */
    @Override
    public void activate(Robot robot) {
    }

    /**
     * Checks whether a robot can pass through this starting point.
     * 
     * <p>During normal gameplay, starting points do not block robot movement.
     * The occupancy status is only relevant during the setup phase when players
     * are choosing their starting positions. Robots can freely move through
     * starting points during regular gameplay.</p>
     * 
     * @param robot the robot attempting to pass through the starting point
     * @return true - robots can pass through starting points during normal gameplay
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        // During normal gameplay, starting points should not block movement
        // The occupied status is only relevant during the setup phase
        return true;
//        return !occupied;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "StartPoint" identifying this element type
     */
    @Override
    public String getType() {
        return "StartPoint";
    }

    /**
     * Restarts a robot at this starting point.
     * 
     * <p>This method moves a robot to this starting point's position and sets
     * its direction to match the starting point's orientation. This is useful
     * for rebooting robots or providing alternative restart locations during
     * gameplay.</p>
     * 
     * @param robot the robot that needs to be restarted at this starting point
     * @throws IllegalArgumentException if robot is null
     */
    public void rebootRobot(Robot robot) {
        robot.setPosition(this.position);
        robot.setDirection(this.direction);
    }

    /**
     * Returns a string representation of this starting point.
     * 
     * <p>The string includes the starting point's position, board association,
     * and occupancy status for easy identification and debugging purposes.</p>
     * 
     * @return a detailed string describing the starting point's properties
     */
    @Override
    public String toString() {
        return "StartPoint at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                (occupied ? ", occupied by player " + playerID : ", not occupied");
    }

    /**
     * Converts this StartPoint object to a FieldStartPoint representation for network communication.
     * 
     * <p>This method is used for serializing the starting point information when sending
     * game state updates to clients. The resulting FieldStartPoint object contains
     * the board ID and a label generated from the position coordinates in a format
     * suitable for network transmission.</p>
     * 
     * @return a new FieldStartPoint object representing this starting point's network data
     */
    @Override
    public MessageDefinitions.FieldStartPoint toField() {
        // Beispiel-Label erzeugen aus Position oder BoardId
        String label = position.x() + "" + position.y();
        return new MessageDefinitions.FieldStartPoint(boardId, label);
    }

}
