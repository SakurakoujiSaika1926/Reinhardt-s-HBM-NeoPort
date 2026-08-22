package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.GasTurbineBlock;
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

public class GasTurbineBlockItem extends BlockItem {
    public GasTurbineBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof GasTurbineBlock gasTurbineBlock)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Direction facing = placeContext.getHorizontalDirection().getOpposite();
        BlockPos clickedPos = placeContext.getClickedPos();
        BlockPos corePos = GasTurbineBlock.legacyCorePos(clickedPos, facing);

        if (!GasTurbineBlock.canPlaceLegacyAt(level, clickedPos, facing, placeContext)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = gasTurbineBlock.defaultBlockState().setValue(GasTurbineBlock.FACING, facing);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        gasTurbineBlock.placeLegacyDummies(level, corePos, facing, context.getPlayer());
        SoundType sound = state.getSoundType(level, corePos, context.getPlayer());
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
