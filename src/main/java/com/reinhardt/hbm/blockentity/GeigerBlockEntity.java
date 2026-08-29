package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.radiation.HbmRadiationWorlds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;

/** Server-side ticking state of the old TileEntityGeiger. */
public final class GeigerBlockEntity extends BlockEntity {
    private static final List<DeferredHolder<SoundEvent, SoundEvent>> GEIGER_SOUNDS = List.of(
            HbmSoundEvents.GEIGER_1,
            HbmSoundEvents.GEIGER_2,
            HbmSoundEvents.GEIGER_3,
            HbmSoundEvents.GEIGER_4,
            HbmSoundEvents.GEIGER_5,
            HbmSoundEvents.GEIGER_6
    );

    private int timer;
    private float ticker;

    public GeigerBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.GEIGER.get(), pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, GeigerBlockEntity geiger) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        geiger.timer++;
        if (geiger.timer == 10) {
            geiger.timer = 0;
            geiger.ticker = (float) HbmRadiationWorlds.getRadiation(serverLevel, pos);
            level.updateNeighborsAt(pos, state.getBlock());
        }

        if (geiger.timer % 5 == 0) {
            if (geiger.ticker > 0.0F) {
                List<Integer> choices = new ArrayList<>();
                if (geiger.ticker < 1.0F) choices.add(0);
                if (geiger.ticker < 5.0F) choices.add(0);
                if (geiger.ticker < 10.0F) choices.add(1);
                if (geiger.ticker > 5.0F && geiger.ticker < 15.0F) choices.add(2);
                if (geiger.ticker > 10.0F && geiger.ticker < 20.0F) choices.add(3);
                if (geiger.ticker > 15.0F && geiger.ticker < 25.0F) choices.add(4);
                if (geiger.ticker > 20.0F && geiger.ticker < 30.0F) choices.add(5);
                if (geiger.ticker > 25.0F) choices.add(6);
                int sound = choices.get(level.random.nextInt(choices.size()));
                if (sound > 0) {
                    geiger.playSound(level, sound);
                }
            } else if (level.random.nextInt(50) == 0) {
                geiger.playSound(level, 1);
            }
        }
    }

    private void playSound(Level level, int oneBasedIndex) {
        int index = Math.max(1, Math.min(oneBasedIndex, GEIGER_SOUNDS.size())) - 1;
        level.playSound(null, worldPosition, GEIGER_SOUNDS.get(index).get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    public float radiation() {
        return ticker;
    }

    public int comparatorOutput() {
        return Math.min((int) Math.ceil(ticker / 5.0F), 15);
    }
}
