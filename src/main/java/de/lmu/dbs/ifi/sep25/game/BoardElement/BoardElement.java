package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class BoardElement {
    protected Position position;
    protected Direction direction;

    public BoardElement() {
    }

    public BoardElement(Position position) {
        this.position = position;
    }

    public BoardElement(Position position, Direction direction) {
        this.position = position;
        this.direction = direction;
    }

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }


    /**
     * Effect activated when the robot enters the square where the element is located.
     * Specific behaviour is implemented by the subclass.
     *
     * @param robot Robot entering the element.
     */
    public abstract void activate(Robot robot);

    /**
     * Checks whether the robot can pass through the element.
     *
     * @param robot The robot attempting to pass through.
     * @return Returns true if it can pass through, otherwise returns false.
     */
    public abstract boolean canPassThrough(Robot robot);


    /**
     * Get the type of the element.
     *
     * @return String representation of the element type.
     */
    public abstract String getType();
}
