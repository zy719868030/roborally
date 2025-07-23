package de.lmu.dbs.ifi.sep25.ui.bot.pathfinding;

import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single tile (node) in the board pathfinding graph.
 */
public class Node {
    /** X (column) coordinate of this tile. */
    public final int x;
    /** Y (row) coordinate of this tile. */
    public final int y;
    /** Convenient Position object for this node. */
    public final Position pos;
    /** All legal moves from this node. */
    public final List<Edge> edges = new ArrayList<>();

    /** True if this tile is a pit (robot will die on entry). */
    public boolean isPit = false;
    /** True if this tile contains a conveyor belt. */
    public boolean isConveyor = false;
    /** True if this tile contains a push panel. */
    public boolean isPushPanel = false;
    /** True if this tile contains a laser hazard. */
    public boolean isLaser = false;
    /** True if this tile contains any wall (directional info is in edges). */
    public boolean isWall = false;

    /**
     * Constructs a node for the given board coordinates.
     *
     * @param x The x coordinate (column).
     * @param y The y coordinate (row).
     */
    public Node(int x, int y) {
        this.x = x;
        this.y = y;
        this.pos = new Position(x, y);
    }
}