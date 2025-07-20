package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

/**
 * Represents the energy space elements on the game board.
 * When the robot finishes moving and stays on the energy space, it can collect energy cubes.
 * Once the energy cubes are collected, the energy space will remain empty until the game ends.
 */
public class EnergySpace extends BoardElement {
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
     * Collect energy according to register rules.
     *
     * @param registerIndex Register index (0-4, corresponding to registers 1-5).
     * @return Actual amount of energy collected.
     */
    public int collectEnergyForRegister(int registerIndex) {
        if (registerIndex < 0 || registerIndex > 4) {
            return 0;
        }

        // Registers 1-4 (Index 0-3)
        if (registerIndex <= 3) {
            if (energyCount > 0) {
                energyCount--;
                return 1;
            } else {
                return 0;
            }
        } else { // Register 5 (Index 4)
            // Regardless of whether there are energy blocks on the grid, 1 energy can be obtained from the “energy bank.”
            // Note: Does not change the grid's energyCount.
            return 1;
        }
    }

    /**
     * Check whether energy can be collected in the specified register.
     */
    public boolean canCollectEnergyInRegister(int registerIndex) {
        if (registerIndex < 0 || registerIndex > 4) {
            return false;
        }

        if (registerIndex <= 3) {
            return energyCount > 0;
        } else {
            return true;
        }
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
     * @param board Game board
     * @param registerIndex Current register index (0-4 for registers 1-5)
     * @return Amount of energy collected
     */
    public int applyEffectWithRegister(Robot robot, Board board, int registerIndex) {
        activate(robot);

        int energyCollected = collectEnergyForRegister(registerIndex);

        if (energyCollected > 0) {
            System.out.println("Robot " + robot.getRobotID() + " collected " + energyCollected +
                    " energy cube(s) from energy space at " + position +
                    " during register " + (registerIndex + 1) +
                    ". Remaining energy count: " + energyCount);
        }

        return energyCollected;
    }

    /**
     * To maintain backward compatibility, the original applyEffect method is retained.
     * However, this method does not know the current register, so it can only be processed according to the rules for registers 1-4.
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        applyEffectWithRegister(robot, board, 0);
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
     *         containing the board ID and energy count of this EnergySpace.
     */
    @Override
    public MessageDefinitions.FieldEnergySpace toField() {
        return new MessageDefinitions.FieldEnergySpace(boardId, getEnergyCount());
    }
}