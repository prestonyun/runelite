package net.runelite.client.plugins.mymorgclient;

import com.google.common.collect.ImmutableSet;
import com.google.gson.*;
import lombok.Getter;
import net.runelite.api.Point;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldArea;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import com.google.inject.Provides;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import net.runelite.client.plugins.mymorgclient.NPCAttributes;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.walking.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.http.api.RuneLiteAPI;
import org.apache.commons.lang3.tuple.Pair;
import org.json.JSONObject;


@PluginDescriptor(
        name = "Morg HTTP Client",
        description = "Actively logs the player status to localhost on port 8081.",
        tags = {"status", "stats"},
        enabledByDefault = true
)
@Slf4j
public class HttpServerPlugin extends Plugin {
    private static final Duration WAIT = Duration.ofSeconds(5);
    @Inject
    public Client client;
    @Inject
    public ClientThread clientThread;
    private int plantCounter;
    public Skill[] skillList;
    public XpTracker xpTracker;
    public Skill mostRecentSkillGained;
    public int tickCount = 0;
    public long startTime = 0;
    public long currentTime = 0;
    public int[] xp_gained_skills;
    @Inject
    public HttpServerConfig config;
    public HttpServer server;
    public int MAX_DISTANCE = 1200;
    public String msg;
    private final Set<String> seenRegions = new HashSet<>();
    private int plane = -1;
    @Getter
    private final LinkedHashSet<TitheFarmPlant> plants = new LinkedHashSet<>();
    @Getter
    private List<WorldPoint> activeCheckpointWPs;
    private WorldPoint lastTickWorldLocation;
    private boolean isRunning;
    private boolean activePathStartedLastTick;

    private boolean activePathMismatchLastTick;

    private boolean calcTilePathOnNextClientTick;
    private boolean activePathFound;
    public Pathmarker pathfinder;
    private List<WorldArea> npcBlockWAs;
    @Getter
    private List<WorldPoint> activePathTiles;
    @Getter
    private List<WorldPoint> activeMiddlePathTiles;
    @Getter
    private boolean pathActive;
    private WorldPoint lastClickedTile;
    private WorldPoint hoveredTile;
    private WorldPoint destinationTile;
    //Tempoross Game State:
    private static final int VARB_IS_TETHERED = 11895;
    private static final int TEMPOROSS_REGION = 12078;
    private static final int UNKAH_REWARD_POOL_REGION = 12588;
    private static final int UNKAH_BOAT_REGION = 12332;
    private static final int FIRE_ID = 37582;
    private static final int FIRE_SPREAD_MILLIS = 24000;
    private static final int FIRE_SPAWN_MILLIS = 9600;
    private static final int FIRE_SPREADING_SPAWN_MILLIS = 1200;
    private static final int WAVE_IMPACT_MILLIS = 7800;
    public static final int TEMPOROSS_HUD_UPDATE = 4075;
    public static final int STORM_INTENSITY = 350;
    public static final int MAX_STORM_INTENSITY = 350;
    private final Set<Integer> TEMPOROSS_GAMEOBJECTS = ImmutableSet.of(
            FIRE_ID, NullObjectID.NULL_41006, NullObjectID.NULL_41007, NullObjectID.NULL_41352,
            NullObjectID.NULL_41353, NullObjectID.NULL_41354, NullObjectID.NULL_41355, ObjectID.DAMAGED_MAST_40996,
            ObjectID.DAMAGED_MAST_40997, ObjectID.DAMAGED_TOTEM_POLE, ObjectID.DAMAGED_TOTEM_POLE_41011);
    private static final String WAVE_INCOMING_MESSAGE = "a colossal wave closes in...";
    private static final String WAVE_END_SAFE = "as the wave washes over you";
    private static final String WAVE_END_DANGEROUS = "the wave slams into you";
    private static final String TEMPOROSS_VULNERABLE_MESSAGE = "tempoross is vulnerable";
    @Getter
    private final Map<NPC, NPCAttributes> fishingSpots = new HashMap<>();
    private final List<GameObject> totemMap = new ArrayList<>();
    private boolean waveIsIncoming;
    private boolean nearRewardPool;
    private int previousRegion;
    private int phase = 1;
    private int uncookedFish = 0;
    private int cookedFish = 0;
    private int crystalFish = 0;
    private JsonObject lastAction = new JsonObject();

    private Instant waveIncomingStartTime;
    @Provides
    private HttpServerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HttpServerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        //MAX_DISTANCE = config.reachedDistance();
        hoveredTile = new WorldPoint(0, 0, 0);
        plantCounter = 0;
        activeCheckpointWPs = new ArrayList<>();
        activePathTiles = new ArrayList<>();
        activeMiddlePathTiles = new ArrayList<>();
        pathActive = false;
        activePathStartedLastTick = false;
        npcBlockWAs = new ArrayList<>();
        pathfinder = new Pathmarker(client, this);
        skillList = Skill.values();
        xpTracker = new XpTracker(this);
        server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/stats", this::handleStats);
        server.createContext("/tithefarm", this::titheFarmEvents);
        server.createContext("/inv", handlerForInv(InventoryID.INVENTORY));
        server.createContext("/equip", handlerForInv(InventoryID.EQUIPMENT));
        server.createContext("/events", this::handleEvents);
        server.createContext("/pathing", this::handlePathing);
        server.createContext("/tempoross", exchange -> {
            handleTempoross(exchange);
        });
        server.createContext("/objectId", exchange -> {
            getGameObjectIdHandler(exchange);
        });
        server.createContext("/path", exchange -> {
            findPath(exchange);
        });
        server.createContext("/clickbox", exchange -> {
            getClickbox(exchange);
        });
        server.createContext("/minimap", exchange -> {
            getMinimapLocation(exchange);
        });
        server.createContext("/validate", exchange -> {
            getClickboxVerification(exchange);
        });
        server.createContext("/reachable", this::getAllReachableTiles);

        server.setExecutor(Executors.newSingleThreadExecutor());
        startTime = System.currentTimeMillis();
        xp_gained_skills = new int[Skill.values().length];
        int skill_count = 0;
        server.start();
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) {
                continue;
            }
            xp_gained_skills[skill_count] = 0;
            skill_count++;
        }
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked menuOptionClicked) {
        JsonObject jb = new JsonObject();
        jb.addProperty("option", menuOptionClicked.getMenuOption());
        jb.addProperty("target", menuOptionClicked.getMenuTarget());
        jb.addProperty("id", menuOptionClicked.getId());
        jb.addProperty("action", menuOptionClicked.getMenuAction().toString());
        jb.addProperty("tick", client.getTickCount());
        lastAction = jb;
        switch (menuOptionClicked.getMenuAction()) {
            case WIDGET_TARGET_ON_GAME_OBJECT:
                //System.out.println("widget target on game object");
                //System.out.println(menuOptionClicked.getParam0());
                //System.out.println(menuOptionClicked.getParam1());
                break;
            case GAME_OBJECT_FIRST_OPTION:
            case GAME_OBJECT_SECOND_OPTION:
            case GAME_OBJECT_THIRD_OPTION:
            case WALK:
                Tile hovered = client.getSelectedSceneTile();
                if (hovered != null) {
                    hoveredTile = hovered.getWorldLocation();
                }
                break;
        }
    }
    @Subscribe
    public void onChatMessage(ChatMessage chatMessage) {
        if (chatMessage.getType() != ChatMessageType.GAMEMESSAGE)
        {
            return;
        }
        String message = chatMessage.getMessage().toLowerCase();
        if (message.contains(WAVE_INCOMING_MESSAGE))
        {
            waveIsIncoming = true;
        }
        else if (message.contains(WAVE_END_SAFE))
        {
            waveIsIncoming = false;
        }
    }

    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        if (NpcID.FISHING_SPOT_10569 == npcSpawned.getNpc().getId()) {
            fishingSpots.put(npcSpawned.getNpc(), new NPCAttributes(npcSpawned.getNpc().getName(), Instant.now(), npcSpawned.getNpc().getWorldLocation()));
        }
        else if (NpcID.FISHING_SPOT_10568 == npcSpawned.getNpc().getId()) {
            fishingSpots.put(npcSpawned.getNpc(), new NPCAttributes(npcSpawned.getNpc().getName(), Instant.now(), npcSpawned.getNpc().getWorldLocation()));
        }
    }

    @Subscribe
    public void onNpcDespawned(NpcDespawned npcDespawned) {
        fishingSpots.remove(npcDespawned.getNpc());
    }

    @Override
    protected void shutDown() throws Exception {
        server.stop(1);
    }

    public Client getClient() {
        return client;
    }

    @Subscribe
    public void onGameTick(GameTick tick) {
        currentTime = System.currentTimeMillis();
        plants.removeIf(plant -> plant.getPlantTimeRelative() == 1);
        xpTracker.update();
        if (client.getCameraZ() != 333) {
            clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, 333, 333));
        }
        int skill_count = 0;
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) {
                continue;
            }
            int xp_gained = handleTracker(skill);
            xp_gained_skills[skill_count] = xp_gained;
            skill_count++;
        }
        tickCount++;
        // Tempoross:
    }

    public void handleMovement(HttpExchange exchange) throws IOException {
        JsonObject jb = new JsonObject();
        jb.addProperty("tick", client.getTickCount());
        jb.addProperty("destinationX", destinationTile.getX());
        jb.addProperty("destinationY", destinationTile.getY());
        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(jb, out);
        }
    }

    public void handleTempoross(HttpExchange exchange) throws IOException {
        for (NPC npc : fishingSpots.keySet()) {
            WorldPoint spotLocation = npc.getWorldLocation();
        }

        JsonObject jb = new JsonObject();
        jb.addProperty("tick", client.getTickCount());
        jb.addProperty("waveIncoming", waveIsIncoming);
        jb.addProperty("fishingspots", fishingSpots.size());
        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(jb, out);
        }
    }

    TileObject findTileObject(int x, int y, int id)
    {
        Scene scene = client.getScene();
        Tile[][][] tiles = scene.getTiles();
        Tile tile = tiles[client.getPlane()][x][y];
        if (tile != null)
        {
            for (GameObject gameObject : tile.getGameObjects())
            {
                if (gameObject != null && gameObject.getId() == id)
                {
                    return gameObject;
                }
            }

            WallObject wallObject = tile.getWallObject();
            if (wallObject != null && wallObject.getId() == id)
            {
                return wallObject;
            }

            DecorativeObject decorativeObject = tile.getDecorativeObject();
            if (decorativeObject != null && decorativeObject.getId() == id)
            {
                return decorativeObject;
            }

            GroundObject groundObject = tile.getGroundObject();
            if (groundObject != null && groundObject.getId() == id)
            {
                return groundObject;
            }
        }
        return null;
    }

    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned event) {
        GameObject gameObject = event.getGameObject();
        TitheFarmPlantType type = TitheFarmPlantType.getPlantType(gameObject.getId());
        if (type == null)
        {
            return;
        }

        TitheFarmPlantState state = TitheFarmPlantState.getState(gameObject.getId());

        TitheFarmPlant newPlant = new TitheFarmPlant(state, type, gameObject);
        TitheFarmPlant oldPlant = getPlantFromCollection(gameObject);

        if (oldPlant == null && newPlant.getType() != TitheFarmPlantType.EMPTY)
        {
            log.debug("Added plant {}", newPlant);
            plants.add(newPlant);
        }
        else if (oldPlant == null)
        {
            return;
        }
        else if (newPlant.getType() == TitheFarmPlantType.EMPTY)
        {
            log.debug("Removed plant {}", oldPlant);
            plants.remove(oldPlant);
        }
        else if (oldPlant.getGameObject().getId() != newPlant.getGameObject().getId())
        {
            if (oldPlant.getState() != TitheFarmPlantState.WATERED && newPlant.getState() == TitheFarmPlantState.WATERED)
            {
                log.debug("Updated plant (watered)");
                oldPlant.setState(newPlant.getState());
                oldPlant.setType(newPlant.getType());
                oldPlant.setGameObject(newPlant.getGameObject());
                newPlant.setPlanted(oldPlant.getPlanted());
                //plants.remove(oldPlant);
                //plants.add(newPlant);
            }
            else
            {
                log.debug("Updated plant");
                oldPlant.setState(newPlant.getState());
                oldPlant.setType(newPlant.getType());
                oldPlant.setGameObject(newPlant.getGameObject());
                oldPlant.setPlanted(newPlant.getPlanted());
                //plants.remove(oldPlant);
                //plants.add(newPlant);
            }
        }
    }

    private TitheFarmPlant getPlantFromCollection(GameObject gameObject)
    {
        WorldPoint gameObjectLocation = gameObject.getWorldLocation();
        for (TitheFarmPlant plant : plants)
        {
            if (gameObjectLocation.equals(plant.getWorldLocation()))
            {
                return plant;
            }
        }
        return null;
    }

    public int handleTracker(Skill skill) {
        int startingSkillXp = xpTracker.getXpData(skill, 0);
        int endingSkillXp = xpTracker.getXpData(skill, tickCount);
        int xpGained = endingSkillXp - startingSkillXp;
        return xpGained;
    }

    public void handlePathing(HttpExchange exchange) throws IOException {
        int[][] collisionData = client.getCollisionMaps()[client.getPlane()].getFlags();
        Tile[][] ts = client.getScene().getTiles()[client.getPlane()];

        int baseX = client.getBaseX();
        int baseY = client.getBaseY();

        JsonArray tiles = new JsonArray();
        for (int localX = 0; localX < collisionData.length; localX++) {
            for (int localY = 0; localY < collisionData[localX].length; localY++) {
                int sceneX = baseX + localX;
                int sceneY = baseY + localY;
                JsonObject tile = new JsonObject();
                tile.addProperty("coordinate", String.format("(%d, %d)", sceneX, sceneY));
                tile.addProperty("collisionData", collisionData[localX][localY]);
                double[] clickbox = null;

                clickbox = getTileClickbox(client, ts[localX][localY]);

                if (clickbox != null) {
                    tile.addProperty("clickbox", String.format("(%f, %f, %f, %f)", clickbox[0], clickbox[1], clickbox[2], clickbox[3]));
                } else {
                    tile.addProperty("clickbox", "-1, -1");
                }
                tiles.add(tile);
            }
        }

        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(tiles, out);
        }
    }

    ;
    public void handleStats(HttpExchange exchange) throws IOException {
        Player player = client.getLocalPlayer();
        JsonArray skills = new JsonArray();
        JsonObject headers = new JsonObject();
        headers.addProperty("username", client.getUsername());
        headers.addProperty("player name", player.getName());
        int skill_count = 0;
        skills.add(headers);
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) {
                continue;
            }
            JsonObject object = new JsonObject();
            object.addProperty("stat", skill.getName());
            object.addProperty("level", client.getRealSkillLevel(skill));
            object.addProperty("boostedLevel", client.getBoostedSkillLevel(skill));
            object.addProperty("xp", client.getSkillExperience(skill));
            object.addProperty("xp gained", String.valueOf(xp_gained_skills[skill_count]));
            skills.add(object);
            skill_count++;
        }

        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(skills, out);
        }
    }

    public void titheFarmEvents(HttpExchange exchange) throws IOException {
        JsonArray output = new JsonArray();
        for (TitheFarmPlant plant : getPlants())
        {
            JsonObject plantStatus = new JsonObject();
            plantStatus.addProperty("state", plant.getState().name());
            plantStatus.addProperty("x", plant.getWorldLocation().getX());
            plantStatus.addProperty("y", plant.getWorldLocation().getY());
            output.add(plantStatus);
        }
        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(output, out);
        }
    }

    public void handleEvents(HttpExchange exchange) throws IOException {
        MAX_DISTANCE = config.reachedDistance();
        Player player = client.getLocalPlayer();
        Actor npc = player.getInteracting();
        String npcName;
        int npcHealth;
        int npcHealth2;
        int health;
        int minHealth = 0;
        int maxHealth = 0;
        if (npc != null) {
            npcName = npc.getName();
            npcHealth = npc.getHealthScale();
            npcHealth2 = npc.getHealthRatio();
            health = 0;
            if (npcHealth2 > 0) {
                minHealth = 1;
                if (npcHealth > 1) {
                    if (npcHealth2 > 1) {
                        // This doesn't apply if healthRatio = 1, because of the special case in the server calculation that
                        // health = 0 forces healthRatio = 0 instead of the expected healthRatio = 1
                        minHealth = (npcHealth * (npcHealth2 - 1) + npcHealth - 2) / (npcHealth - 1);
                    }
                    maxHealth = (npcHealth * npcHealth2 - 1) / (npcHealth - 1);
                    if (maxHealth > npcHealth) {
                        maxHealth = npcHealth;
                    }
                } else {
                    // If healthScale is 1, healthRatio will always be 1 unless health = 0
                    // so we know nothing about the upper limit except that it can't be higher than maxHealth
                    maxHealth = npcHealth;
                }
                // Take the average of min and max possible healths
                health = (minHealth + maxHealth + 1) / 2;
            }
        } else {
            npcName = "null";
            npcHealth = 0;
            npcHealth2 = 0;
            health = 0;
        }
        try {
            Tile hTile = client.getSelectedSceneTile();
            if (hTile != null) {
                hoveredTile = hTile.getWorldLocation();
            }
            JsonObject object = new JsonObject();
            JsonObject camera = new JsonObject();
            JsonObject worldPoint = new JsonObject();
            JsonObject mouse = new JsonObject();
            object.addProperty("animation", player.getAnimation());
            object.addProperty("run enabled", client.getVarpValue(173));
            object.addProperty("animation pose", player.getPoseAnimation());
            object.addProperty("latest msg", msg);
            object.addProperty("run energy", client.getEnergy());
            object.addProperty("game tick", client.getGameCycle());
            object.addProperty("health", client.getBoostedSkillLevel(Skill.HITPOINTS) + "/" + client.getRealSkillLevel(Skill.HITPOINTS));
            object.addProperty("interacting code", String.valueOf(player.getInteracting()));
            object.addProperty("npc name", npcName);
            object.addProperty("npc health ", minHealth);
            object.addProperty("MAX_DISTANCE", MAX_DISTANCE);
            mouse.addProperty("x", client.getMouseCanvasPosition().getX());
            mouse.addProperty("y", client.getMouseCanvasPosition().getY());
            if (hoveredTile != null) {
                mouse.addProperty("hoveredX", hoveredTile.getX());
                mouse.addProperty("hoveredY", hoveredTile.getY());
            }
            else {
                mouse.addProperty("hoveredX", -1);
                mouse.addProperty("hoveredY", -1);
            }
            worldPoint.addProperty("x", player.getWorldLocation().getX());
            worldPoint.addProperty("y", player.getWorldLocation().getY());
            worldPoint.addProperty("plane", player.getWorldLocation().getPlane());
            worldPoint.addProperty("regionID", player.getWorldLocation().getRegionID());
            worldPoint.addProperty("regionX", player.getWorldLocation().getRegionX());
            worldPoint.addProperty("regionY", player.getWorldLocation().getRegionY());
            camera.addProperty("yaw", client.getCameraYaw());
            camera.addProperty("pitch", client.getCameraPitch());
            camera.addProperty("x", client.getCameraX());
            camera.addProperty("y", client.getCameraY());
            camera.addProperty("z", client.getCameraZ());
            camera.addProperty("x2", client.getCameraX2());
            camera.addProperty("y2", client.getCameraY2());
            camera.addProperty("z2", client.getCameraZ2());
            object.add("worldPoint", worldPoint);
            object.add("camera", camera);
            object.add("mouse", mouse);
            object.add("lastAction", lastAction);
            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                RuneLiteAPI.GSON.toJson(object, out);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private HttpHandler handlerForInv(InventoryID inventoryID) {
        return exchange -> {
            Item[] items = invokeAndWait(() -> {
                ItemContainer itemContainer = client.getItemContainer(inventoryID);
                if (itemContainer != null) {
                    return itemContainer.getItems();
                }
                return null;
            });

            if (items == null) {
                exchange.sendResponseHeaders(204, 0);
                return;
            }

            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                RuneLiteAPI.GSON.toJson(items, out);
            }
        };
    }

    private <T> T invokeAndWait(Callable<T> r) {
        try {
            AtomicReference<T> ref = new AtomicReference<>();
            Semaphore semaphore = new Semaphore(0);
            clientThread.invokeLater(() -> {
                try {

                    ref.set(r.call());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    semaphore.release();
                }
            });
            semaphore.acquire();
            return ref.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static double[] getTileClickbox(Client client, Tile tile) {
        LocalPoint l = tile.getLocalLocation();
        Polygon p = Perspective.getCanvasTilePoly(client, l);
        if (p == null) {
            return null;
        }
        if (p.npoints == 0) {
            return null;
        }
        double[] result = new double[4];
        result[0] = p.getBounds2D().getMinX();
        result[1] = p.getBounds2D().getMinY();
        result[2] = p.getBounds2D().getWidth();
        result[3] = p.getBounds2D().getHeight();

        for (int i = 0; i < result.length; i++) {
            if (result[i] < 0)
                return null;
        }
        return result;
    }

    private static double[] getTileClickbox(Client client, LocalPoint tile) {
        Polygon p = Perspective.getCanvasTilePoly(client, tile);
        if (p == null) {
            return null;
        }
        if (p.npoints == 0) {
            return null;
        }
        double[] result = new double[4];
        result[0] = p.getBounds2D().getMinX();
        result[1] = p.getBounds2D().getMinY();
        result[2] = p.getBounds2D().getWidth();
        result[3] = p.getBounds2D().getHeight();

        for (int i = 0; i < result.length; i++) {
            if (result[i] < 0)
                return null;
        }
        return result;
    }

    public void getGameObjectIdHandler(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        InputStreamReader isr = new InputStreamReader(requestBody);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        br.close();
        isr.close();
        requestBody.close();
        JSONObject requestData = null;
        try {
            String s = requestBodyBuilder.toString();
            s = s.substring(1, s.length() - 1).replace("\\", "");
            requestData = new JSONObject(s); // parse request body as json
        } catch (Throwable e) {
            e.printStackTrace();
        }

        int x = (int) requestData.get("x");
        int y = (int) requestData.get("y");
        LocalPoint targetTile = LocalPoint.fromWorld(client, x, y);
        Scene scene = client.getScene();
        Tile[][] tiles = scene.getTiles()[client.getPlane()];
        Tile tile = tiles[targetTile.getSceneX()][targetTile.getSceneY()];
        GameObject[] objects = tile.getGameObjects();
        JsonObject jsonObject = new JsonObject();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (GameObject object : objects) {
            if (object != null) {
                stringBuilder.append(object.getId()).append(", ");
                System.out.println(stringBuilder);
            }

        }
        // Remove the trailing comma and space from the last element
        if (objects.length != 0) {
            stringBuilder.setLength(stringBuilder.length() - 2);
        }
        stringBuilder.append("]");

        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            out.write(stringBuilder.toString());
        }

    }

    public void getClickboxVerification(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        InputStreamReader isr = new InputStreamReader(requestBody);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        br.close();
        isr.close();
        requestBody.close();
        JSONObject requestData = null;
        try {
            String s = requestBodyBuilder.toString();
            s = s.substring(1, s.length() - 1).replace("\\", "");
            requestData = new JSONObject(s); // parse request body as json
        } catch (Throwable e) {
            e.printStackTrace();
        }

        int x = (int) requestData.get("x");
        int y = (int) requestData.get("y");
        final int objId;
        System.out.println(requestData.toString());
        AtomicBoolean result = new AtomicBoolean(false);
        if (requestData.has("objectId")) {
            objId = (int) requestData.get("objectId");
        }
        else {
            objId = -1;
        }
        if (this.clientThread != null) {
            this.clientThread.invoke(() -> {
                try {
                    WorldPoint target = new WorldPoint(x, y, client.getPlane());
                    LocalPoint targetLp = LocalPoint.fromWorld(client, target.getX(), target.getY());
                    Tile targetTile = client.getScene().getTiles()[client.getPlane()][targetLp.getSceneX()][targetLp.getSceneY()];
                    GameObject[] objects = targetTile.getGameObjects();
                    Point mouseP = client.getMouseCanvasPosition();
                    if (objects != null && objId > -1 && mouseP != null) {
                        for (GameObject o : objects) {
                            if (o != null && o.getId() == objId) {
                                Shape p = o.getConvexHull();
                                if (p != null) {
                                    if (p.contains(mouseP.getX(), mouseP.getY())) {
                                        result.set(true);
                                    }
                                }
                            }
                        }
                    }
                    else {
                        Polygon p = Perspective.getCanvasTilePoly(client, targetLp);
                        if (p != null) {
                            if (p.contains(mouseP.getX(), mouseP.getY())) {
                                result.set(true);
                            }
                        }
                    }
                    JsonObject response = new JsonObject();
                    int r = result.get() ? 1 : 0;
                    response.addProperty("result", r);
                    exchange.sendResponseHeaders(200, 0);
                    try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                        RuneLiteAPI.GSON.toJson(response, out);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
    }

    public void getMinimapLocation(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        InputStreamReader isr = new InputStreamReader(requestBody);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        br.close();
        isr.close();
        requestBody.close();
        JSONObject requestData = null;
        try {
            String s = requestBodyBuilder.toString();
            s = s.substring(1, s.length() - 1).replace("\\", "");
            requestData = new JSONObject(s); // parse request body as json
        } catch (Throwable e) {
            e.printStackTrace();
        }
        int x = (int) requestData.get("x");
        int y = (int) requestData.get("y");
        if (this.clientThread != null) {
            this.clientThread.invoke(() -> {
                try {
                    LocalPoint tile = LocalPoint.fromWorld(client, x, y);
                    System.out.println(tile.toString());
                    Point loc = Perspective.localToMinimap(client, tile);
                    System.out.println(loc.toString());
                    JsonObject obj = new JsonObject();
                    obj.addProperty("x", loc.getX());
                    obj.addProperty("y", loc.getY());
                    exchange.sendResponseHeaders(200, 0);
                    try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                        RuneLiteAPI.GSON.toJson(obj, out);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        else {System.out.println("clientThread is null");}


    }

    public void getClickbox(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        InputStreamReader isr = new InputStreamReader(requestBody);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        //System.out.println(requestBodyBuilder.toString());
        //System.out.println(requestBodyBuilder.charAt(1));
        br.close();
        isr.close();
        requestBody.close();
        JSONObject requestData = null;
        try {
            String s = requestBodyBuilder.toString();
            s = s.substring(1, s.length() - 1).replace("\\", "");
            requestData = new JSONObject(s); // parse request body as json
            //System.out.println(requestData.toString());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        int x = (int)requestData.get("x");
        int y = (int)requestData.get("y");
        LocalPoint tile = LocalPoint.fromWorld(client, x, y);
        double[] clickbox = getTileClickbox(client, tile);
        if (clickbox == null) {
            exchange.sendResponseHeaders(204, 0);
            return;
        }
        JsonObject object = new JsonObject();
        object.addProperty("x", clickbox[0]);
        object.addProperty("y", clickbox[1]);
        object.addProperty("width", clickbox[2]);
        object.addProperty("height", clickbox[3]);
        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(object, out);
        }
    }

    public void findPath(HttpExchange exchange) throws IOException {
        InputStream requestBody = exchange.getRequestBody();
        InputStreamReader isr = new InputStreamReader(requestBody);
        BufferedReader br = new BufferedReader(isr);
        StringBuilder requestBodyBuilder = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            requestBodyBuilder.append(line);
        }
        br.close();
        isr.close();
        requestBody.close();
        JSONObject requestData = null;
        try {
            String s = requestBodyBuilder.toString();
            s = s.substring(1, s.length() - 1).replace("\\", "");
            System.out.println(s);
            requestData = new JSONObject(s); // parse request body as json
            //System.out.println(requestData.toString());
        } catch (Throwable e) {
            e.printStackTrace();
            exchange.sendResponseHeaders(400, 0); // Bad Request status code
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                out.write("Error occurred during processing");
            }
        }
        System.out.println("parsed json");
        int startX = (int) requestData.get("startX");
        int startY = (int) requestData.get("startY");
        int endX = (int) requestData.get("targetX");
        int endY = (int) requestData.get("targetY");
        WorldPoint start = new WorldPoint(startX, startY, client.getPlane());
        WorldPoint end = new WorldPoint(endX, endY, client.getPlane());
        LocalCollisionMap localCollisionMap = new LocalCollisionMap(client);
        //System.out.println("initialized collisionmap");
        Pathfinder pathfinder = null;
        try {
            pathfinder = new Pathfinder(localCollisionMap, this.client, start, end);
        } catch (Throwable t) {
            t.printStackTrace();
            exchange.sendResponseHeaders(400, 0); // Bad Request status code
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                out.write("Error occurred during processing");
            }
        }

        List<WorldPoint> path = pathfinder.find();
        System.out.println("found path");
        if (path == null) {
            System.out.println("Failed to create pathfinder");
            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                out.write("[]");
            }
        } else {
            //System.out.println("Path: " + path.toString());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            for (WorldPoint point : path) {
                int x = point.getX();
                int y = point.getY();
                stringBuilder.append("(").append(x).append(", ").append(y).append("), ");
                //System.out.println(stringBuilder);
            }
            // Remove the trailing comma and space from the last element
            if (!path.isEmpty()) {
                stringBuilder.setLength(stringBuilder.length() - 2);
            }
            stringBuilder.append("]");
            //System.out.println(stringBuilder.toString());

            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                out.write(stringBuilder.toString());
            }
        }

    }

    public void getAllReachableTiles(HttpExchange exchange) throws IOException {
        WorldPoint pos = client.getLocalPlayer().getWorldLocation();
        Pathfinder p = new Pathfinder(new LocalCollisionMap(client), client, client.getLocalPlayer().getWorldLocation(), pos);
        List<WorldPoint> reachableTiles = p.findAllReachableTiles();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("[");
        for (WorldPoint point : reachableTiles) {
            int x = point.getX();
            int y = point.getY();
            int difX = Math.abs(pos.getX() - x);
            int difY = Math.abs(pos.getY() - y);
            if (difX < 10 && difY < 10)
            {
                stringBuilder.append("(").append(x).append(", ").append(y).append("), ");
            }

        }
        // Remove the trailing comma and space from the last element
        if (!reachableTiles.isEmpty()) {
            stringBuilder.setLength(stringBuilder.length() - 2);
        }
        stringBuilder.append("]");
        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            out.write(stringBuilder.toString());
        }
    }

    public Map<String, List<Double>> findGoblins() {
        Map<String, List<Double>> goblinTiles = new HashMap<>();
        java.util.List<NPC> npcs = client.getNpcs();
        for (int i = 0; i < npcs.size(); i++) {
            NPC npc = npcs.get(i);
            if (Objects.equals(npc.getName(), "Goblin")) {
                Double xCoord = npc.getCanvasTilePoly().getBounds2D().getCenterX();
                Double yCoord = npc.getCanvasTilePoly().getBounds2D().getCenterY();
                String key = xCoord + "," + yCoord;
                if (!goblinTiles.containsKey(key)) {
                    goblinTiles.put(key, Arrays.asList(xCoord, yCoord));
                }
            }
        }
        return goblinTiles;
    }

}



