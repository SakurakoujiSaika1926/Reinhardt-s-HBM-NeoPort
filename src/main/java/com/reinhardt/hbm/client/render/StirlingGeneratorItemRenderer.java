package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.item.StirlingGeneratorBlockItem;
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
import com.mojang.math.Axis;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public final class StirlingGeneratorItemRenderer extends BlockEntityWithoutLevelRenderer {
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
        boolean hasCog = StirlingGeneratorBlockItem.hasCog(stack);
        float rot = hasCog ? (System.currentTimeMillis() % 3600L) * 0.1F : 0.0F;

        poseStack.pushPose();
        switch (context) {
            case GUI -> {
                // RenderStirling#getRenderer's ItemRenderBase#INVENTORY,
                // followed by its own renderInventory and renderCommonWithStack.
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
                poseStack.translate(0.0F, 11.3F, -11.3F);
                poseStack.translate(0.0F, -1.5F, 0.0F);
                poseStack.scale(3.25F, 3.25F, 3.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                // Both map to the sole 1.7.10 EQUIPPED render type.
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                // 1.7.10 EQUIPPED_FIRST_PERSON: ItemRenderBase's 90 degrees,
                // then RenderStirling#renderCommonWithStack's 90 degrees.
                poseStack.translate(0.5F, 0.25F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case GROUND -> {
                // 1.7.10 ENTITY: 1.5 * 0.25, then the two authored Y rotations.
                poseStack.scale(0.375F, 0.375F, 0.375F);
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
            case NONE, HEAD, FIXED -> throw new IllegalArgumentException(
                    "RenderStirling had no 1.7.10 ItemRenderType for " + context);
        }

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.world), poseStack, bufferSource, state, packedLight, packedOverlay);

        if (hasCog) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.375F, 0.0F);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-rot), 0.0F, 0.0F, 1.0F)));
            poseStack.translate(0.0F, -1.375F, 0.0F);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.cog), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.375F, 0.25F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rot * 2.0F + 3.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0F, -1.375F, -0.25F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.cogSmall), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(rot * Math.PI / 90.0D) * 0.25D + 0.125D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(parts.piston), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    private record PartSet(Block block, ModelResourceLocation world, ModelResourceLocation cog, ModelResourceLocation cogSmall, ModelResourceLocation piston) {
        private static PartSet forStack(ItemStack stack) {
            if (!(stack.getItem() instanceof BlockItem blockItem)) {
                throw new IllegalArgumentException("Stirling renderer received a non-block item: " + stack.getItem());
            }
            Block block = blockItem.getBlock();
            if (block == HbmBlocks.MACHINE_STIRLING.get()) {
                return new PartSet(block, WORLD, COG, COG_SMALL, PISTON);
            }
            if (block == HbmBlocks.MACHINE_STIRLING_CREATIVE.get()) {
                return new PartSet(block, WORLD_CREATIVE, COG_CREATIVE, COG_SMALL_CREATIVE, PISTON_CREATIVE);
            }
            if (block == HbmBlocks.MACHINE_STIRLING_STEEL.get()) {
                return new PartSet(block, WORLD_STEEL, COG_STEEL, COG_SMALL_STEEL, PISTON_STEEL);
            }
            throw new IllegalArgumentException("Stirling renderer received an unsupported block: " + block);
        }
    }
}
