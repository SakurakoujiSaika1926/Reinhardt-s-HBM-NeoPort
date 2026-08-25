package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.FluidPipeBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidNetworks;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Direct 1.7.10 ItemAnalysisTool support for the modern fluid network. */
public final class LegacyAnalysisToolItem extends Item {
    public LegacyAnalysisToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = resolveCore(level, context.getClickedPos());
        if (!(level.getBlockEntity(pos) instanceof FluidPipeBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide && context.getPlayer() != null) {
            Player player = context.getPlayer();
            for (String line : HbmFluidNetworks.debugInfo(level, pos)) {
                player.sendSystemMessage(Component.literal(line).withStyle(ChatFormatting.YELLOW));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static BlockPos resolveCore(Level level, BlockPos clickedPos) {
        BlockEntity blockEntity = level.getBlockEntity(clickedPos);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        return clickedPos;
    }
}
