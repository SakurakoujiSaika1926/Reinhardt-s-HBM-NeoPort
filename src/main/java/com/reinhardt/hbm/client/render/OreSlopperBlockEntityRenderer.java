package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.OreSlopperBlock;
import com.reinhardt.hbm.blockentity.OreSlopperBlockEntity;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class OreSlopperBlockEntityRenderer implements BlockEntityRenderer<OreSlopperBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_ore_slopper_base");
    private static final ModelResourceLocation SLIDER = MachineModelRenderer.standalone("block/machine_ore_slopper_slider");
    private static final ModelResourceLocation HYDRAULICS = MachineModelRenderer.standalone("block/machine_ore_slopper_hydraulics");
    private static final ModelResourceLocation BUCKET = MachineModelRenderer.standalone("block/machine_ore_slopper_bucket");
    private static final ModelResourceLocation BLADES_LEFT = MachineModelRenderer.standalone("block/machine_ore_slopper_blades_left");
    private static final ModelResourceLocation BLADES_RIGHT = MachineModelRenderer.standalone("block/machine_ore_slopper_blades_right");
    private static final ModelResourceLocation FAN = MachineModelRenderer.standalone("block/machine_ore_slopper_fan");

    public OreSlopperBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(SLIDER);
        event.register(HYDRAULICS);
        event.register(BUCKET);
        event.register(BLADES_LEFT);
        event.register(BLADES_RIGHT);
        event.register(FAN);
    }

    @Override
    public void render(OreSlopperBlockEntity slopper, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = slopper.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        Direction legacyDir = OreSlopperBlock.legacyDirFromFacing(facing);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, legacyYaw(legacyDir));

        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        float slide = slopper.slider(partialTick);
        poseStack.translate(0.0F, 0.0F, slide * -3.0F);
        renderPart(SLIDER, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        float extend = slopper.bucket(partialTick) * 1.5F;
        poseStack.translate(0.0F, -Mth.clamp(extend - 0.25F, 0.0F, 1.25F), 0.0F);
        renderPart(HYDRAULICS, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, -Mth.clamp(extend, 0.0F, 1.25F), 0.0F);
        renderPart(BUCKET, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (slopper.clientLifting()) {
            renderLiftedOre(poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
        poseStack.popPose();

        float blades = slopper.blades(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.375F, 2.75F, 0.0F);
        rotateZ(poseStack, blades);
        poseStack.translate(-0.375F, -2.75F, 0.0F);
        renderPart(BLADES_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.375F, 2.75F, 0.0F);
        rotateZ(poseStack, -blades);
        poseStack.translate(0.375F, -2.75F, 0.0F);
        renderPart(BLADES_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.875F, -1.0F);
        rotateX(poseStack, -slopper.fan(partialTick));
        poseStack.translate(0.0F, -1.875F, 1.0F);
        renderPart(FAN, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(OreSlopperBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(5.0D, 5.0D, 5.0D);
    }

    private static void renderLiftedOre(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ItemStack stack = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW, BedrockOreItem.Grade.BASE, BedrockOreItem.Type.LIGHT_METAL);
        poseStack.pushPose();
        poseStack.translate(0.0625F, 4.3125F, 2.0F);
        rotateY(poseStack, 90.0F);
        rotateX(poseStack, -90.0F);
        poseStack.scale(1.75F, 1.75F, 1.75F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                null,
                0
        );
        poseStack.popPose();
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void rotateX(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F)));
    }

    private static void rotateY(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }

    private static void rotateZ(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F)));
    }
}
