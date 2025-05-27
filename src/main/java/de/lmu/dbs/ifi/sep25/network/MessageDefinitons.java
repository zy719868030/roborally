package de.lmu.dbs.ifi.sep25.network;

import java.util.List;

public class MessageDefinitons {

    public record Message <T> (String messageType, T messageBody) {

        /**Secondary constructor for Message: infers messageType from class name**/
        public Message(T messageBody){
            this(messageBody.getClass().getSimpleName().replace("Body", ""), messageBody);
        }
    }

    /**Message Body's defined in the following. As per use cases in protocol documents.**/

    public record BodyHelloClient(String protocol){}

    public record BodyAlive(){}

    public record BodyHelloServer(String group, Boolean isAI, String protocol){}

    public record BodyWelcome(Integer clientID) {}

    public record BodyPlayerValues(String name, Integer figure){}

    public record BodyPlayerAdded(Integer clientID, String name, Integer figure) {}

    public record BodySetStatus(Boolean ready){}

    public record BodyPlayerStatus(Integer clientID, Boolean ready){}

    //FIXME maybe fix needed? depends on Map implementation
    public record BodySelectMap(List<String> availableMaps){}

    public record BodyMapSelected(String map){}

    //FIXME implement map design
    public record BodyGameStarted(Integer energy, List<Object> gameMap){}

    public record BodySendChat(String message, Integer to){} //public with to = -1

    public record BodyReceivedChat(String message, Integer from, Boolean isPrivate){}

    public record BodyError(String error){}

    public record BodyPlayCard(String card){}

    public record BodyCardPlayed(Integer clientID, String card){}

    public record BodyCurrentPlayer(Integer clientID){}

    public record BodyActivePhase(Integer phase){}

    //FIXME implement direction as a enum
    public record BodySetStartingPoint(Integer x, Integer y, String direction){}

    public record BodyStartingPointTaken(Integer x, Integer y, String direction, Integer clientID){}

    //FIXME replace List<String> with List<Card>
    public record BodyYourCards(List<String> cardsInHand){}

    public record BodyNotYourCards(Integer clientID, Integer cardsInHand){}

    public record BodyShuffleCoding(Integer clientID){}

    public record BodySelectedCard(String card, Integer register){}

    public record BodyCardSelected(Integer clientID, Integer register, Boolean filled){}

    public record BodySelectionFinished(Integer clientID){}

    public record BodyTimerStarted(){}

    public record BodyTimerEnded(List<Integer> clientIDs){}

    //FIXME replace String with Card
    public record BodyCardsYouGotNow(List<String> cards){}

    public record BodyCurrentCards(List<ActiveCard> activeCards){}

    //FIXME replace String with Card
    public record ActiveCard(Integer clientID, String card){}

    //FIXME replace String with Card
    public record BodyReplaceCard(Integer register, String newCard, Integer clientID){}

    public record BodyMovement(Integer clientID, Integer x, Integer y){}

    //FIXME implement rotation as enum
    public record BodyPlayerTurning(Integer clientID, String rotation){}

    //TODO optional
    public record BodyAnimation(String type){}

    public record BodyReboot(Integer clientID){}

    //FIXME implement direction as enum
    public record BodyRebootDirection(String direction){}

    //FIXME implement source as enum (PowerUpCard, EnergySpace)
    public record BodyEnergy(Integer clientID, Integer count, String source){}

    public record BodyCheckPointReached(Integer clientID, Integer number){}

    public record BodyGameFinished(Integer clientID){}

}
