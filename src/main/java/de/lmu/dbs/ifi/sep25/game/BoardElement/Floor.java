package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

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
    private static final Floor INSTANCE = new Floor();
    private String boardId;
    private boolean isOnBoard;

    private Floor() {
        this.isOnBoard = false;
        this.boardId = "";
    }

    public static Floor getInstance() {
        return INSTANCE;
    }

    public static Floor createFloor(Position position, String boardId) {
        Floor floor = new Floor();
        floor.setPosition(position);
        floor.setBoardId(boardId);
        floor.setIsOnBoard(true);
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
}

