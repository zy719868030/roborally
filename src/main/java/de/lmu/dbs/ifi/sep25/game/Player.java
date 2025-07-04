package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.game.BoardElement.CheckPoints;
import de.lmu.dbs.ifi.sep25.network.ClientHandler;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class Player {
    private final ClientHandler connection;
    private final String name;
    private final Robot robot;
    private int energy = 5;
    private boolean ready = false;
    private boolean readyRegister = false;

    private final List<RegisterCard> register = new ArrayList<>(5);
    private final List<UpgradeCard> permanentUpgrades = new ArrayList<>();
    private final List<UpgradeCard> temporaryUpgrades = new ArrayList<>();
    private final List<RegisterCard> hand = new ArrayList<>();
    private final Deck<RegisterCard> programmingDeck;
    private static final Logger appLogger = LogManager.getLogger(Player.class);


    // INITIALIZATION

    public Player(String name, int robotID, ClientHandler connection) {
        this.name = name;
        this.robot = new Robot(robotID);
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
     * Getter connection
     *
     * @return associated server connection
     **/
    public ClientHandler getConnection() {
        return connection;
    }

    public Robot getRobot() {
        return robot;
    }

    public List<RegisterCard> getRegister() {
        return new ArrayList<>(register);
    }

    /**
     * Returns a copy of the player hand.
     *
     * @return List of RegisterCard objects, representing a copy of the players actual hand
     **/
    public List<RegisterCard> getHand() {
        return new ArrayList<>(hand);
    }

    public List<RegisterCard> getDiscardPile() {
        return programmingDeck.getDiscardPile();
    }

    public String getName() {
        return name;
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isReadyRegister() {
        return readyRegister;
    }

    public int getEnergy() {
        return energy;
    }

    /**
     * Gets the number of checkpoints this player has successfully reached.
     * This method delegates to the game's checkpoints to find the highest checkpoint
     * reached by this player's robot.
     *
     * @return the number of checkpoints reached by the player
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
                        return checkpoint.getRobotHighestCheckpoint(this.robot.getId());
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

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public void setReadyRegister(boolean ready) {
        readyRegister = ready;
        if (ready) {
            connection.setReadyRegister();
        }
    }

    // ENERGY

    public void addEnergy(int amount, String source) {

        this.energy += amount;
        connection.broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyEnergy(robot.getId(), energy, source)
        ));
    }

    public boolean consumeEnergy(int cost) {
        if (energy >= cost) {
            energy -= cost;
            connection.broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyEnergy(robot.getId(), energy, "Consumption")
            ));
            return true;
        }
        return false;
    }

    // CARDS & HAND

    /**
     * Adds a card to the hand and sends the new hand to the clients.
     **/
    public void addToHand(RegisterCard card) {
        hand.add(card);
        updateHand();
    }

    /**
     * Removes a card from the hand and updates the clients.
     **/
    public void removeFromHand(RegisterCard card) {
        hand.remove(card);
        updateHand();
    }

    /**
     * Sends the current hand to the server.
     **/
    public void updateHand() {
        final List<String> handWithNames = hand.stream().map(CardFactory::getCardName).toList();
        appLogger.info("Update player {}'s hand: {}", robot.getId(), handWithNames);

        // Always send real cards to the player
        connection.sendMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyYourCards(handWithNames)
        ));

        // In der Programmierphase sehen alle Spieler ihre echten Karten zur gleichzeitigen Auswahl.
        if (Game.getInstance().getCurrentPhase() == Game.GamePhase.ACTIVATION.getValue()) {
            connection.broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyNotYourCards(
                            connection.getMyID(), handWithNames.size()
                    )
            ), connection);
        }

        appLogger.info("Hand information sent.");
    }



    /**
     * Deal cards to player at the start of a programming phase
     */
    public void dealProgrammingCards() {
        appLogger.info("Deal cards to players {}", robot.getId());
        // Clear hand
        resetHand();

        // Draw 9 cards (or fewer if damaged)
        int cardsToDraw = Math.max(9 - robot.getDamage(), 1);
        System.out.println("[DEBUG] Player " + robot.getId() + " draws " + cardsToDraw + " cards");
        for (int i = 0; i < cardsToDraw; i++) {
            drawCard();
        }

        // Reset register state
        setReadyRegister(false);
        updateHand();
        appLogger.info("Player {}'s hand has been updated and sent.", robot.getId());
    }

    /**
     * Draws a card from the programming deck. Adds the card to hand and updates clients.
     *
     * @return the card drawn and added to hand.
     **/
    public RegisterCard drawCard() {
        if (programmingDeck.isEmpty()) {
            programmingDeck.reset();
            // Send ShuffleCoding message
            connection.broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyShuffleCoding(this.getRobot().getId())
            ));
        }
        addToHand(programmingDeck.draw());
        return hand.getLast();
    }

    /**
     * Discards a card from the hand and adds it to the discard pile.
     **/
    public void discardCardFromHand(RegisterCard card) {
        if (card != null) {
            if (hand.contains(card)) {
                programmingDeck.discard(card);
                removeFromHand(card);
            } else {
                connection.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Card " + card + " is not in your hand!")));
            }
        }
    }

    /**
     * Selects a card by its name and places it into the specified register slot.
     * <p>
     * This method accepts a string representation of a card name and uses the CardFactory
     * to find the corresponding card object in the player's hand. If a matching card is found,
     * it will be removed from the hand and placed in the specified register slot. If all register
     * slots are filled, the player will be marked as ready for the next phase.
     * <p>
     * If the provided card name is null or an empty string, the specified register slot will be cleared.
     * <p>
     * This method includes various error checks, including:
     * <ul>
     *     <li>Verifying that the player has not already completed register selection</li>
     *     <li>Verifying that the register slot is within valid range (0-4)</li>
     *     <li>Verifying that the specified card exists in the player's hand</li>
     * </ul>
     *
     * @param cardName     The name of the card to select, such as "MoveI", "TurnLeft", etc.
     *                     If null or empty, clears the slot
     * @param registerSlot The index of the register slot to place the card (0-4)
     */
    public void chooseCardToRegister(String cardName, int registerSlot) {
        if (!readyRegister) {
            if (registerSlot >= 0 && registerSlot < 5) {
                if (cardName == null || cardName.isEmpty()) {
                    removeCardFromRegister(registerSlot);
                    return;
                }

                // Use the factory to find the corresponding card in your hand.
                RegisterCard cardToPlay = CardFactory.findCardInHand(cardName, hand);
                if (cardToPlay != null) {
                    if (register.get(registerSlot) != null) {
                        removeCardFromRegister(registerSlot);
                    }
                    register.set(registerSlot, cardToPlay);
                    removeFromHand(cardToPlay);
                    // Notify server that card has been selected
                    connection.broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCardSelected(
                                    connection.getMyID(), registerSlot, Boolean.TRUE)));
                    // Check whether all registers are filled
                    if (register.stream().noneMatch(Objects::isNull)) {
                        setReadyRegister(true);
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
                    new MessageDefinitions.BodyError("You already selected your registry cards!")
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
                    register.set(registerSlot, null);
                    addToHand(removedCard);

                    // Notify server that register slot has been cleared
                    connection.broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCardSelected(
                                    connection.getMyID(), registerSlot, Boolean.FALSE)));

                    return true;
                } else {
                    // No cards to remove
                    return false;
                }
            } else {
                connection.sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyError("Invalid register slot number: " + registerSlot + " (must be between 0 and 4)")
                ));
                return false;
            }
        } else {
            connection.sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("You already selected your registry cards!")
            ));
            return false;
        }
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
                register.set(i, newCard);
                cardsYouGotNow.add(CardFactory.getCardName(newCard));
                connection.sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyCardSelected(connection.getMyID(), i, Boolean.TRUE)
                ));
            }
        }

        connection.sendMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyCardsYouGotNow(cardsYouGotNow))
        );
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

    public void replaceDamageCard(int registerSlot) {
        RegisterCard newCard = programmingDeck.draw();
        register.set(registerSlot, newCard);
        String cardName = CardFactory.getCardName(newCard);
        connection.broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyReplaceCard(registerSlot, cardName, robot.getId())
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
        for (RegisterCard card : hand)
            discardCardFromHand(card);
    }

    /**
     * Resets the register. Fills the register with null objs and sets the flag readyRegister to false.
     **/
    public void resetRegister() {
        // Move cards from registers to discard pile
        for (int i = 0; i < register.size(); i++) {
            RegisterCard card = register.get(i);
            if (card != null) {
                removeCardFromRegister(i);
                discardCardFromHand(card);
            }
        }

        // Fill register with null and set flag
        for (int i = 0; i < 5; i++)
            register.set(i, null);
        setReadyRegister(false);
    }

    // UTILITY

    public String toString() {
        return "Player{" +
                "id=" + robot.getId() +
                ", name='" + name + '\'' +
                ", robot=" + robot +
                ", register=" + register +
                '}';
    }

}