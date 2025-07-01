package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
import de.lmu.dbs.ifi.sep25.card.ProgrammingCard.ProgrammingCard;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Maps.MapType;
import de.lmu.dbs.ifi.sep25.network.ClientHandler;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class Game {
    // Constants
    private static final Logger errorLogger = LogManager.getLogger("ErrorLogger");
    private static final Logger appLogger = org.apache.logging.log4j.LogManager.getLogger(Game.class);

    // Singleton instance
    private static Game instance;

    // Game state
    private GamePhase currentPhase;
    private int currentRegister = 0;
    private int roundNumber = 0;

    // Core game components
    private final Board board;
    private final List<Player> players;
    private final String selectedMap;
    private final Stack<Player> currentPlayerTurn;

    // Card decks
    private final DamageCardPool damageDeck = DamageCardPool.getInstance();
    private final Deck<UpgradeCard> upgradeCards = new Deck<>();
    private final Deck<ProgrammingCard> programmingDeck = new Deck<>();

    /* unnecessary globar variables
    private boolean mapSelectionPending;
    private Player firstReadyPlayer;
    private Timer programmingTimer; // For 30-second timer
    private List<Integer> slowPlayers; // Track slow players
    */


    // 1. Initialization and Setup Methods

    private Game(String mapName) {
        players = new ArrayList<>();
        board = new Board(MapType.fromString(mapName));
        currentPhase = null;
        selectedMap = mapName;
        currentPlayerTurn = new Stack<>();

        players.stream().forEach(p -> p.getRobot().setBoard(board));
//        mapSelectionPending = true;
//        slowPlayers = new ArrayList<>();
        initializeProgrammingDeck();
        initializeGame();
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

    /**
     * Set Board references for all robots when initializing the game
     */
    private void initializeGame() {
        // TODO game.initializeGame(); (in Server) -> call in constructor reicht

        for (Player player : players) {
            Robot robot = player.getRobot();
            robot.setBoard(board);
        }
    }

    private void initializeProgrammingDeck() {
        // List of card names with desired counts for Dizzy Highway
        String[] cardNames = {
                "MoveI", "MoveI", // 2 Move 1 Space
                "MoveII", "MoveII", // 2 Move 2 Spaces
                "MoveIII", // 1 Move 3 Spaces
                "BackUp", "BackUp", // 2 Back Up
                "TurnLeft", "TurnLeft", // 2 Turn Left
                "TurnRight", "TurnRight", // 2 Turn Right
                "UTurn", "UTurn", // 2 U-Turn
                "Again", "Again", // 2 Again
                "PowerUp", // 1 Power Up
                "RepeatRoutine", // 1 Repeat Routine
                "SpeedRoutine", // 1 Speed Routine
                "EnergyRoutine", // 1 Energy Routine
                "SpamFolder", // 1 SPAM Folder
                "SandboxRoutine", // 1 Sandbox Routine
                "WeaselRoutine" // 1 Weasel Routine
        };
        for (String cardName : cardNames) {
            RegisterCard card = CardFactory.createCard(cardName);
            if (card instanceof ProgrammingCard programmingCard) {
                programmingDeck.addCard(programmingCard);
            }
        }
        programmingDeck.shuffle();
    }

    private void initializeUpgradeCards() {
        //TODO @yu or @prajal
        // Initialize 40 upgrade cards according to the game rulebook.

        // Permanent Upgrade Card (yellow)
        String[] permanentUpgrades = {
                "AdminPrivilege", "AdminPrivilege", "AdminPrivilege",
                "BlueScreenOfDeath", "BlueScreenOfDeath",
                "Brakes", "Brakes", "Brakes",
                "CacheMemory",
                "CrabLegs",
                "CorruptionWave",
                "DefragGizmo",
                "DeflectorShield", "DeflectorShield",
                "DoubleBarrelLaser", "DoubleBarrelLaser",
                "Firewall", "Firewall",
                "HoverUnit", "HoverUnit", "HoverUnit",
                "MemoryStick", "MemoryStick",
                "MiniHowitzer",
                "ModularChassis",
                "PressorBeam",
                "RailGun",
                "RammingGear", "RammingGear",
                "RearLaser", "RearLaser",
                "Scrambler",
                "SideArms",
                "TractorBeam",
                "TrojanNeedler",
                "VirusModule"
        };

        // Temporary upgrade card (red)
        String[] temporaryUpgrades = {
                "Boink", "Boink", "Boink",
                "EnergyRoutine",
                "Hack", "Hack", "Hack",
                "ManualSort", "ManualSort",
                "MemorySwap", "MemorySwap",
                "Reboot", "Reboot",
                "Recharge",
                "Recompile",
                "Refresh", "Refresh",
                "RepeatRoutine",
                "SandboxRoutine",
                "SpamBlocker",
                "SpamFolderRoutine",
                "SpeedRoutine",
                "Teleporter",
                "WeaselRoutine",
                "Zoop", "Zoop"
        };

        // When CardFactory supports upgrade card creation:
        // Add permanent upgrade card
//        for (String cardName : permanentUpgrades) {
//            UpgradeCard card = CardFactory.createUpgradeCard(cardName);
//            if (card != null) {
//                upgradeCards.addCard(card);
//            }
//        }
//
//        // Add temporary upgrade card
//        for (String cardName : temporaryUpgrades) {
//            UpgradeCard card = CardFactory.createUpgradeCard(cardName);
//            if (card != null) {
//                upgradeCards.addCard(card);
//            }
//        }

        upgradeCards.shuffle();
    }

    // 2. Game Flow Control Methods

    /**
     * Start the game main loop
     */
    public void startGameLoop() {
        final Thread mainLoop = new Thread(() -> {

            // Setup phase
            setPhase(GamePhase.SETUP);
            determinePlayerOrder();
            handleSetupPhase();

            appLogger.info("Finished setup phase. Moving to main loop.");

            //TODO @Yu
            // For implementing the "loop," I recommend not using an actual loop.
            // Instead, whenever the server receives input messages from clients,
            // update the relevant lists or flags in the game state. After that,
            // always check if all required inputs for the current game phase are present.
            // Using a loop here can cause issues with latches and waiting, making the system more complex and error-prone.
            // This event-driven approach will be simpler to implement and easier to fix etc.
            // .
            // tldr;
            // Avoid using a loop for the "loop" implementation.
            // Instead, update lists/flags whenever the server receives messages from clients and then check if all inputs are ready for the current game phase.

            // Game continues until a player wins
            do {
                // Start a new round
                roundNumber++;
                appLogger.info("Starting round {}.", roundNumber);

                // Programming phase
                setPhase(GamePhase.PROGRAMMING);
                handleProgrammingPhase();

                // Activation phase
                setPhase(GamePhase.ACTIVATION);
                determinePlayerOrder();
                handleActivationPhase();

                // Check if the game has ended
            } while (!checkGameEnd());

        });

        appLogger.info("Starting game main loop.");

        mainLoop.start();
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

    private void endRound() {
        // Clear registers and move cards to discard pile
        for (Player player : players) {
            List<RegisterCard> register = player.getRegister();
            for (RegisterCard card : register) {
                if (card != null) {
                    // Move card to discard pile (if it's not a damage card)
                    // Damage cards should be handled differently
                    if (!(card instanceof DamageCard)) {
                        player.discardCard(card);
                    }
                }
            }

            // Clear the register for next round
            for (int i = 0; i < register.size(); i++) {
                player.removeCard(null, i);
            }

            // Reset player's ready state for next round
            player.setReadyRegister(false);
        }

        // Reset the current register counter
        currentRegister = 0;

        // TODO @lukas：Broadcast end of round message
        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyReceivedChat("Round " + roundNumber +
                                " completed.", 0, false)
                )
        );
    }

    private boolean checkGameEnd() {
        for (Player player : players) {
            // Check if any player has reached all checkpoints
            int checkpointsReached = player.getReachedCheckpoints();
            if (checkpointsReached >= board.getTotalCheckpoints()) {
                // Broadcast game end message
                Server.getInstance().broadcastMessage(
                        new MessageDefinitions.Message<>(
                                new MessageDefinitions.BodyGameFinished(player.getRobot().getId())
                        )
                );

                // TODO @lukas：Send final chat message
                Server.getInstance().broadcastMessage(
                        new MessageDefinitions.Message<>(
                                new MessageDefinitions.BodyReceivedChat(
                                        "Game over! Player " + player.getName() +
                                                " has won by reaching all checkpoints!",
                                        0, false
                                )
                        )
                );

                return true;
            }
        }
        return false;
    }

    // 3. Phase Management Methods

    /**
     * Represents the various phases of the game, defining the sequence of operations
     * that occur during the game's lifecycle.
     *
     * <p>The game progresses through the following phases:
     * <ul>
     *   <li><b>SETUP</b>: Initial configuration and game setup.</li>
     *   <li><b>UPGRADE</b>: Players acquire and apply upgrades.</li>
     *   <li><b>PROGRAMMING</b>: Players select and sequence robot actions.</li>
     *   <li><b>ACTIVATION</b>: Execution of the programmed actions.</li>
     * </ul>
     *
     * <p>Each phase may be associated with an integer value for easier representation or comparison.
     */
    public enum GamePhase {
        SETUP(0),
        UPGRADE(1),
        PROGRAMMING(2),
        ACTIVATION(3);

        private final int value;

        GamePhase(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }

    }

    /**
     * Sets the current phase of the game and broadcasts the updated phase to all connected clients.
     *
     * @param phase The new game phase to be set. Must be one of the defined values in the {@code GamePhase} enum.
     */
    public void setPhase(GamePhase phase) {
        if (currentPhase == null) {
            appLogger.info("Setting initial phase to {}.", phase);
        } else {
            appLogger.info("Exiting {} phase.", currentPhase);
        }

        this.currentPhase = phase;

        appLogger.info("Set new phase and broadcasted: {}.", currentPhase);

        // TODO @Lukas broadcast current phase
        Server.getInstance().broadcastMessage(new MessageDefinitions.Message<>(new MessageDefinitions.BodyActivePhase(currentPhase.getValue())));
    }

    /**
     * Handle the setup phase
     */
    private void handleSetupPhase() {

        initPlacement();

        // Wait for all players to choose starting positions
        // This logic is triggered by the client, server responds
    }

    /**
     * Handle the programming phase
     */
    private void handleProgrammingPhase() {
        // Distribute programming cards to each player
        for (Player player : players) {
            // Clear previous registers if necessary
            for (int i = 0; i < 5; i++) {
                if (player.getRegister().get(i) != null) {
                    player.removeCard(null, i);
                }
            }

            // Deal programming cards to the player
            List<String> cardNames = new ArrayList<>();
            List<RegisterCard> hand = player.getHand();

            // Clear hand from previous round if needed
            hand.clear();

            // Draw cards (usually 9)
            for (int i = 0; i < 9; i++) {
                player.drawCard();
                // Add card names to list for notification
                if (i < hand.size()) {
                    cardNames.add(CardFactory.getCardName(hand.get(i)));
                }
            }

            // TODO @lukas:Notify player of their cards
            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyYourCards(cardNames)
            ));

            // TODO @lukas:Notify other players of card count
            for (Player otherPlayer : players) {
                if (otherPlayer != player) {
                    otherPlayer.getConnection().sendMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyNotYourCards(player.getRobot().getId(), hand.size())
                    ));
                }
            }

            // Reset player's ready state for programming
            player.setReadyRegister(false);
        }

        // TODO: The actual card selection is handled by client events through the Player.chooseCard method
        // Server will wait for all players to finish programming
    }

    /**
     * Handle the activation phase
     */
    private void handleActivationPhase() {
        // Activate all five registers
        for (currentRegister = 0; currentRegister < 5; currentRegister++) {
            System.out.println("Activating register " + (currentRegister + 1));

            // Sort players by priority
            List<Player> sortedPlayers = new ArrayList<>(currentPlayerTurn);

            // Collect all cards from players in the current register
            List<MessageDefinitions.ActiveCard> activeCards = new ArrayList<>();
            for (Player player : sortedPlayers) {
                List<RegisterCard> playerRegister = player.getRegister();
                if (currentRegister < playerRegister.size() && playerRegister.get(currentRegister) != null) {
                    RegisterCard card = playerRegister.get(currentRegister);
                    String cardName = CardFactory.getCardName(card);
                    activeCards.add(new MessageDefinitions.ActiveCard(player.getRobot().getId(), cardName));
                }
            }

            // TODO @lukas:Broadcast CurrentCards message
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCurrentCards(activeCards)
                    )
            );

            // Activate player cards in order
            for (Player player : sortedPlayers) {
                activatePlayerCard(player, currentRegister);
            }

            // Activate board elements
            activateBoardElements();

            // Handle robot lasers
            handleRobotLasers();
        }

        // End of round processing
        endRound();
    }

    /**
     * Handle the initial placement phase, allowing players to select their starting positions in order of connection
     */
    public void initPlacement() {
        while (!currentPlayerTurn.isEmpty()) {
            ClientHandler currentPlayerConnection = currentPlayerTurn.pop().getConnection();

            final CountDownLatch latch = new CountDownLatch(1);  // One latch per turn
            currentPlayerConnection.setPlacementLatch(latch);

            // Notify all clients who is placing TODO @Lukas broadcast CurrentPlayer
            currentPlayerConnection.broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyCurrentPlayer(currentPlayerConnection.getMyID())
            ));

            try {
                boolean completed = latch.await(30, TimeUnit.SECONDS); // Wait until current player confirms placement
                if (!completed) {
                    errorLogger.error("Placement phase timed out for client {}, closing client and connection.", currentPlayerConnection.getMyID());
                    currentPlayerConnection.closeAll();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("Interrupted while waiting for placements.");
            }
        }
    }

    /**
     * Start the programming phase for all players
     */
    public void startProgrammingPhase() {
        setPhase(GamePhase.PROGRAMMING);

        // Deal cards to all players
        for (Player player : players) {
            // Clear previous programming
            player.endRound();

            // Deal new cards
            player.dealProgrammingCards();

            // Send cards to player
            List<String> cardNames = new ArrayList<>();
            for (RegisterCard card : player.getHand()) {
                cardNames.add(CardFactory.getCardName(card));
            }

            player.getConnection().sendMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyYourCards(cardNames)
                    )
            );

            // Notify others
            int cardCount = player.getHand().size();
            for (Player otherPlayer : players) {
                if (otherPlayer != player) {
                    otherPlayer.getConnection().sendMessage(
                            new MessageDefinitions.Message<>(
                                    new MessageDefinitions.BodyNotYourCards(
                                            player.getRobot().getId(), cardCount
                                    )
                            )
                    );
                }
            }
        }
    }

    // 4. Player Management Methods

    /**
     * Adds a player to the current game.
     *
     * @param player The Player instance to be added to the game.
     */
    public void addPlayer(Player player) {
        players.add(player);
    }

    /**
     * Handle player selection of starting position
     *
     * @param player Player selecting starting position
     * @param x      X-coordinate of starting position
     * @param y      Y-coordinate of starting position
     * @return Returns true if starting position is successfully set, otherwise returns false
     */
    public boolean setPlayerStartingPosition(Player player, int x, int y) {
        if (player == null) {
            return false;
        }

        // Check if it is in the setup phase
        if (currentPhase != GamePhase.SETUP) {
            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Not in setup phase")
            ));
            return false;
        }

        final Position targetPos = new Position(x, y);

        // Check if the location is valid
        if (!board.isValidPosition(targetPos)) {
            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Invalid position: (" + x + ", " + y + ")")
            ));
            return false;
        }

        // Check if this location is the starting point
        boolean isStartPoint = board.getStartingPoints().contains(targetPos);

        if (!isStartPoint) {
            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyError("Position (" + x + ", " + y + ") is not a starting point")
            ));
            return false;
        }

        // If the player has already selected a starting position, the previous position must be released.
        Robot robot = player.getRobot();
        Position currentPos = robot.getPosition();
        if (currentPos != null) {
            List<BoardElement> currentElements = board.getElements(currentPos.x(), currentPos.y());
            for (BoardElement element : currentElements) {
                if (element instanceof StartPoint) {
                    ((StartPoint) element).release();
                    break;
                }
            }
        }

        final StartPoint startPoint = (StartPoint) board.getElements(x, y).stream().filter(e -> e instanceof StartPoint).findFirst().orElse(null);

        if (startPoint == null) {
            errorLogger.error("No start point found at (" + x + ", " + y + ")");
            return false;
        }

        // Occupy a new starting point
        startPoint.occupy(robot.getId());

        // Set robot position and orientation
        robot.setPosition(targetPos);
        robot.setDirection(startPoint.getRobotDirection());

        // Update robot position on board
        board.updateRobotPosition(robot, targetPos);

        // Determine direction string (based on map type)
        // TODO @lukas:Message that the broadcast start position is already occupied
        Server.getInstance().broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyStartingPointTaken(x, y, switch (selectedMap) {
                    case "Heavy Merge Area", "Death Trap" -> "left";
                    case "Pilgrimage", "Gear Stripper" -> "top";
                    default -> "right";
                }, robot.getId())
        ));

        return true;
    }

    /**
     * Determine player order based on priority
     */
    private void determinePlayerOrder() {
        if (!currentPlayerTurn.isEmpty()) {
            appLogger.info("Current player order not empty, repopulating.");
        }
        currentPlayerTurn.clear();
        switch (currentPhase) {
            case SETUP -> {
                currentPlayerTurn.addAll(players.reversed());
            }
            case ACTIVATION -> {
                List<Player> sortedPlayers = new ArrayList<>(players);

                // Determine priority based on the antenna on the board
                Position antennaPosition = board.getAntennaPosition();

                if (antennaPosition == null) {
                    // If there is no antenna position, return the default order.
                    errorLogger.error("No antenna position found. currentPlayerTurn unmodified.");
                    break;
                }

                // First sort by distance to the antenna, but only for robots with valid positions
                sortedPlayers.sort(Comparator.comparingInt(p -> {
                    Robot robot = p.getRobot();
                    Position robotPos = robot.getPosition();
                    // Add null check
                    if (robotPos == null) {
                        return Integer.MAX_VALUE; // Robots without positions are ranked last.
                    }
                    return robotPos.distanceTo(antennaPosition);
                }));

                // If players have the same distance, handle according to rules
                // TODO:Simplified here, actual implementation should be more complex


                // TODO @lukas:Broadcast current player order
                for (Player player : sortedPlayers) {
                    MessageDefinitions.Message<MessageDefinitions.BodyCurrentPlayer> message = new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCurrentPlayer(player.getRobot().getId()));
                    Server.getInstance().broadcastMessage(message);
                }

                currentPlayerTurn.addAll(sortedPlayers.reversed());
            }
            default ->
                    throw new IllegalAccessError("determinePlayerOrder shot not be called in phase: " + currentPhase);
        }

        appLogger.info("Current player order set: {}", currentPlayerTurn.stream().map(Player::toString).toList());
    }

    // 5. Card and Action Methods

    /**
     * Activate a player's card in the specified register
     */
    private void activatePlayerCard(Player player, int register) {
        List<RegisterCard> playerRegister = player.getRegister();

        if (register < playerRegister.size() && playerRegister.get(register) != null) {
            RegisterCard card = playerRegister.get(register);

            // TODO @lukas:Broadcast card before execution
            String cardName = CardFactory.getCardName(card);
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCardPlayed(player.getRobot().getId(), cardName)
                    )
            );

            // Handle DamageCard effects explicitly
            if (card instanceof DamageCard damageCard) {
                handleDamageCardEffect(damageCard, player);
            } else {
                // Execute card effect
                card.execute(player.getRobot(), player);
            }
            // Broadcast robot movement if position changed
            Position robotPosition = player.getRobot().getPosition();
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyMovement(
                                    player.getRobot().getId(),
                                    robotPosition.x(),
                                    robotPosition.y()
                            )
                    )
            );
            // Apply board effects after card execution
            board.applyEffects(player.getRobot(), robotPosition.x(), robotPosition.y());
        }
    }

    /**
     * Handles the effects of a DamageCard on a player's robot based on the card's damage type.
     *
     * @param card   The DamageCard to be processed. Contains information about the specific type of damage.
     * @param player The player whose robot is affected by the damage card effect.
     */
    private void handleDamageCardEffect(DamageCard card, Player player) {
        Robot robot = player.getRobot();
        switch (card.getDamageType()) {
            case SPAM:
                // SPAM does nothing when executed (already disrupts register)
                break;
            case VIRUS:
                dealVirusDamage(robot);
                break;
            case WORM:
                handleRobotReboot(robot);
                break;
            case TROJAN_HORSE:
                dealSpamDamage(robot);
                dealSpamDamage(robot);
                break;
        }
    }

    /**
     * Deals a spam damage card to the specified robot by retrieving it from the damage card pool.
     *
     * @param robot The Robot instance to which a spam damage card will be added.
     */
    public void dealSpamDamage(Robot robot) {
        DamageCard spamCard = DamageCardPool.getInstance()
                .getDamageCard(DamageCard.DamageType.SPAM);
        if (spamCard != null) {
            robot.addDamageCard(spamCard.getDamageType());
        }
    }

    /**
     * Deals a virus damage card to the specified robot by retrieving it from
     * the damage card pool. If a virus damage card is available, it will be
     * added to the robot's damage cards.
     *
     * @param robot The Robot instance to which a virus damage card will be added.
     */
    public void dealVirusDamage(Robot robot) {
        DamageCard virusCard = DamageCardPool.getInstance()
                .getDamageCard(DamageCard.DamageType.VIRUS);
        if (virusCard != null) {
            robot.addDamageCard(virusCard.getDamageType());
        }
    }

    // 6. Board-related Methods

    /**
     * Activates all board elements for the current game state. This method processes
     * the following elements in sequence:
     *
     * <ol>
     *   <li><b>Fast Conveyor Belts (Blue)</b>: Moves robots on fast belts and updates their positions.</li>
     *   <li><b>Slow Conveyor Belts (Green)</b>: Moves robots on slow belts and updates their positions.</li>
     *   <li><b>Push Panels</b>: Triggers panels if active in the current register, affecting any robot present.</li>
     *   <li><b>Gears</b>: Rotates robots on gear tiles based on direction, broadcasting orientation changes.</li>
     *   <li><b>Board Lasers</b>: Deals damage to robots hit by fixed board lasers, broadcasting damage updates.</li>
     *   <li><b>Energy Spaces</b>: Grants energy points to robots on these tiles, broadcasting collection events.</li>
     *   <li><b>Checkpoints</b>: Marks progress if a robot reaches a new checkpoint, possibly ending the game.</li>
     * </ol>
     *
     * <p><b>Note:</b> Robot-to-robot laser interactions are handled separately in {@code handleRobotLasers()}.
     */
    private void activateBoardElements() {
        // 1. Blue conveyor belts (fast)
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof Belts belt &&
                            belt.getSpeed() == Belts.BeltSpeed.FAST) {
                        // Find robot at this position and activate belt
                        Robot robot = board.getRobotAt(new Position(x, y));
                        if (robot != null) {
                            belt.applyEffect(robot, board);
                        }
                    }
                }
            }
        }

        // 2. Green conveyor belts (slow)
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof Belts belt &&
                            belt.getSpeed() == Belts.BeltSpeed.SLOW) {
                        Robot robot = board.getRobotAt(new Position(x, y));
                        if (robot != null) {
                            belt.applyEffect(robot, board);
                        }
                    }
                }
            }
        }

        // 3. Push panels (by register number)
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof PushPanel panel) {
                        // Check if the panel activates in the current register
                        if (panel.isActiveInCurrentRegister()) {
                            Robot robot = board.getRobotAt(new Position(x, y));
                            if (robot != null) {
                                panel.applyEffect(robot, board);
                            }
                        }
                    }
                }
            }
        }

        // 4. Gears
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof Gear gear) {
                        Robot robot = board.getRobotAt(new Position(x, y));
                        if (robot != null) {
                            // Save direction before activation
                            Direction oldDirection = robot.getDirection();

                            // Apply gear effect
                            gear.applyEffect(robot, board);

                            // TODO @lukas:Broadcast rotation if direction changed
                            if (oldDirection != robot.getDirection()) {
                                String rotation = gear.getRotationDirection() == Gear.RotationDirection.CLOCKWISE
                                        ? "clockwise" : "counterclockwise";

                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyPlayerTurning(
                                                        robot.getId(), rotation
                                                )
                                        )
                                );

                                // TODO @lukas:Animation
                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyAnimation("Gear")
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }

        // 5. Board lasers
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof Laser laser) {
                        Robot robot = board.getRobotAt(new Position(x, y));
                        if (robot != null) {
                            // Apply laser effect
                            int oldDamage = robot.getDamage();
                            laser.applyEffect(robot, board);

                            // If damage increased, broadcast damage
                            if (robot.getDamage() > oldDamage) {
                                // TODO @lukas:Animation
                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyAnimation("WallShooting")
                                        )
                                );

                                // Deal damage cards
                                List<String> damageCards = new ArrayList<>();
                                for (int i = 0; i < robot.getDamage() - oldDamage; i++) {
                                    damageCards.add("Spam");
                                }

                                //TODO @lukas:MessageDefinitions.BodyDrawDamage
                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyDrawDamage(robot.getId(), damageCards)
                                        )
                                );
                            }
                        }
                    }
                }
            }
        }

        // 6. Energy spaces
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof EnergySpace energySpace) {
                        Robot robot = board.getRobotAt(new Position(x, y));
                        if (robot != null) {
                            // Get player for this robot
                            Player player = null;
                            for (Player p : players) {
                                if (p.getRobot() == robot) {
                                    player = p;
                                    break;
                                }
                            }

                            if (player != null) {
                                int oldEnergy = player.getEnergy();
                                energySpace.applyEffect(robot, board);

                                // TODO @lukas：If energy increased, broadcast energy update
                                if (player.getEnergy() > oldEnergy) {
                                    Server.getInstance().broadcastMessage(
                                            new MessageDefinitions.Message<>(
                                                    new MessageDefinitions.BodyEnergy(
                                                            robot.getId(),
                                                            player.getEnergy(),
                                                            "EnergySpace"
                                                    )
                                            )
                                    );

                                    //TODO @lukas：Animation
                                    Server.getInstance().broadcastMessage(
                                            new MessageDefinitions.Message<>(
                                                    new MessageDefinitions.BodyAnimation("EnergySpace")
                                            )
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }

        // 7. Checkpoints
        for (int y = 0; y < board.getHeight(); y++) {
            for (int x = 0; x < board.getWidth(); x++) {
                for (BoardElement element : board.getElements(x, y)) {
                    if (element instanceof CheckPoints checkpoint) {
                        Robot robot = board.getRobotAt(new Position(x, y));
                        if (robot != null) {
                            int oldCheckpoints = checkpoint.getRobotHighestCheckpoint(robot.getId());
                            checkpoint.applyEffect(robot, board);
                            int newCheckpoints = checkpoint.getRobotHighestCheckpoint(robot.getId());

                            // TODO @lukas：If new checkpoint reached, broadcast update
                            if (newCheckpoints > oldCheckpoints) {
                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyCheckPointReached(
                                                        robot.getId(), newCheckpoints
                                                )
                                        )
                                );

                                // TODO @lukas：Animation
                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyAnimation("CheckPoint")
                                        )
                                );

                                // TODO @lukas：Check for game win
                                if (newCheckpoints >= board.getTotalCheckpoints()) {
                                    Server.getInstance().broadcastMessage(
                                            new MessageDefinitions.Message<>(
                                                    new MessageDefinitions.BodyGameFinished(robot.getId())
                                            )
                                    );
                                }
                            }
                        }
                    }
                }
            }
        }

        // Note: Robot lasers are handled separately in handleRobotLasers()
    }

    /**
     * Processes robot laser firing during the game. Each robot fires a laser in its current
     * direction, potentially damaging other robots in its path.
     *
     * <p><b>Functionality:</b>
     * <ul>
     *   <li>Skips robots that are powered down or have fallen off the board.</li>
     *   <li>Iterates along the laser path from the robot's position in its current direction.</li>
     *   <li>If a wall blocks the laser, the beam stops.</li>
     *   <li>If another robot is hit, it takes 1 damage, and the beam stops.</li>
     *   <li>Broadcasts a server message when a laser hits a robot.</li>
     * </ul>
     *
     * <p><b>Preconditions:</b>
     * <ul>
     *   <li>The board, players, and robot states are fully initialized.</li>
     *   <li>Each robot has a valid position and direction.</li>
     * </ul>
     *
     * <p><b>Postconditions:</b>
     * <ul>
     *   <li>Robots hit by lasers take 1 damage.</li>
     *   <li>Clients receive a broadcast message indicating the laser hit.</li>
     * </ul>
     *
     * <p><b>Note:</b> The laser advances step by step until it hits a wall, robot, or goes off the board.
     */
    private void handleRobotLasers() {
        for (Player player : players) {
            Robot robot = player.getRobot();

            // Skip if robot is powered down or has fallen off
            if (robot.isPoweredDown() || board.hasRobotFallen(robot)) {
                continue;
            }

            Position position = robot.getPosition();
            Direction direction = robot.getDirection();

            // Add null check - skip if robot does not yet have position or direction
            if (position == null || direction == null) {
                continue;
            }

            // Get the position in front of the robot
            Position nextPos = position.move(direction);

            // Continue until we hit a wall, the board edge, or a robot
            while (board.isValidPosition(nextPos)) {
                // Check if there's a wall blocking the laser
                boolean blocked = false;
                for (BoardElement element : board.getElements(nextPos.x(), nextPos.y())) {
                    if (element instanceof Wall wall) {
                        if (!wall.canPassThroughFromDirection(direction)) {
                            blocked = true;
                            break;
                        }
                    }
                }

                if (blocked) {
                    break;
                }

                // Check if there's a robot at this position
                Robot targetRobot = board.getRobotAt(nextPos);
                if (targetRobot != null) {
                    // Deal damage to the robot
                    targetRobot.takeDamage(1);

                    // TODO @lukas：Broadcast laser hit message
                    Server.getInstance().broadcastMessage(
                            new MessageDefinitions.Message<>(
                                    new MessageDefinitions.BodyAnimation("PlayerShooting")
                            )
                    );

                    break;  // Laser stops after hitting a robot
                }

                // Move to next position in the laser path
                nextPos = nextPos.move(direction);
            }
        }
    }

    /**
     * Handle robot rebooting
     *
     * @param robot The robot that needs to reboot
     */
    private void handleRobotReboot(Robot robot) {
        // Find the player for this robot
        Player player = null;
        for (Player p : players) {
            if (p.getRobot() == robot) {
                player = p;
                break;
            }
        }

        if (player == null) return;

        // Broadcast reboot message
        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyReboot(robot.getId())
                )
        );

        // Robot takes damage and cancels programming
        robot.takeDamage(2);
        robot.cancelProgramming();

        // Move to reboot position
        Position rebootPos = board.getRebootPosition();
        robot.setPosition(rebootPos);
        board.updateRobotPosition(robot, rebootPos);

        // Wait for player to choose direction
        // TODO:this would be async with client response
        // For now, just default to north
        Direction defaultDirection = Direction.NORTH;
        robot.setDirection(defaultDirection);

        // TODO @lukas：Broadcast movement to reboot position
        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyMovement(
                                robot.getId(),
                                rebootPos.x(),
                                rebootPos.y()
                        )
                )
        );

        // TODO @lukas：Broadcast direction
        String directionStr = "top"; // Default direction
        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyRebootDirection(directionStr)
                )
        );
    }

    /**
     * Retrieves a list of robots located within a specified range from a given central position.
     * This method is particularly used for actions involving VIRUS cards.
     *
     * @param center The central position from which the range is calculated.
     * @param range  The maximum distance from the center to consider a robot within range.
     * @return A list of Robot objects that are within the specified range from the given position.
     */
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

    /**
     * Spreads the virus effect
     *
     * @param virusSource position of initial virusSource
     */
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

    // 7. Utility and Getter Methods

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
     * such as "Risky Crossing" or "Dizzy Highway".
     */
    public String getMapType() {
        return selectedMap;
    }

    /**
     * Find the player who owns the given robot
     *
     * @param robot The robot to find the owner for
     * @return The player who owns the robot, or null if not found
     */
    private Player getPlayerForRobot(Robot robot) {
        for (Player player : players) {
            if (player.getRobot() == robot) {
                return player;
            }
        }
        return null;
    }

    public int getCurrentPhase() {
        return currentPhase != null ? currentPhase.getValue() : -1;
    }

    public List<Player> getPlayers() {
        return new ArrayList<>(players);
    }


    // Handle player readiness and map selection
//    public void setPlayerReady(Player player, boolean ready) {
//        player.setReady(ready);
//        // Broadcast PlayerStatus
//        for (Player p : players) {
//            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyPlayerStatus(player.getRobot().getId(), ready)
//            ));
//        }
//
//        // Send SelectMap to first ready player
//        if (ready && firstReadyPlayer == null && mapSelectionPending) {
//            firstReadyPlayer = player;
//            List<String> availableMaps = new ArrayList<>();
//            availableMaps.add("Dizzy Highway");
//            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodySelectMap(availableMaps)
//            ));
//        } else if (!ready && firstReadyPlayer == player) {
//            // Handle unready player
//            firstReadyPlayer = null;
//            for (Player p : players) {
//                if (p.isReady()) {
//                    firstReadyPlayer = p;
//                    List<String> availableMaps = new ArrayList<>();
//                    availableMaps.add("Dizzy Highway");
//                    p.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                            new MessageDefinitions.BodySelectMap(availableMaps)
//                    ));
//                    break;
//                }
//            }
//        }
//    }

    // Process map selection
//    public void selectMap(Player player, String mapName) {
//        // Validate player and map
//        if (player != null && (player != firstReadyPlayer || !mapSelectionPending)) {
//            player.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyError("Not authorized to select map")
//            ));
//            return;
//        }
//
//        MapType mapType = MapType.fromString(mapName);
//        if (mapType == null) {
//            if (player != null) {
//                player.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                        new MessageDefinitions.BodyError("Invalid map: " + mapName)
//                ));
//            }
//            return;
//        }
//
//
//        // Set map and initialize board
//        selectedMap = mapName;
//        mapSelectionPending = false;
//
//        board = new Board(MapType.fromString(mapName));
//        initializeGame();
//
//        // Broadcast MapSelected and GameStarted
//        for (Player p : players) {
//            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyMapSelected(mapName)
//            ));
//            //Use convertToBoardElementMap for BodyGameStarted
//            p.getConnection().sendMessage(board.getSerializedBoardAsMessage());
//        }
//        startGameLoop();
//    }

//    // Convert map to List<Object> for BodyGameStarted
//    private List<Object> convertToObjectList(List<List<List<BoardElement>>> map) {
//        List<Object> result = new ArrayList<>();
//        for (List<List<BoardElement>> col : map) {
//            List<Object> colList = new ArrayList<>();
//            for (List<BoardElement> row : col) {
//                colList.add(row == null ? null : new ArrayList<>(row));
//            }
//            result.add(colList);
//        }
//        return result;
//    }

    // Helper method to convert serialized map to List<List<List<BoardElement>>> if needed
//    private List<List<List<BoardElement>>> convertToBoardElementMap() {
//        List<List<List<BoardElement>>> result = new ArrayList<>();
//        Tile[][] grid = board.getGrid();// Assuming getGrid returns BoardElement[][][]
//
//        for (int x = 0; x < board.getWidth(); x++) {
//            List<List<BoardElement>> col = new ArrayList<>();
//            for (int y = 0; y < board.getHeight(); y++) {
//                List<BoardElement> elements = grid[x][y].toSerializableList();
//                col.add(elements);
//            }
//            result.add(col);
//        }
//        return result;
//    }

//    // Added: Stub for AI-only map selection
//    public void selectMapForAI() {
//        if (mapSelectionPending && players.stream().allMatch(p -> p.getConnection() == null)) {
//            String[] availableMaps = {"Dizzy Highway", "Extra Crispy", "Lost Bearings", "Death Trap"};
//            String selectedMap = availableMaps[new Random().nextInt(availableMaps.length)];
//
//            selectMap(null, selectedMap);
//        }
//    }

    /**
     * Starts the game by setting up the initial game phase
     * Called after map selection and board initialization
     */
//    private void startGame() {
//        if (!mapSelectionPending && players.size() >= 2) {
//            // Set up the initial stage
//            setPhase(GamePhase.SETUP);
//            // Send current player information
//            setCurrentPlayer();
//            // Start the game main loop (if necessary)
////            new Thread(this::startGameLoop).start();
////            System.out.println("Game started with map: " + selectedMap);
//        }
//    }

//    private void startTimer() {
//        programmingTimer = new Timer();
//        // Timer and TimerTask classes in java.util
//        programmingTimer.schedule(new TimerTask() {
//            @Override
//            public void run() {
//                handleTimerEnded();
//            }
//        }, 30_000);
//
//        for (Player p : players) {
//            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyTimerStarted()
//            ));
//        }
//    }

//    private void handleTimerEnded() {
//        for (Player p : players) {
//            if (!p.isReadyRegister()) {
//                slowPlayers.add(p.getRobot().getId());
//                fillRemainingRegisters(p);
//            }
//        }
//
//        for (Player p : players) {
//            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyTimerEnded(slowPlayers)
//            ));
//        }
//        setPhase(GamePhase.ACTIVATION);
//    }

//    private void fillRemainingRegisters(Player player) {
//        List<RegisterCard> hand = player.getHand();
//        int index = 0;
//        for (int i = 0; i < 5; i++) {
//            if (player.getRegister().get(i) == null && index < hand.size()) {
//                RegisterCard card = hand.get(index);
//                String cardName = CardFactory.getCardName(card);
//                player.chooseCard(((Card) hand.get(index)).getDescription(), i);
//                index++;
//            }
//        }
//
//        List<String> newCards = player.getRegister().stream()
//                .filter(Objects::nonNull)
//                .map(card -> ((Card) card).getDescription())
//                .collect(Collectors.toList());
//        player.getConnection().sendMessage(new MessageDefinitions.Message<>(
//                new MessageDefinitions.BodyCardsYouGotNow(newCards)
//        ));
//    }

    //    // Getter for map selection state
//    public boolean isMapSelectionPending() {
//        return mapSelectionPending;
//    }

}

