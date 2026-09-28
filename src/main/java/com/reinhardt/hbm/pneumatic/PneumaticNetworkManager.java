package com.reinhardt.hbm.pneumatic;

import com.reinhardt.hbm.block.PneumaticStorageBlock;
import com.reinhardt.hbm.block.PneumaticTubeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * Event-invalidated pneumatic topology cache.  A connected component is only
 * traversed after placement/removal/chunk-load invalidates it; ordinary item
 * operations reuse the component and inspect only its storage endpoints.
 */
public final class PneumaticNetworkManager {
    private static final Map<ServerLevel, LevelState> STATES = new WeakHashMap<>();
    private static long nextRevision = 1L;

    private PneumaticNetworkManager() {
    }

    public record Entry(ItemStack stack, long amount) {
        public Entry {
            stack = stack.copyWithCount(1);
            amount = Math.max(0L, amount);
        }
    }

    public record Snapshot(long revision, List<Entry> entries) {
    }

    public static Snapshot snapshot(ServerLevel level, BlockPos accessor) {
        Component component = component(level, accessor);
        List<Entry> entries = new ArrayList<>();
        for (PneumaticStorageNode storage : storages(level, component, accessor)) {
            for (int slot = 0; slot < storage.pneumaticSlotCount(); slot++) {
                ItemStack prototype = storage.pneumaticPrototype(slot);
                long amount = storage.pneumaticAmount(slot);
                if (prototype.isEmpty() || amount <= 0L) {
                    continue;
                }
                Entry existing = null;
                for (Entry entry : entries) {
                    if (ItemStack.isSameItemSameComponents(entry.stack(), prototype)) {
                        existing = entry;
                        break;
                    }
                }
                if (existing == null) {
                    entries.add(new Entry(prototype, amount));
                } else {
                    int index = entries.indexOf(existing);
                    entries.set(index, new Entry(existing.stack(), saturatedAdd(existing.amount(), amount)));
                }
            }
        }
        entries.sort(Comparator
                .comparingLong(Entry::amount).reversed()
                .thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString()));
        return new Snapshot(component.revision, List.copyOf(entries));
    }

    /** Returns the quantity that could not be inserted. */
    public static long insert(ServerLevel level, BlockPos accessor, ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0L) {
            return Math.max(0L, amount);
        }
        Component component = component(level, accessor);
        long remaining = amount;
        List<PneumaticStorageNode> storages = storages(level, component, accessor);

        // Match occupied slots first so empty clutter slots do not fragment the
        // same item across the network unnecessarily.
        for (int pass = 0; pass < 2 && remaining > 0L; pass++) {
            for (PneumaticStorageNode storage : storages) {
                for (int slot = 0; slot < storage.pneumaticSlotCount() && remaining > 0L; slot++) {
                    ItemStack prototype = storage.pneumaticPrototype(slot);
                    boolean occupiedMatch = !prototype.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack);
                    if ((pass == 0) != occupiedMatch) {
                        continue;
                    }
                    remaining = storage.pneumaticInsert(slot, stack, remaining, simulate);
                }
            }
        }
        if (!simulate && remaining != amount) {
            component.revision = nextRevision++;
        }
        return remaining;
    }

    /** Returns the quantity actually extracted. */
    public static long extract(ServerLevel level, BlockPos accessor, ItemStack stack, long amount, boolean simulate) {
        if (stack.isEmpty() || amount <= 0L) {
            return 0L;
        }
        Component component = component(level, accessor);
        long remaining = amount;
        for (PneumaticStorageNode storage : storages(level, component, accessor)) {
            for (int slot = 0; slot < storage.pneumaticSlotCount() && remaining > 0L; slot++) {
                remaining -= storage.pneumaticExtract(slot, stack, remaining, simulate);
            }
            if (remaining <= 0L) {
                break;
            }
        }
        long extracted = amount - remaining;
        if (!simulate && extracted > 0L) {
            component.revision = nextRevision++;
        }
        return extracted;
    }

    public static long available(ServerLevel level, BlockPos accessor, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0L;
        }
        long amount = 0L;
        Component component = component(level, accessor);
        for (PneumaticStorageNode storage : storages(level, component, accessor)) {
            for (int slot = 0; slot < storage.pneumaticSlotCount(); slot++) {
                ItemStack prototype = storage.pneumaticPrototype(slot);
                if (!prototype.isEmpty() && ItemStack.isSameItemSameComponents(prototype, stack)) {
                    amount = saturatedAdd(amount, storage.pneumaticAmount(slot));
                }
            }
        }
        return amount;
    }

    public static void markContentsChanged(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelState state = state(serverLevel);
        Component component = state.byPosition.get(pos.asLong());
        if (component != null) {
            component.revision = nextRevision++;
        }
    }

    public static void invalidate(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        LevelState state = state(serverLevel);
        Set<Component> affected = java.util.Collections.newSetFromMap(new IdentityHashMap<>());
        Component own = state.byPosition.get(pos.asLong());
        if (own != null) {
            affected.add(own);
        }
        for (Direction direction : Direction.values()) {
            Component adjacent = state.byPosition.get(pos.relative(direction).asLong());
            if (adjacent != null) {
                affected.add(adjacent);
            }
        }
        for (Component component : affected) {
            for (long member : component.members) {
                if (state.byPosition.get(member) == component) {
                    state.byPosition.remove(member);
                }
            }
        }
    }

    private static Component component(ServerLevel level, BlockPos origin) {
        LevelState state = state(level);
        Component cached = state.byPosition.get(origin.asLong());
        if (cached != null) {
            return cached;
        }

        Set<Long> members = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin.immutable());
        while (!queue.isEmpty()) {
            BlockPos current = queue.removeFirst();
            long packed = current.asLong();
            if (members.contains(packed) || !level.hasChunkAt(current) || !isPneumatic(level.getBlockState(current))) {
                continue;
            }
            members.add(packed);
            for (Direction direction : Direction.values()) {
                BlockPos adjacent = current.relative(direction);
                if (!members.contains(adjacent.asLong()) && level.hasChunkAt(adjacent)
                        && isPneumatic(level.getBlockState(adjacent))) {
                    queue.addLast(adjacent.immutable());
                }
            }
        }
        Component created = new Component(members, nextRevision++);
        for (long member : members) {
            state.byPosition.put(member, created);
        }
        return created;
    }

    private static List<PneumaticStorageNode> storages(ServerLevel level, Component component, BlockPos accessor) {
        List<PneumaticStorageNode> result = new ArrayList<>();
        for (long packed : component.members) {
            BlockPos pos = BlockPos.of(packed);
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof PneumaticStorageNode storage && storage.pneumaticAvailableFrom(accessor)) {
                result.add(storage);
            }
        }
        result.sort(Comparator.comparingDouble(storage -> storage.pneumaticPosition().distSqr(accessor)));
        return result;
    }

    private static boolean isPneumatic(BlockState state) {
        return state.getBlock() instanceof PneumaticTubeBlock || state.getBlock() instanceof PneumaticStorageBlock;
    }

    private static LevelState state(ServerLevel level) {
        synchronized (STATES) {
            return STATES.computeIfAbsent(level, ignored -> new LevelState());
        }
    }

    private static long saturatedAdd(long left, long right) {
        if (right > 0L && left > Long.MAX_VALUE - right) {
            return Long.MAX_VALUE;
        }
        return left + right;
    }

    private static final class LevelState {
        private final Map<Long, Component> byPosition = new HashMap<>();
    }

    private static final class Component {
        private final Set<Long> members;
        private long revision;

        private Component(Set<Long> members, long revision) {
            this.members = Set.copyOf(members);
            this.revision = revision;
        }
    }
}
