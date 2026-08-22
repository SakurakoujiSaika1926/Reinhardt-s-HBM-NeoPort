package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.FluidBarrelBlock;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class FluidBarrelBlockEntity extends FluidTankBlockEntity {
    public FluidBarrelBlockEntity(BlockPos pos, BlockState blockState) {
        super(HbmBlockEntities.FLUID_BARREL.get(), pos, blockState, kind(blockState).capacity(), "container.reinhardtshbm.barrel");
    }

    @Override
    protected boolean canAcceptFluid(HbmFluidDefinition fluid) {
        return super.canAcceptFluid(fluid);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidBarrelBlockEntity barrel) {
        FluidTankBlockEntity.tick(level, pos, state, barrel);
        if (barrel.tank().amount() > 0 && !barrel.tank().type().isNone()) {
            barrel.checkFluidInteraction(level, pos);
        }
    }

    @Override
    public List<Port> ports(LevelAccessor level) {
        return Arrays.stream(Direction.values())
                .map(direction -> new Port(this.worldPosition, direction))
                .toList();
    }

    @Override
    protected boolean allowsFluidPort(BlockPos queriedPos, @Nullable Direction side) {
        return queriedPos.equals(this.worldPosition);
    }

    @Override
    public void dropContents(Level level, BlockPos pos) {
        dropInventoryContents(level, pos);
    }

    private void checkFluidInteraction(Level level, BlockPos pos) {
        HbmFluidDefinition fluid = this.tank().type();
        FluidBarrelBlock.Kind kind = kind(this.getBlockState());

        if (kind != FluidBarrelBlock.Kind.ANTIMATTER && fluid.hasTrait(HbmFluidTrait.ANTIMATTER)) {
            level.destroyBlock(pos, false);
            level.explode(
                    null,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.5D,
                    pos.getZ() + 0.5D,
                    5.0F,
                    true,
                    Level.ExplosionInteraction.BLOCK
            );
            return;
        }

        if (kind == FluidBarrelBlock.Kind.PLASTIC
                && (fluid.hasTrait(HbmFluidTrait.CORROSIVE) || fluid.temperatureCelsius() >= 100)) {
            level.destroyBlock(pos, false);
            level.playSound(
                    null,
                    pos,
                    SoundEvents.FIRE_EXTINGUISH,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
        }
    }

    private static FluidBarrelBlock.Kind kind(BlockState state) {
        if (state.getBlock() instanceof FluidBarrelBlock barrel) {
            return barrel.kind();
        }
        return FluidBarrelBlock.Kind.PLASTIC;
    }
}
