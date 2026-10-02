package com.reinhardt.hbm.event;

import com.reinhardt.hbm.item.MeteorCharmItem;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/** Three-second pedestal broadcast cache used by the legacy meteor selector. */
public final class MeteorPedestalBroadcasts {
    private static final long TIMEOUT = 60L;
    private static final Map<ResourceKey<Level>, Map<BlockPos, Entry>> ENTRIES = new HashMap<>();

    private MeteorPedestalBroadcasts() {
    }

    public static void publish(ServerLevel level, BlockPos pos, ItemStack displayed) {
        if (!(displayed.getItem() instanceof MeteorCharmItem charm)) {
            return;
        }
        ENTRIES.computeIfAbsent(level.dimension(), ignored -> new HashMap<>())
                .put(pos.immutable(), new Entry(charm.kind(), level.getGameTime()));
    }

    public static Influence influence(ServerLevel level, BlockPos target) {
        Map<BlockPos, Entry> dimensionEntries = ENTRIES.get(level.dimension());
        if (dimensionEntries == null) {
            return Influence.NONE;
        }
        long minimumTime = level.getGameTime() - TIMEOUT;
        boolean repel = false;
        boolean suppress = false;
        Iterator<Map.Entry<BlockPos, Entry>> iterator = dimensionEntries.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, Entry> cached = iterator.next();
            if (cached.getValue().timestamp() < minimumTime) {
                iterator.remove();
                continue;
            }
            BlockPos pos = cached.getKey();
            if (Math.abs(pos.getX() - target.getX()) > 100 || Math.abs(pos.getZ() - target.getZ()) > 100) {
                continue;
            }
            if (cached.getValue().kind() == MeteorCharmItem.Kind.PROTECTION) {
                repel = true;
            } else if (cached.getValue().kind() == MeteorCharmItem.Kind.METEOR) {
                suppress = true;
            }
        }
        if (dimensionEntries.isEmpty()) {
            ENTRIES.remove(level.dimension());
        }
        return new Influence(repel, suppress);
    }

    private record Entry(MeteorCharmItem.Kind kind, long timestamp) {
    }

    public record Influence(boolean repel, boolean suppress) {
        private static final Influence NONE = new Influence(false, false);
    }
}
