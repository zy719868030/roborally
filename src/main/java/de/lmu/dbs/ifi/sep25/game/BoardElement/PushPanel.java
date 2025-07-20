package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.*;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.*;


/**
 * Represents a push panel element on the RoboRally game board.
 * 
 * <p>Push panels are mechanical elements that push robots in a specific direction
 * when activated during certain register phases. They create dynamic movement
 * patterns and can affect multiple robots in a chain reaction.</p>
 *
 */
public class PushPanel extends BoardElement {
    
    /** List of register numbers (0-4) during which this push panel is active */
    private List<Integer> activeRegisters;
    
    /** The current register phase being executed (0-4) */
    private int currentRegister;
    
    /** The unique identifier of the board this push panel belongs to */
    private String boardId;
    
    /** Flag indicating whether this push panel is placed on a game board */
    private boolean isOnBoard;

//    public PushPanel() {
//        super();
//        this.activeRegisters = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public PushPanel(Position position, String boardId) {
//        super(position, boardId);
//        this.activeRegisters = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public PushPanel(Position position, Direction direction) {
//        super(position, direction);
//        this.activeRegisters = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }
//
//    public PushPanel(Position position, Direction direction, List<Integer> activeRegisters) {
//        super(position, direction);
//        this.activeRegisters = new ArrayList<>(activeRegisters);
//        this.currentRegister = 0;
//        this.isOnBoard = false;
//        this.boardId = "";
//    }

    /**
     * Constructs a push panel with position, direction, active registers, and board identifier.
     * 
     * <p>This constructor creates a push panel that activates during specific register phases
     * and pushes robots in the specified direction. The active registers determine when
     * the push panel will be active during the programming phase execution.</p>
     * 
     * @param position the position of the push panel on the game board
     * @param direction the direction in which the push panel pushes robots
     * @param activeRegisters list of register numbers (0-4) when this push panel is active
     * @param boardId the unique identifier of the board this push panel belongs to
     * @throws IllegalArgumentException if position, direction, activeRegisters, or boardId is null
     */
    public PushPanel(Position position, Direction direction, List<Integer> activeRegisters, String boardId) {
        super(position, direction);
        this.activeRegisters = new ArrayList<>(activeRegisters);
        this.currentRegister = 0;
        this.setBoardId(boardId);
    }

    /**
     * Gets a copy of the list of active registers for this push panel.
     * 
     * <p>The returned list contains the register numbers (0-4) during which this
     * push panel will be active and push robots.</p>
     * 
     * @return a new ArrayList containing the active register numbers
     */
    public List<Integer> getActiveRegisters() {
        return new ArrayList<>(activeRegisters);
    }

    /**
     * Sets the list of active registers for this push panel.
     * 
     * <p>The active registers determine during which programming phases (0-4)
     * this push panel will activate and push robots.</p>
     * 
     * @param activeRegisters list of register numbers (0-4) when this push panel should be active
     * @throws IllegalArgumentException if activeRegisters is null
     */
    public void setActiveRegisters(List<Integer> activeRegisters) {
        this.activeRegisters = new ArrayList<>(activeRegisters);
    }

    /**
     * Adds a register number to the list of active registers.
     * 
     * <p>If the register is not already in the active registers list, it will be added.
     * This allows dynamic configuration of when the push panel should be active.</p>
     * 
     * @param register the register number (0-4) to add to the active registers
     */
    public void addActiveRegister(int register) {
        if (!activeRegisters.contains(register)) {
            activeRegisters.add(register);
        }
    }

    /**
     * Removes a register number from the list of active registers.
     * 
     * <p>If the register is in the active registers list, it will be removed.
     * This allows dynamic configuration of when the push panel should be active.</p>
     * 
     * @param register the register number (0-4) to remove from the active registers
     */
    public void removeActiveRegister(int register) {
        activeRegisters.remove(Integer.valueOf(register));
    }

    /**
     * Sets the current register phase being executed.
     * 
     * <p>This method is called by the game engine to update which register
     * phase is currently being processed. The push panel will check this
     * value to determine if it should activate.</p>
     * 
     * @param register the current register phase (0-4) being executed
     */
    public void setCurrentRegister(int register) {
        this.currentRegister = register;
    }

    /**
     * Checks whether this push panel is active in the current register phase.
     * 
     * <p>This method determines if the push panel should push robots during
     * the current register phase by checking if the current register is in
     * the list of active registers.</p>
     * 
     * @return true if the push panel should activate in the current register, false otherwise
     */
    public boolean isActiveInCurrentRegister() {
        return activeRegisters.contains(currentRegister);
    }

    /**
     * Activates the push panel effect on a robot.
     * 
     * <p>This method is called when a robot is on the push panel's position.
     * However, push panels do not have an immediate effect when robots enter
     * their position - the pushing effect is handled in the applyEffect method
     * during the appropriate register phase.</p>
     * 
     * @param robot the robot that is on the push panel's position
     */
    @Override
    public void activate(Robot robot) {
        // The push plate will not trigger an effect when the robot enters.
        // The push plate's pushing effect should be handled in the applyEffect method.
    }

    /**
     * Checks whether a robot can pass through this push panel.
     * 
     * <p>Robots can freely move onto push panel positions. The push panel itself
     * does not block movement, but will push robots during the activation phase
     * if it is active in the current register.</p>
     * 
     * @param robot the robot attempting to pass through the push panel
     * @return true - robots can pass through push panel positions
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Gets the type identifier for this board element.
     * 
     * @return the string "PushPanel" identifying this element type
     */
    @Override
    public String getType() {
        return "PushPanel";
    }

    /**
     * Checks whether this push panel is currently placed on a game board.
     * 
     * @return true if the push panel is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this push panel is placed on a game board.
     * 
     * @param isOnBoard true to mark the push panel as being on a board, false otherwise
     */
    public void setOnBoard(boolean isOnBoard) {
        this.isOnBoard = isOnBoard;
    }

    /**
     * Gets the unique identifier of the board this push panel belongs to.
     * 
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this push panel and updates the on-board status accordingly.
     * 
     * <p>If the boardId is not empty, the push panel is marked as being on a board.
     * This method automatically manages the isOnBoard flag based on the boardId value.</p>
     * 
     * @param boardId the unique identifier of the board this push panel belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Applies the push panel effect to a robot if the panel is active in the current register.
     * 
     * <p>This method handles the complete push panel interaction process:</p>
     * <ol>
     *   <li>Activates the push panel effect (no immediate action)</li>
     *   <li>Checks if the push panel is active in the current register</li>
     *   <li>If active, pushes the robot in the panel's direction</li>
     * </ol>
     * 
     * <p>The pushing effect can trigger chain reactions if multiple robots
     * are in the push path, and can cause robots to fall off the board.</p>
     * 
     * @param robot the robot on the push panel's position
     * @param board the game board containing the push panel
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);

        if (isActiveInCurrentRegister()) {
            pushRobot(robot, board);
        }
    }

    /**
     * Pushes a robot in the push panel's direction.
     * 
     * <p>This method handles the core pushing logic:</p>
     * <ul>
     *   <li>Calculates the target position after the push</li>
     *   <li>Checks if the target position is valid (within board boundaries)</li>
     *   <li>Handles robots being pushed off the board (fall and restart)</li>
     *   <li>Manages chain reactions if other robots block the push</li>
     *   <li>Moves robots to their new positions</li>
     * </ul>
     * 
     * @param robot the robot to be pushed
     * @param board the game board containing the robot
     */
    private void pushRobot(Robot robot, Board board) {
        if (direction == null) {
            System.err.println("Error: Push panel direction is not set!");
            return;
        }

        // Calculate the position after the push
        Position currentPos = robot.getPosition();
        Position targetPos = currentPos.move(direction);


        // Check if the target location is valid
        if (!board.isValidPosition(targetPos)) {
            System.out.println("Robot " + robot.getRobotID() + " would be pushed off the board! Robot falls and reboots.");
            // The robot has fallen off the game board and needs to be restarted.
            rebootRobot(robot, board);
            return;
        }

        // Check if there are robots at the target location
        Robot targetRobot = board.getRobotAt(targetPos);
        if (targetRobot != null) {
            //There is a robot at the target location. Try to push it.
            pushRobotChain(robot, targetRobot, board, direction);
        } else {
            //There are no robots at the target location, so move directly there.
            moveRobot(robot, targetPos, board);
        }

        System.out.println("Push panel at " + position + " pushed Robot " + robot.getRobotID() + " to " + targetPos);
    }

    /**
     * Recursively pushes a chain of robots in the specified direction.
     * 
     * <p>This method handles chain reactions when multiple robots are in the push path.
     * It recursively pushes robots until it finds an empty space or a robot falls
     * off the board. The chain reaction ensures that all robots in the path are
     * moved appropriately.</p>
     * 
     * <p>The chain reaction process:</p>
     * <ul>
     *   <li>Calculates the next position for the target robot</li>
     *   <li>Checks if the next position is valid (within board boundaries)</li>
     *   <li>Handles robots being pushed off the board</li>
     *   <li>Recursively continues the chain if more robots are blocking</li>
     *   <li>Moves all robots in the chain to their new positions</li>
     * </ul>
     * 
     * @param sourceRobot the robot that initiated the push
     * @param targetRobot the robot at the target location that needs to be pushed
     * @param board the game board containing the robots
     * @param pushDirection the direction in which to push the robots
     */
    private void pushRobotChain(Robot sourceRobot, Robot targetRobot, Board board, Direction pushDirection) {
        // Calculate the next position
        Position targetPos = targetRobot.getPosition();
        Position nextPos = targetPos.move(pushDirection);

        // Check if the next position is valid
        if (!board.isValidPosition(nextPos)) {
            System.out.println("Robot " + targetRobot.getRobotID() + " would be pushed off the board! Robot falls and reboots.");
            // The target robot has fallen off the game board and needs to be restarted.
            rebootRobot(targetRobot, board);

            // Now the source robot can move to the target position.
            moveRobot(sourceRobot, targetPos, board);
            return;
        }

        // Check if there is a robot at the next position
        Robot nextRobot = board.getRobotAt(nextPos);
        if (nextRobot != null) {
            //There is also a robot at the next location, recursively pushing
            pushRobotChain(targetRobot, nextRobot, board, pushDirection);
        } else {
            moveRobot(targetRobot, nextPos, board);
        }
        moveRobot(sourceRobot, targetPos, board);
    }

    /**
     * Moves a robot to the specified position and applies board element effects.
     * 
     * <p>This method handles the actual movement of a robot to a new position
     * and ensures that any board elements at the new position are activated.
     * This can trigger recursive effects if the new position contains another
     * push panel or other interactive elements.</p>
     * 
     * @param robot the robot to be moved
     * @param targetPos the target position for the robot
     * @param board the game board containing the robot
     */
    private void moveRobot(Robot robot, Position targetPos, Board board) {
        robot.setPosition(targetPos);
        board.updateRobotPosition(robot, targetPos);

        //Apply the board element effect to the target location.
        //Note: This may cause recursive calls if the target location also has a push plate.
        board.applyEffects(robot, targetPos.x(), targetPos.y());
    }


    /**
     * Restarts a robot that has fallen off the game board.
     * 
     * <p>This method handles the reboot process for robots that have been pushed
     * off the board. It applies reboot damage, cancels programming, and notifies
     * the client about the reboot event.</p>
     * 
     * <p>The reboot process includes:</p>
     * <ul>
     *   <li>Adding reboot damage to the robot</li>
     *   <li>Finding the associated player</li>
     *   <li>Broadcasting a reboot message to the client</li>
     * </ul>
     * 
     * @param robot the robot that needs to be restarted
     * @param board the game board containing the robot
     */
    private void rebootRobot(Robot robot, Board board) {
        // Use a unified respawn damage method
        board.addRebootDamage(robot);
//        robot.takeDamage(2);
//        robot.cancelProgramming();

//        Position rebootPosition = board.getRebootPosition();
//        if (rebootPosition != null) {
//            robot.setPosition(rebootPosition);
//            board.updateRobotPosition(robot, rebootPosition);
        Player player = Game.getInstance().getPlayers().stream()
                .filter(p -> p.getRobot() == robot)
                .findFirst()
                .orElse(null);
        if (player != null) {
            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyReboot(robot.getClientID())
            ));
//            System.out.println("Robot " + robot.getRobotID() + " has been rebooted at " + rebootPosition);
//        } else {
//            System.err.println("Error: No reboot position found on the board!");
        }
    }

    /**
     * Returns a string representation of this push panel.
     * 
     * <p>The string includes the push panel's position, board association,
     * pushing direction, and active register numbers for easy identification
     * and debugging purposes.</p>
     * 
     * @return a detailed string describing the push panel's properties
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Push Panel at ").append(position);

        if (isOnBoard) {
            sb.append(" on board ").append(boardId);
        } else {
            sb.append(" not on any board");
        }

        if (direction != null) {
            sb.append(", pushing direction: ").append(direction.getName());
        }

        sb.append(", active in registers: ");
        for (int register : activeRegisters) {
            // +1 Convert to register number 1-5
            sb.append(register + 1).append(", ");
        }

        if (!activeRegisters.isEmpty()) {
            sb.setLength(sb.length() - 2);
        }

        return sb.toString();
    }

    /**
     * Converts this PushPanel object to a FieldPushPanel representation for network communication.
     * 
     * <p>This method is used for serializing the push panel information when sending
     * game state updates to clients. The resulting FieldPushPanel object contains
     * the board ID, direction (displayed in opposite direction for client), and
     * active register configuration in a format suitable for network transmission.</p>
     * 
     * @return a new FieldPushPanel object representing this push panel's network data
     */
    @Override
    public MessageDefinitions.FieldPushPanel toField() {
        Direction displayDirection = direction.turnAround();
        return new MessageDefinitions.FieldPushPanel(boardId, List.of(displayDirection.toString()), activeRegisters);
    }

//    /**
//     * Convert the Direction enumeration to the direction string required by the protocol.
//     */
//    private String directionToString(Direction direction) {
//        if (direction == null) return null;
//        return switch (direction) {
//            case NORTH -> "top";
//            case SOUTH -> "bottom";
//            case EAST -> "right";
//            case WEST -> "left";
//        };
//    }


//    /**
//     * Serialize to protocol format
//     * @return Map that complies with the protocol
//     */
//    public Map<String, Object> serialize() {
//        Map<String, Object> result = new HashMap<>();
//        result.put("type", "PushPanel");
//        result.put("isOnBoard", boardId);
//
//        // Add direction
//        if (direction != null) {
//            List<String> orientations = new ArrayList<>();
//            orientations.add(directionToString(direction));
//            result.put("orientations", orientations);
//        }
//
//        // Add activated registers
//        result.put("registers", activeRegisters);
//
//        return result;
//    }

}
