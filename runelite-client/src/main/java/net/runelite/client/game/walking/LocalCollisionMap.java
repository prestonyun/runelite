package net.runelite.client.game.walking;


import net.runelite.client.game.CollisionMap;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.WorldPoint;

public class LocalCollisionMap implements CollisionMap {
    private final Client client;

    @Inject
    public LocalCollisionMap(Client client) {
        this.client = client;
    }

    public boolean n(int x, int y, int z) {
        WorldPoint current = new WorldPoint(x, y, z);
        if (Reachable.isObstacle(this.client, current))
            return false;
        return Reachable.canWalk(Direction.NORTH, Reachable.getCollisionFlag(this.client, current), Reachable.getCollisionFlag(this.client, current.dy(1)));
    }

    public boolean e(int x, int y, int z) {
        WorldPoint current = new WorldPoint(x, y, z);
        if (Reachable.isObstacle(this.client, current))
            return false;
        return Reachable.canWalk(Direction.EAST, Reachable.getCollisionFlag(this.client, current), Reachable.getCollisionFlag(this.client, current.dx(1)));
    }
}
