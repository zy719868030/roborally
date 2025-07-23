package de.lmu.dbs.ifi.sep25.ui.bot.pathfinding;

import de.lmu.dbs.ifi.sep25.game.Direction;

/**
 * Represents a legal movement from one node to another.
 */
public class Edge {
    /** The target node (tile) this edge leads to. */
    public final Node target;
    /** The direction of movement for this edge (NORTH, EAST, SOUTH, WEST). */
    public final Direction direction;
    /** The movement cost or weight for this edge (1 = normal, >1 = hazardous/risky). */
    public final int weight;

    /**
     * Constructs an edge from the current node to a target node.
     *
     * @param target    The node this edge leads to.
     * @param direction The direction of movement.
     * @param weight    The movement cost or risk (higher = less desirable).
     */
    public Edge(Node target, Direction direction, int weight) {
        this.target = target;
        this.direction = direction;
        this.weight = weight;
    }
}