package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.ChargerBlock;
import com.reinhardt.hbm.blockentity.ChargerBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Direct model-part port of 1.7.10 RenderCharger. */
public final class ChargerBlockEntityRenderer implements BlockEntityRenderer<ChargerBlockEntity> {
    private static final ModelResourceLocation BASE = part("charger_base");
    private static final ModelResourceLocation LEFT = part("charger_left");
    private static final ModelResourceLocation RIGHT = part("charger_right");
    private static final ModelResourceLocation LIGHT = part("charger_light");
    private static final ModelResourceLocation SLIDE = part("charger_slide");

    public ChargerBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(LEFT);
        event.register(RIGHT);
        event.register(LIGHT);
        event.register(SLIDE);
    }

    @Override
    public void render(ChargerBlockEntity charger, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = charger.getBlockState();
        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, worldYaw(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BASE), poseStack, bufferSource,
                state, packedLight, packedOverlay);

        double time = (charger.lastUsingTicks() +
                (charger.usingTicks() - charger.lastUsingTicks()) * partialTick) / (double) charger.delay();
        double extend = Math.min(1.0D, time * 2.0D);
        double swivel = Math.max(0.0D, (time - 0.5D) * 2.0D);

        poseStack.pushPose();
        poseStack.translate(-0.34375D, 0.25D, 0.0D);
        poseStack.mulPose(zRotation(10.0F));
        poseStack.translate(0.34375D, -0.25D, 0.0D);
        poseStack.translate(0.0D, -0.25D * extend, 0.0D);

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.28D, 0.0D);
        poseStack.mulPose(xRotation((float) (30.0D * swivel)));
        poseStack.translate(0.0D, -0.28D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LEFT), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.28D, 0.0D);
        poseStack.mulPose(xRotation((float) (-30.0D * swivel)));
        poseStack.translate(0.0D, -0.28D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RIGHT), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();

        MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(LIGHT), poseStack, bufferSource,
                state, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(-0.34375D, 0.25D, 0.0D);
        poseStack.mulPose(zRotation(10.0F));
        poseStack.translate(0.34375D, -0.25D, 0.0D);
        poseStack.translate(0.0D, -0.25D * extend, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SLIDE), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ChargerBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos());
    }

    private static ModelResourceLocation part(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    private static Quaternionf xRotation(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F));
    }

    private static Quaternionf zRotation(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F));
    }

    private static float worldYaw(BlockState state) {
        return switch (state.getValue(ChargerBlock.FACING)) {
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> 270.0F;
            case WEST -> 0.0F;
            default -> 90.0F;
        };
    }
}
