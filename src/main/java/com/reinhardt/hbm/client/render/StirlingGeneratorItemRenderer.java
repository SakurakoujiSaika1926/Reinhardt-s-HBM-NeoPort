package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class StirlingGeneratorItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final float MODEL_CENTER_X = 0.5F;
    private static final float MODEL_CENTER_Y = 1.28125F;
    private static final float MODEL_CENTER_Z = 0.5F;
    private static final float MODEL_FIT_SCALE = 1.05F / 3.0F;

    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_stirling_world");
    private static final ModelResourceLocation COG = MachineModelRenderer.standalone("block/machine_stirling_cog");
    private static final ModelResourceLocation COG_SMALL = MachineModelRenderer.standalone("block/machine_stirling_cog_small");
    private static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/machine_stirling_piston");
    private static final ModelResourceLocation WORLD_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_world");
    private static final ModelResourceLocation COG_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog");
    private static final ModelResourceLocation COG_SMALL_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog_small");
    private static final ModelResourceLocation PISTON_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_piston");
    private static final ModelResourceLocation WORLD_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_world");
    private static final ModelResourceLocation COG_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_cog");
    private static final ModelResourceLocation COG_SMALL_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_cog_small");
    private static final ModelResourceLocation PISTON_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_piston");

    public StirlingGeneratorItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        PartSet parts = PartSet.forStack(stack);
        BlockState state = parts.block.defaultBlockState();
        float rot = (System.currentTimeMillis() % 3600L) * 0.1F;

        poseStack.pushPose();
        fitModelToItemCube(poseStack);
        MachineModelRenderer.orientYaw(poseStack, 90.0F);

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.world), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.375F, 0.5F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-rot), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(-0.5F, -1.375F, -0.5F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.cog), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.5F, 1.375F, 0.75F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rot * 2.0F + 3.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(-0.5F, -1.375F, -0.75F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.cogSmall), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(rot * Math.PI / 90.0D) * 0.25D + 0.125D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.piston), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    private static void fitModelToItemCube(PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        poseStack.scale(MODEL_FIT_SCALE, MODEL_FIT_SCALE, MODEL_FIT_SCALE);
        poseStack.translate(-MODEL_CENTER_X, -MODEL_CENTER_Y, -MODEL_CENTER_Z);
    }

    private record PartSet(Block block, ModelResourceLocation world, ModelResourceLocation cog, ModelResourceLocation cogSmall, ModelResourceLocation piston) {
        private static PartSet forStack(ItemStack stack) {
            if (stack.getItem() instanceof BlockItem blockItem) {
                Block block = blockItem.getBlock();
                if (block == HbmBlocks.MACHINE_STIRLING_CREATIVE.get()) {
                    return new PartSet(block, WORLD_CREATIVE, COG_CREATIVE, COG_SMALL_CREATIVE, PISTON_CREATIVE);
                }
                if (block == HbmBlocks.MACHINE_STIRLING_STEEL.get()) {
                    return new PartSet(block, WORLD_STEEL, COG_STEEL, COG_SMALL_STEEL, PISTON_STEEL);
                }
            }
            return new PartSet(HbmBlocks.MACHINE_STIRLING.get(), WORLD, COG, COG_SMALL, PISTON);
        }
    }
}
