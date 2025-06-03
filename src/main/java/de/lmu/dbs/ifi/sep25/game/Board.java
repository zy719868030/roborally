package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Floor;

import java.util.ArrayList;
import java.util.List;

public class Board {
    private List<BoardElement>[][] elements;
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
                elements[x][y].add(Floor.getInstance());
            }
        }
    }

    public List<BoardElement> getElements(int x, int y) {
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
        List<BoardElement> elementsAt = getElements(x, y);
        for (BoardElement element : elementsAt) {
            element.applyEffect(robot, this);
        }
    }

    /**
     * Gets the position of the reboot token on the board.
     * This is where robots will respawn after falling off the board or into a pit.
     *
     * @return the Position of the reboot token, or null if no reboot token exists on the board
     */
    public Position getRebootPosition() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                List<BoardElement> elements = getElements(x, y);
                for (BoardElement element : elements) {
                    if (element.getType().equals("RestartPoint") || element.getType().equals("RebootPoint")) {
                        return new Position(x, y);
                    }
                }
            }
        }
        return null;
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
