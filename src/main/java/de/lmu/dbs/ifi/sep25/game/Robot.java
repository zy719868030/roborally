package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;

import java.util.ArrayList;
import java.util.List;

public class Robot {
    private Position position;
    private Direction direction;
    private int damage;
    private int id;
    private boolean programmingCancelled;
    private int energy;
    private List<RegisterCard> programming = new ArrayList<>();
    private boolean isPoweredDown;

    public Robot(int startX, int startY, String direction, int id) {
//        this.position = startPosition;
//        this.direction = direction; //TODO move to setPosition
        this.position = new Position(startX, startY);
        this.direction = Direction.valueOf(direction);
        this.id = id;
        this.damage = 0;
        this.energy = 5; // Starting energy for upgrades
        this.isPoweredDown = false;
    }

    public Robot(int x, int y, String direction) {
        this.position = new Position(x, y);
        this.direction = Direction.valueOf(direction);
    }

    // Method for obtaining robot ID
    public int getId() {
        return this.id;
    }

    // Method for damaging robots
    public void takeDamage(int damageAmount) {
        this.damage += damageAmount;
        System.out.println("Robot " + id + " takes " + damageAmount + " damage. Total damage: " + this.damage);
    }

    /**
     * Rotates the robot 90 degrees to the left (counterclockwise) from its current direction.
     * The robot's direction is updated to reflect this change.
     */
    public void turnLeft() {
        if (isPoweredDown) return;
        direction = direction.turnLeft();
    }

    /**
     * Rotates the robot 90 degrees to the right (clockwise) from its current direction.
     * The robot's direction is updated to reflect this change.
     */
    public void turnRight() {
        if (isPoweredDown) return;
        direction = direction.turnRight();
    }

    // Moves the robot forward one space, checking Board for validity
    public void moveForward(Board board) {
        if (isPoweredDown) return;
        Position newPos = position.move(direction);
        if (board.isValidPosition(newPos)) {
            // Check if new position's tiles allow passage
            List<BoardElement> elements = board.getElements(newPos.x(), newPos.y());
            boolean canPass = elements.stream().allMatch(e -> e.canPassThrough(this));
            if (canPass && board.getRobotAt(newPos) == null) {
                position = newPos;
                board.updateRobotPosition(this, position);
                pushRobot(board, direction);
            }
        } else {
            board.handleFall(this);
        }
    }

    // Moves the robot forward by the specified number of steps
    public void moveForward(Board board, int steps) {
        if (isPoweredDown) return;
        if (steps < 0) return; // Ignore negative steps
        for (int i = 0; i < steps; i++) {
            Position currentPos = position; // Store current position
            moveForward(board); // Move one step
            // Stop if position didn't change (e.g., hit a Wall) or robot fell
            if (position.equals(currentPos) || board.hasRobotFallen(this)) {
                break;
            }
        }
    }

    /**
     * Moves the robot one step backward in the direction opposite to its current orientation.
     * This method determines the reverse direction of the robot's current orientation
     * and updates the position accordingly by moving one step in that direction.
     */
    // Moves the robot backward one space
    public void moveBackward(Board board) {
        if (isPoweredDown) return;
        Direction opposite = direction.turnAround();
        Position newPos = position.move(opposite);
        if (board.isValidPosition(newPos)) {
            List<BoardElement> elements = board.getElements(newPos.x(), newPos.y());
            boolean canPass = elements.stream().allMatch(e -> e.canPassThrough(this));
            if (canPass && board.getRobotAt(newPos) == null) {
                position = newPos;
                board.updateRobotPosition(this, position);
                pushRobot(board, opposite);
            }
        } else {
            board.handleFall(this);
        }
    }

    // Moves the robot backward by the specified number of steps
    public void moveBackward(Board board, int steps) {
        if (isPoweredDown) return;
        if (steps < 0) return; // Ignore negative steps
        for (int i = 0; i < steps; i++) {
            Position currentPos = position; // Store current position
            moveBackward(board); // Move one step
            // Stop if position didn't change (e.g., hit a Wall) or robot fell
            if (position.equals(currentPos) || board.hasRobotFallen(this)) {
                break;
            }
        }
    }

    // Pushes another robot in the given direction
    public void pushRobot(Board board, Direction pushDirection) {
        if (isPoweredDown) return;
        Position nextPos = position.move(pushDirection);
        Robot otherRobot = board.getRobotAt(nextPos);
        if (otherRobot != null) {
            Position otherNewPos = nextPos.move(pushDirection);
            if (board.isValidPosition(otherNewPos)) {
                List<BoardElement> elements = board.getElements(otherNewPos.x(), otherNewPos.y());
                boolean canPass = elements.stream().allMatch(e -> e.canPassThrough(otherRobot));
                if (canPass && board.getRobotAt(otherNewPos) == null) {
                    otherRobot.setPosition(otherNewPos);
                    board.updateRobotPosition(otherRobot, otherNewPos);
                    otherRobot.pushRobot(board, pushDirection);
                }
            } else {
                board.handleFall(otherRobot);
            }
        }
    }

    /**
     * Move the robot according to the given distance value.
     * A positive value indicates forward movement, a negative value indicates backward movement, and 0 indicates no movement.
     *
     * @param distance The distance the robot needs to move, which can be a positive number, a negative number, or zero.
     */
    public void applyMove(Board board, int distance) {
        if (isPoweredDown) return;
        if (distance > 0) {
            // Positive values indicate forward movement.
            moveForward(board, distance);
        } else if (distance < 0) {
            // Negative values indicate backward movement.
            moveBackward(board, Math.abs(distance));
            }
    }

    /**
     * Retrieves the current position of the robot.
     *
     * @return the current Position object representing the robot's location.
     */
    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public void setPosition(int x, int y) {
        this.position = new Position(x, y);
    }

    /**
     * Cancels the remaining programming for this round.
     * This is used when the robot is rebooted or certain damage cards are activated.
     */
    public void cancelProgramming() {
        this.programmingCancelled = true;
        System.out.println("Robot " + id + " programming has been cancelled for this round.");
    }

    /**
     * Retrieves the current direction of the robot.
     *
     * @return the current Direction object representing the robot's orientation.
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Set the robot's direction.
     *
     * @param direction New direction.
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * Retrieves the current damage value of the robot.
     *
     * @return the integer value representing the damage the robot has sustained.
     */
    public int getDamage() {
        return damage;
    }

    /**
     * Increases the robot's energy value.
     *
     * @param amount The amount of energy to be increased.
     */
    public void addEnergy(int amount) {
        this.energy += amount;
        System.out.println("Robot " + id + " gained " + amount + " energy. Total energy: " + this.energy);
    }
}
