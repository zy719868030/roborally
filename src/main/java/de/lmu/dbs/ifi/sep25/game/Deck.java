package de.lmu.dbs.ifi.sep25.game;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

/**
 * Represents a generic deck of cards in the RoboRally game.
 * 
 * <p>The Deck class provides a flexible card management system that can handle
 * any type of cards (programming cards, damage cards, upgrade cards, etc.).
 * It implements standard deck operations including drawing, discarding, shuffling,
 * and automatic deck reset when the main deck is exhausted.</p>
 *
 */
public class Deck <Card>{
    
    /** The main deck stack containing cards available for drawing */
    private final Stack<Card> stack = new Stack<>();
    
    /** The discard pile stack containing used cards */
    private final Stack<Card> discardPile = new Stack<>();

    /**
     * Constructs an empty deck with no cards.
     * 
     * <p>This constructor creates a new deck with empty main stack and discard pile.
     * Cards can be added later using the addCard methods.</p>
     */
    public Deck() {
    }

    /**
     * Constructs a deck with the specified list of cards.
     * 
     * <p>This constructor creates a new deck and populates it with the provided cards.
     * After adding all cards, the deck is automatically shuffled to ensure random
     * card distribution.</p>
     * 
     * @param cards the list of cards to initialize the deck with
     * @throws IllegalArgumentException if cards is null
     */
    public Deck(List<Card> cards) {
        this();
        for (Card card : cards) {
            this.stack.push(card);
        }
        shuffle();
    }

    /**
     * Shuffles the main deck to randomize card order.
     * 
     * <p>This method uses Java's Collections.shuffle() to randomize the order
     * of cards in the main deck. This ensures fair and unpredictable card
     * distribution during gameplay.</p>
     */
    public void shuffle() {
        Collections.shuffle(this.stack);
    }

//    // Add different types of damage cards to the deck.
//    private void initializeDamageCards() {
//        // SPAM Card - Causes damage
//        for (int i = 0; i < 20; i++) {
//            damageDeck.addCard(new DamageCard("SPAM", DamageCard.DamageType.SPAM));
//        }
//
//        // WORM Card - Causes restart
//        for (int i = 0; i < 6; i++) {
//            damageDeck.addCard(new DamageCard("WORM", DamageCard.DamageType.WORM));
//        }
//
//        // VIRUS Card - Spreads to nearby robots
//        for (int i = 0; i < 6; i++) {
//            damageDeck.addCard(new DamageCard("VIRUS", DamageCard.DamageType.VIRUS));
//        }
//
//        // TROJAN HORSE Card - Causes additional damage
//        for (int i = 0; i < 6; i++) {
//            damageDeck.addCard(new DamageCard("TROJAN HORSE", DamageCard.DamageType.TROJAN_HORSE));
//        }
//    }
//    Obtain a specific type of damage card
//    public DamageCard getDamageCard(DamageCard.DamageType type) {
//        for (DamageCard card : damageCards) {
//            if (card.getDamageType() == type) {
//                return card.clone();
//            }
//        }
//        return null;
//    }

    /**
     * Draws a card from the top of the deck.
     * 
     * <p>This method removes and returns the top card from the main deck. If the
     * main deck is empty, it automatically attempts to reset the deck by moving
     * all cards from the discard pile back to the main deck and shuffling them.</p>
     * 
     * <p>The drawing process:</p>
     * <ol>
     *   <li>Check if the main deck is empty</li>
     *   <li>If empty, reset the deck from the discard pile</li>
     *   <li>If still empty after reset, return null</li>
     *   <li>Otherwise, pop and return the top card</li>
     * </ol>
     * 
     * @return the drawn card, or null if both deck and discard pile are empty
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
     * Resets the deck by moving all cards from the discard pile back to the main deck.
     * 
     * <p>This method transfers all cards from the discard pile to the main deck
     * and then shuffles the deck to ensure random card distribution. This is
     * typically called automatically when the main deck is exhausted, but can
     * also be called manually if needed.</p>
     * 
     * <p>The reset process:</p>
     * <ul>
     *   <li>Move all cards from discard pile to main deck</li>
     *   <li>Shuffle the main deck if it contains cards</li>
     * </ul>
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
     * Adds a card to the discard pile.
     * 
     * <p>This method places a used card into the discard pile. The card will
     * remain in the discard pile until the deck is reset, at which point all
     * discarded cards will be shuffled back into the main deck.</p>
     * 
     * @param card the card to be added to the discard pile
     */
    public void discard(Card card) {
        if (card != null) {
            discardPile.push(card);
        }
    }

    /**
     * Gets a copy of all cards in the discard pile.
     * 
     * <p>This method returns a new ArrayList containing all cards currently
     * in the discard pile. The returned list is a copy, so modifications to
     * it will not affect the actual discard pile.</p>
     * 
     * @return a new ArrayList containing all cards in the discard pile
     */
    public List<Card> getDiscard() {
        return new ArrayList<>(discardPile);
    }

    /**
     * Gets the number of cards remaining in the main deck.
     * 
     * <p>This method returns the current size of the main deck stack,
     * indicating how many cards are available for drawing.</p>
     *
     * @return the number of cards in the main deck
     */
    public int getSize() {
        return stack.size();
    }

    /**
     * Gets the number of cards in the discard pile.
     * 
     * <p>This method returns the current size of the discard pile stack,
     * indicating how many cards have been discarded.</p>
     *
     * @return the number of cards in the discard pile
     */
    public int getDiscardSize() {
        return discardPile.size();
    }

    /**
     * Checks whether the main deck is empty.
     * 
     * <p>This method determines if there are any cards remaining in the main deck.
     * Note that even if the main deck is empty, there may still be cards in the
     * discard pile that can be used to reset the deck.</p>
     * 
     * @return true if the main deck is empty, false otherwise
     */
    public boolean isEmpty() {
        return stack.isEmpty();
    }

    /**
     * Adds a single card to the main deck.
     * 
     * <p>This method adds one card to the top of the main deck. If the card
     * is null, it is ignored and not added to the deck.</p>
     *
     * @param card the card to be added to the deck
     */
    public void addCard(Card card) {
        if (card != null) {
            stack.push(card);
        }
    }

//    public List<DamageCard> getDamageCards() {
//        return damageCards;
//    }

    /**
     * Gets the main deck stack.
     * 
     * <p>This method returns a reference to the main deck stack. Note that
     * direct manipulation of the returned stack may affect the deck's state.</p>
     * 
     * @return the main deck stack
     */
    public Stack<Card> getStack() {
        return stack;
    }

    /**
     * Gets the discard pile stack.
     * 
     * <p>This method returns a reference to the discard pile stack. Note that
     * direct manipulation of the returned stack may affect the deck's state.</p>
     * 
     * @return the discard pile stack
     */
    public Stack<Card> getDiscardPile() {
        return discardPile;
    }

    /**
     * Adds multiple cards to the deck.
     * 
     * <p>This method processes a list of cards and adds each non-null card
     * to the main deck using the addCard(Card) method. This provides a convenient
     * way to add multiple cards at once.</p>
     * 
     * @param cards the list of cards to be added to the deck
     * @throws IllegalArgumentException if cards is null
     */
    public void addCard(List<Card> cards) {
        for (Card card : cards) {
            addCard(card);
        }
    }

}

