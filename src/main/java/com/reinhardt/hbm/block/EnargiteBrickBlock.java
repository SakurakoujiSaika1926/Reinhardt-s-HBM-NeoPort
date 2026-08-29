package com.reinhardt.hbm.block;

import com.reinhardt.hbm.registry.HbmMobEffects;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** The two 1.7.10 enchanted enargite bricks keep their contact effects. */
public final class EnargiteBrickBlock extends Block {
    private final Kind kind;

    public EnargiteBrickBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
    }

    @Override
    protected void entityInside(BlockState state, Level level, net.minecraft.core.BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        if (level.isClientSide || !(entity instanceof LivingEntity living)) {
            return;
        }
        if (kind == Kind.RADIOACTIVE) {
            HbmLivingRadiation radiation = HbmLivingRadiation.get(living);
            radiation.addRadiation(0.45F);
            HbmLivingRadiation.set(living, radiation);
        } else {
            living.addEffect(new MobEffectInstance(HbmMobEffects.TAINT, 15 * 20, 2));
        }
    }

    public enum Kind {
        RADIOACTIVE,
        MYSTIC
    }
}
