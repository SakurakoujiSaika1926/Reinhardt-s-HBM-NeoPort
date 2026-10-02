package com.reinhardt.hbm.mixin.tom;

import com.reinhardt.hbm.world.TomImpactWorldgen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraft.world.level.levelgen.placement.PlacementContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Consumer;
import java.util.stream.Stream;

/** Applies TOM's old decoration event decisions to final placement attempts. */
@Mixin(PlacedFeature.class)
public abstract class PlacedFeatureMixin {
    @Redirect(
            method = "placeWithContext",
            at = @At(value = "INVOKE", target = "Ljava/util/stream/Stream;forEach(Ljava/util/function/Consumer;)V")
    )
    private void reinhardtshbm$filterTomVegetation(
            Stream<BlockPos> positions,
            Consumer<BlockPos> placer,
            PlacementContext context,
            RandomSource random,
            BlockPos origin
    ) {
        switch (TomImpactWorldgen.decorationPolicy(context)) {
            case DENY -> {
                return;
            }
            case ONE_IN_NINE -> positions.filter(ignored -> random.nextInt(9) == 0).forEach(placer);
            case ALLOW -> positions.forEach(placer);
        }
    }
}
