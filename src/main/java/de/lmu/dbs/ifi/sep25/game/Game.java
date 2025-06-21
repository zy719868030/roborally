package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.game.BoardElement.CheckPoints;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Antenna;

import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private static Game instance;

    private Player currentPlayer;
    private final List<Player> players;
    private Board board;
    private final DamageCardPool damageDeck = DamageCardPool.getInstance();
    private final Deck<UpgradeCard> upgradeCards = new Deck<>();
    private final Board.MapType mapType;
    // Added: Map selection fields
    private Player firstReadyPlayer;
    private String selectedMap;
    private boolean mapSelectionPending;
    private int currentPhase; // 0: Setup, 1: Upgrade, 2: Programming, 3: Activation
    private int currentPlayerIndex; // Index in players list
    private Timer programmingTimer; // For 30-second timer
    private List<Integer> slowPlayers; // Track slow players

    private Game(String mapName) {
        players = new ArrayList<>();
        mapType = parseMapName(mapName);
//      board = new Board(12, 12); FIXME @prajal
        currentPlayer = null;
        currentPlayerIndex = 0;
        currentPhase = -1; // Pre-game (map selection)
        // Added: Initialize map selection state
        firstReadyPlayer = null;
        selectedMap = null;
        mapSelectionPending = true;
        slowPlayers = new ArrayList<>();
        initializeUpgradeCards();
    }

    public static Game getInstance() {
        if (instance != null) {
            return instance;
        }
        throw new IllegalStateException("Game instance has not been initialized yet.");
    }


    public static Game getInstance(String mapName) {

        if (instance == null) {
            instance = new Game(mapName);
        }
        return instance;
    }


    //TODO @yu or @prajal
    private void initializeUpgradeCards() {
        //add upgrade cards

    }

    private Board.MapType parseMapName(String mapName) {
        switch (mapName.toLowerCase()) {
            case "risky crossing":
                return Board.MapType.MAP1;
  /*          case "extra crispy":
                return Board.MapType.EXTRA_CRISPY;
            case "lost bearings":
                return Board.MapType.LOST_BEARINGS;
            case "death trap":
                return Board.MapType.DEATH_TRAP; */
            case "dizzy highway":
            default:
                return Board.MapType.DEFAULT;
        }
    }

    // Handle player readiness and map selection
    public void setPlayerReady(Player player, boolean ready) {
        player.setReady(ready);
        // Broadcast PlayerStatus
        for (Player p : players) {
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyPlayerStatus(player.getRobot().getId(), ready)
            ));
        }

        // Send SelectMap to first ready player
        if (ready && firstReadyPlayer == null && mapSelectionPending) {
            firstReadyPlayer = player;
            List<String> availableMaps = new ArrayList<>();
            availableMaps.add("Dizzy Highway");
            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodySelectMap(availableMaps)
            ));
        } else if (!ready && firstReadyPlayer == player) {
            // Handle unready player
            firstReadyPlayer = null;
            for (Player p : players) {
                if (p.isReady()) {
                    firstReadyPlayer = p;
                    List<String> availableMaps = new ArrayList<>();
                    availableMaps.add("Dizzy Highway");
                    p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodySelectMap(availableMaps)
                    ));
                    break;
                }
            }
        }
    }

    // Process map selection
    public void selectMap(Player player, String mapName) {
        // Validate player and map
        if (player != null && (player != firstReadyPlayer || !mapSelectionPending)) {
            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Not authorized to select map")
            ));
            return;
        }

        if (!"Dizzy Highway".equals(mapName)) {
            if (player != null) {
                player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyError("Invalid map: " + mapName)
                ));
            }
            return;
        }

        // Set map and initialize board
        selectedMap = mapName;
        mapSelectionPending = false;
        board = new Board(parseMapName(mapName));
        initializeGame();

        // Broadcast MapSelected and GameStarted
        for (Player p : players) {
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyMapSelected(mapName)
            ));
            //Use convertToBoardElementMap for BodyGameStarted
            List<List<List<BoardElement>>> gameMap = board.toSerializableMap();
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyGameStarted(5, gameMap)
            ));
        }
        startGame();
    }

    // Added: Stub for AI-only map selection
    public void selectMapForAI() {
        if (mapSelectionPending) {
            selectMap(null, "Dizzy Highway");
        }
    }

    private void startGame() {
        if (!mapSelectionPending && players.size() >= 2) {
            setActivePhase(0); // Setup Phase
        }
    }

    public void setCurrentPlayer() {
        if (players.isEmpty()) return;
        // Use antenna-based priority for Setup Phase (Phase 0)
        if (currentPhase == 0) {
            Antenna antenna = findAntenna();
            if (antenna == null) {
                throw new IllegalStateException("Antenna must be present on the board.");
            } else {
                // Sort players by distance to antenna
                Map<Integer, List<Robot>> distanceGroups = new HashMap<>();
                for (Player p : players) {
                    Robot robot = p.getRobot();
                    Position robotPos = robot.getPosition();
                    if (robotPos != null && !board.hasRobotFallen(robot)) {
                        int distance = antenna.distanceToRobot(robotPos);
                        distanceGroups.computeIfAbsent(distance, k -> new ArrayList<>()).add(robot);
                    }
                }
                List<Player> sortedPlayers = new ArrayList<>();
                List<Integer> sortedDistances = new ArrayList<>(distanceGroups.keySet());
                Collections.sort(sortedDistances);
                for (int distance : sortedDistances) {
                    List<Robot> robots = antenna.sortTiedRobotsByPriority(distanceGroups.get(distance));
                    for (Robot robot : robots) {
                        for (Player p : players) {
                            if (p.getRobot().equals(robot)) {
                                sortedPlayers.add(p);
                                break;
                            }
                        }
                    }
                }
                // Include fallen robots (lowest priority)
                for (Player p : players) {
                    if (!sortedPlayers.contains(p)) {
                        sortedPlayers.add(p);
                    }
                }
                // Set current player to the next in sorted order
                currentPlayerIndex = (currentPlayerIndex + 1) % sortedPlayers.size();
                currentPlayer = sortedPlayers.get(currentPlayerIndex);
            }
        } else {
            // For other phases, use round-robin (to be updated if needed)
            currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
            currentPlayer = players.get(currentPlayerIndex);
        }
        // Send CurrentPlayer message
        for (Player p : players) {
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyCurrentPlayer(currentPlayer.getRobot().getId())
            ));
        }
    }

    public void setActivePhase(int phase) {
        currentPhase = phase;
        for (Player p : players) {
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyActivePhase(phase)
            ));
        }
        if (phase == 0) {
            setCurrentPlayer();
        } else if (phase == 2) {
            startProgrammingPhase();
        } else if (phase == 3) {
            playActivationPhase();
        }
    }

    private void startProgrammingPhase() {
        slowPlayers.clear();
        for (Player p : players) {
            p.drawHand();
            // Check: getName method in RegisterCard (assumed: String getName())
            List<String> cardNames = p.getHand().stream()
                    .map(card -> ((Card) card).getDescription())
                    .collect(Collectors.toList());

            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyYourCards(cardNames)
            ));

            for (Player other : players) {
                if (other != p) {
                    other.getConnection().sendMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyNotYourCards(p.getRobot().getId(), cardNames.size())
                    ));
                }
            }
        }
        startTimer();
    }

    private void startTimer() {
        programmingTimer = new Timer();
        // Timer and TimerTask classes in java.util
        programmingTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                handleTimerEnded();
            }
        }, 30_000);

        for (Player p : players) {
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyTimerStarted()
            ));
        }
    }

    private void handleTimerEnded() {
        for (Player p : players) {
            if (!p.isReadyRegister()) {
                slowPlayers.add(p.getRobot().getId());
                fillRemainingRegisters(p);
            }
        }

        for (Player p : players) {
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyTimerEnded(slowPlayers)
            ));
        }
        setActivePhase(3);
    }

    private void fillRemainingRegisters(Player player) {
        List<RegisterCard> hand = player.getHand();
        int index = 0;
        for (int i = 0; i < 5; i++) {
            if (player.getRegister().get(i) == null && index < hand.size()) {
                player.chooseCard(((Card) hand.get(index)).getDescription(), i);
                index++;
            }
        }

        List<String> newCards = player.getRegister().stream()
                .filter(Objects::nonNull)
                .map(card -> ((Card) card).getDescription())
                .collect(Collectors.toList());
        player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyCardsYouGotNow(newCards)
        ));
    }

    private void playActivationPhase() {
        for (int register = 0; register < 5; register++) {
            // Collect active cards
            List<MessageDefinitions.ActiveCard> activeCards = new ArrayList<>();
            for (Player p : players) {
                RegisterCard card = p.getRegister().get(register);
                if (card != null) {
                    // Check: getName method in RegisterCard
                    activeCards.add(new MessageDefinitions.ActiveCard(p.getRobot().getId(), ((Card) card).getDescription()));
                }
            }
            for (Player p : players) {
                p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyCurrentCards(activeCards)
                ));
            }
            // Sort players by distance to priority antenna
            List<Player> sortedPlayers = new ArrayList<>();
            Antenna antenna = findAntenna();
            if (antenna == null) {
                throw new IllegalStateException("Antenna must be present on the board.");
            }

            // Group robots by distance
            Map<Integer, List<Robot>> distanceGroups = new HashMap<>();
            List<Player> activePlayers = new ArrayList<>();
            for (Player p : players) {
                if (p.getRegister().get(register) != null) {
                    activePlayers.add(p);
                    Robot robot = p.getRobot();
                    Position robotPos = robot.getPosition();
                    if (robotPos != null && !board.hasRobotFallen(robot)) {
                        int distance = antenna.distanceToRobot(robotPos);
                        distanceGroups.computeIfAbsent(distance, k -> new ArrayList<>()).add(robot);
                    }
                }
                // Sort distances and resolve ties

                List<Integer> sortedDistances = new ArrayList<>(distanceGroups.keySet());
                Collections.sort(sortedDistances);
                for (int distance : sortedDistances) {
                    List<Robot> robots = antenna.sortTiedRobotsByPriority(distanceGroups.get(distance));
                    for (Robot robot : robots) {
                        for (Player player : players) {
                            if (player.getRobot().equals(robot)) {
                                sortedPlayers.add(player);
                                activePlayers.remove(player);
                                break;
                            }
                        }
                    }
                }
            }
            // Include remaining active players (e.g., fallen robots)
            sortedPlayers.addAll(activePlayers);

            // Execute Cards
            for (Player p : sortedPlayers) {
                RegisterCard card = p.getRegister().get(register);
                card.execute(p.getRobot(), p);
                Position pos = p.getRobot().getPosition();
                if (pos != null) {
                    p.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyMovement(p.getRobot().getId(), pos.x(), pos.y())
                    ));
                }

                // Check for checkpoints
                Position robotPos = p.getRobot().getPosition();
                if (robotPos != null) {
                    // Check: getElements method in Board
                    List<BoardElement> elements = board.getElements(robotPos.x(), robotPos.y());
                    for (BoardElement element : elements) {
                        if (element instanceof CheckPoints checkPoint) {
                            int robotId = p.getRobot().getId();
                            int checkpoints = checkPoint.getRobotHighestCheckpoint(robotId);
                            if (checkpoints > 0) {
                                p.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                                    new MessageDefinitions.BodyCheckPointReached(robotId, checkpoints)
                                ));
                                if (checkpoints == board.getTotalCheckpoints()) {
                                    p.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                                        new MessageDefinitions.BodyGameFinished(robotId)
                                    ));
                                    return;
                                }
                            }
                        }
                    }
                }
            }
            // Apply board effects
            for (Player p : players) {
                Position pos = p.getRobot().getPosition();
                if (pos != null) {

                    board.applyEffects(p.getRobot(), pos.x(), pos.y());
                    // Example: Send Animation for ConveyorBelt
                    p.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyAnimation("BlueConveyorBelt")
                    ));
                }
            }
        }
        // Reset registers and start next Programming Phase
        for (Player p : players) {
            p.clearRegister();
        }
        setActivePhase(2);
    }

    // Helper method to find the Antenna
    private Antenna findAntenna() {
        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof Antenna antenna) {
                        return antenna;
                    }
                }
            }
        }
        return null;
    }

    public void determineTurn() {
        // Stub
        setCurrentPlayer();
    }

    // TODO game.initializeGame(); (in Server)
    //Set Board references for all robots when initializing the game
    public void initializeGame() {
        if (board == null) {
            board = new Board(mapType);
        }

        for (Player player : players) {
            Robot robot = player.getRobot();
            robot.setBoard(board);
        }
    }

    // Getter for map selection state
    public boolean isMapSelectionPending() {
        return mapSelectionPending;
    }

    // Get all robots within the specified range (for use with VIRUS cards)
    public List<Robot> getRobotsInRange(Position center, int range) {
        List<Robot> robotsInRange = new ArrayList<>();
        for (Player player : players) {
            Robot robot = player.getRobot();
            Position robotPos = robot.getPosition();
            if (robotPos.distanceTo(center) <= range) {
                robotsInRange.add(robot);
            }
        }
        return robotsInRange;
    }


    // Use the damage card pool in the Game class.
    public void dealSpamDamage(Robot robot) {
        DamageCard spamCard = DamageCardPool.getInstance()
                .getDamageCard(DamageCard.DamageType.SPAM);
        if (spamCard != null) {
            robot.addDamageCard(spamCard.getDamageType());
        }
    }

    public void dealVirusDamage(Robot robot) {
        DamageCard virusCard = DamageCardPool.getInstance()
                .getDamageCard(DamageCard.DamageType.VIRUS);
        if (virusCard != null) {
            robot.addDamageCard(virusCard.getDamageType());
        }
    }

    // Spread the virus effect
    public void spreadVirusFrom(Position virusSource) {
        List<Robot> nearbyRobots = getRobotsInRange(virusSource, 6);

        for (Robot robot : nearbyRobots) {
            DamageCard spamCard = DamageCardPool.getInstance()
                    .getDamageCard(DamageCard.DamageType.SPAM);  // 正确的参数类型
            if (spamCard != null) {
                robot.addDamageCard(spamCard.getDamageType());
            }
        }
    }

    public void playRound() {
        for (Player player : players) {
            List<RegisterCard> register = player.getRegister();
            for (int phase = 0; phase < 5; phase++) {
                if (phase < register.size() && register.get(phase) != null) {
                    register.get(phase).execute(player.getRobot(), player);
                }
            }
            Position pos = player.getRobot().getPosition();
            board.applyEffects(player.getRobot(), pos.x(), pos.y());
        }
    }

    public void addPlayer(Player player) {
        players.add(player);
        // Send existing player info and map state
        for (Player p : players) {
            if (p != player) {
                player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyPlayerAdded(p.getRobot().getId(), p.getName(), p.getRobot().getId())
                ));
                player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyPlayerStatus(p.getRobot().getId(), p.isReady())
                ));
            }
        }
        if (selectedMap != null) {
            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyMapSelected(selectedMap)
            ));
            if (board != null) {
                List<List<List<BoardElement>>> gameMap = board.toSerializableMap();
                player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyGameStarted(5, gameMap)
                ));

            }
        }
        // Place robot if board exists
        if (board != null) {
            Position pos = player.getRobot().getPosition();
            board.placeRobot(player.getRobot(), pos.x(), pos.y());
        }
    }
    /**
     * Gets the game board.
     *
     * @return The Board instance used in this game.
     */
    public Board getBoard() {
        return board;
    }

    /**
     * Retrieves the type of map currently selected for the game.
     *
     * @return A string representation of the selected map type. Possible values may include descriptive names
     *         such as "Risky Crossing" or "Dizzy Highway".
     */
    public String getMapType() {
        return parseMapName(selectedMap).toString();
    }

    public int getCurrentPhase() {
        return currentPhase;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }

}

