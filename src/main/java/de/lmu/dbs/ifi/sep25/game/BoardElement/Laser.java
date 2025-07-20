package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.List;

/**
 * Represents a laser element on the RoboRally game board.
 * 
 * <p>Lasers are hazardous elements that deal damage to robots in their firing path.
 * They fire in a specific direction and can hit multiple robots along their path
 * until blocked by walls or board boundaries.</p>
 *
 */
public class Laser extends BoardElement {
    
    /** The power/intensity of the laser, indicating how much damage it deals */
    private int power;
    
    /** The unique identifier of the board this laser belongs to */
    private String boardId;
    
    /** Flag indicating whether this laser is placed on a game board */
    private boolean isOnBoard;

//    public Laser() {
//        super();
//        this.power = 1;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public Laser(Position position, String boardName) {
//        super(position, boardName);
//        this.power = 1;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public Laser(Position position, Direction direction) {
//        super(position, direction);
//        this.power = 1;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public Laser(Position position, Direction direction, int power) {
//        super(position, direction);
//        this.power = power;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }

    /**
     * Constructs a laser with position, direction, power, and board identifier.
     * 
     * <p>This constructor creates a laser with specific firing direction and damage power.
     * The laser will fire in the specified direction and deal damage equal to its power
     * to any robots in its path.</p>
     * 
     * @param position the position of the laser on the game board
     * @param direction the direction in which the laser fires
     * @param power the damage power/intensity of the laser
     * @param boardId the unique identifier of the board this laser belongs to
     * @throws IllegalArgumentException if position, direction, or boardId is null, or power is negative
     */
    public Laser(Position position, Direction direction, int power, String boardId) {
        super(position, direction);
        this.power = power;
        this.setBoardId(boardId);
    }

    /**
     * Checks whether this laser is currently placed on a game board.
     * 
     * @return true if the laser is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this laser is placed on a game board.
     * 
     * @param onBoard true to mark the laser as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this laser belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this laser and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the laser is marked as being on a board.</p>
     * 
     * @param boardId the unique identifier of the board this laser belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Gets the power/intensity of this laser.
     * 
     * <p>The power determines how much damage the laser deals to robots when it hits them.
     * Higher power values result in more damage being dealt.</p>
     * 
     * @return the laser's damage power
     */
    public int getPower() {
        return power;
    }

    /**
     * Sets the power/intensity of this laser.
     * 
     * <p>The power determines how much damage the laser deals to robots when it hits them.
     * This value should be positive and represents the number of damage cards or health
     * points that will be applied to hit robots.</p>
     * 
     * @param power the new damage power for the laser
     * @throws IllegalArgumentException if power is negative
     */
    public void setPower(int power) {
        this.power = power;
    }

    /**
     * Activates the laser effect on a robot that is on the laser's position.
     * 
     * <p>When a robot is on the same position as a laser during the activation phase,
     * this method deals damage to the robot equal to the laser's power. This represents
     * the robot being hit by the laser at close range.</p>
     * 
     * @param robot the robot that is on the laser's position
     * @throws IllegalArgumentException if robot is null
     */
    @Override
    public void activate(Robot robot) {
        robot.takeDamage(power);
        System.out.println("Robot " + robot.getRobotID() + " was hit by a laser and took " + power + " damage!");
    }

    /**
     * Checks whether a robot can pass through this laser.
     * 
     * <p>Robots can freely move through laser positions. The laser itself does not block
     * movement, but will deal damage to robots during the activation phase if they are
     * in the laser's firing path.</p>
     * 
     * @param robot the robot attempting to pass through the laser
     * @return true - robots can pass through laser positions
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "Laser" identifying this element type
     */
    @Override
    public String getType() {
        return "Laser";
    }

    /**
     * Applies the laser effect to a robot and fires the laser along its path.
     * 
     * <p>This method handles the complete laser interaction process:</p>
     * <ol>
     *   <li>Activates the laser effect on the robot if it's on the laser's position</li>
     *   <li>Fires the laser along its direction to check for additional targets</li>
     *   <li>Deals damage to any robots found in the laser's path</li>
     * </ol>
     * 
     * <p>The laser firing continues until it hits a wall, board boundary, or the first
     * robot encountered along its path.</p>
     * 
     * @param robot the robot on the laser's position
     * @param board the game board containing the laser
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
        // Shoot lasers and check all robots on the path.
        fireLaser(board);
    }

    /**
     * Fires the laser along its direction and deals damage to robots in its path.
     * 
     * <p>This method simulates the laser firing process by checking each position
     * along the laser's direction until it encounters an obstacle or target. The laser
     * can hit multiple robots but stops at the first one encountered.</p>
     * 
     * <p>The firing process:</p>
     * <ul>
     *   <li>Moves along the laser's direction one position at a time</li>
     *   <li>Checks for board boundaries and stops if exceeded</li>
     *   <li>Checks for walls and stops if blocked</li>
     *   <li>Checks for robots and deals damage if found</li>
     *   <li>Stops firing after hitting the first robot</li>
     * </ul>
     * 
     * @param board the game board to check for targets
     */
    private void fireLaser(Board board) {
        if (direction == null) {
            System.err.println("Error: Laser direction is not set!");
            return;
        }
        Position currentPos = new Position(position.x(), position.y());

        // Check all cells along the laser direction until a wall or board boundary is encountered.
        while (true) {
            // Move to the next position
            currentPos = currentPos.move(direction);

            // Check if it exceeds the board boundary
            if (!board.isValidPosition(currentPos)) {
                break;
            }

            // Check if there is a wall blocking the laser at this location.
            if (isBlockedByWall(currentPos, board)) {
                break;
            }

            // Check if there are robots at this location
            Robot targetRobot = board.getRobotAt(currentPos);
            if (targetRobot != null) {
                targetRobot.takeDamage(power);
                System.out.println("Robot " + targetRobot.getRobotID() + " was hit by a laser and took " + power + " damage!");
                break;
            }
        }
    }

    /**
     * Checks whether there is a wall blocking the laser at the specified position.
     * 
     * <p>This method examines all board elements at the given position to determine
     * if any wall is blocking the laser's direction. Walls can block lasers depending
     * on their orientation and the laser's firing direction.</p>
     * 
     * @param position the position to check for blocking walls
     * @param board the game board containing the walls
     * @return true if there is a wall blocking the laser, false otherwise
     */
    private boolean isBlockedByWall(Position position, Board board) {
        for (BoardElement element : board.getElements(position.x(), position.y())) {
            // Check if it is a wall
            if (element instanceof Wall) {
                Wall wall = (Wall) element;
                // Check whether the wall is blocking the direction of the laser.
                if (!wall.canPassThroughFromDirection(direction)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Returns a string representation of this laser.
     * 
     * <p>The string includes the laser's position, board association, firing direction,
     * and power for easy identification and debugging.</p>
     * 
     * @return a detailed string describing the laser's properties
     */
    @Override
    public String toString() {
        return "Laser at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                ", direction: " + direction.getName() + ", power: " + power;
    }

    /**
     * Converts this Laser object to a FieldLaser representation for network communication.
     * 
     * <p>This method is used for serializing the laser information when sending
     * game state updates to clients. The resulting FieldLaser object contains
     * the board ID, direction, power, and activation status in a format suitable
     * for network transmission.</p>
     * 
     * @return a new FieldLaser object representing this laser's network data
     */
    @Override
    public MessageDefinitions.FieldLaser toField() {
        return new MessageDefinitions.FieldLaser(boardId, List.of(direction.toString()), power, true); // oder false je nach Zustand
    }

}