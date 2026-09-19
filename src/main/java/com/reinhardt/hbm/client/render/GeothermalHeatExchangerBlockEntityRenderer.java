package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.GeothermalHeatExchangerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class GeothermalHeatExchangerBlockEntityRenderer
        implements LongRangeBlockEntityRenderer<GeothermalHeatExchangerBlockEntity> {
    private static final ModelResourceLocation MAIN = MachineModelRenderer.standalone("block/machine_hephaestus_main");
    private static final ModelResourceLocation ROTOR = MachineModelRenderer.standalone("block/machine_hephaestus_rotor");
    private static final ModelResourceLocation CORE_COBBLESTONE =
            MachineModelRenderer.standalone("block/machine_hephaestus_core_cobblestone");
    private static final ModelResourceLocation CORE_LAVA =
            MachineModelRenderer.standalone("block/machine_hephaestus_core_lava");

    public GeothermalHeatExchangerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MAIN);
        event.register(ROTOR);
        event.register(CORE_COBBLESTONE);
        event.register(CORE_LAVA);
    }

    @Override
    public void render(GeothermalHeatExchangerBlockEntity exchanger, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        renderParts(exchanger.rotor(partialTick), exchanger.bufferedHeat() > 0,
                poseStack, bufferSource, exchanger.getBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    static void renderParts(float movement, boolean active, PoseStack poseStack, MultiBufferSource bufferSource,
                            BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.mulPose(yaw(movement));
        for (int index = 0; index < 3; index++) {
            MachineModelRenderer.renderUnculled(
                    MachineModelRenderer.model(ROTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.mulPose(yaw(120.0F));
        }
        poseStack.popPose();

        if (active) {
            MachineModelRenderer.renderUnculledTintedUv(
                    MachineModelRenderer.model(CORE_LAVA), poseStack, bufferSource, state,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay,
                    0xFFFFFFFF, 0.0F, movement / 10.0F, 0.5F, 0.5F);
        } else {
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(CORE_COBBLESTONE), poseStack, bufferSource, state,
                    packedLight, packedOverlay, 0xFF808080);
        }
    }

    @Override
    public AABB getRenderBoundingBox(GeothermalHeatExchangerBlockEntity exchanger) {
        return new AABB(
                exchanger.getBlockPos().getX() - 3.0D,
                exchanger.getBlockPos().getY(),
                exchanger.getBlockPos().getZ() - 3.0D,
                exchanger.getBlockPos().getX() + 4.0D,
                exchanger.getBlockPos().getY() + 12.0D,
                exchanger.getBlockPos().getZ() + 4.0D);
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public boolean shouldRenderOffScreen(GeothermalHeatExchangerBlockEntity exchanger) {
        return true;
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
