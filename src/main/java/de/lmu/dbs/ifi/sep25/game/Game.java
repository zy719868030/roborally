package de.lmu.dbs.ifi.sep25.game;
import de.lmu.dbs.ifi.sep25.card.Card;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
import de.lmu.dbs.ifi.sep25.card.RegisterCard;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private static Game instance;

    private Player currentPlayer;
    private List<Player> players;
    private Board board;
    private final Deck<UpgradeCard> upgradeCards = new Deck<>();

    private Game(String mapName) {
        players = new ArrayList<>();
        Board.MapType mapType = parseMapName(mapName);
        board = new Board(12, 12); //FIXME @prajal
        currentPlayer = null;
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

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void determineTurn() {
        // Stub
    }

    // TODO game.initializeGame(); (in Server)
    //Set Board references for all robots when initializing the game
    public void initializeGame() {
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

    public void addPlayer(Player player) {
        players.add(player);
        Position pos = player.getRobot().getPosition();
        board.placeRobot(player.getRobot(), pos.x(), pos.y());
    }

    public Board getBoard() {
        return board;
    }
}

/*
public class Game {
    private Player currentPlayer;
    private List<Player> players;
    private List<Card> discardPile;
    private final Board board;

    public Game() {
        players = new ArrayList<>();
        discardPile = new ArrayList<>();
        board = new Board(12, 12); // 12x12 board
        currentPlayer = null;
    }

    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    public void determineTurn() {
        // Stub: Select next player for turn
    }

    public void playRound() {
        // Stub: Execute 5 phases of card actions
        for (Player player : players) {
            List<Card> register = player.getRegister();
            for (int phase = 0; phase < 5; phase++) {
                if (phase < register.size() && register.get(phase) != null) {
                    register.get(phase).execute(player.getRobot());
                }
            }
        }
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public Board getBoard() {
        return board;
    }
}


 */