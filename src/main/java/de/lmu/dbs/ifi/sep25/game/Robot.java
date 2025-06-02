package de.lmu.dbs.ifi.sep25.game;

import java.util.UUID;

public class Robot {
    private final String id;
    private Position position;
    private Direction direction;
    private int damage;

    /*
    public Robot(Position startPosition, Direction direction) {
        this.position = startPosition;
        this.direction = direction;
        this.damage = 0;
    }
    */

    public Robot(int x, int y, Direction direction) {
        this.id = UUID.randomUUID().toString();
        this.position = new Position(x, y);
        this.direction = direction;
        this.damage = 0;
    }

    /**
     * Rotates the robot 90 degrees to the left (counterclockwise) from its current direction.
     * The robot's direction is updated to reflect this change.
     */
    public void turnL() {
        direction = direction.turnLeft();
    }

    /**
     * Rotates the robot 90 degrees to the right (clockwise) from its current direction.
     * The robot's direction is updated to reflect this change.
     */
    public void turnR() {
        direction = direction.turnRight();
    }

    /**
     * Moves the robot one step forward in its current direction.
     * The robot's position is updated based on its current direction
     * using the logic defined in the Position class.
     */
    public void applyMove(int distance) {
        for (int i = 0; i < distance; i++) {
            position = position.move(direction);
        }
    }

    /**
     * Moves the robot forward by the specified number of steps in its current direction.
     * The position of the robot is updated step-by-step based on its direction.
     *
     * @param steps the number of steps the robot will move forward; must be a non-negative integer
     */
    /*
    public void moveForward(int steps) {
        for (int i = 0; i < steps; i++)
            moveForward();
    }
    */


    /**
     * Moves the robot one step backward in the direction opposite to its current orientation.
     * This method determines the reverse direction of the robot's current orientation
     * and updates the position accordingly by moving one step in that direction.
     */
    /*
    public void moveBackward() {
        position = position.move(direction.turnAround());
    }
    */

    /**
     * Retrieves the current position of the robot.
     *
     * @return the current Position object representing the robot's location.
     */
    public Position getPosition() {
        return position;
    }

    public void setPosition(int x, int y){
        this.position = new Position(x, y);
    }

    public String getId(){
        return id;
    }

    public void takeDamage(int amount){
        damage += amount;
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
     * Retrieves the current damage value of the robot.
     *
     * @return the integer value representing the damage the robot has sustained.
     */
    public int getDamage() {
        return damage;
    }
}
