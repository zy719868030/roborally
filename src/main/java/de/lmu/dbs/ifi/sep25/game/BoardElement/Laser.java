package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.List;

/**
 * Represents a laser element on the game board.
 * Lasers deal damage to robots that end their movement on a laser's path.
 */
public class Laser extends BoardElement {
    //power indicates laser intensity (count)
    private int power;
    private String boardId;
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

    public Laser(Position position, Direction direction, int power, String boardId) {
        super(position, direction);
        this.power = power;
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

    public int getPower() {
        return power;
    }

    public void setPower(int power) {
        this.power = power;
    }

    /**
     * When the robot enters a grid containing lasers, the lasers will cause damage to the robot.
     *
     * @param robot The robot that entered the lasers.
     */
    @Override
    public void activate(Robot robot) {
        robot.takeDamage(power);
        System.out.println("Robot " + robot.getId() + " was hit by a laser and took " + power + " damage!");
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "Laser";
    }

    /**
     * Apply laser effect to robots
     * This method is called during the activation phase of each round to check all robots on the laser path and cause damage
     *
     * @param robot Affected robots
     * @param board Game board
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
        // Shoot lasers and check all robots on the path.
        fireLaser(board);
    }

    /**
     * Shoot a laser, check all robots on the path and cause damage.
     *
     * @param board Game board.
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
                System.out.println("Robot " + targetRobot.getId() + " was hit by a laser and took " + power + " damage!");
                break;
            }
        }
    }

    /**
     * Checks whether there is a wall blocking the laser at the specified position.
     *
     * @param position The position to be checked.
     * @param board The game board.
     * @return Returns true if there is a wall blocking the laser, otherwise returns false.
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
     * Get the string representation of the laser.
     *
     * @return The string representation of the laser, including position, direction, and intensity.
     */
    @Override
    public String toString() {
        return "Laser at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                ", direction: " + direction.getName() + ", power: " + power;
    }

    /**
     * Converts the current Laser instance to a FieldLaser representation.
     *
     * @return A FieldLaser object containing the board ID, direction, and power of the laser.
     */
    @Override
    public MessageDefinitions.FieldLaser toField() {
        return new MessageDefinitions.FieldLaser(boardId, List.of(direction.toString()), power, true); // oder false je nach Zustand
    }

}