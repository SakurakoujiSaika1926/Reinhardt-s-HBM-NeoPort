package com.reinhardt.hbm.block;

import com.reinhardt.hbm.pollution.HbmArmorProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PhosgeneGasBlock extends ChlorineGasBlock {
    public PhosgeneGasBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (level.isClientSide || !(entity instanceof LivingEntity living)
                || living instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }
        if (HbmArmorProtection.hasHeadProtection(living, HbmArmorProtection.HazardClass.GAS_LUNG, 1)) {
            return;
        }
        living.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 5 * 20, 0));
        living.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 20, 3));
        living.addEffect(new MobEffectInstance(MobEffects.WITHER, 3 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30 * 20, 1));
        living.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 30 * 20, 2));
    }
}
