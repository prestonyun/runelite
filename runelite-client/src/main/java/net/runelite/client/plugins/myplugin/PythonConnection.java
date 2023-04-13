package net.runelite.client.plugins.myplugin;

import net.runelite.api.Client;
import net.runelite.api.MenuEntry;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import org.java_websocket.client.WebSocketClient;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.java_websocket.WebSocket;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;

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
        JsonObject obj = (new Gson()).fromJson(text, JsonObject.class);
        if (obj.has("type")) {
            JSONObject data, payloadObject;
            String event = obj.get("type").getAsString();
            switch (event) {
                case "greeting":
                    System.out.println("received greeting");
                    payloadObject = new JSONObject();
                    payloadObject.put("greeting", "Hello back!");
                    sendMessage(payloadObject);
                    System.out.println("responded!");
                    break;
                case "oneTickTiles":
                    plugin.send1TickTiles(this);
                    break;
                case "config":
                    plugin.sendConfigs();
                    break;
                case "player":
                    sendPlayerData(plugin.client, this, new JSONObject());
                    break;
                case "environment":
                    sendEnvironmentData(plugin.client, this, new JSONObject());
                    break;
                case "menu_option_coords":
                    if (plugin.isMenuOpened)
                    {
                        sendMenuCoords(plugin.client, this, new JSONObject(), plugin.menuOpened);
                    }
                    break;
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

    public static void sendMenuCoords(Client client, PythonConnection ws, JSONObject obj, MenuOpened m) {
        if (obj == null) {
            obj = new JSONObject();
        }
        JSONObject obj2 = new JSONObject();
        obj.put("type", "menu_option_coords");
        MenuEntry[] ms = m.getMenuEntries();
        for (MenuEntry entry : ms)
        {
            Widget c = entry.getWidget();
            int x = c.getCanvasLocation().getX();
            int y = c.getCanvasLocation().getY();
            String name = c.getName();
            System.out.println(c.getCanvasLocation().getX() + ", " + c.getCanvasLocation().getY());
            obj2.put(name, "[" + x + ", " + y + "]");
        }
        obj.put("coords", obj2);
        ws.sendMessage(obj);

    }

    public static void sendPlayerData(Client client, PythonConnection ws, JSONObject obj) {
        if (obj == null) {
            obj = new JSONObject();
        }
        obj.put("type", "player");
        obj.put("hitpoints", client.getBoostedSkillLevel(Skill.HITPOINTS));
        obj.put("prayerpoints", client.getBoostedSkillLevel(Skill.PRAYER));
        obj.put("energy", client.getEnergy());
        obj.put("is_interacting", client.getLocalPlayer().isInteracting());
        obj.put("animation", client.getLocalPlayer().getAnimation());

        ws.sendMessage(obj);
    }

    public static void sendEnvironmentData(Client client, PythonConnection ws, JSONObject obj) {
        if (obj == null) {
            obj = new JSONObject();
        }
        WorldPoint lastTickLocation = client.getLocalPlayer().getWorldLocation();
        WorldPoint[] oneTickTiles = StateDataPlugin.get1TickTiles(client);
        String tilesString = Arrays.stream(oneTickTiles)
                .map(id -> String.format("%2s", id))
                .collect(Collectors.joining(", ", "[", "]"));
        obj.put("type", "environment");
        obj.put("location", "[" + lastTickLocation.getX() + ", " + lastTickLocation.getY() + "]");
        obj.put("oneTickTiles", tilesString);

        ws.sendMessage(obj);
    }

}