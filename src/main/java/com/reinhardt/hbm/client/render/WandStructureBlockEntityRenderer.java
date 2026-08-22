package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.WandStructureBlock;
import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class WandStructureBlockEntityRenderer implements BlockEntityRenderer<WandStructureBlockEntity> {
    public WandStructureBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WandStructureBlockEntity structure, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = structure.getBlockState();
        if (state.hasProperty(WandStructureBlock.LOAD) && state.getValue(WandStructureBlock.LOAD)) {
            return;
        }

        double maxX = Math.max(1, structure.sizeX);
        double maxY = Math.max(1, structure.sizeY) + 1.0D;
        double maxZ = Math.max(1, structure.sizeZ);
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
        BlockState state = structure.getBlockState();
        if (state.hasProperty(WandStructureBlock.LOAD) && state.getValue(WandStructureBlock.LOAD)) {
            return BlockEntityRenderer.super.getRenderBoundingBox(structure);
        }
        BlockPos pos = structure.getBlockPos();
        return new AABB(
                pos.getX(),
                pos.getY() + 1.0D,
                pos.getZ(),
                pos.getX() + Math.max(1, structure.sizeX),
                pos.getY() + Math.max(1, structure.sizeY) + 1.0D,
                pos.getZ() + Math.max(1, structure.sizeZ)
        );
    }

    @Override
    public boolean shouldRenderOffScreen(WandStructureBlockEntity structure) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
