package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.CardFactory;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
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


    // 1. Initialization and Setup Methods

    private Game(String mapName) {
        players = new ArrayList<>();
        board = new Board(MapType.fromString(mapName));
        currentPhase = null;
        selectedMap = mapName;
        currentPlayerTurn = new Stack<>();
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
    public void initializeGame() {
        // TODO game.initializeGame(); (in Server)

        for (Player player : players) {
            Robot robot = player.getRobot();
            robot.setBoard(board);
        }
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
        players.forEach(p -> p.getRobot().setBoard(board));

        final Thread mainLoop = new Thread(() -> {

            // Setup phase
            setPhase(GamePhase.SETUP);

            resetPlayersRound();

            determinePlayerOrder();
            handleSetupPhase();

            appLogger.info("Finished setup phase. Moving to main loop.");

            startNewGameRound();
        });

        appLogger.info("Starting game main loop.");
        mainLoop.start();
    }

    /**
     * Starts a new round of the game.
     * This method should be called when a round ends and a new one needs to begin,
     * or at the start of the game after setup.
     */
    private void startNewGameRound() {
        // Reset
        resetPlayersRound();
        Server.getInstance().resetReadyRegister();
        // Start new round

        roundNumber++;
        appLogger.info("Starting round {}.", roundNumber);
        setPhase(GamePhase.PROGRAMMING);
        appLogger.info("Enter the programming phase and start executing handleProgrammingPhase()");
        handleProgrammingPhase();
    }

    /**
     * Checks if all required inputs for the current programming phase are ready.
     * If all players have completed their programming, advances to activation phase.
     * This method should be called whenever a player submits programming choices.
     *
     * @return true if game phase was advanced, false otherwise
     */
    public boolean checkAndAdvanceFromProgrammingPhase() {
        // Check if all players have completed programming
        // If all players are ready and it is currently the programming phase, enter the activation phase.
        if (players.stream().allMatch(Player::isReadyRegister)) {
            appLogger.info("All players have completed programming. Moving to activation phase.");
            enterActivationPhase();
            return true;
        }
        return false;
    }

    /**
     * Enters the activationPhase.
     **/
    public synchronized void enterActivationPhase() {
        if (currentPhase != GamePhase.PROGRAMMING) {
            appLogger.warn("enterActivationPhase() called while phase is {}", currentPhase);
            return;
        }

        appLogger.info("Entering activation phase.");
        setPhase(GamePhase.ACTIVATION);
        resetPlayersHand();
        determinePlayerOrder();
        handleActivationPhase();
    }

    /**
     * Checks if the current activation phase for the current register is complete.
     * If completed, advances to the next register or ends the round.
     * This method should be called after each player's robot movement is processed.
     *
     * @return true if game advanced to next register or round, false otherwise
     */
    public boolean checkAndAdvanceActivationPhase() {
        if (currentPlayerTurn.isEmpty() && currentPhase == GamePhase.ACTIVATION) {
            currentRegister++;

            if (currentRegister < 5) {
                appLogger.info("Register {} completed. Moving to register {}.", currentRegister - 1, currentRegister);
                determinePlayerOrder();
                return true;
            } else {
                appLogger.info("All registers processed. Ending round {}.", roundNumber);
                endRound();

                // Check if the game has ended
                if (checkGameEnd()) {
                    appLogger.info("Game end condition met. Game over.");
                } else {
                    startNewGameRound();
                }
                return true;
            }
        }

        return false;
    }

    private void endRound() {
        // Reset players
        resetPlayersRound();

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

    /**
     * Checks if the game has ended and handles all logic when the game ends.
     * Game end condition: any player reaches all checkpoints.
     * When the game ends, this method will:
     * 1. Determine the winner.
     * 2. Broadcast the BodyGameFinished message.
     * 3. Send a chat message that the game has ended.
     *
     * @return Returns true if the game has ended, otherwise returns false.
     */
    private boolean checkGameEnd() {
        Player winner = null;

        for (Player player : players) {
            // Check if any player has reached all checkpoints
            int checkpointsReached = player.getReachedCheckpoints();
            if (checkpointsReached >= board.getTotalCheckpoints()) {
                winner = player;
                break;
            }
        }

        if (winner != null) {
            appLogger.info("Game ended. Player {} has won by reaching all checkpoints.", winner.getName());

            // TODO @lukas：Broadcast game end message
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyGameFinished(winner.getRobot().getRobotID())
                    )
            );

            // TODO @lukas：Send final chat message
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyReceivedChat(
                                    "Game over! Player " + winner.getName() +
                                            " has won by reaching all checkpoints!",
                                    0, false
                            )
                    )
            );
            return true;
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
        Server.getInstance().broadcastMessage(new MessageDefinitions.Message<>(
                new MessageDefinitions.BodyActivePhase(currentPhase.getValue())));
    }

    /**
     * Handle the setup phase
     */
    private void handleSetupPhase() {
        // Wait for all players to each choose starting positions
        // This logic is triggered by the client, server responds
        appLogger.info("Processing setup phase, waiting for all players to select starting positions");
        for (Player player : new ArrayList<>(currentPlayerTurn)) {
            ClientHandler currentPlayerConnection = player.getConnection();
            currentPlayerConnection.broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyCurrentPlayer(currentPlayerConnection.getMyID())
            ));

            final CountDownLatch latch = new CountDownLatch(1);
            currentPlayerConnection.setPlacementLatch(latch);

            try {
                latch.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                appLogger.error("Interrupted while waiting for placements: {}", e.getMessage());
            }
            currentPlayerTurn.remove(player);
        }
//        while (!currentPlayerTurn.isEmpty()) {
//            ClientHandler currentPlayerConnection = currentPlayerTurn.pop().getConnection();
//
//            final CountDownLatch latch = new CountDownLatch(1);  // One latch per turn
//            currentPlayerConnection.setPlacementLatch(latch);
//
//            // Notify all clients who is placing TODO @Lukas broadcast CurrentPlayer
//            currentPlayerConnection.broadcastMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyCurrentPlayer(currentPlayerConnection.getMyID())
//            ));
//
//            try {
//                latch.await();
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                System.err.println("Interrupted while waiting for placements.");
//            }
//        }
        appLogger.info("All players have chosen their starting positions. Enter the programming phase.");
    }

    /**
     * Handle the programming phase
     */
    private void handleProgrammingPhase() {
        appLogger.info("During the dealing phase, begin dealing cards to all players.");
        // Deal cards to all players
        for (Player player : players) {
            // Deal new cards and send to server
            appLogger.info("Deal cards to players {}", player.getClientID());
            player.dealProgrammingCards();
        }

        appLogger.info("All players have been dealt their cards and are waiting for players to select cards.");
        // TODO: The actual card selection is handled by client events through the Player.chooseCard method
        // Server will wait for all players to finish programming
    }

    /**
     * Handle the activation phase using per-register logic and correct player order updates.
     */
    private void handleActivationPhase() {
        while (!checkGameEnd() && currentRegister < 5) {
            handleCurrentRegister();

            // Advance to next register or round
            if (!checkAndAdvanceActivationPhase()) {
                break;
            }
        }
    }

    /**
     * Executes one register's worth of actions in the activation phase:
     * - broadcasts active cards
     * - executes cards in current register
     * - activates board elements and lasers
     */
    private void handleCurrentRegister() {
        appLogger.info("Activating register {}", currentRegister + 1);

        // Collect active cards to broadcast
        List<MessageDefinitions.ActiveCard> activeCards = new ArrayList<>();
        for (Player player : currentPlayerTurn) {
            RegisterCard card = player.getRegisterCard(currentRegister);
            if (card != null) {
                String cardName = CardFactory.getCardName(card);
                activeCards.add(new MessageDefinitions.ActiveCard(player.getRobot().getClientID(), cardName));
            }
        }

        // Broadcast card information
        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(new MessageDefinitions.BodyCurrentCards(activeCards))
        );

        // Execute player cards in correct order
        while (!currentPlayerTurn.isEmpty()) {
            Player player = currentPlayerTurn.pop();

            RegisterCard card = player.getRegisterCard(currentRegister);
            if (card != null) {
                appLogger.info("Player {} plays {} from register {}",
                        player.getConnection().getMyID(),
                        CardFactory.getCardName(card),
                        currentRegister);
            } else {
                appLogger.info("Player {} has no card for register {}",
                        player.getConnection().getMyID(),
                        currentRegister);
            }

            activatePlayerCard(player, currentRegister);
        }

        // Activate board elements (e.g., belts, gears)
        activateBoardElements();

        // Fire robot lasers
        handleRobotLasers();

        // Check if game ends during this register
        if (checkGameEnd()) {
            appLogger.info("Game ended during activation of register {}", currentRegister + 1);
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
        startPoint.occupy(robot.getRobotID());

        // Set robot position and orientation
//        robot.setPosition(targetPos);
        Board tempBoard = robot.getBoard();
        robot.setBoard(null);
        robot.setPosition(targetPos);
        robot.setBoard(tempBoard);
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
                }, player.getConnection().getMyID())
        ));

        return true;
    }

    /**
     * Determine player order based on priority
     */
    private void determinePlayerOrder() {
        currentPlayerTurn.clear();

        switch (currentPhase) {
            case SETUP -> {
                List<Player> setupOrder = Server.getInstance()
                        .getSnapshotReadyOrder().stream()
                        .map(ClientHandler::getPlayer)
                        .toList();

                // Push in order: first ready client should act first
                for (Player player : setupOrder) {
                    currentPlayerTurn.push(player);
                }

                appLogger.info("SETUP player order: {}", setupOrder.stream().map(p -> "Player " + p.getConnection().getMyID()).toList());
            }
            case ACTIVATION -> {
                List<Player> sortedPlayers = new ArrayList<>(players);
                Position antennaPosition = board.getAntennaPosition();

                if (antennaPosition == null) {
                    errorLogger.error("No antenna position found. currentPlayerTurn unmodified.");
                    return;
                }

                // Sort players by distance to antenna
                sortedPlayers.sort(Comparator.comparingInt(p -> {
                    Position robotPos = p.getRobot().getPosition();
                    return (robotPos != null) ? robotPos.distanceTo(antennaPosition) : Integer.MAX_VALUE;
                }));

                for (int i = sortedPlayers.size() - 1; i >= 0; i--) {
                    currentPlayerTurn.push(sortedPlayers.get(i));
                }

                appLogger.info("Current player order set: {}", sortedPlayers.stream().map(Player::toString).toList());
            }
            default ->
                    throw new IllegalAccessError("determinePlayerOrder should not be called in phase: " + currentPhase);
        }
    }

    // 5. Card and Action Methods

    /**
     * Activate a player's card in the specified register
     */
    private void activatePlayerCard(Player player, int register) {
        List<RegisterCard> playerRegister = player.getRegister();

        if (register < playerRegister.size() && playerRegister.get(register) != null) {
            RegisterCard card = playerRegister.get(register);

            String cardName = CardFactory.getCardName(card);
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyCardPlayed(player.getClientID(), cardName)
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
                                    player.getClientID(),
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

    /**
     * Resets all the players for the round.
     */
    private void resetPlayersRound() {
        appLogger.info("Resetting round for all players.");
        for (Player player : players)
            player.resetRound();
    }

    /**
     * Resets all the players hand.
     **/
    private void resetPlayersHand() {
        for (Player player : players)
            player.resetHand();
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
                                                        robot.getRobotID(), rotation
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
                                                new MessageDefinitions.BodyDrawDamage(robot.getRobotID(), damageCards)
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
                                                            robot.getRobotID(),
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
                            int oldCheckpoints = checkpoint.getRobotHighestCheckpoint(robot.getRobotID());
                            checkpoint.applyEffect(robot, board);
                            int newCheckpoints = checkpoint.getRobotHighestCheckpoint(robot.getRobotID());

                            // TODO @lukas：If new checkpoint reached, broadcast update
                            if (newCheckpoints > oldCheckpoints) {
                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyCheckPointReached(
                                                        robot.getRobotID(), newCheckpoints
                                                )
                                        )
                                );

                                // TODO @lukas：Animation
                                Server.getInstance().broadcastMessage(
                                        new MessageDefinitions.Message<>(
                                                new MessageDefinitions.BodyAnimation("CheckPoint")
                                        )
                                );

                                if (newCheckpoints >= board.getTotalCheckpoints()) {
                                    checkGameEnd();
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
        Player player = getPlayerForRobot(robot);
        if (player == null) return;

        // Broadcast reboot message
        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyReboot(robot.getRobotID())
                )
        );

        // Robot takes damage and cancels programming
        dealSpamDamage(robot);
        dealSpamDamage(robot);
        robot.cancelProgramming();

        // Move to reboot position
        Position rebootPos = board.getRebootPosition();
        robot.setPosition(rebootPos);
        board.updateRobotPosition(robot, rebootPos);

        // Broadcast movement to reboot position
        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyMovement(
                                robot.getRobotID(),
                                rebootPos.x(),
                                rebootPos.y()
                        )
                )
        );

        // Wait for player to choose direction
        // TODO:this would be async with client response
        Direction defaultDirection = Direction.NORTH;
        if (player.getConnection() != null) {
            player.getConnection().sendMessage(
                    new MessageDefinitions.Message<>(
                            // The special value “select” indicates that a selection is required.
                            new MessageDefinitions.BodyRebootDirection("select")
                    )
            );
        } else {
            // If there is no connection (such as an AI player), use the default direction.
            robot.setDirection(defaultDirection);
            Server.getInstance().broadcastMessage(
                    new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyRebootDirection("top")  // Default direction
                    )
            );
        }
    }

    /**
     * Inflicts damage on a robot and broadcasts damage information.
     * When the specific damage card type is depleted, allows player to choose alternative damage types.
     *
     * @param robot        The robot that has been damaged.
     * @param damageAmount The amount of damage to apply.
     * @param damageType   The type of damage ("Spam", "Virus", "Worm", "Trojan").
     */
    public void applyAndBroadcastDamage(Robot robot, int damageAmount, String damageType) {
        if (damageAmount <= 0) return;
        List<String> damageCards = new ArrayList<>();
        DamageCard.DamageType type = DamageCard.DamageType.valueOf(damageType.toUpperCase());

        // Check if there are enough damage cards of the specified type in the pool
        DamageCardPool damagePool = DamageCardPool.getInstance();
        int availableCount = damagePool.getAvailableCount(type);

        // If there aren't enough damage cards of the specified type
        // If there are other types of damage cards available, let the player choose
        if (availableCount < damageAmount) {
            List<String> availablePiles = new ArrayList<>();
            for (DamageCard.DamageType damageCardType : DamageCard.DamageType.values()) {
                if (damagePool.hasAvailable(damageCardType)) {
                    availablePiles.add(damageCardType.name());
                }
            }

            if (!availablePiles.isEmpty()) {
                // Find the corresponding player
                Player player = getPlayerForRobot(robot);
                if (player != null) {
                    // Request the player to select a damage card type
                    player.getConnection().sendMessage(
                            new MessageDefinitions.Message<>(
                                    new MessageDefinitions.BodyPickDamage(
                                            damageAmount, availablePiles
                                    )
                            )
                    );
                    return;
                }
            }

            // If no damage cards are available or player can't be found, don't apply damage
            appLogger.warn("No damage cards available for robot {}", robot.getRobotID());
            return;
        }

        // If there are enough damage cards of the specified type, apply and broadcast
        for (int i = 0; i < damageAmount; i++) {
            damageCards.add(damageType);
            robot.addDamageCard(type);
        }

        Server.getInstance().broadcastMessage(
                new MessageDefinitions.Message<>(
                        new MessageDefinitions.BodyDrawDamage(robot.getRobotID(), damageCards)
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

}

