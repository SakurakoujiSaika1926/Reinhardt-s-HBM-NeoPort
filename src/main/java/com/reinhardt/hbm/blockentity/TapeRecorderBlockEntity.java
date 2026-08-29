package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Client animation state for the legacy redstone-powered tape recorder. */
public final class TapeRecorderBlockEntity extends BlockEntity {
    private float rotation;

    public TapeRecorderBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.TAPE_RECORDER.get(), pos, state);
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, TapeRecorderBlockEntity recorder) {
        if (level.hasNeighborSignal(pos)) {
            recorder.rotation += 3.0F;
            if (recorder.rotation >= 360.0F) {
                recorder.rotation -= 360.0F;
            }
        } else {
            recorder.rotation = 0.0F;
        }
    }

    public float rotation(float partialTick) {
        return rotation;
    }
}
