package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.VendingMachineBlockEntity;
import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.reinhardt.hbm.item.VendingMachineBlockItem;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/** Direct port of the two-high, token-operated 1.7.10 vending machine. */
public final class VendingMachineBlock extends LargeMachineBlock implements EntityBlock {
    @Override
    protected BlockState rotate(BlockState state, net.minecraft.world.level.block.Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, net.minecraft.world.level.block.Mirror mirror) {
        return state.setValue(FACING, mirror.getRotation(state.getValue(FACING)).rotate(state.getValue(FACING)));
    }

    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);
    private static final Footprint FOOTPRINT = Footprint.box(0, 0, 0, 1, 0, 0);

    public VendingMachineBlock(Properties properties) {
        super(properties, FOOTPRINT, VendingMachineBlock::mapVendingMachineOffset);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VARIANT, 0));
    }

    private static BlockPos mapVendingMachineOffset(Direction facing, BlockPos offset) {
        return switch (facing) {
            case SOUTH -> new BlockPos(offset.getX(), offset.getY(), offset.getZ());
            case EAST -> new BlockPos(offset.getZ(), offset.getY(), -offset.getX());
            case NORTH -> new BlockPos(-offset.getX(), offset.getY(), -offset.getZ());
            case WEST -> new BlockPos(-offset.getZ(), offset.getY(), offset.getX());
            default -> throw new IllegalArgumentException("Vending machine facing must be horizontal: " + facing);
        };
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        net.minecraft.core.Direction facing = context.getHorizontalDirection().getOpposite();
        if (!this.canPlaceMachineFootprintAt(context.getLevel(), context.getClickedPos(), facing, context)) {
            return null;
        }
        return this.defaultBlockState()
                .setValue(FACING, facing)
                .setValue(VARIANT, variant(context.getItemInHand()));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.ENTITYBLOCK_ANIMATED;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VendingMachineBlockEntity(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hitResult) {
        BlockPos corePos = corePos(level, pos);
        if (corePos == null) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return useOnPart(stack, level, pos, corePos, player);
    }

    static ItemInteractionResult useOnPart(ItemStack stack, Level level, BlockPos interactionPos,
                                           BlockPos corePos, Player player) {
        if (!isCoinToken(stack)
                || !(level.getBlockEntity(corePos) instanceof VendingMachineBlockEntity vending)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        // BlockVendingMachine consumed a token for every successful use,
        // including creative players.
        stack.shrink(1);
        ItemStack reward = vending.dispense(level.random);
        if (!reward.isEmpty()) {
            Direction direction = level.getBlockState(corePos).getValue(FACING);
            level.addFreshEntity(new ItemEntity(level,
                    corePos.getX() + 0.5D + direction.getStepX() * 0.75D,
                    corePos.getY() + 0.25D,
                    corePos.getZ() + 0.5D + direction.getStepZ() * 0.75D,
                    reward));
        }
        level.playSound(null, interactionPos, HbmSoundEvents.VENDING_MACHINE_DISPENSE.get(),
                SoundSource.BLOCKS, 1.0F, 0.75F);
        return ItemInteractionResult.CONSUME;
    }

    static boolean isCoinToken(ItemStack stack) {
        return stack.is(coinToken());
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return variantStack(state);
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(variantStack(state));
    }

    private ItemStack variantStack(BlockState state) {
        if (!(this.asItem() instanceof VendingMachineBlockItem vending)) {
            throw new IllegalStateException("Vending-machine block item is not variant-aware: " + this.asItem());
        }
        return LegacyVariantBlockItem.stackFor(vending, state.getValue(VARIANT));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<net.minecraft.world.level.block.Block, BlockState> builder) {
        builder.add(FACING, VARIANT);
    }

    private static Item coinToken() {
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ReinhardtsHBM.MOD_ID, "coin_token"));
    }

    @Nullable
    private static BlockPos corePos(Level level, BlockPos pos) {
        if (level.getBlockState(pos).is(HbmBlocks.VENDING_MACHINE.get())) {
            return pos;
        }
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy
                && level.getBlockState(dummy.getCorePos()).is(HbmBlocks.VENDING_MACHINE.get())) {
            return dummy.getCorePos();
        }
        return null;
    }

    private static int variant(ItemStack stack) {
        if (stack.getItem() instanceof com.reinhardt.hbm.item.VendingMachineBlockItem vending) {
            return vending.variantIndex(stack);
        }
        return 0;
    }
}
