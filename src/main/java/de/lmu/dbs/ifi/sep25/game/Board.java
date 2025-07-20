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
 * Represents the game board where all gameplay elements and interactions occur in RoboRally.
 * 
 * <p>The Board class is the central component that manages the game environment, including
 * the grid layout, robot positions, board elements, and all game mechanics. It serves as
 * the foundation for all gameplay interactions and state management.</p>
 * 
 * <p>Key features of the Board:</p>
 * <ul>
 *   <li><strong>Grid Management:</strong> Maintains a 2D grid of tiles containing board elements</li>
 *   <li><strong>Robot Tracking:</strong> Manages robot positions and movement across the board</li>
 *   <li><strong>Element Interaction:</strong> Handles all board element effects and interactions</li>
 *   <li><strong>Fall Handling:</strong> Manages robots falling off the board and reboot mechanics</li>
 *   <li><strong>Map Initialization:</strong> Sets up different board configurations based on map types</li>
 *   <li><strong>Serialization:</strong> Provides board state serialization for network communication</li>
 *   <li><strong>Sub-Board Support:</strong> Manages multiple board sections with different IDs</li>
 *   <li><strong>Animation Coordination:</strong> Triggers animations for various board element interactions</li>
 * </ul>
 * 
 * <p>The Board class coordinates all game mechanics including movement, damage application,
 * checkpoint tracking, energy collection, and robot rebooting. It ensures proper game flow
 * and maintains the integrity of the game state throughout gameplay.</p>
 * 
 * @author Edle Eisbecher Team
 * @version 1.0
 * @since 1.0
 */
public class Board {

    /** Logger for general board operations and debugging */
    private static final Logger logger = org.apache.logging.log4j.LogManager.getLogger(Board.class);
    
    /** Logger specifically for message-related operations */
    private static final Logger messageLogger = LogManager.getLogger("MessageLogger");

    /** Gson instance configured for map serialization with custom type adapters */
    private final Gson mapGson = new GsonBuilder()
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldDeserializer())
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldSerializer())
            .create();

    /** 2D grid of tiles representing the game board */
    private final Tile[][] grid;
    
    /** Width of the game board in tiles */
    private final int width;
    
    /** Height of the game board in tiles */
    private final int height;
    
    /** Mapping from positions to robots for quick robot lookup by position */
    private final Map<Position, Robot> robotPositions = new HashMap<>();
    
    /** Mapping from robots to positions for quick position lookup by robot */
    private final Map<Robot, Position> robotToPosition = new HashMap<>();
    
    /** List of robots that have fallen off the board and are awaiting reboot */
    private final List<Robot> fallenRobots = new ArrayList<>();
    
    /** Special position indicating a robot is off the board (void state) */
    private static final Position VOID_POINT = new Position(-1, -1);
    
    /** Position of the priority antenna on the board */
    private Position antennaPosition;
    
    /** Map of sub-boards for managing different board sections */
    public final Map<String, SubBoard> subBoards = new HashMap<>();

    /**
     * Inner class representing a sub-board section with specific coordinate ranges.
     * 
     * <p>SubBoards are used to manage different sections of the main board,
     * each with their own board ID and coordinate boundaries. This allows for
     * modular board design and separate management of different board areas.</p>
     * 
     * @author Edle Eisbecher Team
     * @version 1.0
     * @since 1.0
     */
    public static class SubBoard {
        
        /** Unique identifier for this sub-board */
        private final String boardId;
        
        /** Minimum X coordinate for this sub-board */
        private final int minX;
        
        /** Maximum X coordinate for this sub-board */
        private final int maxX;

        /**
         * Constructs a sub-board with specified boundaries.
         * 
         * @param boardId the unique identifier for this sub-board
         * @param minX the minimum X coordinate for this sub-board
         * @param maxX the maximum X coordinate for this sub-board
         */
        public SubBoard(String boardId, int minX, int maxX) {
            this.boardId = boardId;
            this.minX = minX;
            this.maxX = maxX;
        }

        /**
         * Checks whether a position is within this sub-board's boundaries.
         * 
         * @param pos the position to check
         * @return true if the position is within this sub-board, false otherwise
         */
        boolean contains(Position pos) {
            return pos.y() >= minX && pos.y() <= maxX;
        }

        /**
         * Gets the board ID for this sub-board.
         * 
         * @return the board ID
         */
        String getBoardId() {
            return boardId;
        }

        /**
         * Gets the minimum X coordinate for this sub-board.
         * 
         * @return the minimum X coordinate
         */
        public int getMinX() {
            return minX;
        }
    }

    /**
     * Constructs a Board object and initializes it based on the given map type.
     * 
     * <p>This constructor creates a complete game board with the specified dimensions
     * and configuration. The initialization process includes:</p>
     * <ul>
     *   <li>Determining board dimensions based on map style</li>
     *   <li>Creating the tile grid with appropriate floor elements</li>
     *   <li>Setting up sub-board configurations</li>
     *   <li>Initializing board elements from the map implementation</li>
     *   <li>Configuring the antenna position</li>
     * </ul>
     * 
     * <p>The board supports different styles ("normal" and "reverse") which affect
     * the floor tile distribution and overall board layout.</p>
     * 
     * @param mapType the type of map to initialize, defining the board's structure and elements
     * @throws IllegalArgumentException if the map type corresponds to an unknown or unsupported board style
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
     * Initializes the game board based on the specified map type.
     * 
     * <p>This method sets up the specific board elements and configurations for the
     * given map type. It creates the appropriate map implementation and copies all
     * board elements to the grid, excluding floor elements which are already present.</p>
     * 
     * <p>The initialization process includes:</p>
     * <ul>
     *   <li>Creating the appropriate map implementation based on map type</li>
     *   <li>Setting the antenna position for priority determination</li>
     *   <li>Adding all board elements to their respective positions</li>
     *   <li>Configuring sub-board mappings if applicable</li>
     * </ul>
     * 
     * @param mapType the type of map to initialize, determining the specific board configuration
     * @throws IllegalArgumentException if the map type is unknown or unsupported
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
     * Adds a board element to the board at its specified position.
     * 
     * <p>This method validates that the element's position is within the board boundaries
     * and adds it to the appropriate tile in the grid. Elements are added to tiles
     * which can contain multiple elements at the same position.</p>
     * 
     * @param element the board element to add to the board
     * @throws IllegalArgumentException if element is null
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

    /**
     * Retrieves all board elements from the entire board.
     * 
     * <p>This method collects all board elements from every tile in the grid
     * and returns them as a single list. This is useful for board-wide operations
     * such as serialization or element counting.</p>
     * 
     * @return a list containing all board elements on the board
     */
    public List<BoardElement> getElements() {
        List<BoardElement> elements = new ArrayList<>();
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                elements.addAll(grid[x][y].getElements());
            }
        }
        return elements;
    }

    /**
     * Retrieves all board elements at a specific position.
     * 
     * <p>This method returns all board elements located at the specified position.
     * Since tiles can contain multiple elements, this method returns a list of
     * all elements at that position.</p>
     * 
     * @param position the position to get elements from
     * @return a list of board elements at the specified position
     */
    public List<BoardElement> getElements(Position position) {
        return getElements(position.x(), position.y());
    }

    /**
     * Retrieves all board elements at specific coordinates.
     * 
     * <p>This method returns all board elements located at the specified x,y coordinates.
     * If the coordinates are invalid or the tile is null, an empty list is returned.</p>
     * 
     * @param x the x coordinate
     * @param y the y coordinate
     * @return a list of board elements at the specified coordinates, or empty list if invalid
     */
    public List<BoardElement> getElements(int x, int y) {
        if (isValidPosition(new Position(x, y)) && grid[x][y] != null) {
            return grid[x][y].getElements();
        }
        return new ArrayList<>();
    }

    /**
     * Places a robot at the specified coordinates on the board.
     * 
     * <p>This method validates the coordinates and either places the robot at the
     * specified position or handles the robot falling off the board if the coordinates
     * are invalid.</p>
     * 
     * @param robot the robot to place on the board
     * @param x the x coordinate for placement
     * @param y the y coordinate for placement
     * @throws IllegalArgumentException if robot is null
     */
    public void placeRobot(Robot robot, int x, int y) {
        if (x >= 0 && x < width && y >= 0 && y < height) {
            robot.setPosition(x, y);
            updateRobotPosition(robot, robot.getPosition());
        } else {
            // Handle robots falling off the board
            handleFall(robot);
        }
    }

    /**
     * Applies effects from board elements to a robot at specific coordinates.
     * 
     * <p>This method triggers all board element effects for a robot at the specified
     * position. It handles various interactions including:</p>
     * <ul>
     *   <li>Energy space collection and player energy updates</li>
     *   <li>Checkpoint progress tracking and win condition checking</li>
     *   <li>Animation triggering for visual feedback</li>
     *   <li>Network message broadcasting for client updates</li>
     * </ul>
     * 
     * <p>The method also manages fallen robot states and prevents effect application
     * to robots that have fallen off the board.</p>
     * 
     * @param robot the robot to apply effects to
     * @param x the x coordinate where effects should be applied
     * @param y the y coordinate where effects should be applied
     */
    public void applyEffects(Robot robot, int x, int y) {
        // Skip if robot has fallen off
        if (fallenRobots.contains(robot)) return;
        if (x >= 0 && x < width && y >= 0 && y < height) {
            grid[x][y].applyEffects(robot, this);
            if (fallenRobots.contains(robot)) return;
            //List<BoardElement> elementsAt = getElements(x, y);
            for (BoardElement element : grid[x][y].getElements()) {
                if (fallenRobots.contains(robot)) break;
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

    /**
     * Maps board element types to their corresponding animation types.
     * 
     * <p>This method provides a mapping between board element types and the
     * animation types that should be triggered when robots interact with them.
     * The animations provide visual feedback to players about element interactions.</p>
     * 
     * @param elementType the type of board element
     * @return the corresponding animation type, or null if no animation is needed
     */
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
     * <p>This method validates that the given position has coordinates that fall
     * within the board's boundaries (0 to width-1 for x, 0 to height-1 for y).</p>
     * 
     * @param position the position to be checked
     * @return true if the position is valid, false if it's outside the board boundaries
     */
    public boolean isValidPosition(Position position) {
        int x = position.x();
        int y = position.y();
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    /**
     * Gets the robot at the specified position.
     * 
     * <p>This method returns the robot located at the given position, if any.
     * Since only one robot can occupy a position at a time, this method returns
     * either a single robot or null.</p>
     * 
     * @param position the position to check for a robot
     * @return the robot at that position, or null if no robot is present
     */
    public Robot getRobotAt(Position position) {
        return robotPositions.get(position);
    }

    /**
     * Gets the position of the reboot token on the board.
     * 
     * <p>This method searches the board for a Reboot element and returns its position.
     * This is where robots will respawn after falling off the board or into a pit.
     * If no reboot token is found, a fallback position (0,0) is returned.</p>
     * 
     * @return the position of the reboot token, or (0,0) if no reboot token exists
     */
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

    /**
     * Gets the 2D grid of tiles representing the game board.
     * 
     * @return the tile grid as a 2D array
     */
    public Tile[][] getGrid() {
        return grid;
    }

    /**
     * Gets the width of the game board.
     * 
     * @return the board width in tiles
     */
    public int getWidth() {
        return width;
    }

    /**
     * Gets the height of the game board.
     * 
     * @return the board height in tiles
     */
    public int getHeight() {
        return height;
    }

    /**
     * Updates the robot's position on the board.
     * 
     * <p>This method manages the robot position tracking system, including:</p>
     * <ul>
     *   <li>Removing the robot from its old position</li>
     *   <li>Adding the robot to its new position</li>
     *   <li>Handling fallen robot states</li>
     *   <li>Managing void positions for off-board robots</li>
     * </ul>
     * 
     * <p>The method also handles robots falling off the board and returning to the board
     * during reboot processes.</p>
     * 
     * @param robot the robot whose position is being updated
     * @param newPosition the new position for the robot
     */
    public void updateRobotPosition(Robot robot, Position newPosition) {
        // Added to clear fallen status when robot returns to board
//        fallenRobots.remove(robot);
        // Check if robot has fallen - if so, don't process normal position updates
        if (fallenRobots.contains(robot) && (newPosition == null || newPosition.equals(new Position(-1, -1)))) {
            // Robot is fallen and being moved to void position - allow this
            robotToPosition.remove(robot);
            Position oldPosition = robotToPosition.get(robot);
            if (oldPosition != null) {
                robotPositions.remove(oldPosition);
            }
            return;
        }

        // Clear fallen status when robot returns to board (during reboot)
        if (fallenRobots.contains(robot) && newPosition != null && isValidPosition(newPosition)) {
            fallenRobots.remove(robot);
            logger.info("Robot {} returned to board at position {}", robot.getRobotID(), newPosition);
        }

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
     * 
     * <p>This method converts the 2D tile grid into a three-dimensional list structure
     * suitable for network transmission. The structure is organized as:</p>
     * <ul>
     *   <li>Top-level list: represents columns in the grid</li>
     *   <li>Second-level list: represents rows within each column</li>
     *   <li>Third-level list: contains board elements in each tile</li>
     * </ul>
     * 
     * <p>Null tiles in the grid are represented as null in the serialized map.</p>
     * 
     * @return a three-dimensional list representing the serialized board state
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
     * 
     * <p>This method creates a complete board state message that can be transmitted
     * to clients. The message includes metadata and the serialized map representation
     * of the board.</p>
     * 
     * @return a JSON string representing the serialized board as a message
     */
    public String getSerializedBoardAsMessage() {
        String result = mapGson.toJson(new MessageDefinitions.Message<>(new MessageDefinitions.BodyGameStarted(5, toSerializableMap())));
        System.out.println("[BOARD] Serialized board message: {"+result+"}");
        return result;
//        return mapGson.toJson(new MessageDefinitions.Message<>(new MessageDefinitions.BodyGameStarted(5, toSerializableMap())));
    }

    /**
     * Gets the total number of checkpoints on the game board.
     * 
     * <p>This method counts all CheckPoints elements across the entire board.
     * The checkpoint count is used for win condition checking and progress tracking.</p>
     * 
     * @return the total number of checkpoints on the board
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

    /**
     * Handles robots falling off the board.
     * 
     * <p>This method manages the complete fall process for robots that go outside
     * the board boundaries. The process includes:</p>
     * <ul>
     *   <li>Removing the robot from board position mappings</li>
     *   <li>Setting the robot to the void position (-1, -1)</li>
     *   <li>Adding the robot to the fallen robots list</li>
     *   <li>Applying reboot damage and programming cancellation</li>
     *   <li>Sending reboot messages to clients</li>
     * </ul>
     * 
     * @param robot the robot that has fallen off the board
     */
    public void handleFall(Robot robot) {
        // Remove from board mappings
        Position oldPosition = robotToPosition.get(robot);
        if (oldPosition != null) {
            robotPositions.remove(oldPosition);
        }
        // Set position to VOID_POINT (-1, -1) to indicate it's off the board
        robot.setPosition(VOID_POINT);
        robotToPosition.remove(robot);
        // Add to fallen robots list
        fallenRobots.add(robot);
        // Apply damage and cancel programming
//        robot.takeDamage(2);
//        robot.cancelProgramming();
        logger.info("Robot {} fell off the board and is at VOID_POINT (-1, -1)", robot.getRobotID());

        // Send Reboot message before rebooting
        Player player = Game.getInstance().getPlayers().stream()
                .filter(p -> p.getRobot() == robot)
                .findFirst()
                .orElse(null);
        if (player != null) {
            addRebootDamage(robot);
            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
                    new MessageDefinitions.BodyReboot(robot.getClientID())
            ));
            logger.info("Sent BodyReboot message for robot {}, waiting for direction selection", robot.getRobotID());
        }

        // Reboot the robot
//        rebootRobot(robot);
    }

    /**
     * Checks whether a robot has fallen off the board.
     * 
     * @param robot the robot to check
     * @return true if the robot has fallen, false otherwise
     */
    public boolean hasRobotFallen(Robot robot) {
        return fallenRobots.contains(robot);
    }

    /**
     * Reboots a fallen robot to the reboot point.
     * 
     * <p>This method handles the complete reboot process for robots that have fallen
     * off the board. The process includes:</p>
     * <ul>
     *   <li>Removing the robot from the fallen robots list</li>
     *   <li>Handling occupied reboot positions with chain pushing</li>
     *   <li>Placing the robot at the reboot position</li>
     *   <li>Updating position mappings</li>
     *   <li>Notifying the robot of movement</li>
     * </ul>
     * 
     * <p>The method also handles cases where the reboot position is occupied by
     * another robot, implementing chain pushing to make space.</p>
     * 
     * @param robot the robot to be rebooted
     */
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
                            break;
                        } else {
                            logger.error("Cannot push robots from reboot position - no valid space found");
                            break;
                        }
                    }
                }
            }
        }
        // Place robot on reboot position
        robot.setPosition(rebootPos);
        updateRobotPosition(robot, rebootPos);
        robot.notifyMovement();

        // Set direction (TODO should allow player to choose, but for now use default NORTH)
//        robot.setDirection(Direction.NORTH);

        // Add 2 SPAM damage cards (as per rules)
//        Player player = Game.getInstance().getPlayers().stream()
//                .filter(p -> p.getRobot() == robot)
//                .findFirst()
//                .orElse(null);
//        if (player != null) {
//            robot.addDamageCard(DamageCard.DamageType.SPAM);
//            robot.addDamageCard(DamageCard.DamageType.SPAM);
//            player.getConnection().broadcastMessage(new MessageDefinitions.Message<>(
//                    new MessageDefinitions.BodyMovement(player.getConnection().getMyID(), rebootPos.x(), rebootPos.y())
//            ));
//        }
//        System.out.println("Reboot robot " + robot.getPosition().x() + " " + robot.getPosition().y());
        logger.info("Robot {} rebooted to {}", robot.getRobotID(), rebootPos);

    }

    /**
     * Applies uniform reboot damage and programming cancellation to a robot.
     * 
     * <p>This method implements the official RoboRally rules for reboot penalties:</p>
     * <ul>
     *   <li>2 damage points added to the robot</li>
     *   <li>2 SPAM damage cards added to the robot's discard pile</li>
     *   <li>Remaining programming for the current round is cancelled</li>
     * </ul>
     * 
     * <p>This penalty system ensures that falling off the board has significant
     * consequences while still allowing robots to continue playing.</p>
     * 
     * @param robot the robot to apply reboot damage to
     */
    public void addRebootDamage(Robot robot) {
        // Add 2 damage points
        robot.takeDamage(2);

        // Add 2 SPAM damage cards to discard pile
        robot.addDamageCard(DamageCard.DamageType.SPAM);
        robot.addDamageCard(DamageCard.DamageType.SPAM);

        // Cancel remaining programming for this round
        robot.cancelProgramming();

        logger.info("Robot {} received reboot damage: 2 points + 2 SPAM cards", robot.getRobotID());
    }

    /**
     * Returns a list of all robots within a specified distance from a center point.
     * 
     * <p>This method calculates the distance from the center point to each robot
     * and returns all robots that are within or at the specified range distance.
     * The distance calculation uses the Position.distanceTo() method.</p>
     * 
     * @param center the central position from which distances are calculated
     * @param range the maximum distance (inclusive) from the center within which robots are detected
     * @return a list of robots whose position is at most range units away from center
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
     * 
     * <p>This method searches the entire board for StartPoint elements that are
     * not currently occupied by any player. These positions can be used for
     * initial robot placement or as alternative restart locations.</p>
     * 
     * @return a list of Position objects representing available starting points
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
     * 
     * <p>This method searches the board for an Antenna element and returns its position.
     * The antenna is used to determine player priority during the game. If no antenna
     * is found, a default position (0,0) is returned with a warning.</p>
     * 
     * @return the position of the antenna, or (0,0) if no antenna is found
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
     * Retrieves the first available Reboot element from the board.
     * 
     * <p>This method searches for a Reboot element at the reboot position and returns it.
     * If no Reboot element is found, an IllegalStateException is thrown since reboot
     * functionality is essential for game continuity.</p>
     * 
     * @return the Reboot element found on the board
     * @throws IllegalStateException if no Reboot element is found on the board
     */
    public Reboot getReboot() {
        //TODO add position param to check for subboard
        final Reboot reboot = (Reboot) getElements(getRebootPosition()).stream().filter(e -> e.getType().equals("Reboot")).findFirst().orElse(null);
        if (reboot == null)
            throw new IllegalStateException("Reboot element not found on board!");
        return reboot;
    }

}

