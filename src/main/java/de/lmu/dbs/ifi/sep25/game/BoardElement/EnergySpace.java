package de.lmu.dbs.ifi.sep25.game.BoardElement;

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
        if (energyCount > 0) {
            appLogger.info("{} collected {} energy cube(s) from energy space at {}. Remaining energy count: {}", robot.toString(), energyCount, position, energyCount == 0 ? "unchanged" : energyCount);
            robot.getPlayer().addEnergy(1, "EnergySpace");
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyAnimation("EnergySpace")
                    )
            );
        } else {
            appLogger.info("No energy left in this energy space.");
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
        return new MessageDefinitions.FieldEnergySpace(boardId, getEnergyCount());
    }
}