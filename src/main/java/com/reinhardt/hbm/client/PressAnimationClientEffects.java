package com.reinhardt.hbm.client;

import com.reinhardt.hbm.blockentity.PressBlockEntity;
import com.reinhardt.hbm.network.PressAnimationPayload;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class PressAnimationClientEffects {
    private PressAnimationClientEffects() {
    }

    public static void accept(PressAnimationPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        BlockEntity blockEntity = minecraft.level.getBlockEntity(payload.pos());
        if (!(blockEntity instanceof PressBlockEntity press)) {
            return;
        }
        press.acceptAnimationSync(payload.progress(), payload.speed(), payload.delay(), payload.retracting());
        if (payload.impact()) {
            minecraft.level.playLocalSound(
                    payload.pos().getX() + 0.5D,
                    payload.pos().getY() + 0.5D,
                    payload.pos().getZ() + 0.5D,
                    HbmSoundEvents.PRESS_OPERATE.get(),
                    SoundSource.BLOCKS,
                    1.5F,
                    1.0F,
                    false
            );
        }
    }
}
