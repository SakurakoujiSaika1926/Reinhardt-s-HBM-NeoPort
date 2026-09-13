package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/**
 * Direct port of {@code RenderBarrel#renderInventoryBlock} for the three
 * craftable fluid barrels. These were rendered by the legacy block handler,
 * never by an OBJ auto-fit profile.
 */
public final class FluidBarrelItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation PLASTIC = model("block/barrel_plastic");
    private static final ModelResourceLocation STEEL = model("block/barrel_steel");
    private static final ModelResourceLocation TCALLOY = model("block/barrel_tcalloy");

    public FluidBarrelItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PLASTIC);
        event.register(STEEL);
        event.register(TCALLOY);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        BakedModel model = MachineModelRenderer.model(modelFor(stack));

        poseStack.pushPose();
        // The legacy RenderBlocks item path centers the block, then applies
        // its fixed display transform. RenderBarrel itself adds no scale and
        // its Y=-0.5 placement is exactly that centered barrel origin.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_LEFT_HAND, THIRD_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_LEFT_HAND, FIRST_PERSON_RIGHT_HAND -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> poseStack.scale(0.5F, 0.5F, 0.5F);
            default -> {
            }
        }
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static ModelResourceLocation modelFor(ItemStack stack) {
        return switch (BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath()) {
            case "barrel_plastic" -> PLASTIC;
            case "barrel_steel" -> STEEL;
            case "barrel_tcalloy" -> TCALLOY;
            default -> throw new IllegalArgumentException("Fluid barrel renderer received an unsupported item: " + stack.getItem());
        };
    }

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id(path));
    }
}
