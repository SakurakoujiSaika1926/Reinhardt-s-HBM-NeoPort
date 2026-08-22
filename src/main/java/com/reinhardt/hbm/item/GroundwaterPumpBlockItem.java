package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.GroundwaterPumpBlock;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.client.render.GroundwaterPumpItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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

public class GroundwaterPumpBlockItem extends BlockItem {
    public GroundwaterPumpBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final GroundwaterPumpItemRenderer renderer = new GroundwaterPumpItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof GroundwaterPumpBlock pumpBlock)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Direction facing = placeContext.getHorizontalDirection().getOpposite();
        BlockPos targetPos = placeContext.getClickedPos();
        BlockPos corePos = targetPos.relative(facing.getOpposite());

        if (!level.getWorldBorder().isWithinBounds(corePos)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(corePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }
        if (!LargeMachineBlock.canPlaceFootprintAt(level, corePos, facing, GroundwaterPumpBlock.FOOTPRINT, placeContext)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = pumpBlock.defaultBlockState().setValue(GroundwaterPumpBlock.FACING, facing);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        pumpBlock.setPlacedBy(level, corePos, state, context.getPlayer(), context.getItemInHand());
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
