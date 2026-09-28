package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.PowerPylonBlock;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;

/**
 * Common-side boundary for the client-owned pylon model geometry.
 *
 * <p>The block entity must remain loadable on a dedicated server, so it cannot
 * name the client renderer or any client-only class in
 * its constant pool. The client installs this provider during renderer
 * registration, before a client level can create pylon block entities.</p>
 */
public final class PowerPylonRenderGeometry {
    private static volatile Provider clientProvider;

    private PowerPylonRenderGeometry() {
    }

    public static void installClientProvider(Provider provider) {
        clientProvider = provider;
    }

    @Nullable
    public static GeometryResult capture(PowerPylonBlockEntity blockEntity, BlockState state,
                                         PowerPylonBlock.Kind kind, Direction facing) {
        Provider provider = clientProvider;
        return provider == null ? null : provider.capture(blockEntity, state, kind, facing);
    }

    @FunctionalInterface
    public interface Provider {
        GeometryResult capture(PowerPylonBlockEntity blockEntity, BlockState state,
                               PowerPylonBlock.Kind kind, Direction facing);
    }

    public record GeometryResult(Object geometryToken, AABB bounds, long revision) {
    }
}
