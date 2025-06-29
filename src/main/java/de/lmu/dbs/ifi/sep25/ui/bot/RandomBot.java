package de.lmu.dbs.ifi.sep25.ui.bot;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.lmu.dbs.ifi.sep25.ui.bot.SimpleRandomBot.*;
import de.lmu.dbs.ifi.sep25.network.ClientSingleton;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions;
import de.lmu.dbs.ifi.sep25.network.MessageDefinitions.*;
import de.lmu.dbs.ifi.sep25.utils.FieldDeserializer;
import de.lmu.dbs.ifi.sep25.utils.FieldSerializer;
import de.lmu.dbs.ifi.sep25.utils.JsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Random;

public abstract class RandomBot {
    protected final boolean isBot = true; // Default to true for standalone bot
    protected static final String[] BOT_NAMES = {"Bot1", "Bot2", "Bot3", "Bot4", "Bot5", "Bot6"};
    protected static final int[] AVAILABLE_FIGURES = {0, 1, 2, 3, 4, 5};
    protected final Random random = new Random();
    protected final Gson gson = new GsonBuilder()
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldDeserializer())
            .registerTypeAdapter(MessageDefinitions.Field.class, new FieldSerializer())
            .create();
    protected Socket socket;
    protected BufferedReader reader;
    protected PrintWriter writer;
    protected Integer id;

    public void start(String host, int port) throws IOException {
        socket = new Socket(host, port);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        sendMessage(new Message<>(new BodyHelloServer("Edle Eisbecher", true, "Version 0.1")));
        // Start listening immediately
        String name = isBot ? BOT_NAMES[random.nextInt(BOT_NAMES.length)] : "Bot1";
        int figure = AVAILABLE_FIGURES[random.nextInt(AVAILABLE_FIGURES.length)];
        sendMessage(new Message<>(new BodyPlayerValues(name, figure)));
        System.out.println("[SimpleRandomBot] Selected name: " + name + ", figure: " + figure);
        System.out.println("[RandomBot] Sent message: PlayerValues (name=" + name + ", figure=" + figure + ")");

        new Thread(this::listenForMessages).start();

        System.out.println("[RandomBot] Started on " + host + ":" + port);
    }

    protected void sendMessage(Message<?> message) {
        if (writer != null) {
            String json = gson.toJson(message);
            writer.println(json);
            writer.flush();
            System.out.println("[RandomBot] Sent: " + json);
        }
    }

    protected void sendMessage(String json) {
        if (writer != null) {
            writer.println(json);
            writer.flush();
            System.out.println("[RandomBot] Sent raw: " + json);
        }
    }

    protected void sendMessageSelf(Message<?> message) {
        System.out.println("[RandomBot] Self message: " + gson.toJson(message));
    }

    protected void listenForMessages() {
        try {
            String json;
            while ((json = reader.readLine()) != null) {
                System.out.println("[RandomBot] Received: " + json);
                try {
                    String messageType = JsonUtil.parseUnknown(json).messageType();
                    handleMessage(json, messageType);
                } catch (Exception e) {
                    System.err.println("[RandomBot] Error processing: " + json);
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            System.err.println("[RandomBot] Disconnected: " + e.getMessage());
            closeAll();
        }
    }

    protected abstract void handleMessage(String json, String messageType);

    protected void closeAll() {
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[RandomBot] Error closing: " + e.getMessage());
        }
    }

    public int getID() {
        return id != null ? id : -1;
    }
}