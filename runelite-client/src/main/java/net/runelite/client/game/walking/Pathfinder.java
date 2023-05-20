package net.runelite.client.game.walking;

import net.runelite.client.game.CollisionMap;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import net.runelite.api.coords.WorldPoint;

public class Pathfinder {
    private final CollisionMap collisionMap;

    private final WorldPoint startCoords;

    private final WorldPoint destination;

    private final Deque<WorldPoint> boundary = new ArrayDeque<>();

    private final CoordMap predecessors = new CoordMap();

    public Pathfinder(CollisionMap map, WorldPoint start, WorldPoint target) {
        this.collisionMap = map;
        this.startCoords = start;
        this.destination = target;
    }

    public List<WorldPoint> find() {
        this.boundary.add(this.startCoords);
        this.predecessors.put(this.startCoords, null);
        while (!this.boundary.isEmpty()) {
            System.out.println("boundary size: " + this.boundary.size());
            WorldPoint node = this.boundary.removeFirst();
            if (node.equals(this.destination)) {
                List<WorldPoint> result = new LinkedList<>();
                while (node != null) {
                    result.add(0, node);
                    node = this.predecessors.get(node);
                    System.out.println("node: " + node);
                    System.out.println("predecessors: " + this.predecessors);
                }
                return result;
            }
            addNeighbours(node);
        }
        System.out.println("boundary is empty");
        return null;
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
