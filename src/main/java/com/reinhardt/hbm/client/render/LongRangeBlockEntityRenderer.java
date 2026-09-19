package com.reinhardt.hbm.client.render;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Opt-in renderer policy for large legacy OBJ machines whose geometry extends
 * far away from the owning block entity. Vanilla BE renderer distance/frustum
 * checks are centered on the BE, so tall towers and wide multiblocks can vanish
 * while part of their model is still clearly on screen.
 */
interface LongRangeBlockEntityRenderer<T extends BlockEntity> extends BlockEntityRenderer<T> {
    int LONG_RANGE_VIEW_DISTANCE = 256;

    @Override
    default boolean shouldRenderOffScreen(T blockEntity) {
        return true;
    }

    @Override
    default int getViewDistance() {
        return LONG_RANGE_VIEW_DISTANCE;
    }
}
