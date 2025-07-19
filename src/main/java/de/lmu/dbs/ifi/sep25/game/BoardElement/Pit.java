package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.*;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

/**
 * Represents a pit element on the game board.
 * When the robot moves onto a pit, it immediately falls into the pit and triggers the restart process.
 */
public class Pit extends BoardElement {
    private String boardId;
    private boolean isOnBoard;

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

    public Pit(Position position, String boardId) {
        super(position, boardId);
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
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
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * When the robot enters the pit, it immediately falls into the pit.
     * This triggers the restart process.
     *
     * @param robot The robot that entered the pit.
     */
    @Override
    public void activate(Robot robot) {
        System.out.println("Robot " + robot.getRobotID() + " fell into a pit at " + position + "!");
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "Pit";
    }

    /**
     * Apply pit effect to robot
     * When robot moves onto pit, trigger restart process
     *
     * @param robot Robot entering pit
     * @param board Game board
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
        rebootRobot(robot, board);
    }

    /**
     * Restart the robot that has fallen into the pit.
     *
     * @param robot The robot that needs to be restarted.
     * @param board The game board.
     */
    private void rebootRobot(Robot robot, Board board) {
        board.handleFall(robot);
    }

    /**
     * Get the string representation of the pit.
     *
     * @return The string representation of the pit, including its position.
     */
    @Override
    public String toString() {
        return "Pit at " + position + (isOnBoard ? " on board " + boardId : " not on any board");
    }

    /**
     * Converts the pit information into a FieldPit message definition.
     *
     * @return A FieldPit message definition representing the pit on the board.
     */
    @Override
    public MessageDefinitions.FieldPit toField() {
        return new MessageDefinitions.FieldPit(boardId);
    }
}