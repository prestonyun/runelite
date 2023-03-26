package net.runelite.client.plugins.myplugin;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import net.runelite.api.coords.WorldPoint;

@AllArgsConstructor
@Getter
class TreeRespawn
{
    private final Tree tree;
    private final int lenX;
    private final int lenY;
    private final WorldPoint worldLocation;
    private final Instant startTime;
    private final int respawnTime;

    boolean isExpired()
    {
        return Instant.now().isAfter(startTime.plusMillis(respawnTime));
    }
}
