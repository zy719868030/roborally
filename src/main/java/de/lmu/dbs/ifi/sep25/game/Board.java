package de.lmu.dbs.ifi.sep25.game;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;
import de.lmu.dbs.ifi.sep25.game.Maps.*;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.ui.GameController;
import de.lmu.dbs.ifi.sep25.utils.FieldDeserializer;
import de.lmu.dbs.ifi.sep25.utils.FieldSerializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the game board where all gameplay elements and interactions occur.
 * The Board class manages the layout, positions of robots, and special tiles.
 */
public class Board {

    // 0. Logging
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(Board.class);
    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");

    // 1. Resource
    private final Gson mapGson = new GsonBuilder()
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldDeserializer())
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldSerializer())
            .create();

    // 2. Board state
    private final Tile[][] grid;
    private final int width;
    private final int height;
    private final Map<Position, Robot> robotPositions = new HashMap<>();
    private final Map<Robot, Position> robotToPosition = new HashMap<>();
    private final List<Robot> fallenRobots = new ArrayList<>();
    private static final Position VOID_POINT = new Position(-1, -1);
    private Position antennaPosition;
    public final Map<String, SubBoard> subBoards = new HashMap<>();

    // SubBoard class to manage boardId and coordinate ranges
    public static class SubBoard {
        private final String boardId;
        private final int minX;
        private final int maxX;

        public SubBoard(String boardId, int minX, int maxX) {
            this.boardId = boardId;
            this.minX = minX;
            this.maxX = maxX;
        }

        boolean contains(Position pos) {
            return pos.y() >= minX && pos.y() <= maxX;
        }

        String getBoardId() {
            return boardId;
        }
    }


    /**
     * Constructs a Board object and initializes it based on the given map type.
     * This process involves determining the board dimensions, creating sub-boards,
     * initializing tiles, and setting up specific board configurations depending
     * on the provided map type.
     *
     * @param mapType The type of map to initialize. It defines the board's structure,
     *                sub-board arrangement, and tile settings based on the map style
     *                (e.g., "normal" or "reverse").
     * @throws IllegalArgumentException if the map type corresponds to an unknown
     *                                  or unsupported board style.
     */
    public Board(MapType mapType) {
        final String boardStyle = MapType.boardStyleFromMapType(mapType);

        logger.info("Initializing \"{}\" board with map type: {}", boardStyle, mapType.toString());

        switch (boardStyle) {
            case "normal" -> {
                this.width = 13;
                this.height = 10;
                this.grid = new Tile[width][height];

                Floor.createFloor("StartA");
                Floor.createFloor("5B");

                for (int x = 0; x < width; x++) {
                    Floor floor = (x < 3) ? Floor.getInstance("StartA") : Floor.getInstance("5B");
                    for (int y = 0; y < height; y++) {
                        grid[x][y] = new Tile();
                        grid[x][y].addElement(floor);
                    }
                }
            }
            case "reverse" -> {
                this.width = 13;
                this.height = 10;
                this.grid = new Tile[width][height];

                Floor.createFloor("StartA");
                Floor.createFloor("5B");

                for (int x = 0; x < width; x++) {
                    Floor floor = (x > 9) ? Floor.getInstance("StartA") : Floor.getInstance("5B");
                    for (int y = 0; y < height; y++) {
                        grid[x][y] = new Tile();
                        grid[x][y].addElement(floor);
                    }
                }

            }
            default ->
                    throw new IllegalArgumentException("Unknown/Not implemented board style: " + MapType.boardStyleFromMapType(mapType));
        }

        logger.info("Board initialized.");
        logger.info("Initializing board implementation...");

        initializeBoard(mapType);
    }

    /**
     * Initializes the game board based on the specified map type. This method sets up the grid,
     * board elements, and antenna position for the game. Depending on the map type, different
     * implementations and configurations of the board are applied.
     *
     * @param mapType The type of map to initialize. It determines the specific board configuration
     *                and elements for the game environment.
     */
    private void initializeBoard(MapType mapType) {
        GameMap mapImplementation = switch (mapType) {
            case DIZZY_HIGHWAY -> new DizzyHighway();
            case LOST_BEARINGS -> new LostBearings();
            case EXTRA_CRISPY -> new ExtraCrispy();
            case DEATH_TRAP -> new DeathTrap();
            default -> throw new IllegalArgumentException("Unknown map type: " + mapType);
        };

        // Copy elements and antenna from the map implementation
        this.antennaPosition = mapImplementation.getAntennaPosition();

        logger.info("Board implementation initialized.");
        logger.info("Adding elements to board...");

        for (BoardElement element : mapImplementation.getElements()) {
            if (!(element instanceof Floor)) {
                addElement(element);
            }
        }

        logger.info("Added elements to board.");

//        logger.info("Board elements:");
//        logger.info(getElements(1, 1).toString());
//        logger.info(getElements(antennaPosition.x(), antennaPosition.y()).toString());
//
//        logger.info("Checkpoint: " + getElements(12, 3).toString());
//        logger.info("Checkpoint: " + getElements(12, 3).getFirst().toString());
    }

    /**
     * Adds a {@link BoardElement} to the board at its specified position,
     * if the position is within the bounds of the grid.
     *
     * @param element The board element to add. Its position is determined
     *                using the {@code getPosition()} method.
     */
    private void addElement(BoardElement element) {
        Position pos = element.getPosition();
        if (isValidPosition(pos)) {
            grid[pos.x()][pos.y()].addElement(element);
        }
    }

//    // Sets initial robot position based on map type and player choice (0-4)
//    public void setStartPosition(Robot robot, int playerChoice) {
//        // Define 5 possible starting positions per map
//        Position[] startPositions = {
//                new Position(8, 1), new Position(6, 0), new Position(5, 1),
//                new Position(4, 1), new Position(3, 0), new Position(1, 1)
//        };
//
//        // Validate player choice (0-4)
//        int choice = Math.min(Math.max(playerChoice, 0), startPositions.length - 1);
//        Position startPos = startPositions[choice];
//        // If position is occupied, try next available
//        int originalChoice = choice;
//        while (getRobotAt(startPos) != null && choice < startPositions.length - 1) {
//            choice++;
//            startPos = startPositions[choice];
//        }
//        // If all positions are taken, use fallback
//        if (getRobotAt(startPos) != null) {
//            startPos = startPositions[originalChoice];
//            System.out.println("Warning: Start position for Robot " + robot.getId() + " may overlap.");
//        }
//        robot.setPosition(startPos);
//        robot.setDirection(Direction.EAST);
//        updateRobotPosition(robot, startPos);
//        Player player = Game.getInstance().getPlayers().stream()
//                .filter(p -> p.getRobot() == robot)
//                .findFirst()
//                .orElse(null);
//        if (player != null) {
//            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyStartingPointTaken(startPos.x(), startPos.y(), Direction.EAST.toString(), robot.getId())
//            ));
//        }
//    }

    public List<BoardElement> getElements() {
        List<BoardElement> elements = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                elements.addAll(grid[x][y].getElements());
            }
        }
        return elements;
    }

    public List<BoardElement> getElements(Position position) {
        return getElements(position.x(), position.y());
    }

    public List<BoardElement> getElements(int x, int y) {
        if (isValidPosition(new Position(x, y)) && grid[x][y] != null) {
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
                    int checkpoints = checkPoint.getRobotHighestCheckpoint(robot.getRobotID()); // Check CheckPoints class for correct implementation
                    if (checkpoints > 0) {
                        Player player = Game.getInstance().getPlayers().stream()
                                .filter(p -> p.getRobot() == robot)
                                .findFirst()
                                .orElse(null);
                        if (player != null) {
                            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                                    new MessageDefinitions.BodyCheckPointReached(robot.getRobotID(), checkpoints)
                            ));
                            if (checkpoints == getTotalCheckpoints()) {
                                player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                                        new MessageDefinitions.BodyGameFinished(robot.getRobotID())
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
        for (Tile[] tiles : grid) {
            List<List<MessageDefinitions.Field>> col = new ArrayList<>();
            for (int y = 0; y < grid[0].length; y++) {
                Tile tile = tiles[y];

                if (tile == null) {
                    col.add(null);
                } else {
                    col.add(tiles[y].toSerializableList().stream().map(BoardElement::toField).toList());
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
//        robot.setPosition(VOID_POINT);
//        // Remove from board mappings
//        Position oldPosition = robotToPosition.get(robot);
//        if (oldPosition != null) {
//            robotPositions.remove(oldPosition);
//        }
//        robotToPosition.remove(robot);
        // Add to fallen robots list
        fallenRobots.add(robot);
        // Apply damage and cancel programming
        robot.takeDamage(2);
        robot.cancelProgramming();
        logger.info("Robot {} fell off the board and is at VOID_POINT (-1, -1)", robot.getRobotID());

        // Send Reboot message before rebooting
        Player player = Game.getInstance().getPlayers().stream()
                .filter(p -> p.getRobot() == robot)
                .findFirst()
                .orElse(null);
        if (player != null) {
            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyReboot(robot.getRobotID())
            ));
        }

        // Reboot the robot
        rebootRobot(robot);
    }

    public boolean hasRobotFallen(Robot robot) {
        return fallenRobots.contains(robot);
    }

    // Reboots a fallen robot to the reboot point
    public void rebootRobot(Robot robot) {
        // Remove from fallen robots list
        fallenRobots.remove(robot);
        Position rebootPos = getRebootPosition();
        Robot otherRobot = getRobotAt(rebootPos);
        // Handle if reboot position is occupied
        if (otherRobot != null) {
            Reboot reboot = (Reboot) getElements(rebootPos.x(), rebootPos.y()).stream()
                    .filter(e -> e.getType().equals("Reboot"))
                    .findFirst()
                    .orElse(null);
            if (reboot != null) {
                Direction pushDir = reboot.getDirection(); // Check Reboot class for correct implementation
                Position pushPos = rebootPos.move(pushDir);
                // Try to push the occupying robot
                if (isValidPosition(pushPos) && getRobotAt(pushPos) == null) {
                    otherRobot.setPosition(pushPos);
                    updateRobotPosition(otherRobot, pushPos);
                    otherRobot.notifyMovement();
                } else {
                    // If can't push, recursively handle chain pushing
                    logger.warn("Cannot push robot {} from reboot position, attempting chain push", otherRobot.getRobotID());
                    // Keep trying to push in the direction until we find an empty space
                    Position currentPos = pushPos;
                    List<Robot> robotChain = new ArrayList<>();
                    robotChain.add(otherRobot);

                    while (isValidPosition(currentPos) && getRobotAt(currentPos) != null) {
                        robotChain.add(getRobotAt(currentPos));
                        currentPos = currentPos.move(pushDir);
                        // If we found a valid empty position, push all robots in the chain
                        if (isValidPosition(currentPos)) {
                            for (int i = robotChain.size() - 1; i >= 0; i--) {
                                Robot r = robotChain.get(i);
                                Position newPos = (i == 0) ? pushPos : robotChain.get(i - 1).getPosition().move(pushDir);
                                r.setPosition(newPos);
                                updateRobotPosition(r, newPos);
                                r.notifyMovement();
                            }
                        } else {
                            logger.error("Cannot push robots from reboot position - no valid space found");
                        }
                    }
                }
            }
        }
        // Place robot on reboot position
        robot.setPosition(rebootPos);
        updateRobotPosition(robot, rebootPos);

        // Set direction (TODO should allow player to choose, but for now use default NORTH)
        robot.setDirection(Direction.NORTH);

        // Add 2 SPAM damage cards (as per rules)
        Player player = Game.getInstance().getPlayers().stream()
                .filter(p -> p.getRobot() == robot)
                .findFirst()
                .orElse(null);
        if (player != null) {
            robot.addDamageCard(DamageCard.DamageType.SPAM);
            robot.addDamageCard(DamageCard.DamageType.SPAM);
            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyMovement(player.getConnection().getMyID(), rebootPos.x(), rebootPos.y())
            ));
        }

//        System.out.println("Reboot robot " + robot.getPosition().x() + " " + robot.getPosition().y());
        logger.info("Robot {} rebooted to {}", robot.getRobotID(), rebootPos);

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

    /**
     * Retrieves the first available "Reboot" element from the board based on its type.
     * If no "Reboot" element exists, an {@link IllegalStateException} is thrown.
     *
     * @return The {@link Reboot} element found on the board.
     * @throws IllegalStateException If no "Reboot" element is found on the board.
     */
    public Reboot getReboot() {
        //TODO add position param to check for subboard
        final Reboot reboot = (Reboot) getElements(getRebootPosition()).stream().filter(e -> e.getType().equals("Reboot")).findFirst().orElse(null);
        if (reboot == null)
            throw new IllegalStateException("Reboot element not found on board!");
        return reboot;
    }

}

