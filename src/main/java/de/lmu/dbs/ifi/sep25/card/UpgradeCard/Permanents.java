package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

import de.lmu.dbs.ifi.sep25.game.Robot;

public abstract class Permanents extends UpgradeCard {

    // Permanent upgrades are activated immediately once equipped.
    public Permanents(String description, int cost) {
        super(description, cost);
        this.isActive = true;
    }

    // Activation of permanent upgrade cards is ongoing.
    @Override
    public void activate(Robot robot) {
        // Here can apply some persistent effects.
        applyPermanentEffect(robot);
    }

    // Subclasses implement specific permanent effects.
    public abstract void applyPermanentEffect(Robot robot);

    @Override
    public abstract Permanents clone();
}