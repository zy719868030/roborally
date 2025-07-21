package de.lmu.dbs.ifi.sep25.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;

import java.lang.reflect.Type;
import java.util.List;

public class FieldSerializer implements JsonSerializer<MessageDefinitions.Field> {
    @Override
    public JsonElement serialize(MessageDefinitions.Field src, Type typeOfSrc, JsonSerializationContext context) {
        JsonObject obj = new JsonObject();
        String typeName = src.type();

        // Shared field
        obj.addProperty("type", typeName);
        obj.addProperty("isOnBoard", src.isOnBoard());

        switch (typeName) {
            case "ConveyorBelt" -> {
                MessageDefinitions.FieldConveyorBelt f = (MessageDefinitions.FieldConveyorBelt) src;
                obj.addProperty("speed", f.speed());
                obj.add("orientations", context.serialize(f.orientations()));
            }
            case "PushPanel" -> {
                MessageDefinitions.FieldPushPanel f = (MessageDefinitions.FieldPushPanel) src;
                obj.add("orientations", context.serialize(f.orientations()));
                obj.add("registers", context.serialize(f.registers()));
            }
            case "Gear", "Wall", "Antenna" -> {
                List<String> orientations = switch (typeName) {
                    case "Gear" -> ((MessageDefinitions.FieldGear) src).orientations();
                    case "Wall" -> ((MessageDefinitions.FieldWall) src).orientations();
                    case "Antenna" -> ((MessageDefinitions.FieldAntenna) src).orientations();
                    default -> List.of(); // never reached
                };
                obj.add("orientations", context.serialize(orientations));
            }
            case "Laser" -> {
                MessageDefinitions.FieldLaser f = (MessageDefinitions.FieldLaser) src;
                obj.addProperty("count", f.count());
                obj.add("orientations", context.serialize(f.orientations()));
            }
            case "Energy-Space" -> {
                MessageDefinitions.FieldEnergySpace f = (MessageDefinitions.FieldEnergySpace) src;
                obj.addProperty("count", f.count());
            }
            case "CheckPoint" -> {
                MessageDefinitions.FieldCheckPoint f = (MessageDefinitions.FieldCheckPoint) src;
                obj.addProperty("count", f.count());
            }
            case "RestartPoint" -> {
                MessageDefinitions.FieldRestartPoint f = (MessageDefinitions.FieldRestartPoint) src;
                obj.add("orientations", context.serialize(f.orientations()));
            }
            // Other types (Empty, Pit, RestartPoint, StartPoint) have no extra fields
        }

        return obj;
    }
}
