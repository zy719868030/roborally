package de.lmu.dbs.ifi.sep25.ui.bot.pathfinding;

import de.lmu.dbs.ifi.sep25.game.Direction;
import de.lmu.dbs.ifi.sep25.game.Position;

import java.util.*;

public class Pathfinder {

    /**
     * Finds the lowest-weight path from start to goal using Dijkstra's algorithm.
     *
     * @param graph     The BoardGraph (with weighted edges)
     * @param start     Starting position
     * @param goal      Goal position
     * @return List of Directions for the optimal path, or empty if unreachable
     */
    public static List<Direction> dijkstra(BoardGraph graph, Position start, Position goal) {
        Node startNode = graph.getNode(start.x(), start.y());
        Node goalNode = graph.getNode(goal.x(), goal.y());
        if (startNode == null || goalNode == null) return List.of();

        Map<Node, Integer> dist = new HashMap<>();
        Map<Node, Node> parent = new HashMap<>();
        Map<Node, Direction> moveDir = new HashMap<>();
        PriorityQueue<Node> queue = new PriorityQueue<>(Comparator.comparingInt(n -> dist.getOrDefault(n, Integer.MAX_VALUE)));
        Set<Node> visited = new HashSet<>();

        dist.put(startNode, 0);
        queue.add(startNode);

        while (!queue.isEmpty()) {
            Node curr = queue.poll();
            if (!visited.add(curr)) continue;
            if (curr.equals(goalNode)) break;
            for (Edge edge : curr.edges) {
                Node next = edge.target;
                int alt = dist.get(curr) + edge.weight;
                if (alt < dist.getOrDefault(next, Integer.MAX_VALUE)) {
                    dist.put(next, alt);
                    parent.put(next, curr);
                    moveDir.put(next, edge.direction);
                    queue.add(next);
                }
            }
        }

        // Reconstruct path
        LinkedList<Direction> path = new LinkedList<>();
        Node curr = goalNode;
        while (parent.containsKey(curr) && curr != startNode) {
            path.addFirst(moveDir.get(curr));
            curr = parent.get(curr);
        }
        return path;
    }
}