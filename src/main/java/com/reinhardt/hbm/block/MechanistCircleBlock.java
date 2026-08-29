package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** 1.7.10 Mechanist's Circle: mech key starts the BOTPrime head spawn. */
public final class MechanistCircleBlock extends Block {
    public MechanistCircleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        ItemStack held = player.getMainHandItem();
        if (held.getItem() != BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("mech_key"))) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            var head = HbmEntityTypes.LEGACY_WORM_HEAD.get().create(level);
            if (head != null) {
                head.moveTo(pos.getX() + 0.5D, 300.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
                head.setDeltaMovement(0.0D, -1.0D, 0.0D);
                level.addFreshEntity(head);
                level.setBlock(pos, HbmBlocks.BRICK_JUNGLE_CRACKED.get().defaultBlockState(), Block.UPDATE_ALL);
                if (!player.getAbilities().instabuild) held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
