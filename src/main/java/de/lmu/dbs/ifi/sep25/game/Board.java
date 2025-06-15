package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Board {
    private final List<BoardElement>[][] grid;
    private final int width;
    private final int height;
    private final Map<Position, Robot> robotPositions = new HashMap<>();
    private final Map<Robot, Position> robotToPosition = new HashMap<>();

    // Added to store robots that fall off the board
    private final List<Robot> fallenRobots = new ArrayList<>();
    // Designated point for fallen robots (outside 12x12 grid)
    private static final Position VOID_POINT = new Position(-1, -1);


    @SuppressWarnings("unchecked")
    public Board(MapType mapType) {
        this.width = 10;
        this.height = 13;

        this.grid = new ArrayList[width][height];
        initializeBoard(mapType);
    }

    public enum MapType {
        DEFAULT, MAP1, MAP2, MAP3, MAP4, MAP5
    }

    // Initialize board with tiles based on map type
    private void initializeBoard(MapType mapType) {
        // Populate grid with Floor tiles by default
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                grid[x][y] = new ArrayList<>();
                grid[x][y].add(Floor.getInstance());
            }
        }

                // Dizzy Highway as the default map (starter course)

                // Walls
                // (7,1) top: blocks movement upward (NORTH)
                grid[7][1].add(new Wall(new Position(7, 1), Direction.NORTH));
                // (5,2) right: blocks movement to the right (EAST)
                grid[5][2].add(new Wall(new Position(5, 2), Direction.EAST));
                // (4,2) right: blocks movement to the right (EAST)
                grid[4][2].add(new Wall(new Position(4, 2), Direction.EAST));
                // (2,1) bottom: blocks movement downward (SOUTH)
                grid[2][1].add(new Wall(new Position(2, 1), Direction.SOUTH));

                // Todo: Antenna at (5,0) facing right (toward (5,1))

                // Conveyor Belts (double arrows = FAST/blue, single arrow at curve = rotating)
                // 1. (9,4) to (1,4), curves at (1,4) toward (1,5)
                for (int x = 9; x >= 2; x--) {
                    grid[x][4].add(new Belts(new Position(x, 4), Direction.NORTH, Belts.BeltSpeed.FAST));
                }
                List<Direction> outDirs1 = new ArrayList<>();
                outDirs1.add(Direction.EAST);
                List<Direction> inDirs1 = new ArrayList<>();
                inDirs1.add(Direction.SOUTH);
                grid[1][4].set(1, new Belts(new Position(1, 4), outDirs1, inDirs1, Belts.BeltSpeed.FAST));

                // Belt 2: (9,5) to (8,5), then curve at (8,5) toward (8,4)
                // Belt from (9,5) to (8,5) — straight upward
                grid[9][5].add(new Belts(new Position(9, 5), Direction.NORTH, Belts.BeltSpeed.FAST));

                // Curve at (8,5): south → west (⤶)
                List<Direction> outDirs2a = new ArrayList<>();
                outDirs2a.add(Direction.WEST);
                List<Direction> inDirs2a = new ArrayList<>();
                inDirs2a.add(Direction.SOUTH);
                grid[8][5].set(1, new Belts(new Position(8, 5), outDirs2a, inDirs2a, Belts.BeltSpeed.FAST));

                // Curve at (8,4): east → north (⬐)
                List<Direction> outDirs2b = new ArrayList<>();
                outDirs2b.add(Direction.NORTH);
                List<Direction> inDirs2b = new ArrayList<>();
                inDirs2b.add(Direction.EAST);
                grid[8][4].set(1, new Belts(new Position(8, 4), outDirs2b, inDirs2b, Belts.BeltSpeed.FAST));

                // 3. (2,3) to (2,4), curves at (2,4) toward (1,4)
                grid[2][3].add(new Belts(new Position(2, 3), Direction.EAST, Belts.BeltSpeed.FAST));
                List<Direction> outDirs3 = new ArrayList<>();
                outDirs3.add(Direction.NORTH);
                List<Direction> inDirs3 = new ArrayList<>();
                inDirs3.add(Direction.WEST);
                grid[2][4].set(1, new Belts(new Position(2, 4), outDirs3, inDirs3, Belts.BeltSpeed.FAST));

                // 4. (1,3) to (1,11), curves at (1,11) toward (2,11)
                for (int y = 3; y <= 10; y++) {
                    grid[1][y].add(new Belts(new Position(1, y), Direction.EAST, Belts.BeltSpeed.FAST));
                }
                List<Direction> outDirs4 = new ArrayList<>();
                outDirs4.add(Direction.SOUTH);
                List<Direction> inDirs4 = new ArrayList<>();
                inDirs4.add(Direction.WEST);
                grid[1][11].set(1, new Belts(new Position(1, 11), outDirs4, inDirs4, Belts.BeltSpeed.FAST));

                // 5. (0,10) to (1,10), curves at (1,10) toward (1,11)
                grid[0][10].add(new Belts(new Position(0, 10), Direction.SOUTH, Belts.BeltSpeed.FAST));
                List<Direction> outDirs5 = new ArrayList<>();
                outDirs5.add(Direction.EAST);
                List<Direction> inDirs5 = new ArrayList<>();
                inDirs5.add(Direction.NORTH);
                grid[1][10].set(1, new Belts(new Position(1, 10), outDirs5, inDirs5, Belts.BeltSpeed.FAST));

                // 6. (0,11) to (8,11), curves at (8,11) toward (8,10)
                for (int x = 0; x <= 7; x++) {
                    grid[x][11].add(new Belts(new Position(x, 11), Direction.NORTH, Belts.BeltSpeed.FAST));
                }
                List<Direction> outDirs6 = new ArrayList<>();
                outDirs6.add(Direction.WEST);
                List<Direction> inDirs6 = new ArrayList<>();
                inDirs6.add(Direction.SOUTH);
                grid[8][11].set(1, new Belts(new Position(8, 11), outDirs6, inDirs6, Belts.BeltSpeed.FAST));

                // 7. (7,12) to (7,11), curves at (7,11) toward (8,11)
                grid[7][12].add(new Belts(new Position(7, 12), Direction.WEST, Belts.BeltSpeed.FAST));
                List<Direction> outDirs7 = new ArrayList<>();
                outDirs7.add(Direction.NORTH);
                List<Direction> inDirs7 = new ArrayList<>();
                inDirs7.add(Direction.EAST);
                grid[7][11].set(1, new Belts(new Position(7, 11), outDirs7, inDirs7, Belts.BeltSpeed.FAST));

                // Belt 8: from (8,12) ⇦ to (8,5)
                for (int y = 12; y >= 5; y--) {
                    grid[8][y].add(new Belts(new Position(8, y), Direction.WEST, Belts.BeltSpeed.FAST));
                }

                // Energy Spaces
                Position[] energyPositions = {
                        new Position(0, 3), new Position(2, 10), new Position(9, 12),
                        new Position(7, 5), new Position(4, 7), new Position(5, 8)
                };
                for (Position pos : energyPositions) {
                    grid[pos.x()][pos.y()].add(new EnergySpace(pos));
                }

                // Checkpoint at (6,12)
                CheckPoints checkpoint = new CheckPoints(new Position(6, 12), 1);
                grid[6][12].add(checkpoint);

                // Reboot at (6,7)
                Reboot reboot = Reboot.getInstance();
                reboot.setPosition(new Position(6, 7));
                grid[6][7].add(reboot);

                // Lasers
                // 1. (6,6) top to (5,6) bottom, firing from (5,6) bottom to (6,6) top (NORTH)
                grid[6][6].add(new Laser(new Position(6, 6), Direction.NORTH, 1));
                // 2. (3,6) left to (3,7) right, firing from (3,7) right to (3,6) left (WEST)
                grid[3][6].add(new Laser(new Position(3, 6), Direction.WEST, 1));
                // 3. (3,9) bottom to (4,9) top, firing from (4,9) top to (3,9) bottom (SOUTH)
                grid[3][9].add(new Laser(new Position(3, 9), Direction.SOUTH, 1));
                // 4. (6,8) left to (6,9) right, firing from (6,8) left to (6,9) right (EAST)
                grid[6][9].add(new Laser(new Position(6, 9), Direction.EAST, 1));

    }

    // Todo: Loads a map from MapType
    /*public void loadMap(MapType mapType) {
        mapType.loadMap(this);
    }
    */

    // Sets initial robot position based on map type and player choice (0-4)
    public void setDHStartPosition(Robot robot, int playerChoice) {
        // Define 5 possible starting positions per map
        Position[] startPositions = {
                new Position(8, 1), new Position(6, 0), new Position(5, 1),
                new Position(4, 1), new Position(3, 0), new Position(1, 1)
        };

        // Validate player choice (0-4)
        int choice = Math.min(Math.max(playerChoice, 0), startPositions.length - 1);
        Position startPos = startPositions[choice];
        // If position is occupied, try next available
        int originalChoice = choice;
        while (getRobotAt(startPos) != null && choice < startPositions.length - 1) {
            choice++;
            startPos = startPositions[choice];
        }
        // If all positions are taken, use fallback
        if (getRobotAt(startPos) != null) {
            startPos = startPositions[originalChoice];
            System.out.println("Warning: Start position for Robot " + robot.getId() + " may overlap.");
        }
        robot.setPosition(startPos);
        updateRobotPosition(robot, startPos);
    }

    // Gets elements at a position (used for tile effects)
    public List<BoardElement> getElements(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[x][y];
        }
        return new ArrayList<>();
    }

    public void placeRobot(Robot robot, int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            robot.setPosition(x, y);
            updateRobotPosition(robot, robot.getPosition());
        } else {
            // Handle robots falling off the board
            handleFall(robot);
        }
    }

    public void applyEffects(Robot robot, int x, int y) {
        // Skip if robot has fallen off
        if (fallenRobots.contains(robot)) return;
        List<BoardElement> elementsAt = getElements(x, y);
        for (BoardElement element : elementsAt) {
            element.applyEffect(robot, this);
        }
    }

    /**
     * Checks whether the specified position is within the valid range of the game board.
     *
     * @param position The position to be checked.
     * @return Returns true if the position is valid, otherwise returns false.
     */
    public boolean isValidPosition(Position position) {
        int x = position.x();
        int y = position.y();
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Get the robot at the specified position.
     *
     * @param position The position to check.
     * @return If there is a robot at that position, return the robot object; otherwise, return null.
     */
    public Robot getRobotAt(Position position) {
        return robotPositions.get(position);
    }

    /**
     * Gets the position of the reboot token on the board.
     * This is where robots will respawn after falling off the board or into a pit.
     *
     * @return the Position of the reboot token, or null if no reboot token exists on the board
     */

    // [PG] Changed "RestartPoint" and "RebootPoint" to "Reboot" to match our Reboot tile
    // Changed null fallback to (0,0) to avoid NullPointerException
    public Position getRebootPosition() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (getElements(x, y).stream().anyMatch(e -> e.getType().equals("Reboot"))) {
                    return new Position(x, y);
                }
            }
        }
        return new Position(0, 0); // Fallback to (0,0) instead of null
    }

    public List<BoardElement>[][] getGrid() {
        return grid;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    /**
     * Update the robot's position on the map.
     * This method should be called when the robot moves.
     *
     * @param robot       The moving robot.
     * @param newPosition The robot's new position.
     */
    public void updateRobotPosition(Robot robot, Position newPosition) {
        // Added to clear fallen status when robot returns to board
        fallenRobots.remove(robot);
        // Added falling mechanic check to handle robots going off-board
        if (newPosition != null && !isValidPosition(newPosition)) {
            handleFall(robot);
            return;
        }

        // Get the robot's old position
        Position oldPosition = robotToPosition.get(robot);

        // Remove robot from old location mapping
        if (oldPosition != null) {
            robotPositions.remove(oldPosition);
        }

        // Update location mapping
        if (newPosition != null) {
            robotPositions.put(newPosition, robot);
            robotToPosition.put(robot, newPosition);
        } else {
            robotToPosition.remove(robot);
        }
    }

    /**
     * Transforms the board's grid into a serializable map representation.
     * The resulting map is a three-dimensional list structure where:
     * - Each top-level list represents a column in the grid.
     * - Each second-level list represents a row within a column.
     * - Each third-level list contains the elements present in a specific tile of the grid.
     * Tiles that are null in the grid are represented as null in the map.
     *
     * @return A three-dimensional list representing the serialized state of the board.
     */
    public List<List<List<BoardElement>>> toSerializableMap() {
        List<List<List<BoardElement>>> map = new ArrayList<>();
        for (int x = 0; x < grid.length; x++) {
            List<List<BoardElement>> col = new ArrayList<>();
            for (int y = 0; y < grid[0].length; y++) {
                //Tile tile = grid[x][y]; //FIXME will be fixed when board is fixed....
                //col.add(tile == null ? null : tile.getElements());
                // Fixed: Replaced Tile with List<BoardElement> to match grid type
                // grid[x][y] is never null (always has Floor), so no need for null check
                List<BoardElement> elements = grid[x][y];
                col.add(elements);
            }
            map.add(col);
        }
        return map;
    }

    /**
     * Get the total number of checkpoints on the game board.
     *
     * @return The total number of checkpoints.
     */
    public int getTotalCheckpoints() {
        int count = 0;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // Get all elements on the current grid
                List<BoardElement> elementsAtPos = getElements(x, y);
                // Check if there are checkpoint elements
                for (BoardElement element : elementsAtPos) {
                    if (element instanceof CheckPoints) {
                        count++;
                    }
                }
            }
        }

        return count;
    }
    // Handles robots falling off the 12x12 grid
    public void handleFall(Robot robot) {
        // Set position to VOID_POINT (-1, -1)
        robot.setPosition(VOID_POINT);
        // Remove from board mappings
        Position oldPosition = robotToPosition.get(robot);
        if (oldPosition != null) {
            robotPositions.remove(oldPosition);
        }
        robotToPosition.remove(robot);
        // Add to fallen robots list
        fallenRobots.add(robot);
        // Apply damage and cancel programming
        robot.takeDamage(2);
        robot.cancelProgramming();
        System.out.println("Robot " + robot.getId() + " fell off the board and is at VOID_POINT (-1, -1)");
    }

    // Checks if a robot has fallen off the board
    public boolean hasRobotFallen(Robot robot) {
        return fallenRobots.contains(robot);
    }

    // Reboots a fallen robot to the reboot point
    public void rebootRobot(Robot robot) {
        Position rebootPos = getRebootPosition();
        robot.setPosition(rebootPos.x(), rebootPos.y());
        updateRobotPosition(robot, rebootPos);
        robot.takeDamage(2);
        robot.cancelProgramming();
        System.out.println("Robot " + robot.getId() + " rebooted to " + rebootPos);
        fallenRobots.remove(robot);
    }

    /**
     * Returns a list of all robots whose position is within a specified distance
     * from the specified center.
     *
     * @param center the central position from which the distance is calculated
     * @param range  the maximum distance (inclusive) from the center within which robots are detected
     * @return List of robots whose position is at most {@code range} units away from {@code center}
     */
    public List<Robot> getRobotsInRange(Position center, int range) {
        List<Robot> robotsInRange = new ArrayList<>();
        for (Map.Entry<Position, Robot> entry : robotPositions.entrySet()) {
            Position robotPos = entry.getKey();
            if (center.distanceTo(robotPos) <= range) {
                robotsInRange.add(entry.getValue());
            }
        }
        return robotsInRange;
    }

    /**
     * Retrieves a list of available starting positions on the game board.
     * A starting position is considered valid if it contains a {@code StartPoint} element
     * that is not currently occupied.
     *
     * @return A list of {@code Position} objects representing the valid starting points.
     */
    public List<Position> getStartingPoints() {
        List<Position> positions = new ArrayList<>();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                for (BoardElement element : getElements(i, j)) {
                    if (element instanceof StartPoint sp && !sp.isOccupied()) {
                        positions.add(new Position(i, j));
                        break;
                    }
                }
            }
        }
        return positions;
    }

}

