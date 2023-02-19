package net.runelite.client.game;

import com.google.common.collect.ImmutableMap;
import com.google.gson.stream.JsonReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class CustomNPCManager {
    private static final Logger log = LoggerFactory.getLogger(NPCManager.class);

    private static final Set<Integer> blacklistXpMultiplier = Set.of(new Integer[] {
            Integer.valueOf(8026), Integer.valueOf(8058), Integer.valueOf(8059), Integer.valueOf(8060), Integer.valueOf(8061),

            Integer.valueOf(7850), Integer.valueOf(7852), Integer.valueOf(7853), Integer.valueOf(7884), Integer.valueOf(7885),
            Integer.valueOf(7849), Integer.valueOf(7851), Integer.valueOf(7854), Integer.valueOf(7855), Integer.valueOf(7882), Integer.valueOf(7883), Integer.valueOf(7886), Integer.valueOf(7887), Integer.valueOf(7888), Integer.valueOf(7889),
            Integer.valueOf(494), Integer.valueOf(6640), Integer.valueOf(6656),

            Integer.valueOf(2042), Integer.valueOf(2043), Integer.valueOf(2044) });

    private ImmutableMap<Integer, NPCStats> statsMap;

    @Inject
    private CustomNPCManager() {
        try {
            JsonReader reader = new JsonReader(new InputStreamReader(NPCManager.class.getResourceAsStream("/npc_stats.json"), StandardCharsets.UTF_8));
            try {
                ImmutableMap.Builder<Integer, NPCStats> builder = ImmutableMap.builderWithExpectedSize(3152);
                reader.beginObject();
                while (reader.hasNext())
                    builder.put(
                            Integer.valueOf(Integer.parseInt(reader.nextName())), NPCStats.NPC_STATS_TYPE_ADAPTER
                                    .read(reader));
                reader.endObject();
                this.statsMap = builder.build();
                log.info("Loaded {} NPC stats", Integer.valueOf(this.statsMap.size()));
                reader.close();
            } catch (Throwable throwable) {
                try {
                    reader.close();
                } catch (Throwable throwable1) {
                    throwable.addSuppressed(throwable1);
                }
                throw throwable;
            }
        } catch (IOException ex) {
            log.warn("Error loading NPC stats", ex);
        }
    }

    @Nullable
    public NPCStats getStats(int npcId) {
        return (NPCStats)this.statsMap.get(Integer.valueOf(npcId));
    }

    public int getHealth(int npcId) {
        NPCStats s = (NPCStats)this.statsMap.get(Integer.valueOf(npcId));
        if (s == null || s.getHitpoints() == -1)
            return -1;
        return s.getHitpoints();
    }

    public int getAttackSpeed(int npcId) {
        NPCStats s = (NPCStats)this.statsMap.get(Integer.valueOf(npcId));
        if (s == null || s.getAttackSpeed() == -1)
            return -1;
        return s.getAttackSpeed();
    }

    public double getXpModifier(int npcId) {
        if (blacklistXpMultiplier.contains(Integer.valueOf(npcId)))
            return 1.0D;
        NPCStats s = (NPCStats)this.statsMap.get(Integer.valueOf(npcId));
        if (s == null)
            return 1.0D;
        return s.calculateXpModifier();
    }
}
