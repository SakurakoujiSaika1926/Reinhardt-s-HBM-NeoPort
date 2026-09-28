package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;

public class WandStructureBlockEntityRenderer implements LongRangeBlockEntityRenderer<WandStructureBlockEntity> {
    public WandStructureBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WandStructureBlockEntity structure, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        WandStructureBlockEntity.RenderSnapshot snapshot = structure.renderSnapshot();
        if (snapshot.load()) {
            return;
        }

        double maxX = snapshot.sizeX();
        double maxY = snapshot.sizeY() + 1.0D;
        double maxZ = snapshot.sizeZ();
        LevelRenderer.renderLineBox(
                poseStack,
                bufferSource.getBuffer(RenderType.lines()),
                0.0D,
                1.0D,
                0.0D,
                maxX,
                maxY,
                maxZ,
                1.0F,
                1.0F,
                1.0F,
                1.0F
        );
    }

    @Override
    public AABB getRenderBoundingBox(WandStructureBlockEntity structure) {
        return structure.renderSnapshot().bounds();
    }

}
