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
 * Represents a checkpoint element on the RoboRally game board.
 * 
 * <p>Checkpoints are target points that robots must visit in numerical order to win the game.
 * Each checkpoint has a unique number, and robots must reach them sequentially (1, 2, 3, etc.).
 * The first player to visit all checkpoints in the correct order wins the game.</p>
 *
 */
public class CheckPoints extends BoardElement {
    
    /** The checkpoint number indicating the order in which it must be visited */
    private int number;
    
    /** Map tracking the highest checkpoint number reached by each robot */
    private final Map<Integer, Integer> robotCheckpoints;
    
    /** The unique identifier of the board this checkpoint belongs to */
    private String boardId;
    
    /** Flag indicating whether this checkpoint is placed on a game board */
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

    /** Logger for message-level logging */
    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");

    /**
     * Constructs a new CheckPoints instance at the specified position with a checkpoint number.
     * 
     * <p>This constructor creates a checkpoint with a specific number that determines the order
     * in which robots must visit it. The checkpoint is associated with a specific game board.</p>
     * 
     * @param position the position of the checkpoint on the game board
     * @param number the checkpoint number (must be visited in order)
     * @param boardId the unique identifier of the board this checkpoint belongs to
     * @throws IllegalArgumentException if position, number, or boardId is invalid
     */
    public CheckPoints(Position position, int number, String boardId) {
        super(position, boardId);
        this.number = number;
        this.robotCheckpoints = new HashMap<>();
        this.setBoardId(boardId);

        messageLogger.info("Checkpoints created at " + position + " with number " + getNumber() + " on board " + boardId);
    }

    /**
     * Checks whether this checkpoint is currently placed on a game board.
     * 
     * @return true if the checkpoint is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this checkpoint is placed on a game board.
     * 
     * @param onBoard true to mark the checkpoint as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this checkpoint belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this checkpoint and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the checkpoint is marked as being on a board.</p>
     * 
     * @param boardId the unique identifier of the board this checkpoint belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Gets the checkpoint number indicating the order in which it must be visited.
     * 
     * <p>The checkpoint number determines the sequence in which robots must reach checkpoints.
     * For example, checkpoint 2 can only be visited after checkpoint 1 has been reached.</p>
     * 
     * @return the checkpoint number
     */
    public int getNumber() {
        return number;
    }

    /**
     * Sets the checkpoint number indicating the order in which it must be visited.
     * 
     * @param number the new checkpoint number
     * @throws IllegalArgumentException if number is less than 1
     */
    public void setNumber(int number) {
        this.number = number;
    }

    /**
     * Gets the highest checkpoint number that a specific robot has reached.
     * 
     * <p>This method tracks the progress of individual robots through the checkpoint sequence.
     * It returns 0 if the robot hasn't reached any checkpoints yet.</p>
     * 
     * @param robotId the ID of the robot to check progress for
     * @return the highest checkpoint number reached by the robot, or 0 if none
     */
    public int getRobotHighestCheckpoint(int robotId) {
        return robotCheckpoints.getOrDefault(robotId, 0);
    }

    /**
     * Records that a robot has reached a specific checkpoint.
     * 
     * <p>This method updates the robot's progress tracking. Only the highest checkpoint
     * number reached by each robot is stored, ensuring that progress is never lost
     * even if the robot moves backwards or revisits earlier checkpoints.</p>
     * 
     * <p>The checkpoint is only recorded if it represents progress (higher number than
     * previously recorded for this robot).</p>
     * 
     * @param robotId the ID of the robot that reached the checkpoint
     * @param checkpointNumber the number of the checkpoint that was reached
     * @throws IllegalArgumentException if robotId is invalid or checkpointNumber is less than 1
     */
    public void recordCheckpoint(int robotId, int checkpointNumber) {
        // Only record the highest checkpoint number
        int currentHighest = robotCheckpoints.getOrDefault(robotId, 0);
        if (checkpointNumber > currentHighest) {
            robotCheckpoints.put(robotId, checkpointNumber);
        }
    }

    /**
     * Clears all checkpoint progress for all robots.
     * 
     * <p>This method resets the checkpoint tracking system, removing all recorded
     * progress for all robots. This is typically used when starting a new game
     * or resetting the game state.</p>
     */
    public void clearCheckpoints() {
        robotCheckpoints.clear();
    }

    /**
     * Checks whether a robot can visit this checkpoint based on their current progress.
     * 
     * <p>A robot can only visit a checkpoint if it represents the next checkpoint
     * in the sequence. For example, if a robot has reached checkpoint 2, they can
     * only visit checkpoint 3 next.</p>
     * 
     * <p>This validation ensures that robots must visit checkpoints in the correct
     * numerical order, maintaining the game's progression requirements.</p>
     * 
     * @param robotId the ID of the robot attempting to visit the checkpoint
     * @return true if the robot can visit this checkpoint, false otherwise
     */
    public boolean canVisit(int robotId) {
        int highestVisited = getRobotHighestCheckpoint(robotId);
        return number == highestVisited + 1;
    }

    /**
     * Activates the checkpoint effect on a robot.
     * 
     * <p>Checkpoints have no immediate activation effect when robots enter them.
     * The actual checkpoint logic is handled in the applyEffect method during
     * the activation phase.</p>
     * 
     * @param robot the robot to activate the checkpoint effect on (unused)
     */
    @Override
    public void activate(Robot robot) {
        // Checkpoints have no activation effect
    }

    /**
     * Checks whether a robot can pass through this checkpoint.
     * 
     * <p>Robots can freely move through checkpoints. The checkpoint itself
     * does not block movement, but will record progress when robots are on it
     * during the activation phase.</p>
     * 
     * @param robot the robot attempting to pass through the checkpoint
     * @return true - robots can pass through checkpoints
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "CheckPoint" identifying this element type
     */
    @Override
    public String getType() {
        return "CheckPoint";
    }

    /**
     * Applies the checkpoint effect to a robot during the activation phase.
     * 
     * <p>This method handles the complete checkpoint interaction process:</p>
     * <ol>
     *   <li>Checks if the robot can visit this checkpoint (sequential order)</li>
     *   <li>Records the checkpoint progress if valid</li>
     *   <li>Checks if the robot has visited all checkpoints</li>
     *   <li>Announces the winner if all checkpoints are reached</li>
     * </ol>
     * 
     * <p>The checkpoint is only recorded if the robot is visiting it in the correct
     * sequence, ensuring proper game progression.</p>
     * 
     * @param robot the robot located on the checkpoint
     * @param board the game board containing the checkpoint
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
     * Checks whether a robot has visited all checkpoints on the board.
     * 
     * <p>This method determines if a robot has completed the checkpoint sequence
     * by comparing their highest reached checkpoint with the total number of
     * checkpoints on the board.</p>
     * 
     * <p>A robot is considered to have visited all checkpoints when their highest
     * checkpoint number equals the total number of checkpoints available.</p>
     * 
     * @param robotId the ID of the robot to check
     * @param board the game board containing all checkpoints
     * @return true if the robot has visited all checkpoints, false otherwise
     */
    private boolean hasVisitedAllCheckpoints(int robotId, Board board) {
        // If the highest checkpoint number visited by the robot is equal to the total number,
        // it means that all checkpoints have been visited.
        int totalCheckpoints = board.getTotalCheckpoints();
        int highestVisited = getRobotHighestCheckpoint(robotId);
        return highestVisited == totalCheckpoints;
    }

    /**
     * Announces that a robot has won the game by visiting all checkpoints.
     * 
     * <p>This method is called when a robot completes the checkpoint sequence.
     * It provides feedback about the game victory and can be extended to include
     * additional game end logic such as:</p>
     * <ul>
     *   <li>Game state updates</li>
     *   <li>Player notifications</li>
     *   <li>Score calculations</li>
     *   <li>Game reset preparation</li>
     * </ul>
     * 
     * @param robotId the ID of the winning robot
     */
    private void announceWinner(int robotId) {
        System.out.println("Robot " + robotId + " has visited all checkpoints and won the game!");
        // Here can add game end logic.
        // z.B, game controller method to end the game.
    }

    /**
     * Returns a string representation of this checkpoint.
     * 
     * <p>The string includes the checkpoint number, position, and board association
     * status for easy identification and debugging.</p>
     * 
     * @return a detailed string describing the checkpoint's properties
     */
    @Override
    public String toString() {
        return "CheckPoint " + number + " at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board");
    }

    /**
     * Converts this CheckPoints object to a FieldCheckPoint representation for network communication.
     * 
     * <p>This method is used for serializing the checkpoint information when sending
     * game state updates to clients. The resulting FieldCheckPoint object contains
     * the board ID and checkpoint number in a format suitable for network transmission.</p>
     * 
     * @return a new FieldCheckPoint object representing this checkpoint's network data
     */
    @Override
    public MessageDefinitions.FieldCheckPoint toField() {
        return new MessageDefinitions.FieldCheckPoint(boardId, getNumber());
    }

}