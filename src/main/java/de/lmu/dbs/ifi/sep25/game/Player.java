package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.network.ClientHandler;

import java.util.ArrayList;
import java.util.List;

public class Player {
    private final String name;
    private final Robot robot;
    private final List<Card> register = new ArrayList<>(5); //TODO maybe create a subytype of card: RegisterCard <- RegularProgramming, Damage (since dmg and prog cards can be played)
    private final List<UpgradeCard> upgrades = new ArrayList<>(); //TODO need multiple upgrade lists, for permanent and temp
    private final List<Card> discardPile = new ArrayList<>(); //TODO should be same subtype like register, personal discard pile; see gameplay loop
    private final List<Card> hand = new ArrayList<>(); //TODO implement player card hand
    private boolean ready = false;
    private final ClientHandler connection;
    private final Deck programmingDeck = new Deck(); //TODO implement programming deck and card cycle with deck -> register -> discard <- damage ...


    public Player(String name, int robotID, ClientHandler connection) {
        this.name = name;
        this.robot = new Robot(robotID);
        this.connection = connection;
        for (int i = 0; i < 5; i++) {
            register.add(null);
        }
    }

    public void chooseCard(Card card, int slot) {
        if (slot >= 0 && slot < 5) {
            register.set(slot, card);
        }
    }

    public Robot getRobot() {
        return robot;
    }

    public List<Card> getRegister() {
        return register;
    }

    public String getName() {
        return name;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    public boolean isReady() {
        return ready;
    }

}