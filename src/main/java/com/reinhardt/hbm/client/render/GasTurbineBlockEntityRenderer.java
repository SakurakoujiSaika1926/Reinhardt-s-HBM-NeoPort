package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.GasTurbineBlock;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class GasTurbineBlockEntityRenderer implements BlockEntityRenderer<GasTurbineBlockEntity> {
    static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/machine_turbine_gas_world");

    public GasTurbineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(GasTurbineBlockEntity turbine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turbine.getBlockState();
        Direction facing = state.hasProperty(GasTurbineBlock.FACING) ? state.getValue(GasTurbineBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyGasTurbineParts(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(GasTurbineBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(7.0D, 4.0D, 7.0D);
    }
}
