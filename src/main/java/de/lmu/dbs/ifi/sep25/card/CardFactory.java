package de.lmu.dbs.ifi.sep25.card;

import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCard;
import de.lmu.dbs.ifi.sep25.card.DamageCard.DamageCardPool;
import de.lmu.dbs.ifi.sep25.card.ProgrammingCard.ProgrammingCard;
import de.lmu.dbs.ifi.sep25.card.ProgrammingCard.RegularPro;
import de.lmu.dbs.ifi.sep25.card.ProgrammingCard.SpecialPro;
import de.lmu.dbs.ifi.sep25.card.UpgradeCard.UpgradeCard;
import de.lmu.dbs.ifi.sep25.game.Board;
import de.lmu.dbs.ifi.sep25.game.Player;
import de.lmu.dbs.ifi.sep25.game.Robot;

import java.util.List;

/**
 * Factory class for creating and identifying card instances by string identifiers.
 * <p>
 * Provides methods to construct {@link RegisterCard} implementations (programming,
 * special, and damage cards) based on their names, as well as utilities for looking
 * up cards in a player's hand or retrieving the canonical string for upgrade cards.
 * </p>
 */
public class CardFactory {

    /** Singleton pool used for drawing {@link DamageCard} instances. */
    private static final DamageCardPool damageCardPool = DamageCardPool.getInstance();

    /**
     * Creates a {@link RegisterCard} instance corresponding to the given card name.
     * <p>
     * Recognized names include:
     * <ul>
     *   <li>Programming cards: MoveI, MoveII, MoveIII, TurnLeft, TurnRight, UTurn,
     *       BackUp, PowerUp, Again</li>
     *   <li>Special cards: RepeatRoutine, SpeedRoutine, EnergyRoutine, SpamFolder,
     *       SandboxRoutine, WeaselRoutine</li>
     *   <li>Damage cards: Spam, Worm, Virus, Trojan</li>
     * </ul>
     * Returns {@code null} if the name is unrecognized or {@code null/empty}.
     * </p>
     *
     * @param cardName the string identifier of the desired card
     * @return a new {@link RegisterCard} with the specified behavior, or {@code null}
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
     * Returns the standard string identifier for a given {@link RegisterCard} instance.
     * <p>
     * Uses the card's runtime class and properties (e.g., distance for moves,
     * special effect keyword, or damage type) to reverse-map to one of the names
     * recognized by {@link #createCard(String)}. If no match is found, returns
     * {@code card.toString()} or {@code null} if the card is {@code null}.
     * </p>
     *
     * @param card the {@link RegisterCard} to name
     * @return the canonical card name, or {@code null} if the card is {@code null}
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
     * Searches the given hand (list) for a {@link RegisterCard} matching the specified name.
     *
     * @param cardName the name to search for (as returned by {@link #getCardName(RegisterCard)})
     * @param hand     the list of cards in the player's hand
     * @return the first matching {@link RegisterCard}, or {@code null} if none found
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

    /**
     * Create an upgrade card.
     * @param cardName Upgrade card name.
     * @return Corresponding upgrade card object.
     */
//    public static UpgradeCard createUpgradeCard(String cardName) {
//        if (cardName == null || cardName.isEmpty()) {
//            return null;
//        }
//
//        return switch (cardName) {
//            // Permanent Upgrade Card
//            case "AdminPrivilege" -> new AdminPrivilege();
//            case "BlueScreenOfDeath" -> new BlueScreenOfDeath();
//            case "Brakes" -> new Brakes();
//            case "CacheMemory" -> new CacheMemory();
//            case "CrabLegs" -> new CrabLegs();
//            case "CorruptionWave" -> new CorruptionWave();
//            case "DefragGizmo" -> new DefragGizmo();
//            case "DeflectorShield" -> new DeflectorShield();
//            case "DoubleBarrelLaser" -> new DoubleBarrelLaser();
//            case "Firewall" -> new Firewall();
//            case "HoverUnit" -> new HoverUnit();
//            case "MemoryStick" -> new MemoryStick();
//            case "MiniHowitzer" -> new MiniHowitzer();
//            case "ModularChassis" -> new ModularChassis();
//            case "PressorBeam" -> new PressorBeam();
//            case "RailGun" -> new RailGun();
//            case "RammingGear" -> new RammingGear();
//            case "RearLaser" -> new RearLaser();
//            case "Scrambler" -> new Scrambler();
//            case "SideArms" -> new SideArms();
//            case "TractorBeam" -> new TractorBeam();
//            case "TrojanNeedler" -> new TrojanNeedler();
//            case "VirusModule" -> new VirusModule();
//
//            // Temporary upgrade card
//            case "Boink" -> new Boink();
//            case "EnergyRoutine" -> new EnergyRoutineUpgrade();
//            case "Hack" -> new Hack();
//            case "ManualSort" -> new ManualSort();
//            case "MemorySwap" -> new MemorySwap();
//            case "Reboot" -> new RebootUpgrade();
//            case "Recharge" -> new Recharge();
//            case "Recompile" -> new Recompile();
//            case "Refresh" -> new Refresh();
//            case "RepeatRoutine" -> new RepeatRoutineUpgrade();
//            case "SandboxRoutine" -> new SandboxRoutineUpgrade();
//            case "SpamBlocker" -> new SpamBlocker();
//            case "SpamFolderRoutine" -> new SpamFolderRoutineUpgrade();
//            case "SpeedRoutine" -> new SpeedRoutineUpgrade();
//            case "Teleporter" -> new Teleporter();
//            case "WeaselRoutine" -> new WeaselRoutineUpgrade();
//            case "Zoop" -> new Zoop();
//
//            default -> null;
//        };
//    }


    /**
     * Returns the canonical string identifier for an {@link UpgradeCard} instance.
     * <p>
     * Uses the card's simple class name to map back to the expected name used
     * by clients or serialization. Known special cases (e.g., EnergyRoutineUpgrade
     * &rarr; "EnergyRoutine", RebootUpgrade &rarr; "Reboot") are handled explicitly.
     * </p>
     *
     * @param card the {@link UpgradeCard} to name
     * @return the upgrade card’s string identifier, or {@code null} if the card is {@code null}
     */
    public static String getUpgradeCardName(UpgradeCard card) {
        if (card == null) {
            return null;
        }

        // Return the corresponding name based on the class name
        String className = card.getClass().getSimpleName();
        return switch (className) {
            case "AdminPrivilege" -> "AdminPrivilege";
            case "BlueScreenOfDeath" -> "BlueScreenOfDeath";
            case "Brakes" -> "Brakes";
            case "EnergyRoutineUpgrade" -> "EnergyRoutine";
            case "RebootUpgrade" -> "Reboot";
            default -> className;
        };
    }

}