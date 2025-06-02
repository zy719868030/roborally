package de.lmu.dbs.ifi.sep25.network;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Utility class for JSON serialization and deserialization of Message objects.
 */
public class JsonUtil {
    private static final Gson gson = new Gson();

    /**
     * Parses a JSON string into a Message object with a specific body type.
     *
     * @param json      the raw JSON string representing the message
     * @param bodyClass the class of the message body type
     * @param <T>       the type of the message body
     * @return a deserialized Message object
     */
    public static <T> MessageDefinitions.Message<T> parseMessage(String json, Class<T> bodyClass) {
        return gson.fromJson(json, TypeToken.getParameterized(MessageDefinitions.Message.class, bodyClass).getType());
    }

    public static MessageDefinitions.Message<?> parseUnknown(String json) {
        return gson.fromJson(json, new TypeToken<MessageDefinitions.Message<Object>>() {
        }.getType());
    }
}
