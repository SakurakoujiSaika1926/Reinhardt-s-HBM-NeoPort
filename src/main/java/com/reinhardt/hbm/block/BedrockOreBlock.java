package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.BedrockOreBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidStack;
import com.reinhardt.hbm.item.DrillbitItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public class BedrockOreBlock extends Block implements EntityBlock {
    public BedrockOreBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockOreBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore) {
            player.displayClientMessage(ore.statusLine(), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!player.isCreative() || stack.isEmpty() || !(level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            if (stack.getItem() instanceof DrillbitItem) {
                // 1.7.10 writes the held drillbit's actual tier, rather than incrementing the node.
                ore.setTier(DrillbitItem.typeOf(stack).tier());
            } else if (FluidUtil.getFluidHandler(stack.copyWithCount(1)).isPresent()) {
                FluidUtil.getFluidHandler(stack.copyWithCount(1)).ifPresent(handler -> {
                    net.neoforged.neoforge.fluids.FluidStack fluid = handler.drain(Integer.MAX_VALUE, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
                    if (!fluid.isEmpty()) {
                        HbmFluidDefinition type = HbmFluids.fromNeoFluid(fluid.getFluid()).orElse(HbmFluids.none());
                        ore.setAcidRequirement(type.isNone() ? HbmFluidStack.EMPTY : new HbmFluidStack(type, fluid.getAmount()));
                    }
                });
            } else {
                ore.configure(stack.copy(), ore.acidRequirement(), ore.color(), ore.tier(), level.random.nextInt(10));
            }
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.bedrock_ore.updated"), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    public ItemStack getCloneItemStack(BlockState state, BlockHitResult target, Level level, BlockPos pos, Player player) {
        return new ItemStack(this);
    }

    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return false;
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }
}
