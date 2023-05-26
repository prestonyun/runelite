package net.runelite.client.plugins.mymorgclient;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import net.runelite.api.Point;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import com.google.inject.Provides;
import net.runelite.api.events.GameTick;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.game.CollisionMap;
import net.runelite.client.game.walking.*;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.mywebsocket.MyPythonConnection;
import net.runelite.http.api.RuneLiteAPI;
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
    public Skill[] skillList;
    public XpTracker xpTracker;
    public Skill mostRecentSkillGained;
    public int tickCount = 0;
    public long startTime = 0;
    public long currentTime = 0;
    public int[] xp_gained_skills;
    @Inject
    public HttpServerConfig config;
    @Inject
    public ClientThread clientThread;
    public HttpServer server;
    public int MAX_DISTANCE = 1200;
    public String msg;
    private final Set<String> seenRegions = new HashSet<>();
    private int plane = -1;

    @Provides
    private HttpServerConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(HttpServerConfig.class);
    }

    @Override
    protected void startUp() throws Exception {
        //MAX_DISTANCE = config.reachedDistance();
        skillList = Skill.values();
        xpTracker = new XpTracker(this);
        server = HttpServer.create(new InetSocketAddress(8081), 0);
        server.createContext("/stats", this::handleStats);
        server.createContext("/inv", handlerForInv(InventoryID.INVENTORY));
        server.createContext("/equip", handlerForInv(InventoryID.EQUIPMENT));
        server.createContext("/events", this::handleEvents);
        server.createContext("/pathing", this::handlePathing);
        server.createContext("/path", exchange -> {
            findPath(exchange);
        });
        server.createContext("/clickbox", exchange -> {
            getClickbox(exchange);
        });
        server.createContext("/minimap", exchange -> {
            getMinimapLocation(exchange);
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
    public void onChatMessage(ChatMessage event) {
        msg = event.getMessage();
        //System.out.println("onChatmsg:" + msg);
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
        xpTracker.update();
        Player localPlayer = client.getLocalPlayer();
        if (localPlayer == null) {
            if (this.plane == -1) {
                this.plane = localPlayer.getWorldLocation().getPlane();
            }
            else if (this.plane != localPlayer.getWorldLocation().getPlane()) {
                this.plane = localPlayer.getWorldLocation().getPlane();
                sendRegion();
            }
        }
        if (client.getCameraZ() != 433) {
            clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, 433, 433));
        }
        int skill_count = 0;
        //System.out.println("run: " + String.valueOf(client.getVarpValue(173)));
        for (Skill skill : Skill.values()) {
            if (skill == Skill.OVERALL) {
                continue;
            }
            int xp_gained = handleTracker(skill);
            xp_gained_skills[skill_count] = xp_gained;
            skill_count++;
        }
        tickCount++;

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
        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(object, out);
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

    private static double[] getTileClickbox(Client client, LocalPoint tile, Boolean adjusted) {
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

        int anim = client.getLocalPlayer().getPoseAnimation();
        WorldPoint pos = client.getLocalPlayer().getWorldLocation();
        LocalPoint posl = LocalPoint.fromWorld(client, pos.getX(), pos.getY());
        double dx = tile.getX() - posl.getX();
        double dy = tile.getY() - posl.getY();
        double dy_dx = dy / dx;
        double mag = Math.sqrt(1 + Math.pow(dy_dx, 2));

        if (anim == 819 || anim == 820 ||anim == 822) {
            return result;
        }
        else if (anim == 824) {
            double x = result[0];
            double y = result[1];
            result[0] = x * (1 / mag);
            result[1] = y * (dy_dx / mag);
            return result;
        }

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
        LocalPoint tile = LocalPoint.fromWorld(client, x, y);
        Point loc = Perspective.localToMinimap(client, tile);
        JsonObject obj = new JsonObject();
        obj.addProperty("x", loc.getX());
        obj.addProperty("y", loc.getY());
        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(obj, out);
        }
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
        System.out.println(requestBodyBuilder.toString());
        System.out.println(requestBodyBuilder.charAt(1));
        br.close();
        isr.close();
        requestBody.close();
        JSONObject requestData = null;
        try {
            String s = requestBodyBuilder.toString();
            s = s.substring(1, s.length() - 1).replace("\\", "");
            requestData = new JSONObject(s); // parse request body as json
            System.out.println(requestData.toString());
        } catch (Throwable e) {
            e.printStackTrace();
        }
        int x = (int)requestData.get("x");
        int y = (int)requestData.get("y");
        LocalPoint tile = LocalPoint.fromWorld(client, x, y);
        double[] clickbox = getTileClickbox(client, tile);
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
            System.out.println(requestData.toString());
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
        System.out.println("initialized collisionmap");
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
            System.out.println("Path: " + path.toString());
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("[");
            for (WorldPoint point : path) {
                int x = point.getX();
                int y = point.getY();
                stringBuilder.append("(").append(x).append(", ").append(y).append("), ");
                System.out.println(stringBuilder);
            }
            // Remove the trailing comma and space from the last element
            if (!path.isEmpty()) {
                stringBuilder.setLength(stringBuilder.length() - 2);
            }
            stringBuilder.append("]");
            System.out.println(stringBuilder.toString());

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
            if (difX < 20 && difY < 20)
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

    public void sendRegion() {
        Player localPlayer = client.getLocalPlayer();
        if (this.client.getGameState() != GameState.LOGGED_IN || localPlayer == null) {
            return;
        }

        WorldPoint localWorldPoint = localPlayer.getWorldLocation();

        if (localWorldPoint == null) {
            return;
        }

        int plane = client.getPlane();
        int regionID = localWorldPoint.getRegionID();

        if (this.seenRegions.contains("" + regionID + "-" + regionID)) {
            return;
        }

        CollisionData[] col = this.client.getCollisionMaps();

        if (col == null) {
            return;
        }

        List<TileFlag> tileFlags = new ArrayList<>();
        //Map<WorldPoint, List<Transport>> transportLinks = buildTransportLinks();

        CollisionData data = col[plane];

        if (data == null) {
            return;
        }

        int[][] flags = data.getFlags();

        for (int x = 0; x < flags.length; x++) {
            for (int y = 0; y < flags[x].length; y++) {
                LocalPoint localPoint = LocalPoint.fromScene(x, y);
                WorldPoint worldPoint = WorldPoint.fromLocalInstance(client, localPoint);

                int tileX = worldPoint.getX();
                int tileY = worldPoint.getY();

                int flag = flags[x][y];

                if (flag != 16777215) {
                    int regionId = tileX >> 6 << 8 | tileY >> 6;

                    TileFlag tileFlag = new TileFlag(tileX, tileY, plane, 2359552, regionId);
                    Tile tile = Reachable.getAt(this.client, x + this.client.getBaseX(), y + this.client.getBaseY(), plane);
                    if (tile == null) {
                        tileFlags.add(tileFlag);
                    }
                    else {
                        tileFlag.setFlag(flag);
                        WorldPoint tileCoords = tile.getWorldLocation();

                        WorldPoint northernTile = tileCoords.dy(1);
                        if (Reachable.getCollisionFlag(this.client, northernTile) != 16777215) {
                            if (Reachable.isObstacle(this.client, northernTile) && !Reachable.isWalled(Direction.NORTH, tileFlag.getFlag())) {
                                tileFlag.setFlag(tileFlag.getFlag() + 2);
                            }
                            WorldPoint easternTile = tileCoords.dx(1);
                            if (Reachable.getCollisionFlag(this.client, easternTile) != 16777215) {
                                if (Reachable.isObstacle(this.client, easternTile) && !Reachable.isWalled(Direction.EAST, tileFlag.getFlag())) {
                                    tileFlag.setFlag(tileFlag.getFlag() + 8);
                                }

                                //List<Transport> transports = transportLinks.get(tileCoords);
                                if (plane == this.client.getPlane()) {
                                    for (Direction direction : Direction.values()) {
                                        switch (direction) {
                                            case NORTH:
                                                if ((Reachable.hasDoor(this.client, tile, direction) || Reachable.hasDoor(this.client, northernTile, Direction.SOUTH))) {//&&
                                                        //notTransport(transports, tileCoords, northernTile)) {
                                                    tileFlag.setFlag(tileFlag.getFlag() - 2);
                                                }
                                                break;
                                            case EAST:
                                                if ((Reachable.hasDoor(this.client, tile, direction) || Reachable.hasDoor(this.client, easternTile, Direction.WEST))) {//&&
                                                        //notTransport(transports, tileCoords, easternTile)) {
                                                    tileFlag.setFlag(tileFlag.getFlag() - 8);
                                                }
                                                break;
                                        }
                                    }
                                }
                                tileFlags.add(tileFlag); }}
                    }
                }
            }
        } this.seenRegions.add("" + regionID + "-" + regionID);
    }

    /*
    public static Map<WorldPoint, List<Transport>> buildTransportLinks() {
        Map<WorldPoint, List<Transport>> out = new HashMap<>();
        for (Transport transport : TransportLoader.buildTransports())
        {
            ((List<Transport>)out.computeIfAbsent(transport.getSource(), x -> new ArrayList()))).add(transport);
        }
        return out;
    }

    public boolean notTransport(List<Transport> transports, WorldPoint from, WorldPoint to) {
        if (transports == null) {
            return true;
        }
        return transports.stream().noneMatch(t -> (t.getSource().equals(from) && t.getDestination().equals(to)));
    }

     */
}



