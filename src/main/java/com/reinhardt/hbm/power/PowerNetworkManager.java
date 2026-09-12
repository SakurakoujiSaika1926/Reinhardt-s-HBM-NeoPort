package com.reinhardt.hbm.power;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.EnergyCableBlock;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.integration.sable.HbmSablePowerCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

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
    private static final TagKey<Block> MOBILE_POWER_COMPATIBLE = TagKey.create(
            Registries.BLOCK,
            ReinhardtsHBM.id("mobile_power_compatible")
    );
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-PowerSolver");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<NetworkKey, LevelNetwork> NETWORKS = new ConcurrentHashMap<>();

    private PowerNetworkManager() {
    }

    public static void tickFromEndpoint(Level level, PowerEndpoint endpoint) {
        if (level.isClientSide) {
            return;
        }
        if (!mobilePowerNodeAllowed(level, endpoint.getPowerPos())) {
            return;
        }

        LevelNetwork network = network(level, endpoint.getPowerPos());
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
            for (Map.Entry<NetworkKey, LevelNetwork> entry : NETWORKS.entrySet()) {
                if (entry.getKey().dimension.equals(level.dimension())) {
                    entry.getValue().markDirty();
                }
            }
        }
    }

    public static boolean canCableConnectTo(LevelAccessor level, BlockPos cablePos, Direction direction) {
        BlockPos touchingPos = cablePos.relative(direction);
        if (!samePowerSpace(level, cablePos, touchingPos)) {
            return false;
        }
        if (powerCoreForConnector(level, cablePos, touchingPos, direction.getOpposite()) != null) {
            return true;
        }

        // Match 1.12's BlockCable.computeConnectToNeighbor fallback: an HBM
        // cable also renders a connector when the adjacent block exposes a
        // Forge Energy capability on the face toward the cable.  The transfer
        // bridge already uses this exact side; keeping the test here makes the
        // persisted connection properties agree with the actual FE transfer.
        if (!(level instanceof Level realLevel)) {
            return false;
        }
        if (!realLevel.isLoaded(touchingPos)) {
            return false;
        }
        IEnergyStorage storage = realLevel.getCapability(
                Capabilities.EnergyStorage.BLOCK, touchingPos, direction.getOpposite());
        return storage != null && (storage.canReceive() || storage.canExtract());
    }

    /** Resolves a native HBM endpoint at a core or multiblock dummy position. */
    public static PowerEndpoint endpointAt(LevelAccessor level, BlockPos pos) {
        BlockPos corePos = resolvePowerCorePos(level, pos);
        if (corePos == null) {
            return null;
        }
        if (!mobilePowerNodeAllowed(level, corePos)) {
            return null;
        }
        BlockEntity blockEntity = level.getBlockEntity(corePos);
        return blockEntity instanceof PowerEndpoint endpoint ? endpoint : null;
    }

    /** Snapshot used by the direct port of 1.7.10's power-network analyzer. */
    public static NetworkDiagnostics diagnostics(Level level, BlockPos clickedPos) {
        if (level.isClientSide) {
            return null;
        }
        return network(level, clickedPos).diagnostics(level, clickedPos);
    }

    /**
     * 1.7.10's power gauge reads PowerNetMK2.energyTracker for its own shared
     * cable network. The solver already owns that value, so this only walks a
     * bounded local graph to find the matching endpoint record.
     */
    public static long transferredPowerAt(Level level, BlockPos clickedPos) {
        if (level.isClientSide) {
            return 0L;
        }
        return network(level, clickedPos).transferredPowerAt(level, clickedPos);
    }

    public record NetworkDiagnostics(String id, int links, int providers, int receivers, List<BlockPos> linkPositions) {
    }

    private static LevelNetwork network(Level level, BlockPos pos) {
        return NETWORKS.computeIfAbsent(networkKey(level, pos), LevelNetwork::new);
    }

    private static NetworkKey networkKey(Level level, BlockPos pos) {
        return new NetworkKey(level.dimension(), HbmSablePowerCompat.powerSpaceId(level, pos));
    }

    private static boolean samePowerSpace(LevelAccessor level, BlockPos first, BlockPos second) {
        return HbmSablePowerCompat.samePowerSpace(level, first, second);
    }

    private static boolean mobilePowerNodeAllowed(LevelAccessor level, BlockPos pos) {
        return !HbmSablePowerCompat.isSubLevelBlock(level, pos)
                || level.getBlockState(pos).is(MOBILE_POWER_COMPATIBLE);
    }

    private record NetworkKey(ResourceKey<Level> dimension, String powerSpace) {
    }

    private static final class LevelNetwork {
        private final NetworkKey key;
        private final Map<BlockPos, PowerEndpoint> endpoints = new HashMap<>();
        private List<List<BlockPos>> components = List.of();
        private CompletableFuture<SolveResult> inFlight;
        private Map<BlockPos, Long> transferredPower = Map.of();
        private Set<BlockPos> foreignCables = Set.of();
        private long graphVersion;
        private long lastTick = Long.MIN_VALUE;
        private long lastPruneTick = Long.MIN_VALUE;
        private long lastForeignScan = Long.MIN_VALUE;
        private boolean dirty = true;

        private LevelNetwork(NetworkKey key) {
            this.key = key;
        }

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
                SolveSnapshot snapshot = snapshot(level);
                this.inFlight = CompletableFuture.supplyAsync(() -> solve(snapshot), SOLVER);
            }

            // 1.7.10 refreshes the neighbour cache every 20 ticks but performs
            // transfers every tick. This transient bridge never creates a
            // converter block or a second persistent energy network.
            if (HbmConfig.AUTO_CABLE_CONVERSION.get()) {
                transferForeignEnergy(level);
            }
        }

        private void transferForeignEnergy(Level level) {
            if (this.lastForeignScan == Long.MIN_VALUE || this.lastTick - this.lastForeignScan >= 20L) {
                Set<BlockPos> cables = new HashSet<>();
                for (BlockPos endpoint : this.endpoints.keySet()) {
                    collectCableNodes(level, endpoint, cables);
                }
                this.foreignCables = Set.copyOf(cables);
                this.lastForeignScan = this.lastTick;

                // Forge Energy providers may be attached after the cable's
                // placement update (or become available after a capability
                // invalidation).  Keep the persisted connection mask in lock
                // step with the same six-side probe used by the transfer
                // bridge; UPDATE_CLIENTS then selects the arm-bearing model.
                for (BlockPos cable : this.foreignCables) {
                    EnergyCableBlock.refreshConnections(level, cable);
                }
            }
            for (BlockPos cable : this.foreignCables) {
                for (Direction direction : Direction.values()) {
                    BlockPos neighborPos = cable.relative(direction);
                    if (!level.isLoaded(neighborPos)) {
                        continue;
                    }
                    if (!samePowerSpace(level, cable, neighborPos)) {
                        continue;
                    }
                    // Native endpoints (including the explicit converter) are
                    // handled by the HBM network and must not be bridged twice.
                    if (endpointAt(level, neighborPos) != null || isPowerNode(level, neighborPos)) {
                        continue;
                    }
                    IEnergyStorage storage = level.getCapability(
                            Capabilities.EnergyStorage.BLOCK, neighborPos, direction.getOpposite());
                    if (storage == null) {
                        continue;
                    }
                    pullFromForeignStorage(level, cable, storage);
                    pushToForeignStorage(level, cable, storage);
                }
            }
        }

        private void collectCableNodes(Level level, BlockPos start, Set<BlockPos> cables) {
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            Set<BlockPos> visited = new HashSet<>();
            BlockPos origin = start.immutable();
            queue.add(origin);
            visited.add(origin);
            while (!queue.isEmpty() && visited.size() < MAX_COMPONENT_NODES) {
                BlockPos current = queue.removeFirst();
                if (level.getBlockState(current).getBlock() instanceof EnergyCableBlock) {
                    cables.add(current.immutable());
                }
                for (BlockPos next : adjacentGraphNodes(level, current)) {
                    BlockPos immutable = next.immutable();
                    if (visited.add(immutable) && isPowerNode(level, immutable)) {
                        queue.addLast(immutable);
                    }
                }
            }
        }

        private void pullFromForeignStorage(Level level, BlockPos cable, IEnergyStorage storage) {
            double rate = HbmConfig.HE_TO_FE_CONVERSION_RATE.get();
            if (rate <= 0D || !storage.canExtract()) {
                return;
            }
            int maxExtractFe = storage.extractEnergy(Integer.MAX_VALUE, true);
            long heBudget = (long) Math.floor(maxExtractFe / rate);
            if (heBudget <= 0L) {
                return;
            }
            long acceptedHe = externalTransfer(level, cable, heBudget, true, true);
            if (acceptedHe <= 0L) {
                return;
            }
            int feToExtract = (int) Math.min(maxExtractFe, Math.min(Integer.MAX_VALUE,
                    Math.round(acceptedHe * rate)));
            int extractedFe = storage.extractEnergy(feToExtract, false);
            if (extractedFe <= 0) {
                return;
            }
            long injectedHe = Math.min(acceptedHe, (long) Math.floor(extractedFe / rate));
            if (injectedHe > 0L) {
                externalTransfer(level, cable, injectedHe, true, false);
            }
        }

        private void pushToForeignStorage(Level level, BlockPos cable, IEnergyStorage storage) {
            double rate = HbmConfig.HE_TO_FE_CONVERSION_RATE.get();
            if (rate <= 0D || !storage.canReceive()) {
                return;
            }
            int freeSpaceFe = storage.receiveEnergy(Integer.MAX_VALUE, true);
            long heBudget = (long) Math.floor(freeSpaceFe / rate);
            if (heBudget <= 0L) {
                return;
            }
            long extractedHe = externalTransfer(level, cable, heBudget, false, true);
            if (extractedHe <= 0L) {
                return;
            }
            int feToSend = (int) Math.min(freeSpaceFe, Math.min(Integer.MAX_VALUE,
                    Math.round(extractedHe * rate)));
            int receivedFe = storage.receiveEnergy(feToSend, false);
            if (receivedFe <= 0) {
                return;
            }
            long usedHe = Math.min(extractedHe, (long) Math.floor(receivedFe / rate));
            if (usedHe > 0L) {
                externalTransfer(level, cable, usedHe, false, false);
            }
        }

        /** Transfers HE between a cable and reachable native endpoints. */
        private long externalTransfer(Level level, BlockPos cable, long amount, boolean receive, boolean simulate) {
            if (amount <= 0L || !isPowerNode(level, cable)) {
                return 0L;
            }
            Map<BlockPos, Long> limits = receive ? directedReachable(level, cable) : null;
            List<ExternalEndpoint> candidates = new ArrayList<>();
            if (receive) {
                for (Map.Entry<BlockPos, Long> entry : limits.entrySet()) {
                    PowerEndpoint endpoint = endpointAt(level, entry.getKey());
                    if (endpoint != null && !entry.getKey().equals(cable)
                            && endpoint.getRequestedInput() > 0L) {
                        candidates.add(new ExternalEndpoint(endpoint, entry.getValue()));
                    }
                }
            } else {
                for (BlockPos pos : graphComponent(level, cable)) {
                    PowerEndpoint endpoint = endpointAt(level, pos);
                    if (endpoint == null || endpoint.getAvailableOutput() <= 0L) {
                        continue;
                    }
                    long limit = directedReachable(level, pos).getOrDefault(cable, 0L);
                    if (limit > 0L) {
                        candidates.add(new ExternalEndpoint(endpoint, limit));
                    }
                }
            }
            candidates.sort(Comparator
                    .comparingInt((ExternalEndpoint candidate) -> candidate.endpoint.getPowerPriority().ordinal())
                    .reversed()
                    .thenComparing(candidate -> candidate.endpoint.getPowerPos()));
            long remaining = amount;
            for (ExternalEndpoint candidate : candidates) {
                long available = receive
                        ? candidate.endpoint.getRequestedInput()
                        : candidate.endpoint.getAvailableOutput();
                long transfer = Math.min(remaining, Math.min(Math.max(0L, available), candidate.limit));
                if (transfer <= 0L) {
                    continue;
                }
                if (!simulate) {
                    candidate.endpoint.applyPower(receive ? 0L : transfer, receive ? transfer : 0L);
                }
                remaining -= transfer;
                if (remaining <= 0L) {
                    break;
                }
            }
            return amount - remaining;
        }

        private Set<BlockPos> graphComponent(Level level, BlockPos start) {
            Set<BlockPos> visited = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            BlockPos origin = start.immutable();
            visited.add(origin);
            queue.add(origin);
            while (!queue.isEmpty() && visited.size() < MAX_COMPONENT_NODES) {
                BlockPos current = queue.removeFirst();
                for (BlockPos next : adjacentGraphNodes(level, current)) {
                    BlockPos immutable = next.immutable();
                    if (visited.add(immutable) && isPowerNode(level, immutable)) {
                        queue.addLast(immutable);
                    }
                }
            }
            return visited;
        }

        private record ExternalEndpoint(PowerEndpoint endpoint, long limit) {
        }

        NetworkDiagnostics diagnostics(Level level, BlockPos clickedPos) {
            BlockPos start = resolvePowerCorePos(level, clickedPos);
            if (start == null && isPowerNode(level, clickedPos)) {
                start = clickedPos;
            }
            if (start == null) {
                return null;
            }

            Set<BlockPos> visited = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start.immutable());
            visited.add(start.immutable());
            List<BlockPos> links = new ArrayList<>();
            int providers = 0;
            int receivers = 0;

            while (!queue.isEmpty() && visited.size() < MAX_COMPONENT_NODES) {
                BlockPos current = queue.removeFirst();
                BlockEntity blockEntity = level.getBlockEntity(current);
                if (blockEntity instanceof PowerEndpoint endpoint) {
                    if (endpoint.getAvailableOutput() > 0L) {
                        providers++;
                    }
                    if (endpoint.getRequestedInput() > 0L) {
                        receivers++;
                    }
                } else if (isPowerNode(level, current)) {
                    links.add(current.immutable());
                }

                for (BlockPos next : adjacentGraphNodes(level, current)) {
                    if (visited.size() >= MAX_COMPONENT_NODES || visited.contains(next) || !isPowerNode(level, next)) {
                        continue;
                    }
                    BlockPos immutable = next.immutable();
                    visited.add(immutable);
                    queue.add(immutable);
                }
            }

            if (visited.isEmpty()) {
                return null;
            }
            links.sort(Comparator
                    .comparingInt((BlockPos pos) -> pos.getX())
                    .thenComparingInt(pos -> pos.getY())
                    .thenComparingInt(pos -> pos.getZ()));
            String id = Integer.toHexString(visited.hashCode());
            return new NetworkDiagnostics(id, links.size(), providers, receivers, List.copyOf(links));
        }

        long transferredPowerAt(Level level, BlockPos clickedPos) {
            Long direct = this.transferredPower.get(clickedPos);
            if (direct != null) {
                return direct;
            }

            Set<BlockPos> visited = new HashSet<>();
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            BlockPos start = clickedPos.immutable();
            if (!isPowerNode(level, start)) {
                return 0L;
            }
            visited.add(start);
            queue.add(start);

            while (!queue.isEmpty() && visited.size() < MAX_COMPONENT_NODES) {
                BlockPos current = queue.removeFirst();
                Long transferred = this.transferredPower.get(current);
                if (transferred != null) {
                    return transferred;
                }
                for (BlockPos next : adjacentGraphNodes(level, current)) {
                    BlockPos immutable = next.immutable();
                    if (visited.add(immutable) && isPowerNode(level, immutable)) {
                        queue.addLast(immutable);
                    }
                }
            }
            return 0L;
        }

        private void pruneInvalidEndpoints(Level level) {
            List<BlockPos> removed = new ArrayList<>();
            for (Map.Entry<BlockPos, PowerEndpoint> entry : this.endpoints.entrySet()) {
                if (!this.key.equals(networkKey(level, entry.getKey()))) {
                    removed.add(entry.getKey());
                    continue;
                }
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

        private SolveSnapshot snapshot(Level level) {
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
                                endpoint.getPowerPriority(),
                                directedReachable(level, pos)
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
            this.transferredPower = result.transferredPower;
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
        if (!mobilePowerNodeAllowed(level, pos)) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PowerEndpoint) {
            return true;
        }
        if (blockEntity instanceof PowerGraphNode graphNode) {
            return graphNode.isPowerGraphEnabled(level);
        }
        return level.getBlockState(pos).getBlock() instanceof EnergyCableBlock;
    }

    private static List<BlockPos> adjacentGraphNodes(Level level, BlockPos current) {
        return adjacentGraphNodes(level, current, false);
    }

    private static List<BlockPos> adjacentGraphNodes(Level level, BlockPos current, boolean directed) {
        List<BlockPos> nodes = new ArrayList<>();

        BlockEntity blockEntity = level.getBlockEntity(current);
        if (level.getBlockState(current).getBlock() instanceof EnergyCableBlock) {
            for (Direction direction : Direction.values()) {
                BlockPos neighbor = current.relative(direction);
                if (samePowerSpace(level, current, neighbor)
                        && (!directed || canEnterPowerNode(level, current, neighbor))) {
                    nodes.add(neighbor.immutable());
                }
                BlockPos core = powerCoreForConnector(level, current, neighbor, direction.getOpposite(), directed);
                if (core != null) {
                    nodes.add(core.immutable());
                }
            }
            return nodes;
        }

        if (blockEntity instanceof PowerEndpoint || blockEntity instanceof PowerGraphNode graphNode
                && graphNode.isPowerGraphEnabled(level)) {
            List<BlockPos> connectors = directed && blockEntity instanceof PowerGraphNode graphNode
                    ? graphNode.getPowerFlowPositions(level)
                    : powerConnectorPositions(level, blockEntity);
            for (BlockPos connector : connectors) {
                if (samePowerSpace(level, current, connector)
                        && (!directed || canEnterPowerNode(level, current, connector))) {
                    nodes.add(connector.immutable());
                }
            }
        }

        if (blockEntity instanceof PowerGraphNode graphNode) {
            for (BlockPos remote : graphNode.getRemotePowerLinks(level)) {
                if (samePowerSpace(level, current, remote)) {
                    nodes.add(remote.immutable());
                }
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
        return powerCoreForConnector(level, connectorPos, touchingPos, machineSide, true);
    }

    private static BlockPos powerCoreForConnector(LevelAccessor level, BlockPos connectorPos, BlockPos touchingPos,
                                                   Direction machineSide, boolean directed) {
        if (!samePowerSpace(level, connectorPos, touchingPos)) {
            return null;
        }
        BlockPos corePos = resolvePowerCorePos(level, touchingPos);
        if (corePos == null) {
            return null;
        }
        if (!samePowerSpace(level, connectorPos, corePos) || !mobilePowerNodeAllowed(level, corePos)) {
            return null;
        }

        BlockEntity core = level.getBlockEntity(corePos);
        if (!directed || canConnectPower(level, core, connectorPos, machineSide)) {
            return corePos.immutable();
        }
        return null;
    }

    private static boolean canEnterPowerNode(Level level, BlockPos from, BlockPos target) {
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (!(blockEntity instanceof PowerGraphNode graphNode)) {
            return true;
        }
        Direction machineSide = Direction.getNearest(
                from.getX() - target.getX(),
                from.getY() - target.getY(),
                from.getZ() - target.getZ()
        );
        return graphNode.canAcceptPowerFrom(level, from, machineSide);
    }

    private static Map<BlockPos, Long> directedReachable(Level level, BlockPos start) {
        Map<BlockPos, Long> limits = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        BlockPos origin = start.immutable();
        limits.put(origin, Long.MAX_VALUE);
        queue.add(origin);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < MAX_COMPONENT_NODES) {
            BlockPos current = queue.removeFirst();
            long currentLimit = limits.getOrDefault(current, 0L);
            for (BlockPos next : adjacentGraphNodes(level, current, true)) {
                BlockPos immutable = next.immutable();
                long nextLimit = Math.min(currentLimit, powerFlowLimit(level, immutable));
                if (nextLimit <= 0L || nextLimit <= limits.getOrDefault(immutable, -1L)) {
                    continue;
                }
                limits.put(immutable, nextLimit);
                queue.addLast(immutable);
            }
        }
        return Map.copyOf(limits);
    }

    private static long powerFlowLimit(LevelAccessor level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof PowerGraphNode graphNode) {
            return Math.max(0L, graphNode.getPowerFlowLimit(level));
        }
        return Long.MAX_VALUE;
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
            if (!samePowerSpace(level, pos, corePos) || !mobilePowerNodeAllowed(level, corePos)) {
                return null;
            }
            BlockEntity core = level.getBlockEntity(corePos);
            if (core instanceof PowerEndpoint
                    || core instanceof PowerGraphNode graphNode && graphNode.isPowerGraphEnabled(level)) {
                return corePos.immutable();
            }
            return null;
        }
        if (!mobilePowerNodeAllowed(level, pos)) {
            return null;
        }
        if (blockEntity instanceof PowerEndpoint
                || blockEntity instanceof PowerGraphNode graphNode && graphNode.isPowerGraphEnabled(level)) {
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
        Map<BlockPos, Long> transferredPower = new HashMap<>();
        for (ComponentSnapshot component : snapshot.components) {
            long transferred = solveComponent(component, allocations, snapshot.gameTime);
            for (EndpointSnapshot endpoint : component.endpoints) {
                transferredPower.put(endpoint.pos, transferred);
            }
        }
        return new SolveResult(snapshot.graphVersion, Map.copyOf(allocations), Map.copyOf(transferredPower));
    }

    private static long solveComponent(ComponentSnapshot component, Map<BlockPos, Allocation> allocations, long gameTime) {
        List<EndpointSnapshot> providers = component.endpoints.stream()
                .filter(endpoint -> endpoint.availableOutput > 0L
                        && component.canReachConsumer(endpoint.pos))
                .map(endpoint -> endpoint.withAvailableOutput(
                        Math.min(endpoint.availableOutput, component.maxPathLimitToConsumer(endpoint.pos))))
                .filter(endpoint -> endpoint.availableOutput > 0L)
                .toList();
        List<EndpointSnapshot> consumers = component.endpoints.stream()
                .filter(endpoint -> endpoint.requestedInput > 0L
                        && component.canReachProvider(endpoint.pos))
                .map(endpoint -> endpoint.withRequestedInput(
                        Math.min(endpoint.requestedInput, component.maxPathLimitFromProvider(endpoint.pos))))
                .filter(endpoint -> endpoint.requestedInput > 0L)
                .toList();

        long availableOutput = 0L;
        long requestedInput = 0L;
        for (EndpointSnapshot endpoint : component.endpoints) {
            allocations.put(endpoint.pos, Allocation.NONE);
        }
        for (EndpointSnapshot endpoint : providers) {
            availableOutput = saturatedAdd(availableOutput, endpoint.availableOutput);
        }
        for (EndpointSnapshot endpoint : consumers) {
            requestedInput = saturatedAdd(requestedInput, endpoint.requestedInput);
        }

        long transferred = Math.min(availableOutput, requestedInput);
        if (transferred <= 0L) {
            return 0L;
        }

        // Mirrors 1.7.10 PowerNetMK2: every endpoint that currently advertises
        // input participates in the same priority queue, including FEnSU in
        // buffer mode. Providers are then debited proportionally to the energy
        // accepted by those receivers.
        distributeConsumersByPriority(consumers, transferred, allocations, gameTime);
        distributeProducers(providers, availableOutput, transferred, allocations, gameTime + 17L);
        return transferred;
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
        private boolean canReachConsumer(BlockPos provider) {
            return endpoints.stream().anyMatch(endpoint -> endpoint.requestedInput > 0L
                    && endpoint.pos != provider
                    && endpoints.stream().anyMatch(candidate -> candidate.pos.equals(provider)
                            && candidate.reachableLimits.containsKey(endpoint.pos)));
        }

        private boolean canReachProvider(BlockPos consumer) {
            return endpoints.stream().anyMatch(provider -> provider.availableOutput > 0L
                    && provider.pos != consumer
                    && provider.reachableLimits.containsKey(consumer));
        }

        private long maxPathLimitToConsumer(BlockPos provider) {
            return endpoints.stream()
                    .filter(endpoint -> endpoint.pos.equals(provider))
                    .flatMap(endpoint -> endpoints.stream()
                            .filter(consumer -> consumer.requestedInput > 0L)
                            .map(consumer -> endpoint.reachableLimits.getOrDefault(consumer.pos, 0L)))
                    .mapToLong(Long::longValue)
                    .max()
                    .orElse(0L);
        }

        private long maxPathLimitFromProvider(BlockPos consumer) {
            return endpoints.stream()
                    .filter(provider -> provider.availableOutput > 0L)
                    .mapToLong(provider -> provider.reachableLimits.getOrDefault(consumer, 0L))
                    .max()
                    .orElse(0L);
        }
    }

    private record EndpointSnapshot(
            BlockPos pos,
            long availableOutput,
            long requestedInput,
            PowerEndpoint.ConnectionPriority priority,
            Map<BlockPos, Long> reachableLimits
    ) {
        private EndpointSnapshot withAvailableOutput(long output) {
            return new EndpointSnapshot(pos, output, requestedInput, priority, reachableLimits);
        }

        private EndpointSnapshot withRequestedInput(long input) {
            return new EndpointSnapshot(pos, availableOutput, input, priority, reachableLimits);
        }
    }

    private record SolveResult(long graphVersion, Map<BlockPos, Allocation> allocations,
                               Map<BlockPos, Long> transferredPower) {
    }

    private record Allocation(long usedOutput, long receivedInput) {
        private static final Allocation NONE = new Allocation(0L, 0L);
    }
}
