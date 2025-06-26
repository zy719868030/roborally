package de.lmu.dbs.ifi.sep25.network;

import java.util.List;

@SuppressWarnings("unused")
public class MessageDefinitions {

    private MessageDefinitions() {
    }

    public record Message<T>(String messageType, T messageBody) {
        /**
         * Secondary constructor for Message: infers messageType from class name
         **/
        public Message(T messageBody) {
            this(messageBody.getClass().getSimpleName().replace("Body", ""), messageBody);
        }
    }

    /**
     * Message Body's defined in the following. As per use cases in protocol documents.
     **/

    public record BodyHelloClient(String protocol) {
    }

    public record BodyAlive() {
    }

    public record BodyHelloServer(String group, Boolean isAI, String protocol) {
    }

    public record BodyWelcome(Integer clientID) {
    }

    public record BodyPlayerValues(String name, Integer figure) {
    }

    public record BodyPlayerAdded(Integer clientID, String name, Integer figure) {
    }

    public record BodySetStatus(Boolean ready) {
    }

    public record BodyPlayerStatus(Integer clientID, Boolean ready) {
    }

    public record BodySelectMap(List<String> availableMaps) {
    }

    public record BodyMapSelected(String map) {
    }

    public record BodyGameStarted(Integer energy, List<List<List<Field>>> gameMap) {
    }

    public static abstract class Field {
        private final String type;
        private final String isOnBoard;

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

    public static class FieldEmpty extends Field {
        public FieldEmpty(String isOnBoard) {
            super(isOnBoard);
        }
    }

    public static class FieldStartPoint extends Field {
        public FieldStartPoint(String isOnBoard, String label) {
            super(isOnBoard);
        }
    }


    public static class FieldConveyorBelt extends Field {
        private final Integer speed;
        private final List<String> directions;

        /**
         * @param directions minimum of 2 directions: first directions is the push direction, rest are pull directions
         * @param speed      1 == green | 2 == blue
         **/
        public FieldConveyorBelt(String isOnBoard, Integer speed, List<String> directions) {
            super(isOnBoard);
            if (directions.size() < 2)
                throw new IllegalArgumentException("Conveyor requires at least 2 orientations");
            this.speed = speed;
            this.directions = directions;

        }

        public Integer speed() {
            return speed;
        }

        public List<String> directions() {
            return directions;
        }
    }

    public static class FieldPushPanel extends Field {
        private final List<String> orientations;
        private final List<Integer> registers;

        /**
         * @param registers active on x register
         **/
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

    public static class FieldGear extends Field {
        private final List<String> orientations;

        /**
         * @param orientations "clockwise" | "counterclockwise"
         **/
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

    public static class FieldPit extends Field {
        public FieldPit(String isOnBoard) {
            super(isOnBoard);
        }
    }

    public static class FieldEnergySpace extends Field {
        private final Integer count;

        /**
         * @param count stored energy
         **/
        public FieldEnergySpace(String isOnBoard, Integer count) {
            super(isOnBoard);
            this.count = count;

        }
        public Integer getCount() {
            return count;
        }

        public Integer count() {
            return count;
        }
    }

    public static class FieldWall extends Field {
        private final List<String> orientations;

        /**
         * @param orientations directions which are walled off
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

    public static class FieldLaser extends Field {
        private final Integer count;
        private final List<String> orientations;
        private final boolean active;


        /**
         * @param orientations direction in which laser faces
         * @param count        laser number count (1-3)
         */
        public FieldLaser(String isOnBoard, List<String> orientations, Integer count) {
            this(isOnBoard, orientations, count, true); // Standardmäßig aktiv
        }

        public FieldLaser(String isOnBoard, List<String> orientations, Integer count, boolean active) {
            super(isOnBoard);
            if (orientations.size() != 1)
                throw new IllegalArgumentException("Laser requires exactly 1 orientation");
            if (count < 1 || count > 3)
                throw new IllegalArgumentException("Laser requires count between 1 and 3");

            this.orientations = orientations;
            this.count = count;
            this.active = active;
        }
        public boolean isActive() {
            return active;
        }


        public List<String> orientations() {
            return orientations;
        }

        public Integer count() {
            return count;
        }
    }

    public static class FieldAntenna extends Field {
        private final List<String> orientations;

        /**
         * @param orientations direction of signal (max size 1)
         **/
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

    public static class FieldCheckPoint extends Field {
        private final Integer count;

        /**
         * @param count checkpoint number (>0)
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

    public record BodySendChat(String message, Integer to) {
    }

    public record BodyReceivedChat(String message, Integer from, Boolean isPrivate) {
    }

    public record BodyError(String error) {
    }

    //1.0
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

    public record ActiveCard(Integer clientID, String card) {
    }

    public record BodyReplaceCard(Integer register, String newCard, Integer clientID) {
    }

    public record BodyMovement(Integer clientID, Integer x, Integer y) {
    }

    public record BodyPlayerTurning(Integer clientID, String rotation) {
    }

    //1.0
    public record BodyDrawDamage(Integer clientID, List<String> cards) {

    }

    //1.0
    public record BodyPickDamage(Integer count, List<String> availablePiles) {
    }

    // 1.0
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

    public record BodyPlayerRenamed(int clientID, String newName) {
    }//@SEBAS

}
