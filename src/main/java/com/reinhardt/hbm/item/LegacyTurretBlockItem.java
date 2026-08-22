package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.LegacyTurretBlock;
import com.reinhardt.hbm.client.render.TurretItemRenderer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
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

public class LegacyTurretBlockItem extends BlockItem {
    public LegacyTurretBlockItem(Block block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private final TurretItemRenderer renderer = new TurretItemRenderer();

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return this.renderer;
            }
        });
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(getBlock() instanceof LegacyTurretBlock turretBlock)) {
            return super.useOn(context);
        }
        Level level = context.getLevel();
        BlockPlaceContext placeContext = new BlockPlaceContext(context);
        Direction facing = placeContext.getHorizontalDirection().getOpposite();
        BlockPos clickedPos = placeContext.getClickedPos();
        BlockPos corePos = clickedPos.relative(facing, -turretBlock.type().placeOffset());

        if (!level.getWorldBorder().isWithinBounds(corePos)
                || !level.getBlockState(corePos).canBeReplaced(placeContext)
                || !turretBlock.canPlaceAt(level, corePos, facing, placeContext)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockState state = turretBlock.defaultBlockState().setValue(LegacyTurretBlock.FACING, facing);
        if (!level.setBlock(corePos, state, Block.UPDATE_ALL)) {
            return InteractionResult.FAIL;
        }
        turretBlock.setPlacedBy(level, corePos, state, context.getPlayer(), context.getItemInHand());
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
