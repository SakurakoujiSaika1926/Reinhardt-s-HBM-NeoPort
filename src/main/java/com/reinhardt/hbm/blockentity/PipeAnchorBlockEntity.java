package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PipeAnchorBlock;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class PipeAnchorBlockEntity extends FluidPipeBlockEntity {
    private final Set<BlockPos> links = new LinkedHashSet<>();

    public PipeAnchorBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.PIPE_ANCHOR.get(), pos, blockState);
    }

    public Direction externalDirection() {
        return getBlockState().getValue(PipeAnchorBlock.FACING).getOpposite();
    }

    @Override
    public boolean canConnectFrom(Direction direction, com.reinhardt.hbm.fluid.HbmFluidDefinition fluid) {
        return direction == externalDirection() && canConnect(fluid);
    }

    @Override
    public List<BlockPos> networkLinks() {
        return List.copyOf(links);
    }

    public boolean addLink(BlockPos other) {
        if (other.equals(worldPosition)) {
            return false;
        }
        boolean changed = links.add(other.immutable());
        if (changed) {
            markNetworkChanged();
        }
        return changed;
    }

    public boolean removeLink(BlockPos other) {
        boolean changed = links.remove(other);
        if (changed) {
            markNetworkChanged();
        }
        return changed;
    }

    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide) {
            for (BlockPos link : List.copyOf(links)) {
                if (level.getBlockEntity(link) instanceof PipeAnchorBlockEntity other) {
                    other.removeLink(worldPosition);
                }
            }
            links.clear();
        }
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        long[] packed = new long[links.size()];
        int index = 0;
        for (BlockPos link : links) {
            packed[index++] = link.asLong();
        }
        tag.putLongArray("Links", packed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        links.clear();
        for (long packed : tag.getLongArray("Links")) {
            links.add(BlockPos.of(packed));
        }
        if (level != null) {
            markNetworkChanged();
        }
    }
}
