package de.lmu.dbs.ifi.sep25.game.Maps;

/**
 * Enum representing the different types of maps available in the game.
 * Each map type corresponds to a specific board layout and associated game elements.
 * <p>
 * The enum provides a default map type labeled as "Dizzy Highway" and a series of
 * numbered maps for customization or varying game scenarios. The map names are intended
 * to be unique identifiers for board configuration and usage within the game logic.
 */
public enum MapType {
    DIZZY_HIGHWAY, EXTRA_CRISPY, LOST_BEARINGS, DEATH_TRAP,
    PASSING_LANE, CHOP_SHOP_CHALLENGE, UNDERTOW,
    HEAVY_MERGE_AREA, PILGRIMAGE, GEAR_STRIPPER, BURN_RUN;

    /**
     * Returns the string representation of the current enum value.
     * This method provides a user-friendly name for each specific map type.
     *
     * @return The string representation of the enum value, such as "Dizzy Highway", "Extra Crispy",
     * or "Lost Bearings".
     */
    public String toString() {
        return switch (this) {
            case DIZZY_HIGHWAY -> "Dizzy Highway";
            case EXTRA_CRISPY -> "Extra Crispy";
            case LOST_BEARINGS -> "Lost Bearings";
            case DEATH_TRAP -> "Death Trap";
            case PASSING_LANE -> "Passing Lane";
            case CHOP_SHOP_CHALLENGE -> "Chop Shop Challenge";
            case UNDERTOW -> "Undertow";
            case HEAVY_MERGE_AREA -> "Heavy Merge Area";
            case PILGRIMAGE -> "Pilgrimage";
            case GEAR_STRIPPER -> "Gear Stripper";
            case BURN_RUN -> "Burn Run";
        };
    }

    /**
     * Converts a given map name string into the corresponding MapType enum value.
     *
     * @param mapName The name of the map as a string. This value is case-insensitive
     *                and must match one of the predefined map names such as
     *                "dizzy highway", "extra crispy", etc.
     * @return The corresponding MapType enum value for the given map name.
     * @throws IllegalArgumentException If the mapName does not match any known map names.
     */
    public static MapType fromString(String mapName) {
        return switch (mapName.toLowerCase()) {
            case "dizzy highway" -> DIZZY_HIGHWAY;
            case "extra crispy" -> EXTRA_CRISPY;
            case "lost bearings" -> LOST_BEARINGS;
            case "death trap" -> DEATH_TRAP;
            case "passing lane" -> PASSING_LANE;
            case "chop shop challenge" -> CHOP_SHOP_CHALLENGE;
            case "undertow" -> UNDERTOW;
            case "heavy merge area" -> HEAVY_MERGE_AREA;
            case "pilgrimage" -> PILGRIMAGE;
            case "gear stripper" -> GEAR_STRIPPER;
            case "burn run" -> BURN_RUN;
            default -> throw new IllegalArgumentException("Unknown map name: " + mapName);
        };
    }

    /**
     * Determines the board style based on a given map type.
     * The method maps different types of maps to corresponding board styles.
     * For example, certain map types might represent "double", "reverse", "pilgrimage", etc.,
     * while others default to "normal".
     *
     * @param mapType The map type for which the board style needs to be determined.
     *                Must be a non-null value representing a specific type of map.
     * @return A string representing the board style associated with the given map type.
     *         Possible values include "double", "reverse", "pilgrimage", "undertow",
     *         "gearstripper", "burnrun", or "normal" if no specific mapping exists.
     * @throws IllegalArgumentException If the provided mapType is null.
     */
    public static String boardStyleFromMapType(MapType mapType) {
        if (mapType == null)
            throw new IllegalArgumentException("Map type cannot be null.");
        else if (mapType == PASSING_LANE || mapType == CHOP_SHOP_CHALLENGE) {
            return "double";
        } else if (mapType == UNDERTOW) {
            return "undertow";
        } else if (mapType == HEAVY_MERGE_AREA || mapType == DEATH_TRAP) {
            return "reverse";
        } else if (mapType == PILGRIMAGE) {
            return "pilgrimage";
        } else if (mapType == GEAR_STRIPPER) {
            return "gearstripper";
        } else if (mapType == BURN_RUN) {
            return "burnrun";
        } else {
            return "normal";
        }
    }
}