package net.runelite.client.game.walking;

import net.runelite.api.Client;
import net.runelite.api.coords.Direction;
import net.runelite.client.game.CollisionMap;

import java.util.*;

import net.runelite.api.coords.WorldPoint;

public class Pathfinder {
    private final CollisionMap collisionMap;

    private final WorldPoint startCoords;

    private final WorldPoint destination;

    private final Deque<WorldPoint> boundary = new ArrayDeque<>();

    private final CoordMap predecessors = new CoordMap();
    private final Client client;

    public Pathfinder(CollisionMap map, Client client, WorldPoint start, WorldPoint target) {
        this.collisionMap = map;
        this.startCoords = start;
        this.destination = target;
        this.client = client;
    }

    public List<WorldPoint> find() {
        this.boundary.add(this.startCoords);
        this.predecessors.put(this.startCoords, null);
        while (!this.boundary.isEmpty()) {
            WorldPoint node = this.boundary.removeFirst();
            if (node.equals(this.destination)) {
                List<WorldPoint> result = new LinkedList<>();
                while (node != null) {
                    result.add(0, node);
                    node = this.predecessors.get(node);
                }
                return result;
            }
            addNeighbours(node);
        }
        System.out.println("boundary is empty");
        return null;
    }

    public List<WorldPoint> findAllReachableTiles() {
        Deque<WorldPoint> boundary = new ArrayDeque<>();
        CoordMap predecessors = new CoordMap();
        boundary.add(this.startCoords);
        predecessors.put(this.startCoords, null);
        List<WorldPoint> reachableTiles = new ArrayList<>();

        while (!boundary.isEmpty()) {
            WorldPoint node = boundary.removeFirst();
            reachableTiles.add(node); // Add node to reachable tiles
            List<Object> r = addNeighbors(node, predecessors, boundary);
            predecessors = (CoordMap) r.get(0);
            boundary = (Deque<WorldPoint>) r.get(1);
        }

        return reachableTiles;
    }


    private List<Object> addNeighbors(WorldPoint node, CoordMap predecessors, Deque<WorldPoint> boundary) {
        for (Direction direction : Direction.values()) {
            WorldPoint neighbor = Reachable.getNeighbour(direction, node);
            int startFlag = Reachable.getCollisionFlag(this.client, node);
            int endFlag = Reachable.getCollisionFlag(this.client, neighbor);

            if (Reachable.canWalk(direction, startFlag, endFlag) && !predecessors.containsKey(neighbor)) {
                predecessors.put(neighbor, node);
                boundary.add(neighbor);
            }
        }
        List<Object> result = new ArrayList<>();
        result.add(predecessors);
        result.add(boundary);
        return result;
    }

    private void addNeighbours(WorldPoint position) {
        System.out.println("addNeighbours: " + position);
        if (this.collisionMap.w(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dx(-1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putE(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
        if (this.collisionMap.e(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dx(1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putW(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
        if (this.collisionMap.s(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dy(-1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putN(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
        if (this.collisionMap.n(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dy(1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putS(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
        if (this.collisionMap.sw(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dx(-1).dy(-1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putNE(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
        if (this.collisionMap.se(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dx(1).dy(-1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putNW(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
        if (this.collisionMap.nw(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dx(-1).dy(1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putSE(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
        if (this.collisionMap.ne(position.getX(), position.getY(), position.getPlane())) {
            WorldPoint neighbor = position.dx(1).dy(1);
            if (!this.predecessors.containsKey(neighbor)) {
                this.predecessors.putSW(neighbor);
                this.boundary.addLast(neighbor);
            }
        }
    }
}
