package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.SoyuzLauncherBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class SoyuzLauncherBlockItem extends BlockItem {
    public SoyuzLauncherBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof SoyuzLauncherBlock launcher)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        BlockPos corePos = placeContext.getClickedPos().above(SoyuzLauncherBlock.HEIGHT_OFFSET);
        Direction facing = Direction.EAST;

        if (!level.getWorldBorder().isWithinBounds(corePos)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(corePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }
        if (!launcher.canPlaceAt(placeContext, corePos)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = launcher.defaultBlockState().setValue(LargeMachineBlock.FACING, facing);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        launcher.setPlacedBy(level, corePos, state, context.getPlayer(), context.getItemInHand());
        SoundType sound = state.getSoundType();
        level.playSound(
                context.getPlayer(),
                corePos,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) * 0.5F,
                sound.getPitch() * 0.8F
        );

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }
}
