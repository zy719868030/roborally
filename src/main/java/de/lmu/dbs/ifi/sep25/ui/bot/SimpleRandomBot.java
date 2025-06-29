package de.lmu.dbs.ifi.sep25.ui.bot;

import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.Client;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;
import java.util.ArrayList;
import java.util.*;
import java.io.IOException;
import java.util.stream.Collectors;

public class SimpleRandomBot extends RandomBot {
    private static final boolean isBot = true;
    private static final String[] BOT_NAMES = {"Bot1", "Bot2", "Bot3", "Bot4", "Bot5", "Bot6"};
    private static final int[] AVAILABLE_FIGURES = {0, 1, 2, 3, 4, 5};
    private static final String[] DIRECTIONS = {"top", "right", "bottom", "left"};
    private static final String[] CARD_TYPES = {"Move1", "Move2", "Move3", "TurnLeft", "TurnRight", "UTurn", "BackUp"}; // Replace with actual types
    private final Random random = new Random();
    private List<String> hand = new ArrayList<>();
    private List<List<List<Field>>> gameMap;
    private Map<Integer, Integer> energy = new HashMap<>();
    private Map<Integer, Integer> checkpointsReached = new HashMap<>();
    private String phase = "Setup";
    private int currentRegister = 0;
    private BodyMovement rebootPosition;
    private int rebootingInProgress = -1;
    private boolean firstReadyRegistry = true;

    @Override
    protected void handleMessage(String json, String messageType) {
        switch (messageType) {
            case "HelloClient" -> handleBodyHelloClient(json);
            case "Alive" -> handleBodyAlive(json);
            case "Welcome" -> handleBodyWelcome(json);
            case "PlayerAdded" -> handleBodyPlayerAdded(json);
            case "PlayerRenamed" -> handleBodyPlayerRenamed(json);
            case "PlayerStatus" -> handleBodyPlayerStatus(json);
            case "SelectMap" -> handleRandomSelectMap(json);
            case "MapSelected" -> handleBodyMapSelected(json);
            case "GameStarted" -> handleRandomGameStarted(json);
            case "ReceivedChat" -> handleBodyReceivedChat(json);
            case "Error" -> handleBodyError(json);
            case "CardPlayed" -> handleBodyCardPlayed(json);
            case "CurrentPlayer" -> handleRandomCurrentPlayer(json);
            case "ActivePhase" -> handleBodyActivePhase(json);
            case "StartingPointTaken" -> handleBodyStartingPointTaken(json);
            case "SetStartingPoint" -> handleRandomSetStartingPoint(json);
            case "YourCards" -> handleRandomYourCards(json);
            case "NotYourCards" -> handleBodyNotYourCards(json);
            case "ShuffleCoding" -> handleBodyShuffleCoding(json);
            case "CardSelected" -> handleBodyCardSelected(json);
            case "SelectionFinished" -> handleRandomSelectionFinished(json);
            case "TimerEnded" -> handleBodyTimerEnded(json);
            case "CardsYouGotNow" -> handleBodyCardsYouGotNow(json);
            case "CurrentCards" -> handleBodyCurrentCards(json);
            case "ReplaceCard" -> handleBodyReplaceCard(json);
            case "Movement" -> handleBodyMovement(json);
            case "PlayerTurning" -> handleBodyPlayerTurning(json);
            case "Animation" -> handleBodyAnimation(json);
            case "Reboot" -> handleRandomReboot(json);
            case "RebootDirection" -> handleBodyRebootDirection(json);
            case "Energy" -> handleBodyEnergy(json);
            case "CheckPointReached" -> handleBodyCheckPointReached(json);
            case "GameFinished" -> handleBodyGameFinished(json);
            case "PickDamage" -> handleRandomPickDamage(json);
            default -> System.out.println("[SimpleRandomBot] Ignored: " + messageType);
        }
    }

    // Connection Handlers
    private void handleBodyHelloClient(String json) {
        Message<BodyHelloClient> message = JsonUtil.parseMessage(json, BodyHelloClient.class);
        System.out.println("[SimpleRandomBot] Protocol: " + message.messageBody().protocol());
    }

    private void handleBodyAlive(String json) {
        sendMessage(json);
        System.out.println("[SimpleRandomBot] Sent Alive");
    }

    private void handleBodyWelcome(String json) {
        Message<BodyWelcome> message = JsonUtil.parseMessage(json, BodyWelcome.class);
        id = message.messageBody().clientID();
        System.out.println("[SimpleRandomBot] ID: " + id);
        String name = BOT_NAMES[random.nextInt(BOT_NAMES.length)];
        int figure = AVAILABLE_FIGURES[random.nextInt(AVAILABLE_FIGURES.length)];
        sendMessage(new Message<>(new BodyPlayerValues(name, figure)));
        System.out.println("[SimpleRandomBot] Sent PlayerValues: " + name + ", Figure: " + figure);
        // Send ready status after initialization
        sendMessage(new Message<>(new BodySetStatus(true)));
        System.out.println("[SimpleRandomBot] Sent ready status");
        // Set up ClientSingleton here
        ClientSingleton.setInstance(new Client() {
            @Override
            public void start(String h, int p) throws IOException { SimpleRandomBot.this.start(h, p); }
            @Override
            public void sendMessage(Message<?> message) { SimpleRandomBot.this.sendMessage(message); }
            @Override
            public int getID() { return SimpleRandomBot.this.id; }
            @Override
            public void flushPendingPlayers() { /* No-op */ }
        });
    }

    private int figureRetryCount = 0;
    private static final int MAX_RETRIES = AVAILABLE_FIGURES.length;

    private void handleBodyError(String json) {
        Message<BodyError> message = JsonUtil.parseMessage(json, BodyError.class);
        String error = message.messageBody().error();
        System.out.println("[SimpleRandomBot] Error: " + error);
        if (error.contains("Figure already selected") && figureRetryCount < MAX_RETRIES) {
            // Try another figure
            List<Integer> remainingFigures = Arrays.stream(AVAILABLE_FIGURES).boxed().collect(Collectors.toCollection(ArrayList::new));
            for (int i = 0; i < figureRetryCount; i++) {
                remainingFigures.remove(random.nextInt(remainingFigures.size()));
            }
            if (!remainingFigures.isEmpty()) {
            String name = BOT_NAMES[random.nextInt(BOT_NAMES.length)];
            int figure = remainingFigures.get(random.nextInt(remainingFigures.size()));
            sendMessage(new Message<>(new BodyPlayerValues(name, figure)));
            System.out.println("[SimpleRandomBot] Retried PlayerValues: " + name + ", Figure: " + figure);
            figureRetryCount++;
            } else {
            System.err.println("[SimpleRandomBot] Error: No figures available");
            closeAll();
        }
    } else {
        System.err.println("[SimpleRandomBot] Error: " + (figureRetryCount >= MAX_RETRIES ? "Max retries reached" : error));
        closeAll();
    }
}

    private void handleBodyCardPlayed(String json) {
        Message<BodyCardPlayed> message = JsonUtil.parseMessage(json, BodyCardPlayed.class);
        System.out.println("[SimpleRandomBot] Card played by " + message.messageBody().clientID());
    }

    // Lobby Handlers
    private void handleBodyPlayerAdded(String json) {
        Message<BodyPlayerAdded> message = JsonUtil.parseMessage(json, BodyPlayerAdded.class);
        System.out.println("[SimpleRandomBot] Player added: ID=" + message.messageBody().clientID());
    }

    private void handleBodyPlayerRenamed(String json) {
        Message<BodyPlayerRenamed> message = JsonUtil.parseMessage(json, BodyPlayerRenamed.class);
        System.out.println("[SimpleRandomBot] Player renamed: ID=" + message.messageBody().clientID());
    }

    private void handleBodyPlayerStatus(String json) {
        try {
            Message<BodyPlayerStatus> message = JsonUtil.parseMessage(json, BodyPlayerStatus.class);
            System.out.println("[SimpleRandomBot] Player status: ID=" + message.messageBody().clientID() +
                    ", Ready=" + message.messageBody().ready());
            //sendMessage(new Message<>(new BodySetStatus(true)));
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling PlayerStatus: " + e.getMessage());
        }
        //System.out.println("[SimpleRandomBot] Set ready");
    }

    private void handleRandomSelectMap(String json) {
        try {
            Message<BodySelectMap> message = JsonUtil.parseMessage(json, BodySelectMap.class);
            List<String> maps = message.messageBody().availableMaps();
            if (maps != null && !maps.isEmpty()) {
                String map = maps.get(random.nextInt(maps.size()));
                sendMessage(new Message<>(new BodyMapSelected(map)));
                System.out.println("[SimpleRandomBot] Selected map: " + map);
            } else {
                System.err.println("[SimpleRandomBot] Error: No maps available");
                sendMessage(new Message<>(new BodyError("No maps available")));
            }
        }catch(Exception e) {
            System.err.println("[SimpleRandomBot] Error handling SelectMap: " + e.getMessage());
        }
    }

    private void handleBodyMapSelected(String json) {
        Message<BodyMapSelected> message = JsonUtil.parseMessage(json, BodyMapSelected.class);
        System.out.println("[SimpleRandomBot] Map selected: " + message.messageBody().map());
    }

    private void handleBodyReceivedChat(String json) {
        Message<BodyReceivedChat> message = JsonUtil.parseMessage(json, BodyReceivedChat.class);
        String sender = message.messageBody().from() == 0 ? "[SERVER]" : "Player" + message.messageBody().from();
        System.out.println("[SimpleRandomBot] Chat: " + sender + ": " + message.messageBody().message());
    }

    // Game Setup Handlers
    private void handleRandomGameStarted(String json) {
        Message<BodyGameStarted> message = JsonUtil.parseMessage(json, BodyGameStarted.class);
        gameMap = message.messageBody().gameMap();
        energy.put(id, message.messageBody().energy());
        checkpointsReached.put(id, 0);
        System.out.println("[SimpleRandomBot] GameStarted: Energy=" + message.messageBody().energy());
    }

    private void handleBodyStartingPointTaken(String json) {
        Message<BodyStartingPointTaken> message = JsonUtil.parseMessage(json, BodyStartingPointTaken.class);
        System.out.println("[SimpleRandomBot] Starting point taken by " + message.messageBody().clientID());
    }

    private void handleRandomSetStartingPoint(String json) {
        List<Position> startingPoints = new ArrayList<>();
        if (gameMap != null && !gameMap.isEmpty()) {
            for (int x = 0; x < gameMap.size(); x++) {
                for (int y = 0; y < gameMap.get(x).size(); y++) {
                    for (Field field : gameMap.get(x).get(y)) {
                        if (field instanceof FieldStartPoint) {
                            startingPoints.add(new Position(x, y));
                        }
                    }
                }
            }
        }
        if (!startingPoints.isEmpty()) {
            Position pos = startingPoints.get(random.nextInt(startingPoints.size()));
            String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            sendMessage(new Message<>(new BodySetStartingPoint(pos.x(), pos.y(), direction)));
            System.out.println("[SimpleRandomBot] Starting point: (" + pos.x() + ", " + pos.y() + "), Dir=" + direction);
        } else {
            int x = random.nextInt(10);
            int y = random.nextInt(13);
            String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            sendMessage(new Message<>(new BodySetStartingPoint(x, y, direction)));
            System.out.println("[SimpleRandomBot] Fallback starting point: (" + x + ", " + y + ")");
        }
    }

    // Gameplay Handlers
    private void handleRandomYourCards(String json) {
        Message<BodyYourCards> message = JsonUtil.parseMessage(json, BodyYourCards.class);
        hand.clear();
        hand.addAll(message.messageBody().cardsInHand());
        System.out.println("[SimpleRandomBot] Cards: " + hand);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < hand.size(); i++) {
            indices.add(i);
        }
        if(id !=null) {
            for (int register = 0; register < 5 && !indices.isEmpty(); register++) {
                int idx = indices.remove(random.nextInt(indices.size()));
                String card = hand.get(idx);
                sendMessage(new Message<>(new BodyCardSelected(id, register, true)));
                System.out.println("[SimpleRandomBot] Selected card " + card + " for register " + register);
            }
            sendMessage(new Message<>(new BodySelectionFinished(id)));
            System.out.println("[SimpleRandomBot] SelectionFinished");
        }else{
            System.out.println("[SimpleRandomBot] Error: ID is null, cannot select cards");
        }
    }

    private void handleBodyNotYourCards(String json) {
        Message<BodyNotYourCards> message = JsonUtil.parseMessage(json, BodyNotYourCards.class);
        System.out.println("[SimpleRandomBot] Not my cards for " + message.messageBody().clientID());
    }

    private void handleBodyShuffleCoding(String json) {
        Message<BodyShuffleCoding> message = JsonUtil.parseMessage(json, BodyShuffleCoding.class);
        System.out.println("[SimpleRandomBot] ShuffleCoding for " + message.messageBody().clientID());
    }

    private void handleBodyCardSelected(String json) {
        Message<BodyCardSelected> message = JsonUtil.parseMessage(json, BodyCardSelected.class);
        System.out.println("[SimpleRandomBot] Card selected by " + message.messageBody().clientID());
    }

    private void handleRandomSelectionFinished(String json) {
        Message<BodySelectionFinished> message = JsonUtil.parseMessage(json, BodySelectionFinished.class);
        if (message.messageBody().clientID().equals(id) && firstReadyRegistry) {
            sendMessage(new Message<>(new BodyTimerStarted()));
            firstReadyRegistry = false;
        }
        System.out.println("[SimpleRandomBot] Selection finished by " + message.messageBody().clientID());
    }

    private void handleBodyTimerEnded(String json) {
        Message<BodyTimerEnded> message = JsonUtil.parseMessage(json, BodyTimerEnded.class);
        System.out.println("[SimpleRandomBot] Timer ended");
    }

    private void handleBodyCardsYouGotNow(String json) {
        Message<BodyCardsYouGotNow> message = JsonUtil.parseMessage(json, BodyCardsYouGotNow.class);
        System.out.println("[SimpleRandomBot] Got cards: " + message.messageBody().cards());
    }

    private void handleBodyCurrentCards(String json) {
        Message<BodyCurrentCards> message = JsonUtil.parseMessage(json, BodyCurrentCards.class);
        currentRegister++;
        System.out.println("[SimpleRandomBot] Current cards, register: " + currentRegister);
    }

    private void handleBodyReplaceCard(String json) {
        Message<BodyReplaceCard> message = JsonUtil.parseMessage(json, BodyReplaceCard.class);
        System.out.println("[SimpleRandomBot] Replaced card in register " + message.messageBody().register());
    }

    private void handleRandomCurrentPlayer(String json) {
        Message<BodyCurrentPlayer> message = JsonUtil.parseMessage(json, BodyCurrentPlayer.class);
        if (message.messageBody().clientID().equals(id)) {
            System.out.println("[SimpleRandomBot] My turn, phase: " + phase);
        }
    }

    private void handleBodyActivePhase(String json) {
        Message<BodyActivePhase> message = JsonUtil.parseMessage(json, BodyActivePhase.class);
        phase = switch (message.messageBody().phase()) {
            case 0 -> "Aufbauphase";
            case 1 -> "Upgradephase";
            case 2 -> "Programmierphase";
            case 3 -> "Aktivierungsphase";
            default -> "Unbekannt";
        };
        System.out.println("[SimpleRandomBot] Phase: " + phase);
    }

    private void handleRandomReboot(String json) {
        Message<BodyReboot> message = JsonUtil.parseMessage(json, BodyReboot.class);
        if (message.messageBody().clientID().equals(id)) {
            String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            sendMessage(new Message<>(new BodyRebootDirection(direction)));
            System.out.println("[SimpleRandomBot] Reboot direction: " + direction);
        }
        rebootingInProgress = message.messageBody().clientID();
    }

    private void handleBodyRebootDirection(String json) {
        Message<BodyRebootDirection> message = JsonUtil.parseMessage(json, BodyRebootDirection.class);
        System.out.println("[SimpleRandomBot] Reboot direction: ID=" + rebootingInProgress + ", Dir=" + message.messageBody().direction());
        if (rebootPosition != null) {
            sendMessageSelf(new Message<>(rebootPosition));
            rebootPosition = null;
        }
        sendMessageSelf(new Message<>(new BodyAnimation("Reboot")));
    }

    private void handleBodyMovement(String json) {
        Message<BodyMovement> message = JsonUtil.parseMessage(json, BodyMovement.class);
        if (rebootingInProgress == message.messageBody().clientID()) {
            rebootPosition = message.messageBody();
            rebootingInProgress = -1;
        }
        System.out.println("[SimpleRandomBot] Movement: ID=" + message.messageBody().clientID());
    }

    private void handleBodyPlayerTurning(String json) {
        Message<BodyPlayerTurning> message = JsonUtil.parseMessage(json, BodyPlayerTurning.class);
        System.out.println("[SimpleRandomBot] Turning: ID=" + message.messageBody().clientID());
    }

    private void handleBodyAnimation(String json) {
        Message<BodyAnimation> message = JsonUtil.parseMessage(json, BodyAnimation.class);
        System.out.println("[SimpleRandomBot] Animation: " + message.messageBody().type());
    }

    private void handleBodyEnergy(String json) {
        Message<BodyEnergy> message = JsonUtil.parseMessage(json, BodyEnergy.class);
        energy.put(message.messageBody().clientID(), message.messageBody().count());
        System.out.println("[SimpleRandomBot] Energy: ID=" + message.messageBody().clientID());
    }

    private void handleBodyCheckPointReached(String json) {
        Message<BodyCheckPointReached> message = JsonUtil.parseMessage(json, BodyCheckPointReached.class);
        checkpointsReached.put(message.messageBody().clientID(), message.messageBody().number());
        System.out.println("[SimpleRandomBot] Checkpoint: ID=" + message.messageBody().clientID());
    }

    private void handleBodyGameFinished(String json) {
        Message<BodyGameFinished> message = JsonUtil.parseMessage(json, BodyGameFinished.class);
        System.out.println("[SimpleRandomBot] Game finished: Winner=" + message.messageBody().clientID());
    }

    private void handleRandomPickDamage(String json) {
        Message<BodyPickDamage> message = JsonUtil.parseMessage(json, BodyPickDamage.class);
        List<String> piles = message.messageBody().availablePiles();
        int count = message.messageBody().count();
        List<String> selected = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < piles.size(); i++) {
            indices.add(i);
        }
        for (int i = 0; i < count && !indices.isEmpty(); i++) {
            int idx = indices.remove(random.nextInt(indices.size()));
            selected.add(piles.get(idx));
        }
        sendMessage(new Message<>(new BodySelectedDamage(selected)));
        System.out.println("[SimpleRandomBot] Picked damage: " + selected);
    }

    private record Position(int x, int y) {}

    public static void main(String[] args) {
        if(isBot) {
            SimpleRandomBot bot = new SimpleRandomBot();
            try {
                bot.start("localhost", 12345);
                System.out.println("[SimpleRandomBot] Bot started");
            } catch (IOException e) {
                System.err.println("[SimpleRandomBot] Failed to start: " + e.getMessage());
            }
        }else{
            System.err.println("[SimpleRandomBot] Not running in bot mode");
        }
    }
}