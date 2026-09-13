package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyWormHeadEntity;
import com.reinhardt.hbm.registry.HbmEntityTypes;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.event.EventHooks;

/** 1.7.10 Mechanist's Circle: mech key starts the BOTPrime head spawn. */
public final class MechanistCircleBlock extends Block {
    public MechanistCircleBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos,
                                               Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.getItem() != BuiltInRegistries.ITEM.get(ReinhardtsHBM.id("mech_key"))) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            ServerLevel server = (ServerLevel) level;
            LegacyWormHeadEntity head = new LegacyWormHeadEntity(HbmEntityTypes.LEGACY_WORM_HEAD.get(), server);
            head.moveTo(pos.getX() + 0.5D, 300.0D, pos.getZ() + 0.5D, 0.0F, 0.0F);
            head.setDeltaMovement(0.0D, -1.0D, 0.0D);
            EventHooks.finalizeMobSpawn(head, server,
                    level.getCurrentDifficultyAt(pos), MobSpawnType.SPAWN_EGG, null);
            head.initializeLegacySpawn(server);
            level.addFreshEntity(head);
            level.setBlock(pos, HbmBlocks.BRICK_JUNGLE_CRACKED.get().defaultBlockState(), Block.UPDATE_ALL);
            // BlockBallsSpawner decremented its held mech_key unconditionally,
            // including for creative-mode players.
            held.shrink(1);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
