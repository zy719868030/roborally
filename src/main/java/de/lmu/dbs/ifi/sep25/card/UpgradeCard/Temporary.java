package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

public class Temporary extends UpgradeCard {
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
            System.out.println("Robot " + robot.getId() + " nutzt temporäres Upgrade: " + description);
            usesRemaining--;
        } else {
            System.out.println("Keine Verwendungen mehr für: " + description);
        }
    }

    @Override
    public Temporary clone() {
        return new Temporary(this.description, this.usesRemaining);
    }
}