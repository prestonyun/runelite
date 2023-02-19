package net.runelite.api.coords;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.runelite.api.Client;
import net.runelite.api.CollisionData;
import net.runelite.api.Point;
import net.runelite.api.Tile;

public class CustomWorldArea {
    private final int x;

    private final int y;

    private final int width;

    private final int height;

    private final int plane;

    public int getX() {
        return this.x;
    }

    public int getY() {
        return this.y;
    }

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public int getPlane() {
        return this.plane;
    }

    public CustomWorldArea(int x, int y, int width, int height, int plane) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.plane = plane;
    }

    public CustomWorldArea(WorldPoint location, int width, int height) {
        this.x = location.getX();
        this.y = location.getY();
        this.plane = location.getPlane();
        this.width = width;
        this.height = height;
    }

    public CustomWorldArea(WorldPoint swLocation, WorldPoint neLocation) {
        this.x = swLocation.getX();
        this.y = swLocation.getY();
        this.plane = swLocation.getPlane();
        this.width = neLocation.getX() - swLocation.getX();
        this.height = neLocation.getY() - swLocation.getY();
    }

    public CustomWorldArea(WorldArea worldArea) {
        this.x = worldArea.getX();
        this.y = worldArea.getY();
        this.width = worldArea.getWidth();
        this.height = worldArea.getHeight();
        this.plane = worldArea.getPlane();
    }

    private Point getAxisDistances(CustomWorldArea other) {
        Point p1 = getComparisonPoint(other);
        Point p2 = other.getComparisonPoint(this);
        return new Point(Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY() - p2.getY()));
    }

    public int distanceTo(CustomWorldArea other) {
        if (getPlane() != other.getPlane())
            return Integer.MAX_VALUE;
        return distanceTo2D(other);
    }

    public int distanceTo(WorldPoint other) {
        return distanceTo(new CustomWorldArea(other, 1, 1));
    }

    public int distanceTo2D(CustomWorldArea other) {
        Point distances = getAxisDistances(other);
        return Math.max(distances.getX(), distances.getY());
    }

    public int distanceTo2D(WorldPoint other) {
        return distanceTo2D(new CustomWorldArea(other, 1, 1));
    }

    public boolean contains(WorldPoint worldPoint) {
        return (distanceTo(worldPoint) == 0);
    }

    public boolean contains2D(WorldPoint worldPoint) {
        return (distanceTo2D(worldPoint) == 0);
    }

    public boolean isInMeleeDistance(CustomWorldArea other) {
        if (other == null || getPlane() != other.getPlane())
            return false;
        Point distances = getAxisDistances(other);
        return (distances.getX() + distances.getY() == 1);
    }

    public boolean isInMeleeDistance(WorldPoint other) {
        return isInMeleeDistance(new CustomWorldArea(other, 1, 1));
    }

    public boolean canMelee(Client client, CustomWorldArea other) {
        if (isInMeleeDistance(other)) {
            Point p1 = getComparisonPoint(other);
            Point p2 = other.getComparisonPoint(this);
            CustomWorldArea w1 = new CustomWorldArea(p1.getX(), p1.getY(), 1, 1, getPlane());
            return w1.canTravelInDirection(client, p2.getX() - p1.getX(), p2.getY() - p1.getY());
        }
        return false;
    }

    public boolean intersectsWith(CustomWorldArea other) {
        if (getPlane() != other.getPlane())
            return false;
        Point distances = getAxisDistances(other);
        return (distances.getX() + distances.getY() == 0);
    }

    public boolean canTravelInDirection(Client client, int dx, int dy) {
        return canTravelInDirection(client, dx, dy, x -> true);
    }

    public boolean canTravelInDirection(Client client, int dx, int dy, Predicate<? super WorldPoint> extraCondition) {
        dx = Integer.signum(dx);
        dy = Integer.signum(dy);
        if (dx == 0 && dy == 0)
            return true;
        LocalPoint lp = LocalPoint.fromWorld(client, this.x, this.y);
        int startX = 0;
        if (lp != null)
            startX = lp.getSceneX() + dx;
        int startY = 0;
        if (lp != null)
            startY = lp.getSceneY() + dy;
        int checkX = startX + ((dx > 0) ? (this.width - 1) : 0);
        int checkY = startY + ((dy > 0) ? (this.height - 1) : 0);
        int endX = startX + this.width - 1;
        int endY = startY + this.height - 1;
        int xFlags = 2359552;
        int yFlags = 2359552;
        int xyFlags = 2359552;
        int xWallFlagsSouth = 2359552;
        int xWallFlagsNorth = 2359552;
        int yWallFlagsWest = 2359552;
        int yWallFlagsEast = 2359552;
        if (dx < 0) {
            xFlags |= 0x8;
            xWallFlagsSouth |= 0x30;
            xWallFlagsNorth |= 0x6;
        }
        if (dx > 0) {
            xFlags |= 0x80;
            xWallFlagsSouth |= 0x60;
            xWallFlagsNorth |= 0x3;
        }
        if (dy < 0) {
            yFlags |= 0x2;
            yWallFlagsWest |= 0x81;
            yWallFlagsEast |= 0xC;
        }
        if (dy > 0) {
            yFlags |= 0x20;
            yWallFlagsWest |= 0xC0;
            yWallFlagsEast |= 0x18;
        }
        if (dx < 0 && dy < 0)
            xyFlags |= 0x4;
        if (dx < 0 && dy > 0)
            xyFlags |= 0x10;
        if (dx > 0 && dy < 0)
            xyFlags |= 0x1;
        if (dx > 0 && dy > 0)
            xyFlags |= 0x40;
        CollisionData[] collisionData = client.getCollisionMaps();
        if (collisionData == null)
            return false;
        int[][] collisionDataFlags = collisionData[this.plane].getFlags();
        if (dx != 0) {
            int y;
            for (y = startY; y <= endY; y++) {
                if ((collisionDataFlags[checkX][y] & xFlags) != 0 ||
                        !extraCondition.test(WorldPoint.fromScene(client, checkX, y, this.plane)))
                    return false;
            }
            for (y = startY + 1; y <= endY; y++) {
                if ((collisionDataFlags[checkX][y] & xWallFlagsSouth) != 0)
                    return false;
            }
            for (y = endY - 1; y >= startY; y--) {
                if ((collisionDataFlags[checkX][y] & xWallFlagsNorth) != 0)
                    return false;
            }
        }
        if (dy != 0) {
            int x;
            for (x = startX; x <= endX; x++) {
                if ((collisionDataFlags[x][checkY] & yFlags) != 0 ||
                        !extraCondition.test(WorldPoint.fromScene(client, x, checkY, client.getPlane())))
                    return false;
            }
            for (x = startX + 1; x <= endX; x++) {
                if ((collisionDataFlags[x][checkY] & yWallFlagsWest) != 0)
                    return false;
            }
            for (x = endX - 1; x >= startX; x--) {
                if ((collisionDataFlags[x][checkY] & yWallFlagsEast) != 0)
                    return false;
            }
        }
        if (dx != 0 && dy != 0) {
            if ((collisionDataFlags[checkX][checkY] & xyFlags) != 0 ||
                    !extraCondition.test(WorldPoint.fromScene(client, checkX, checkY, client.getPlane())))
                return false;
            if (this.width == 1)
                if ((collisionDataFlags[checkX][checkY - dy] & xFlags) != 0 && extraCondition
                        .test(WorldPoint.fromScene(client, checkX, startY, client.getPlane())))
                    return false;
            if (this.height == 1)
                return ((collisionDataFlags[checkX - dx][checkY] & yFlags) == 0 ||
                        !extraCondition.test(WorldPoint.fromScene(client, startX, checkY, client.getPlane())));
        }
        return true;
    }

    private Point getComparisonPoint(CustomWorldArea other) {
        int x;
        int y;
        if (other.x <= this.x) {
            x = this.x;
        } else if (other.x >= this.x + this.width - 1) {
            x = this.x + this.width - 1;
        } else {
            x = other.x;
        }
        if (other.y <= this.y) {
            y = this.y;
        } else if (other.y >= this.y + this.height - 1) {
            y = this.y + this.height - 1;
        } else {
            y = other.y;
        }
        return new Point(x, y);
    }

    public CustomWorldArea calculateNextTravellingPoint(Client client, CustomWorldArea target, boolean stopAtMeleeDistance) {
        return calculateNextTravellingPoint(client, target, stopAtMeleeDistance, x -> true);
    }

    public CustomWorldArea calculateNextTravellingPoint(Client client, CustomWorldArea target, boolean stopAtMeleeDistance, Predicate<? super WorldPoint> extraCondition) {
        if (this.plane != target.getPlane())
            return null;
        if (intersectsWith(target)) {
            if (stopAtMeleeDistance)
                return null;
            return this;
        }
        int dx = target.x - this.x;
        int dy = target.y - this.y;
        Point axisDistances = getAxisDistances(target);
        if (stopAtMeleeDistance && axisDistances.getX() + axisDistances.getY() == 1)
            return this;
        LocalPoint lp = LocalPoint.fromWorld(client, this.x, this.y);
        if (lp == null || lp
                .getSceneX() + dx < 0 || lp.getSceneX() + dy >= 104 || lp
                .getSceneY() + dx < 0 || lp.getSceneY() + dy >= 104)
            return null;
        int dxSig = Integer.signum(dx);
        int dySig = Integer.signum(dy);
        if (stopAtMeleeDistance && axisDistances.getX() == 1 && axisDistances.getY() == 1) {
            if (canTravelInDirection(client, dxSig, 0, extraCondition))
                return new CustomWorldArea(this.x + dxSig, this.y, this.width, this.height, this.plane);
        } else {
            if (canTravelInDirection(client, dxSig, dySig, extraCondition))
                return new CustomWorldArea(this.x + dxSig, this.y + dySig, this.width, this.height, this.plane);
            if (dx != 0 && canTravelInDirection(client, dxSig, 0, extraCondition))
                return new CustomWorldArea(this.x + dxSig, this.y, this.width, this.height, this.plane);
            if (dy != 0 && Math.max(Math.abs(dx), Math.abs(dy)) > 1 &&
                    canTravelInDirection(client, 0, dy, extraCondition))
                return new CustomWorldArea(this.x, this.y + dySig, this.width, this.height, this.plane);
        }
        return this;
    }

    public boolean hasLineOfSightTo(Client client, CustomWorldArea other) {
        int cmpThisX, cmpThisY, cmpOtherX, cmpOtherY;
        if (this.plane != other.getPlane())
            return false;
        LocalPoint sourceLp = LocalPoint.fromWorld(client, this.x, this.y);
        LocalPoint targetLp = LocalPoint.fromWorld(client, other.getX(), other.getY());
        if (sourceLp == null || targetLp == null)
            return false;
        int thisX = sourceLp.getSceneX();
        int thisY = sourceLp.getSceneY();
        int otherX = targetLp.getSceneX();
        int otherY = targetLp.getSceneY();
        if (otherX <= thisX) {
            cmpThisX = thisX;
        } else {
            cmpThisX = Math.min(otherX, thisX + this.width - 1);
        }
        if (otherY <= thisY) {
            cmpThisY = thisY;
        } else {
            cmpThisY = Math.min(otherY, thisY + this.height - 1);
        }
        if (thisX <= otherX) {
            cmpOtherX = otherX;
        } else {
            cmpOtherX = Math.min(thisX, otherX + other.getWidth() - 1);
        }
        if (thisY <= otherY) {
            cmpOtherY = otherY;
        } else {
            cmpOtherY = Math.min(thisY, otherY + other.getHeight() - 1);
        }
        Tile[][][] tiles = client.getScene().getTiles();
        Tile sourceTile = tiles[this.plane][cmpThisX][cmpThisY];
        Tile targetTile = tiles[other.getPlane()][cmpOtherX][cmpOtherY];
        if (sourceTile == null || targetTile == null)
            return false;
        return sourceTile.hasLineOfSightTo(targetTile);
    }

    public boolean hasLineOfSightTo(Client client, WorldPoint other) {
        return hasLineOfSightTo(client, new CustomWorldArea(other, 1, 1));
    }

    public WorldPoint toWorldPoint() {
        return new WorldPoint(this.x, this.y, this.plane);
    }

    public List<WorldPoint> toWorldPointList() {
        List<WorldPoint> list = new ArrayList<>(this.width * this.height);
        for (int x = 0; x < this.width; x++) {
            for (int y = 0; y < this.height; y++)
                list.add(new WorldPoint(getX() + x, getY() + y, getPlane()));
        }
        return list;
    }
}
