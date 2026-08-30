package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.RadioTorchBlock;
import com.reinhardt.hbm.blockentity.RadioTorchBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact rtty.obj world renderer and metadata rotation table from 1.7.10. */
public final class RadioTorchBlockEntityRenderer implements BlockEntityRenderer<RadioTorchBlockEntity> {
    private static final net.minecraft.client.resources.model.ModelResourceLocation[] MODELS = {
            model("sender"), model("receiver"), model("counter"),
            model("logic"), model("reader"), model("controller")
    };

    public RadioTorchBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (net.minecraft.client.resources.model.ModelResourceLocation model : MODELS) {
            event.register(model);
        }
    }

    @Override
    public void render(RadioTorchBlockEntity radio, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = radio.getBlockState();
        RadioTorchBlock.Kind kind = radio.kind();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.5D, 0.5D);

        // RenderRTTY rotates the model around its translated block centre. The
        // order and angles below are the old ForgeDirection metadata cases.
        switch (state.getValue(RadioTorchBlock.FACING)) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            case NORTH -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            }
            case EAST -> {
                poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }
        }

        ResourceLocation texture = texture(kind, radio.signal() > 0);
        MachineModelRenderer.renderUnculledUv(
                MachineModelRenderer.model(MODELS[kind.ordinal()]), poseStack, bufferSource,
                state, packedLight, packedOverlay, texture, 0.0F, 0.0F
        );
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RadioTorchBlockEntity radio) {
        return new AABB(radio.getBlockPos());
    }

    private static net.minecraft.client.resources.model.ModelResourceLocation model(String kind) {
        return MachineModelRenderer.standalone("block/radio_torch_" + kind);
    }

    private static ResourceLocation texture(RadioTorchBlock.Kind kind, boolean active) {
        String name = switch (kind) {
            case SENDER -> "rtty_sender_" + (active ? "on" : "off");
            case RECEIVER -> "rtty_rec_" + (active ? "on" : "off");
            case LOGIC -> "rtty_logic_" + (active ? "on" : "off");
            case COUNTER -> "rtty_counter";
            case READER -> "rtty_reader";
            case CONTROLLER -> "rtty_controller";
        };
        return com.reinhardt.hbm.ReinhardtsHBM.id("textures/block/" + name + ".png");
    }
}
