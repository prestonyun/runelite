package net.runelite.client.game.walking;

import java.nio.ByteBuffer;
import java.util.BitSet;

public class BitSet4D {
    private final int sizeX;

    private final int sizeY;

    private final int sizeZ;

    private final int sizeW;

    private final BitSet bits;

    public BitSet4D(int sizeX, int sizeY, int sizeZ, int sizeW) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.sizeW = sizeW;
        this.bits = new BitSet(sizeX * sizeY * sizeZ * sizeW);
    }

    public BitSet4D(ByteBuffer buffer, int sizeX, int sizeY, int sizeZ, int sizeW) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.sizeW = sizeW;
        int oldLimit = buffer.limit();
        buffer.limit(buffer.position() + (sizeX * sizeY * sizeZ * sizeW + 7) / 8);
        this.bits = BitSet.valueOf(buffer);
        buffer.position(buffer.limit());
        buffer.limit(oldLimit);
    }

    public void write(ByteBuffer buffer) {
        int startPos = buffer.position();
        buffer.put(this.bits.toByteArray());
        buffer.position(startPos + (this.sizeX * this.sizeY * this.sizeZ * this.sizeW + 7) / 8);
    }

    public boolean get(int x, int y, int z, int w) {
        return this.bits.get(index(x, y, z, w));
    }

    public void set(int x, int y, int z, int flag, boolean value) {
        this.bits.set(index(x, y, z, flag), value);
    }

    public void setAll(boolean value) {
        this.bits.set(0, this.bits.size(), value);
    }

    private int index(int x, int y, int z, int w) {
        if (x < 0 || y < 0 || z < 0 || w < 0 || x >= this.sizeX || y >= this.sizeY || z >= this.sizeZ || w >= this.sizeW)
            throw new IndexOutOfBoundsException("(" + x + ", " + y + ", " + z + ", " + w + ")");
        int index = z;
        index = index * this.sizeY + y;
        index = index * this.sizeX + x;
        index = index * this.sizeW + w;
        return index;
    }
}
