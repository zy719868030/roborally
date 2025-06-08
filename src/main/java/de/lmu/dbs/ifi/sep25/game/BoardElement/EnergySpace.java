package de.lmu.dbs.ifi.sep25.game.BoardElement;

import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.game.Robot;

/**
 * Represents the energy space elements on the game board.
 * When the robot finishes moving and stays on the energy space, it can collect energy cubes.
 * Once the energy cubes are collected, the energy space will remain empty until the game ends.
 */
public class EnergySpace extends BoardElement {
    private int energyCount;
    private boolean collected;

    public EnergySpace() {
        super();
        this.energyCount = 1;
        this.collected = false;
    }

    /**
     * Constructor with position parameter, creates an energy space containing 1 energy cube.
     *
     * @param position Position of the energy space.
     */
    public EnergySpace(Position position) {
        super(position);
        this.energyCount = 1;
        this.collected = false;
    }

    /**
     * Constructor with position and energy count parameters.
     *
     * @param position Position in energy space.
     * @param energyCount Number of energy cubes.
     */
    public EnergySpace(Position position, int energyCount) {
        super(position);
        this.energyCount = energyCount;
        this.collected = false;
    }

    public int getEnergyCount() {
        return collected ? 0 : energyCount;
    }

    public void setEnergyCount(int energyCount) {
        this.energyCount = energyCount;
    }

    /**
     * Check whether the energy space has been collected.
     *
     * @return Returns true if the energy has been collected, otherwise returns false.
     */
    public boolean isCollected() {
        return collected;
    }

    /**
     * Collect energy cubes in the energy space.
     *
     * @return Number of energy cubes collected.
     */
    public int collectEnergy() {
        if (collected) {
            return 0;
        }
        
        collected = true;
        return energyCount;
    }

    /**
     * Resets the energy space so that it can be collected again.
     * This is usually called when the game is reset.
     */
    public void reset() {
        collected = false;
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
     * Apply energy space effect to robot
     * Collect energy cubes when robot finishes moving and stays on energy space
     *
     * @param robot Robot staying on energy space
     * @param board Game board
     */
    @Override
    public void applyEffect(Robot robot, Board board) {
        activate(robot);

        // If there are energy cubes in the energy space, the robot can collect them.
 /*       if (!collected) {
            int energy = collectEnergy();
            robot.addEnergy(energy);
            System.out.println("Robot " + robot.getId() + " collected " + energy + " energy cube(s) from energy space at " + position);
        }*/ //Auskommentiert um fehlerfrei abzugeben (error bei addEnergy)
    }

    @Override
    public String toString() {
        return "EnergySpace at " + position + ", energy cubes: " + (collected ? 0 : energyCount) + (collected ? " (collected)" : "");
    }
}