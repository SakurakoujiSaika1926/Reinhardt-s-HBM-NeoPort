package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
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

/** Exact 1.7.10 RenderBarrel inventory path for the six filled barrel items. */
public final class BarrelItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation RED = model("barrel_item/red_barrel");
    private static final ModelResourceLocation PINK = model("barrel_item/pink_barrel");
    private static final ModelResourceLocation LOX = model("barrel_item/lox_barrel");
    private static final ModelResourceLocation TAINT = model("barrel_item/taint_barrel");
    private static final ModelResourceLocation YELLOW = model("barrel_item/yellow_barrel");
    private static final ModelResourceLocation VITRIFIED = model("barrel_item/vitrified_barrel");

    public BarrelItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(RED);
        event.register(PINK);
        event.register(LOX);
        event.register(TAINT);
        event.register(YELLOW);
        event.register(VITRIFIED);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModelResourceLocation location = modelFor(stack);
        if (location == null) {
            return;
        }

        BlockState state = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
        BakedModel model = MachineModelRenderer.model(location);

        poseStack.pushPose();
        // ItemRenderer applies its block-space centering before entering a
        // BEWLR. Undo it so the legacy view transform keeps its original
        // order, then restore the centering immediately before drawing.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        applyLegacyBlockItemTransform(context, poseStack);
        poseStack.translate(-0.5F, -0.5F, -0.5F);
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static ModelResourceLocation modelFor(ItemStack stack) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return switch (id) {
            case "red_barrel" -> RED;
            case "pink_barrel" -> PINK;
            case "lox_barrel" -> LOX;
            case "taint_barrel" -> TAINT;
            case "yellow_barrel" -> YELLOW;
            case "vitrified_barrel" -> VITRIFIED;
            default -> null;
        };
    }

    private static void applyLegacyBlockItemTransform(ItemDisplayContext context, PoseStack poseStack) {
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
    }

    private static ModelResourceLocation model(String path) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id(path));
    }
}
