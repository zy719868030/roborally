package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.Card;
import java.util.Collections;
import java.util.Stack;

public class Deck {
    private Stack<Card> stack;
    private Stack<Card> discardPile;

    public Deck() {
        this.stack = new Stack<>();
        this.discardPile = new Stack<>();
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
}

