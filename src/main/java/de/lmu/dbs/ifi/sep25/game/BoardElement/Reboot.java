package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;

public class Reboot extends BoardElement {
    private String boardId;
    private boolean isOnBoard;
    private static Reboot instance = null;

    private Reboot() {
        this.isOnBoard = false;
        this.boardId = "";
        // Private constructor for singleton
    }
    public Reboot(Position position) {
        super(position);
        this.isOnBoard = false;
        this.boardId = "";
    }

    public Reboot(Position position, String boardId) {
        super(position);
        this.setBoardId(boardId);
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


    public static Reboot getInstance() {
        if (instance == null) {
            instance = new Reboot();
        }
        return instance;
    }

    @Override
    public void activate(Robot robot) {
        // Causes two points of SPAM damage when restarting.
        robot.takeDamage(2);
    }

    @Override
    public String getType() {
        return "Reboot";
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Apply restart effect to robot
     * When the robot falls off the game board or into a pit, it will restart immediately
     *
     * @param robot Robot to be restarted
     * @param board Game board
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
        Position rebootPosition = board.getRebootPosition();
        if (rebootPosition != null) {
            // Set robot position to restart point
            robot.setPosition(rebootPosition);

            // Reset robot programming (cancel remaining registers for current round)
            robot.cancelProgramming();

            System.out.println("Robot " + robot.getId() + " has been rebooted at " + rebootPosition);
        } else {
            System.err.println("Error: No reboot position found on the board!");
        }
    }

    /**
     * Get the string representation of the restart point.
     *
     * @return The string representation of the restart point.
     */
    @Override
    public String toString() {
            return "Reboot point at " + (position != null ? position.toString() : "unspecified position") +
                    (isOnBoard ? " on board " + boardId : " not on any board");
    }
}

