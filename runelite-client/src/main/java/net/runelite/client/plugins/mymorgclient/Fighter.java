package net.runelite.client.plugins.mymorgclient;

import net.runelite.client.plugins.mymorgclient.NPCAttributes;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.coords.WorldPoint;
import net.runelite.http.api.RuneLiteAPI;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Fighter {
    private Client client;
    private WorldPoint playerLocation;
    private Map<NPC, NPCAttributes> targets;

    public Fighter(Client client) {
        this.client = client;
        this.playerLocation = null;
        this.targets = new HashMap<>();
    }
    public void handleFighter(HttpExchange exchange) throws IOException {
        JsonObject jb = new JsonObject();

        List<NPC> npcs = client.getNpcs();
        for (NPC npc : npcs) {
            if (npc.getName() != null) {
                if (npc.getName().equals("Goblin")) {
                    targets.put(npc, new NPCAttributes(npc.getName(), npc.getId(), npc.getWorldLocation(), npc.getIndex()));
                }
            }
        }

        jb.addProperty("tick", client.getTickCount());

        exchange.sendResponseHeaders(200, 0);
        try (OutputStreamWriter out = new OutputStreamWriter(exchange.getResponseBody())) {
            RuneLiteAPI.GSON.toJson(jb, out);
        }
    }

}
