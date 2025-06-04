package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class UpgradeCard extends Card {
    protected int cost;
    protected boolean isActive;

    public UpgradeCard(String description, int cost) {
        super(description, CardType.UPGRADE);
        this.cost = cost;
        this.isActive = false;
    }

    // Upgrade cards are not executed in the register, but remain effective after being equipped.
    @Override
    public void execute(Robot robot, Player player) {
        activate(robot);
    }

    // Activate the upgrade card effect
    public abstract void activate(Robot robot);

    // Check if the upgrade card can be used
    public boolean canUse(Robot robot) {
        return isActive;
    }

    public int getCost() {
        return cost;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        this.isActive = active;
    }

    public abstract  boolean isPermanent();

    @Override
    public abstract UpgradeCard clone();
}