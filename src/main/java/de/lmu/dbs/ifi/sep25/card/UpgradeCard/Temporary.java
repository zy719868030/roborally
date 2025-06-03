package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

import de.lmu.dbs.ifi.sep25.game.Robot;

public class Temporary extends UpgradeCard {
    // Remaining availability, e.g. expires after 3 times
    private int usesRemaining;

    public Temporary(String description, int usesRemaining) {
        super(description);
        this.usesRemaining = usesRemaining;
    }

    public int getUsesRemaining() {
        return usesRemaining;
    }

    @Override
    public void execute(Robot robot) {
        if (usesRemaining > 0) {
            // Print a log of robot usage upgrades
            System.out.println("Robot " + robot.getId() + " uses temporary upgrade: " + description);
            usesRemaining--;
        } else {
            // Prompts that there are no more times left
            System.out.println("No more uses for " + description);
        }
    }

    @Override
    public Temporary clone() {
        return new Temporary(this.description, this.usesRemaining);
    }
}