package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.game.Board;

public abstract class BoardElement {
    protected Position position;
    protected Direction direction;
    // Added: boardName for protocol's isOnBoard attribute
    protected final String boardName;

    // No-arg constructor for Reboot singleton
    public BoardElement() {
        this.boardName = null; // Temporary: Allows Reboot to compile
    }
    public BoardElement(String boardName) {
        this.boardName = boardName;
    }

    public BoardElement(Position position, String boardName) {

        this.position = position;
        this.boardName = boardName;
    }

    public BoardElement(Position position, Direction direction) {
        this.position = position;
        this.direction = direction;
        this.boardName = null;
    }

    // Constructor for Belts and Laser
    public BoardElement(Position position, Direction direction, String boardName) {
        this.position = position;
        this.direction = direction;
        this.boardName = boardName;
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

    public String getBoardName() {
        return boardName;
    }


    /**
     * Effect activated when the robot enters the square where the element is located.
     * Specific behaviour is implemented by the subclass.
     *
     * @param robot Robot entering the element.
     */
    public abstract void activate(Robot robot);

    public void applyEffect(Robot robot, Board board) {
        activate(robot);
    }

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
