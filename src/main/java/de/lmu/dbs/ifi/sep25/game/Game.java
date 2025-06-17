package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import java.util.Map;

import java.util.ArrayList;
import java.util.List;

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

    private Game(String mapName) {
        players = new ArrayList<>();
        mapType = parseMapName(mapName);
//      board = new Board(12, 12); FIXME @prajal
        currentPlayer = null;
        // Added: Initialize map selection state
        firstReadyPlayer = null;
        selectedMap = null;
        mapSelectionPending = true;
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
            case "dizzy highway":
            default:
                return Board.MapType.DEFAULT;
        }
    }

    // Handle player readiness and map selection
    public void setPlayerReady(Player player, boolean ready) {
        player.setReady(ready);
        // Added: Broadcast PlayerStatus
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
        // Added: Validate player and map
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

        // Added: Set map and initialize board
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
            List<List<List<BoardElement>>> gameMap = convertToBoardElementMap();
            p.getConnection().sendMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyGameStarted(5, gameMap)
            ));
        }
    }

    // Convert map to List<Object> for BodyGameStarted
    private List<Object> convertToObjectList(List<List<List<Map<String, Object>>>> map) {
        List<Object> result = new ArrayList<>();
        for (List<List<Map<String, Object>>> col : map) {
            List<Object> colList = new ArrayList<>();
            for (List<Map<String, Object>> row : col) {
                colList.add(row == null ? null : new ArrayList<>(row));
            }
            result.add(colList);
        }
        return result;
    }

    // Helper method to convert serialized map to List<List<List<BoardElement>>> if needed
    private List<List<List<BoardElement>>> convertToBoardElementMap() {
        List<List<List<BoardElement>>> result = new ArrayList<>();
        List<BoardElement>[][] grid = board.getGrid(); // Assuming getGrid returns BoardElement[][][]
        for (int x = 0; x < board.getWidth(); x++) {
            List<List<BoardElement>> col = new ArrayList<>();
            for (int y = 0; y < board.getHeight(); y++) {
                List<BoardElement> elements = new ArrayList<>(grid[x][y]);
                col.add(elements);
            }
            result.add(col);
        }
        return result;
    }

    // Added: Stub for AI-only map selection
    public void selectMapForAI() {
        if (mapSelectionPending) {
            selectMap(null, "Dizzy Highway");
        }
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void determineTurn() {
        // Stub
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
                List<List<List<BoardElement>>> gameMap = convertToBoardElementMap();
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




}

