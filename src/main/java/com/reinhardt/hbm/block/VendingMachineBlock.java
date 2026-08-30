package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.VendingMachineBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/** Direct port of the two-high, token-operated 1.7.10 vending machine. */
public final class VendingMachineBlock extends LargeMachineBlock implements EntityBlock {
    public static final IntegerProperty VARIANT = IntegerProperty.create("variant", 0, 1);
    private static final Footprint FOOTPRINT = Footprint.legacySouthBox(1, 0, 0, 0, 0, 0);

    public VendingMachineBlock(Properties properties) {
        super(properties, FOOTPRINT, RotationBasis.HBM_LEGACY_SOUTH);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(VARIANT, 0));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(VARIANT, variant(context.getItemInHand()));
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
        if (!stack.is(coinToken())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos corePos = corePos(level, pos);
        if (corePos == null || !(level.getBlockEntity(corePos) instanceof VendingMachineBlockEntity vending)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        ItemStack reward = vending.dispense(level.random);
        if (!reward.isEmpty()) {
            Direction direction = level.getBlockState(corePos).getValue(FACING);
            level.addFreshEntity(new ItemEntity(level,
                    corePos.getX() + 0.5D + direction.getStepX() * 0.75D,
                    corePos.getY() + 0.25D,
                    corePos.getZ() + 0.5D + direction.getStepZ() * 0.75D,
                    reward));
        }
        level.playSound(null, corePos, SoundEvents.DISPENSER_DISPENSE, SoundSource.BLOCKS, 1.0F, 0.75F);
        return ItemInteractionResult.CONSUME;
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
