package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.ParticleAcceleratorBlock;
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
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class ParticleAcceleratorBlockItem extends BlockItem {
    public ParticleAcceleratorBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        ObjMachineBlockItem.installRenderer(consumer);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof ParticleAcceleratorBlock acceleratorBlock)) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Direction facing = placeContext.getHorizontalDirection().getOpposite();
        BlockPos corePos = ParticleAcceleratorBlock.legacyCorePos(placeContext.getClickedPos(), acceleratorBlock.kind());

        if (!acceleratorBlock.canPlaceLegacy(level, corePos, facing, placeContext)) {
            return InteractionResult.FAIL;
        }

        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = acceleratorBlock.defaultBlockState().setValue(LargeMachineBlock.FACING, facing);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }

        acceleratorBlock.setPlacedBy(level, corePos, state, context.getPlayer(), context.getItemInHand());
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
