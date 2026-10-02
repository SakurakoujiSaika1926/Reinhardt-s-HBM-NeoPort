package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.SatelliteDockBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Original RenderDecoBlock sat-dock transform, using the untouched 1.7.10 OBJ. */
public final class SatelliteDockBlockEntityRenderer implements LongRangeBlockEntityRenderer<SatelliteDockBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/sat_dock");

    public SatelliteDockBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(SatelliteDockBlockEntity dock, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = dock.getBlockState();
        poseStack.pushPose();
        // RenderDecoBlock's two 180 degree Z rotations cancel before the OBJ is drawn.
        poseStack.translate(0.5F, 0.0F, 0.5F);
        STATIC_MESHES.render(MODEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SatelliteDockBlockEntity dock) {
        BlockPos pos = dock.getBlockPos();
        // All face-used sat_dock.obj vertices after the +.5 X/Z origin shift.
        return new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 0.75D, pos.getZ() + 2.0D);
    }
}
