package net.runelite.client.plugins.mywebsocket;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.runelite.api.Client;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.game.walking.GlobalCollisionMap;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import net.runelite.client.game.walking.Pathfinder;
import net.runelite.client.game.CollisionMap;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class MyPythonConnection extends WebSocketClient {
    private final MyWebsocketPlugin plugin;
    private final WebSocket socket;
    private boolean isConnected;
    private static CollisionMap collisionMap = null;
    private final Client client;

    public MyPythonConnection(URI serverUri, Draft draft, MyWebsocketPlugin plugin) {
        super(serverUri, draft);
        this.socket = this.getConnection();
        this.plugin = plugin;
        this.client = plugin.client;
    }

    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        isConnected = true;
        System.out.println("opened connection");
    }

    @Override
    public void onMessage(String text) {
        JsonObject obj = (new Gson()).fromJson(text, JsonObject.class);
        String event = obj.get("type").getAsString();
        JSONObject payloadObject;
        switch (event) {
            case "greeting":
                payloadObject = new JSONObject();
                payloadObject.put("greeting", "Hello back!");
                sendMessage(payloadObject);
                break;
            case "pathfind":
                JsonObject data = obj.get("data").getAsJsonObject();
                String path = findPath(data);
                JSONObject pathObject = new JSONObject();
                pathObject.put("path", path);
                sendMessage(pathObject);
                break;
        }
    }

    @Override
    public void onClose(int i, String s, boolean b) {
        isConnected = false;
        System.out.println("closed connection");
    }

    @Override
    public void onError(Exception e) {
        System.err.println("An error occurred: " + e.getMessage());
    }

    public void sendMessage(JSONObject message) {
        System.out.println("sending message: " + message.toString());
        this.socket.send(message.toString());
    }

    public String findPath(JsonObject data) {
        int startX = data.get("startX").getAsInt();
        int startY = data.get("startY").getAsInt();
        int endX = data.get("endX").getAsInt();
        int endY = data.get("endY").getAsInt();
        WorldPoint start = new WorldPoint(startX, startY, client.getPlane());
        WorldPoint end = new WorldPoint(endX, endY, client.getPlane());
        try {
            BufferedInputStream input = new BufferedInputStream(MyPythonConnection.class.getResourceAsStream("regions"));
            try { GZIPInputStream gzip = new GZIPInputStream(input);
                try { collisionMap = new GlobalCollisionMap(gzip.readAllBytes());
                    gzip.close();
                    input.close();} catch (Throwable throwable) { try { gzip.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }  } catch (Throwable throwable) { try { input.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        Pathfinder pathfinder = new Pathfinder(collisionMap, start, end);
        List<WorldPoint> path = pathfinder.find();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (WorldPoint point : path) {
            int x = point.getX();
            int y = point.getY();
            stringBuilder.append("WorldPoint.from_tuple((").append(x).append(", ").append(y).append(")), ");
        }
        // Remove the trailing comma and space from the last element
        if (!path.isEmpty()) {
            stringBuilder.setLength(stringBuilder.length() - 2);
        }
        stringBuilder.append("]");

        String result = stringBuilder.toString();
        return result;
    }
}