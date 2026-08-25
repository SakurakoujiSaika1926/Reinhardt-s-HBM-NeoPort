package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.ExcavatorBlock;
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
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ExcavatorBlockItem extends BlockItem {
    public ExcavatorBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof ExcavatorBlock excavatorBlock)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Direction facing = placeContext.getHorizontalDirection().getOpposite();
        BlockPos clickedPos = placeContext.getClickedPos();
        BlockPos corePos = ExcavatorBlock.legacyCorePos(clickedPos, facing);

        if (!level.getWorldBorder().isWithinBounds(corePos)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(corePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }
        if (!ExcavatorBlock.canPlaceLegacyAt(level, clickedPos, facing, placeContext)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = excavatorBlock.defaultBlockState().setValue(ExcavatorBlock.FACING, facing);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        excavatorBlock.setPlacedBy(level, corePos, state, context.getPlayer(), context.getItemInHand());
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
