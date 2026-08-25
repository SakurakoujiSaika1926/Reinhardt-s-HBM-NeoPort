package com.reinhardt.hbm.drone;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Server-side equivalent of the old RequestNetwork active-waypoint map.
 * Nodes are intentionally transient: the 1.7.10 network rebuilt its leases
 * from loaded block entities instead of persisting route data in the world.
 */
public final class DroneRequestNetwork {
    public static final int MAX_RANGE = 24;
    public static final int MAX_PATH_DEPTH = 10;
    private static final long MAX_AGE_TICKS = 40L;
    private static final Map<ServerLevel, State> STATES = new WeakHashMap<>();

    private DroneRequestNetwork() {
    }

    public enum NodeKind {
        WAYPOINT,
        DOCK,
        PROVIDER,
        REQUESTER;

        public boolean isWaypoint() {
            return this == WAYPOINT;
        }
    }

    public static final class Node {
        private final BlockPos pos;
        private final NodeKind kind;
        private final boolean active;
        private final long lease;
        private final Set<BlockPos> reachable = new LinkedHashSet<>();

        private Node(BlockPos pos, NodeKind kind, boolean active, long lease) {
            this.pos = pos.immutable();
            this.kind = kind;
            this.active = active;
            this.lease = lease;
        }

        public BlockPos pos() {
            return pos;
        }

        public NodeKind kind() {
            return kind;
        }

        public boolean active() {
            return active;
        }

        public Set<BlockPos> reachable() {
            return Set.copyOf(reachable);
        }
    }

    public static void refresh(ServerLevel level, DroneRequestNetworkNode source, boolean active) {
        State state = STATES.computeIfAbsent(level, ignored -> new State());
        long now = level.getGameTime();
        purge(state, now);

        BlockPos pos = source.droneNodePosition();
        Node node = new Node(pos, source.droneNodeKind(), active, now);
        state.nodes.put(pos, node);

        // Legacy TileEntityRequestNetwork tested known paths twice, once from
        // each end. Rebuild both local ends here so a changed obstacle takes
        // effect in the same refresh interval.
        for (Node candidate : state.nodes.values()) {
            if (candidate.pos.equals(pos) || !withinRange(pos, candidate.pos)) {
                continue;
            }
            rebuildEdge(level, node, candidate);
        }
    }

    public static void remove(ServerLevel level, BlockPos pos) {
        State state = STATES.get(level);
        if (state == null) {
            return;
        }
        state.nodes.remove(pos);
        for (Node node : state.nodes.values()) {
            node.reachable.remove(pos);
        }
        if (state.nodes.isEmpty()) {
            STATES.remove(level);
        }
    }

    public static Node node(ServerLevel level, BlockPos pos) {
        State state = STATES.get(level);
        return state == null ? null : state.nodes.get(pos);
    }

    public static List<Node> localNodes(ServerLevel level, BlockPos center, int chunkRange) {
        State state = STATES.get(level);
        if (state == null) {
            return List.of();
        }
        long now = level.getGameTime();
        purge(state, now);
        int maxDistance = (chunkRange * 2 + 1) * 16;
        long maxDistanceSq = (long) maxDistance * maxDistance;
        List<Node> nodes = new ArrayList<>();
        for (Node node : state.nodes.values()) {
            if (node.pos.distSqr(center) <= maxDistanceSq) {
                nodes.add(node);
            }
        }
        return nodes;
    }

    /** Returns the legacy BFS path excluding the starting node and including the end node. */
    public static List<Node> path(ServerLevel level, Node start, Node end) {
        if (start == null || end == null) {
            return null;
        }
        State state = STATES.get(level);
        if (state == null) {
            return null;
        }

        ArrayDeque<List<Node>> paths = new ArrayDeque<>();
        paths.add(List.of(start));
        for (int depth = 0; depth < MAX_PATH_DEPTH && !paths.isEmpty(); depth++) {
            int pathsThisDepth = paths.size();
            int iterationBrake = 1_000;
            for (int count = 0; count < pathsThisDepth; count++) {
                List<Node> oldPath = paths.removeFirst();
                Node tail = oldPath.getLast();
                for (BlockPos connectedPos : tail.reachable) {
                    Node connected = state.nodes.get(connectedPos);
                    if (connected == null) {
                        continue;
                    }
                    List<Node> next = new ArrayList<>(oldPath);
                    if (connected.pos.equals(end.pos)) {
                        next.add(connected);
                        next.removeFirst();
                        return next;
                    }
                    if (!contains(next, connected.pos)) {
                        next.add(connected);
                        paths.addLast(next);
                    }
                    if (--iterationBrake <= 0) {
                        break;
                    }
                }
            }
        }
        return null;
    }

    private static boolean contains(Collection<Node> nodes, BlockPos pos) {
        for (Node node : nodes) {
            if (node.pos.equals(pos)) {
                return true;
            }
        }
        return false;
    }

    private static void rebuildEdge(ServerLevel level, Node left, Node right) {
        boolean connected = connectable(left, right) && hasPath(level, left.pos, right.pos);
        if (connected) {
            left.reachable.add(right.pos);
            right.reachable.add(left.pos);
        } else {
            left.reachable.remove(right.pos);
            right.reachable.remove(left.pos);
        }
    }

    private static boolean connectable(Node left, Node right) {
        return left.kind.isWaypoint() || right.kind.isWaypoint();
    }

    private static boolean withinRange(BlockPos left, BlockPos right) {
        return left.distSqr(right) <= (long) MAX_RANGE * MAX_RANGE;
    }

    private static boolean hasPath(ServerLevel level, BlockPos from, BlockPos to) {
        Vec3 fromCenter = Vec3.atCenterOf(from);
        Vec3 toCenter = Vec3.atCenterOf(to);
        if (fromCenter.distanceToSqr(toCenter) > (long) MAX_RANGE * MAX_RANGE) {
            return false;
        }
        return clearRay(level, fromCenter, toCenter) && clearRay(level, toCenter, fromCenter);
    }

    private static boolean clearRay(ServerLevel level, Vec3 from, Vec3 to) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
        return hit.getType() == BlockHitResult.Type.MISS;
    }

    private static void purge(State state, long now) {
        Set<BlockPos> expired = new HashSet<>();
        state.nodes.entrySet().removeIf(entry -> {
            boolean stale = entry.getValue().lease < now - MAX_AGE_TICKS;
            if (stale) {
                expired.add(entry.getKey());
            }
            return stale;
        });
        if (!expired.isEmpty()) {
            for (Node node : state.nodes.values()) {
                node.reachable.removeAll(expired);
            }
        }
    }

    private static final class State {
        private final Map<BlockPos, Node> nodes = new HashMap<>();
    }
}
