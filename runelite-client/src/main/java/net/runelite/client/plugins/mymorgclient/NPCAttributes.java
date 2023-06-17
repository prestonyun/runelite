package net.runelite.client.plugins.mymorgclient;

import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

import java.time.Instant;

public class NPCAttributes {
    @Getter
    private String npcType;
    @Getter
    private Instant npcInstant;
    @Getter
    private WorldPoint npcLocation;

    public NPCAttributes(String npcType, Instant npcInstant, WorldPoint npcLocation) {
        this.npcType = npcType;
        this.npcInstant = npcInstant;
        this.npcLocation = npcLocation;
    }
}
