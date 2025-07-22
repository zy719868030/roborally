package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Wall;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Represents a robot in the RoboRally game, managing its position, movement, damage,
 * and interaction with the game board and other robots.
 * 
 * <p>The Robot class serves as the physical game piece that players control on the board.
 * It handles all movement mechanics, collision detection, damage management, and
 * network communication for position and state updates.</p>
 * 
 * <p>Key responsibilities of the Robot class:</p>
 * <ul>
 *   <li><strong>Movement Control:</strong> Handles forward, backward, and rotational movement</li>
 *   <li><strong>Collision Detection:</strong> Checks walls, other robots, and board boundaries</li>
 *   <li><strong>Damage Management:</strong> Tracks damage level and applies damage cards</li>
 *   <li><strong>Board Interaction:</strong> Interacts with board elements and other robots</li>
 *   <li><strong>Network Communication:</strong> Broadcasts movement and state changes</li>
 *   <li><strong>Reboot System:</strong> Handles robot rebooting and recovery</li>
 * </ul>
 * 
 * <p>The robot's game state includes:</p>
 * <ul>
 *   <li><strong>Position:</strong> Current coordinates on the board</li>
 *   <li><strong>Direction:</strong> Current orientation (NORTH, SOUTH, EAST, WEST)</li>
 *   <li><strong>Damage Level:</strong> Accumulated damage affecting programming</li>
 *   <li><strong>Power State:</strong> Whether the robot is powered down</li>
 *   <li><strong>Programming Status:</strong> Whether programming is cancelled</li>
 * </ul>
 * 
 * <p>The class implements comprehensive movement validation including wall collision
 * detection, robot-to-robot interaction, and board boundary checking. All movement
 * operations are validated against the current board state before execution.</p>
 * 
 * <p>Network communication is handled through the associated player's connection,
 * broadcasting movement, rotation, and state changes to all clients for real-time
 * synchronization.</p>
 * 
 * @author Edle Eisbecher Team
 * @version 1.0
 * @since 1.0
 * @see Position
 * @see Direction
 * @see Board
 * @see Player
 * @see DamageCard
 */
public class Robot {
    
    /** Current position of the robot on the board */
    private Position position;
    
    /** Current direction the robot is facing */
    private Direction direction;
    
    /** Current damage level affecting programming capabilities */
    private int damage;
    
    /** Unique identifier for this robot */
    private final int robotID;
    
    /** Client ID of the player controlling this robot */
    private final int clientID;
    
    /** Flag indicating if programming has been cancelled for this round */
    private boolean programmingCancelled;
    
    /** List of programming cards for the current round */
    private List<RegisterCard> programming = new ArrayList<>();
    
    /** Flag indicating if the robot is powered down */
    public boolean isPoweredDown;
    
    /** Reference to the current game board */
    private Board currentBoard;
    
    /** Personal deck for robot-specific cards */
    private Deck<Card> personalDeck;

    /**
     * Constructs a new Robot with the specified robot ID and client ID.
     * 
     * <p>This constructor initializes a robot with default values for all game state.
     * The robot starts with no damage, is not powered down, and has an empty
     * personal deck. Position and direction are initially null and must be set
     * before the robot can participate in the game.</p>
     * 
     * <p>The constructor sets up the basic robot identity and initial state:</p>
     * <ul>
     *   <li><strong>Robot Identity:</strong> Sets unique robot and client IDs</li>
     *   <li><strong>Damage State:</strong> Initializes damage to 0</li>
     *   <li><strong>Power State:</strong> Sets powered down to false</li>
     *   <li><strong>Personal Deck:</strong> Creates empty personal deck</li>
     *   <li><strong>Programming State:</strong> Initializes programming list</li>
     * </ul>
     * 
     * <p>The robot must be placed on a board and have its position and direction
     * set before it can perform any movement or game actions.</p>
     * 
     * @param robotID the unique identifier for this robot
     * @param clientID the client ID of the player controlling this robot
     */
    public Robot(int robotID, int clientID) {
        this.robotID = robotID;
        this.clientID = clientID;
        this.damage = 0;
        this.isPoweredDown = false;
        this.personalDeck = new Deck<>();
    }


    /**
     * Checks if the robot is currently powered down.
     * 
     * <p>This method returns the current power state of the robot. When powered down,
     * the robot cannot perform movement actions, rotations, or other game actions
     * until it is rebooted or the power state is reset.</p>
     * 
     * <p>A powered down robot:</p>
     * <ul>
     *   <li>Cannot move forward or backward</li>
     *   <li>Cannot rotate or turn</li>
     *   <li>Cannot push other robots</li>
     *   <li>May have programming cancelled</li>
     * </ul>
     *
     * @return true if the robot is powered down, false otherwise
     */
    public boolean isPoweredDown() {
        return this.isPoweredDown;
    }

    /**
     * Gets the robot's personal deck of cards.
     * 
     * <p>This method returns the robot's personal deck which may contain
     * robot-specific cards, special abilities, or other unique card collections
     * that are separate from the player's programming deck.</p>
     *
     * @return the robot's personal deck
     */
    public Deck<Card> getPersonalDeck() {
        return personalDeck;
    }

    /**
     * Gets the unique identifier for this robot.
     * 
     * <p>This method returns the robot ID that uniquely identifies this robot
     * in the game. The robot ID is used for tracking, identification, and
     * network communication throughout the game session.</p>
     *
     * @return the unique robot identifier
     */
    public int getRobotID() {
        return this.robotID;
    }

    /**
     * Gets the client ID of the player controlling this robot.
     * 
     * <p>This method returns the client ID that identifies which player
     * is controlling this robot. The client ID is used for network communication,
     * player identification, and game state management.</p>
     *
     * @return the client ID of the controlling player
     */
    public int getClientID() {
        return this.clientID;
    }

    /**
     * Applies damage to the robot, increasing its damage level.
     * 
     * <p>This method increases the robot's damage level by the specified amount.
     * Damage affects the robot's programming capabilities by reducing the number
     * of cards available during the programming phase.</p>
     * 
     * <p>Damage effects include:</p>
     * <ul>
     *   <li>Reduced programming card hand size</li>
     *   <li>Potential programming cancellation</li>
     *   <li>Damage card integration into programming deck</li>
     *   <li>Reboot requirements at high damage levels</li>
     * </ul>
     * 
     * <p>The method logs the damage application for debugging and monitoring purposes.</p>
     * 
     * @param damageAmount the amount of damage to apply to the robot
     */
    public void takeDamage(int damageAmount) {
        this.damage += damageAmount;
        System.out.println("Robot " + robotID + " takes " + damageAmount + " damage. Total damage: " + this.damage);

    }

    /**
     * Rotates the robot 90 degrees to the left (counterclockwise) from its current direction.
     * 
     * <p>This method updates the robot's direction by rotating it 90 degrees counterclockwise.
     * The rotation is only performed if the robot is not powered down. After rotation,
     * the method broadcasts the turning action to all clients for synchronization.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Direction Update:</strong> Rotates the direction 90° left</li>
     *   <li><strong>Client Notification:</strong> Broadcasts the turning action</li>
     * </ul>
     * 
     * <p>If the robot is powered down, the method returns without performing any action.</p>
     */
    public void turnLeft() {
        if (isPoweredDown) return;
        direction = direction.turnLeft();
        notifyTurning("counterclockwise");
    }

    /**
     * Rotates the robot 90 degrees to the right (clockwise) from its current direction.
     * 
     * <p>This method updates the robot's direction by rotating it 90 degrees clockwise.
     * The rotation is only performed if the robot is not powered down. After rotation,
     * the method broadcasts the turning action to all clients for synchronization.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Direction Update:</strong> Rotates the direction 90° right</li>
     *   <li><strong>Client Notification:</strong> Broadcasts the turning action</li>
     * </ul>
     * 
     * <p>If the robot is powered down, the method returns without performing any action.</p>
     */
    public void turnRight() {
        if (isPoweredDown) return;
        direction = direction.turnRight();
        notifyTurning("clockwise");
    }

    /**
     * Rotates the robot 180 degrees to face the opposite direction.
     * 
     * <p>This method updates the robot's direction by rotating it 180 degrees,
     * effectively making it face the opposite direction. The rotation is only
     * performed if the robot is not powered down.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Direction Update:</strong> Rotates the direction 180°</li>
     *   <li><strong>Client Notification:</strong> Broadcasts two turning actions</li>
     * </ul>
     * 
     * <p>The method broadcasts two turning notifications to represent the 180-degree rotation.</p>
     */
    public void turnAround() {
        if (isPoweredDown) return;
        direction = direction.turnAround();
        notifyTurning("clockwise");
        notifyTurning("clockwise");
    }

    /**
     * Moves the robot forward one space in its current direction.
     * 
     * <p>This method attempts to move the robot one space forward in its current direction.
     * The movement is only performed if the robot is not powered down and has not fallen
     * off the board. The method includes comprehensive collision detection and validation.</p>
     * 
     * <p>The method performs the following validations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Fall Check:</strong> Ensures the robot has not fallen off the board</li>
     *   <li><strong>Wall Check:</strong> Validates exit from current position</li>
     *   <li><strong>Boundary Check:</strong> Ensures new position is within board bounds</li>
     *   <li><strong>Entry Check:</strong> Validates entry into target position</li>
     *   <li><strong>Robot Check:</strong> Ensures target position is not occupied</li>
     * </ul>
     * 
     * <p>If any validation fails, the movement is cancelled and the robot remains
     * in its current position. If the robot would fall off the board, the fall
     * handling mechanism is triggered.</p>
     * 
     * <p>After successful movement, the method updates the board position and
     * attempts to push any robots in the new position.</p>
     * 
     * @param board the game board to validate movement against
     */
    public void moveForward(Board board) {
        if (isPoweredDown) return;

        // Check if robot has fallen off the board
        if (board.hasRobotFallen(this)) {
            Logger logger = Logger.getLogger(this.getClass().getName());
            logger.info("Robot {" + clientID + "} cannot move - has fallen off the board");
            return;
        }

        // Check if current position has walls blocking exit in movement direction
        List<BoardElement> currentElements = board.getElements(position.x(), position.y());
        for (BoardElement element : currentElements) {
            if (element instanceof Wall wall) {
                if (!wall.canExitToDirection(direction)) {
                    // Cannot exit current cell due to wall
                    return;
                }
            }
        }

        Position newPos = position.move(direction);
        if (board.isValidPosition(newPos)) {
            // Check if new position's tiles allow entry from current direction
            List<BoardElement> targetElements = board.getElements(newPos.x(), newPos.y());
            boolean canEnter = true;

            // Check walls in target cell
            for (BoardElement element : targetElements) {
                if (element instanceof Wall wall) {
                    // Check if wall blocks entry from the opposite direction
                    if (!wall.canPassThroughFromDirection(direction.turnAround())) {
                        canEnter = false;
                        break;
                    }
                } else if (!element.canPassThrough(this)) {
                    canEnter = false;
                    break;
                }
            }

            if (canEnter && board.getRobotAt(newPos) == null) {
                Position oldPos = position;
                position = newPos;
                board.updateRobotPosition(this, position);
                pushRobot(board, direction);

                System.out.println("Robot {"+robotID+"} MOVED from {"+oldPos+"} to {"+position+"} facing {"+direction.getName()+"} - SUCCESS");
                notifyMovement();
            } else {
                if (!canEnter) {
                    System.out.println("Robot {"+robotID+"} BLOCKED at {"+position+"} - cannot enter target position " +
                            "{"+newPos+"} due to wall/obstacle");
                } else {
                    Robot blockingRobot = board.getRobotAt(newPos);
                    System.out.println("Robot {"+robotID+"} BLOCKED at {"+position+"} - target position  " +
                            "{"+newPos+"} occupied by Robot {"+position+blockingRobot.getRobotID()+ "}");
                }
            }
        } else {
            board.handleFall(this);
            Logger logger = Logger.getLogger(this.getClass().getName());
            logger.info("Robot " + clientID + " attempted to move forward off the board at position " + position +
                    ". Movement prevented.");
        }
    }

    /**
     * Moves the robot forward by the specified number of steps.
     * 
     * <p>This method moves the robot forward by the specified number of steps, performing
     * the same validation as single-step movement for each step. The movement stops
     * early if the robot hits a wall, encounters another robot, or falls off the board.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Step Validation:</strong> Ensures the number of steps is non-negative</li>
     *   <li><strong>Iterative Movement:</strong> Performs single-step movements</li>
     *   <li><strong>Early Termination:</strong> Stops if movement is blocked or robot falls</li>
     * </ul>
     * 
     * <p>Each step is validated individually, so the robot may complete some steps
     * before being blocked by an obstacle or boundary.</p>
     * 
     * @param board the game board to validate movement against
     * @param steps the number of steps to move forward (must be non-negative)
     */
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
     * Moves the robot backward one space in the direction opposite to its current orientation.
     * 
     * <p>This method attempts to move the robot one space backward by calculating the
     * opposite direction and performing the same validation as forward movement.
     * The movement is only performed if the robot is not powered down and has not fallen.</p>
     * 
     * <p>The method performs the following validations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Fall Check:</strong> Ensures the robot has not fallen off the board</li>
     *   <li><strong>Wall Check:</strong> Validates exit from current position in backward direction</li>
     *   <li><strong>Boundary Check:</strong> Ensures new position is within board bounds</li>
     *   <li><strong>Entry Check:</strong> Validates entry into target position</li>
     *   <li><strong>Robot Check:</strong> Ensures target position is not occupied</li>
     * </ul>
     * 
     * <p>If any validation fails, the movement is cancelled and the robot remains
     * in its current position. If the robot would fall off the board, the fall
     * handling mechanism is triggered.</p>
     * 
     * @param board the game board to validate movement against
     */
    public void moveBackward(Board board) {
        if (isPoweredDown) {
//            Logger logger = Logger.getLogger(this.getClass().getName());
//            logger.info("Robot {" + clientID + " } cannot move backward - is powered down");
            return;
        }

        // Check if robot has fallen off the board
        if (board.hasRobotFallen(this)) {
            Logger logger = Logger.getLogger(this.getClass().getName());
            logger.info("Robot {" + clientID + "} cannot move - has fallen off the board");
            return;
        }

        Direction opposite = direction.turnAround();
//        Logger logger = Logger.getLogger(this.getClass().getName());
//        logger.info("Robot {"+clientID+"} at position {"+ position +"} facing {" + direction +"} attempting to move " +
//                "backward (direction:"+ opposite);


        // Check if current position has walls blocking exit in backward direction
        List<BoardElement> currentElements = board.getElements(position.x(), position.y());
//        logger.info("Robot {"+clientID+"} current position elements: {"+currentElements.size());
        for (BoardElement element : currentElements) {
            if (element instanceof Wall wall) {
//                logger.info("Robot {"+clientID+"} found wall at current position blocking directions: {"+wall.getBlockedDirections()+"}");
                if (!wall.canExitToDirection(opposite)) {
//                    logger.info("Robot {"+clientID+"} BLOCKED by wall - cannot exit current cell in direction {"+ opposite+"}");
                    // Cannot exit current cell due to wall
                    return;
                }
            }
        }

        Position newPos = position.move(opposite);
//        logger.info("Robot {"+clientID+"} calculated new position: {"+newPos+"}");
        if (board.isValidPosition(newPos)) {
            Robot robotAtTarget = board.getRobotAt(newPos);
            if (robotAtTarget != null) {
//                logger.info("Robot {"+clientID+"} BLOCKED - target position {"+newPos+"} is occupied by Robot " +
//                        "{"+robotAtTarget.getRobotID()+"}");
                return;
            }

            List<BoardElement> targetElements = board.getElements(newPos.x(), newPos.y());
//            logger.info("Robot {"+clientID+"} target position elements: {"+targetElements.size()+"}");
            boolean canEnter = true;

            // Check walls in target cell
            for (BoardElement element : targetElements) {
                if (element instanceof Wall wall) {
//                    logger.info("Robot {"+clientID+"} found wall at target position blocking directions: {"+wall.getBlockedDirections()+"}");
                    // Check if wall blocks entry from the direction we're coming from
                    if (!wall.canPassThroughFromDirection(opposite.turnAround())) {
//                        logger.info("Robot {"+clientID+"} BLOCKED by wall at target - cannot enter from direction {"+opposite.turnAround()+"}");
                        canEnter = false;
                        break;
                    }
                } else if (!element.canPassThrough(this)) {
//                    logger.info("Robot {"+clientID+"} BLOCKED by element at target: {"+element.getType()+"}");
                    canEnter = false;
                    break;
                }
            }

            if (canEnter) {
//                logger.info("Robot {"+clientID+"} SUCCESSFULLY moving backward from {"+position+"} to {"+newPos+"}");
                position = newPos;
                board.updateRobotPosition(this, position);
                pushRobot(board, opposite);
                notifyMovement();
//            } else {
//                logger.info("Robot {"+clientID+"} BLOCKED - cannot enter target position {"+newPos+"}");
            }
        } else {
//            logger.info("Robot {"+clientID+"} would fall off board - target position {"+newPos+"} is invalid, triggering reboot");
            board.handleFall(this);
        }
    }

    /**
     * Moves the robot backward by the specified number of steps.
     * 
     * <p>This method moves the robot backward by the specified number of steps, performing
     * the same validation as single-step backward movement for each step. The movement
     * stops early if the robot hits a wall, encounters another robot, or falls off the board.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Step Validation:</strong> Ensures the number of steps is non-negative</li>
     *   <li><strong>Iterative Movement:</strong> Performs single-step backward movements</li>
     *   <li><strong>Early Termination:</strong> Stops if movement is blocked or robot falls</li>
     * </ul>
     * 
     * <p>Each step is validated individually, so the robot may complete some steps
     * before being blocked by an obstacle or boundary.</p>
     * 
     * @param board the game board to validate movement against
     * @param steps the number of steps to move backward (must be non-negative)
     */
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

    /**
     * Pushes another robot in the specified direction.
     * 
     * <p>This method attempts to push another robot that is adjacent to this robot
     * in the specified direction. The push is only performed if the robot is not
     * powered down and the target position contains another robot.</p>
     * 
     * <p>The method performs the following validations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Robot Check:</strong> Ensures there is a robot at the target position</li>
     *   <li><strong>Wall Check:</strong> Validates the other robot can exit its position</li>
     *   <li><strong>Boundary Check:</strong> Ensures the new position is within board bounds</li>
     *   <li><strong>Entry Check:</strong> Validates entry into the new position</li>
     *   <li><strong>Occupancy Check:</strong> Ensures the new position is not occupied</li>
     * </ul>
     * 
     * <p>If the other robot cannot be pushed (due to walls, boundaries, or occupied
     * positions), the push is cancelled. If the other robot would fall off the board,
     * the fall handling mechanism is triggered.</p>
     * 
     * <p>The method recursively calls itself to handle chain pushing, where multiple
     * robots may be pushed in sequence.</p>
     * 
     * @param board the game board to validate pushing against
     * @param pushDirection the direction to push the other robot
     */
    public void pushRobot(Board board, Direction pushDirection) {
        if (isPoweredDown) return;
        Position nextPos = position.move(pushDirection);
        Robot otherRobot = board.getRobotAt(nextPos);
        if (otherRobot != null) {
            // Check if the other robot can be pushed (check walls at its current position)
            List<BoardElement> otherRobotElements = board.getElements(nextPos.x(), nextPos.y());
            boolean canPush = true;
            for (BoardElement element : otherRobotElements) {
                if (element instanceof Wall wall) {
                    if (!wall.canExitToDirection(pushDirection)) {
                        canPush = false;
                        break;
                    }
                }
            }

            if (!canPush) {
                return; // Cannot push due to wall
            }

            Position otherNewPos = nextPos.move(pushDirection);
            if (board.isValidPosition(otherNewPos)) {
                List<BoardElement> targetElements = board.getElements(otherNewPos.x(), otherNewPos.y());
                boolean canEnter = true;

                // Check if target position allows entry
                for (BoardElement element : targetElements) {
                    if (element instanceof Wall wall) {
                        if (!wall.canPassThroughFromDirection(pushDirection.turnAround())) {
                            canEnter = false;
                            break;
                        }
                    } else if (!element.canPassThrough(otherRobot)) {
                        canEnter = false;
                        break;
                    }
                }

                if (canEnter && board.getRobotAt(otherNewPos) == null) {
                    otherRobot.setPosition(otherNewPos);
                    board.updateRobotPosition(otherRobot, otherNewPos);
                    otherRobot.pushRobot(board, pushDirection);
                    otherRobot.notifyMovement();
                }
            } else {
                board.handleFall(otherRobot);
            }
        }
    }

    /**
     * Moves the robot according to the given distance value.
     * 
     * <p>This method provides a unified interface for robot movement based on a
     * distance parameter. Positive values indicate forward movement, negative
     * values indicate backward movement, and zero indicates no movement.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Power Check:</strong> Verifies the robot is not powered down</li>
     *   <li><strong>Direction Determination:</strong> Determines movement direction based on distance sign</li>
     *   <li><strong>Movement Execution:</strong> Calls appropriate movement method</li>
     * </ul>
     * 
     * <p>This method is commonly used for card-based movement where the distance
     * is determined by the programming card being executed.</p>
     * 
     * @param board the game board to validate movement against
     * @param distance the distance to move (positive for forward, negative for backward, zero for no movement)
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
     * Gets the current position of the robot on the board.
     * 
     * <p>This method returns the Position object representing the robot's current
     * coordinates on the game board. The position contains the x and y coordinates
     * that define the robot's location.</p>
     *
     * @return the current Position object representing the robot's location
     */
    public Position getPosition() {
        return position;
    }

    /**
     * Sets the robot's position to the specified Position object.
     * 
     * <p>This method updates the robot's position and broadcasts the movement
     * to all clients for synchronization. The method is commonly used for
     * direct position updates such as rebooting or special movement effects.</p>
     * 
     * @param position the new Position object for the robot
     */
    public void setPosition(Position position) {
        this.position = position;
        notifyMovement();
    }

    /**
     * Sets the robot's position using x and y coordinates.
     * 
     * <p>This method creates a new Position object from the specified coordinates
     * and updates the robot's position. It also broadcasts the movement to all
     * clients for synchronization.</p>
     * 
     * @param x the x-coordinate of the new position
     * @param y the y-coordinate of the new position
     */
    public void setPosition(int x, int y) {
        this.position = new Position(x, y);
        notifyMovement();
    }

    /**
     * Broadcasts the robot's movement to all clients.
     * 
     * <p>This method sends a movement notification to all clients when the robot's
     * position changes. The notification includes the client ID and the new
     * coordinates for client synchronization.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Board Check:</strong> Verifies the robot has a valid board reference</li>
     *   <li><strong>Position Check:</strong> Ensures the position is not null</li>
     *   <li><strong>Player Lookup:</strong> Finds the player controlling this robot</li>
     *   <li><strong>Message Broadcasting:</strong> Sends movement notification to all clients</li>
     * </ul>
     * 
     * <p>If the robot has no board reference, position, or associated player,
     * the method returns without sending any notifications.</p>
     */
    public void notifyMovement(){
        if (currentBoard != null && position != null) {
            Player player = Game.getInstance().getPlayers().stream()
                    .filter(p -> p.getRobot() == this)
                    .findFirst()
                    .orElse(null);
            if (player != null) {
                player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyMovement(clientID, position.x(), position.y())
                ));
            }
        }
    }

    /**
     * Broadcasts the robot's turning action to all clients.
     * 
     * <p>This method sends a turning notification to all clients when the robot
     * rotates. The notification includes the client ID and the rotation direction
     * for client synchronization.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Board Check:</strong> Verifies the robot has a valid board reference</li>
     *   <li><strong>Player Lookup:</strong> Finds the player controlling this robot</li>
     *   <li><strong>Message Broadcasting:</strong> Sends turning notification to all clients</li>
     * </ul>
     * 
     * <p>If the robot has no board reference or associated player, the method
     * returns without sending any notifications.</p>
     * 
     * @param rotation the rotation direction ("clockwise" or "counterclockwise")
     */
    public void notifyTurning(String rotation) {
        if (currentBoard != null) {
            Player player = Game.getInstance().getPlayers().stream()
                    .filter(p -> p.getRobot() == this)
                    .findFirst()
                    .orElse(null);
            if (player != null) {
                player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyPlayerTurning(clientID, rotation)
                ));
            }
        }
    }

    /**
     * Cancels the remaining programming for this round.
     * 
     * <p>This method cancels the robot's programming for the current round, typically
     * used when the robot is rebooted or certain damage cards are activated. The
     * method sets the programming cancelled flag and resets the player's register.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Flag Setting:</strong> Sets programmingCancelled to true</li>
     *   <li><strong>Player Lookup:</strong> Finds the player controlling this robot</li>
     *   <li><strong>Register Reset:</strong> Resets the player's programming register</li>
     *   <li><strong>Logging:</strong> Records the programming cancellation</li>
     * </ul>
     * 
     * <p>This method is commonly called during reboot operations or when damage
     * cards like VIRUS or WORM are activated.</p>
     */
    public void cancelProgramming() {
        this.programmingCancelled = true;
        //programming.clear();
        Player player = Game.getInstance().getPlayers().stream()
                .filter(p -> p.getRobot() == this)
                .findFirst()
                .orElse(null);
        if (player != null) {
            player.resetRegister();
        }
        System.out.println("Robot " + robotID + " programming has been cancelled for this round.");
    }

    /**
     * Gets the current direction of the robot.
     * 
     * <p>This method returns the Direction object representing the robot's current
     * orientation on the board. The direction indicates which way the robot is facing
     * and affects movement and interaction with board elements.</p>
     *
     * @return the current Direction object representing the robot's orientation
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * Sets the robot's direction to the specified Direction.
     * 
     * <p>This method updates the robot's orientation without broadcasting any
     * notifications. It is commonly used for direct direction updates such as
     * rebooting or special rotation effects.</p>
     * 
     * @param direction the new Direction for the robot
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * Gets the current damage level of the robot.
     * 
     * <p>This method returns the integer value representing the total damage
     * the robot has sustained. Damage affects the robot's programming capabilities
     * by reducing the number of cards available during the programming phase.</p>
     *
     * @return the integer value representing the robot's damage level
     */
    public int getDamage() {
        return damage;
    }

    /**
     * Sets the board reference for this robot.
     * 
     * <p>This method establishes the connection between the robot and the game board.
     * The board reference is used for movement validation, position updates, and
     * network communication.</p>
     * 
     * @param board the game board to associate with this robot
     */
    public void setBoard(Board board) {
        this.currentBoard = board;
    }

    /**
     * Gets the board reference for this robot.
     * 
     * <p>This method returns the game board that this robot is associated with.
     * The board reference is used for movement validation and position management.</p>
     *
     * @return the game board associated with this robot
     */
    public Board getBoard() {
        return currentBoard;
    }

    /**
     * Reboots the robot, resetting its position and direction.
     * 
     * <p>This method performs a complete reboot of the robot, typically triggered
     * when the robot falls off the board or sustains critical damage. The reboot
     * process resets the robot's position, direction, and broadcasts the reboot
     * event to all clients.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Board Check:</strong> Verifies the robot has a valid board reference</li>
     *   <li><strong>Board Reboot:</strong> Calls the board's reboot mechanism</li>
     *   <li><strong>Player Lookup:</strong> Finds the player controlling this robot</li>
     *   <li><strong>Reboot Notification:</strong> Broadcasts reboot event to all clients</li>
     *   <li><strong>Direction Reset:</strong> Sets direction to NORTH</li>
     * </ul>
     * 
     * <p>If the robot has no board reference, the method returns without performing
     * any reboot operations.</p>
     */
    public void reboot() {
        if (currentBoard != null) {
            currentBoard.rebootRobot(this);
            Player player = Game.getInstance().getPlayers().stream()
                    .filter(p -> p.getRobot() == this)
                    .findFirst()
                    .orElse(null);
            if (player != null) {
                player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyReboot(this.clientID)
                ));
            }
            setDirection(Direction.NORTH);
        }
    }

    /**
     * Adds a damage card of the specified type to the robot.
     * 
     * <p>This method adds a damage card to the robot's programming deck, affecting
     * future programming phases. The damage card is obtained from the global damage
     * card pool and added to the player's programming deck discard pile.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Card Retrieval:</strong> Gets damage card from the global pool</li>
     *   <li><strong>Player Lookup:</strong> Finds the player controlling this robot</li>
     *   <li><strong>Deck Integration:</strong> Adds card to programming deck discard pile</li>
     *   <li><strong>Client Notification:</strong> Broadcasts damage card draw</li>
     *   <li><strong>Special Effects:</strong> Handles SPAM card immediate replacement</li>
     * </ul>
     * 
     * <p>Damage cards affect the robot's programming by:</p>
     * <ul>
     *   <li>Reducing available programming cards</li>
     *   <li>Introducing unwanted cards into the deck</li>
     *   <li>Triggering special effects during activation</li>
     *   <li>Potentially cancelling programming</li>
     * </ul>
     * 
     * <p>If the damage type is SPAM and the game is in activation phase, the method
     * immediately replaces the first register slot with a new programming card.</p>
     * 
     * @param type the type of damage card to add (SPAM, WORM, VIRUS, TROJAN_HORSE)
     */
    public void addDamageCard(DamageCard.DamageType type) {
        DamageCard damageCard = DamageCardPool.getInstance().getDamageCard(type);
        if (damageCard != null) {
            Player player = Game.getInstance().getPlayers().stream()
                    .filter(p -> p.getRobot() == this)
                    .findFirst()
                    .orElse(null);
            if (player != null) {
                // Add the damage card to the player's programming deck discard pile (not the robot's personal deck).
                player.getProgrammingDeck().discard(damageCard);
                System.out.println("Robot " + robotID + " receives damage card: " + damageCard.getDamageType());
                if (player.getConnection() != null) {
                    String cardName = damageCard.getDamageType().name(); // "SPAM", "VIRUS", etc.
                    List<String> cards = List.of(cardName);

                    player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyDrawDamage(this.clientID, cards)
                    ));
                }
                if (type == DamageCard.DamageType.SPAM && Game.getInstance().getCurrentPhase() == 3) {
                    player.replaceDamageCard(0); // Replace first register slot for SPAM
                }
            } else {
                System.err.println("Error: Could not find player for robot " + robotID);
            }
        }
    }

    /**
     * Replaces a damage card with a programming card.
     * 
     * <p>This method is a simplified placeholder for damage card replacement.
     * In actual gameplay, new cards would be drawn from the programming deck
     * to replace damage cards in the register.</p>
     * 
     * <p>Currently, this method only logs the replacement action. Future
     * implementations should include proper card drawing and register updating.</p>
     */
    public void replaceDamageCard() {
        System.out.println("Robot " + robotID + " replaces damage card with programming card");
    }

    /**
     * Returns a string representation of the robot.
     * 
     * <p>This method provides a formatted string representation of the robot object,
     * including the robot ID, position coordinates, and direction. The method includes
     * a null check for the position to handle cases where the robot has not been
     * placed on the board yet.</p>
     * 
     * <p>The string format is:</p>
     * <ul>
     *   <li>With position: "Robot {robotID} ({x}, {y}) {direction}"</li>
     *   <li>Without position: "Robot {robotID} (no position or direction)"</li>
     * </ul>
     * 
     * <p>This method is useful for debugging, logging, and displaying robot state
     * in user interfaces.</p>
     *
     * @return a formatted string representation of the robot with ID, position, and direction
     */
    public String toString() {
        return position == null ? "Robot " + robotID + " (no position or direction)" : "Robot " + robotID + " (" + position.x() + ", " + position.y() + ") " + direction;
    }

    /**
     * Gets the player controlling this robot.
     * 
     * <p>This method searches through all players in the game to find the one
     * that controls this robot. The search is performed by comparing robot references
     * with the current robot instance.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Game Access:</strong> Gets the current game instance</li>
     *   <li><strong>Player Search:</strong> Searches through all players</li>
     *   <li><strong>Robot Matching:</strong> Finds player with matching robot reference</li>
     *   <li><strong>Result Return:</strong> Returns the controlling player or null</li>
     * </ul>
     * 
     * <p>If no player is found controlling this robot, the method returns null.
     * This could happen if the robot is not properly associated with a player
     * or if the game state is inconsistent.</p>
     *
     * @return the Player controlling this robot, or null if not found
     */
    public Player getPlayer() {
        return Game.getInstance().getPlayers().stream()
                .filter(p -> p.getRobot() == this)
                .findFirst()
                .orElse(null);
    }

    /**
     * Gets the name of the player controlling this robot.
     * 
     * <p>This method retrieves the display name of the player that controls this robot.
     * If the controlling player cannot be found, it returns a default name based on
     * the client ID.</p>
     * 
     * <p>The method performs the following operations:</p>
     * <ul>
     *   <li><strong>Player Lookup:</strong> Finds the player controlling this robot</li>
     *   <li><strong>Name Retrieval:</strong> Gets the player's display name</li>
     *   <li><strong>Fallback Handling:</strong> Returns default name if player not found</li>
     * </ul>
     * 
     * <p>The fallback name format is "Spieler{clientID}" when no controlling player
     * is found. This ensures that the method always returns a meaningful string
     * for display purposes.</p>
     *
     * @return the name of the controlling player, or "Spieler{clientID}" if not found
     */
    public String getPlayerName() {
        Player p = getPlayer();
        return p != null ? p.getName() : ("Spieler" + clientID);
    }

//    public void addEnergy(int amount) {
//        this.energy += amount;
//        System.out.println("Robot " + id + " gained " + amount + " energy. Total energy: " + this.energy);
//    }
}
