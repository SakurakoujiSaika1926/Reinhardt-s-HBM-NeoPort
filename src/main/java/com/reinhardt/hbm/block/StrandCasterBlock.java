package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.MachineInventory;
import com.reinhardt.hbm.blockentity.StrandCasterBlockEntity;
import com.reinhardt.hbm.item.FoundryMoldItem;
import com.reinhardt.hbm.item.ScrewdriverItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.Set;

public class StrandCasterBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final Footprint FOOTPRINT = footprint();
    private static final VoxelShape SHAPE = Shapes.block();

    public StrandCasterBlock(Properties properties) {
        super(properties, FOOTPRINT, SHAPE, StrandCasterBlock::mapStrandCasterOffset);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        net.minecraft.core.Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState().setValue(FACING, facing);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StrandCasterBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (blockEntityType != HbmBlockEntities.STRAND_CASTER.get()) {
            return null;
        }
        return (tickerLevel, pos, tickerState, blockEntity) -> StrandCasterBlockEntity.tick(
                tickerLevel,
                pos,
                tickerState,
                (StrandCasterBlockEntity) blockEntity
        );
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isCrouching()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof MenuProvider menuProvider && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(menuProvider, buffer -> buffer.writeBlockPos(pos));
            return InteractionResult.CONSUME;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof StrandCasterBlockEntity caster) {
            if (stack.getItem() instanceof FoundryMoldItem && caster.installMold(stack, player)) {
                if (!level.isClientSide) {
                    level.playSound(null, pos, HbmSoundEvents.UPGRADE_PLUG.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (stack.is(ItemTags.SHOVELS) && caster.scrape(player)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
            if (ScrewdriverItem.isScrewdriver(stack) && caster.removeMold(player)) {
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide
                && level.getBlockEntity(pos) instanceof MachineInventory inventory) {
            inventory.dropContents(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private static Footprint footprint() {
        Set<BlockPos> offsets = new LinkedHashSet<>();
        offsets.addAll(Footprint.box(-1, 0, 0, 0, -6, 0).offsets());
        offsets.addAll(Footprint.box(-1, 0, 0, 2, -1, 0).offsets());
        for (BlockPos offset : StrandCasterBlockEntity.UNROTATED_BOTTOM_FLUID_PORTS) {
            offsets.add(offset);
        }
        for (BlockPos offset : StrandCasterBlockEntity.UNROTATED_TOP_POUR_PORTS) {
            offsets.add(offset);
        }
        return new Footprint(offsets.stream().toList());
    }

    public static Direction localSideDirection(BlockPos unrotatedOffset, Direction facing, boolean positiveRot) {
        return switch (facing) {
            case SOUTH -> positiveRot ? Direction.WEST : Direction.EAST;
            case EAST -> positiveRot ? Direction.SOUTH : Direction.NORTH;
            case NORTH -> positiveRot ? Direction.EAST : Direction.WEST;
            case WEST -> positiveRot ? Direction.NORTH : Direction.SOUTH;
            default -> throw new IllegalArgumentException("Strand caster facing must be horizontal: " + facing);
        };
    }

    public static BlockPos mapStrandCasterOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("Strand caster facing must be horizontal: " + facing);
        };
    }
}
