package de.lmu.dbs.ifi.sep25.card.DamageCard;

import java.util.ArrayList;
import java.util.List;

/**
 * Damage Card Pool - Global singleton that manages all damage cards.
 * Unlike personal programming card decks, damage cards are shared resources that are distributed on demand.
 */
public class DamageCardPool {
    private static DamageCardPool instance;
    private final List<DamageCard> spamCards;
    private final List<DamageCard> wormCards;
    private final List<DamageCard> virusCards;
    private final List<DamageCard> trojanCards;

    private DamageCardPool() {
        this.spamCards = new ArrayList<>();
        this.wormCards = new ArrayList<>();
        this.virusCards = new ArrayList<>();
        this.trojanCards = new ArrayList<>();
        initializeDamageCards();
    }

    /**
     * Get the singleton instance of the damage card pool.
     */
    public static DamageCardPool getInstance() {
        if (instance == null) {
            instance = new DamageCardPool();
        }
        return instance;
    }

    /**
     * Initialize all damage cards
     */
    private void initializeDamageCards() {
        // SPAM Card - Causes damage
        for (int i = 0; i < 20; i++) {
            spamCards.add(new DamageCard("SPAM", DamageCard.DamageType.SPAM));
        }

        // WORM Card - Causes restart
        for (int i = 0; i < 6; i++) {
            wormCards.add(new DamageCard("WORM", DamageCard.DamageType.WORM));
        }

        // VIRUS Card - Spreads to nearby robots
        for (int i = 0; i < 6; i++) {
            virusCards.add(new DamageCard("VIRUS", DamageCard.DamageType.VIRUS));
        }

        // TROJAN HORSE Card - Causes additional damage
        for (int i = 0; i < 6; i++) {
            trojanCards.add(new DamageCard("TROJAN HORSE", DamageCard.DamageType.TROJAN_HORSE));
        }
    }

    /**
     * Get a damage card of the specified type.
     * @param type Damage card type.
     * @return A copy of the damage card. If there are no cards of this type left, return null.
     */
    public DamageCard getDamageCard(DamageCard.DamageType type) {
        List<DamageCard> targetList = getCardListByType(type);

        if (targetList != null && !targetList.isEmpty()) {
            // Return a copy instead of the original card to protect the original card pool.
            DamageCard original = targetList.get(0);
            return original.clone();
        }

        System.out.println("Warning: No more " + type + " damage cards available!");
        return null;
    }

    /**
     * Get the corresponding card list based on type.
     */
    private List<DamageCard> getCardListByType(DamageCard.DamageType type) {
        switch (type) {
            case SPAM:
                return spamCards;
            case WORM:
                return wormCards;
            case VIRUS:
                return virusCards;
            case TROJAN_HORSE:
                return trojanCards;
            default:
                return null;
        }
    }

    /**
     * Check if there are any remaining damage cards of the specified type in stock.
     */
    public boolean hasAvailable(DamageCard.DamageType type) {
        List<DamageCard> targetList = getCardListByType(type);
        return targetList != null && !targetList.isEmpty();
    }

    /**
     * Get the remaining number of damage cards of a specified type.
     */
    public int getAvailableCount(DamageCard.DamageType type) {
        List<DamageCard> targetList = getCardListByType(type);
        return targetList != null ? targetList.size() : 0;
    }

    /**
     * Reset damage card pool (used when starting a new game)
     */
    public void reset() {
        spamCards.clear();
        wormCards.clear();
        virusCards.clear();
        trojanCards.clear();
        initializeDamageCards();
    }

    /**
     * Get the total number of damage cards
     */
    public int getTotalCards() {
        return spamCards.size() + wormCards.size() + virusCards.size() + trojanCards.size();
    }

    /**
     * Print damage card pool status (for debugging)
     */
    public void printStatus() {
        System.out.println("--- Damage Card Pool Status ---");
        System.out.println("SPAM: " + spamCards.size());
        System.out.println("WORM: " + wormCards.size());
        System.out.println("VIRUS: " + virusCards.size());
        System.out.println("TROJAN: " + trojanCards.size());
        System.out.println("Total: " + getTotalCards());
    }
}
