package de.lmu.dbs.ifi.sep25.game;

import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;
import de.lmu.dbs.ifi.sep25.game.BoardElement.CheckPoints;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Floor;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Reboot;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Belts;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Gear;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Laser;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Pit;
import de.lmu.dbs.ifi.sep25.game.BoardElement.Wall;

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

    public enum MapType {
        DEFAULT, MAP1, MAP2, MAP3, MAP4, MAP5
    }


    @SuppressWarnings("unchecked")
    public Board(MapType mapType) {
        this.width = 12;
        this.height = 12;
        // Initialize grid for 12x12 board
        this.grid = new ArrayList[width][height];
        initializeBoard(mapType);
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
        switch (mapType) {
            case DEFAULT:
                Reboot rebootDefault = Reboot.getInstance();
                rebootDefault.setPosition(new Position(5, 3));
                grid[5][3].add(rebootDefault);
                CheckPoints checkpointDefault = new CheckPoints();
                checkpointDefault.setPosition(new Position(10, 10));
                grid[10][10].add(checkpointDefault);
                Belts conveyorDefault = new Belts(new Position(3,3), Direction.NORTH, Belts.BeltSpeed.SLOW);
                //conveyorDefault.setPosition(new Position(3, 3));
                grid[3][3].add(conveyorDefault);
                Gear gearDefault = new Gear(new Position(4,4), Gear.RotationDirection.CLOCKWISE);

                grid[4][4].add(gearDefault);
                Wall wallDefault = new Wall();
                wallDefault.setPosition(new Position(6, 6));
                grid[6][6].add(wallDefault);
                Pit pitDefault = new Pit();
                pitDefault.setPosition(new Position(8, 8));
                grid[8][8].add(pitDefault);
                Laser laserDefault = new Laser(new Position(7,7), Direction.EAST);

                grid[7][7].add(laserDefault);

                break;
            case MAP1: // Risky Crossing board (beginner's course)
                Reboot reboot = Reboot.getInstance();
                reboot.setPosition(new Position(6, 6));
                grid[6][6].add(reboot);
                CheckPoints checkpoint1 = new CheckPoints();
                checkpoint1.setPosition(new Position(3, 3));
                grid[3][3].add(checkpoint1);
                CheckPoints checkpoint2 = new CheckPoints();
                checkpoint2.setPosition(new Position(9, 9));
                grid[9][9].add(checkpoint2);
                // North-South conveyor (moves down)
                for (int y = 2; y <= 10; y++) {
                    if (y != 6) { // Skip (6,6) as it has Reboot
                        Belts conveyorNS = new Belts(new Position(6, y), Direction.SOUTH, Belts.BeltSpeed.SLOW);
                        conveyorNS.setPosition(new Position(6, y));
                        grid[6][y].add(conveyorNS);
                    }
                }
                // East-West conveyor (moves right)
                for (int x = 2; x <= 10; x++) {
                    if (x != 6) { // Skip (6,6) as it has Reboot
                        Belts conveyorEW = new Belts(new Position(x, 6), Direction.EAST, Belts.BeltSpeed.SLOW);

                        grid[x][6].add(conveyorEW);
                    }
                }
                Laser laserNS = new Laser(new Position(6,3), Direction.EAST);
                laserNS.setPosition(new Position(6, 3));
                grid[6][3].add(laserNS);
                Laser laserEW = new Laser(new Position(3,6), Direction.SOUTH);
                laserEW.setPosition(new Position(3, 6));
                grid[3][6].add(laserEW);
                Wall wall1 = new Wall();
                wall1.setPosition(new Position(5, 5));
                grid[5][5].add(wall1);
                Wall wall2 = new Wall();
                wall2.setPosition(new Position(7, 7));
                grid[7][7].add(wall2);
                Pit pitNS = new Pit();
                pitNS.setPosition(new Position(6, 11));
                grid[6][11].add(pitNS);
                Pit pitEW = new Pit();
                pitEW.setPosition(new Position(11, 6));
                grid[11][6].add(pitEW);
                break;
            case MAP2:
            case MAP3:
            case MAP4:
            case MAP5:
                // Placeholder for other maps
                break;
        }
    }

    // Sets initial robot position based on map type and player choice (0-4)
    public void setStartPosition(Robot robot, MapType mapType, int playerChoice) {
        // Define 5 possible starting positions per map
        Position[] startPositions;
        switch (mapType) {
            case DEFAULT:
                startPositions = new Position[] {
                        new Position(5, 5), new Position(5, 6), new Position(6, 5),
                        new Position(6, 6), new Position(4, 5)
                };
                break;
            case MAP1: // Risky Crossing starting positions
                startPositions = new Position[] {
                        new Position(0, 0),  // Top-left corner
                        new Position(0, 11), // Bottom-left corner
                        new Position(11, 0), // Top-right corner
                        new Position(11, 11), // Bottom-right corner
                        new Position(0, 6)   // Left side near E-W conveyor
                };
                break;
            case MAP2:
                startPositions = new Position[] {
                        new Position(1, 1), new Position(1, 2), new Position(2, 1),
                        new Position(2, 2), new Position(1, 3)
                };
                break;
            case MAP3:
                startPositions = new Position[] {
                        new Position(2, 2), new Position(2, 3), new Position(3, 2),
                        new Position(3, 3), new Position(2, 4)
                };
                break;
            case MAP4:
                startPositions = new Position[] {
                        new Position(3, 3), new Position(3, 4), new Position(4, 3),
                        new Position(4, 4), new Position(3, 5)
                };
                break;
            case MAP5:
                startPositions = new Position[] {
                        new Position(4, 4), new Position(4, 5), new Position(5, 4),
                        new Position(5, 5), new Position(4, 6)
                };
                break;
            default:
                startPositions = new Position[] { new Position(0, 0) };
        }
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
        if (fallenRobots.contains(robot)) {
            Position rebootPos = getRebootPosition();
            robot.setPosition(rebootPos.x(), rebootPos.y());
            fallenRobots.remove(robot);
            updateRobotPosition(robot, rebootPos);
            robot.takeDamage(2); // Additional reboot penalty
            robot.cancelProgramming();
            System.out.println("Robot " + robot.getId() + " rebooted to " + rebootPos);
        }
    }

}


