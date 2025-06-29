package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a floor tile in the game that serves as a basic passive element.
 * The Floor class is implemented as a singleton, meaning only one instance
 * of this class can exist during the runtime of the application.
 * <p>
 * The floor tile does not apply any effect on a robot or the board. It is primarily
 * used as a base tile in the game where no specific action or behavior is required.
 */
@SuppressWarnings("unused")
public class Floor extends BoardElement {
    private final static Map<String, Floor> INSTANCES = new HashMap<>();
    private String boardId;
    private boolean isOnBoard;

    private Floor(String boardId) {
        this.setIsOnBoard(true);
        this.setBoardId(boardId);
    }

    public static Floor createFloor(String boardId) {
        Floor floor = new Floor(boardId);
        INSTANCES.putIfAbsent(boardId, floor);
        return floor;
    }

    public static Floor getInstance(String boardId) {
        Floor floor = INSTANCES.getOrDefault(boardId, null);
        if (floor == null)
            throw new IllegalArgumentException("No Floor instance with boardId " + boardId + " exists!");
        return floor;
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
        // If boardId is not empty, set it to be on the board.
        this.isOnBoard = !boardId.isEmpty();
    }

    @Override
    public void activate(Robot robot) {
        // No effect
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "Floor";
    }

    public void applyEffect(Robot robot, Board board) {
        // No effect
        activate(robot);
    }

    @Override
    public String toString() {
        return "Floor at " + (position != null ? position.toString() : "unspecified position")
                + (isOnBoard ? " on board " + boardId : " not on any board");
    }

    /**
     * Converts this Floor instance into a FieldEmpty object from MessageDefinitions.
     * This method is used to represent the current Floor instance in a format
     * suitable for use in message definitions.
     *
     * @return A new instance of MessageDefinitions.FieldEmpty with the board ID of this Floor.
     */
    @Override
    public MessageDefinitions.FieldEmpty toField() {
        return new MessageDefinitions.FieldEmpty(boardId);
    }
}

