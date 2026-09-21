package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.SoyuzCapsuleBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SoyuzCapsuleBlockEntityRenderer implements BlockEntityRenderer<SoyuzCapsuleBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/soyuz_capsule");

    public SoyuzCapsuleBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(SoyuzCapsuleBlockEntity capsule, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = capsule.getBlockState();
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(SoyuzCapsuleBlockEntity capsule) {
        // soyuz_capsule.obj spans x/z -5..+5 and y 0..13.607275.
        BlockPos pos = capsule.getBlockPos();
        return new AABB(
                pos.getX() - 5.0D,
                pos.getY(),
                pos.getZ() - 5.0D,
                pos.getX() + 5.0D,
                pos.getY() + 14.0D,
                pos.getZ() + 5.0D
        );
    }
}
