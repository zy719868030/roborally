package de.lmu.dbs.ifi.sep25.game;

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

    private void drawHand() {
        programmingDeck.shuffle();
        int cardsToDraw = Math.max(9 - robot.getDamage(), 1); // Fewer cards if damaged
        for (int i = 0; i < cardsToDraw; i++) {
            drawCard();
        }
    }

    public void chooseCard(RegisterCard card, int registerSlot) {
        if (!readyRegister) {
            if (registerSlot >= 0 && registerSlot < 5 ){
                if (hand.contains(card)) {
                    register.set(registerSlot, card);
                    hand.remove(card);
                    if (register.stream().noneMatch(Objects::isNull)) {
                        setReadyRegister(true);
                    }
                } else {
                    connection.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Card " + card + " is not in your hand!")));
                }
            } else {
                connection.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("Invalid registerSlot number: " + registerSlot + " (must be between 0 and 4)")));
            }
        } else {
            connection.sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyError("You already selected your registry cards!")));
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
        this.ready = ready;
        if (connection != null) {
            connection.sendMessage("Player " + name + " ready: " + ready);
        }
    }

    public boolean isReady() {
        return ready;
    }

    public boolean isReadyRegister() {
        return readyRegister;
    }

    public void setReadyRegister(boolean ready) {
        readyRegister = ready;
        //TODO call timer if first
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