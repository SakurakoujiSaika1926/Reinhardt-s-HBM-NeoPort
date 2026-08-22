package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.ElectrolyzerBlock;
import com.reinhardt.hbm.blockentity.ElectrolyzerBlockEntity;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ElectrolyzerBlockEntityRenderer implements BlockEntityRenderer<ElectrolyzerBlockEntity> {
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_electrolyser_world");

    public ElectrolyzerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
    }

    @Override
    public void render(ElectrolyzerBlockEntity electrolyzer, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = electrolyzer.getBlockState();
        Direction facing = state.hasProperty(ElectrolyzerBlock.FACING) ? state.getValue(ElectrolyzerBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, LegacyMachineGeometry.legacyWavefrontYaw(facing, 0.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(WORLD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ElectrolyzerBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(7.0D, 5.0D, 7.0D);
    }
}
