package com.reinhardt.hbm.power;

import com.reinhardt.hbm.block.EnergyCableBlock;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class PowerNetworkManager {
    private static final int MAX_COMPONENT_NODES = 8192;
    private static final long PRUNE_INTERVAL_TICKS = 20L;
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-PowerSolver");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<ResourceKey<Level>, LevelNetwork> NETWORKS = new ConcurrentHashMap<>();

    private PowerNetworkManager() {
    }

    public static void tickFromEndpoint(Level level, PowerEndpoint endpoint) {
        if (level.isClientSide) {
            return;
        }

        LevelNetwork network = network(level);
        network.register(endpoint);

        long gameTime = level.getGameTime();
        if (network.lastTick == gameTime) {
            return;
        }

        network.lastTick = gameTime;
        network.tick(level);
    }

    public static void markDirty(Level level) {
        if (!level.isClientSide) {
            network(level).markDirty();
        }
    }

    public static boolean canCableConnectTo(LevelAccessor level, BlockPos cablePos, Direction direction) {
        return powerCoreForConnector(level, cablePos, cablePos.relative(direction), direction.getOpposite()) != null;
    }

    private static LevelNetwork network(Level level) {
        return NETWORKS.computeIfAbsent(level.dimension(), key -> new LevelNetwork());
    }

    private static final class LevelNetwork {
        private final Map<BlockPos, PowerEndpoint> endpoints = new HashMap<>();
        private List<List<BlockPos>> components = List.of();
        private CompletableFuture<SolveResult> inFlight;
        private long graphVersion;
        private long lastTick = Long.MIN_VALUE;
        private long lastPruneTick = Long.MIN_VALUE;
        private boolean dirty = true;

        void register(PowerEndpoint endpoint) {
            BlockPos pos = endpoint.getPowerPos().immutable();
            PowerEndpoint old = this.endpoints.put(pos, endpoint);
            // Block entities are recreated when a multiblock is replaced or a
            // chunk reloads. Its connector layout/mode may differ even when
            // the core position stays the same.
            if (old != endpoint) {
                markDirty();
            }
        }

        void markDirty() {
            // A large machine replaces many dummy blocks in one tick. One
            // rebuild already incorporates all of those changes, so further
            // marks must not invalidate the same pending solve repeatedly.
            if (this.dirty) {
                return;
            }
            this.dirty = true;
            this.graphVersion++;
        }

        void tick(Level level) {
            if (this.dirty || this.lastTick - this.lastPruneTick >= PRUNE_INTERVAL_TICKS) {
                pruneInvalidEndpoints(level);
                this.lastPruneTick = this.lastTick;
            }

            if (this.inFlight != null && this.inFlight.isDone()) {
                SolveResult result = this.inFlight.join();
                this.inFlight = null;
                if (result.graphVersion == this.graphVersion) {
                    apply(result);
                }
            }

            if (this.dirty) {
                this.components = rebuildComponents(level);
                this.dirty = false;
            }

            if (this.inFlight == null && !this.components.isEmpty()) {
                SolveSnapshot snapshot = snapshot();
                this.inFlight = CompletableFuture.supplyAsync(() -> solve(snapshot), SOLVER);
            }
        }

        private void pruneInvalidEndpoints(Level level) {
            List<BlockPos> removed = new ArrayList<>();
            for (Map.Entry<BlockPos, PowerEndpoint> entry : this.endpoints.entrySet()) {
                BlockEntity blockEntity = level.getBlockEntity(entry.getKey());
                if (!(blockEntity instanceof PowerEndpoint) || blockEntity != entry.getValue()) {
                    removed.add(entry.getKey());
                }
            }

            if (!removed.isEmpty()) {
                for (BlockPos pos : removed) {
                    this.endpoints.remove(pos);
                }
                markDirty();
            }
        }

        private List<List<BlockPos>> rebuildComponents(Level level) {
            if (this.endpoints.isEmpty()) {
                return List.of();
            }

            Set<BlockPos> visitedNodes = new HashSet<>();
            Set<BlockPos> assignedEndpoints = new HashSet<>();
            List<List<BlockPos>> rebuilt = new ArrayList<>();

            for (BlockPos start : sortedPositions(this.endpoints.keySet())) {
                if (assignedEndpoints.contains(start)) {
                    continue;
                }

                List<BlockPos> componentEndpoints = new ArrayList<>();
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                queue.add(start);
                visitedNodes.add(start);
                int visitedInComponent = 0;

                while (!queue.isEmpty() && visitedInComponent < MAX_COMPONENT_NODES) {
                    BlockPos current = queue.removeFirst();
                    visitedInComponent++;

                    if (this.endpoints.containsKey(current)) {
                        componentEndpoints.add(current);
                        assignedEndpoints.add(current);
                    }

                    for (BlockPos next : adjacentGraphNodes(level, current)) {
                        if (visitedNodes.contains(next) || !isPowerNode(level, next)) {
                            continue;
                        }
                        visitedNodes.add(next.immutable());
                        queue.add(next.immutable());
                    }
                }

                if (!componentEndpoints.isEmpty()) {
                    rebuilt.add(sortedPositions(componentEndpoints));
                }
            }

            return rebuilt;
        }

        private SolveSnapshot snapshot() {
            List<ComponentSnapshot> snapshots = new ArrayList<>();
            for (List<BlockPos> component : this.components) {
                List<EndpointSnapshot> endpoints = new ArrayList<>();
                for (BlockPos pos : component) {
                    PowerEndpoint endpoint = this.endpoints.get(pos);
                    if (endpoint != null) {
                        endpoints.add(new EndpointSnapshot(
                                pos,
                                Math.max(0L, endpoint.getAvailableOutput()),
                                Math.max(0L, endpoint.getRequestedInput()),
                                endpoint.getPowerPriority()
                        ));
                    }
                }
                if (!endpoints.isEmpty()) {
                    snapshots.add(new ComponentSnapshot(endpoints));
                }
            }
            return new SolveSnapshot(this.graphVersion, this.lastTick, snapshots);
        }

        private void apply(SolveResult result) {
            for (Map.Entry<BlockPos, Allocation> entry : result.allocations.entrySet()) {
                PowerEndpoint endpoint = this.endpoints.get(entry.getKey());
                if (endpoint != null) {
                    Allocation allocation = entry.getValue();
                    endpoint.applyPower(allocation.usedOutput, allocation.receivedInput);
                }
            }
        }
    }

    private static boolean isPowerNode(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PowerEndpoint) {
            return true;
        }
        if (blockEntity instanceof PowerGraphNode) {
            return true;
        }
        return level.getBlockState(pos).getBlock() instanceof EnergyCableBlock;
    }

    private static List<BlockPos> adjacentGraphNodes(Level level, BlockPos current) {
        List<BlockPos> nodes = new ArrayList<>();

        BlockEntity blockEntity = level.getBlockEntity(current);
        if (level.getBlockState(current).getBlock() instanceof EnergyCableBlock) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                nodes.add(neighbor.immutable());
                BlockPos core = powerCoreForConnector(level, current, neighbor, direction.getOpposite());
                if (core != null) {
                    nodes.add(core.immutable());
                }
            }
            return nodes;
        }

        if (blockEntity instanceof PowerEndpoint || blockEntity instanceof PowerGraphNode) {
            nodes.addAll(powerConnectorPositions(level, blockEntity));
        }

        if (blockEntity instanceof PowerGraphNode graphNode) {
            for (BlockPos remote : graphNode.getRemotePowerLinks(level)) {
                nodes.add(remote.immutable());
            }
        }
        return nodes;
    }

    private static List<BlockPos> powerConnectorPositions(LevelAccessor level, BlockEntity blockEntity) {
        if (blockEntity instanceof PowerEndpoint endpoint) {
            return endpoint.getPowerConnectorPositions(level);
        }
        if (blockEntity instanceof PowerGraphNode graphNode) {
            return graphNode.getPowerConnectorPositions(level);
        }
        return List.of();
    }

    private static BlockPos powerCoreForConnector(LevelAccessor level, BlockPos connectorPos, BlockPos touchingPos, Direction machineSide) {
        BlockPos corePos = resolvePowerCorePos(level, touchingPos);
        if (corePos == null) {
            return null;
        }

        BlockEntity core = level.getBlockEntity(corePos);
        if (canConnectPower(level, core, connectorPos, machineSide)) {
            return corePos.immutable();
        }
        return null;
    }

    private static boolean canConnectPower(LevelAccessor level, BlockEntity blockEntity, BlockPos connectorPos, Direction machineSide) {
        if (blockEntity instanceof PowerEndpoint endpoint) {
            return endpoint.canConnectPower(level, connectorPos, machineSide);
        }
        if (blockEntity instanceof PowerGraphNode graphNode) {
            return graphNode.canConnectPower(level, connectorPos, machineSide);
        }
        return false;
    }

    private static BlockPos resolvePowerCorePos(LevelAccessor level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            BlockPos corePos = dummy.getCorePos();
            BlockEntity core = level.getBlockEntity(corePos);
            if (core instanceof PowerEndpoint || core instanceof PowerGraphNode) {
                return corePos.immutable();
            }
            return null;
        }
        if (blockEntity instanceof PowerEndpoint || blockEntity instanceof PowerGraphNode) {
            return pos.immutable();
        }
        return null;
    }

    private static List<BlockPos> sortedPositions(Iterable<BlockPos> positions) {
        List<BlockPos> sorted = new ArrayList<>();
        for (BlockPos pos : positions) {
            sorted.add(pos.immutable());
        }
        sorted.sort(Comparator
                .comparingInt((BlockPos pos) -> pos.getX())
                .thenComparingInt(pos -> pos.getY())
                .thenComparingInt(pos -> pos.getZ()));
        return sorted;
    }

    private static SolveResult solve(SolveSnapshot snapshot) {
        Map<BlockPos, Allocation> allocations = new HashMap<>();
        for (ComponentSnapshot component : snapshot.components) {
            solveComponent(component, allocations, snapshot.gameTime);
        }
        return new SolveResult(snapshot.graphVersion, allocations);
    }

    private static void solveComponent(ComponentSnapshot component, Map<BlockPos, Allocation> allocations, long gameTime) {
        long availableOutput = 0L;
        long requestedInput = 0L;
        for (EndpointSnapshot endpoint : component.endpoints) {
            allocations.put(endpoint.pos, Allocation.NONE);
            availableOutput = saturatedAdd(availableOutput, endpoint.availableOutput);
            requestedInput = saturatedAdd(requestedInput, endpoint.requestedInput);
        }

        long transferred = Math.min(availableOutput, requestedInput);
        if (transferred <= 0L) {
            return;
        }

        // Mirrors 1.7.10 PowerNetMK2: every endpoint that currently advertises
        // input participates in the same priority queue, including FEnSU in
        // buffer mode. Providers are then debited proportionally to the energy
        // accepted by those receivers.
        distributeConsumersByPriority(component.endpoints, transferred, allocations, gameTime);
        distributeProducers(component.endpoints, availableOutput, transferred, allocations, gameTime + 17L);
    }

    private static long saturatedAdd(long left, long right) {
        if (left >= Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static void distributeConsumersByPriority(
            List<EndpointSnapshot> endpoints,
            long delivered,
            Map<BlockPos, Allocation> allocations,
            long rotationSeed
    ) {
        long remaining = delivered;
        for (int i = PowerEndpoint.ConnectionPriority.VALUES.length - 1; i >= 0 && remaining > 0L; i--) {
            PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.VALUES[i];
            List<EndpointSnapshot> consumers = rotate(endpoints.stream()
                    .filter(endpoint -> endpoint.requestedInput > 0L && endpoint.priority == priority)
                    .toList(), rotationSeed + i * 31L);
            if (consumers.isEmpty()) {
                continue;
            }
            long priorityDemand = 0L;
            for (EndpointSnapshot endpoint : consumers) {
                priorityDemand = saturatedAdd(priorityDemand, endpoint.requestedInput);
            }
            long priorityDelivered = Math.min(remaining, priorityDemand);
            applyConsumerShares(consumers, priorityDemand, priorityDelivered, allocations);
            remaining -= priorityDelivered;
        }
    }

    private static void applyConsumerShares(
            List<EndpointSnapshot> endpoints,
            long totalDemand,
            long delivered,
            Map<BlockPos, Allocation> allocations
    ) {
        List<Share> shares = proportionalShares(endpoints, totalDemand, delivered, true);
        for (Share share : shares) {
            Allocation old = allocations.get(share.endpoint.pos);
            allocations.put(share.endpoint.pos, new Allocation(old.usedOutput, share.allocated));
        }
    }

    private static void distributeProducers(
            List<EndpointSnapshot> endpoints,
            long totalOutput,
            long delivered,
            Map<BlockPos, Allocation> allocations,
            long rotationSeed
    ) {
        List<EndpointSnapshot> producers = rotate(endpoints.stream()
                .filter(endpoint -> endpoint.availableOutput > 0L)
                .toList(), rotationSeed);
        List<Share> shares = proportionalShares(producers, totalOutput, delivered, false);
        for (Share share : shares) {
            Allocation old = allocations.get(share.endpoint.pos);
            allocations.put(share.endpoint.pos, new Allocation(share.allocated, old.receivedInput));
        }
    }

    private static List<Share> proportionalShares(
            List<EndpointSnapshot> endpoints,
            long total,
            long delivered,
            boolean consumers
    ) {
        if (endpoints.isEmpty() || total <= 0L || delivered <= 0L) {
            return List.of();
        }

        long remaining = delivered;
        List<Share> shares = new ArrayList<>();
        for (EndpointSnapshot endpoint : endpoints) {
            long capacity = consumers ? endpoint.requestedInput : endpoint.availableOutput;
            Division division = weightedDivision(delivered, capacity, total);
            long allocated = Math.min(capacity, division.quotient);
            long remainder = division.remainder;
            remaining -= allocated;
            shares.add(new Share(endpoint, capacity, allocated, remainder));
        }

        List<Share> byRemainder = new ArrayList<>(shares);
        byRemainder.sort(Comparator.comparingLong((Share share) -> share.remainder).reversed());
        for (Share share : byRemainder) {
            if (remaining <= 0L) {
                break;
            }
            if (share.allocated < share.capacity) {
                share.allocated++;
                remaining--;
            }
        }
        return shares;
    }

    private static Division weightedDivision(long total, long part, long whole) {
        if (total <= 0L || part <= 0L || whole <= 0L) {
            return Division.ZERO;
        }
        if (part >= whole) {
            return new Division(total, 0L);
        }
        if (total <= Long.MAX_VALUE / part) {
            long numerator = total * part;
            return new Division(numerator / whole, numerator % whole);
        }
        BigInteger numerator = BigInteger.valueOf(total).multiply(BigInteger.valueOf(part));
        BigInteger[] result = numerator.divideAndRemainder(BigInteger.valueOf(whole));
        return new Division(result[0].longValue(), result[1].longValue());
    }

    private static List<EndpointSnapshot> rotate(List<EndpointSnapshot> endpoints, long seed) {
        if (endpoints.size() <= 1) {
            return endpoints;
        }
        int offset = (int) Math.floorMod(seed, (long) endpoints.size());
        if (offset == 0) {
            return endpoints;
        }
        List<EndpointSnapshot> rotated = new ArrayList<>(endpoints.size());
        rotated.addAll(endpoints.subList(offset, endpoints.size()));
        rotated.addAll(endpoints.subList(0, offset));
        return rotated;
    }

    private static final class Share {
        private final EndpointSnapshot endpoint;
        private final long capacity;
        private long allocated;
        private final long remainder;

        private Share(EndpointSnapshot endpoint, long capacity, long allocated, long remainder) {
            this.endpoint = endpoint;
            this.capacity = capacity;
            this.allocated = allocated;
            this.remainder = remainder;
        }
    }

    private record Division(long quotient, long remainder) {
        private static final Division ZERO = new Division(0L, 0L);
    }

    private record SolveSnapshot(long graphVersion, long gameTime, List<ComponentSnapshot> components) {
    }

    private record ComponentSnapshot(List<EndpointSnapshot> endpoints) {
    }

    private record EndpointSnapshot(
            BlockPos pos,
            long availableOutput,
            long requestedInput,
            PowerEndpoint.ConnectionPriority priority
    ) {
    }

    private record SolveResult(long graphVersion, Map<BlockPos, Allocation> allocations) {
    }

    private record Allocation(long usedOutput, long receivedInput) {
        private static final Allocation NONE = new Allocation(0L, 0L);
    }
}
