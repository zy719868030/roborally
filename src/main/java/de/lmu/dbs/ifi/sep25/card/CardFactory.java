package de.lmu.dbs.ifi.sep25.card;

import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
import de.lmu.dbs.ifi.sep25.card.ProgrammingCard.ProgrammingCard;
import de.lmu.dbs.ifi.sep25.card.ProgrammingCard.RegularPro;
import de.lmu.dbs.ifi.sep25.card.ProgrammingCard.SpecialPro;
import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.List;

/**
 * Card factory class, responsible for creating various types of card objects and providing conversion between strings
 * and card objects.
 */
public class CardFactory {

    private static final DamageCardPool damageCardPool = DamageCardPool.getInstance();

    /**
     * Create a corresponding RegisterCard object based on the card name.
     *
     * @param cardName Card name.
     * @return The corresponding RegisterCard object. If there is no match, return null.
     */
    public static RegisterCard createCard(String cardName) {
        if (cardName == null || cardName.isEmpty()) {
            return null;
        }

        // Create corresponding card objects based on card names
        return switch (cardName) {
            // RegularPro
            case "MoveI" -> new RegularPro("Move 1 Space", "move", 1);
            case "MoveII" -> new RegularPro("Move 2 Spaces", "move", 2);
            case "MoveIII" -> new RegularPro("Move 3 Spaces", "move", 3);
            case "TurnLeft" -> new RegularPro("Turn Left", "turnleft", 0);
            case "TurnRight" -> new RegularPro("Turn Right", "turnright", 0);
            case "UTurn" -> new RegularPro("U-Turn", "uturn", 0);
            case "BackUp" -> new RegularPro("Back Up", "backup", 1);
            case "PowerUp" -> new RegularPro("Power Up", "powerup", 0);
            case "Again" -> new RegularPro("Again", "again", 0);

            // SpecialPro
            case "RepeatRoutine" -> new SpecialPro("Repeat Routine", "repeat", "repeat routine");
            case "SpeedRoutine" -> new SpecialPro("Speed Routine", "speed", "speed routine");
            case "EnergyRoutine" -> new SpecialPro("Energy Routine", "energy", "energy routine");
            case "SpamFolder" -> new SpecialPro("SPAM Folder", "spamfolder", "spam folder");
            case "SandboxRoutine" -> new SpecialPro("Sandbox Routine", "sandbox", "sandbox routine");
            case "WeaselRoutine" -> new SpecialPro("Weasel Routine", "weasel", "weasel routine");

            // DamageCard
            case "Spam" -> damageCardPool.getDamageCard(DamageCard.DamageType.SPAM);
            case "Worm" -> damageCardPool.getDamageCard(DamageCard.DamageType.WORM);
            case "Virus" -> damageCardPool.getDamageCard(DamageCard.DamageType.VIRUS);
            case "Trojan" -> damageCardPool.getDamageCard(DamageCard.DamageType.TROJAN_HORSE);

            default -> null;
        };
    }

    /**
     * Get the string identifier of the card.
     *
     * @param card Card object.
     * @return String identifier of the card.
     */
    public static String getCardName(RegisterCard card) {
        if (card == null) {
            return null;
        }

        // Return the corresponding name based on the card type.
        if (card instanceof RegularPro regularPro) {
            String actionType = regularPro.getActionType().toLowerCase();

            return switch (actionType) {
                case "move" -> {
                    int distance = regularPro.getDistance();
                    if (distance == 1) yield "MoveI";
                    else if (distance == 2) yield "MoveII";
                    else if (distance == 3) yield "MoveIII";
                    else yield "Move" + distance;
                }
                case "turnleft" -> "TurnLeft";
                case "turnright" -> "TurnRight";
                case "uturn" -> "UTurn";
                case "backup" -> "BackUp";
                case "powerup" -> "PowerUp";
                case "again" -> "Again";
                default -> card.toString();
            };
        } else if (card instanceof SpecialPro specialPro) {
            String specialEffect = specialPro.getSpecialEffect().toLowerCase();

            return switch (specialEffect) {
                case "repeat routine" -> "RepeatRoutine";
                case "speed routine" -> "SpeedRoutine";
                case "energy routine" -> "EnergyRoutine";
                case "spam folder" -> "SpamFolder";
                case "sandbox routine" -> "SandboxRoutine";
                case "weasel routine" -> "WeaselRoutine";
                default -> card.toString();
            };
        } else if (card instanceof DamageCard damageCard) {
            DamageCard.DamageType damageType = damageCard.getDamageType();

            return switch (damageType) {
                case SPAM -> "Spam";
                case WORM -> "Worm";
                case VIRUS -> "Virus";
                case TROJAN_HORSE -> "Trojan";
                default -> card.toString();
            };
        }

        // If the type cannot be determined, return the toString result.
        return card.toString();
    }

    /**
     * Search for cards with the corresponding name in the player's hand.
     *
     * @param cardName Card name.
     * @param hand Player's hand.
     * @return The card found. If no card is found, return null.
     */
    public static RegisterCard findCardInHand(String cardName, List<RegisterCard> hand) {
        if (cardName == null || hand == null || hand.isEmpty()) {
            return null;
        }

        for (RegisterCard card : hand) {
            if (getCardName(card).equals(cardName)) {
                return card;
            }
        }

        return null;
    }
}