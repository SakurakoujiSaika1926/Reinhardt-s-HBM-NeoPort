package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.power.PowerEndpoint;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

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

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class HbmFluidNetworks {
    private static final int MAX_PIPE_SEARCH = 1024;
    private static final int MAX_BALANCE_PER_ENDPOINT = 1_000_000_000;
    private static final ExecutorService SOLVER = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "RHbm-FluidNetSolver");
        thread.setDaemon(true);
        return thread;
    });
    private static final Map<ResourceKey<Level>, LevelNetwork> NETWORKS = new ConcurrentHashMap<>();

    private HbmFluidNetworks() {
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            network(level).tickBalance(level);
        }
    }

    public static void registerPipe(Level level, BlockPos pos, HbmFluidDefinition type, boolean open) {
        if (!level.isClientSide) {
            network(level).upsert(pos, type == null || type.isNone() ? Set.of() : Set.of(type.name()), open);
        }
    }

    public static void registerPipe(Level level, BlockPos pos, List<HbmFluidDefinition> types, boolean open) {
        if (!level.isClientSide) {
            Set<String> typeNames = new HashSet<>();
            if (types != null) {
                for (HbmFluidDefinition type : types) {
                    if (type != null && !type.isNone()) {
                        typeNames.add(type.name());
                    }
                }
            }
            network(level).upsert(pos, typeNames, open);
        }
    }

    public static void unregisterPipe(Level level, BlockPos pos) {
        if (!level.isClientSide) {
            network(level).remove(pos);
        }
    }

    public static boolean canPipeConnect(LevelAccessor level, BlockPos pipePos, Direction direction, HbmFluidDefinition type) {
        BlockPos target = pipePos.relative(direction);
        BlockEntity neighbor = level.getBlockEntity(target);
        if (neighbor instanceof FluidPipeBlockEntity pipe) {
            return pipe.canConnect(type);
        }
        if (type == null || type.isNone()) {
            return false;
        }
        if (level instanceof Level realLevel) {
            return realLevel.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite()) != null;
        }
        return false;
    }

    public static FluidStack drainFrom(Level level, BlockPos target, Direction side, HbmFluidDefinition type, int amount, @Nullable BlockPos excluded, boolean execute) {
        if (type == null || type.isNone() || amount <= 0) {
            return FluidStack.EMPTY;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof FluidPipeBlockEntity pipe && pipe.canConnect(type)) {
            return drainFromPipeNetwork(level, target, type, amount, excluded, execute);
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side);
        if (handler == null) {
            return FluidStack.EMPTY;
        }
        return handler.drain(HbmFluids.toNeoStack(type, amount), execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
    }

    public static int fillInto(Level level, BlockPos target, Direction side, FluidStack stack, @Nullable BlockPos excluded, boolean execute) {
        if (stack.isEmpty()) {
            return 0;
        }
        HbmFluidDefinition type = HbmFluids.fromNeoFluid(stack.getFluid()).orElse(HbmFluids.none());
        if (type.isNone()) {
            return 0;
        }
        BlockEntity blockEntity = level.getBlockEntity(target);
        if (blockEntity instanceof FluidPipeBlockEntity pipe && pipe.canConnect(type)) {
            return fillPipeNetwork(level, target, type, stack, excluded, execute);
        }
        IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, side);
        return handler == null ? 0 : handler.fill(stack, execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
    }

    public static FluidStack drainFromPipeNetwork(Level level, BlockPos start, HbmFluidDefinition type, int amount, @Nullable BlockPos excluded, boolean execute) {
        if (type == null || type.isNone() || amount <= 0) {
            return FluidStack.EMPTY;
        }

        EndpointSelection selection = endpoints(level, start, type, excluded);
        if (selection.endpoints().isEmpty()) {
            return FluidStack.EMPTY;
        }

        LevelNetwork network = network(level);
        int startIndex = network.cursor(CursorKind.DRAIN, selection.cursorKey(), selection.endpoints().size());
        int remaining = amount;
        int drainedTotal = 0;
        int lastSuccessIndex = -1;

        for (int i = 0; i < selection.endpoints().size() && remaining > 0; i++) {
            int endpointIndex = (startIndex + i) % selection.endpoints().size();
            Endpoint endpoint = selection.endpoints().get(endpointIndex);
            FluidStack requested = HbmFluids.toNeoStack(type, remaining);
            FluidStack drained = endpoint.handler().drain(requested, execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            if (drained.isEmpty()) {
                continue;
            }
            int accepted = Math.min(remaining, drained.getAmount());
            drainedTotal += accepted;
            remaining -= accepted;
            lastSuccessIndex = endpointIndex;
        }

        if (execute && lastSuccessIndex >= 0) {
            network.advance(CursorKind.DRAIN, selection.cursorKey(), lastSuccessIndex + 1, selection.endpoints().size());
        }
        return drainedTotal <= 0 ? FluidStack.EMPTY : HbmFluids.toNeoStack(type, drainedTotal);
    }

    public static int fillPipeNetwork(Level level, BlockPos start, HbmFluidDefinition type, FluidStack offered, @Nullable BlockPos excluded, boolean execute) {
        int remaining = offered.getAmount();
        int filled = 0;
        EndpointSelection selection = endpoints(level, start, type, excluded);
        if (selection.endpoints().isEmpty()) {
            return 0;
        }

        LevelNetwork network = network(level);
        int startIndex = network.cursor(CursorKind.FILL, selection.cursorKey(), selection.endpoints().size());
        int lastSuccessIndex = -1;

        for (int i = 0; i < selection.endpoints().size() && remaining > 0; i++) {
            int endpointIndex = (startIndex + i) % selection.endpoints().size();
            Endpoint endpoint = selection.endpoints().get(endpointIndex);
            FluidStack slice = offered.copy();
            slice.setAmount(remaining);
            int accepted = endpoint.handler().fill(slice, execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            int moved = Math.min(remaining, accepted);
            remaining -= moved;
            filled += moved;
            lastSuccessIndex = endpointIndex;
        }

        if (execute && lastSuccessIndex >= 0) {
            network.advance(CursorKind.FILL, selection.cursorKey(), lastSuccessIndex + 1, selection.endpoints().size());
        }
        return filled;
    }

    private static EndpointSelection endpoints(Level level, BlockPos start, HbmFluidDefinition type, @Nullable BlockPos excluded) {
        Set<BlockPos> pipes = cachedPipes(level, start, type);
        return endpoints(level, type, pipes, excluded);
    }

    private static EndpointSelection endpoints(Level level, HbmFluidDefinition type, Set<BlockPos> pipes, @Nullable BlockPos excluded) {
        ArrayList<Endpoint> endpoints = new ArrayList<>();
        for (BlockPos pipePos : pipes) {
            for (Direction direction : Direction.values()) {
                BlockPos target = pipePos.relative(direction);
                if (target.equals(excluded) || pipes.contains(target)) {
                    continue;
                }
                IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite());
                if (handler != null) {
                    endpoints.add(new Endpoint(target, direction.getOpposite(), handler));
                }
            }
        }
        endpoints.sort(Comparator
                .comparingInt((Endpoint endpoint) -> endpoint.pos().getX())
                .thenComparingInt(endpoint -> endpoint.pos().getY())
                .thenComparingInt(endpoint -> endpoint.pos().getZ())
                .thenComparingInt(endpoint -> endpoint.side().ordinal()));
        return new EndpointSelection(cursorKey(type, pipes), List.copyOf(endpoints));
    }

    private static EndpointCursorKey cursorKey(HbmFluidDefinition type, Set<BlockPos> pipes) {
        BlockPos anchor = pipes.stream()
                .min(Comparator
                        .comparingInt((BlockPos pos) -> pos.getX())
                        .thenComparingInt(pos -> pos.getY())
                        .thenComparingInt(pos -> pos.getZ()))
                .orElse(BlockPos.ZERO)
                .immutable();
        return new EndpointCursorKey(type == null ? "none" : type.name(), anchor);
    }

    private static BalanceSnapshot createBalanceSnapshot(Level level, long gameTime, Set<PipeComponent> components) {
        List<BalanceComponentSnapshot> snapshots = new ArrayList<>();
        for (PipeComponent component : components) {
            HbmFluidDefinition type = HbmFluids.byName(component.typeName()).orElse(HbmFluids.none());
            if (type.isNone()) {
                continue;
            }

            List<BalanceEndpointSnapshot> endpoints = new ArrayList<>();
            FluidStack probe = HbmFluids.toNeoStack(type, MAX_BALANCE_PER_ENDPOINT);
            for (Endpoint endpoint : endpoints(level, type, component.pipes(), null).endpoints()) {
                int demand = Math.max(0, endpoint.handler().fill(probe, IFluidHandler.FluidAction.SIMULATE));
                FluidStack availableStack = endpoint.handler().drain(probe, IFluidHandler.FluidAction.SIMULATE);
                int available = HbmFluids.fromNeoFluid(availableStack.getFluid())
                        .filter(definition -> definition == type)
                        .map(ignored -> Math.max(0, availableStack.getAmount()))
                        .orElse(0);
                if (demand > 0 || available > 0) {
                    endpoints.add(new BalanceEndpointSnapshot(
                            new EndpointKey(endpoint.pos(), endpoint.side()),
                            available,
                            demand,
                            PowerEndpoint.ConnectionPriority.NORMAL
                    ));
                }
            }
            if (!endpoints.isEmpty()) {
                snapshots.add(new BalanceComponentSnapshot(type.name(), endpoints));
            }
        }
        return new BalanceSnapshot(gameTime, snapshots);
    }

    private static BalanceResult solveBalance(BalanceSnapshot snapshot) {
        List<BalanceComponentResult> results = new ArrayList<>();
        for (BalanceComponentSnapshot component : snapshot.components()) {
            BalanceComponentResult result = solveBalanceComponent(component, snapshot.gameTime());
            if (!result.providerAllocations().isEmpty() && !result.receiverAllocations().isEmpty()) {
                results.add(result);
            }
        }
        return new BalanceResult(results);
    }

    private static BalanceComponentResult solveBalanceComponent(BalanceComponentSnapshot component, long gameTime) {
        long totalAvailable = 0L;
        long totalDemand = 0L;
        for (BalanceEndpointSnapshot endpoint : component.endpoints()) {
            totalAvailable += endpoint.available();
            totalDemand += endpoint.demand();
        }
        int planned = (int) Math.min(Integer.MAX_VALUE, Math.min(totalAvailable, totalDemand));
        if (planned <= 0) {
            return BalanceComponentResult.empty(component.typeName());
        }

        Map<EndpointKey, Integer> providers = distributeBalanceProviders(component.endpoints(), planned, gameTime + 17L);
        Map<EndpointKey, Integer> receivers = distributeBalanceReceivers(component.endpoints(), planned, gameTime);
        return new BalanceComponentResult(component.typeName(), providers, receivers);
    }

    private static Map<EndpointKey, Integer> distributeBalanceReceivers(
            List<BalanceEndpointSnapshot> endpoints,
            int planned,
            long rotationSeed
    ) {
        Map<EndpointKey, Integer> allocations = new HashMap<>();
        int remaining = planned;
        for (int i = PowerEndpoint.ConnectionPriority.VALUES.length - 1; i >= 0 && remaining > 0; i--) {
            PowerEndpoint.ConnectionPriority priority = PowerEndpoint.ConnectionPriority.VALUES[i];
            List<BalanceShareTarget> targets = rotateBalanceTargets(endpoints.stream()
                    .filter(endpoint -> endpoint.demand() > 0 && endpoint.priority() == priority)
                    .map(endpoint -> new BalanceShareTarget(endpoint.key(), endpoint.demand()))
                    .toList(), rotationSeed + i * 31L);
            long priorityDemand = 0L;
            for (BalanceShareTarget target : targets) {
                priorityDemand += target.capacity();
            }
            int priorityPlanned = (int) Math.min(remaining, priorityDemand);
            allocations.putAll(distributeBalanceShares(targets, priorityDemand, priorityPlanned));
            remaining -= priorityPlanned;
        }
        return allocations;
    }

    private static Map<EndpointKey, Integer> distributeBalanceProviders(
            List<BalanceEndpointSnapshot> endpoints,
            int planned,
            long rotationSeed
    ) {
        List<BalanceShareTarget> targets = rotateBalanceTargets(endpoints.stream()
                .filter(endpoint -> endpoint.available() > 0)
                .map(endpoint -> new BalanceShareTarget(endpoint.key(), endpoint.available()))
                .toList(), rotationSeed);
        long totalAvailable = 0L;
        for (BalanceShareTarget target : targets) {
            totalAvailable += target.capacity();
        }
        return distributeBalanceShares(targets, totalAvailable, planned);
    }

    private static Map<EndpointKey, Integer> distributeBalanceShares(List<BalanceShareTarget> targets, long total, int planned) {
        Map<EndpointKey, Integer> allocations = new HashMap<>();
        if (targets.isEmpty() || total <= 0L || planned <= 0) {
            return allocations;
        }

        int remaining = planned;
        List<BalanceShare> shares = new ArrayList<>();
        for (BalanceShareTarget target : targets) {
            long numerator = (long) planned * target.capacity();
            int allocated = (int) Math.min(target.capacity(), numerator / total);
            long remainder = numerator % total;
            remaining -= allocated;
            BalanceShare share = new BalanceShare(target.key(), target.capacity(), allocated, remainder);
            shares.add(share);
        }

        List<BalanceShare> byRemainder = new ArrayList<>(shares);
        byRemainder.sort(Comparator.comparingLong((BalanceShare share) -> share.remainder).reversed());
        for (BalanceShare share : byRemainder) {
            if (remaining <= 0) {
                break;
            }
            if (share.allocated < share.capacity) {
                share.allocated++;
                remaining--;
            }
        }
        for (BalanceShare share : shares) {
            if (share.allocated > 0) {
                allocations.put(share.key, share.allocated);
            }
        }
        return allocations;
    }

    private static List<BalanceShareTarget> rotateBalanceTargets(List<BalanceShareTarget> targets, long seed) {
        if (targets.size() <= 1) {
            return targets;
        }
        int offset = (int) Math.floorMod(seed, (long) targets.size());
        if (offset == 0) {
            return targets;
        }
        List<BalanceShareTarget> rotated = new ArrayList<>(targets.size());
        rotated.addAll(targets.subList(offset, targets.size()));
        rotated.addAll(targets.subList(0, offset));
        return rotated;
    }

    private static void applyBalance(Level level, BalanceResult result) {
        for (BalanceComponentResult component : result.components()) {
            HbmFluidDefinition type = HbmFluids.byName(component.typeName()).orElse(HbmFluids.none());
            if (type.isNone()) {
                continue;
            }
            int drained = drainBalanceProviders(level, type, component.providerAllocations());
            if (drained <= 0) {
                continue;
            }
            Map<EndpointKey, Integer> receiverAllocations = component.receiverAllocations();
            int plannedReceivers = 0;
            for (int amount : receiverAllocations.values()) {
                plannedReceivers += amount;
            }
            if (drained < plannedReceivers) {
                List<BalanceShareTarget> targets = new ArrayList<>();
                for (Map.Entry<EndpointKey, Integer> entry : receiverAllocations.entrySet()) {
                    targets.add(new BalanceShareTarget(entry.getKey(), entry.getValue()));
                }
                receiverAllocations = distributeBalanceShares(targets, plannedReceivers, drained);
            }
            fillBalanceReceivers(level, type, receiverAllocations);
        }
    }

    private static int drainBalanceProviders(Level level, HbmFluidDefinition type, Map<EndpointKey, Integer> allocations) {
        int drained = 0;
        for (Map.Entry<EndpointKey, Integer> entry : allocations.entrySet()) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, entry.getKey().pos(), entry.getKey().side());
            if (handler == null) {
                continue;
            }
            FluidStack stack = handler.drain(HbmFluids.toNeoStack(type, entry.getValue()), IFluidHandler.FluidAction.EXECUTE);
            if (!stack.isEmpty() && HbmFluids.fromNeoFluid(stack.getFluid()).filter(definition -> definition == type).isPresent()) {
                drained += stack.getAmount();
            }
        }
        return drained;
    }

    private static void fillBalanceReceivers(Level level, HbmFluidDefinition type, Map<EndpointKey, Integer> allocations) {
        for (Map.Entry<EndpointKey, Integer> entry : allocations.entrySet()) {
            IFluidHandler handler = level.getCapability(Capabilities.FluidHandler.BLOCK, entry.getKey().pos(), entry.getKey().side());
            if (handler != null) {
                handler.fill(HbmFluids.toNeoStack(type, entry.getValue()), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private static Set<BlockPos> cachedPipes(Level level, BlockPos start, HbmFluidDefinition type) {
        Set<BlockPos> cached = network(level).componentFor(start, type);
        return cached == null ? collectPipes(level, start, type) : cached;
    }

    private static Set<BlockPos> collectPipes(Level level, BlockPos start, HbmFluidDefinition type) {
        Set<BlockPos> visited = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);

        while (!queue.isEmpty() && visited.size() < MAX_PIPE_SEARCH) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (!(blockEntity instanceof FluidPipeBlockEntity pipe) || !pipe.canConnect(type)) {
                visited.remove(pos);
                continue;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (visited.contains(next)) {
                    continue;
                }
                BlockEntity neighbor = level.getBlockEntity(next);
                if (neighbor instanceof FluidPipeBlockEntity nextPipe && nextPipe.canConnect(type)) {
                    queue.addLast(next);
                }
            }
        }
        return visited;
    }

    private record Endpoint(BlockPos pos, Direction side, IFluidHandler handler) {
    }

    private record EndpointSelection(EndpointCursorKey cursorKey, List<Endpoint> endpoints) {
    }

    private record EndpointCursorKey(String typeName, BlockPos anchor) {
    }

    private enum CursorKind {
        FILL,
        DRAIN
    }

    private record EndpointKey(BlockPos pos, Direction side) {
    }

    private record BalanceSnapshot(long gameTime, List<BalanceComponentSnapshot> components) {
    }

    private record BalanceComponentSnapshot(String typeName, List<BalanceEndpointSnapshot> endpoints) {
    }

    private record BalanceEndpointSnapshot(
            EndpointKey key,
            int available,
            int demand,
            PowerEndpoint.ConnectionPriority priority
    ) {
    }

    private record BalanceResult(List<BalanceComponentResult> components) {
        private static final BalanceResult EMPTY = new BalanceResult(List.of());
    }

    private record BalanceComponentResult(
            String typeName,
            Map<EndpointKey, Integer> providerAllocations,
            Map<EndpointKey, Integer> receiverAllocations
    ) {
        private static BalanceComponentResult empty(String typeName) {
            return new BalanceComponentResult(typeName, Map.of(), Map.of());
        }
    }

    private record BalanceShareTarget(EndpointKey key, int capacity) {
    }

    private static final class BalanceShare {
        private final EndpointKey key;
        private final int capacity;
        private int allocated;
        private final long remainder;

        private BalanceShare(EndpointKey key, int capacity, int allocated, long remainder) {
            this.key = key;
            this.capacity = capacity;
            this.allocated = allocated;
            this.remainder = remainder;
        }
    }

    private static LevelNetwork network(Level level) {
        return NETWORKS.computeIfAbsent(level.dimension(), key -> new LevelNetwork());
    }

    private static final class LevelNetwork {
        private final Map<BlockPos, PipeNode> pipes = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> fillCursors = new HashMap<>();
        private final Map<EndpointCursorKey, Integer> drainCursors = new HashMap<>();
        private Map<PipeComponentKey, PipeComponent> componentsByPipe = Map.of();
        private CompletableFuture<SolveResult> inFlight;
        private CompletableFuture<BalanceResult> balanceInFlight;
        private long graphVersion;
        private long lastBalanceTick = Long.MIN_VALUE;
        private boolean dirty = true;

        synchronized void upsert(BlockPos pos, Set<String> typeNames, boolean open) {
            PipeNode next = new PipeNode(pos.immutable(), Set.copyOf(typeNames), open);
            PipeNode old = this.pipes.put(next.pos(), next);
            if (!next.equals(old)) {
                markDirty();
            }
            tick();
        }

        synchronized void remove(BlockPos pos) {
            if (this.pipes.remove(pos.immutable()) != null) {
                markDirty();
            }
            tick();
        }

        synchronized Set<BlockPos> componentFor(BlockPos start, HbmFluidDefinition type) {
            tick();
            PipeComponent component = this.componentsByPipe.get(new PipeComponentKey(start.immutable(), type == null ? "none" : type.name()));
            if (component == null || type == null || type.isNone()) {
                return null;
            }
            return component.pipes();
        }

        synchronized int cursor(CursorKind kind, EndpointCursorKey key, int size) {
            if (size <= 0) {
                return 0;
            }
            return cursorMap(kind).getOrDefault(key, 0) % size;
        }

        synchronized void advance(CursorKind kind, EndpointCursorKey key, int nextIndex, int size) {
            if (size <= 0) {
                return;
            }
            cursorMap(kind).put(key, Math.floorMod(nextIndex, size));
        }

        void tickBalance(Level level) {
            BalanceResult completed = null;
            Set<PipeComponent> components = Set.of();
            synchronized (this) {
                tick();
                if (this.balanceInFlight != null && this.balanceInFlight.isDone()) {
                    completed = this.balanceInFlight.join();
                    this.balanceInFlight = null;
                }
                if (!this.componentsByPipe.isEmpty()) {
                    components = Set.copyOf(this.componentsByPipe.values());
                }
            }

            if (completed != null) {
                applyBalance(level, completed);
            }

            long gameTime = level.getGameTime();
            if (components.isEmpty()) {
                return;
            }
            synchronized (this) {
                if (this.balanceInFlight != null || this.lastBalanceTick == gameTime) {
                    return;
                }
                this.lastBalanceTick = gameTime;
            }

            BalanceSnapshot snapshot = createBalanceSnapshot(level, gameTime, components);
            if (snapshot.components().isEmpty()) {
                return;
            }

            synchronized (this) {
                if (this.balanceInFlight == null) {
                    this.balanceInFlight = CompletableFuture.supplyAsync(() -> solveBalance(snapshot), SOLVER);
                }
            }
        }

        private void markDirty() {
            this.dirty = true;
            this.graphVersion++;
            this.componentsByPipe = Map.of();
        }

        private Map<EndpointCursorKey, Integer> cursorMap(CursorKind kind) {
            return kind == CursorKind.FILL ? this.fillCursors : this.drainCursors;
        }

        private void tick() {
            if (this.inFlight != null && this.inFlight.isDone()) {
                SolveResult result = this.inFlight.join();
                this.inFlight = null;
                if (result.graphVersion() == this.graphVersion) {
                    this.componentsByPipe = result.componentsByPipe();
                }
            }

            if (this.dirty && this.inFlight == null) {
                SolveSnapshot snapshot = new SolveSnapshot(this.graphVersion, Map.copyOf(this.pipes));
                this.dirty = false;
                this.inFlight = CompletableFuture.supplyAsync(() -> solve(snapshot), SOLVER);
            }
        }
    }

    private static SolveResult solve(SolveSnapshot snapshot) {
        Map<PipeComponentKey, PipeComponent> componentsByPipe = new HashMap<>();
        Set<PipeComponentKey> visited = new HashSet<>();

        for (PipeNode node : snapshot.pipes().values()) {
            if (!node.active()) {
                continue;
            }

            for (String typeName : node.typeNames()) {
                PipeComponentKey startKey = new PipeComponentKey(node.pos(), typeName);
                if (visited.contains(startKey)) {
                    continue;
                }

                List<BlockPos> component = new ArrayList<>();
                ArrayDeque<BlockPos> queue = new ArrayDeque<>();
                queue.add(node.pos());

                while (!queue.isEmpty() && component.size() < MAX_PIPE_SEARCH) {
                    BlockPos current = queue.removeFirst();
                    PipeComponentKey currentKey = new PipeComponentKey(current, typeName);
                    if (!visited.add(currentKey)) {
                        continue;
                    }
                    PipeNode currentNode = snapshot.pipes().get(current);
                    if (currentNode == null || !currentNode.active() || !currentNode.typeNames().contains(typeName)) {
                        continue;
                    }
                    component.add(current);

                    for (Direction direction : Direction.values()) {
                        BlockPos next = current.relative(direction).immutable();
                        PipeComponentKey nextKey = new PipeComponentKey(next, typeName);
                        if (!visited.contains(nextKey)) {
                            PipeNode nextNode = snapshot.pipes().get(next);
                            if (nextNode != null && nextNode.active() && nextNode.typeNames().contains(typeName)) {
                                queue.addLast(next);
                            }
                        }
                    }
                }

                if (!component.isEmpty()) {
                    Set<BlockPos> immutablePipes = Set.copyOf(component);
                    PipeComponent pipeComponent = new PipeComponent(typeName, immutablePipes);
                    for (BlockPos pos : immutablePipes) {
                        componentsByPipe.put(new PipeComponentKey(pos, typeName), pipeComponent);
                    }
                }
            }
        }

        return new SolveResult(snapshot.graphVersion(), Map.copyOf(componentsByPipe));
    }

    private record PipeNode(BlockPos pos, Set<String> typeNames, boolean open) {
        private boolean active() {
            return open && !typeNames.isEmpty();
        }
    }

    private record PipeComponentKey(BlockPos pos, String typeName) {
    }

    private record PipeComponent(String typeName, Set<BlockPos> pipes) {
    }

    private record SolveSnapshot(long graphVersion, Map<BlockPos, PipeNode> pipes) {
    }

    private record SolveResult(long graphVersion, Map<PipeComponentKey, PipeComponent> componentsByPipe) {
    }
}
