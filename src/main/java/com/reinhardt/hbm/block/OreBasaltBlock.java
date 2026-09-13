package com.reinhardt.hbm.block;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

public final class OreBasaltBlock extends LegacyVariantBlock {
    private static final String[] DROPS = {
            "sulfur", "fluorite", "ingot_asbestos", "gem_volcanic", "powder_molysite"
    };

    public OreBasaltBlock(Properties properties) {
        super(properties, 4);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (!(level instanceof ServerLevel serverLevel)
                || state.getValue(VARIANT) != 2
                || !level.isEmptyBlock(pos.above())) {
            return;
        }
        if (level.random.nextInt(10) == 0) {
            level.setBlock(pos.above(), HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
        serverLevel.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + 0.5D, pos.getY() + 1.1D, pos.getZ() + 0.5D,
                5, 0.5D, 0.0D, 0.5D, 0.0D);
    }

    /** The asbestos metadata used BlockOutgas's on-break replacement in 1.7.10. */
    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state,
                              BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide && state.getValue(VARIANT) == 2) {
            level.setBlock(pos, HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
    }

    @Override
    public void onBlockExploded(BlockState state, Level level, BlockPos pos, Explosion explosion) {
        boolean asbestos = state.hasProperty(VARIANT) && state.getValue(VARIANT) == 2;
        super.onBlockExploded(state, level, pos, explosion);
        if (asbestos && !level.isClientSide) {
            level.setBlock(pos, HbmBlocks.GAS_ASBESTOS.get().defaultBlockState(), 3);
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int variant = Math.max(0, Math.min(4, state.getValue(VARIANT)));
        Item item = BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(DROPS[variant]));
        return item == null ? List.of() : List.of(new ItemStack(item));
    }
}
