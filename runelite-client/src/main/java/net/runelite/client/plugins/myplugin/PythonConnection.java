package net.runelite.client.plugins.myplugin;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.MenuOpened;
import net.runelite.api.widgets.Widget;
import org.java_websocket.WebSocket;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

public class PythonConnection extends WebSocketClient {
    private final StateDataPlugin plugin;
    private final WebSocket socket;
    private boolean isConnected;

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
        System.out.println(obj.toString());
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
                    //plugin.send1TickTiles(this);
                    break;
                case "config":
                    plugin.sendConfigs();
                    break;
                case "camera":
                    plugin.setCameraOrientation();
                    break;
                case "menu_option_coords":
                    if (plugin.isMenuOpened) {
                        sendMenuCoords(this, new JSONObject(), plugin.menuOpened);
                    }
                    break;
                case "tinderbox_index":
                    sendMessage("tinderbox_index", getInventoryIndex(ItemID.TINDERBOX));
                    break;
                case "inventory":
                    getInventoryItems();
                    break;
                case "indices":
                    int item = obj.get("data").getAsInt();
                    sendMessage("indices", getInventoryIndices(item));
                    break;
                case "trees":
                    Map<String, List<Double>> trees = plugin.findTreeTiles();
                    JSONObject treeObject = new JSONObject();
                    treeObject.put("type", "trees");
                    StringBuilder treeClickBoxes = new StringBuilder();
                    for (List<Double> clickbox : trees.values()) {
                        treeClickBoxes.append(String.format("[%f, %f], ", clickbox.get(0), clickbox.get(1)));
                    }
                    if (treeClickBoxes.length() > 2) {
                        treeClickBoxes.setLength(treeClickBoxes.length() - 2);
                    }
                    treeObject.put("data", treeClickBoxes.toString());
                    sendMessage(treeObject);
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

    public void sendMessage(JSONObject message) {
        System.out.println(message.toString());
        this.socket.send(message.toString());
    }

    public static JSONObject getPlayerData(Client client, PythonConnection ws, JSONObject obj) {
        if (obj == null) {
            obj = new JSONObject();
        }
        obj.put("hitpoints", client.getBoostedSkillLevel(Skill.HITPOINTS));
        obj.put("prayerpoints", client.getBoostedSkillLevel(Skill.PRAYER));
        obj.put("energy", client.getEnergy());
        obj.put("is_interacting", client.getLocalPlayer().isInteracting());
        obj.put("animation", client.getLocalPlayer().getAnimation());

        return obj;
    }

    public static JSONObject getEnvironmentData(Client client, JSONObject obj) {
        if (obj == null) {
            obj = new JSONObject();
        }
        int tick = client.getTickCount();
        WorldPoint lastTickLocation = client.getLocalPlayer().getWorldLocation();
        obj.put("location", "(" + lastTickLocation.getX() + ", " + lastTickLocation.getY() + ")");
        obj.put("oneTickTiles", StateDataPlugin.get1TickTilesString(client));
        obj.put("tick", tick);

        return obj;
    }

    public static void sendMenuCoords(PythonConnection ws, JSONObject obj, MenuOpened m) {
        if (obj == null) {
            obj = new JSONObject();
        }
        obj.put("type", "menu_option_coords");
        MenuEntry[] ms = m.getMenuEntries();
        System.out.println("ms size: " + ms.length);
        int index = ms.length;
        for (MenuEntry entry : ms) {
            System.out.println(entry.getType());
            Widget c = entry.getWidget();
            if (c != null) {
                int x = c.getCanvasLocation().getX() + 5;
                int y = c.getCanvasLocation().getY() + 25 + (17 * index);
                String name = entry.getOption();
                System.out.println(name + ": " + x + ", " + y);
                obj.put(name, "[" + x + ", " + y + "]");
            }
            index--;

        }
        ws.sendMessage(obj);
    }

    private void sendMessage(String type, int value) {
        JSONObject obj = new JSONObject();
        obj.put(type, value);

        this.socket.send(obj.toString());
    }

    private int getInventoryIndex(int itemID) {
        int index = -1;
        ItemContainer inven = plugin.client.getItemContainer(InventoryID.INVENTORY);
        if (inven != null) {
            Item[] items = inven.getItems();
            for (int i = 0; i < items.length; i++) {
                if (items[i].getId() == itemID) {
                    index = i;
                    break;
                }
            }
        }
        return index;
    }

    public JSONObject getInventoryItems() {
        ItemContainer inventory = plugin.client.getItemContainer(InventoryID.INVENTORY);
        Item[] items = inventory.getItems();
        Map<Integer, Map<String, Object>> inventoryItems = new HashMap<>();

        for (int i = 0; i < items.length; i++) {
            if (items[i] != null && items[i].getId() != -1) {
                int itemID = items[i].getId();
                int quantity = inventory.count(itemID);
                if (!inventoryItems.containsKey(itemID)) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("quantity", quantity);
                    List<Integer> indices = new ArrayList<>();
                    indices.add(i);
                    item.put("indices", indices);
                    inventoryItems.put(itemID, item);
                } else {
                    Map<String, Object> item = inventoryItems.get(itemID);
                    List<Integer> indices = (List<Integer>) item.get("indices");
                    indices.add(i);
                }
            }
        }
        JSONObject obj = new JSONObject();
        JSONObject obj2 = new JSONObject();
        ObjectMapper objectMapper = new ObjectMapper();

        for (Map.Entry<Integer, Map<String, Object>> entry : inventoryItems.entrySet()) {
            int itemID = entry.getKey();
            Map<String, Object> item = entry.getValue();
            int quantity = (int) item.get("quantity");
            List<Integer> indices = (List<Integer>) item.get("indices");
            String key = String.valueOf(itemID);
            String value = String.format("[%d,%s]", quantity, indices.stream().map(String::valueOf).collect(Collectors.joining(",")));
            try {
                Object keyJson = objectMapper.readValue(key, Object.class);
                Object valueJson = objectMapper.readValue(value, Object.class);
                String prettyKey = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(keyJson);
                String prettyValue = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(valueJson);
                obj.put(prettyKey, prettyValue);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        obj2.put("inventory", obj.toString());
        return obj;
    }

    private void sendMessage(String type, String data) {
        JSONObject obj = new JSONObject();
        obj.put(type, data);
        this.socket.send(obj.toString());
    }

    private String getInventoryIndices(int itemID) {
        int[] indices = new int[28];
        ItemContainer inven = plugin.client.getItemContainer(InventoryID.INVENTORY);
        if (inven != null) {
            Item[] items = inven.getItems();
            for (int i = 0; i < items.length; i++) {
                if (items[i].getId() == itemID) {
                    indices[i] = i;
                } else
                    indices[i] = -1;
            }

        }
        return Arrays.toString(indices);
    }

    public boolean isConnected() {
        return isConnected;
    }

}