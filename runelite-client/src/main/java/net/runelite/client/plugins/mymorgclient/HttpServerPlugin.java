package net.runelite.client.plugins.mymorgclient;

import com.google.gson.JsonParser;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import com.google.inject.Provides;
import net.runelite.api.events.GameTick;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.time.Duration;
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
import net.runelite.client.game.walking.GlobalCollisionMap;
import net.runelite.client.game.walking.Pathfinder;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.plugins.mywebsocket.MyPythonConnection;
import net.runelite.http.api.RuneLiteAPI;

@PluginDescriptor(
        name = "Morg HTTP Client",
        description = "Actively logs the player status to localhost on port 8081.",
        tags = {"status", "stats"},
        enabledByDefault = true
)
@Slf4j
public class HttpServerPlugin extends Plugin
{
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
    @Provides
    private HttpServerConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(HttpServerConfig.class);
    }

    @Override
    protected void startUp() throws Exception
    {
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
        server.setExecutor(Executors.newSingleThreadExecutor());
        startTime = System.currentTimeMillis();
        xp_gained_skills = new int[Skill.values().length];
        int skill_count = 0;
        server.start();
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }
            xp_gained_skills[skill_count] = 0;
            skill_count++;
        }
    }
    @Subscribe
    public void onChatMessage(ChatMessage event)
    {
        msg = event.getMessage();
        //System.out.println("onChatmsg:" + msg);
    }

    @Override
    protected void shutDown() throws Exception
    {
        server.stop(1);
    }
    public Client getClient() {
        return client;
    }
    @Subscribe
    public void onGameTick(GameTick tick)
    {
        currentTime = System.currentTimeMillis();
        xpTracker.update();
        if (client.getCameraZ() != 433) {
            clientThread.invokeLater(() -> client.runScript(ScriptID.CAMERA_DO_ZOOM, 433, 433));
        }
        int skill_count = 0;
        //System.out.println("run: " + String.valueOf(client.getVarpValue(173)));
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
                continue;
            }
            int xp_gained = handleTracker(skill);
            xp_gained_skills[skill_count] = xp_gained;
            skill_count ++;
        }
        tickCount++;

    }

    public int handleTracker(Skill skill){
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
                    }
                    else {
                        tile.addProperty("clickbox", "-1, -1");
                    }
                    tiles.add(tile);
                }
            }

            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
                RuneLiteAPI.GSON.toJson(tiles, out);
            }
        };


    public void handleStats(HttpExchange exchange) throws IOException
    {
        Player player = client.getLocalPlayer();
        JsonArray skills = new JsonArray();
        JsonObject headers = new JsonObject();
        headers.addProperty("username", client.getUsername());
        headers.addProperty("player name", player.getName());
        int skill_count = 0;
        skills.add(headers);
        for (Skill skill : Skill.values())
        {
            if (skill == Skill.OVERALL)
            {
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
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody()))
        {
            RuneLiteAPI.GSON.toJson(skills, out);
        }
    }

    public void handleEvents(HttpExchange exchange) throws IOException
    {
        MAX_DISTANCE = config.reachedDistance();
        Player player = client.getLocalPlayer();
        Actor npc = player.getInteracting();
        String npcName;
        int npcHealth;
        int npcHealth2;
        int health;
        int minHealth = 0;
        int maxHealth = 0;
        if (npc != null)
        {
            npcName = npc.getName();
            npcHealth = npc.getHealthScale();
            npcHealth2 = npc.getHealthRatio();
            health = 0;
            if (npcHealth2 > 0)
            {
                minHealth = 1;
                if (npcHealth > 1)
                {
                    if (npcHealth2 > 1)
                    {
                        // This doesn't apply if healthRatio = 1, because of the special case in the server calculation that
                        // health = 0 forces healthRatio = 0 instead of the expected healthRatio = 1
                        minHealth = (npcHealth * (npcHealth2 - 1) + npcHealth - 2) / (npcHealth- 1);
                    }
                    maxHealth = (npcHealth * npcHealth2 - 1) / (npcHealth- 1);
                    if (maxHealth > npcHealth)
                    {
                        maxHealth = npcHealth;
                    }
                }
                else
                {
                    // If healthScale is 1, healthRatio will always be 1 unless health = 0
                    // so we know nothing about the upper limit except that it can't be higher than maxHealth
                    maxHealth = npcHealth;
                }
                // Take the average of min and max possible healths
                health = (minHealth + maxHealth + 1) / 2;
            }
        }
        else
        {
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
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody()))
        {
            RuneLiteAPI.GSON.toJson(object, out);
        }
    }
    private HttpHandler handlerForInv(InventoryID inventoryID)
    {
        return exchange -> {
            Item[] items = invokeAndWait(() -> {
                ItemContainer itemContainer = client.getItemContainer(inventoryID);
                if (itemContainer != null)
                {
                    return itemContainer.getItems();
                }
                return null;
            });

            if (items == null)
            {
                exchange.sendResponseHeaders(204, 0);
                return;
            }

            exchange.sendResponseHeaders(200, 0);
            try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody()))
            {
                RuneLiteAPI.GSON.toJson(items, out);
            }
        };
    }
    private <T> T invokeAndWait(Callable<T> r)
    {
        try
        {
            AtomicReference<T> ref = new AtomicReference<>();
            Semaphore semaphore = new Semaphore(0);
            clientThread.invokeLater(() -> {
                try
                {

                    ref.set(r.call());
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                finally
                {
                    semaphore.release();
                }
            });
            semaphore.acquire();
            return ref.get();
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private static double[] getTileClickbox(Client client, Tile tile)
    {
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

        for (int i = 0; i < result.length; i++)
        {
            if (result[i] < 0)
                return null;
        }
        return result;
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
        System.out.println(requestBodyBuilder.toString());
        br.close();
        isr.close();
        requestBody.close();

        String requestBodyData = requestBodyBuilder.toString();
        System.out.println(requestBodyData);
        JsonObject requestData = new JsonParser().parse(requestBodyData).getAsJsonObject();
        System.out.println(requestData.toString());

        int startX = requestData.get("startX").getAsInt();
        int startY = requestData.get("startY").getAsInt();
        int endX = requestData.get("endX").getAsInt();
        int endY = requestData.get("endY").getAsInt();
        CollisionMap collisionMap ;
        WorldPoint start = new WorldPoint(startX, startY, client.getPlane());
        WorldPoint end = new WorldPoint(endX, endY, client.getPlane());
        try {
            BufferedInputStream input = new BufferedInputStream(HttpServerPlugin.class.getResourceAsStream("regions"));
            try { GZIPInputStream gzip = new GZIPInputStream(input);
                try { collisionMap = new GlobalCollisionMap(gzip.readAllBytes());
                    gzip.close();
                    input.close();} catch (Throwable throwable) { try { gzip.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }  } catch (Throwable throwable) { try { input.close(); } catch (Throwable throwable1) { throwable.addSuppressed(throwable1); }  throw throwable; }
        } catch (IOException e) {
            System.out.println("Failed to load regions");
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

        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            out.write(stringBuilder.toString());
        }
    }
}

