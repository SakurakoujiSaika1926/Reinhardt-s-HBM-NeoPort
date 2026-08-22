package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.LargeFluidTankBlock;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class LargeFluidTankBlockEntity extends FluidTankBlockEntity {
    public static final int CAPACITY = 16_000_000;
    private static final int FLOOR_CHECK_COUNT = 16;

    private boolean tilted;
    private int floorBlocksChecked;
    private int floorBlocksValid;

    public LargeFluidTankBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.LARGE_FLUID_TANK.get(), pos, state, CAPACITY,
                "container.reinhardtshbm.big_ass_tank");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, LargeFluidTankBlockEntity tank) {
        tank.checkTilt(level);
        FluidTankBlockEntity.tick(level, pos, state, tank);
        if (!tank.tank().type().isNone() && tank.tank().type().hasTrait(HbmFluidTrait.ANTIMATTER)) {
            level.destroyBlock(pos, false);
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                    10.0F, true, Level.ExplosionInteraction.BLOCK);
        }
    }

    public boolean tilted() {
        return this.tilted;
    }

    @Override
    public List<Port> ports(LevelAccessor level) {
        BlockState state = level.getBlockState(this.worldPosition);
        Direction facing = state.hasProperty(LargeFluidTankBlock.FACING)
                ? state.getValue(LargeFluidTankBlock.FACING)
                : Direction.SOUTH;
        return List.of(
                new Port(this.worldPosition.relative(facing, 6), facing),
                new Port(this.worldPosition.relative(facing.getOpposite(), 6), facing.getOpposite())
        );
    }

    @Override
    protected int maxProviderTransfer() {
        return Math.max(50_000, tank().amount() / 100);
    }

    @Override
    protected int maxReceiverTransfer() {
        return Math.max(50_000, (tank().capacity() - tank().amount()) / 100);
    }

    private void checkTilt(Level level) {
        if (Math.floorMod(level.getGameTime() + this.worldPosition.hashCode(), 20L) != 0L) {
            return;
        }

        if (this.floorBlocksChecked >= FLOOR_CHECK_COUNT) {
            boolean nextTilted = this.floorBlocksValid < this.floorBlocksChecked * 0.95D;
            if (nextTilted && !this.tilted) {
                level.playSound(null, this.worldPosition, HbmSoundEvents.METAL_IMPACT.get(),
                        SoundSource.BLOCKS, 3.0F, 1.0F);
            }
            if (nextTilted != this.tilted) {
                this.tilted = nextTilted;
                syncTilt();
            }
            this.floorBlocksChecked = 0;
            this.floorBlocksValid = 0;
        }

        int index = this.floorBlocksChecked;
        BlockPos floorPos = this.worldPosition.offset(-3 + index / 4 * 2, -1, -3 + index % 4 * 2);
        this.floorBlocksChecked++;
        if (isValidHeavyFoundation(level, floorPos)) {
            this.floorBlocksValid++;
        }
    }

    private static boolean isValidHeavyFoundation(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.isFaceSturdy(level, pos, Direction.UP) || !state.isSolidRender(level, pos)) {
            return false;
        }
        if (state.is(BlockTags.SAND) || state.is(BlockTags.WOOL) || state.is(BlockTags.DIRT)) {
            return false;
        }
        if (state.is(HbmBlocks.DIRT_DEAD.get()) || state.is(HbmBlocks.DIRT_OILY.get())
                || state.is(HbmBlocks.STONE_CRACKED.get())) {
            return false;
        }
        return state.getBlock().getExplosionResistance() >= Blocks.STONE.getExplosionResistance();
    }

    private void syncTilt() {
        setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("Tilted", this.tilted);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.tilted = tag.getBoolean("Tilted");
    }
}
