package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;
//TODO @yu add generic type to enable deck for: damage cards, register cards, upgrade cards....
public class Deck {
    private Stack<Card> stack;
    private Stack<Card> discardPile;
    private List<DamageCard> damageCards;

    public Deck() {
        this.stack = new Stack<>();
        this.discardPile = new Stack<>();
        this.damageCards = new ArrayList<>();
        initializeDamageCards();
    }

    public Deck(Card[] cards) {
        this();
        for (Card card : cards) {
            this.stack.push(card);
        }
        shuffle();
    }

    public void shuffle() {
        Collections.shuffle(this.stack);
    }


    // Add different types of damage cards to the deck.
    private void initializeDamageCards() {
        // SPAM Card - Causes damage
        for (int i = 0; i < 20; i++) {
            damageCards.add(new DamageCard("SPAM", DamageCard.DamageType.SPAM));
        }

        // WORM Card - Causes restart
        for (int i = 0; i < 6; i++) {
            damageCards.add(new DamageCard("WORM", DamageCard.DamageType.WORM));
        }

        // VIRUS Card - Spreads to nearby robots
        for (int i = 0; i < 6; i++) {
            damageCards.add(new DamageCard("VIRUS", DamageCard.DamageType.VIRUS));
        }

        // TROJAN HORSE Card - Causes additional damage
        for (int i = 0; i < 6; i++) {
            damageCards.add(new DamageCard("TROJAN HORSE", DamageCard.DamageType.TROJAN_HORSE));
        }
    }

    /**
     * Draw a card from the top of the deck.
     * If the deck is empty, reset the deck (return the cards in the discard pile to the deck and shuffle).
     *
     * @return The drawn card. If both the deck and discard pile are empty, return null.
     */
    public Card draw() {
        // If the deck is empty, try to reset it.
        if (stack.isEmpty()) {
            reset();
            if (stack.isEmpty()) {
                return null;
            }
        }
        return stack.pop();
    }

    // Obtain a specific type of damage card
    public DamageCard getDamageCard(DamageCard.DamageType type) {
        for (DamageCard card : damageCards) {
            if (card.getDamageType() == type) {
                return card.clone();
            }
        }
        return null;
    }

    /**
     * Return cards from the discard pile to the deck and shuffle the deck.
     */
    public void reset() {
        while (!discardPile.isEmpty()) {
            stack.push(discardPile.pop());
        }
        if (!stack.isEmpty()) {
            shuffle();
        }
    }

     /**
     * Put the card into the discard pile.
     *
     * @param card The card to be put into the discard pile.
     */
    public void discard(Card card) {
        if (card != null) {
            discardPile.push(card);
        }
    }

    /**
     * Get the number of cards remaining in the deck.
     *
     * @return The number of cards in the deck.
     */
    public int getSize() {
        return stack.size();
    }

    /**
     * Get the number of cards in the discard pile.
     *
     * @return The number of cards in the discard pile.
     */
    public int getDiscardSize() {
        return discardPile.size();
    }

    /**
     * Checks whether the deck is empty.
     *
     * @return Returns true if the deck is empty, otherwise returns false.
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /**
     * Add a card to the deck.
     *
     * @param card The card to be added.
     */
    public void addCard(Card card) {
        if (card != null) {
            stack.push(card);
        }
    }

    public List<DamageCard> getDamageCards() {
        return damageCards;
    }

    public Stack<Card> getStack() {
        return stack;
    }

    public Stack<Card> getDiscardPile() {
        return discardPile;
    }

    /**
     * Adds a list of cards to the deck by processing each card individually.
     *
     * @param cards A list of Card objects to be added to the deck. Each card in the list is added using the addCard(Card) method.
     */
    public void addCard(List<Card> cards) {
        for (Card card : cards) {
            addCard(card);
        }
    }
}

