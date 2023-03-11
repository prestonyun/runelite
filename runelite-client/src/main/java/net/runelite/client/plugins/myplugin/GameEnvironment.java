package net.runelite.client.plugins.myplugin;

import net.runelite.api.Client;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.Tile;
import net.runelite.client.game.walking.Reachable;

import javax.swing.plaf.synth.Region;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GameEnvironment {
    private final Client client;

    public GameEnvironment(Client client) {
        this.client = client;
    }

    public List<List<Integer>> getRegionTilesAsList() {
        List<List<Integer>> regionList = new ArrayList<>();
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        int regionID = playerLocation.getRegionID();
        int regionX = (regionID >>> 8) << 6;
        int regionY = (regionID & 0xff) << 6;

        for (int i = 0; i < 64; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < 64; j++) {
                Tile tile = client.getScene().getTiles()[0][j + regionX % 64][i + regionY % 64];
                int objectId = -1;
                if (tile != null) {
                    if (tile.getGroundObject() != null) {
                        objectId = tile.getGroundObject().getId();
                    } else if (tile.getWallObject() != null) {
                        objectId = tile.getWallObject().getId();
                    } else if (tile.getDecorativeObject() != null) {
                        objectId = tile.getDecorativeObject().getId();
                    }
                }
                row.add(objectId);
            }
            regionList.add(row);
        }
        return regionList;
    }


    public List<List<Integer>> getRegionDynamicInfoAsList() {
        List<List<Integer>> dynamicInfoList = new ArrayList<>();
        WorldPoint playerLocation = client.getLocalPlayer().getWorldLocation();
        int regionID = playerLocation.getRegionID();
        int regionX = (regionID >>> 8) << 6;
        int regionY = (regionID & 0xff) << 6;

        for (int i = 0; i < 64; i++) {
            List<Integer> row = new ArrayList<>();
            for (int j = 0; j < 64; j++) {
                WorldPoint location = new WorldPoint(regionX + j, regionY + i, playerLocation.getPlane());
                boolean isWalkable = Reachable.isWalkable(client, location);
                NPC npc = client.getNpcs().stream().filter(n -> n.getWorldLocation().equals(location)).findFirst().orElse(null);
                Player player = client.getPlayers().stream().filter(p -> p.getWorldLocation().equals(location)).findFirst().orElse(null);
                int objectId = -1;
                if (npc != null) {
                    objectId = npc.getId();
                } else if (player != null) {
                    objectId = player.getId();
                }
                row.add(objectId);
                row.add(isWalkable ? 1 : 0);
            }
            dynamicInfoList.add(row);
        }
        return dynamicInfoList;
    }






}
