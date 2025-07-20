package de.lmu.dbs.ifi.sep25.game.BoardElement;

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


    private int energyCount;
    private String boardId;
    private boolean isOnBoard;


    public EnergySpace() {
        super();
        // According to official standards, each Energy Space initially contains 1 energy block.
        this.energyCount = 1;
        this.isOnBoard = false;
        this.boardId = "";
    }

    /**
     * Constructor with position parameter, creates an energy space containing 1 energy cube.
     *
     * @param position Position of the energy space.
     */
    public EnergySpace(Position position, String boardId) {
        super(position, boardId);
        this.energyCount = 1;
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    public EnergySpace(Position position, int energyCount, String boardId) {
        super(position, boardId);
        this.energyCount = energyCount;
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    public boolean isOnBoard() {
        return isOnBoard;
    }

    public void setIsOnBoard(boolean onBoard) {
        isOnBoard = onBoard;
    }

    public String getBoardId() {
        return boardId;
    }

    public void setBoardId(String boardId) {
        this.boardId = boardId;
        this.isOnBoard = !boardId.isEmpty();
    }

    public int getEnergyCount() {
        return energyCount;
    }

    public void setEnergyCount(int energyCount) {
        this.energyCount = energyCount;
    }

    /**
     * Resets the energy space to its initial state.
     */
    public void reset() {
        this.energyCount = 1;
    }

    @Override
    public void activate(Robot robot) {
    }

    @Override
    public boolean canPassThrough(Robot robot) {
        return true;
    }

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
        appLogger.info("EnergySpace.toField() called: position={}, energyCount={}, boardId={}", position, currentCount, boardId);

        // Ensure that the correct parameter order and values are passed
        MessageDefinitions.FieldEnergySpace field = new MessageDefinitions.FieldEnergySpace(boardId, currentCount);
        appLogger.info("Created FieldEnergySpace with count: {}", field.count());
//        return new MessageDefinitions.FieldEnergySpace(boardId, getEnergyCount());
        appLogger.info("FieldEnergySpace type: {}", field.type());
        appLogger.info("FieldEnergySpace isOnBoard: {}", field.isOnBoard());
        return field;
    }
}