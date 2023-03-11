package net.runelite.client.game.walking;

public class TileFlag {
    private int x;

    private int y;

    private int z;

    private int flag;

    private int region;
    private int result;

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setZ(int z) {
        this.z = z;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }

    public void setRegion(int region) {
        this.region = region;
    }

    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TileFlag))
            return false;
        TileFlag other = (TileFlag)o;
        return !other.canEqual(this) ? false : ((getX() != other.getX()) ? false : ((getY() != other.getY()) ? false : ((getZ() != other.getZ()) ? false : ((getFlag() != other.getFlag()) ? false : (!(getRegion() != other.getRegion()))))));
    }

    protected boolean canEqual(Object other) {
        return other instanceof TileFlag;
    }

    public int hashCode() {
        int PRIME = 59;
        result = 1;
        result = result * 59 + getX();
        result = result * 59 + getY();
        result = result * 59 + getZ();
        result = result * 59 + getFlag();
        return result * 59 + getRegion();
    }

    public String toString() {
        return "TileFlag(x=" + getX() + ", y=" + getY() + ", z=" + getZ() + ", flag=" + getFlag() + ", region=" + getRegion() + ")";
    }

    public TileFlag(int x, int y, int z, int flag, int region) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.flag = flag;
        this.region = region;
    }

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getZ() {
        return this.z;
    }

    public int getFlag() {
        return this.flag;
    }

    public int getRegion() {
        return this.region;
    }
}
