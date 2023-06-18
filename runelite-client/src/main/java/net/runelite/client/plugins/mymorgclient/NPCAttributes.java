package net.runelite.client.plugins.mymorgclient;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;

public class NPCAttributes {
    @Getter
    private String NpcType;
    @Getter
    private Instant NpcInstant;
    @Getter
    private WorldPoint NpcLocation;

    public NPCAttributes(String npcType, Instant npcInstant, WorldPoint npcLocation) {
        this.NpcType = npcType;
        this.NpcInstant = npcInstant;
        this.NpcLocation = npcLocation;
    }
}
