package com.reinhardt.hbm.block;

import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/** 1.7.10 BlockOre behaviour for the two radioactive trinitite waste blocks. */
public final class TrinititeBlock extends Block {
    public TrinititeBlock(Properties properties) {
        super(properties);
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        super.stepOn(level, pos, state, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)
                || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        HbmLivingRadiation data = HbmLivingRadiation.get(living);
        data.addEnvironmentRadiation(1.0F);
        data.addRadiation(1.0F);
        HbmLivingRadiation.set(living, data);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                pos.getX() + random.nextDouble(), pos.getY() + 1.1D, pos.getZ() + random.nextDouble(),
                0.0D, 0.0D, 0.0D);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(HbmItems.TRINITITE.get()));
    }
}
