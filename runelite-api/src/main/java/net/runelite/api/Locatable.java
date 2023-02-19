package net.runelite.api;

import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public interface Locatable {
    WorldPoint getWorldLocation();

    LocalPoint getLocalLocation();
}
