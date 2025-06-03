package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Represents the gear element on the game board.
 * The gear rotates the robot standing on it during the activation phase.
 * There are two types of gears: clockwise rotation (green) and counterclockwise rotation (red).
 */
public class Gears extends BoardElement {

    public enum RotationDirection {
        CLOCKWISE,
        COUNTERCLOCKWISE
    }

    public enum GearColor {
        // Red gear rotates counterclockwise; Green gear rotates clockwise.
        RED,
        GREEN
    }

    private RotationDirection rotationDirection;
    private GearColor color;

    public Gears() {
        super();
        this.rotationDirection = RotationDirection.CLOCKWISE;
        this.color = GearColor.GREEN;
    }

    public Gears(Position position) {
        super(position);
        this.rotationDirection = RotationDirection.CLOCKWISE;
        this.color = GearColor.GREEN;
    }


    /**
     * Constructor with position and rotation direction parameters
     *
     * @param position Gear position
     * @param rotationDirection Gear rotation direction
     */
    public Gears(Position position, RotationDirection rotationDirection) {
        super(position);
        this.rotationDirection = rotationDirection;
        // Set colours based on rotation direction
        this.color = (rotationDirection == RotationDirection.CLOCKWISE) ? GearColor.GREEN : GearColor.RED;
    }

    /**
     * Constructor with position and colour parameters
     *
     * @param position Gear position
     * @param color Gear colour
     */
    public Gears(Position position, GearColor color) {
        super(position);
        this.color = color;

        // Set the rotation direction based on the colour
        this.rotationDirection = (color == GearColor.GREEN) ? RotationDirection.CLOCKWISE :
                RotationDirection.COUNTERCLOCKWISE;
    }

    public RotationDirection getRotationDirection() {
        return rotationDirection;
    }


    /**
     * Set the rotation direction of the gear.
     * Update the colour of the gear at the same time.
     *
     * @param rotationDirection New rotation direction.
     */
    public void setRotationDirection(RotationDirection rotationDirection) {
        this.rotationDirection = rotationDirection;
        this.color = (rotationDirection == RotationDirection.CLOCKWISE) ? GearColor.GREEN : GearColor.RED;
    }

    public GearColor getColor() {
        return color;
    }

    /**
     * Set the colour of the gear.
     * At the same time, update the rotation direction of the gear.
     *
     * @param color New colour.
     */
    public void setColor(GearColor color) {
        this.color = color;
        // Update rotation direction based on colour
        this.rotationDirection = (color == GearColor.GREEN) ? RotationDirection.CLOCKWISE :
                RotationDirection.COUNTERCLOCKWISE;
    }

    /**
     * When the robot is on the gear, the gear rotates the robot.
     *
     * @param robot The robot on the gear.
     */
    @Override
    public void activate(Robot robot) {
        // Rotate the robot according to the rotation direction of the gear.
        if (rotationDirection == RotationDirection.CLOCKWISE) {
            robot.turnRight();
            System.out.println("Green gear at " + position + " rotated Robot " + robot.getId() + " clockwise");
        } else {
            robot.turnLeft();
            System.out.println("Red gear at " + position + " rotated Robot " + robot.getId() + " counterclockwise");
        }
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    @Override
    public String getType() {
        return "Gear";
    }

    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
    }

    @Override
    public String toString() {
        return (color == GearColor.GREEN ? "Green" : "Red") +
                " gear at " + position +
                ", rotation: " + (rotationDirection == RotationDirection.CLOCKWISE ? "clockwise" : "counterclockwise");
    }
}
