package net.runelite.client.plugins.mymorgclient;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;

public class NPCAttributes {
    @Getter
    private String NpcType;
    @Getter
    private WorldPoint NpcLocation;
    @Getter
    private int NpcId;
    @Getter
    private int NpcIndex;

    public NPCAttributes(String npcType, int npcId, WorldPoint npcLocation, int npcIndex) {
        this.NpcType = npcType;
        this.NpcLocation = npcLocation;
        this.NpcId = npcId;
        this.NpcIndex = npcIndex;
    }
}
