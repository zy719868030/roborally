package de.lmu.dbs.ifi.sep25.game;

public class Board {
    private Tile[][] tiles;
    private int width;
    private int height;

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile(x, y); // Creates Tile with default Floor
            }
        }
    }

    public Tile getTile(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return tiles[x][y];
        }
        return null;
    }

    public void placeRobot(Robot robot, int x, int y) {
        Tile tile = getTile(x, y);
        if (tile != null) {
            tile.setRobot(robot);
        }
    }
}