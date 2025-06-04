package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.network.ClientHandler;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private final String name;
    private final Robot robot;

    /*
    private final List<Card> register = new ArrayList<>(5); //TODO maybe create a subytype of card: RegisterCard <- RegularProgramming, Damage (since dmg and prog cards can be played)
    private final List<UpgradeCard> upgrades = new ArrayList<>(); //TODO need multiple upgrade lists, for permanent and temp
    private final List<Card> discardPile = new ArrayList<>(); //TODO should be same subtype like register, personal discard pile; see gameplay loop
    private final List<Card> hand = new ArrayList<>(); //TODO implement player card hand
    */
    private final List<RegisterCard> register = new ArrayList<>(5);
    private final List<UpgradeCard> permanentUpgrades = new ArrayList<>();
    private final List<UpgradeCard> temporaryUpgrades = new ArrayList<>();
    private final List<RegisterCard> discardPile = new ArrayList<>();
    private final List<RegisterCard> hand = new ArrayList<>();
    private boolean ready = false;
    private final ClientHandler connection;
    private final Deck programmingDeck = new Deck(); //TODO implement programming deck and card cycle with deck -> register -> discard <- damage ...
    private static int nextId = 1;

    public Player(String name, int startX, int startY, ClientHandler connection) {
        this.name = name;
        this.robot = new Robot(startX, startY, "NORTH", nextID++); //Use nextID
        this.connection = connection;
        for (int i = 0; i < 5; i++) {
            register.add(null);
        }
    }

    private void initializeHand() {
        programmingDeck.shuffle();
        int cardsToDraw = Math.max(9 - robot.getDamage(), 1); // Fewer cards if damaged
        for (int i = 0; i < cardsToDraw && !programmingDeck.isEmpty(); i++) {
            RegisterCard card = programmingDeck.drawCard();
            if (card != null) {
                hand.add(card);
            }
        }
    }

    public void chooseCard(RegisterCard card, int slot) {
        if (slot >= 0 && slot < 5 && hand.contains(card)) {
            register.set(slot, card);
            hand.remove(card);
            if (connection != null) {
                connection.sendMessage("Player " + name + " chose card " + card + " in slot " + slot);
            }
            if (register.stream().noneMatch(c -> c == null)) {
                setReady(true);
            }
        }
    }

    public void discardCard(RegisterCard card) {
        if (card != null) {
            discardPile.add(card);
        }
    }

    public void addUpgrade(UpgradeCard upgrade, boolean isPermanent) {
        if (upgrade != null && robot.getEnergy() >= upgrade.getCost()) {
            robot.addEnergy(-upgrade.getCost());
            if (isPermanent) {
                permanentUpgrades.add(upgrade);
                upgrade.applyPermanentEffect(robot);
            } else {
                temporaryUpgrades.add(upgrade);
                upgrade.applyTemporaryEffect(robot);
            }
        }
    }

    public void drawCard() {
        if (programmingDeck.isEmpty()) {
            recycleDiscardPile();
        }
        RegisterCard card = programmingDeck.drawCard();
        if (card != null) {
            hand.add(card);
        }
    }

    private void recycleDiscardPile() {
        programmingDeck.addCards(discardPile);
        discardPile.clear();
        programmingDeck.shuffle();
    }

    public void endRound() {
        for (RegisterCard card : register) {
            if (card != null) {
                discardCard(card);
            }
        }
        register.clear();
        for (int i = 0; i < 5; i++) {
            register.add(null);
        }
        temporaryUpgrades.clear();
        robot.resetProgramming();
        initializeHand();
        setReady(false);
    }

    public void applyDamageCard(RegisterCard card) {
        if (card instanceof DamageCard) {
            robot.takeDamage(1);
            discardCard(card);
        }
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
        return new ArrayList<>(discardPile);
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

}