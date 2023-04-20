package net.runelite.client.plugins.myplugin;

import com.google.inject.Provides;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.inject.Inject;

import lombok.AccessLevel;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.callback.ClientThread;
import org.java_websocket.drafts.Draft_6455;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.json.JSONObject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.util.ImageUtil;


@Slf4j
@PluginDescriptor(
        name = "State Data"
)
public class StateDataPlugin extends Plugin {
    private static final int DESIRED_PITCH = 512;
    private static final int DESIRED_YAW = 0;
    @Getter
    private final Set<GameObject> treeObjects = new HashSet<>();
    @Getter(AccessLevel.PACKAGE)
    private final List<TreeRespawn> respawns = new ArrayList<>();
    public boolean isMenuOpened;
    public MenuOpened menuOpened;
    @Inject
    protected Client client;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private StateDataConfig config;
    private StateDataPanel panel;
    private NavigationButton navButton;
    private WorldPoint lastTickLocation;
    private ItemContainer previousInventory;
    @Inject
    private ClientThread clientThread;
    private PythonConnection ws;
    private PythonConnection state;
    private Properties props;
    private GameEnvironment ga;
    private int currentPlane;
    private JSONObject obj;

    private static WidgetItem getWidgetItem(Widget parentWidget, int idx) {
        assert parentWidget.isIf3();
        Widget wi = parentWidget.getChild(idx);
        return new WidgetItem(wi.getItemId(), wi.getItemQuantity(), wi.getBounds(), parentWidget, wi.getBounds());
    }

    @Provides
    StateDataConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(StateDataConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        if (this.clientThread != null) {
            clientThread.invoke(() ->
            {
                Widget settingsInit = client.getWidget(WidgetInfo.SETTINGS_INIT);
                if (settingsInit != null) {
                    client.createScriptEvent(settingsInit.getOnLoadListener())
                            .setSource(settingsInit)
                            .run();
                    clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, 433, 433));
                }
            });
        }

        panel = injector.getInstance(StateDataPanel.class);
        panel.init(config);
        log.info("Example started!");
        ga = new GameEnvironment(client);
        isMenuOpened = false;


        props = new Properties();
        try (InputStream input = new FileInputStream("config.properties")) {
            props.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        ws = new PythonConnection(new URI("ws://localhost:8765"), new Draft_6455(), this);
        ws.connect();

        state = new PythonConnection(new URI("ws://localhost:8766"), new Draft_6455(), this);
        state.connect();

        final BufferedImage icon = ImageUtil.loadImageResource(getClass(), "combaticon.png");

        navButton = NavigationButton.builder()
                .tooltip("State Data")
                .icon(icon)
                .priority(7)
                .panel(panel)
                .build();

        clientToolbar.addNavigation(navButton);
        //previousInventory = client.getItemContainer(InventoryID.INVENTORY);
    }

    @Override
    protected void shutDown() throws Exception {
        log.info("Example stopped!");
        treeObjects.clear();
        panel = null;
        clientToolbar.removeNavigation(navButton);
        ws.close();
        previousInventory = null;
        treeObjects.clear();
    }

    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getItemContainer() == client.getItemContainer(InventoryID.INVENTORY)) {
            previousInventory = null;
        }
    }

    @Subscribe
    public void onGameTick(GameTick t) throws IOException, URISyntaxException {
        if (client.getGameState() != GameState.LOGGED_IN) {
            lastTickLocation = null;
        } else {
            obj = new JSONObject();
            int tick = client.getTickCount();
            if (client.getCameraZ() != 433) {
                clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, 433, 433));
            }

            currentPlane = client.getPlane();

            lastTickLocation = client.getLocalPlayer().getWorldLocation();

            //obj.put("valid_movements", ga.getValidMovementLocationsAsString(client, lastTickLocation, 10));
            //obj.put("inventory", getInventoryAsString());

            //ws.send(obj.toString());
            if (state.isConnected()) {
                try {
                    PythonConnection.sendPlayerData(client, state, new JSONObject());
                    PythonConnection.sendEnvironmentData(client, state, new JSONObject());
                    state.getInventoryItems();
                    obj.put("tick", tick);
                    state.send(obj.toString());
                } catch (WebsocketNotConnectedException e) {
                    System.err.println("WebSocket not connected: " + e.getMessage());
                }
            } else try {
                state = new PythonConnection(new URI("ws://localhost:8766"), new Draft_6455(), this);
                state.connect();
            } catch (Exception e) {
                System.out.println("Cannot connect to websocket: " + e.getMessage());
            }
            //getCompassPosition();
            //LocalPoint p = LocalPoint.fromWorld(client, 3234, 3231);
            //sendTileClickbox(ws, p);
            //Map<WorldPoint, Tile> m = findTreeTiles();
            //ws.send(status.toString());
        }
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) {
        if (gameStateChanged.getGameState() == GameState.LOGGED_IN) {
            client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "Example says " + config.gameStateData(), null);
        }
    }

    //@Subscribe
    //public void onInteractingChanged(InteractingChanged interactingChanged) {
    //if (interactingChanged.getSource() == client.getLocalPlayer()) {
    //obj = new JSONObject();
    //obj.put("type", "interaction");
    //if (interactingChanged.getTarget() != null) {
    //obj.put("interacting_with", interactingChanged.getTarget());
    //obj.put("is_interacting", true);
    //obj.put("is_interacting", false);
    //ws.send(obj.toString());
    //}
    //}

    @Subscribe
    public void onAnimationChanged(AnimationChanged animationChanged) {
        if (animationChanged.getActor() == client.getLocalPlayer()) {
            //obj = new JSONObject();
            int animationID = client.getLocalPlayer().getAnimation();
            //obj.put("type", "animation");
            //obj.put("animationID", animationID);
            //ws.send(obj.toString());
        }
    }

    @Subscribe
    public void onMenuOpened(MenuOpened m) {
        isMenuOpened = true;
        menuOpened = m;
        System.out.println("menu opened " + m.getFirstEntry().getType());
    }

    @Subscribe
    public void onGameObjectDespawned(final GameObjectDespawned event) {
        final GameObject object = event.getGameObject();

        Tree tree = Tree.findTree(object.getId());
        if (tree != null) {
            if (tree.getRespawnTime() != null && currentPlane == object.getPlane()) {
                log.debug("Adding respawn timer for {} tree at {}", tree, object.getLocalLocation());

                Point min = object.getSceneMinLocation();
                WorldPoint base = WorldPoint.fromScene(client, min.getX(), min.getY(), client.getPlane());
                TreeRespawn treeRespawn = new TreeRespawn(tree, object.sizeX() - 1, object.sizeY() - 1,
                        base, Instant.now(), (int) tree.getRespawnTime(base.getRegionID()).toMillis());
                respawns.add(treeRespawn);
            }

            if (tree == Tree.REDWOOD) {
                treeObjects.remove(event.getGameObject());
            }
        }
    }

    Map<WorldPoint, Tile> findTreeTiles() {
        Map<WorldPoint, Tile> regularTreeTiles = new HashMap<>();
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        for (int x = 0; x < scene.getTiles()[client.getPlane()].length; x++) {
            for (int y = 0; y < scene.getTiles()[client.getPlane()][x].length; y++) {
                Tile tile = tiles[client.getPlane()][x][y];
                if (tile != null) {
                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject != null) {
                            Tree tree = Tree.findTree(gameObject.getId());
                            if (tree == Tree.REGULAR_TREE) {
                                regularTreeTiles.put(tile.getWorldLocation(), tile);
                            }
                        }
                    }
                }
            }
        }

        for (Map.Entry<WorldPoint, Tile> entry : regularTreeTiles.entrySet()) {
            System.out.println(entry.getKey().getX() + ", " + entry.getKey().getY());
            //System.out.println(entry.getValue().getWorldLocation().getX() + ", " + entry.getValue().getWorldLocation().getY());
        }

        return regularTreeTiles;
    }

    public String getInventoryAsString() {
        ItemContainer inventory = client.getItemContainer(InventoryID.INVENTORY);
        if (inventory != null) {
            Item[] items = inventory.getItems();
            String[] fixedSizeItems = new String[28];
            Arrays.fill(fixedSizeItems, "-1");
            for (int i = 0; i < items.length && i < fixedSizeItems.length; i++) {
                if (items[i] != null) {
                    fixedSizeItems[i] = String.valueOf(items[i].getId());
                }
            }
            String inventoryString = Arrays.stream(fixedSizeItems)
                    .map(id -> String.format("%2s", id))
                    .collect(Collectors.joining(", ", "[", "]"));
            return inventoryString;
        } else {
            String inventoryString = IntStream.range(0, 28)
                    .mapToObj(i -> String.valueOf(-1))
                    .collect(Collectors.joining(", ", "[", "]"));
            return inventoryString;
        }
    }

    public void printCameraOrientation() {
        int pitch = client.getCameraPitch();
        int yaw = client.getCameraYaw();

        System.out.println("Camera Pitch: " + pitch);
        System.out.println("Camera Yaw: " + yaw);
    }

    public void setCameraOrientation() {
        if (client.getCameraPitch() != DESIRED_PITCH || client.getCameraYaw() != DESIRED_YAW) {
            System.out.println("sending camera mismatch");
            JSONObject obj = new JSONObject();
            obj.put("type", "camera_mismatch");
            ws.send(obj.toString());
        }
    }

    public void getCompassPosition() {
        ItemComposition compass = client.getItemDefinition(ItemID.COMPASS);
        System.out.println(compass.getXan2d() + ", " + compass.getYan2d());
        //Point loc = client.getLocalPlayer().getCanvasSpriteLocation();
    }

    public void getTileLocation() {
        if (client.getSelectedSceneTile() != null) {
            Tile selectedTile = client.getSelectedSceneTile();
            LocalPoint lp = LocalPoint.fromScene(selectedTile.getSceneLocation().getX(), selectedTile.getSceneLocation().getY());
            Point p = Perspective.localToCanvas(client, lp, client.getPlane());
            Polygon poly = Perspective.getCanvasTilePoly(client, lp);
            Rectangle bounds = poly.getBounds();
            System.out.println(bounds.getCenterX() + ", " + bounds.getCenterY());

            ItemLayer il = selectedTile.getItemLayer();
            if (il != null) {
                System.out.println(il.getCanvasLocation().getX() + ", " + il.getCanvasLocation().getY());
            }
            //System.out.println(p.getX() + " ," + p.getY());
        }
    }

    public void send1TickTiles(PythonConnection ws) {
        JSONObject payload = new JSONObject();
        payload.put("type", "oneTickTiles");
        WorldPoint[] tiles = get1TickTiles(client);
        String tilesString = Arrays.stream(tiles)
                .map(id -> String.format("%2s", id))
                .collect(Collectors.joining(", ", "[", "]"));

        payload.put("oneTickTiles", tilesString);
        ws.send(payload.toString());
    }

    public static WorldPoint[] get1TickTiles(Client client) {
        // Get the current player's tile
        WorldPoint playerTile = client.getLocalPlayer().getWorldLocation();

        // Set the x and y boundaries for the tiles
        int xStart = playerTile.getX() - 2;
        int xEnd = playerTile.getX() + 2;
        int yStart = playerTile.getY() - 2;
        int yEnd = playerTile.getY() + 2;

        // Calculate the tiles within the boundaries
        WorldPoint[] tiles = new WorldPoint[26];
        int counter = 0;
        for (int x = xStart; x <= xEnd; x++) {
            for (int y = yStart; y <= yEnd; y++) {
                tiles[counter] = new WorldPoint(x, y, client.getPlane());
                counter++;
                if (counter >= 26) {
                    break;
                }
            }
            if (counter >= 26) {
                break;
            }
        }

        return tiles;
    }

    public void sendTileClickbox(PythonConnection ws, LocalPoint tile) {
        JSONObject payload = new JSONObject();
        Polygon poly = Perspective.getCanvasTilePoly(client, tile);
        Rectangle bounds = poly.getBounds();
        int minX = bounds.x;
        int maxX = bounds.x + bounds.width;
        int minY = bounds.y;
        int maxY = bounds.y + bounds.height;
        String s = "[" + minX + ", " + maxX + "], [" + minY + ", " + maxY + "]";
        payload.put("tile_clickbox", s);
        System.out.println(s);
        ws.send(payload.toString());
    }

    public void sendConfigs() {
        Canvas c = client.getCanvas();
        JSONObject obj = new JSONObject();
        java.awt.Point canvasLoc = c.getLocationOnScreen();
        int w = canvasLoc.x + c.getWidth();
        int h = canvasLoc.y + c.getHeight();
        obj.put("type", "config");
        obj.put("canvas_pos", "[" + canvasLoc.x + ", " + canvasLoc.y + ", " + w + ", " + h + "]");
        ws.send(obj.toString());
    }

}
