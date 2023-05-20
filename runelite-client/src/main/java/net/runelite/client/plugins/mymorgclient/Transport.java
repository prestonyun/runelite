package net.runelite.client.plugins.mymorgclient;

import net.runelite.api.coords.WorldPoint;

public final class Transport {
    private final WorldPoint source;
    private final WorldPoint destination;

    public Transport(WorldPoint source, WorldPoint destination) {
        this.source = source;
        this.destination = destination;
    }

    public boolean equals(Object o) {
        if (o == this) return true;
        if (!(o instanceof Transport)) return false;
        Transport other = (Transport) o;
        Object this$source = getSource(), other$source = other.getSource();
        if ((this$source == null) ? (other$source != null) : !this$source.equals(other$source)) return false;
        Object this$destination = getDestination(), other$destination = other.getDestination();
        return !((this$destination == null) ? (other$destination != null) : !this$destination.equals(other$destination));
    }

    /*    */
    /*    */
    /*  9 */
    public WorldPoint getSource() {
        return this.source;
    }

    public WorldPoint getDestination() {
        /* 10 */
        return this.destination;
        /*    */
    }

    public int hashCode() {
        int PRIME = 59;
        int result = 1;
        Object $source = getSource();
        result = result * 59 + (($source == null) ? 43 : $source.hashCode());
        Object $destination = getDestination();
        return result * 59 + (($destination == null) ? 43 : $destination.hashCode());
    }

    public String toString() {
        return "Transport(source=" + getSource() + ", destination=" + getDestination() + ")";
    }
    /*    */
}
