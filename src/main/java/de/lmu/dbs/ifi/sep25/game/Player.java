package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.network.ClientHandler;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    private final Deck<RegisterCard> programmingDeck = new Deck<>();

    public Player(String name, int robotID, ClientHandler connection) {
        this.name = name;
        this.robot = new Robot(robotID);
        this.connection = connection;
        for (int i = 0; i < 5; i++) {
            register.add(null);
        }
    }

    // Handle map selection
    public void selectMap(String mapName) {
        if (Game.getInstance().isMapSelectionPending()) {
            Game.getInstance().selectMap(this, mapName);
        } else {
            connection.sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Map selection phase has ended")
            ));
        }
    }

    // Getter for connection
    public ClientHandler getConnection() {
        return connection;
    }

    public void drawHand() {
        programmingDeck.shuffle();
        int cardsToDraw = Math.max(9 - robot.getDamage(), 1); // Fewer cards if damaged
        for (int i = 0; i < cardsToDraw; i++) {
            drawCard();
        }
    }

    // Added: Method to clear the register
    public void clearRegister() {
        for (int i = 0; i < register.size(); i++) {
            register.set(i, null);
        }
        readyRegister = false;
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
     * @param cardName The name of the card to select, such as "MoveI", "TurnLeft", etc.
     *                 If null or empty, clears the slot
     * @param registerSlot The index of the register slot to place the card (0-4)
     */
    public void chooseCard(String cardName, int registerSlot) {
        if (!readyRegister) {
            if (registerSlot >= 0 && registerSlot < 5) {
                if (cardName == null || cardName.isEmpty()) {
                    removeCard(null, registerSlot);
                    return;
                }

                // Use the factory to find the corresponding card in your hand.
                RegisterCard cardToPlay = CardFactory.findCardInHand(cardName, hand);

                if (cardToPlay != null) {
                    register.set(registerSlot, cardToPlay);
                    hand.remove(cardToPlay);

                    // Notify server that card has been selected
                    connection.broadcastMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyCardSelected(connection.getMyID() , registerSlot, Boolean.TRUE)));

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
     * @param cardName The name of the card to remove (not used in the actual removal logic, but included for API consistency)
     * @param registerSlot The index of the register slot to clear (0-4)
     * @return true if a card was successfully removed, false otherwise
     */
    public boolean removeCard(String cardName, int registerSlot) {
        if (!readyRegister) {
            if (registerSlot >= 0 && registerSlot < 5) {
                RegisterCard removedCard = register.get(registerSlot);
                if (removedCard != null) {
                    // Remove the card from the register and return it to your hand.
                    register.set(registerSlot, null);
                    hand.add(removedCard);

                    // Notify server that register slot has been cleared
                    connection.broadcastMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyCardSelected(connection.getMyID() , registerSlot, Boolean.TRUE)));

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

    public void discardCard(RegisterCard card) {
        if (card != null) {
            if (hand.contains(card)) {
                programmingDeck.discard(card);
                hand.remove(card);
            }
            else {
                connection.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Card " + card + " is not in your hand!")));
            }
        }
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

    public void drawCard() {
        if (programmingDeck.isEmpty()) {
            programmingDeck.reset();
        }
        hand.add(programmingDeck.draw());
    }

    //TODO fix as rounds are implemented in game class game main loop
    //public void endRound(){}

    public void replaceDamageCard(int registerSlot) {
        register.set(registerSlot, programmingDeck.draw());
    }

    public Robot getRobot() {
        return robot;
    }

    public List<RegisterCard> getRegister() {
        return new ArrayList<>(register);
    }

    public List<RegisterCard> getHand() {
        return new ArrayList<>(hand);
    }

    public List<RegisterCard> getDiscardPile() {
        return programmingDeck.getDiscardPile();
    }

    public String getName() {
        return name;
    }

    public void setReady(boolean ready) {
       // this.ready = ready;
       // if (connection != null) {
          //  connection.sendMessage("Player " + name + " ready: " + ready);
      //  }
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isReadyRegister() {
        return readyRegister;
    }

    public void setReadyRegister(boolean ready) {
        readyRegister = ready;
        connection.setReadyRegister();
    }

    public int getEnergy() {
        return energy;
    }

    public void addEnergy(int amount) {
        this.energy += amount;
    }

    public boolean consumeEnergy(int cost) {
        if (energy >= cost) {
            energy -= cost;
            return true;
        }
        return false;
    }

}