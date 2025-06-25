package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.Card;
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

import java.util.*;
import java.util.stream.Collectors;

public class Game {
    private static Game instance;
    private Player currentPlayer;
    private final List<Player> players;
    private Board board;
    private final DamageCardPool damageDeck = DamageCardPool.getInstance();
    private final Deck<UpgradeCard> upgradeCards = new Deck<>();
    private final Deck<ProgrammingCard> programmingDeck = new Deck<>();

    // Map selection fields
//    private Player firstReadyPlayer;
    private String selectedMap;
//    private boolean mapSelectionPending;
    private GamePhase currentPhase = null;
    private int currentRegister = 0;
    private int roundNumber = 0;
    private int currentPlayerIndex; // Index in players list
//    private Timer programmingTimer; // For 30-second timer
//    private List<Integer> slowPlayers; // Track slow players

    private Game(String mapName) {
        players = new ArrayList<>();
        board = new Board(MapType.fromString(mapName));
        currentPlayer = null;
//        currentPlayerIndex = 0;
        //currentPhase = -1; // Pre-game (map selection)
        // Initialize map selection state
//        firstReadyPlayer = null;
        selectedMap = mapName;
//        mapSelectionPending = true;
//        slowPlayers = new ArrayList<>();
        initializeProgrammingDeck();
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


    public void setCurrentPlayer() {
        if (players.isEmpty()) return;
        // Use antenna-based priority for Setup Phase (Phase 0)
        if (currentPhase == GamePhase.SETUP) {
            Position antennaPos = board.getAntennaPosition();
            Antenna antenna = null;
            for (BoardElement element : board.getElements(antennaPos.x(), antennaPos.y())) {
                if (element instanceof Antenna) {
                    antenna = (Antenna) element;
                    break;
                }
            }

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
        // TODO @lukas Send CurrentPlayer message
        for (Player p : players) {
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyCurrentPlayer(currentPlayer.getRobot().getId())
            ));
        }
    }



    public void determineTurn() {
        // Stub
        setCurrentPlayer();
    }

    // TODO game.initializeGame(); (in Server)
    //Set Board references for all robots when initializing the game
    public void initializeGame() {
        if (board == null) {
            board = new Board(MapType.fromString(selectedMap));
        }

        for (Player player : players) {
            Robot robot = player.getRobot();
            robot.setBoard(board);
        }
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

    /**
     * Adds a player to the current game.
     *
     * @param player The Player instance to be added to the game.
     */
    public void addPlayer(Player player) {
        players.add(player);
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
        return selectedMap;
    }

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
     * Handle the setup phase
     */
    private void handleSetupPhase() {
        // Players choose starting positions
         broadcastCurrentPhase();

        // Wait for all players to choose starting positions
        // This logic is triggered by the client, server responds
    }

    /**
     * Handle the programming phase
     */
    private void handleProgrammingPhase() {
        broadcastCurrentPhase();

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
        broadcastCurrentPhase();

        // Activate all five registers
        for (currentRegister = 0; currentRegister < 5; currentRegister++) {
            System.out.println("Activating register " + (currentRegister + 1));

            // Sort players by priority
            List<Player> sortedPlayers = determinePlayerOrder();

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
     * Set the current game phase and broadcast to all clients
     */
    public void setPhase(GamePhase phase) {
        this.currentPhase = phase;
        broadcastCurrentPhase();

        // Trigger the appropriate processing as needed.
        switch(phase) {
            case SETUP: handleSetupPhase(); break;
            case PROGRAMMING: handleProgrammingPhase(); break;
            case ACTIVATION: handleActivationPhase(); break;
        }
    }

    /**
     * Broadcast the current game phase to all clients
     */
    // TODO @lukas:Check whether to keep it here or write it to the server/client.
    //  If you want to modify it, please also modify the part where I call this method, as well as four other places.
    private void broadcastCurrentPhase() {
        MessageDefinitions.Message<MessageDefinitions.BodyActivePhase> message =
                new MessageDefinitions.Message<>(new MessageDefinitions.BodyActivePhase(currentPhase.getValue()));
        Server.getInstance().broadcastMessage(message);
    }

    /**
     * Determine player order based on priority
     */
    private List<Player> determinePlayerOrder() {
        List<Player> sortedPlayers = new ArrayList<>(players);

        // Determine priority based on the antenna on the board
        Position antennaPosition = board.getAntennaPosition();

        // First sort by distance to the antenna
        sortedPlayers.sort(Comparator.comparingInt(p ->
                p.getRobot().getPosition().distanceTo(antennaPosition)));

        // If players have the same distance, handle according to rules
        // TODO:Simplified here, actual implementation should be more complex


        // TODO @lukas:Broadcast current player order
        for (Player player : sortedPlayers) {
            MessageDefinitions.Message<MessageDefinitions.BodyCurrentPlayer> message = new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyCurrentPlayer(player.getRobot().getId()));
            Server.getInstance().broadcastMessage(message);
        }

        return sortedPlayers;
    }

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

    // Method to handle DamageCard effects
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
//                                Server.getInstance().broadcastMessage(
//                                        new MessageDefinitions.Message<>(
//                                                MessageDefinitions.BodyDrawDamage(robot.getId(), damageCards)
//                                        )
//                                );
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

    private void handleRobotLasers() {
        for (Player player : players) {
            Robot robot = player.getRobot();

            // Skip if robot is powered down or has fallen off
            if (robot.isPoweredDown() || board.hasRobotFallen(robot)) {
                continue;
            }

            Position position = robot.getPosition();
            Direction direction = robot.getDirection();

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
     * Find the player who owns the given robot
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

    /**
     * Handle the initial placement phase, allowing players to select their starting positions in order of connection
     */
//    public void initPlacement() {
//        Server server = Server.getInstance();
//        List<ClientHandler> placementOrder = server.getLobby().getClients();
//
//        for (ClientHandler handler : placementOrder) {
//            int clientID = handler.getMyID();
//            // Send a message to let players choose their starting position.
//            handler.sendMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyCurrentPlayer(clientID)
//            ));
//            // Wait for the player's response TODO (this requires asynchronous processing)
//        }
//    }


    /**
     * Start the game main loop
     */
//    public void startGameLoop() {
//        // Setup phase
//        setPhase(GamePhase.SETUP);
//        handleSetupPhase();
//
//        // Game continues until a player wins
//        while (true) {
//            // Start a new round
//            roundNumber++;
//            System.out.println("Starting round " + roundNumber);
//
//            // Programming phase
//            setPhase(GamePhase.PROGRAMMING);
//            handleProgrammingPhase();
//
//            // Activation phase
//            setPhase(GamePhase.ACTIVATION);
//            handleActivationPhase();
//
//            // Check if the game has ended
//            if (checkGameEnd()) {
//                break;
//            }
//        }
//    }

    /**
     * Start the programming phase for all players
     */
//    public void startProgrammingPhase() {
//        setPhase(GamePhase.PROGRAMMING);
//
//        // Deal cards to all players
//        for (Player player : players) {
//            // Clear previous programming
//            player.endRound();
//
//            // Deal new cards
//            player.dealProgrammingCards();
//
//            // Send cards to player
//            List<String> cardNames = new ArrayList<>();
//            for (RegisterCard card : player.getHand()) {
//                cardNames.add(CardFactory.getCardName(card));
//            }
//
//            player.getConnection().sendMessage(
//                    new MessageDefinitions.Message<>(
//                            new MessageDefinitions.BodyYourCards(cardNames)
//                    )
//            );
//
//            // Notify others
//            int cardCount = player.getHand().size();
//            for (Player otherPlayer : players) {
//                if (otherPlayer != player) {
//                    otherPlayer.getConnection().sendMessage(
//                            new MessageDefinitions.Message<>(
//                                    new MessageDefinitions.BodyNotYourCards(
//                                            player.getRobot().getId(), cardCount
//                                    )
//                            )
//                    );
//                }
//            }
//        }
//    }

}

