package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.annotations.SerializedName;

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

    public abstract class Field {
        private final String isOnBoard;

        public Field(String isOnBoard) {
            this.isOnBoard = isOnBoard;
        }

        public String isOnBoard() {
            return isOnBoard;
        }

        @SerializedName("type")
        public abstract String getType(); // virtual field for JSON
    }

    public class FieldEmpty extends Field {
        public FieldEmpty(String isOnBoard) {
            super(isOnBoard);
        }

        @Override
        public String getType() {
            return "Empty";
        }
    }

    public class FieldStartPoint extends Field {
        public FieldStartPoint(String isOnBoard) {
            super(isOnBoard);
        }

        @Override
        public String getType() {
            return "StartPoint";
        }
    }

    public class FieldConveyorBelt extends Field {
        private final Integer speed;
        private final List<String> directions;

        /**
         * @param directions minimum of 2 directions: first directions is the push direction, rest are pull directions
         * @param speed      1 == green | 2 == blue
         **/
        public FieldConveyorBelt(String isOnBoard, Integer speed, List<String> directions) {
            if (directions.size() < 2)
                throw new IllegalArgumentException("Conveyor requires at least 2 orientations");
            this.speed = speed;
            this.directions = directions;
            super(isOnBoard);
        }

        private Integer speed() {
            return speed;
        }

        private List<String> directions() {
            return directions;
        }

        @Override
        public String getType() {
            return "ConveyorBelt";
        }
    }

    public class FieldPushPanel extends Field {
        private final List<String> orientations;
        private final List<Integer> registers;

        /**
         * @param registers active on x register
         * **/
        public FieldPushPanel(String isOnBoard, List<String> orientations, List<Integer> registers) {
            if (orientations.isEmpty())
                throw new IllegalArgumentException("PushPanel requires at least 1 orientation");
            this.orientations = orientations;
            this.registers = registers;
            super(isOnBoard);
        }

        private List<String> orientations() {
            return orientations;
        }

        private List<Integer> registers() {
            return registers;
        }

        @Override
        public String getType() {
            return "PushPanel";
        }
    }

    public class FieldGear extends Field {
        private final List<String> orientations;

        /**
         * @param orientations "clockwise" | "counterclockwise"
         * **/
        public FieldGear(String isOnBoard, List<String> orientations) {
            if (orientations.size() != 1)
                throw new IllegalArgumentException("Gear requires 1 orientation");
            this.orientations = orientations;
            super(isOnBoard);
        }

        private List<String> orientations() {
            return orientations;
        }

        @Override
        public String getType() {
            return "Gear";
        }
    }

    public class FieldPit extends Field {
        public FieldPit(String isOnBoard) {
            super(isOnBoard);
        }

        @Override
        public String getType() {
            return "Pit";
        }
    }

    public class FieldEnergySpace extends Field {
        private final Integer count;

        /**
         * @param count stored energy
         * **/
        public FieldEnergySpace(String isOnBoard, Integer count) {
            this.count = count;
            super(isOnBoard);
        }

        private Integer count() {
            return count;
        }

        @Override
        public String getType() {
            return "Energy-Space";
        }
    }

    public class FieldWall extends Field {
        private final List<String> orientations;

        /**
         * @param orientations directions which are walled off
         */
        public FieldWall(String isOnBoard, List<String> orientations) {
            if (orientations.isEmpty())
                throw new IllegalArgumentException("Wall requires at least 1 orientation");
            this.orientations = orientations;
            super(isOnBoard);
        }

        private List<String> orientations() {
            return orientations;
        }

        @Override
        public String getType() {
            return "Wall";
        }
    }

    public class FieldLaser extends Field {
        private final Integer count;
        private final List<String> orientations;

        /**
         * @param orientations direction in which laser faces
         * @param count laser number count (1-3)
         */
        public FieldLaser(String isOnBoard, List<String> orientations, Integer count) {
            if (orientations.size() == 1)
                throw new IllegalArgumentException("Laser requires exactly 1 orientation");
            this.orientations = orientations;
            if (count < 1 || count > 3)
                throw new IllegalArgumentException("Laser requires count between 1 and 3");
            this.count = count;
            super(isOnBoard);
        }

        private List<String> orientations() {
            return orientations;
        }

        private Integer count() {
            return count;
        }

        @Override
        public String getType() {
            return "Laser";
        }
    }

    public class FieldAntenna extends Field {
        private final List<String> orientations;

        /**
         * @param orientations direction of signal (max size 1)
         * **/
        public FieldAntenna(String isOnBoard, List<String> orientations) {
            if (orientations.size() != 1)
                throw new IllegalArgumentException("Antenna requires exactly 1 orientation");
            this.orientations = orientations;
            super(isOnBoard);
        }

        private List<String> orientations() {
            return orientations;
        }

        @Override
        public String getType() {
            return "Antenna";
        }
    }

    public class FieldCheckpoint extends Field {
        private final Integer count;

        /**
         * @param count checkpoint number (>0)
         */
        public FieldCheckpoint(String isOnBoard, Integer count) {
            if (count >= 1)
                throw new IllegalArgumentException("Checkpoint number requires to be positive");
            this.count = count;
            super(isOnBoard);
        }

        private Integer count() {
            return count;
        }

        @Override
        public String getType() {
            return "Checkpoint";
        }
    }

    public class FieldRestartPoint extends Field {
        public FieldRestartPoint(String isOnBoard) {
            super(isOnBoard);
        }

        @Override
        public String getType() {
            return "RestartPoint";
        }
    }

    public record BodySendChat(String message, Integer to) {
    }

    public record BodyReceivedChat(String message, Integer from, Boolean isPrivate) {
    }

    public record BodyError(String error) {
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

}
