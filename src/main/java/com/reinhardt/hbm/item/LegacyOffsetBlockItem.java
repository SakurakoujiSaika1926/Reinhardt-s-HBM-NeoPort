package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LargeMachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;

public class LegacyOffsetBlockItem extends BlockItem {
    private final int legacyOffset;
    private final boolean legacyOppositeFacing;

    public LegacyOffsetBlockItem(Block block, Item.Properties properties, int legacyOffset, boolean legacyOppositeFacing) {
        super(block, properties);
        this.legacyOffset = legacyOffset;
        this.legacyOppositeFacing = legacyOppositeFacing;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof LargeMachineBlock machine)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Direction facing = this.legacyOppositeFacing
                ? placeContext.getHorizontalDirection().getOpposite()
                : placeContext.getHorizontalDirection();
        BlockPos clickedPos = placeContext.getClickedPos();
        BlockPos corePos = clickedPos.relative(facing, -this.legacyOffset);

        if (!level.getWorldBorder().isWithinBounds(corePos)) {
            return InteractionResult.FAIL;
        }
        if (!level.getBlockState(corePos).canBeReplaced(placeContext)) {
            return InteractionResult.FAIL;
        }
        if (!LargeMachineBlock.canPlaceFootprintAt(level, corePos, facing, machine.machineFootprint(), placeContext, machine.machineRotationBasis())) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = machine.defaultBlockState().setValue(LargeMachineBlock.FACING, facing);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        machine.setPlacedBy(level, corePos, state, context.getPlayer(), context.getItemInHand());
        playPlaceSound(level, corePos, state, context.getPlayer());

        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static void playPlaceSound(Level level, BlockPos pos, BlockState state, LivingEntity placer) {
        SoundType sound = state.getSoundType(level, pos, placer);
        level.playSound(
                placer instanceof net.minecraft.world.entity.player.Player player ? player : null,
                pos,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) * 0.5F,
                sound.getPitch() * 0.8F
        );
    }
}
