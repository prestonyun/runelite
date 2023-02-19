package net.runelite.client.plugins.myplugin;

import java.util.Arrays;
import java.util.List;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Tile;
import net.runelite.api.WallObject;
import net.runelite.api.coords.Direction;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

public class Reachable {
    private static final int MAX_ATTEMPTED_TILES = 1000;

    public static boolean check(int flag, int checkFlag) {
        return ((flag & checkFlag) != 0);
    }

    public static boolean isObstacle(int endFlag) {
        return check(endFlag, 19005696);
    }

    public static boolean isObstacle(Client client, WorldPoint worldPoint) {
        return isObstacle(getCollisionFlag(client, worldPoint));
    }

    public static int getCollisionFlag(Client client, WorldPoint point) {
        CollisionData[] collisionMaps = client.getCollisionMaps();
        if (collisionMaps == null)
            return 16777215;
        CollisionData collisionData = collisionMaps[client.getPlane()];
        if (collisionData == null)
            return 16777215;
        LocalPoint localPoint = LocalPoint.fromWorld(client, point);
        if (localPoint == null)
            return 16777215;
        return collisionData.getFlags()[localPoint.getSceneX()][localPoint.getSceneY()];
    }

    public static boolean isWalled(Direction direction, int startFlag) {
        switch (direction) {
            case NORTH:
                return check(startFlag, 2);
            case EAST:
                return check(startFlag, 8);
            case SOUTH:
                return check(startFlag, 32);
            case WEST:
                return check(startFlag, 128);
        }
        throw new IllegalArgumentException();
    }

    public static boolean isWalled(Client client, WorldPoint source, WorldPoint destination) {
        return isWalled(getAt(client, source), getAt(client, destination));
    }

    public static boolean isWalled(Tile source, Tile destination) {
        WallObject wall = source.getWallObject();
        if (wall == null)
            return false;
        WorldPoint a = source.getWorldLocation();
        WorldPoint b = destination.getWorldLocation();
        switch (wall.getOrientationA()) {
            case 1:
                return (a.dx(-1).equals(b) || a.dx(-1).dy(1).equals(b) || a.dx(-1).dy(-1).equals(b));
            case 2:
                return (a.dy(1).equals(b) || a.dx(-1).dy(1).equals(b) || a.dx(1).dy(1).equals(b));
            case 4:
                return (a.dx(1).equals(b) || a.dx(1).dy(1).equals(b) || a.dx(1).dy(-1).equals(b));
            case 8:
                return (a.dy(-1).equals(b) || a.dx(-1).dy(-1).equals(b) || a.dx(-1).dy(1).equals(b));
        }
        return false;
    }

    public static boolean hasDoor(Client client, WorldPoint source, Direction direction) {
        Tile tile = getAt(client, source);
        if (tile == null)
            return false;
        return hasDoor(client, tile, direction);
    }

    public static boolean hasDoor(Client client, Tile source, Direction direction) {
        WallObject wall = source.getWallObject();
        if (wall == null)
            return false;
        ObjectComposition objectComposition = getObjectDefinition(client, wall.getId());
        if (objectComposition == null)
            return isWalled(direction, getCollisionFlag(client, source.getWorldLocation()));
        List<String> actions = Arrays.asList(objectComposition.getActions());
        return (isWalled(direction, getCollisionFlag(client, source.getWorldLocation())) && (actions.contains("Open") || actions.contains("Close")));
    }

    public static ObjectComposition getObjectDefinition(Client client, int id) {
        ObjectComposition objectDefinition = client.getObjectDefinition(id);
        if (objectDefinition == null)
            return null;
        if (objectDefinition.getImpostorIds() != null && objectDefinition.getImpostor() != null)
            return objectDefinition.getImpostor();
        return objectDefinition;
    }

    public static Tile getAt(Client client, WorldPoint worldPoint) {
        return getAt(client, worldPoint.getX(), worldPoint.getY(), worldPoint.getPlane());
    }

    public static Tile getAt(Client client, int worldX, int worldY, int plane) {
        if (!WorldPoint.isInScene(client, worldX, worldY))
            return null;
        int x = worldX - client.getBaseX();
        int y = worldY - client.getBaseY();
        return client.getScene().getTiles()[plane][x][y];
    }
}
