package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.GeigerBlock;
import com.reinhardt.hbm.blockentity.GeigerBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact RenderGeiger anchor and rotation table from 1.7.10. */
public final class GeigerBlockEntityRenderer implements BlockEntityRenderer<GeigerBlockEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("block/geiger");

    public GeigerBlockEntityRenderer(BlockEntityRendererProvider.Context ignored) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(GeigerBlockEntity geiger, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderAssembly(geiger.getBlockState(), poseStack, bufferSource, packedLight, packedOverlay);
    }

    public static void renderAssembly(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyGeigerTransform(poseStack, geigerFacing(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.GEIGER.get().defaultBlockState().setValue(GeigerBlock.FACING, Direction.NORTH);
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            // RenderGeiger#getRenderer#renderInventory
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        // RenderGeiger#getRenderer#renderCommon
        poseStack.translate(0.2F, 0.0F, 0.0F);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyGeigerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> { }
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Geiger counter facing must be horizontal: " + facing);
        }
    }

    private static Direction geigerFacing(BlockState state) {
        if (!state.hasProperty(GeigerBlock.FACING)) {
            throw new IllegalStateException("Geiger counter state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(GeigerBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Geiger counter state has a vertical facing: " + facing);
        }
        return facing;
    }

}
