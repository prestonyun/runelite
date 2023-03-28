package net.runelite.client.plugins.myplugin;

import org.java_websocket.client.WebSocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;

public class PythonConnection extends WebSocketClient {
    private final StateDataPlugin plugin;

    public PythonConnection(URI serverUri, Draft draft, StateDataPlugin plugin) {
        super(serverUri, draft);
        this.socket = this.getConnection();
        this.plugin = plugin;
    }
    @Override
    public void onOpen(ServerHandshake serverHandshake) {
        isConnected = true;
        System.out.println("opened connection");
    }
    @Override
    public void onMessage(String text) {
        JsonObject obj = (JsonObject)(new Gson()).fromJson(text, JsonObject.class);
        if (obj.has("type")) {
            JSONObject data, payloadObject;
            String event = obj.get("type").toString();
            switch (event) {
                case "greeting":
                    payloadObject = new JSONObject();
                    payloadObject.put("greeting", "Hello back!");
                    sendMessage(payloadObject);
                    break;
                case "oneTickTiles":
                    plugin.send1TickTiles(this);
            }
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
    private WebSocket socket;

    private boolean isConnected;

    public boolean isConnected() {
        return isConnected;
    }

    private void sendMessage(JSONObject message) {
        this.socket.send(message.toString());
    }

}