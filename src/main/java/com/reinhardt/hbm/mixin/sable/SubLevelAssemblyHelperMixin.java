package com.reinhardt.hbm.mixin.sable;

import com.reinhardt.hbm.integration.sable.HbmSableAssemblyCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional tail hook for Sable's direct chunk-to-chunk block mover. */
@Pseudo
@Mixin(targets = "dev.ryanhcode.sable.api.SubLevelAssemblyHelper", remap = false)
public abstract class SubLevelAssemblyHelperMixin {
    @Inject(method = "moveBlocks", at = @At("TAIL"), remap = false, require = 0)
    private static void reinhardtshbm$afterMoveBlocks(
            ServerLevel sourceLevel,
            @Coerce Object assemblyTransform,
            Iterable<BlockPos> sourcePositions,
            CallbackInfo callbackInfo
    ) {
        HbmSableAssemblyCompat.afterMoveBlocks(sourceLevel, assemblyTransform, sourcePositions);
    }
}
