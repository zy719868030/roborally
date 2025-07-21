package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Game;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents the energy space elements on the game board.
 * When the robot finishes moving and stays on the energy space, it can collect energy cubes.
 * Once the energy cubes are collected, the energy space will remain empty until the game ends.
 */
public class EnergySpace extends BoardElement {
    private static final Logger appLogger = LogManager.getLogger(EnergySpace.class);



    /** The number of energy cubes contained in this energy space */
    private int energyCount;
    private String boardId;

    /** Flag indicating whether this energy space is placed on a game board */
    private boolean isOnBoard;


    /**
     * Default constructor for energy space.
     *
     * <p>This constructor creates an energy space with default values: 1 energy cube
     * and not collected. It is primarily used for special cases or initialization.</p>
     */
    public EnergySpace() {
        super();
        // According to official standards, each Energy Space initially contains 1 energy block.
        this.energyCount = 1;
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructs an energy space at the specified position with 1 energy cube.
     *
     * <p>This constructor creates a standard energy space containing 1 energy cube.
     * The energy space is associated with a specific game board.</p>
     *
     * @param position the position of the energy space on the game board
     * @param boardId the unique identifier of the board this energy space belongs to
     * @throws IllegalArgumentException if position or boardId is null
     */
    public EnergySpace(Position position, String boardId) {
        super(position, boardId);
        this.energyCount = 1;
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Constructs an energy space at the specified position with a custom energy count.
     *
     * <p>This constructor allows for energy spaces with varying amounts of energy.
     * It is useful for creating special energy spaces that provide more or less
     * energy than the standard 1 cube.</p>
     *
     * @param position the position of the energy space on the game board
     * @param energyCount the number of energy cubes in this space
     * @param boardId the unique identifier of the board this energy space belongs to
     * @throws IllegalArgumentException if position or boardId is null, or energyCount is negative
     */
    public EnergySpace(Position position, int energyCount, String boardId) {
        super(position, boardId);
        this.energyCount = energyCount;
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Checks whether this energy space is currently placed on a game board.
     *
     * @return true if the energy space is on a board, false otherwise
     */
    public boolean isOnBoard() {
        return isOnBoard;
    }

    /**
     * Sets whether this energy space is placed on a game board.
     *
     * @param onBoard true to mark the energy space as being on a board, false otherwise
     */
    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    /**
     * Gets the unique identifier of the board this energy space belongs to.
     *
     * @return the board ID, or an empty string if not associated with any board
     */
    public String getBoardId() {
        return boardId;
    }

    /**
     * Sets the board ID for this energy space and updates the on-board status accordingly.
     *
     * <p>If the boardId is not empty, the energy space is marked as being on a board.</p>
     *
     * @param boardId the unique identifier of the board this energy space belongs to
     * @throws IllegalArgumentException if boardId is null
     */
    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    /**
     * Gets the current number of energy cubes available in this energy space.
     *
     * <p>This method returns the actual available energy, taking into account whether
     * the energy has been collected. If the energy space has been collected, it returns 0.</p>
     *
     * @return the number of energy cubes available (0 if already collected)
     */
    public int getEnergyCount() {
        return energyCount;
    }

    /**
     * Sets the number of energy cubes in this energy space.
     *
     * <p>This method sets the maximum energy capacity of the space. Note that this
     * does not reset the collection status - if the space was already collected,
     * it will remain collected until reset.</p>
     *
     * @param energyCount the new number of energy cubes
     * @throws IllegalArgumentException if energyCount is negative
     */
    public void setEnergyCount(int energyCount) {
        this.energyCount = energyCount;
    }

    /**
     * Resets the energy space to its initial state.
     */
    public void reset() {
        this.energyCount = 1;
    }

    /**
     * Activates the energy space effect on a robot.
     *
     * <p>Energy spaces have no immediate activation effect when robots enter them.
     * The actual energy collection logic is handled in the applyEffect method during
     * the activation phase.</p>
     *
     * @param robot the robot to activate the energy space effect on (unused)
     */
    @Override
    public void activate(Robot robot) {
    }

    /**
     * Checks whether a robot can pass through this energy space.
     *
     * <p>Robots can freely move through energy spaces. The energy space itself
     * does not block movement, but will provide energy when robots are on it
     * during the activation phase.</p>
     *
     * @param robot the robot attempting to pass through the energy space
     * @return true - robots can pass through energy spaces
     */
    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

    /**
     * Gets the type identifier for this board element.
     *
     * @return the string "EnergySpace" identifying this element type
     */
    @Override
    public String getType() {
        return "EnergySpace";
    }

    /**
     * Apply energy space effect to robot based on current register
     *
     * @param robot Robot staying on energy space
     */
    public void applyEffect(Robot robot) {
        activate(robot);
        Game gameInstance = Game.getInstance();
        int currentRegister = gameInstance.getCurrentRegister();

        boolean canCollectEnergy = false;

        // Registers 1-4 (indexes 0-3): Collection is only possible when count > 0.
        if (currentRegister >= 0 && currentRegister <= 3) {
            if (energyCount > 0) {
                energyCount--;
                canCollectEnergy = true;
                appLogger.info("{} collected 1 energy cube from energy space at {} in register {}. Remaining energy count: {}",
                        robot.toString(), position, currentRegister + 1, energyCount);
            } else {
                appLogger.info("No energy left in this energy space for robot {} in register {}", robot.toString(), currentRegister + 1);
            }
            // Register 5 (index 4): Regardless of the grid count, energy can be obtained from the “energy bank.”
        } else if (currentRegister == 4) {
            canCollectEnergy = true;
            appLogger.info("{} collected 1 energy cube from energy bank at {} in register 5. Grid count remains: {}",
                    robot.toString(), position, energyCount);
        }

        if (canCollectEnergy) {
            robot.getPlayer().addEnergy(1, "EnergySpace");
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyAnimation("EnergySpace")
                    )
            );
        }
    }

    /**
     * Apply energy space effect to robot based on current register (with board parameter)
     * This method overrides the base BoardElement method to maintain compatibility.
     *
     * @param robot Robot staying on energy space
     * @param board Game board instance (for compatibility with BoardElement interface)
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        applyEffect(robot);
    }

    /**
     * Returns a string representation of this energy space.
     *
     * <p>The string includes the energy space's position, board association,
     * energy count, and collection status for easy identification and debugging.</p>
     *
     * @return a detailed string describing the energy space's properties
     */
    @Override
    public String toString() {
        return "EnergySpace at " + position +
                (isOnBoard ? " on board " + boardId : " not on any board") +
                ", energy cubes: " + energyCount;
    }

    /**
     * Converts the current EnergySpace object into its corresponding
     * FieldEnergySpace representation for use in message definitions.
     *
     * @return A new instance of {@code MessageDefinitions.FieldEnergySpace}
     * containing the board ID and energy count of this EnergySpace.
     */
    @Override
    public MessageDefinitions.FieldEnergySpace toField() {
        int currentCount = getEnergyCount();
//        appLogger.info("EnergySpace.toField() called: position={}, energyCount={}, boardId={}", position, currentCount, boardId);
        return new MessageDefinitions.FieldEnergySpace(boardId, currentCount);
    }
}