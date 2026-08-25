package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.DroneCrateBlockEntity;
import com.reinhardt.hbm.blockentity.DroneDockBlockEntity;
import com.reinhardt.hbm.blockentity.DroneInventoryBlockEntity;
import com.reinhardt.hbm.blockentity.DroneProviderBlockEntity;
import com.reinhardt.hbm.blockentity.DroneRequesterBlockEntity;
import com.reinhardt.hbm.item.LegacyDroneLinkerItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.jetbrains.annotations.Nullable;

/** Full-cube 1.7.10 drone crate/dock family; only their block entities differ. */
public final class DroneNetworkContainerBlock extends Block implements EntityBlock {
    public enum Kind { CRATE, DOCK, PROVIDER, REQUESTER }

    private final Kind kind;

    public DroneNetworkContainerBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return switch (kind) {
            case CRATE -> new DroneCrateBlockEntity(pos, state);
            case DOCK -> new DroneDockBlockEntity(pos, state);
            case PROVIDER -> new DroneProviderBlockEntity(pos, state);
            case REQUESTER -> new DroneRequesterBlockEntity(pos, state);
        };
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (kind == Kind.CRATE && type == HbmBlockEntities.DRONE_CRATE.get()) {
            return (tickLevel, pos, tickState, entity) -> DroneCrateBlockEntity.tick(tickLevel, pos, tickState, (DroneCrateBlockEntity) entity);
        }
        if (kind == Kind.DOCK && type == HbmBlockEntities.DRONE_DOCK.get()) {
            return (tickLevel, pos, tickState, entity) -> DroneDockBlockEntity.tick(tickLevel, pos, tickState, (DroneDockBlockEntity) entity);
        }
        if (kind == Kind.PROVIDER && type == HbmBlockEntities.DRONE_PROVIDER.get()) {
            return (tickLevel, pos, tickState, entity) -> DroneProviderBlockEntity.tick(tickLevel, pos, tickState, (DroneProviderBlockEntity) entity);
        }
        if (kind == Kind.REQUESTER && type == HbmBlockEntities.DRONE_REQUESTER.get()) {
            return (tickLevel, pos, tickState, entity) -> DroneRequesterBlockEntity.tick(tickLevel, pos, tickState, (DroneRequesterBlockEntity) entity);
        }
        return null;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hit) {
        if (kind == Kind.CRATE && stack.getItem() instanceof LegacyDroneLinkerItem) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof MenuProvider provider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(provider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof DroneInventoryBlockEntity inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.block();
    }
}
