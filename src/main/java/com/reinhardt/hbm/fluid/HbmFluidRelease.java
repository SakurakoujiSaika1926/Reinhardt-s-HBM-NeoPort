package com.reinhardt.hbm.fluid;

import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmHazardSystem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Central dispatcher for the legacy fluid-trait release callbacks.
 *
 * <p>The 1.7.10 implementation notified every trait attached to the released
 * fluid.  Keeping that dispatch in one place prevents spill/burn paths from
 * applying pollution while silently omitting vent radiation.</p>
 */
public final class HbmFluidRelease {
    private HbmFluidRelease() {
    }

    public static void onRelease(
            Level level,
            BlockPos pos,
            HbmFluidDefinition fluid,
            HbmPollution.ReleaseType releaseType,
            double millibuckets
    ) {
        if (level == null || pos == null || fluid == null || fluid.isNone() || millibuckets <= 0.0D) {
            return;
        }

        HbmPollution.polluteFluid(level, pos, fluid, releaseType, millibuckets);

        double radiationPerMillibucket = HbmHazardSystem.fluidRadiation(fluid);
        if (radiationPerMillibucket > 0.0D && level instanceof ServerLevel serverLevel) {
            ChunkRadiationData.get(serverLevel).incrementRadiation(pos, millibuckets * radiationPerMillibucket);
        }
    }
}
