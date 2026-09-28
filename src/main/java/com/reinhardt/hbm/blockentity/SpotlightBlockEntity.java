package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.Consumer;

/** Publishes the connected fixture geometry without querying the world from section workers. */
public final class SpotlightBlockEntity extends BlockEntity {
    private static final Set<SpotlightBlockEntity> CLIENT_INSTANCES =
            Collections.newSetFromMap(new WeakHashMap<>());
    private static volatile Consumer<SpotlightBlockEntity> clientRenderSnapshotUpdater = spotlight -> { };
    private static volatile long clientRenderSnapshotRevision;
    private volatile RenderSnapshot renderSnapshot;

    public SpotlightBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.SPOTLIGHT.get(), pos, state);
        this.renderSnapshot = new RenderSnapshot(state, null, 0, null, new AABB(pos), -1L);
    }

    public static void setClientRenderSnapshotUpdater(Consumer<SpotlightBlockEntity> updater) {
        clientRenderSnapshotUpdater = updater == null ? spotlight -> { } : updater;
        clientRenderSnapshotRevision++;
        for (SpotlightBlockEntity spotlight : new ArrayList<>(CLIENT_INSTANCES)) {
            spotlight.refreshClientRenderSnapshot();
        }
    }

    public long clientRenderSnapshotRevision() {
        return clientRenderSnapshotRevision;
    }

    public RenderSnapshot renderSnapshot() {
        return this.renderSnapshot;
    }

    public void refreshClientRenderSnapshot() {
        if (this.level != null && this.level.isClientSide) {
            clientRenderSnapshotUpdater.accept(this);
        }
    }

    public void publishRenderSnapshot(RenderSnapshot next) {
        RenderSnapshot previous = this.renderSnapshot;
        this.renderSnapshot = next;
        if (this.level != null && this.level.isClientSide
                && !sameSectionCoverage(previous.bounds(), next.bounds())) {
            BlockState state = getBlockState();
            this.level.sendBlockUpdated(this.worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (this.level != null && this.level.isClientSide) {
            CLIENT_INSTANCES.add(this);
            refreshClientRenderSnapshot();
        }
    }

    @Override
    public void setBlockState(BlockState state) {
        super.setBlockState(state);
        refreshClientRenderSnapshot();
    }

    @Override
    public void setRemoved() {
        CLIENT_INSTANCES.remove(this);
        super.setRemoved();
    }

    private static boolean sameSectionCoverage(AABB first, AABB second) {
        return SectionPos.blockToSectionCoord(first.minX) == SectionPos.blockToSectionCoord(second.minX)
                && SectionPos.blockToSectionCoord(first.minY) == SectionPos.blockToSectionCoord(second.minY)
                && SectionPos.blockToSectionCoord(first.minZ) == SectionPos.blockToSectionCoord(second.minZ)
                && SectionPos.blockToSectionCoord(inclusiveMaximum(first.minX, first.maxX))
                == SectionPos.blockToSectionCoord(inclusiveMaximum(second.minX, second.maxX))
                && SectionPos.blockToSectionCoord(inclusiveMaximum(first.minY, first.maxY))
                == SectionPos.blockToSectionCoord(inclusiveMaximum(second.minY, second.maxY))
                && SectionPos.blockToSectionCoord(inclusiveMaximum(first.minZ, first.maxZ))
                == SectionPos.blockToSectionCoord(inclusiveMaximum(second.minZ, second.maxZ));
    }

    private static double inclusiveMaximum(double min, double max) {
        return max > min ? Math.nextDown(max) : max;
    }

    public record RenderSnapshot(BlockState blockState, Direction connectionDirection,
                                 int connectionCount, Object modelToken,
                                 AABB bounds, long revision) {
    }
}
