package net.runelite.client.game.walking;

import net.runelite.client.game.CollisionMap;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Objects;

public class GlobalCollisionMap implements CollisionMap {
    private final BitSet4D[] regions = new BitSet4D[65536];

    public GlobalCollisionMap(byte[] data) {
        ByteBuffer buffer = ByteBuffer.wrap(data);
        while (buffer.hasRemaining()) {
            int region = buffer.getShort() & 0xFFFF;
            this.regions[region] = new BitSet4D(buffer, 64, 64, 4, 2);
        }
    }

    public byte[] toBytes() {
        int regionCount = (int)Arrays.<BitSet4D>stream(this.regions).filter(Objects::nonNull).count();
        ByteBuffer buffer = ByteBuffer.allocate(regionCount * 4098);
        for (int i = 0; i < this.regions.length; i++) {
            if (this.regions[i] != null) {
                buffer.putShort((short)i);
                this.regions[i].write(buffer);
            }
        }
        return buffer.array();
    }

    public void set(int x, int y, int z, int w, boolean value) {
        BitSet4D region = this.regions[x / 64 * 256 + y / 64];
        if (region == null)
            return;
        region.set(x % 64, y % 64, z, w, value);
    }

    public boolean get(int x, int y, int z, int w) {
        BitSet4D region = this.regions[x / 64 * 256 + y / 64];
        if (region == null)
            return false;
        return region.get(x % 64, y % 64, z, w);
    }

    public void createRegion(int region) {
        this.regions[region] = new BitSet4D(64, 64, 4, 2);
        this.regions[region].setAll(true);
    }

    public boolean n(int x, int y, int z) {
        return get(x, y, z, 0);
    }

    public boolean e(int x, int y, int z) {
        return get(x, y, z, 1);
    }
}
