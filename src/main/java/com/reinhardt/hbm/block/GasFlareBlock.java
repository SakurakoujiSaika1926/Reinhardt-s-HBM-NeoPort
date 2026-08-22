package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.GasFlareBlockEntity;
import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.power.PowerNetworkManager;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GasFlareBlock extends LargeMachineBlock implements EntityBlock {
    public static final Footprint FOOTPRINT = Footprint.legacySouthBox(11, 0, 1, 1, 1, 1);
    private static final List<Box> COLLISION = List.of(
            new Box(-1.5D, 0.0D, -1.5D, 1.5D, 3.875D, 1.5D),
            new Box(-0.75D, 3.875D, -0.75D, 0.75D, 9.0D, 0.75D),
            new Box(-1.5D, 9.0D, -1.5D, 1.5D, 9.375D, 1.5D),
            new Box(-0.75D, 9.375D, -0.75D, 0.75D, 12.0D, 0.75D)
    );

    public GasFlareBlock(Properties properties) {
        super(properties, FOOTPRINT, shapeForPart(BlockPos.ZERO), RotationBasis.HBM_LEGACY_SOUTH);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GasFlareBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> blockEntityType
    ) {
        if (blockEntityType != HbmBlockEntities.GAS_FLARE.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> GasFlareBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (GasFlareBlockEntity) blockEntity
        );
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            net.minecraft.world.entity.LivingEntity placer,
            net.minecraft.world.item.ItemStack stack
    ) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (!level.isClientSide) {
            refreshConnections(level, pos);
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!state.is(oldState.getBlock())) {
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        boolean removed = !state.is(newState.getBlock());
        if (removed && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
        if (removed) {
            refreshConnections(level, pos);
            PowerNetworkManager.markDirty(level);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    public static VoxelShape shapeForPart(BlockPos offsetFromCore) {
        VoxelShape shape = Shapes.empty();
        for (Box box : COLLISION) {
            shape = Shapes.or(shape, box.slice(offsetFromCore));
        }
        return shape;
    }

    private static void refreshConnections(Level level, BlockPos corePos) {
        if (level.isClientSide) {
            return;
        }
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            EnergyCableBlock.refreshConnections(level, corePos.relative(direction, 2));
            BlockPos ductPos = corePos.relative(direction, 2);
            BlockState ductState = level.getBlockState(ductPos);
            if (ductState.getBlock() instanceof FluidDuctBlock duct) {
                duct.refreshConnections(level, ductPos);
            }
        }
    }

    private record Box(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        private VoxelShape slice(BlockPos offset) {
            double localMinX = clamp(this.minX - offset.getX() + 0.5D);
            double localMaxX = clamp(this.maxX - offset.getX() + 0.5D);
            double localMinY = clamp(this.minY - offset.getY());
            double localMaxY = clamp(this.maxY - offset.getY());
            double localMinZ = clamp(this.minZ - offset.getZ() + 0.5D);
            double localMaxZ = clamp(this.maxZ - offset.getZ() + 0.5D);
            if (localMinX >= localMaxX || localMinY >= localMaxY || localMinZ >= localMaxZ) {
                return Shapes.empty();
            }
            return Shapes.box(localMinX, localMinY, localMinZ, localMaxX, localMaxY, localMaxZ);
        }

        private static double clamp(double value) {
            return Math.max(0.0D, Math.min(1.0D, value));
        }
    }
}
