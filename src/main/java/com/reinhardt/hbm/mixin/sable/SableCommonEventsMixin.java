package com.reinhardt.hbm.mixin.sable;

import com.reinhardt.hbm.block.HbmGasBlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * HBM gases have no collision, occlusion, heat-map or floating-block
 * contribution.  Sable's general block-change hook otherwise rebuilds its
 * physics neighborhood for both halves of every gas movement.
 */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.SableCommonEvents", remap = false)
public abstract class SableCommonEventsMixin {
    @Inject(method = "handleBlockChange", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void reinhardtshbm$ignoreNonPhysicalGasChanges(
            ServerLevel level,
            LevelChunk chunk,
            int x,
            int y,
            int z,
            BlockState oldState,
            BlockState newState,
            CallbackInfo callbackInfo
    ) {
        boolean oldGas = oldState.getBlock() instanceof HbmGasBlock;
        boolean newGas = newState.getBlock() instanceof HbmGasBlock;

        // Only bypass air/gas transitions. A gas-to-solid or solid-to-gas
        // replacement must still update Sable's real collision geometry.
        boolean oldNonPhysical = oldState.isAir() || oldGas;
        boolean newNonPhysical = newState.isAir() || newGas;
        if ((oldGas || newGas) && oldNonPhysical && newNonPhysical) {
            callbackInfo.cancel();
        }
    }
}
