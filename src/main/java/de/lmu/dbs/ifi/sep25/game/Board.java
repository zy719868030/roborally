package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.tile.TileElement;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private List<TileElement>[][] elements;
    private final int width;
    private final int height;

    @SuppressWarnings("unchecked")
    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        this.elements = new ArrayList[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                elements[x][y] = new ArrayList<>();
                elements[x][y].add(de.lmu.dbs.ifi.sep25.game.tile.Floor.getInstance());
            }
        }
    }

    public List<TileElement> getElements(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return elements[x][y];
        }
        return new ArrayList<>();
    }

    public void placeRobot(Robot robot, int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            robot.setPosition(x, y);
        }
    }

    public void applyEffects(Robot robot, int x, int y) {
        List<TileElement> elementsAt = getElements(x, y);
        for (TileElement element : elementsAt) {
            element.applyEffect(robot, this);
        }
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}


/*
@SuppressWarnings({"unused"})
public class Board {
    private final Tile[][] tiles;
    private final int width;
    private final int height;

    public Board(int width, int height) {
        this.width = width;
        this.height = height;
        this.tiles = new Tile[width][height];
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                tiles[x][y] = new Tile();
            }
        }
    }

    public Tile getTile(Position position) {
        return tiles[position.x()][position.y()];
    }

    public void addTileElement(TileElement element, Position position) {
        tiles[position.x()][position.y()].addElement(element);
    }

    public void removeTileElement(TileElement element, Position position) {
        tiles[position.x()][position.y()].removeElement(element);
    }

    public boolean isInBounds(Position pos) {
        return pos.x() >= 0 && pos.x() < width && pos.y() >= 0 && pos.y() < height;
    }

    //obsolete method, since xy position should be stored in robot, not in tiles
//    public void placeRobot(Robot robot, int x, int y) {
//        Tile tile = getTile(x, y);
//        if (tile != null) {
//            tile.setRobot(robot);
//        }
//    }
}

*/
