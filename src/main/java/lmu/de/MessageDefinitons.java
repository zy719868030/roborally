package lmu.de;

import java.util.List;

public class MessageDefinitons {

    public static record Message <T> (String messageType, T messageBody) {

        /**Secondary constructor for Message: infers messageType from class name**/
        public Message(T messageBody){
            this(messageBody.getClass().getSimpleName().replace("Body", ""), messageBody);
        }
    }

    /**Message Body's defined in the following. As per use cases in protocol documents.**/

    public static record BodyHelloClient(String protocol){}

    public static record BodyAlive(){}

    public static record BodyHelloServer(String group, Boolean isAI, String protocol){}

    public static record BodyWelcome(Integer clientID) {}

    public static record BodyPlayerValues(String name, Integer figure){}

    public static record BodyPlayerAdded(Integer clientID, String name, Integer figure) {}

    public static record BodySetStatus(Boolean ready){}

    public static record BodyPlayerStatus(Integer clientID, Boolean ready){}

    //FIXME maybe fix needed? depends on Map implementation
    public static record BodySelectMap(List<String> availableMaps){}

    public static record BodyMapSelected(String map){}

    //FIXME implement map design
    public static record BodyGameStarted(Integer energy, List<Object> gameMap){}

    public static record BodySendChat(String message, Integer to){} //public with to = -1

    public static record BodyReceivedChat(String message, Integer from, Boolean isPrivate){}

    public static record BodyError(String error){}

    public static record BodyPlayCard(String card){}

    public static record BodyCardPlayed(Integer clientID, String card){}

    public static record BodyCurrentPlayer(Integer clientID){}

    public static record BodyActivePhase(Integer phase){}

    //FIXME implement direction as a enum
    public static record BodySetStartingPoint(Integer x, Integer y, String direction){}

    public static record BodyStartingPointTaken(Integer x, Integer y, String direction, Integer clientID){}

    //FIXME replace List<String> with List<Card>
    public static record BodyYourCards(List<String> cardsInHand){}

    public static record BodyNotYourCards(Integer clientID, Integer cardsInHand){}

    public static record BodyShuffleCoding(Integer clientID){}

    public static record BodySelectedCard(String card, Integer register){}

    public static record BodyCardSelected(Integer clientID, Integer register, Boolean filled){}

    public static record BodySelectionFinished(Integer clientID){}

    public static record BodyTimerStarted(){}

    public static record BodyTimerEnded(List<Integer> clientIDs){}

    //FIXME replace String with Card
    public static record BodyCardsYouGotNow(List<String> cards){}

    public static record BodyCurrentCards(List<ActiveCard> activeCards){}

    //FIXME replace String with Card
    public static record ActiveCard(Integer clientID, String card){}

    //FIXME replace String with Card
    public static record BodyReplaceCard(Integer register, String newCard, Integer clientID){}

    public static record BodyMovement(Integer clientID, Integer x, Integer y){}

    //FIXME implement rotation as enum
    public static record BodyPlayerTurning(Integer clientID, String rotation){}

    //TODO optional
    public static record BodyAnimation(String type){}

    public static record BodyReboot(Integer clientID){}

    //FIXME implement direction as enum
    public static record BodyRebootDirection(String direction){}

    //FIXME implement source as enum (PowerUpCard, EnergySpace)
    public static record BodyEnergy(Integer clientID, Integer count, String source){}

    public static record BodyCheckPointReached(Integer clientID, Integer number){}

    public static record BodyGameFinished(Integer clientID){}

}
