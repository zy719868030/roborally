package de.lmu.dbs.ifi.sep25.utils;

import com.google.gson.*;
import de.lmu.dbs.ifi.sep25.game.BoardElement.*;

import java.lang.reflect.Type;

public class BoardElementDeserializer implements JsonDeserializer<BoardElement> {

    @Override
    public BoardElement deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String type = obj.get("type").getAsString();

        return switch (type) { //FIXME should autofix once BoardElements are implemented
            case "Empty" -> context.deserialize(obj, Floor.class);
            case "StartPoint" -> context.deserialize(obj, StartPoint.class);
            case "ConveyorBelt" -> context.deserialize(obj, Belts.class);
            case "PushPanel" -> context.deserialize(obj, PushPanel.class);
            case "Gear" -> context.deserialize(obj, Gear.class);
            case "Pit" -> context.deserialize(obj, Pit.class);
            case "Energy-Space" -> context.deserialize(obj, EnergySpace.class);
            case "Wall" -> context.deserialize(obj, Wall.class);
            case "Laser" -> context.deserialize(obj, Laser.class);
            case "Antenna" -> context.deserialize(obj, Antenna.class);
            case "CheckPoint" -> context.deserialize(obj, CheckPoints.class);
            case "RestartPoint" -> context.deserialize(obj, Reboot.class);
            default -> throw new JsonParseException("Unknown BoardElement type: " + type);
        };
    }
}
