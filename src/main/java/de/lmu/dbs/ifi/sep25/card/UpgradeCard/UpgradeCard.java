package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

public abstract class UpgradeCard extends Card {
    public UpgradeCard(String description) {
        super(description, CardType.UPGRADE);
    }

    @Override
    public abstract UpgradeCard clone();
}