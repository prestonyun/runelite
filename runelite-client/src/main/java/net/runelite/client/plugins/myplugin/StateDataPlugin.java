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

    private int counter = 0;
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
            CollisionData c = client.getCollisionMaps()[client.getPlane()];
            LocalPoint lp = LocalPoint.fromWorld(client, lastTickLocation);
            //System.out.println(c.getFlags()[lp.getSceneX()][lp.getSceneY()]);

            if (state.isConnected()) {
                try {
                    if (counter < 1) {
                        findTreeTiles();
                        counter++;
                    }
                    findTreeTiles();
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

    public Map<String, List<Double>> findTreeTiles() {
        Map<String, List<Double>> regularTreeTiles = new HashMap<>();
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        for (int x = 0; x < scene.getTiles()[client.getPlane()].length; x++) {
            for (int y = 0; y < scene.getTiles()[client.getPlane()][x].length; y++) {
                Tile tile = tiles[client.getPlane()][x][y];
                if (tile != null) {
                    for (GameObject gameObject : tile.getGameObjects()) {
                        if (gameObject != null) {
                            Tree tree = Tree.findTree(gameObject.getId());
                            if (tree == Tree.REGULAR_TREE && gameObject.getClickbox() != null) {
                                Double xCoord = gameObject.getClickbox().getBounds2D().getCenterX();
                                Double yCoord = gameObject.getClickbox().getBounds2D().getCenterY();
                                String key = xCoord + "," + yCoord;
                                if (!regularTreeTiles.containsKey(key)) {
                                    regularTreeTiles.put(key, Arrays.asList(xCoord, yCoord));
                                }
                            }
                        }
                    }
                }
            }
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

    private int[][] getCollisionData() {
        if (client.getCollisionMaps() != null) {
            int plane = client.getPlane();
            CollisionData collisionData = client.getCollisionMaps()[plane];

            if (collisionData != null) {
                return collisionData.getFlags();
            }
        }

        return null;
    }

    // Check if the destination tile is reachable
    private boolean isTileReachable(WorldPoint destination) {
        int[][] collisionData = getCollisionData();
        if (collisionData == null) {
            return false;
        }

        LocalPoint playerLocalPoint = LocalPoint.fromWorld(client, client.getLocalPlayer().getWorldLocation());
        if (playerLocalPoint == null) {
            return false;
        }

        LocalPoint destinationLocalPoint = LocalPoint.fromWorld(client, destination);
        if (destinationLocalPoint == null) {
            return false;
        }

        return findPath(playerLocalPoint, destinationLocalPoint, collisionData) != null;
    }

    // Simple pathfinding method using Breadth-First Search (BFS)
    private LinkedList<Point> findPath(LocalPoint start, LocalPoint destination, int[][] collisionData) {
        Queue<Point> queue = new LinkedList<>();
        Map<Point, Point> cameFrom = new HashMap<>();
        Point startPoint = new Point(start.getSceneX(), start.getSceneY());
        Point destinationPoint = new Point(destination.getSceneX(), destination.getSceneY());

        queue.add(startPoint);
        cameFrom.put(startPoint, null);

        while (!queue.isEmpty()) {
            Point current = queue.poll();

            if (current.equals(destinationPoint)) {
                break;
            }

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    if (dx == 0 && dy == 0) {
                        continue;
                    }

                    Point neighbor = new Point(current.getX() + dx, current.getY() + dy);

                    if (isWalkable(current, neighbor, collisionData) && !cameFrom.containsKey(neighbor)) {
                        queue.add(neighbor);
                        cameFrom.put(neighbor, current);
                    }
                }
            }
        }

        if (!cameFrom.containsKey(destinationPoint)) {
            return null;
        }

        LinkedList<Point> path = new LinkedList<>();
        Point current = destinationPoint;

        while (current != null) {
            path.addFirst(current);
            current = cameFrom.get(current);
        }

        return path;
    }

    // Check if it is walkable between two adjacent scene points
    private boolean isWalkable(Point a, Point b, int[][] collisionData) {
        int ax = a.getX();
        int ay = a.getY();
        int bx = b.getX();
        int by = b.getY();

        if (ax < 0 || ax >= Constants.SCENE_SIZE || ay < 0 || ay >= Constants.SCENE_SIZE
                || bx < 0 || bx >= Constants.SCENE_SIZE || by < 0 || by >= Constants.SCENE_SIZE) {
            return false;
        }

        int deltaX = bx - ax;
        int deltaY = by - ay;

        if (Math.abs(deltaX) > 1 || Math.abs(deltaY) > 1) {
            // Only consider adjacent tiles
            return false;
        }

        int srcFlags = collisionData[ax][ay];
        int destFlags = collisionData[bx][by];

        if ((srcFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0 || (destFlags & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0) {
            // One or both tiles are blocked
            return false;
        }

        if (deltaX == 0) {
            // North or south move
            if ((deltaY == 1 && (srcFlags & CollisionDataFlag.BLOCK_MOVEMENT_SOUTH) != 0) || (deltaY == -1 && (srcFlags & CollisionDataFlag.BLOCK_MOVEMENT_NORTH) != 0)) {
                return false;
            }
        } else if (deltaY == 0) {
            // East or west move
            if ((deltaX == 1 && (srcFlags & CollisionDataFlag.BLOCK_MOVEMENT_EAST) != 0) || (deltaX == -1 && (srcFlags & CollisionDataFlag.BLOCK_MOVEMENT_WEST) != 0)) {
                return false;
            }
        } else {
            // Diagonal move
            if (deltaX == 1 && deltaY == 1) {
                // Moving southeast
                if ((srcFlags & (CollisionDataFlag.BLOCK_MOVEMENT_SOUTH | CollisionDataFlag.BLOCK_MOVEMENT_EAST)) != 0 || (destFlags & (CollisionDataFlag.BLOCK_MOVEMENT_NORTH | CollisionDataFlag.BLOCK_MOVEMENT_WEST)) != 0) {
                    return false;
                }
            } else if (deltaX == 1 && deltaY == -1) {
                // Moving northeast
                if ((srcFlags & (CollisionDataFlag.BLOCK_MOVEMENT_NORTH | CollisionDataFlag.BLOCK_MOVEMENT_EAST)) != 0 || (destFlags & (CollisionDataFlag.BLOCK_MOVEMENT_SOUTH | CollisionDataFlag.BLOCK_MOVEMENT_WEST)) != 0) {
                    return false;
                }
            } else if (deltaX == -1 && deltaY == 1) {
                // Moving southwest
                if ((srcFlags & (CollisionDataFlag.BLOCK_MOVEMENT_SOUTH | CollisionDataFlag.BLOCK_MOVEMENT_WEST)) != 0 || (destFlags & (CollisionDataFlag.BLOCK_MOVEMENT_NORTH | CollisionDataFlag.BLOCK_MOVEMENT_EAST)) != 0) {
                    return false;
                }
            } else if (deltaX == -1 && deltaY == -1) {
                // Moving northwest
                if ((srcFlags & (CollisionDataFlag.BLOCK_MOVEMENT_NORTH | CollisionDataFlag.BLOCK_MOVEMENT_WEST)) != 0 || (destFlags & (CollisionDataFlag.BLOCK_MOVEMENT_SOUTH | CollisionDataFlag.BLOCK_MOVEMENT_EAST)) != 0) {
                    return false;
                }
            }
        }

        return true;
    }

    private TileObject findTileObject(Tile tile, int id)
    {
        if (tile == null)
        {
            return null;
        }

        final GameObject[] tileGameObjects = tile.getGameObjects();
        final DecorativeObject tileDecorativeObject = tile.getDecorativeObject();
        final WallObject tileWallObject = tile.getWallObject();
        final GroundObject groundObject = tile.getGroundObject();

        if (objectIdEquals(tileWallObject, id))
        {
            return tileWallObject;
        }

        if (objectIdEquals(tileDecorativeObject, id))
        {
            return tileDecorativeObject;
        }

        if (objectIdEquals(groundObject, id))
        {
            return groundObject;
        }

        for (GameObject object : tileGameObjects)
        {
            if (objectIdEquals(object, id))
            {
                return object;
            }
        }

        return null;
    }

    private boolean objectIdEquals(TileObject tileObject, int id)
    {
        if (tileObject == null)
        {
            return false;
        }

        if (tileObject.getId() == id)
        {
            return true;
        }

        // Menu action EXAMINE_OBJECT sends the transformed object id, not the base id, unlike
        // all of the GAME_OBJECT_OPTION actions, so check the id against the impostor ids
        final ObjectComposition comp = client.getObjectDefinition(tileObject.getId());

        if (comp.getImpostorIds() != null)
        {
            for (int impostorId : comp.getImpostorIds())
            {
                if (impostorId == id)
                {
                    return true;
                }
            }
        }

        return false;
    }
}

