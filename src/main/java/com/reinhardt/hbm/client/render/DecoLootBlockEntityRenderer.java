package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.DecoLootBlockEntity;
import com.reinhardt.hbm.registry.HbmItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Direct port of the 1.7.10 RenderLoot special-item branches. */
public final class DecoLootBlockEntityRenderer implements BlockEntityRenderer<DecoLootBlockEntity> {
    private static final BlockState MODEL_STATE = Blocks.IRON_BLOCK.defaultBlockState();
    private static final RandomSource ITEM_RANDOM = RandomSource.create();
    private static final float LEGACY_ITEM_THICKNESS = 0.0625F;
    private static final Set<String> RENDER_WARNINGS = ConcurrentHashMap.newKeySet();

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
            try {
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
            } catch (RuntimeException exception) {
                // A loot pile must never take down the render thread because an external item
                // supplies a custom/broken model.  Keep rendering the remaining pile entries.
                String warningKey = stack.getItem().toString();
                if (RENDER_WARNINGS.add(warningKey)) {
                    ReinhardtsHBM.LOGGER.warn("Skipping loot pile item {} after a rendering failure", stack,
                            exception);
                }
            } finally {
                poseStack.popPose();
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(DecoLootBlockEntity loot) {
        BlockPos pos = loot.getBlockPos();
        return new AABB(
                pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 3.0D, pos.getZ() + 2.0D);
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
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();
        BakedModel resolvedModel = itemRenderer.getModel(stack, loot.getLevel(), null, 0);

        // Keep the hand-built 1.7.10 card geometry for normal 2D icons.
        // It is important that the fallback is rendered from a clean pose: applying the
        // legacy card rotation to a 3D/custom item produces invalid transforms in several
        // third-party renderers (TACZ is one example).
        boolean renderedLegacy = false;
        poseStack.pushPose();
        try {
            poseStack.translate(0.25F, 0.0F, 0.25F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            try {
                renderedLegacy = renderLegacyItemIn2D(loot, stack, resolvedModel, poseStack,
                        bufferSource, packedLight, packedOverlay);
            } catch (RuntimeException ignoredLegacyFailure) {
                // A malformed/custom icon is treated as a missing legacy layer.  The clean
                // ItemRenderer path below is still able to render most 3D and built-in items.
            }
        } finally {
            poseStack.popPose();
        }

        if (!renderedLegacy) {
            poseStack.pushPose();
            try {
                poseStack.translate(0.5F, 0.0625F, 0.5F);
                itemRenderer.renderStatic(stack,
                        resolvedModel.isGui3d() ? ItemDisplayContext.GROUND : ItemDisplayContext.FIXED,
                        packedLight, packedOverlay, poseStack, bufferSource, loot.getLevel(), 0);
            } finally {
                poseStack.popPose();
            }
        }
    }

    /**
     * Direct equivalent of the 1.7.10 RenderLoot call to ItemRenderer.renderItemIn2D.
     * Deliberately bypasses every modern ItemDisplayContext transform and its model-centering translation.
     */
    private static boolean renderLegacyItemIn2D(DecoLootBlockEntity loot, ItemStack stack, BakedModel resolvedModel,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        boolean renderedAnyLayer = false;

        for (BakedModel pass : resolvedModel.getRenderPasses(stack, true)) {
            for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                Set<LegacyItemLayer> layers = legacyItemLayers(pass, renderType);
                if (layers.isEmpty()) {
                    continue;
                }
                renderedAnyLayer = true;

                VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(
                        bufferSource, renderType, true, stack.hasFoil());
                for (LegacyItemLayer layer : layers) {
                    int color = layer.tintIndex() < 0
                            ? 0xFFFFFFFF
                            : minecraft.getItemColors().getColor(stack, layer.tintIndex());
                    renderLegacyIcon(poseStack.last(), consumer, layer.sprite(), color,
                            packedLight, packedOverlay);
                }
            }
        }
        return renderedAnyLayer;
    }

    private static Set<LegacyItemLayer> legacyItemLayers(BakedModel model, RenderType renderType) {
        Set<LegacyItemLayer> layers = new LinkedHashSet<>();
        collectLegacyItemLayers(model, null, renderType, layers);
        for (Direction direction : Direction.values()) {
            collectLegacyItemLayers(model, direction, renderType, layers);
        }
        return layers;
    }

    private static void collectLegacyItemLayers(BakedModel model, Direction side, RenderType renderType,
                                                Set<LegacyItemLayer> layers) {
        ITEM_RANDOM.setSeed(42L);
        for (BakedQuad quad : model.getQuads(null, side, ITEM_RANDOM, ModelData.EMPTY, renderType)) {
            if (quad.getDirection() == Direction.SOUTH) {
                layers.add(new LegacyItemLayer(quad.getSprite(), quad.getTintIndex()));
            }
        }
    }

    private static void renderLegacyIcon(PoseStack.Pose pose, VertexConsumer consumer, TextureAtlasSprite sprite,
                                         int color, int packedLight, int packedOverlay) {
        float maxU = sprite.getU1();
        float minV = sprite.getV0();
        float minU = sprite.getU0();
        float maxV = sprite.getV1();
        int width = sprite.contents().width();
        int height = sprite.contents().height();

        // Front face: exact 1.7.10 renderItemIn2D vertex order and UV order.
        legacyVertex(consumer, pose, 0.0F, 0.0F, 0.0F, maxU, maxV,
                0.0F, 0.0F, 1.0F, color, packedLight, packedOverlay);
        legacyVertex(consumer, pose, 1.0F, 0.0F, 0.0F, minU, maxV,
                0.0F, 0.0F, 1.0F, color, packedLight, packedOverlay);
        legacyVertex(consumer, pose, 1.0F, 1.0F, 0.0F, minU, minV,
                0.0F, 0.0F, 1.0F, color, packedLight, packedOverlay);
        legacyVertex(consumer, pose, 0.0F, 1.0F, 0.0F, maxU, minV,
                0.0F, 0.0F, 1.0F, color, packedLight, packedOverlay);

        // Back face.
        legacyVertex(consumer, pose, 0.0F, 1.0F, -LEGACY_ITEM_THICKNESS, maxU, minV,
                0.0F, 0.0F, -1.0F, color, packedLight, packedOverlay);
        legacyVertex(consumer, pose, 1.0F, 1.0F, -LEGACY_ITEM_THICKNESS, minU, minV,
                0.0F, 0.0F, -1.0F, color, packedLight, packedOverlay);
        legacyVertex(consumer, pose, 1.0F, 0.0F, -LEGACY_ITEM_THICKNESS, minU, maxV,
                0.0F, 0.0F, -1.0F, color, packedLight, packedOverlay);
        legacyVertex(consumer, pose, 0.0F, 0.0F, -LEGACY_ITEM_THICKNESS, maxU, maxV,
                0.0F, 0.0F, -1.0F, color, packedLight, packedOverlay);

        float halfPixelU = 0.5F * (maxU - minU) / width;
        float halfPixelV = 0.5F * (maxV - minV) / height;

        // Left-facing pixel strips.
        for (int pixel = 0; pixel < width; pixel++) {
            float x = (float) pixel / width;
            float u = maxU + (minU - maxU) * x - halfPixelU;
            legacyVertex(consumer, pose, x, 0.0F, -LEGACY_ITEM_THICKNESS, u, maxV,
                    -1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, x, 0.0F, 0.0F, u, maxV,
                    -1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, x, 1.0F, 0.0F, u, minV,
                    -1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, x, 1.0F, -LEGACY_ITEM_THICKNESS, u, minV,
                    -1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
        }

        // Right-facing pixel strips.
        for (int pixel = 0; pixel < width; pixel++) {
            float fraction = (float) pixel / width;
            float u = maxU + (minU - maxU) * fraction - halfPixelU;
            float x = fraction + 1.0F / width;
            legacyVertex(consumer, pose, x, 1.0F, -LEGACY_ITEM_THICKNESS, u, minV,
                    1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, x, 1.0F, 0.0F, u, minV,
                    1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, x, 0.0F, 0.0F, u, maxV,
                    1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, x, 0.0F, -LEGACY_ITEM_THICKNESS, u, maxV,
                    1.0F, 0.0F, 0.0F, color, packedLight, packedOverlay);
        }

        // Up-facing pixel strips.
        for (int pixel = 0; pixel < height; pixel++) {
            float fraction = (float) pixel / height;
            float v = maxV + (minV - maxV) * fraction - halfPixelV;
            float y = fraction + 1.0F / height;
            legacyVertex(consumer, pose, 0.0F, y, 0.0F, maxU, v,
                    0.0F, 1.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, 1.0F, y, 0.0F, minU, v,
                    0.0F, 1.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, 1.0F, y, -LEGACY_ITEM_THICKNESS, minU, v,
                    0.0F, 1.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, 0.0F, y, -LEGACY_ITEM_THICKNESS, maxU, v,
                    0.0F, 1.0F, 0.0F, color, packedLight, packedOverlay);
        }

        // Down-facing pixel strips.
        for (int pixel = 0; pixel < height; pixel++) {
            float y = (float) pixel / height;
            float v = maxV + (minV - maxV) * y - halfPixelV;
            legacyVertex(consumer, pose, 1.0F, y, 0.0F, minU, v,
                    0.0F, -1.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, 0.0F, y, 0.0F, maxU, v,
                    0.0F, -1.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, 0.0F, y, -LEGACY_ITEM_THICKNESS, maxU, v,
                    0.0F, -1.0F, 0.0F, color, packedLight, packedOverlay);
            legacyVertex(consumer, pose, 1.0F, y, -LEGACY_ITEM_THICKNESS, minU, v,
                    0.0F, -1.0F, 0.0F, color, packedLight, packedOverlay);
        }
    }

    private static void legacyVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                     float x, float y, float z, float u, float v,
                                     float normalX, float normalY, float normalZ,
                                     int color, int packedLight, int packedOverlay) {
        consumer.addVertex(pose, x, y, z)
                .setColor((color >>> 16) & 0xFF, (color >>> 8) & 0xFF, color & 0xFF, 0xFF)
                .setUv(u, v)
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, normalX, normalY, normalZ);
    }

    private record LegacyItemLayer(TextureAtlasSprite sprite, int tintIndex) {
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
