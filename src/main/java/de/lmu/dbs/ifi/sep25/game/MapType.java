package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.*;

import java.util.ArrayList;
import java.util.List;

public enum MapType {
    MAP1, MAP2, MAP3, MAP4, MAP5
}

    // Initializes the board with the specified map's layout
    public void loadMap(Board board) {
        // Clear current board
        List<BoardElement>[][] grid = board.getGrid();
        int width = board.getWidth();
        int height = board.getHeight();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new ArrayList<>();
                grid[x][y].add(Floor.getInstance());
            }
        }

        switch (this) {
            case MAP1:
                // Initialize MAP1 layout (to be implemented in a separate class)
                // Example: grid[5][5].add(new Gear(new Position(5, 5), Gear.RotationDirection.CLOCKWISE));
            case MAP1:
                Map1.initialize(board);

                break;
            case MAP2:
                // Initialize MAP2 layout (to be implemented in a separate class)
                break;
            case MAP3:
                // Initialize MAP3 layout
                break;
            case MAP4:
                // Initialize MAP4 layout
                break;
            case MAP5:
                // Initialize MAP5 layout
                break;
        }
        // TODO: Delegate to separate map classes, e.g., Map1.java, Map2.java
    }

    // Sets starting positions for robots based on the map type
    public Position[] getStartPositions() {
        switch (this) {
            case MAP1:
                // MAP1 starting positions
                return new Position[] {
                        new Position(0, 0), new Position(0, 1), new Position(1, 0),
                        new Position(1, 1), new Position(0, 2)
                };
            case MAP2:
                // MAP2 starting positions
                return new Position[] {
                        new Position(2, 2), new Position(2, 3), new Position(3, 2),
                        new Position(3, 3), new Position(2, 4)
                };
            case MAP3:
                // MAP3 starting positions
                return new Position[] {
                        new Position(4, 4), new Position(4, 5), new Position(5, 4),
                        new Position(5, 5), new Position(4, 6)
                };
            case MAP4:
                // MAP4 starting positions
                return new Position[] {
                        new Position(6, 6), new Position(6, 7), new Position(7, 6),
                        new Position(7, 7), new Position(6, 8)
                };
            case MAP5:
                // MAP5 starting positions
                return new Position[] {
                        new Position(8, 8), new Position(8, 9), new Position(9, 8),
                        new Position(9, 9), new Position(8, 10)
                };
        }
        // Fallback (should never reach here)
        return new Position[] { new Position(0, 0) };
    }
}