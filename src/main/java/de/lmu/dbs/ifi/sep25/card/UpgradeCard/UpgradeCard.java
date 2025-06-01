package de.lmu.dbs.ifi.sep25.card.UpgradeCard;

import de.lmu.dbs.ifi.sep25.card.Card;

public abstract class UpgradeCard extends Card {
    public UpgradeCard(String description) {
        super(description, CardType.UPGRADE);
    }

    @Override
    public abstract UpgradeCard clone();
}