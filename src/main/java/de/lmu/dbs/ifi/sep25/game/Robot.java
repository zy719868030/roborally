package de.lmu.dbs.ifi.sep25.game;

public class Robot {
    private Position position;
    private Direction direction;
    private int damage;
    private int id;
    private boolean programmingCancelled;
    private int energy;

    public Robot(int id) {
//        this.position = startPosition;
//        this.direction = direction; //TODO move to setPosition
        this.damage = 0;
        this.id = id;
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
        direction = direction.turnLeft();
    }

    /**
     * Rotates the robot 90 degrees to the right (clockwise) from its current direction.
     * The robot's direction is updated to reflect this change.
     */
    public void turnRight() {
        direction = direction.turnRight();
    }

    /**
     * Moves the robot one step forward in its current direction.
     * The robot's position is updated based on its current direction
     * using the logic defined in the Position class.
     */
    public void moveForward() {
        position = position.move(direction);
    }

    /**
     * Moves the robot forward by the specified number of steps in its current direction.
     * The position of the robot is updated step-by-step based on its direction.
     *
     * @param steps the number of steps the robot will move forward; must be a non-negative integer
     */
    public void moveForward(int steps) {
        for (int i = 0; i < steps; i++)
            moveForward();
    }

    /**
     * Moves the robot one step backward in the direction opposite to its current orientation.
     * This method determines the reverse direction of the robot's current orientation
     * and updates the position accordingly by moving one step in that direction.
     */
    public void moveBackward() {
        position = position.move(direction.turnAround());
    }

    /**
     * Move the robot according to the given distance value.
     * A positive value indicates forward movement, a negative value indicates backward movement, and 0 indicates no movement.
     *
     * @param distance The distance the robot needs to move, which can be a positive number, a negative number, or zero.
     */
    public void applyMove(int distance) {
        if (distance > 0) {
            // Positive values indicate forward movement.
            moveForward(distance);
        } else if (distance < 0) {
            // Negative values indicate backward movement.
            for (int i = 0; i < Math.abs(distance); i++) {
                moveBackward();
            }
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
