package de.lmu.dbs.ifi.sep25.ui.bot;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
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
    private static final String[] CARD_TYPES = {"Move1", "Move2", "Move3", "TurnLeft", "TurnRight", "UTurn", "BackUp", "PowerUp", "Again"}; // Replace with actual types
    private final Random random = new Random();
    private static final int DIZZY_HIGHWAY_CHECKPOINTS = 1;
    private static final Position DIZZY_HIGHWAY_REBOOT_POSITION = new Position(7, 3);
    private static final List<Position> START_A_POSITIONS = Arrays.asList(
            new Position(0, 3), new Position(0, 6), new Position(1, 1),
            new Position(1, 4), new Position(1, 5), new Position(1, 8)
    );
    private List<String> hand = new ArrayList<>();
    private List<List<List<Field>>> gameMap;
    private Map<Integer, Integer> energy = new HashMap<>();
    private Map<Integer, Integer> checkpointsReached = new HashMap<>();
    private String phase = "Setup";
    private int currentRegister = 0;
    private BodyMovement rebootPosition;
    private int rebootingInProgress = -1;
    private boolean firstReadyRegistry = true;
    private int figureRetryCount = 0;
    private int startPointRetries = 0;
    private static final int MAX_RETRIES = AVAILABLE_FIGURES.length;
    private static final int MAX_START_POINT_RETRIES = START_A_POSITIONS.size();
    private boolean isReady = false; // Track ready state
    private Position botPosition;
    private String botDirection;
    private long lastCurrentPlayerTime = 0;
    private static final long STUCK_TIMEOUT = 10000; // 10 seconds
    private List<String> selectedCards = new ArrayList<>(Collections.nCopies(5, null));

    @Override
    protected void handleMessage(String json, String messageType) {
        try{
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
            case "ReceivedChat" -> System.out.println("[SimpleRandomBot] Chat: [SERVER]: " + JsonUtil.parseMessage(json, BodyReceivedChat.class).messageBody().message());
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
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling message: " + e.getMessage());
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
        try {
            Message<BodyWelcome> message = JsonUtil.parseMessage(json, BodyWelcome.class);
            id = message.messageBody().clientID();
            System.out.println("[SimpleRandomBot] ID: " + id);
            ClientSingleton.setInstance(new Client() {
                @Override
                public void start(String h, int p) throws IOException {
                    SimpleRandomBot.this.start(h, p);
                }
                @Override
                public void sendMessage(Message<?> message) {
                    SimpleRandomBot.this.sendMessage(message);
                }
                @Override
                public int getID() {
                    return SimpleRandomBot.this.id;
                }
                @Override
                public void flushPendingPlayers() { /* No-op */ }
            });
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling Welcome: " + e.getMessage());
        }
    }

    private void handleBodyError(String json) {
        try {
            Message<BodyError> message = JsonUtil.parseMessage(json, BodyError.class);
            String error = message.messageBody().error();
            System.out.println("[SimpleRandomBot] Error: " + error);

            if (error.contains("Figure already taken") && figureRetryCount < MAX_RETRIES) {
                // Try another figure
                List<Integer> remainingFigures = Arrays.stream(AVAILABLE_FIGURES).boxed().collect(Collectors.toCollection(ArrayList::new));
                for (int i = 0; i < figureRetryCount && !remainingFigures.isEmpty(); i++) {
                    remainingFigures.remove(random.nextInt(remainingFigures.size()));
                }
                if (!remainingFigures.isEmpty()) {
                    String name = BOT_NAMES[random.nextInt(BOT_NAMES.length)];
                    int figure = remainingFigures.get(random.nextInt(remainingFigures.size()));
                    sendMessage(new Message<>(new BodyPlayerValues(name, figure)));
                    System.out.println("[SimpleRandomBot] Retried PlayerValues: " + name + ", Figure: " + figure);
                    figureRetryCount++;
                    sendMessage(new Message<>(new BodySetStatus(true)));
                    System.out.println("[SimpleRandomBot] Sent message: SetStatus (ready=true)");
                    isReady = true;
                } else {
                    System.err.println("[SimpleRandomBot] Error: No figures available after " + MAX_RETRIES + " retries");
                }
            } else if (error.contains("Player not initialized")) {
                String name = isBot ? BOT_NAMES[random.nextInt(BOT_NAMES.length)] : "Bot1";
                int figure = AVAILABLE_FIGURES[random.nextInt(AVAILABLE_FIGURES.length)];
                sendMessage(new Message<>(new BodyPlayerValues(name, figure)));
                System.out.println("[SimpleRandomBot] Retried PlayerValues: " + name + ", Figure: " + figure);
                sendMessage(new Message<>(new BodySetStatus(true)));
                System.out.println("[SimpleRandomBot] Sent message: SetStatus (ready=true)");
                isReady = true;
            } else if (error.toLowerCase().contains("ready")) {
                sendMessage(new Message<>(new BodySetStatus(true)));
                System.out.println("[SimpleRandomBot] Sent message: SetStatus (ready=true) on ready error");
                isReady = true;
            } else if (error.equals("Starting point already taken!") && "Aufbauphase".equals(phase) && startPointRetries < MAX_START_POINT_RETRIES) {
                // Retry with a new start point
                List<Position> availableStartPoints = new ArrayList<>(START_A_POSITIONS);
                if (availableStartPoints.isEmpty()) {
                    availableStartPoints.addAll(Arrays.asList(
                            new Position(0, 3), new Position(0, 6), new Position(1, 1),
                            new Position(1, 4), new Position(1, 8)
                    ));
                }
                if (!availableStartPoints.isEmpty()) {
                    Position position = availableStartPoints.remove(random.nextInt(availableStartPoints.size()));
                    String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                    sendMessage(new Message<>(new BodySetStartingPoint(position.x(), position.y(), direction)));
                    System.out.println("[SimpleRandomBot] Starting point retry: (" + position.x() + ", " + position.y() + "), Dir=" + direction);
                    startPointRetries++;
                } else {
                    System.err.println("[SimpleRandomBot] Error: No available start points for retry");
                    sendMessage(new Message<>(new BodySetStatus(true)));
                    System.out.println("[SimpleRandomBot] Sent SetStatus on no start points");
                }
            } else if (error.equals("Game has already started.")) {
                System.out.println("[SimpleRandomBot] Ignoring SetStatus as game started");
            } else {
                System.err.println("[SimpleRandomBot] Unhandled error: " + error);
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling Error message: " + e.getMessage());
        }
    }

    private void handleBodyCardPlayed(String json) {
        Message<BodyCardPlayed> message = JsonUtil.parseMessage(json, BodyCardPlayed.class);
        System.out.println("[SimpleRandomBot] Card played by " + message.messageBody().clientID());
    }

    // Lobby Handlers
    private void handleBodyPlayerAdded(String json) {
        try {
            Message<BodyPlayerAdded> message = JsonUtil.parseMessage(json, BodyPlayerAdded.class);
            int clientId = message.messageBody().clientID();
            System.out.println("[SimpleRandomBot] PlayerAdded: ID=" + clientId + ", Name=" + message.messageBody().name());
            if (clientId != id && !isReady) {
                sendMessage(new Message<>(new BodySetStatus(true)));
                System.out.println("[RandomBot] Sent message: SetStatus (ready=true)");
                isReady = true;
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling PlayerAdded: " + e.getMessage());
        }
    }

    private void handleBodyPlayerRenamed(String json) {
        Message<BodyPlayerRenamed> message = JsonUtil.parseMessage(json, BodyPlayerRenamed.class);
        System.out.println("[SimpleRandomBot] Player renamed: ID=" + message.messageBody().clientID());
    }

    private void handleBodyPlayerStatus(String json) {
        try {
            Message<BodyPlayerStatus> message = JsonUtil.parseMessage(json, BodyPlayerStatus.class);
            int clientId = message.messageBody().clientID();
            boolean ready = message.messageBody().ready();
            System.out.println("[SimpleRandomBot] Player status: ID=" + clientId + ", Ready=" + ready);

            if (!isReady) {
                sendMessage(new Message<>(new BodySetStatus(true)));
                System.out.println("[RandomBot] Sent message: SetStatus (ready=true)");
                isReady = true;
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling PlayerStatus: " + e.getMessage());
        }
    }

    private void handleRandomSelectMap(String json) {
        try {
            Message<BodySelectMap> message = JsonUtil.parseMessage(json, BodySelectMap.class);
            List<String> maps = message.messageBody().availableMaps();
            System.out.println("[SimpleRandomBot] Received SelectMap: " + maps);
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

    private void handleRandomGameStarted(String json) {
        try {
            // [Updated] Parse JSON directly to handle EnergySpace and other field types
            JsonObject fullMessage = gson.fromJson(json, JsonObject.class);
            JsonObject messageBody = fullMessage.getAsJsonObject("messageBody");
            JsonArray serverMap = messageBody.getAsJsonArray("gameMap");
            int energyValue = messageBody.get("energy").getAsInt();

            // Convert server map to gameMap
            gameMap = new ArrayList<>();
            for (int x = 0; x < serverMap.size(); x++) {
                JsonArray row = serverMap.get(x).getAsJsonArray();
                List<List<Field>> newRow = new ArrayList<>();
                for (int y = 0; y < row.size(); y++) {
                    JsonArray fields = row.get(y).getAsJsonArray();
                    List<Field> newCell = new ArrayList<>();
                    for (JsonElement fieldElement : fields) {
                        JsonObject fieldJson = fieldElement.getAsJsonObject();
                        String type = fieldJson.has("type") ? fieldJson.get("type").getAsString() : "Empty";
                        String isOnBoardStr = fieldJson.has("isOnBoard") ? fieldJson.get("isOnBoard").getAsString() : "true";
                        boolean isOnBoard = "true".equalsIgnoreCase(isOnBoardStr);
                        List<String> orientations = fieldJson.has("orientations") ?
                                gson.fromJson(fieldJson.get("orientations"), new TypeToken<List<String>>(){}.getType()) : null;
                        Integer count = fieldJson.has("count") ? fieldJson.get("count").getAsInt() : null;
                        Integer speed = fieldJson.has("speed") ? fieldJson.get("speed").getAsInt() : null;
                        newCell.add(new Field(type, isOnBoard, orientations, count, speed));
                    }
                    newRow.add(newCell);
                }
                gameMap.add(newRow);
            }
            energy.put(id, energyValue);
            checkpointsReached.put(id, 0);
            System.out.println("[SimpleRandomBot] GameStarted: Energy=" + energyValue);
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error parsing GameStarted: " + e.getMessage());
            // [Unchanged] Initialize a fallback 13x10 map (StartA: x=0–2, 5B: x=3–12, y=0–9)
            gameMap = new ArrayList<>();
            for (int x = 0; x < 13; x++) {
                List<List<Field>> row = new ArrayList<>();
                for (int y = 0; y < 10; y++) {
                    String fieldType = (x <= 2 && START_A_POSITIONS.contains(new Position(x, y))) ? "StartPoint" : "Empty";
                    row.add(List.of(new Field(fieldType, true, null, null, null)));
                }
                gameMap.add(row);
            }
            energy.put(id, 5); // Default energy
            botPosition = START_A_POSITIONS.get(random.nextInt(START_A_POSITIONS.size()));
            botDirection = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
            System.out.println("[SimpleRandomBot] Initialized fallback gameMap (13x10, StartA + 5B), Position=" + botPosition + ", Direction=" + botDirection);
        }
    }

    private void handleBodyStartingPointTaken(String json) {
        try{
            Message<BodyStartingPointTaken> message = JsonUtil.parseMessage(json, BodyStartingPointTaken.class);
            int clientId = message.messageBody().clientID();
            int x = message.messageBody().x();
            int y = message.messageBody().y();
            String direction = message.messageBody().direction();
            System.out.println("[SimpleRandomBot] Starting point taken by " + message.messageBody().clientID()+ " at (" + x + ", " + y + ")");
            if (clientId != id && botPosition != null && botPosition.x() == x && botPosition.y() == y) {
                System.out.println("[SimpleRandomBot] Bot’s starting point taken, retrying...");
                botPosition = null;
                startPointRetries++;
                handleRandomSetStartingPoint("{\"messageType\":\"SetStartingPoint\",\"messageBody\":{}}");
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling StartingPointTaken: " + e.getMessage());
        }
    }

    private void handleRandomSetStartingPoint(String json) {
        try {
            List<Position> startingPoints = new ArrayList<>(START_A_POSITIONS); // Use predefined StartA positions
            if (startPointRetries < MAX_START_POINT_RETRIES && !startingPoints.isEmpty()) {
                Position pos = startingPoints.get(random.nextInt(startingPoints.size()));
                String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                sendMessage(new Message<>(new BodySetStartingPoint(pos.x(), pos.y(), direction)));
                botPosition = pos;
                botDirection = direction;
                System.out.println("[SimpleRandomBot] Starting point attempt " + (startPointRetries + 1) + ": (" + pos.x() + ", " + pos.y() + "), Dir=" + direction);
                startPointRetries++;
            } else {
                System.err.println("[SimpleRandomBot] No available starting points after " + MAX_START_POINT_RETRIES + " retries");
                startPointRetries = 0; // Reset retries
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling SetStartingPoint: " + e.getMessage());
            if (startPointRetries < MAX_START_POINT_RETRIES) {
                handleRandomSetStartingPoint(json); // Retry
            } else {
                System.err.println("[SimpleRandomBot] Failed to set starting point after retries");
                startPointRetries = 0;
            }
        }
    }


    // Gameplay Handlers
    private void handleRandomYourCards(String json) {
        try {
            Message<BodyYourCards> message = JsonUtil.parseMessage(json, BodyYourCards.class);
            hand.clear();
            List<?> rawCards = message.messageBody().cardsInHand();
            if (rawCards != null && !rawCards.isEmpty()) {
                if (rawCards.get(0) instanceof List) {
                    // Flatten nested list (e.g., [["TurnRight","MoveIII",...]])
                    List<String> nestedCards = (List<String>) rawCards.get(0);
                    hand.addAll(nestedCards);
                } else {
                    // Assume flat list of strings (fallback)
                    for (Object card : rawCards) {
                        if (card instanceof String) {
                            hand.add((String) card);
                        }
                    }
                }
            }
            System.out.println("[SimpleRandomBot] Cards: " + hand);
            if (id != null && hand.size() >= 5) {
                Collections.shuffle(hand, random);
                // Ensure register 0 does not get "Again"
                List<String> validCards = new ArrayList<>(hand);
                validCards.remove("Again"); // Exclude Again for register 0
                if (validCards.isEmpty()) {
                    System.err.println("[SimpleRandomBot] Error: No valid cards for register 0 (all are Again)");
                    return;
                }
                String card0 = validCards.get(random.nextInt(validCards.size()));
                selectedCards.set(0, card0);
                sendMessage(new Message<>(new BodySelectedCard(card0, 0)));
                System.out.println("[SimpleRandomBot] Selected card " + card0 + " for register 0");
                // Select registers 1-4 from full hand
                List<String> remainingCards = new ArrayList<>(hand);
                remainingCards.remove(card0);
                Collections.shuffle(remainingCards, random);
                for (int register = 1; register < 5 && !remainingCards.isEmpty(); register++) {
                    String card = remainingCards.remove(0);
                    selectedCards.set(register, card);
                    sendMessage(new Message<>(new BodySelectedCard(card, register)));
                    System.out.println("[SimpleRandomBot] Selected card " + card + " for register " + register);
                }
                sendMessage(new Message<>(new BodySelectionFinished(id)));
                System.out.println("[SimpleRandomBot] SelectionFinished");
            } else {
                System.err.println("[SimpleRandomBot] Error: ID=" + id + ", Cards=" + hand.size());
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling YourCards: " + e.getMessage());
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
        System.out.println("[SimpleRandomBot] Card selected by " + message.messageBody().clientID() +
                ", Register=" + message.messageBody().register() + ", Filled=" + message.messageBody().filled());
    }

    private void handleRandomSelectionFinished(String json) {
        try {
            Message<BodySelectionFinished> message = JsonUtil.parseMessage(json, BodySelectionFinished.class);
            if (message.messageBody().clientID().equals(id)) {
                System.out.println("[SimpleRandomBot] Selection finished by " + id);
                // Send BodyTimerStarted to prompt server
                sendMessage(new Message<>(new BodyTimerStarted()));
                System.out.println("[SimpleRandomBot] Sent BodyTimerStarted");
                // Reset firstReadyRegistry to prevent multiple sends
                firstReadyRegistry = false;
                // Execute register 0 card immediately
                if (selectedCards.size() > 0 && selectedCards.get(0) != null) {
                    String cardName = selectedCards.get(0);
                    System.out.println("[SimpleRandomBot] Executing card in register 0: " + cardName);
                    executeCardAction(cardName); // Sends BodyPlayerTurning for TurnRight
                    currentRegister++;
                } else {
                    System.err.println("[SimpleRandomBot] No card in register 0 to execute");
                }
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling SelectionFinished: " + e.getMessage());
        }
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
        System.out.println("[SimpleRandomBot] Received CurrentCards: " + json); // Log raw message
        try {
            Message<BodyCurrentCards> message = JsonUtil.parseMessage(json, BodyCurrentCards.class);
            BodyCurrentCards body = message.messageBody();
            for (ActiveCard activeCard : body.activeCards()) {
                if (activeCard.clientID().equals(id)) {
                    String cardName = activeCard.card();
                    System.out.println("[SimpleRandomBot] Playing card from server: " + cardName);
                    executeCardAction(cardName);
                    currentRegister++;
                    return; // Exit after processing own card
                }
            }
            System.out.println("[SimpleRandomBot] CurrentCards: No card found for clientID " + id);
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error parsing CurrentCards with JsonUtil: " + e.getMessage());
        }
    }

    private void handleBodyReplaceCard(String json) {
        try {
            JsonObject fullMessage = gson.fromJson(json, JsonObject.class);
            JsonObject messageBody = fullMessage.getAsJsonObject("messageBody");
            int clientId = messageBody.get("clientID").getAsInt();
            int register = messageBody.get("register").getAsInt();
            String newCard = messageBody.get("newCard").getAsString();
            if (clientId == id) {
                selectedCards.set(register, newCard);
                System.out.println("[SimpleRandomBot] Replaced card in register " + register + " with " + newCard);
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling ReplaceCard: " + e.getMessage());
        }
    }

    private void handleRandomCurrentPlayer(String json) {
        try{
            Message<BodyCurrentPlayer> message = JsonUtil.parseMessage(json, BodyCurrentPlayer.class);
            if (message.messageBody().clientID().equals(id)) {
                System.out.println("[SimpleRandomBot] My turn, phase: " + phase);
                if ("Aufbauphase".equals(phase)) {
                    // List of valid start points from Start A board
                    List<Position> availableStartPoints = new ArrayList<>(START_A_POSITIONS);
                    if (availableStartPoints.isEmpty()) {
                        availableStartPoints.addAll(Arrays.asList(
                                new Position(0, 3), new Position(0, 6), new Position(1, 1),
                                new Position(1, 4), new Position(1, 8)
                        ));
                    }
                    if (!availableStartPoints.isEmpty()) {
                        Position position = availableStartPoints.remove(random.nextInt(availableStartPoints.size()));
                        String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                        sendMessage(new Message<>(new BodySetStartingPoint(position.x(), position.y(), direction)));
                        System.out.println("[SimpleRandomBot] Starting point attempt: (" + position.x() + ", " + position.y() + "), Dir=" + direction);
                    } else {
                        System.err.println("[SimpleRandomBot] Error: No available start points");
                        sendMessage(new Message<>(new BodySetStatus(true)));
                        System.out.println("[SimpleRandomBot] Sent SetStatus on no start points");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling CurrentPlayer: " + e.getMessage());
        }
    }

    private void handleBodyActivePhase(String json) {
        try{
        Message<BodyActivePhase> message = JsonUtil.parseMessage(json, BodyActivePhase.class);
        phase = switch (message.messageBody().phase()) {
            case 0 -> "Aufbauphase";
            case 1 -> "Upgradephase";
            case 2 -> "Programmierphase";
            case 3 -> "Aktivierungsphase";
            default -> "Unbekannt";
        };
        System.out.println("[SimpleRandomBot] Phase: " + phase);
        // Execute register 0 card in Aktivierungsphase
        if (phase.equals("Aktivierungsphase") && currentRegister == 0 && selectedCards.size() > 0 && selectedCards.get(0) != null) {
            String cardName = selectedCards.get(0);
            System.out.println("[SimpleRandomBot] Executing card in register 0: " + cardName);
            executeCardAction(cardName); // Sends BodyMovement, BodyPlayerTurning, or random move for Again
            currentRegister++;
        }
    } catch (Exception e) {
        System.err.println("[SimpleRandomBot] Error handling ActivePhase: " + e.getMessage());
    }
    }

    private void handleRandomReboot(String json) {
        try{
            Message<BodyReboot> message = JsonUtil.parseMessage(json, BodyReboot.class);
            int clientId = message.messageBody().clientID();
            rebootingInProgress = clientId;
            if (clientId == id) {
                String direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                Position newPosition = DIZZY_HIGHWAY_REBOOT_POSITION;
                sendMessage(new Message<>(new BodyRebootDirection(direction)));
                rebootPosition = new BodyMovement(id, newPosition.x(), newPosition.y());
                botPosition = newPosition;
                botDirection = direction;
                System.out.println("[SimpleRandomBot] Reboot Position=(" + newPosition.x() + ", " + newPosition.y() + "), Direction: " + direction);
            }else {
                System.out.println("[SimpleRandomBot] Opponent reboot: ID=" + clientId);
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling Reboot: " + e.getMessage());
        }
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
        try {
            Message<BodyMovement> message = JsonUtil.parseMessage(json, BodyMovement.class);
            int clientId = message.messageBody().clientID();
            int x = message.messageBody().x();
            int y = message.messageBody().y();
            System.out.println("[SimpleRandomBot] Movement: ID=" + clientId + " to (" + x + ", " + y + ")");
            if (clientId == id) {
                botPosition = new Position(x, y);
                if (currentRegister == 1) {
                    System.out.println("[SimpleRandomBot] Executed register 1 move to (" + x + ", " + y + ")");
                }
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling Movement: " + e.getMessage());
        }
    }

    private void executeCardAction(String cardName) {
        try {
            if (botPosition == null || botDirection == null) {
                botPosition = START_A_POSITIONS.get(random.nextInt(START_A_POSITIONS.size()));
                botDirection = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
                System.out.println("[SimpleRandomBot] Initialized default position=" + botPosition + ", direction=" + botDirection);
            }
            sendMessage(new Message<>(new BodyPlayCard(cardName)));
            System.out.println("[SimpleRandomBot] Played card: " + cardName);
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error executing card action: " + e.getMessage());
        }
    }

    private record Field(String type, boolean isOnBoard, List<String> orientations, Integer count, Integer speed) {}

    private void handleBodyPlayerTurning(String json) {
        Message<BodyPlayerTurning> message = JsonUtil.parseMessage(json, BodyPlayerTurning.class);
        int clientId = message.messageBody().clientID();
        String rotation = message.messageBody().rotation();
        System.out.println("[SimpleRandomBot] Turning: ID=" + clientId + ", Rotation=" + rotation);
        if (clientId == id) {
            botDirection = updateDirection(botDirection, rotation);
            if (currentRegister == 1) {
                System.out.println("[SimpleRandomBot] Executed register 1 turn to " + botDirection);
            }
        }
    }

    private String updateDirection(String current, String rotation) {
        List<String> dirs = Arrays.asList("top", "right", "bottom", "left");
        int idx = dirs.indexOf(current);
        if (rotation.equals("clockwise")) {
            idx = (idx + 1) % 4;
        } else if (rotation.equals("counterclockwise")) {
            idx = (idx - 1 + 4) % 4;
        }
        return dirs.get(idx);
    }

    private void handleBodyAnimation(String json) {
        Message<BodyAnimation> message = JsonUtil.parseMessage(json, BodyAnimation.class);
        System.out.println("[SimpleRandomBot] Animation: " + message.messageBody().type());
    }

    private void handleBodyEnergy(String json) {
        Message<BodyEnergy> message = JsonUtil.parseMessage(json, BodyEnergy.class);
        energy.put(message.messageBody().clientID(), message.messageBody().count());
        System.out.println("[SimpleRandomBot] Energy: ID=" + message.messageBody().clientID() + ", Count=" + message.messageBody().count());
    }

    private void handleBodyCheckPointReached(String json) {
        try {
        Message<BodyCheckPointReached> message = JsonUtil.parseMessage(json, BodyCheckPointReached.class);
            int clientId = message.messageBody().clientID();
            int checkpointNumber = message.messageBody().number();
            // DizzyHighway (1 checkpoint)
            if (checkpointNumber == 1) {
                checkpointsReached.put(clientId, checkpointNumber);
                System.out.println("[SimpleRandomBot] Checkpoint reached: ID=" + clientId + ", Checkpoint=" + checkpointNumber + " (DizzyHighway)");
                if (clientId == id) {
                    System.out.println("[SimpleRandomBot] Bot reached checkpoint 1, token acquired");
                } else {
                    System.out.println("[SimpleRandomBot] Opponent reached checkpoint 1");
                }
            } else {
                System.err.println("[SimpleRandomBot] Unexpected checkpoint number: " + checkpointNumber + " for DizzyHighway (expected 1)");
            }
        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling CheckPointReached: " + e.getMessage());
        }
    }

    private void handleBodyGameFinished(String json) {
        try {
        Message<BodyGameFinished> message = JsonUtil.parseMessage(json, BodyGameFinished.class);
            int winnerId = message.messageBody().clientID();
            Integer checkpoints = checkpointsReached.getOrDefault(winnerId, 0);
            if (checkpoints == DIZZY_HIGHWAY_CHECKPOINTS) {
                System.out.println("[SimpleRandomBot] Game finished: Winner ID=" + winnerId + " reached all " + DIZZY_HIGHWAY_CHECKPOINTS + " checkpoints (DizzyHighway)");
                if (winnerId == id) {
                    System.out.println("[SimpleRandomBot] Bot wins, acquired checkpoint token!");
                } else {
                    System.out.println("[SimpleRandomBot] Opponent wins, acquired checkpoint token");
                }
            } else {
                System.err.println("[SimpleRandomBot] Error: Winner ID=" + winnerId + " has " + checkpoints + " checkpoints, expected " + DIZZY_HIGHWAY_CHECKPOINTS);
            }
            // Reset game state
            checkpointsReached.clear();
            isReady = false;
            phase = "Finished";
            currentRegister = 0;
            hand.clear();

        } catch (Exception e) {
            System.err.println("[SimpleRandomBot] Error handling GameFinished: " + e.getMessage());
        }
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
            String host = args.length > 0 ? args[0] : "sep.dbs.ifi.lmu.de"; // [Updated] Default to test server
            int port = args.length > 1 ? Integer.parseInt(args[1]) : 52021; // [Updated] Default to test server port
            try {
                bot.start(host, port);
                System.out.println("[SimpleRandomBot] Bot started on " + host + port);
            } catch (IOException e) {
                System.err.println("[SimpleRandomBot] Failed to start: " + e.getMessage());
                try {
                    bot.start("localhost", 12345); // Fallback to default
                    System.out.println("[SimpleRandomBot] Bot started on localhost:12345");
                } catch (IOException e2) {
                    System.err.println("[SimpleRandomBot] Failed to start on localhost:12345: " + e2.getMessage());
                }
            }
        }else{
            System.err.println("[SimpleRandomBot] Not running in bot mode");
        }
    }
}