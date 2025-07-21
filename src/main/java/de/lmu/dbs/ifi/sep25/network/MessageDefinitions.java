package de.lmu.dbs.ifi.sep25.network;

import java.util.List;

@SuppressWarnings("unused")
public class MessageDefinitions {

    private MessageDefinitions() {
    }

    /**
     * Generic message wrapper containing the message type and its body.
     *
     * @param messageType The type of the message (inferred from class name).
     * @param messageBody The body of the message.
     * @param <T>         The type of the message body.
     */
    public record Message<T>(String messageType, T messageBody) {
        /**
         * Convenience constructor that infers the message type from the body class name.
         *
         * @param messageBody The body of the message.
         */
        public Message(T messageBody) {
            this(messageBody.getClass().getSimpleName().replace("Body", ""), messageBody);
        }
    }

    /**
     * Sent by the client upon connection to declare the protocol version.
     */
    public record BodyHelloClient(String protocol) {
    }

    /**
     * Keep-alive message sent periodically.
     */
    public record BodyAlive() {
    }

    /**
     * Sent by the server during the initial handshake.
     */
    public record BodyHelloServer(String group, Boolean isAI, String protocol) {
    }

    /**
     * Sent by the server to assign a client ID.
     */
    public record BodyWelcome(Integer clientID) {
    }

    /**
     * Contains player name and figure ID.
     */
    public record BodyPlayerValues(String name, Integer figure) {
    }

    /**
     * Notifies that a player has been added to the lobby.
     */
    public record BodyPlayerAdded(Integer clientID, String name, Integer figure) {
    }

    /**
     * Sent by the client to indicate readiness.
     */
    public record BodySetStatus(Boolean ready) {
    }

    /**
     * Broadcast to all players showing a player’s readiness state.
     */
    public record BodyPlayerStatus(Integer clientID, Boolean ready) {
    }

    /**
     * Server sends list of maps and who should select the map.
     */
    public record BodySelectMap(List<String> availableMaps, int selectorID) {
    }

    /**
     * Client sends selected map name.
     */
    public record BodyMapSelected(String map) {
    }

    /**
     * Indicates the game has started, includes initial energy and full map.
     */
    public record BodyGameStarted(Integer energy, List<List<List<Field>>> gameMap) {
    }

    /**
     * Wrapper class for different types of game board fields.
     */
    public static abstract class Field {
        /**
         * Type of the field (e.g., Wall, Pit).
         */
        private final String type;
        /**
         * Board segment ID.
         */
        private final String isOnBoard;

        /**
         * Base constructor for all field types.
         *
         * @param isOnBoard Identifier for board region/segment.
         */
        public Field(String isOnBoard) {
            String type = this.getClass().getSimpleName().replace("Field", "");
            this.type = type.equals("EnergySpace") ? "Energy-Space" : type;
            this.isOnBoard = isOnBoard;
        }

        public String type() {
            return type;
        }

        public String isOnBoard() {
            return isOnBoard;
        }
    }

    /**
     * Represents an empty tile on the board.
     */
    public static class FieldEmpty extends Field {
        public FieldEmpty(String isOnBoard) {
            super(isOnBoard);
        }
    }

    /**
     * Starting point tile (with label A/B/...).
     */
    public static class FieldStartPoint extends Field {
        public FieldStartPoint(String isOnBoard, String label) {
            super(isOnBoard);
        }
    }

    /**
     * Represents a conveyor belt tile.
     */
    public static class FieldConveyorBelt extends Field {
        private final Integer speed;
        private final List<String> orientations;

        /**
         * @param isOnBoard    Board segment ID.
         * @param speed        1 = green; 2 = blue.
         * @param orientations First = push direction; remaining = pull directions.
         */
        public FieldConveyorBelt(String isOnBoard, Integer speed, List<String> orientations) {
            super(isOnBoard);
            if (orientations.size() < 2)
                throw new IllegalArgumentException("Conveyor requires at least 2 orientations");
            this.speed = speed;
            this.orientations = orientations;
        }

        public Integer speed() {
            return speed;
        }

        public List<String> orientations() {
            return orientations;
        }
    }

    /**
     * Push panel that activates on specific registers.
     */
    public static class FieldPushPanel extends Field {
        private final List<String> orientations;
        private final List<Integer> registers;

        /**
         * @param isOnBoard    Board segment ID.
         * @param orientations Push directions.
         * @param registers    Active registers (e.g. 2, 4).
         */
        public FieldPushPanel(String isOnBoard, List<String> orientations, List<Integer> registers) {
            super(isOnBoard);
            if (orientations.isEmpty())
                throw new IllegalArgumentException("PushPanel requires at least 1 orientation");
            this.orientations = orientations;
            this.registers = registers;
        }

        public List<String> orientations() {
            return orientations;
        }

        public List<Integer> registers() {
            return registers;
        }
    }

    /**
     * Gear tile which rotates robots.
     */
    public static class FieldGear extends Field {
        private final List<String> orientations;

        /**
         * @param isOnBoard    Board segment ID.
         * @param orientations Rotation direction: "clockwise" or "counterclockwise".
         */
        public FieldGear(String isOnBoard, List<String> orientations) {
            super(isOnBoard);
            if (orientations.size() != 1)
                throw new IllegalArgumentException("Gear requires 1 orientation");
            this.orientations = orientations;
        }

        public List<String> orientations() {
            return orientations;
        }
    }

    /**
     * Pit field: causes robot to die.
     */
    public static class FieldPit extends Field {
        public FieldPit(String isOnBoard) {
            super(isOnBoard);
        }
    }

    /**
     * Energy space tile that contains energy cubes.
     */
    public static class FieldEnergySpace extends Field {
        private final Integer count;

        /**
         * @param isOnBoard Board segment ID.
         * @param count     Number of energy cubes on tile.
         */
        public FieldEnergySpace(String isOnBoard, Integer count) {
            super(isOnBoard);
            this.count = count;
        }

        public Integer count() {
            return count;
        }
    }

    /**
     * Wall field blocking robot movement.
     */
    public static class FieldWall extends Field {
        private final List<String> orientations;

        /**
         * @param isOnBoard    Board segment ID.
         * @param orientations Blocked directions.
         */
        public FieldWall(String isOnBoard, List<String> orientations) {
            super(isOnBoard);
            if (orientations.isEmpty())
                throw new IllegalArgumentException("Wall requires at least 1 orientation");
            this.orientations = orientations;
        }

        public List<String> orientations() {
            return orientations;
        }
    }

    /**
     * Laser field that damages robots in a direction.
     */
    public static class FieldLaser extends Field {
        private final Integer count;
        private final List<String> orientations;

        public FieldLaser(String isOnBoard, List<String> orientations, Integer count) {
            this(isOnBoard, orientations, count, true);
        }

        /**
         * @param isOnBoard    Board segment ID.
         * @param orientations Laser direction (must be 1).
         * @param count        Number of beams (1–3).
         * @param active       Whether laser is active (unused).
         */
        public FieldLaser(String isOnBoard, List<String> orientations, Integer count, boolean active) {
            super(isOnBoard);
            if (orientations.size() != 1)
                throw new IllegalArgumentException("Laser requires exactly 1 orientation");
            if (count < 1 || count > 3)
                throw new IllegalArgumentException("Laser requires count between 1 and 3");

            this.orientations = orientations;
            this.count = count;
        }

        public Integer count() {
            return count;
        }

        public List<String> orientations() {
            return orientations;
        }
    }

    /**
     * Antenna field for remote control signal.
     */
    public static class FieldAntenna extends Field {
        private final List<String> orientations;

        /**
         * @param isOnBoard    Board segment ID.
         * @param orientations Direction of signal (must be 1).
         */
        public FieldAntenna(String isOnBoard, List<String> orientations) {
            super(isOnBoard);
            if (orientations.size() != 1)
                throw new IllegalArgumentException("Antenna requires exactly 1 orientation");
            this.orientations = orientations;
        }

        public List<String> orientations() {
            return orientations;
        }
    }

    /**
     * Checkpoint tile with a number.
     */
    public static class FieldCheckPoint extends Field {
        private final Integer count;

        /**
         * @param isOnBoard Board segment ID.
         * @param count     Checkpoint number (>0).
         */
        public FieldCheckPoint(String isOnBoard, Integer count) {
            super(isOnBoard);
            if (count < 1)
                throw new IllegalArgumentException("Checkpoint number requires to be positive");
            this.count = count;
        }

        public Integer count() {
            return count;
        }
    }

    /**
     * Restart point for player respawn.
     */
    public static class FieldRestartPoint extends Field {
        private final List<String> orientations;

        public FieldRestartPoint(String isOnBoard, List<String> orientations) {
            super(isOnBoard);
            if (orientations.size() != 1)
                throw new IllegalArgumentException("RestartPoint requires exactly 1 orientation");
            this.orientations = orientations;
        }

        public List<String> orientations() {
            return orientations;
        }
    }

    /**
     * Sent by client to send a chat message.
     */
    public record BodySendChat(String message, Integer to) {
    }

    /**
     * Server broadcasts received chat message.
     */
    public record BodyReceivedChat(String message, Integer from, Boolean isPrivate) {
    }

    /**
     * Server reports an error to the client.
     */
    public record BodyError(String error) {
    }

    /**
     * Player (dis)connects or joins/leaves.
     */
    public record BodyConnectionUpdate(Integer clientID, Boolean isConnected, String action) {
    }

    public record BodyPlayCard(String card) {
    }

    public record BodyCardPlayed(Integer clientID, String card) {
    }

    public record BodyCurrentPlayer(Integer clientID) {
    }

    public record BodyActivePhase(Integer phase) {
    }

    public record BodySetStartingPoint(Integer x, Integer y, String direction) {
    }

    public record BodyStartingPointTaken(Integer x, Integer y, String direction, Integer clientID) {
    }

    public record BodyYourCards(List<String> cardsInHand) {
    }

    public record BodyNotYourCards(Integer clientID, Integer cardsInHand) {
    }

    public record BodyShuffleCoding(Integer clientID) {
    }

    public record BodySelectedCard(String card, Integer register) {
    }

    public record BodyCardSelected(Integer clientID, Integer register, Boolean filled) {
    }

    public record BodySelectionFinished(Integer clientID) {
    }

    public record BodyTimerStarted() {
    }

    public record BodyTimerEnded(List<Integer> clientIDs) {
    }

    public record BodyCardsYouGotNow(List<String> cards) {
    }

    public record BodyCurrentCards(List<ActiveCard> activeCards) {
    }

    /**
     * Represents an active card played by a player.
     */
    public record ActiveCard(Integer clientID, String card) {
    }

    public record BodyReplaceCard(Integer register, String newCard, Integer clientID) {
    }

    public record BodyMovement(Integer clientID, Integer x, Integer y) {
    }

    public record BodyPlayerTurning(Integer clientID, String rotation) {
    }

    public record BodyDrawDamage(Integer clientID, List<String> cards) {
    }

    public record BodyPickDamage(Integer count, List<String> availablePiles) {
    }

    public record BodySelectedDamage(List<String> cards) {
    }

    public record BodyAnimation(String type) {
    }

    public record BodyReboot(Integer clientID) {
    }

    public record BodyRebootDirection(String direction) {
    }

    public record BodyEnergy(Integer clientID, Integer count, String source) {
    }

    public record BodyCheckPointReached(Integer clientID, Integer number) {
    }

    public record BodyGameFinished(Integer clientID) {
    }

    /**
     * Informs clients about a player name change.
     */
    public record BodyPlayerRenamed(int clientID, String newName) {
    }
}