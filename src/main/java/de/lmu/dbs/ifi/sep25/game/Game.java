package de.lmu.dbs.ifi.sep25.game;

import java.util.ArrayList;
import java.util.List;

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
