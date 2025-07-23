package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.game.Position;
import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.io.IOException;
import java.util.List;

public class BotClient extends Client {
    private final BotStrategy strategy;

    public BotClient(BotStrategy strategy) {
        super(); // call Client constructor
        this.strategy = strategy;
    }

    @Override
    protected void onYourCards(List<String> hand) {
        List<String> selected = strategy.chooseRegisterCards(hand, getCurrentGameMap());
        for (int i = 0; i < selected.size(); i++) {
            String cardName = selected.get(i);
            // Send the selected card for each register slot
            sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySelectedCard(cardName, i)));
        }
        // Optionally send "SelectionFinished" if protocol requires
    }

    @Override
    protected void onSelectStartingPoint(List<Position> available) {
        Position choice = strategy.chooseStartingPoint(available);
        sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodySetStartingPoint(choice.x(), choice.y())));
    }

    @Override
    protected void onRebootDirectionRequest() {
        String dir = strategy.chooseRebootDirection(getCurrentGameMap());
        sendMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyRebootDirection(dir)));
    }

    // ...add other overrides for upgrades, damage selection, etc as needed


    //FIXME for testing purposes main class
    public static void main(String[] args) throws IOException {
        BotClient randomBot = new BotClient(new RandomBotStrategy());
        randomBot.start("localhost", 12345);

//        // For pathfinding:
//        BotClient smartBot = new BotClient(new PathfindingBotStrategy());
//        smartBot.start("localhost", 1337);
    }
}

