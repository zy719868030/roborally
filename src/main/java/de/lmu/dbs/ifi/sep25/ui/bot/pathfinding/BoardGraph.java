package de.lmu.dbs.ifi.sep25.ui.bot.pathfinding;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;

import java.util.List;

/**
 * Represents the full RoboRally board as a directed, weighted graph for AI pathfinding.
 * <p>
 * Each tile on the board is represented as a {@link Node}, which contains field type flags
 * and a list of legal outgoing {@link Edge}s. The graph is automatically constructed from
 * the board's field data, respecting all movement-blocking features (walls, pits, out-of-bounds).
 * <p>
 * Supports access by coordinates and exposes the full node grid for traversal algorithms.
 *
 * <ul>
 *   <li>Nodes store pit, conveyor, laser, push panel, and wall information per tile</li>
 *   <li>Edges represent possible robot moves and encode hazards as weights</li>
 *   <li>Edges are only added if not blocked by walls or pits, and remain in-bounds</li>
 * </ul>
 */
public class BoardGraph {
    /** 2D array of all nodes; nodes[x][y] represents tile (x, y) on the board. */
    public final Node[][] nodes;
    private final int width, height;

    /**
     * Constructs a BoardGraph from the game's board field data.
     * Initializes all nodes and their outgoing edges based on legal movement rules.
     *
     * @param boardMap The 3D list structure from the server, where each tile contains a list of Fields.
     */
    public BoardGraph(List<List<List<Field>>> boardMap) {
        this.width = boardMap.size();
        this.height = boardMap.getFirst().size();
        nodes = new Node[width][height];

        // Build nodes
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                nodes[x][y] = new Node(x, y);

                for (Field f : boardMap.get(x).get(y)) {
                    if (f instanceof FieldPit) nodes[x][y].isPit = true;
                    if (f instanceof FieldConveyorBelt) nodes[x][y].isConveyor = true;
                    if (f instanceof FieldPushPanel) nodes[x][y].isPushPanel = true;
                    if (f instanceof FieldLaser) nodes[x][y].isLaser = true;
                    if (f instanceof FieldWall) nodes[x][y].isWall = true;
                }
            }
        }

        // Build edges: 4 cardinal directions
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                Node node = nodes[x][y];
                for (Direction dir : Direction.values()) {
                    int nx = x + dir.getDx();
                    int ny = y + dir.getDy();
                    if (isInBounds(nx, ny)) {
                        // Check for wall between node and neighbor
                        boolean wallBlocks = wallBlocks(boardMap, x, y, nx, ny, dir);
                        boolean pitBlocks = nodes[nx][ny].isPit;

                        if (!wallBlocks && !pitBlocks) {
                            int weight = 1;
                            if (nodes[nx][ny].isLaser) weight += 5; // Example: Avoid lasers if possible
                            node.edges.add(new Edge(nodes[nx][ny], dir, weight));
                        }
                    }
                }
            }
        }
    }

    /**
     * Checks if the given coordinates are within the bounds of the board.
     *
     * @param x The x coordinate (column).
     * @param y The y coordinate (row).
     * @return true if (x, y) is a legal tile on the board, false otherwise.
     */
    private boolean isInBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Returns true if movement between (x, y) and (nx, ny) in the specified direction is blocked by any wall.
     *
     * @param boardMap The field map.
     * @param x        The starting x coordinate.
     * @param y        The starting y coordinate.
     * @param nx       The target x coordinate.
     * @param ny       The target y coordinate.
     * @param dir      The direction of movement.
     * @return true if movement is blocked by a wall, false otherwise.
     */
    private boolean wallBlocks(List<List<List<Field>>> boardMap, int x, int y, int nx, int ny, Direction dir) {
        // 1. Wall on FROM tile in "dir"
        for (Field f : boardMap.get(x).get(y)) {
            if (f instanceof FieldWall wall && wall.orientations().contains(dir.toString().toLowerCase())) {
                return true;
            }
        }
        // 2. Wall on TO tile in opposite direction
        for (Field f : boardMap.get(nx).get(ny)) {
            if (f instanceof FieldWall wall && wall.orientations().contains(dir.turnAround().toString().toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Retrieves the node at the given board coordinates, or null if out of bounds.
     *
     * @param x The x coordinate.
     * @param y The y coordinate.
     * @return The Node at (x, y), or null if invalid.
     */
    public Node getNode(int x, int y) {
        return isInBounds(x, y) ? nodes[x][y] : null;
    }
}