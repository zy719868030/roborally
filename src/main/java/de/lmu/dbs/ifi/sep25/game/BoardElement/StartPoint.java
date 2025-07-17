package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

/**
 * Represents the starting points in the game.
 * Players can choose to place robots at these starting points at the beginning of the game.
 */
public class StartPoint extends BoardElement {

    // Mark whether this starting point has been occupied
    private boolean occupied;

    // Record the player ID occupying this starting point
    private int playerID;
    private String boardId;
    private boolean isOnBoard;

    /**
     * Create a starting point.
     *
     * @param position Starting point position.
     * @param direction Direction the robot faces when placed at this starting point.
     */
    public StartPoint(Position position, Direction direction) {
        super(position, direction);
        this.occupied = false;
        this.playerID = -1; //-1 indicates unoccupied
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Create a starting point with board information.
     *
     * @param position Starting point position.
     * @param direction Direction the robot faces when placed at this starting point.
     * @param boardId The ID of the board this starting point is on.
     */
    public StartPoint(Position position, Direction direction, String boardId) {
        super(position, direction);
        this.occupied = false;
        this.playerID = -1; //-1 indicates unoccupied
        this.setBoardId(boardId);
    }

    /**
     * Attempt to occupy this starting point.
     *
     * @param player Player ID attempting to occupy.
     * @return Returns true if the occupation is successful, otherwise returns false.
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
     * Check if this starting point is on a board.
     *
     * @return Returns true if this starting point is on a board, otherwise returns false.
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Release this starting point
     */
    public void release() {
        occupied = false;
        playerID = -1;
    }

    /**
     * Set whether this starting point is on a board.
     *
     * @param onBoard True if this starting point is on a board, otherwise false.
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }


    /**
     * Check whether this starting point has already been occupied.
     *
     * @return Returns true if it has already been occupied, otherwise returns false.
     */
    public boolean isOccupied() {
        return occupied;
    }

    /**
     * Get the ID of the board this starting point is on.
     *
     * @return Board ID.
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Get the player ID occupying this starting point.
     *
     * @return Player ID. If not occupied, return -1.
     */
    public int getPlayerID() {
        return playerID;
    }

    /**
     * Set the ID of the board this starting point is on.
     *
     * @param boardId Board ID.
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }



    /**
     * Get the orientation of the robot when it is placed at this starting point.
     *
     * @return The orientation of the robot.
     */
    public Direction getRobotDirection() {
        return direction;
    }

    /**
     * The starting point itself does not activate the robot.
     */
    @Override
    public void activate(Robot robot) {
    }


    /**
     * Check whether the robot can pass through the starting point.
     * If the starting point is occupied, the robot cannot pass through.
     * If the starting point is not occupied, the robot can pass through.
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        // During normal gameplay, starting points should not block movement
        // The occupied status is only relevant during the setup phase
        return true;
//        return !occupied;
    }

    /**
     * Get element type
     */
    @Override
    public String getType() {
        return "StartPoint";
    }

    /**
     * Restart the robot at this starting point.
     *
     * @param robot The robot that needs to be restarted.
     */
    public void rebootRobot(Robot robot) {
        robot.setPosition(this.position);
        robot.setDirection(this.direction);
    }

    @Override
    public String toString() {
        return "StartPoint at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                (occupied ? ", occupied by player " + playerID : ", not occupied");
    }


    /**
     * Converts this starting point into a {@link MessageDefinitions.FieldStartPoint} representation.
     *
     * @return A FieldStartPoint object corresponding to this starting point, initialized with the associated board ID.
     */

    @Override
    public MessageDefinitions.FieldStartPoint toField() {
        // Beispiel-Label erzeugen aus Position oder BoardId
        String label = position.x() + "" + position.y();
        return new MessageDefinitions.FieldStartPoint(boardId, label);
    }



}
