package de.lmu.dbs.ifi.sep25.utils;

import com.google.gson.*;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;

public class FieldDeserializer implements JsonDeserializer<MessageDefinitions.Field> {
    private static final Logger appLogger = LogManager.getLogger(FieldDeserializer.class);
    @Override
    public MessageDefinitions.Field deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
            throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String type = obj.get("type").getAsString();

//        appLogger.debug("[DESERIALIZER] Processing field type: {}", type);

        return switch (type) {
            case "Empty" -> context.deserialize(obj, MessageDefinitions.FieldEmpty.class);
            case "StartPoint" -> context.deserialize(obj, MessageDefinitions.FieldStartPoint.class);
            case "ConveyorBelt" -> context.deserialize(obj, MessageDefinitions.FieldConveyorBelt.class);
            case "PushPanel" -> context.deserialize(obj, MessageDefinitions.FieldPushPanel.class);
            case "Gear" -> context.deserialize(obj, MessageDefinitions.FieldGear.class);
            case "Pit" -> context.deserialize(obj, MessageDefinitions.FieldPit.class);
            case "Energy-Space" -> context.deserialize(obj, MessageDefinitions.FieldEnergySpace.class);
            case "Wall" -> context.deserialize(obj, MessageDefinitions.FieldWall.class);
            case "Laser" -> context.deserialize(obj, MessageDefinitions.FieldLaser.class);
            case "Antenna" -> context.deserialize(obj, MessageDefinitions.FieldAntenna.class);
            case "CheckPoint" -> context.deserialize(obj, MessageDefinitions.FieldCheckPoint.class);
            case "RestartPoint" -> context.deserialize(obj, MessageDefinitions.FieldRestartPoint.class);

            default -> throw new JsonParseException("Unknown field type: " + type);
        };
    }
}
