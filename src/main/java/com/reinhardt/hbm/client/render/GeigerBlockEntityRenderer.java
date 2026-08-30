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
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack,
                legacyYaw(state.getValue(GeigerBlock.FACING)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    public static void renderItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.GEIGER.get().defaultBlockState().setValue(GeigerBlock.FACING, Direction.NORTH);
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            // RenderGeiger's inventory renderer scales the OBJ by ten.
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        poseStack.translate(0.2F, 0.0F, 0.0F);
        poseStack.mulPose(MachineModelRenderer.yawQuaternion(90.0F));
        // RenderGeiger's item path renders the OBJ directly. The placed block
        // path owns the +0.5 model-origin translation used by renderAssembly.
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case WEST -> 90.0F;
            case SOUTH -> 180.0F;
            case EAST -> 270.0F;
            default -> 0.0F;
        };
    }
}
