package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.game.BoardElement.CheckPoints;
import de.lmu.dbs.ifi.sep25.network.ClientHandler;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Represents a player in the RoboRally game, managing their robot, cards, energy,
 * and game state throughout the game session.
 * 
 * <p>The Player class serves as the central entity that connects a human player
 * or AI to the game world. It manages all player-specific game elements including
 * the robot, programming cards, energy system, upgrades, and network communication.</p>
 *
 * 
 * <p>The class implements comprehensive card management with damage integration,
 * where damage cards are mixed with programming cards in the discard pile and
 * shuffled back into the deck, affecting the player's programming options.</p>
 * 
 * <p>Network communication is handled through the ClientHandler connection,
 * allowing real-time synchronization of player state with the game server
 * and other clients.</p>
 *
 */
public class Player {
    
    /** Network connection handler for client-server communication */
    private final ClientHandler connection;
    
    /** Player's display name in the game */
    private final String name;
    
    /** The robot controlled by this player */
    private final Robot robot;
    
    /** Unique client identifier for network communication */
    private final Integer clientID;
    
    /** Current energy points available for upgrades and special actions */
    private int energy = 5;
    
    /** Flag indicating if the player is ready for the current game phase */
    private boolean ready = false;
    
    /** Flag indicating if the player has completed their register programming */
    private boolean readyRegister = false;

    /** Programming register with 5 slots (0-4) for action cards */
    private final List<RegisterCard> register = new ArrayList<>(5);
    
    /** Permanent upgrades (yellow) that persist across rounds */
    private final List<UpgradeCard> permanentUpgrades = new ArrayList<>();
    
    /** Temporary upgrades (red) that expire after use */
    private final List<UpgradeCard> temporaryUpgrades = new ArrayList<>();
    
    /** Current hand of programming cards available for selection */
    private final List<RegisterCard> hand = new ArrayList<>();
    
    /** Deck of programming cards with draw/discard functionality */
    private final Deck<RegisterCard> programmingDeck;
    
    /** Logger for application-level messages and debugging */
    private static final Logger appLogger = LogManager.getLogger(Player.class);

    /**
     * Constructs a new Player with the specified name, robot ID, and network connection.
     * 
     * <p>This constructor initializes a complete player with all necessary game components,
     * including a robot, programming deck, and network communication setup. The player
     * starts with default energy, empty registers, and a full programming deck.</p>
     *
     * @param name the player's display name
     * @param robotID the unique identifier for the player's robot
     * @param connection the network connection handler for client communication
     * @throws IllegalArgumentException if any parameter is null or invalid
     */
    public Player(String name, int robotID, ClientHandler connection) {
        this.name = name;
        this.clientID = connection.getMyID();
        this.robot = new Robot(robotID, clientID);
        this.connection = connection;

        for (int i = 0; i < 5; i++) {
            register.add(null);
        }

        List<RegisterCard> defaultProgrammingCards = Stream.of(
                "MoveI", "MoveI", // 2 Move 1 Space
                "MoveII", "MoveII", // 2 Move 2 Spaces
                "MoveIII", // 1 Move 3 Spaces
                "BackUp", "BackUp", // 2 Back Up
                "TurnLeft", "TurnLeft", // 2 Turn Left
                "TurnRight", "TurnRight", // 2 Turn Right
                "UTurn", "UTurn", // 2 U-Turn
                "Again", "Again", // 2 Again
                "PowerUp" // 1 Power Up
        ).map(CardFactory::createCard).toList();

        programmingDeck = new Deck<>(defaultProgrammingCards);
    }

    // GETTERS

    /**
     * Gets the network connection handler for this player.
     * @return the associated server connection handler
     */
    public ClientHandler getConnection() {
        return connection;
    }

    /**
     * Gets the robot controlled by this player.
     * @return the robot controlled by this player
     */
    public Robot getRobot() {
        return robot;
    }

    /**
     * Gets the unique client identifier for this player.
     * @return the unique client identifier
     */
    public Integer getClientID() {
        return clientID;
    }

    /**
     * Gets a defensive copy of the player's programming register.

     * @return a new ArrayList containing all cards in the programming register
     */
    public List<RegisterCard> getRegister() {
        return new ArrayList<>(register);
    }

    /**
     * Retrieves the card located in the specified register slot.
     *
     * @param registerSlot the index of the register slot to retrieve the card from (0-4)
     * @return the RegisterCard at the specified register slot, or null if the slot is empty
     */
    public RegisterCard getRegisterCard(int registerSlot) {
        return register.get(registerSlot);
    }

    /**
     * Returns a defensive copy of the player's current hand.
     *
     * @return a new ArrayList containing all cards in the player's hand
     */
    public List<RegisterCard> getHand() {
        return new ArrayList<>(hand);
    }

    /**
     * Gets the current discard pile from the programming deck.

     * @return a list of all cards in the discard pile
     */
    public List<RegisterCard> getDiscardPile() {
        return programmingDeck.getDiscardPile();
    }

    /**
     * Gets the player's display name.

     * @return the player's display name
     */
    public String getName() {
        return name;
    }

    /**
     * Checks if the player is ready for the current game phase.
     *
     * @return true if the player is ready, false otherwise
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Checks if the player has completed their register programming.
     *
     * @return true if the player has completed register programming, false otherwise
     */
    public boolean isReadyRegister() {
        return readyRegister;
    }

    /**
     * Gets the player's current energy points.
     * 
     * <p>This method returns the number of energy points the player currently
     * has available. Energy points are the currency used for purchasing
     * upgrades and performing special actions in the game.</p>
     *
     * @return the current number of energy points
     */
    public int getEnergy() {
        return energy;
    }

    /**
     * Gets the number of checkpoints this player has successfully reached.
     * 
     * <p>This method calculates and returns the highest checkpoint number
     * that the player's robot has reached on the game board. Checkpoints
     * are the victory condition in RoboRally, and reaching all checkpoints
     * in order results in winning the game.</p>
     *
     * @return the number of checkpoints reached by the player (0 if none reached)
     */
    public int getReachedCheckpoints() {
        // Assuming we can access the game instance from the player
        Game game = Game.getInstance();
        Board board = game.getBoard();

        // Find the highest checkpoint number reached by this robot
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof CheckPoints) {
                        CheckPoints checkpoint = (CheckPoints) element;
                        // Get the highest checkpoint reached by this robot
                        return checkpoint.getRobotHighestCheckpoint(this.robot.getRobotID());
                    }
                }
            }
        }

        // If no checkpoints found or none reached
        return 0;
    }

    // SETTERS

    ///

//    public void setStartingPoint(int x, int y, String direction) {
//        if (Game.getInstance().getCurrentPhase() != 0) {
//            connection.sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyError("Not in setup phase")
//            ));
//            return;
//        }
//        Board board = Game.getInstance().getBoard();
//        Position pos = new Position(x, y);
//        if (board.isValidPosition(pos) && board.getElements(x, y).stream().anyMatch(e -> e instanceof StartPoint)) {
//            Direction dir = Direction.fromString(direction);
//            if (dir == null) {
//                connection.sendMessage(new MessageDefinitions.Message<>(
//                        new MessageDefinitions.BodyError("Invalid direction: " + direction)
//                ));
//                return;
//            }
//            robot.setPosition(pos);
//            robot.setDirection(dir);
//            board.placeRobot(robot, x, y);
//            connection.broadcastMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyStartingPointTaken(x, y, dir.toString(), robot.getId())
//            ));
//        } else {
//            connection.sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyError("Invalid starting position: (" + x + ", " + y + ")")
//            ));
//        }
//    }
    /**
     * Sets the reboot direction for the player's robot when it has fallen.
     * 
     * <p>This method is called when a robot has fallen into a pit and needs
     * to be rebooted. It sets the direction the robot will face when it
     * respawns at the reboot point.</p>
     *
     * 
     * @param direction the direction string for the robot's reboot orientation
     */
    public void setRebootDirection(String direction) {
        if (Game.getInstance().getCurrentPhase() != 3) {
            connection.sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Not in activation phase")
            ));
            return;
        }
        Direction dir = Direction.fromString(direction);
        if (dir == null) {
            connection.sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Invalid reboot direction: " + direction)
            ));
            return;
        }
        robot.setDirection(dir);
        connection.broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyRebootDirection(dir.toString())
        ));
    }

    /**
     * Sets the general readiness state of the player.

     * 
     * @param ready the new readiness state to set
     */
    public void setReady(boolean ready) {
        this.ready = ready;
    }

    /**
     * Sets the register programming readiness state with synchronization and validation.
     * 
     * <p>This method manages the player's register programming completion state
     * with comprehensive validation and synchronization to ensure proper game flow.
     * The method is synchronized to prevent concurrent modifications that could
     * lead to inconsistent state.</p>
     * 
     * @param ready the new register readiness state to set
     */
    public synchronized void setReadyRegister(boolean ready) {
        Game game = Server.getInstance().getGame();

        if (ready) {
            if (this.readyRegister) {
                appLogger.warn("Ignored duplicate setReadyRegister(true) for {}", getPlayerIdentifier());
                return;
            }

            if (game.getCurrentPhase() != Game.GamePhase.PROGRAMMING.getValue()) {
                appLogger.warn("Blocked setReadyRegister(true) outside PROGRAMMING phase for {}", getPlayerIdentifier());
                return;
            }

            this.readyRegister = true;

//            appLogger.info("{} readyRegister set to: true", getPlayerIdentifier());
        } else {
            this.readyRegister = false;
//            appLogger.info("{} readyRegister set to: false", getPlayerIdentifier());
        }
    }

    // ENERGY

    /**
     * Adds energy points to the player's current energy total.
     * 
     * <p>This method increases the player's energy points by the specified amount
     * and broadcasts the energy change to all clients. Energy points are the
     * currency used for purchasing upgrades and performing special actions.</p>
     *
     * 
     * @param amount the number of energy points to add
     * @param source a string describing the source of the energy gain (e.g., "EnergySpace", "PowerUp")
     */
    public void addEnergy(int amount, String source) {
        this.energy += amount;
        connection.broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyEnergy(clientID, energy, source)
        ));
    }

    /**
     * Attempts to consume energy points for a specific cost.
     * 
     * <p>This method checks if the player has sufficient energy points to cover
     * the specified cost and, if so, deducts the energy and broadcasts the change.
     * If insufficient energy is available, no energy is consumed and the method
     * returns false.</p>
     * 
     * @param cost the number of energy points required for the action
     * @return true if sufficient energy was available and consumed, false otherwise
     */
    public boolean consumeEnergy(int cost) {
        if (energy >= cost) {
            energy -= cost;
            connection.broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyEnergy(clientID, energy, "Consumption")
            ));
            return true;
        }
        return false;
    }

    // CARDS & HAND

    /**
     * Adds a card to the player's hand.
     * 
     * <p>This method adds a programming card to the player's current hand.
     * The card becomes available for selection during the programming phase
     * and can be placed into register slots or discarded as needed.</p>
     *
     * 
     * @param card the RegisterCard to add to the hand
     */
    public void addToHand(RegisterCard card) {
        hand.add(card);
//        updateHand();
    }

    /**
     * Removes a card from the player's hand.
     * 
     * @param card the RegisterCard to remove from the hand
     */
    public void removeFromHand(RegisterCard card) {
        hand.remove(card);
//        updateHand();
    }

    /**
     * Sends the current hand to the client and broadcasts hand information to other players.
     * 
     * <p>This method synchronizes the client's view of the player's hand with the
     * server state and provides other players with information about the hand size
     * for game balance and strategy purposes.</p>
     *
     */
    public void updateHand() {
        final List<String> handWithNames = hand.stream().map(CardFactory::getCardName).toList();
        appLogger.info("Update player {}'s hand: {}", clientID, handWithNames);

        // Always send real cards to the player
        connection.sendMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyYourCards(handWithNames)
        ));

        // In der Programmierphase sehen alle Spieler ihre echten Karten zur gleichzeitigen Auswahl.
        connection.broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyNotYourCards(
                        clientID, handWithNames.size()
                )
        ), connection);
    }

    /**
     * Deals programming cards to the player at the start of a programming phase.
     * 
     * <p>This method prepares the player for the programming phase by clearing
     * their current hand and drawing new cards based on their robot's damage level.
     * The number of cards drawn is affected by damage, with damaged robots receiving
     * fewer programming options.</p>
     *
     */
    public void dealProgrammingCards() {
        appLogger.info("{} dealing programming cards", getPlayerIdentifier());
        // Clear hand
        resetHand();

        // Falsch!!!Draw 9 cards (or fewer if damaged)
//        int cardsToDraw = Math.max(9 - robot.getDamage(), 1);
        // Draw 9 cards each round, regardless of damage taken.
        int cardsToDraw = 9;
        appLogger.debug("{} draws {} cards", getPlayerIdentifier(), cardsToDraw);
        for (int i = 0; i < cardsToDraw; i++) {
            drawCard();
        }

        // Reset register state
        appLogger.info("{} reset register state. Current register: {}", getPlayerIdentifier(), registerToString());
        setReadyRegister(false);
        updateHand();
//        appLogger.info("{} hand has been updated and sent", getPlayerIdentifier());
    }

    /**
     * Draws a card from the programming deck and adds it to the player's hand.
     * 
     * <p>This method draws a single card from the player's programming deck and
     * adds it to their hand. If the deck is empty, it automatically resets the
     * deck by shuffling the discard pile back into the deck.</p>
     *
     * @return the card drawn and added to the hand
     */
    public RegisterCard drawCard() {
        if (programmingDeck.isEmpty()) {
            programmingDeck.reset();
            // Send ShuffleCoding message TODO maybe switch to broadcast, also move to deck shuffle
            connection.broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyShuffleCoding(this.getRobot().getRobotID())
            ));
        }
        RegisterCard card = null;
        if (!programmingDeck.isEmpty()) {
            card = programmingDeck.draw();
        }
//        } else {
//            // Draw from the robot's personal deck (mainly damage cards)
//            Deck<Card> personalDeck = robot.getPersonalDeck();
//            if (personalDeck.isEmpty()) {
//                personalDeck.reset();
//            }
//            card = (RegisterCard) personalDeck.draw();
//        }

        if (card != null) {
            addToHand(card);
        }
        return card;
//        addToHand(programmingDeck.draw());
//        return hand.getLast();
    }

    /**
     * Discards a card from the hand and adds it to the discard pile.
     * 
     * <p>This method removes a card from the player's hand and adds it to the
     * discard pile. The card is no longer available for use in the current round
     * but will be shuffled back into the deck when the deck is reset.</p>
     *
     *
     * @param card the RegisterCard to discard from the hand
     */
    public void discardCardFromHand(RegisterCard card) {
        if (card != null) {
            // Only check your hand for this card during the non-active phase.
            if (Game.getInstance().getCurrentPhase() != Game.GamePhase.ACTIVATION.getValue()) {
                if (!hand.contains(card)) {
                    connection.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Card " + card + " is not in your hand!")));
                    return;
                }
            }
            programmingDeck.discard(card);
            removeFromHand(card);
        }
    }

    /**
     * Selects a card by its name and places it into the specified register slot.
     *
     *
     * @param cardName     The name of the card to select, such as "MoveI", "TurnLeft", etc.
     *                     If null or empty, clears the slot
     * @param registerSlot The index of the register slot to place the card (0-4)
     */
    public void chooseCardToRegister(String cardName, int registerSlot) {
        // First check for Again card in first register
        if (cardName != null && cardName.toLowerCase().contains("again") && registerSlot == 0) {
            connection.sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Again card cannot be played in the first register")
            ));
            return;  // Important: Return here to prevent the card from being placed
        }

        if (!readyRegister) {
            if (registerSlot >= 0 && registerSlot < 5) {
                if (cardName == null || cardName.isEmpty()) {
                    removeCardFromRegister(registerSlot);
                    return;
                }

                // Check if the “Again” card is trying to be placed in the first register.
                if ("Again".equals(cardName) && registerSlot == 0) {
                    appLogger.warn("Player {} attempted to place Again card in first register", connection.getMyID());
                    connection.sendMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyError("Again card cannot be played in the first register!")
                    ));
                    return;
                }

                // Use the factory to find the corresponding card in your hand.
                RegisterCard cardToPlay = CardFactory.findCardInHand(cardName, hand);
                if (cardToPlay != null) {
                    if (register.get(registerSlot) != null) {
                        removeCardFromRegister(registerSlot);
                    }
                    register.set(registerSlot, cardToPlay);
                    appLogger.info("Player {}: {} -> {} <> {}", clientID, cardName, registerSlot, registerToString());
//                    appLogger.info("Current register: {}", registerToString());
                    removeFromHand(cardToPlay);
                    // Notify server that card has been selected
                    connection.broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCardSelected(
                                    clientID, registerSlot, Boolean.TRUE)), connection);

                    // automatic ready check:
                    if (register.stream().allMatch(Objects::nonNull)) {
                        appLogger.info("{} has finished their selection.", this.toString());
                        connection.setReadyRegister();
                    }
                } else {
                    connection.sendMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyError("Card " + cardName + " is not in your hand!")
                    ));
                }
            } else {
                connection.sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyError("Invalid registerSlot number: " + registerSlot
                                + " (must be between 0 and 4)")
                ));
            }
        } else {
            connection.sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("You already selected your registry cards! Called in chooseCardToRegister() method in Player.java. CALLED IN chooseCardToRegister.")
            ));
        }
    }

    /**
     * Removes a card from the specified register slot and returns it to the player's hand.
     * <p>
     * This method clears the specified register slot by removing the card currently placed there
     * and adding it back to the player's hand. If the register is already empty, no action is taken.
     * The method also verifies that the player has not already finalized their register selection
     * and that the provided register slot is within the valid range.
     * <p>
     * After removing the card, the server is notified of this change.
     *
     * @param registerSlot The index of the register slot to clear (0-4)
     * @return true if a card was successfully removed, false otherwise
     */
    public boolean removeCardFromRegister(int registerSlot) {
        if (!readyRegister) {
            if (registerSlot >= 0 && registerSlot < 5) {
                RegisterCard removedCard = register.get(registerSlot);
                if (removedCard != null) {
                    // Remove the card from the register and return it to your hand.
                    appLogger.info("Player {}: {} <- {} <> {}", clientID,
                            CardFactory.getCardName(removedCard), registerSlot, registerToString());
                    register.set(registerSlot, null);
                    addToHand(removedCard);

                    // Notify server that register slot has been cleared
                    connection.broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCardSelected(
                                    clientID, registerSlot, Boolean.FALSE)), connection);

                    return true;
                } else {
                    connection.sendMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyError("Invalid register slot: " + registerSlot + " (Must be between 0 and 4)")
                    ));
                    return false;
                }
            } else {
                connection.sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyError("Invalid register slot number: " + registerSlot + " (must be between 0 and 4)")
                ));
                return false;
            }
        }
//        } else {
//            connection.sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyError("Unable to modify the register: You have completed your card " +
//                            "selection for this round. Please wait for the next round to begin.")
//            ));
//            return false;
//        }
        return false;
    }

    /**
     * Fills the remaining slots of the register with new cards.
     **/
    public void fillRemainingRegisterSlots() {
        resetHand();
        final List<String> cardsYouGotNow = new ArrayList<>();
        for (int i = 0; i < register.size(); i++) {
            if (register.get(i) == null) {
                RegisterCard newCard = drawCard();
                while (i == 0 && CardFactory.getCardName(newCard).equals("Again")) {
                    discardCardFromHand(newCard);
                    newCard = drawCard();
                }
                register.set(i, newCard);
                removeFromHand(newCard);
                appLogger.info("Filled missing slot {} with {} (fallback by server)", i, CardFactory.getCardName(newCard));
                cardsYouGotNow.add(CardFactory.getCardName(newCard));
            }
        }

        connection.sendMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyCardsYouGotNow(cardsYouGotNow))
        );
        // setReadyRegister(true) call needed to simulate push of the ready button
//        appLogger.info("Calling setReadyRegister(false) in fillRemainingRegisterSlots() method in Player.java. CALLED IN fillRemainingRegisterSlots. Current register: {}", registerToString());
        setReadyRegister(false);
    }

    public void addUpgrade(UpgradeCard upgrade) {
        if (consumeEnergy(upgrade.getCost())) {
            if (upgrade.isPermanent()) {
                if (permanentUpgrades.size() == 3) {
                    //TODO add logic to optionally remove 1 card to replace
                } else {
                    permanentUpgrades.add(upgrade);
                }
            } else {
                if (temporaryUpgrades.size() == 3) {
                    //TODO add logic to optionally remove 1 card to replace
                } else {
                    temporaryUpgrades.add(upgrade);
                }
            }
        } else {
            connection.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Not enough energy to upgrade!")));
        }
    }

    //TODO fix as rounds are implemented in game class game main loop
    //public void endRound(){}

    /**
     * Replaces a damage card in the specified register slot with a new programming card.
     * 
     * <p>This method is part of the damage system and is called when a damage card
     * needs to be replaced with a functional programming card. The method draws
     * a new card from the programming deck and places it directly into the specified
     * register slot, effectively replacing the damage card that was there.</p>
     *

     * @param registerSlot the index of the register slot to replace (0-4)
     */
    public void replaceDamageCard(int registerSlot) {
        RegisterCard newCard = programmingDeck.draw();
        register.set(registerSlot, newCard);
        String cardName = CardFactory.getCardName(newCard);
        connection.broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyReplaceCard(registerSlot, cardName, robot.getRobotID())
        ));

        //register.set(registerSlot, programmingDeck.draw());
    }

    // RESET

    /**
     * Process end of round for this player
     */
    public void resetRound() {
        // Clear registers
        resetRegister();

        // Clear hand
        resetHand();
    }

    /**
     * Resets the hand by discarding the remaining cards to the discard pile.
     **/
    public void resetHand() {
        List<RegisterCard> cardsToDiscard = new ArrayList<>(hand);
        for (RegisterCard card : cardsToDiscard)
            discardCardFromHand(card);
    }

    /**
     * Resets the register. Fills the register with null objs and sets the flag readyRegister to false.
     **/
    public void resetRegister() {
        setReadyRegister(false);

        appLogger.info("Resetting register for {}. Current register: {}", getPlayerIdentifier(), registerToString());
        // Move cards from registers to discard pile
        for (int i = 0; i < register.size(); i++) {
            RegisterCard card = register.get(i);
            if (card != null) {
//                appLogger.info("{} clearing register slot {} and discarding card", getPlayerIdentifier(), i);
                removeCardFromRegister(i);
                discardCardFromHand(card);
            }
        }

        // Fill register with null and set flag
        for (int i = 0; i < 5; i++) {
            register.set(i, null);
        }

        appLogger.info("Register has successfully been reset");
    }

    // UTILITY

    /**
     * Converts the player's register to a string representation.
     * The register is represented as a map, where the key is the index of the register slot,
     * and the value is the name of the card in that slot. Card names are retrieved using
     * the {@code CardFactory} class.
     *
     * @return a string representation of the player's register, where each entry maps
     * register indices to the corresponding card names
     */
    public String registerToString() {
        try {
            return IntStream.range(0, register.size())
                    .boxed()
                    .collect(Collectors.toMap(
                            i -> i,
                            i -> {
                                RegisterCard card = register.get(i);
                                return card != null ? CardFactory.getCardName(card) : "EMPTY";
                            }
                    )).toString();
        } catch (NullPointerException e) {
            return "Register not yet initialized. (Nullpointer exception)";
        }
    }

    /**
     * Helper method to provide a consistent player identifier for logging.
     * Format: Player [clientID=X, robotID=Y, name=Z]
     *
     * @return Formatted player identifier string
     */
    private String getPlayerIdentifier() {
        return String.format("Player [clientID=%d, robotID=%d, name='%s']",
                clientID, robot.getRobotID(), name);
    }

    /**
     * Get the player's programming card deck.
     * Used for the damage card mechanism: when injured, damage cards are placed in the discard pile and mixed with programming cards during the next shuffle.
     *
     * @return The player's programming card deck (Deck<RegisterCard>).
     */
    public Deck<RegisterCard> getProgrammingDeck() {
        return programmingDeck;
    }

    /**
     * Returns a string representation of the Player object, providing detailed information
     * about the player's attributes including client ID, robot details, name, and register contents.
     *
     * @return a formatted string representing the Player object with its client ID, robot ID,
     * name, robot state, and the string representation of the register.
     */
    public String toString() {
        return getPlayerIdentifier() + " Register=" + registerToString();
//        return "Player{" +
//                "clientID=" + clientID +
//                ", robotID='" + robot.getRobotID() +
//                ", name='" + name + '\'' +
//                ", robot=" + robot.toString() +
//                ", register=" + registerToString() +
//                '}';
    }
}