package com.reinhardt.hbm.block;

import com.reinhardt.hbm.blockentity.WandTandemBlockEntity;
import com.reinhardt.hbm.client.WandClientHooks;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.worldgen.structure.StructureWandBlockTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;
import net.neoforged.fml.loading.FMLEnvironment;

public class WandTandemBlock extends AbstractFacingWandBlock {
    public WandTandemBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new WandTandemBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return (tickerLevel, pos, tickerState, blockEntity) -> {
            if (blockEntity instanceof WandTandemBlockEntity tandem) {
                WandTandemBlockEntity.tick(tickerLevel, pos, tickerState, tandem);
            }
        };
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, net.minecraft.world.InteractionHand hand, BlockHitResult hitResult) {
        if (level.getBlockEntity(pos) instanceof WandTandemBlockEntity tandem && stack.is(Items.PAPER)) {
            if (!level.isClientSide) {
                CustomData data = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                CompoundTag tag = data.copyTag();
                if (tag.isEmpty()) {
                    stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, CustomData.of(tandem.paperConfigTag()));
                } else {
                    tandem.applyPaperConfig(tag);
                }
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (player.isShiftKeyDown() || isStructureWand(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof WandTandemBlockEntity tandem && stack.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock() == HbmBlocks.WAND_AIR.get()
                    ? net.minecraft.world.level.block.Blocks.AIR
                    : blockItem.getBlock();
            if (canSetReplacement(stack, block)) {
                if (!level.isClientSide) {
                    tandem.setReplacement(block, StructureWandBlockTarget.legacyMeta(block, stack));
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide);
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player.isShiftKeyDown()) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            openClientScreen(pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void openClientScreen(BlockPos pos) {
        if (FMLEnvironment.dist.isClient()) {
            WandClientHooks.openTandem(pos);
        }
    }
}
