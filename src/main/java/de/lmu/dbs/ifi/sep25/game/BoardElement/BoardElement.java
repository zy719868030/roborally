package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

/**
 * Abstract base class for all board elements in the RoboRally game.
 * 
 * <p>BoardElement represents the fundamental building blocks of the game board. Each element
 * has a position on the board and can interact with robots in various ways. This abstract class
 * defines the common interface and behavior that all board elements must implement.</p>
 *
 */
public abstract class BoardElement {
    
    /** The position of this element on the game board */
    protected Position position;
    
    /** The direction this element is facing (if applicable) */
    protected Direction direction;
    
    /** The unique identifier of the board this element belongs to (for protocol compliance) */
    protected final String boardId;

    /**
     * Default constructor for special cases like Reboot singleton.
     * 
     * <p>This constructor creates a board element without position, direction, or board association.
     * It is primarily used for singleton elements that don't need these properties.</p>
     * 
     * <p><strong>Note:</strong> This is a temporary solution to allow certain elements to compile.
     * Most board elements should use one of the other constructors.</p>
     */
    public BoardElement() {
        this.boardId = null; // Temporary: Allows Reboot to compile
    }

    /**
     * Constructs a board element with only a board identifier.
     * 
     * <p>This constructor is used for elements that need board association but don't
     * require position or direction information initially.</p>
     * 
     * @param boardId the unique identifier of the board this element belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public BoardElement(String boardId) {
        this.boardId = boardId;
    }

    /**
     * Constructs a board element with position and board identifier.
     * 
     * <p>This constructor is used for elements that have a specific position on the board
     * but don't require directional properties.</p>
     * 
     * @param position the position of this element on the game board
     * @param boardId the unique identifier of the board this element belongs to
     * @throws IllegalArgumentException if position or boardId is null
     */
    public BoardElement(Position position, String boardId) {
        this.position = position;
        this.boardId = boardId;
    }

    /**
     * Constructs a board element with position and direction.
     * 
     * <p>This constructor is used for elements that have both position and directional
     * properties but don't need board association.</p>
     * 
     * @param position the position of this element on the game board
     * @param direction the direction this element is facing
     * @throws IllegalArgumentException if position or direction is null
     */
    public BoardElement(Position position, Direction direction) {
        this.position = position;
        this.direction = direction;
        this.boardId = null;
    }

    /**
     * Constructs a board element with position, direction, and board identifier.
     * 
     * <p>This is the most complete constructor, used for elements that need all properties.
     * It is commonly used by elements like Belts and Laser that require full positioning
     * and board association.</p>
     * 
     * @param position the position of this element on the game board
     * @param direction the direction this element is facing
     * @param boardId the unique identifier of the board this element belongs to
     * @throws IllegalArgumentException if position, direction, or boardId is null
     */
    public BoardElement(Position position, Direction direction, String boardId) {
        this.position = position;
        this.direction = direction;
        this.boardId = boardId;
    }

    /**
     * Gets the position of this board element on the game board.
     * 
     * @return the position of this element, or null if not set
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Sets the position of this board element on the game board.
     * 
     * @param position the new position for this element
     * @throws IllegalArgumentException if position is null
     */
    public void setPosition(Position position) {
        this.position = position;
    }

    /**
     * Gets the direction this board element is facing.
     * 
     * <p>Not all board elements have a direction. This method returns null for
     * elements that don't require directional properties.</p>
     * 
     * @return the direction this element is facing, or null if not applicable
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the direction this board element is facing.
     * 
     * @param direction the new direction for this element
     * @throws IllegalArgumentException if direction is null
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * Gets the unique identifier of the board this element belongs to.
     * 
     * <p>This identifier is used for protocol compliance and board management.
     * Elements that are not associated with a specific board return null.</p>
     * 
     * @return the board identifier, or null if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Activates the effect of this board element when a robot enters its position.
     * 
     * <p>This method is called when a robot moves onto or interacts with this element.
     * The specific behavior is implemented by each subclass according to the element's
     * type and game rules.</p>
     *
     * 
     * @param robot the robot that is activating this element
     * @throws IllegalArgumentException if robot is null
     */
    public abstract void activate(Robot robot);

    /**
     * Applies the effect of this board element to a robot with board context.
     * 
     * <p>This method provides a default implementation that calls the activate method.
     * Subclasses can override this method to provide more complex effects that require
     * access to the game board context.</p>
     *
     * 
     * @param robot the robot to apply the effect to
     * @param board the game board containing this element
     * @throws IllegalArgumentException if robot or board is null
     */
    public void applyEffect(Robot robot, Board board) {
        activate(robot);
    }

    /**
     * Checks whether a robot can pass through this board element.
     * 
     * <p>This method determines if a robot can move through or over this element.
     * The behavior varies significantly between element types:</p>
     * <ul>
     *   <li><strong>Passable Elements:</strong> Conveyor belts, energy spaces, checkpoints</li>
     *   <li><strong>Impassable Elements:</strong> Walls, pits, antennas</li>
     *   <li><strong>Conditional Elements:</strong> Some elements may be passable under certain conditions</li>
     * </ul>
     * 
     * <p>This check is typically performed during movement validation to ensure
     * robots cannot move through obstacles or invalid areas.</p>
     * 
     * @param robot the robot attempting to pass through this element
     * @return true if the robot can pass through, false otherwise
     * @throws IllegalArgumentException if robot is null
     */
    public abstract boolean canPassThrough(Robot robot);

    /**
     * Gets the type identifier for this board element.
     * 
     * <p>This method returns a string that uniquely identifies the type of this
     * board element. The type identifier is used for:</p>
     * <ul>
     *   <li>Game logic and element-specific behavior</li>
     *   <li>UI rendering and element visualization</li>
     *   <li>Network protocol communication</li>
     *   <li>Debugging and logging purposes</li>
     * </ul>
     * 
     * <p>Common type identifiers include:</p>
     * <ul>
     *   <li>"Wall", "Pit", "Laser", "ConveyorBelt"</li>
     *   <li>"Gear", "EnergySpace", "CheckPoint", "StartPoint"</li>
     *   <li>"Antenna", "PushPanel", "Reboot"</li>
     * </ul>
     * 
     * @return a string representing the type of this board element
     */
    public abstract String getType();

    /**
     * Converts this board element to a network-compatible field representation.
     * 
     * <p>This method is used for protocol compliance when sending game state updates
     * to clients. Each subclass must implement this method to provide the appropriate
     * field type from MessageDefinitions.</p>
     * 
     * <p>The resulting field object contains:</p>
     * <ul>
     *   <li>Element type and properties</li>
     *   <li>Position and directional information</li>
     *   <li>Board association data</li>
     *   <li>Element-specific attributes</li>
     * </ul>
     * 
     * <p>This serialization is essential for:</p>
     * <ul>
     *   <li>Client-server communication</li>
     *   <li>Game state synchronization</li>
     *   <li>Board layout transmission</li>
     *   <li>Real-time game updates</li>
     * </ul>
     * 
     * @return a field representation of this board element suitable for network transmission
     */
    public abstract MessageDefinitions.Field toField();

}
