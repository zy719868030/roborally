package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a checkpoint element on the game board.
 * Checkpoints are target points that robots must visit in order.
 * Robots must visit checkpoints in numerical order, and the first player to visit all checkpoints wins.
 */
public class CheckPoints extends BoardElement {
    //number indicates the checkpoint number.（count）
    private int number;
    private final Map<Integer, Integer> robotCheckpoints;
    private String boardId;
    private boolean isOnBoard;

//    public CheckPoints() {
//        super();
//        this.number = 1;
//        this.robotCheckpoints = new HashMap<>();
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public CheckPoints(Position position, String boardId) {
//        super(position, boardId);
//        this.number = 1;
//        this.robotCheckpoints = new HashMap<>();
//        this.isOnBoard = false;
//        this.boardId = "";
//    }

    /*
    public CheckPoints(Position position, int number) {
        super(position);
        this.number = number;
        this.robotCheckpoints = new HashMap<>();
        this.isOnBoard = false;
        this.boardId = "";
    }
    */

    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");

    public CheckPoints(Position position, int number, String boardId) {
        super(position, boardId);
        this.number = number;
        this.robotCheckpoints = new HashMap<>();
        this.setBoardId(boardId);

        messageLogger.info("Checkpoints created at " + position + " with number " + getNumber() + " on board " + boardId);
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

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public int getRobotHighestCheckpoint(int robotId) {
        return robotCheckpoints.getOrDefault(robotId, 0);
    }

    /**
     * Record robot access checkpoints
     *
     * @param robotId Robot ID
     * @param checkpointNumber Access checkpoint number
     */
    public void recordCheckpoint(int robotId, int checkpointNumber) {
        // Only record the highest checkpoint number
        int currentHighest = robotCheckpoints.getOrDefault(robotId, 0);
        if (checkpointNumber > currentHighest) {
            robotCheckpoints.put(robotId, checkpointNumber);
        }
    }

    public void clearCheckpoints() {
        robotCheckpoints.clear();
    }

    /**
     * Checks whether the robot can access this checkpoint.
     * The robot must access the checkpoints in numerical order.
     *
     * @param robotId The robot's ID.
     * @return Returns true if the robot can access this checkpoint, otherwise returns false.
     */
    public boolean canVisit(int robotId) {
        int highestVisited = getRobotHighestCheckpoint(robotId);
        return number == highestVisited + 1;
    }

    @Override
    public void activate(Robot robot) {
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "CheckPoint";
    }

    /**
     * Apply checkpoint effect to robot
     * If robot can access this checkpoint, record it as visited
     *
     * @param robot Robot located on checkpoint
     * @param board Game board
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);

        // Check whether the robot can access this checkpoint.
        int robotId = robot.getRobotID();
        if (canVisit(robotId)) {
            recordCheckpoint(robotId, number);
            System.out.println("Robot " + robotId + " reached checkpoint " + number + "!");

            // Check whether all checkpoints have been visited
            if (hasVisitedAllCheckpoints(robotId, board)) {
                announceWinner(robotId);
            }
        }
    }

    /**
     * Check whether the robot has visited all checkpoints.
     *
     * @param robotId Robot ID.
     * @param board Game board.
     * @return Returns true if the robot has visited all checkpoints, otherwise returns false.
     */
    private boolean hasVisitedAllCheckpoints(int robotId, Board board) {
        // If the highest checkpoint number visited by the robot is equal to the total number,
        // it means that all checkpoints have been visited.
        int totalCheckpoints = board.getTotalCheckpoints();
        int highestVisited = getRobotHighestCheckpoint(robotId);
        return highestVisited == totalCheckpoints;
    }

    /**
     * Announce the winner
     *
     * @param robotId ID of the winning robot
     */
    private void announceWinner(int robotId) {
        System.out.println("Robot " + robotId + " has visited all checkpoints and won the game!");
        // Here can add game end logic.
        // z.B, game controller method to end the game.
    }

    @Override
    public String toString() {
        return "CheckPoint " + number + " at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board");
    }

    /**
     * Converts the current `CheckPoints` instance to a `MessageDefinitions.FieldCheckpoint` object.
     *
     * @return A new `MessageDefinitions.FieldCheckpoint` instance populated with the `boardId` and `number` fields of the current `CheckPoints` object.
     */
    @Override
    public MessageDefinitions.FieldCheckPoint toField() {
        return new MessageDefinitions.FieldCheckPoint(boardId, getNumber());
    }

}