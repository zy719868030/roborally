package de.lmu.dbs.ifi.sep25.network;

import de.lmu.dbs.ifi.sep25.game.BoardElement.BoardElement;

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

    public record BodyGameStarted(Integer energy, List<List<List<BoardElement>>> gameMap) {
    }

    public record BodySendChat(String message, Integer to) {
    } //public with to = -1

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
