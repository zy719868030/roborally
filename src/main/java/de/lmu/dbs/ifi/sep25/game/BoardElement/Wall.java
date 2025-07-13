package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.ArrayList;
import java.util.List;

/**
 * The Walls class represents wall elements on the game board.
 * Walls prevent robots from passing through and can have multiple orientations
 * (i.e., walls can block movement in multiple directions).
 */
public class Wall extends BoardElement {
    private List<Direction> blockedDirections;
    private String boardId;
    private boolean isOnBoard;

    public Wall() {
        super();
        this.blockedDirections = new ArrayList<>();
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Wall(Position position, String boardId) {
        super(position, boardId);
        this.blockedDirections = new ArrayList<>();
        this.isOnBoard = false;
        this.boardId = "";
    }

    //Constructor with position and blocking direction parameters
    public Wall(Position position, Direction blockedDirection, String boardId) {
        super(position, boardId);
        this.blockedDirections = new ArrayList<>();
        this.blockedDirections.add(blockedDirection);
        this.isOnBoard = false;
        this.boardId = "";
    }

    //Constructor with position and multiple blocking direction parameters
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

    public void addBlockedDirection(Direction direction) {
        if (!blockedDirections.contains(direction)) {
            blockedDirections.add(direction);
        }
    }

    public List<Direction> getBlockedDirections() {
        return new ArrayList<>(blockedDirections);
    }

    public boolean isDirectionBlocked(Direction direction) {
        return blockedDirections.contains(direction);
    }


    /**
     * When the robot enters a grid containing a wall, no special effect is triggered.
     * The main purpose of walls is to block movement, not to trigger effects when entered.
     *
     * @param robot The robot entering this element.
     */
    @Override
    public void activate(Robot robot) {
        // Walls do not trigger effects when robots enter them.
        // The blocking effect of walls should be handled in the movement logic.
    }

    /**
     * Checks whether the robot can pass through the wall.
     * This method is called when checking if a robot can enter a cell with this wall.
     * The wall blocks movement if the wall is configured to block entry from the robot's current position.
     *
     * @param robot The robot attempting to pass through.
     * @return Returns true if it can pass through, otherwise returns false.
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
     * Checks whether the robot can pass through the wall from the specified direction.
     *
     * @param fromDirection The direction from which the robot attempts to pass through.
     * @return Returns true if it can pass through, otherwise returns false.
     */
    public boolean canPassThroughFromDirection(Direction fromDirection) {
//        return !isDirectionBlocked(fromDirection);
        String fromDirStr = fromDirection.toString().toLowerCase();
        for (Direction blockedDir : blockedDirections) {
            if (blockedDir.toString().toLowerCase().equals(fromDirStr)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks whether the robot can exit this cell in the specified direction.
     * Used to check if a wall on the current cell blocks movement out.
     *
     * @param toDirection The direction in which the robot attempts to exit this cell.
     * @return Returns true if it can exit, otherwise returns false.
     */
    public boolean canExitToDirection(Direction toDirection) {
        // If the wall blocks the direction to which the robot is exiting, it cannot pass
//        return !isDirectionBlocked(toDirection);
        String toDirStr = toDirection.toString().toLowerCase();
        for (Direction blockedDir : blockedDirections) {
            if (blockedDir.toString().toLowerCase().equals(toDirStr)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Get element type
     *
     * @return ‘Wall’ string
     */
    @Override
    public String getType() {
        return "Wall";
    }


    /**
     * Get the string representation of the wall.
     *
     * @return The string representation of the wall, including its position and blocking direction.
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
     * Converts the current Wall object to a FieldWall object representation.
     * This includes the board ID and a list of blocked directions as strings.
     *
     * @return a FieldWall object containing the board ID and the blocked directions of the wall.
     */
    @Override
    public MessageDefinitions.FieldWall toField() {
        return new MessageDefinitions.FieldWall(boardId, blockedDirections.stream().map(Direction::toString).toList());
    }


}
