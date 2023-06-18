package net.runelite.client.game.walking;


import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public interface CustomLocatable {
    WorldPoint getWorldLocation();

    LocalPoint getLocalLocation();
}
