package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.blockentity.DecoLootBlockEntity;
import com.reinhardt.hbm.registry.HbmItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct port of the 1.7.10 RenderLoot special-item branches. */
public final class DecoLootBlockEntityRenderer implements BlockEntityRenderer<DecoLootBlockEntity> {
    private static final BlockState MODEL_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    private static final ModelResourceLocation MINI_NUKE = model("loot_mini_nuke");

    private static final ModelResourceLocation NCRPA_HELMET = model("loot_ncrpa_helmet");
    private static final ModelResourceLocation NCRPA_EYES = model("loot_ncrpa_eyes");
    private static final ModelResourceLocation NCRPA_CHEST = model("loot_ncrpa_chest");
    private static final ModelResourceLocation NCRPA_LEFT_ARM = model("loot_ncrpa_left_arm");
    private static final ModelResourceLocation NCRPA_RIGHT_ARM = model("loot_ncrpa_right_arm");
    private static final ModelResourceLocation NCRPA_LEFT_LEG = model("loot_ncrpa_left_leg");
    private static final ModelResourceLocation NCRPA_RIGHT_LEG = model("loot_ncrpa_right_leg");
    private static final ModelResourceLocation NCRPA_LEFT_BOOT = model("loot_ncrpa_left_boot");
    private static final ModelResourceLocation NCRPA_RIGHT_BOOT = model("loot_ncrpa_right_boot");

    private static final ModelResourceLocation TRENCHMASTER_HELMET = model("loot_trenchmaster_helmet");
    private static final ModelResourceLocation TRENCHMASTER_LIGHT = model("loot_trenchmaster_light");
    private static final ModelResourceLocation TRENCHMASTER_CHEST = model("loot_trenchmaster_chest");
    private static final ModelResourceLocation TRENCHMASTER_LEFT_ARM = model("loot_trenchmaster_left_arm");
    private static final ModelResourceLocation TRENCHMASTER_RIGHT_ARM = model("loot_trenchmaster_right_arm");
    private static final ModelResourceLocation TRENCHMASTER_LEFT_LEG = model("loot_trenchmaster_left_leg");
    private static final ModelResourceLocation TRENCHMASTER_RIGHT_LEG = model("loot_trenchmaster_right_leg");
    private static final ModelResourceLocation TRENCHMASTER_LEFT_BOOT = model("loot_trenchmaster_left_boot");
    private static final ModelResourceLocation TRENCHMASTER_RIGHT_BOOT = model("loot_trenchmaster_right_boot");

    public DecoLootBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MINI_NUKE);
        event.register(NCRPA_HELMET);
        event.register(NCRPA_EYES);
        event.register(NCRPA_CHEST);
        event.register(NCRPA_LEFT_ARM);
        event.register(NCRPA_RIGHT_ARM);
        event.register(NCRPA_LEFT_LEG);
        event.register(NCRPA_RIGHT_LEG);
        event.register(NCRPA_LEFT_BOOT);
        event.register(NCRPA_RIGHT_BOOT);
        event.register(TRENCHMASTER_HELMET);
        event.register(TRENCHMASTER_LIGHT);
        event.register(TRENCHMASTER_CHEST);
        event.register(TRENCHMASTER_LEFT_ARM);
        event.register(TRENCHMASTER_RIGHT_ARM);
        event.register(TRENCHMASTER_LEFT_LEG);
        event.register(TRENCHMASTER_RIGHT_LEG);
        event.register(TRENCHMASTER_LEFT_BOOT);
        event.register(TRENCHMASTER_RIGHT_BOOT);
    }

    @Override
    public void render(DecoLootBlockEntity loot, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        for (DecoLootBlockEntity.LootEntry entry : loot.items()) {
            ItemStack stack = entry.stack();
            poseStack.pushPose();
            poseStack.translate(entry.x(), entry.y(), entry.z());
            if (isMiniNuke(stack)) {
                renderMiniNuke(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.NCRPA_HELMET.get())) {
                renderNcrpaHelmet(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.NCRPA_PLATE.get())) {
                renderNcrpaPlate(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.NCRPA_LEGS.get())) {
                renderNcrpaLegs(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.NCRPA_BOOTS.get())) {
                renderNcrpaBoots(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.TRENCHMASTER_HELMET.get())) {
                renderTrenchmasterHelmet(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.TRENCHMASTER_PLATE.get())) {
                renderTrenchmasterPlate(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.TRENCHMASTER_LEGS.get())) {
                renderTrenchmasterLegs(poseStack, bufferSource, packedLight, packedOverlay);
            } else if (stack.is(HbmItems.TRENCHMASTER_BOOTS.get())) {
                renderTrenchmasterBoots(poseStack, bufferSource, packedLight, packedOverlay);
            } else {
                renderStandardItem(loot, stack, poseStack, bufferSource, packedLight, packedOverlay);
            }
            poseStack.popPose();
        }
    }

    private static boolean isMiniNuke(ItemStack stack) {
        if (!(stack.getItem() instanceof com.reinhardt.hbm.item.StandardAmmoItem)) {
            return false;
        }
        var type = com.reinhardt.hbm.item.StandardAmmoItem.standardType(stack);
        return type.ordinal() >= com.reinhardt.hbm.item.StandardAmmoItem.StandardAmmoType.NUKE_STANDARD.ordinal()
                && type.ordinal() <= com.reinhardt.hbm.item.StandardAmmoItem.StandardAmmoType.NUKE_HIVE.ordinal();
    }

    private static void renderStandardItem(DecoLootBlockEntity loot, ItemStack stack, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.translate(0.25F, 0.0F, 0.25F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
        Minecraft.getInstance().getItemRenderer().renderStatic(stack, ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, bufferSource, loot.getLevel(), 0);
    }

    private static void renderMiniNuke(PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(1.0F, 0.5F, 1.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MINI_NUKE), poseStack,
                bufferSource, MODEL_STATE, packedLight, packedOverlay);
    }

    private static void armorPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    }

    private static void renderNcrpaHelmet(PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(NCRPA_HELMET, poseStack, bufferSource, packedLight, packedOverlay);
        renderFullBright(NCRPA_EYES, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNcrpaPlate(PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(NCRPA_CHEST, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0F));
        render(NCRPA_LEFT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        render(NCRPA_RIGHT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderNcrpaLegs(PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(NCRPA_LEFT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(NCRPA_RIGHT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderNcrpaBoots(PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(NCRPA_LEFT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(NCRPA_RIGHT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderTrenchmasterHelmet(PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(TRENCHMASTER_HELMET, poseStack, bufferSource, packedLight, packedOverlay);
        renderFullBright(TRENCHMASTER_LIGHT, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    private static void renderTrenchmasterPlate(PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(TRENCHMASTER_CHEST, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0F));
        render(TRENCHMASTER_LEFT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        render(TRENCHMASTER_RIGHT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderTrenchmasterLegs(PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(TRENCHMASTER_LEFT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(TRENCHMASTER_RIGHT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderTrenchmasterBoots(PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(TRENCHMASTER_LEFT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(TRENCHMASTER_RIGHT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void render(ModelResourceLocation location, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack,
                bufferSource, MODEL_STATE, packedLight, packedOverlay);
    }

    private static void renderFullBright(ModelResourceLocation location, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack,
                bufferSource, MODEL_STATE, LightTexture.FULL_BRIGHT, packedOverlay);
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }
}
