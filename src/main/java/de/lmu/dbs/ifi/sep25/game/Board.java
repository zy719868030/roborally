package de.lmu.dbs.ifi.sep25.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.utils.FieldDeserializer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the game board where all gameplay elements and interactions occur.
 * The Board class manages the layout, positions of robots, and special tiles.
 */
public class Board {
    // Serialization
    private final Gson mapGson =  new GsonBuilder()
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldDeserializer())
            .setPrettyPrinting()
            .create();

    //private final List<BoardElement>[][] grid;
    private final Tile[][] grid;
    private final int width;
    private final int height;
    private final Map<Position, Robot> robotPositions = new HashMap<>();
    private final Map<Robot, Position> robotToPosition = new HashMap<>();

    // Added to store robots that fall off the board
    private final List<Robot> fallenRobots = new ArrayList<>();
    // Designated point for fallen robots (outside 12x12 grid)
    private static final Position VOID_POINT = new Position(-1, -1);
    private final Map<String, SubBoard> subBoards;

    // SubBoard class to manage boardId and coordinate ranges
    private static class SubBoard {
        private final String boardId;
        private final int minY;
        private final int maxY;

        SubBoard(String boardId, int minY, int maxY) {
            this.boardId = boardId;
            this.minY = minY;
            this.maxY = maxY;
        }

        boolean contains(Position pos) {
            return pos.y() >= minY && pos.y() <= maxY;
        }

        String getBoardId() {
            return boardId;
        }
    }

    @SuppressWarnings("unchecked")
    public Board(MapType mapType) {
        this.width = 10;
        this.height = 13;
        this.grid = new Tile[width][height];
        this.subBoards = new HashMap<>();
        initializeGrid();
        initializeBoard(mapType);
    }

    private void initializeGrid() {
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                //grid[x][y] = new ArrayList<>();
                grid[x][y] = new Tile();
                grid[x][y].addElement(Floor.getInstance());
            }
        }
    }




    /**
     * Enum representing the different types of maps available in the game.
     * Each map type corresponds to a specific board layout and associated game elements.
     * <p>
     * The enum provides a default map type labeled as "Dizzy Highway" and a series of
     * numbered maps for customization or varying game scenarios. The map names are intended
     * to be unique identifiers for board configuration and usage within the game logic.
     */
    public enum MapType {
        DIZZY_HIGHWAY, EXTRA_CRISPY, LOST_BEARINGS, DEATH_TRAP;

        //TODO @prajal add string conversion to your map types
        public String toString() {
            return switch (this) {
                case DIZZY_HIGHWAY -> "Dizzy Highway";
                case EXTRA_CRISPY -> "Extra Crispy";
                case LOST_BEARINGS -> "Lost Bearings";
                case DEATH_TRAP -> "Death Trap";
                default -> "Unknown";
            };
        }
    }

    // Initialize board with tiles based on map type
    private void initializeBoard(MapType mapType) {
        // Initialize sub-boards for Dizzy Highway
        if (mapType == MapType.DIZZY_HIGHWAY) {
            subBoards.put("StartA", new SubBoard("StartA", 0, 2)); // Columns 0–2
            subBoards.put("5B", new SubBoard("5B", 3, 12)); // Columns 3–12
        } else {
            // Default to single board for other map types
            subBoards.put("1B", new SubBoard("1B", 0, height - 1));
        }

        if (mapType == MapType.DIZZY_HIGHWAY) {
            // Dizzy Highway with Start A and 5B
            // Start A (y: 0–2)
            addElement(new Wall(new Position(7, 1), Direction.NORTH, "StartA"));
            addElement(new Wall(new Position(5, 2), Direction.EAST, "StartA"));
            addElement(new Wall(new Position(4, 2), Direction.EAST, "StartA"));
            addElement(new Wall(new Position(2, 1), Direction.SOUTH, "StartA"));
            addElement(new Antenna(new Position(5, 0), Direction.EAST, "StartA"));
            Position[] startPositions = {
                    new Position(8, 1), new Position(6, 0), new Position(5, 1),
                    new Position(4, 1), new Position(3, 0), new Position(1, 1)
            };
            for (Position pos : startPositions) {
                addElement(new StartPoint(pos, Direction.EAST, "StartA"));
            }

            // 5B (y: 3–12)
            for (int x = 9; x >= 2; x--) {
                addElement(new Belts(new Position(x, 4), Direction.NORTH, Belts.BeltSpeed.FAST, "5B"));
            }
            List<Direction> outDirs1 = new ArrayList<>();
            outDirs1.add(Direction.EAST);
            List<Direction> inDirs1 = new ArrayList<>();
            inDirs1.add(Direction.SOUTH);
            addElement(new Belts(new Position(1, 4), outDirs1, inDirs1, Belts.BeltSpeed.FAST, "5B"));

            addElement(new Belts(new Position(9, 5), Direction.NORTH, Belts.BeltSpeed.FAST, "5B"));
            List<Direction> outDirs2a = new ArrayList<>();
            outDirs2a.add(Direction.WEST);
            List<Direction> inDirs2a = new ArrayList<>();
            inDirs2a.add(Direction.SOUTH);
            addElement(new Belts(new Position(8, 5), outDirs2a, inDirs2a, Belts.BeltSpeed.FAST, "5B"));

            List<Direction> outDirs2b = new ArrayList<>();
            outDirs2b.add(Direction.NORTH);
            List<Direction> inDirs2b = new ArrayList<>();
            inDirs2b.add(Direction.EAST);
            addElement(new Belts(new Position(8, 4), outDirs2b, inDirs2b, Belts.BeltSpeed.FAST, "5B"));

            addElement(new Belts(new Position(2, 3), Direction.EAST, Belts.BeltSpeed.FAST, "5B"));
            List<Direction> outDirs3 = new ArrayList<>();
            outDirs3.add(Direction.NORTH);
            List<Direction> inDirs3 = new ArrayList<>();
            inDirs3.add(Direction.WEST);
            addElement(new Belts(new Position(2, 4), outDirs3, inDirs3, Belts.BeltSpeed.FAST, "5B"));

            for (int y = 3; y <= 10; y++) {
                addElement(new Belts(new Position(1, y), Direction.EAST, Belts.BeltSpeed.FAST, "5B"));
            }
            List<Direction> outDirs4 = new ArrayList<>();
            outDirs4.add(Direction.SOUTH);
            List<Direction> inDirs4 = new ArrayList<>();
            inDirs4.add(Direction.WEST);
            addElement(new Belts(new Position(1, 11), outDirs4, inDirs4, Belts.BeltSpeed.FAST, "5B"));

            addElement(new Belts(new Position(0, 10), Direction.SOUTH, Belts.BeltSpeed.FAST, "5B"));
            List<Direction> outDirs5 = new ArrayList<>();
            outDirs5.add(Direction.EAST);
            List<Direction> inDirs5 = new ArrayList<>();
            inDirs5.add(Direction.NORTH);
            addElement(new Belts(new Position(1, 10), outDirs5, inDirs5, Belts.BeltSpeed.FAST, "5B"));

            for (int x = 0; x <= 7; x++) {
                addElement(new Belts(new Position(x, 11), Direction.NORTH, Belts.BeltSpeed.FAST, "5B"));
            }
            List<Direction> outDirs6 = new ArrayList<>();
            outDirs6.add(Direction.WEST);
            List<Direction> inDirs6 = new ArrayList<>();
            inDirs6.add(Direction.SOUTH);
            addElement(new Belts(new Position(8, 11), outDirs6, inDirs6, Belts.BeltSpeed.FAST, "5B"));

            addElement(new Belts(new Position(7, 12), Direction.WEST, Belts.BeltSpeed.FAST, "5B"));
            List<Direction> outDirs7 = new ArrayList<>();
            outDirs7.add(Direction.NORTH);
            List<Direction> inDirs7 = new ArrayList<>();
            inDirs7.add(Direction.EAST);
            addElement(new Belts(new Position(7, 11), outDirs7, inDirs7, Belts.BeltSpeed.FAST, "5B"));

            for (int y = 12; y >= 5; y--) {
                addElement(new Belts(new Position(8, y), Direction.WEST, Belts.BeltSpeed.FAST, "5B"));
            }

            Position[] energyPositions = {
                    new Position(0, 3), new Position(2, 10), new Position(9, 12),
                    new Position(7, 5), new Position(4, 7), new Position(5, 8)
            };
            for (Position pos : energyPositions) {
                addElement(new EnergySpace(pos, "5B"));
            }

            addElement(new CheckPoints(new Position(6, 12), 1, "5B"));

            Reboot reboot = Reboot.getInstance();
            reboot.setPosition(new Position(6, 7));
            reboot.setBoardId("5B");
            addElement(reboot);

            addElement(new Laser(new Position(6, 6), Direction.NORTH, 1, "5B"));
            addElement(new Laser(new Position(3, 6), Direction.WEST, 1, "5B"));
            addElement(new Laser(new Position(3, 9), Direction.SOUTH, 1, "5B"));
            addElement(new Laser(new Position(6, 9), Direction.EAST, 1, "5B"));

            // Optional: Add Pit and Gear (adjust positions as needed)
            addElement(new Pit(new Position(3, 3), "5B"));
            addElement(new Gear(new Position(5, 5), Gear.RotationDirection.CLOCKWISE, "5B"));
        }
        // Add other map types (e.g., MAP1 for Risky Crossing) as needed
    }

    // Added: Helper method to add elements to the grid
    private void addElement(BoardElement element) {
        Position pos = element.getPosition();
        if (pos.x() >= 0 && pos.x() < width && pos.y() >= 0 && pos.y() < height) {
            grid[pos.x()][pos.y()].addElement(element);
        }
    }

    // Sets initial robot position based on map type and player choice (0-4)
    public void setStartPosition(Robot robot, int playerChoice) {
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
        robot.setDirection(Direction.EAST);
        updateRobotPosition(robot, startPos);
        Player player = Game.getInstance().getPlayers().stream()
                .filter(p -> p.getRobot() == robot)
                .findFirst()
                .orElse(null);
        if (player != null) {
            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyStartingPointTaken(startPos.x(), startPos.y(), Direction.EAST.toString(), robot.getId())
            ));
        }
    }

    // Gets elements at a position (used for tile effects)
    public List<BoardElement> getElements(int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            return grid[x][y].getElements();
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
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[x][y].applyEffects(robot, this);
        //List<BoardElement> elementsAt = getElements(x, y);
        for (BoardElement element : grid[x][y].getElements()) {
            String animationType = mapElementToAnimationType(element.getType()); // Check mapping below
            if (animationType != null) {
                Player player = Game.getInstance().getPlayers().stream()
                        .filter(p -> p.getRobot() == robot)
                        .findFirst()
                        .orElse(null);
                if (player != null) {
                    player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                            new MessageDefinitions.BodyAnimation(animationType)
                    ));
                }
            }
            if (element instanceof EnergySpace) {
                Player player = Game.getInstance().getPlayers().stream()
                        .filter(p -> p.getRobot() == robot)
                        .findFirst()
                        .orElse(null);
                if (player != null) {
                    player.addEnergy(1, "EnergySpace");
                }
            }
            if (element instanceof CheckPoints checkPoint) {
                int checkpoints = checkPoint.getRobotHighestCheckpoint(robot.getId()); // Check CheckPoints class for correct implementation
                if (checkpoints > 0) {
                    Player player = Game.getInstance().getPlayers().stream()
                            .filter(p -> p.getRobot() == robot)
                            .findFirst()
                            .orElse(null);
                    if (player != null) {
                        player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                                new MessageDefinitions.BodyCheckPointReached(robot.getId(), checkpoints)
                        ));
                        if (checkpoints == getTotalCheckpoints()) {
                            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                                    new MessageDefinitions.BodyGameFinished(robot.getId())
                            ));
                        }
                    }
                }
            }
        }
        }
    }

    private String mapElementToAnimationType(String elementType) {
        return switch (elementType) {
            case "Belts" -> "BlueConveyorBelt"; // Adjust based on BeltSpeed if needed
            case "Gear" -> "Gear";
            case "Laser" -> "PlayerShooting";
            case "PushPanel" -> "PushPanel";
            case "EnergySpace" -> "EnergySpace";
            default -> null;
        };
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

    public Tile[][] getGrid() {
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
    private List<List<List<MessageDefinitions.Field>>> toSerializableMap() {
        List<List<List<MessageDefinitions.Field>>> map = new ArrayList<>();
        for (int x = 0; x < grid.length; x++) {
            List<List<MessageDefinitions.Field>> col = new ArrayList<>();
            for (int y = 0; y < grid[0].length; y++) {
                Tile tile = grid[x][y];

                if (tile == null) {
                    col.add(null);
                } else {
                    col.add(grid[x][y].toSerializableList().stream().map(BoardElement::toField).toList());
                }
            }
            map.add(col);
        }
        return map;
    }

    /**
     * Serializes the current state of the board into a JSON-formatted message.
     * The resulting message includes metadata and the serialized map representation of the board.
     *
     * @return A JSON string representing the serialized board as a message.
     */
    public String getSerializedBoardAsMessage() {
        return mapGson.toJson(new MessageDefinitions.Message<>(new MessageDefinitions.BodyGameStarted(5, toSerializableMap())));
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

    public boolean hasRobotFallen(Robot robot) {
        return fallenRobots.contains(robot);
    }

    // Reboots a fallen robot to the reboot point
    public void rebootRobot(Robot robot) {
        Position rebootPos = getRebootPosition();
        Robot otherRobot = getRobotAt(rebootPos);
        if (otherRobot != null) {
            Reboot reboot = (Reboot) getElements(rebootPos.x(), rebootPos.y()).stream()
                    .filter(e -> e.getType().equals("Reboot"))
                    .findFirst()
                    .orElse(null);
            if (reboot != null) {
                Direction pushDir = reboot.getDirection(); // Check Reboot class for correct implementation
                Position pushPos = rebootPos.move(pushDir);
                if (isValidPosition(pushPos) && getRobotAt(pushPos) == null) {
                    otherRobot.setPosition(pushPos);
                    updateRobotPosition(otherRobot, pushPos);
                    otherRobot.notifyMovement();
                } else {
                    List<Position> startPoints = getStartingPoints();
                    Position altPos = startPoints.stream()
                            .filter(pos -> getRobotAt(pos) == null)
                            .findFirst()
                            .orElse(rebootPos);
                    rebootPos = altPos;
                }
            }
        }
        robot.setPosition(rebootPos);
        updateRobotPosition(robot, rebootPos);
        robot.takeDamage(2);
        robot.cancelProgramming();
        System.out.println("Robot " + robot.getId() + " rebooted to " + rebootPos);

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

    /**
     * Gets the position of the priority antenna on the board.
     * This is used to determine player priority during the game.
     *
     * @return the Position of the antenna, or a default position if no antenna is found
     */
    public Position getAntennaPosition() {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                for (BoardElement element : getElements(x, y)) {
                    if (element instanceof Antenna) {
                        return element.getPosition();
                    }
                }
            }
        }
        // If no antenna is found, return a default position (0,0)
        System.err.println("Warning: No antenna found on the board! Using default position.");
        return new Position(0, 0);
    }

}

