package net.runelite.client.game.walking;

import java.util.HashMap;
import java.util.Map;
import net.runelite.api.coords.WorldPoint;

public class CoordMap {
    public static final byte NONE = 0;

    public static final byte CUSTOM = 1;

    public static final byte N = 2;

    public static final byte NE = 3;

    public static final byte E = 4;

    public static final byte SE = 5;

    public static final byte S = 6;

    public static final byte SW = 7;

    public static final byte W = 8;

    public static final byte NW = 9;

    private final byte[][] regions = new byte[65536][];

    private final Map<WorldPoint, WorldPoint> custom = new HashMap<>();

    public boolean containsKey(WorldPoint key) {
        return (region(key)[index(key)] != 0);
    }

    public WorldPoint get(WorldPoint key) {
        byte code = region(key)[index(key)];
        switch (code) {
            case 0:
                return null;
            case 1:
                return this.custom.get(key);
            case 2:
                return key.dy(1);
            case 3:
                return key.dx(1).dy(1);
            case 4:
                return key.dx(1);
            case 5:
                return key.dx(1).dy(-1);
            case 6:
                return key.dy(-1);
            case 7:
                return key.dx(-1).dy(-1);
            case 8:
                return key.dx(-1);
            case 9:
                return key.dx(-1).dy(1);
        }
        return key;
    }

    public void put(WorldPoint key, WorldPoint value) {
        region(key)[index(key)] = 1;
        this.custom.put(key, value);
    }

    public void putN(WorldPoint key) {
        region(key)[index(key)] = 2;
    }

    public void putNE(WorldPoint key) {
        region(key)[index(key)] = 3;
    }

    public void putE(WorldPoint key) {
        region(key)[index(key)] = 4;
    }

    public void putSE(WorldPoint key) {
        region(key)[index(key)] = 5;
    }

    public void putS(WorldPoint key) {
        region(key)[index(key)] = 6;
    }

    public void putSW(WorldPoint key) {
        region(key)[index(key)] = 7;
    }

    public void putW(WorldPoint key) {
        region(key)[index(key)] = 8;
    }

    public void putNW(WorldPoint key) {
        region(key)[index(key)] = 9;
    }

    private int index(WorldPoint worldPoint) {
        return worldPoint.getX() % 64 + worldPoint.getY() % 64 * 64 + worldPoint.getPlane() % 64 * 64 * 64;
    }

    private byte[] region(WorldPoint worldPoint) {
        int regionIndex = worldPoint.getX() / 64 * 256 + worldPoint.getY() / 64;
        byte[] region = this.regions[regionIndex];
        if (region == null)
            region = this.regions[regionIndex] = new byte[16384];
        return region;
    }
}
