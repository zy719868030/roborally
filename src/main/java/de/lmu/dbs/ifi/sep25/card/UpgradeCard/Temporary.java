package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class Temporary extends UpgradeCard {
    // Has the temporary upgrade card already been used?
    private boolean isUsed;

    // Temporary upgrade can be used after purchase.
    public Temporary(String description, int cost) {
        super(description, cost);
        this.isUsed = false;
        this.isActive = true;
    }

    // Expires after use
    @Override
    public void activate(Robot robot) {
        if (!isUsed && isActive) {
            applyTemporaryEffect(robot);
            isUsed = true;
            isActive = false; // 使用后失效
        }
    }

    // Subclass implements specific temporary effects
    public abstract void applyTemporaryEffect(Robot robot);

    @Override
    public boolean canUse(Robot robot) {
        return isActive && !isUsed;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public boolean isPermanent() {
        return false;
    }

    @Override
    public abstract Temporary clone();
}