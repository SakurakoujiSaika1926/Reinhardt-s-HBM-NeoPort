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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.StainedGlassPaneBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Direct port of the 1.7.10 RenderLoot special-item branches. */
public final class DecoLootBlockEntityRenderer implements LongRangeBlockEntityRenderer<DecoLootBlockEntity> {
    private static final BlockState MODEL_STATE = Blocks.IRON_BLOCK.defaultBlockState();
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
    private static final List<ModelResourceLocation> SPECIAL_MODELS = List.of(
            MINI_NUKE,
            NCRPA_HELMET, NCRPA_EYES, NCRPA_CHEST, NCRPA_LEFT_ARM, NCRPA_RIGHT_ARM,
            NCRPA_LEFT_LEG, NCRPA_RIGHT_LEG, NCRPA_LEFT_BOOT, NCRPA_RIGHT_BOOT,
            TRENCHMASTER_HELMET, TRENCHMASTER_LIGHT, TRENCHMASTER_CHEST,
            TRENCHMASTER_LEFT_ARM, TRENCHMASTER_RIGHT_ARM, TRENCHMASTER_LEFT_LEG,
            TRENCHMASTER_RIGHT_LEG, TRENCHMASTER_LEFT_BOOT, TRENCHMASTER_RIGHT_BOOT);
    private static final ClientRenderSnapshotInstances<DecoLootBlockEntity> CLIENT_INSTANCES =
            new ClientRenderSnapshotInstances<>();
    private static volatile BakedModelGeneration geometryGeneration;

    public DecoLootBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        DecoLootBlockEntity.setClientRenderSnapshotUpdater(DecoLootBlockEntityRenderer::updateRenderSnapshot);
    }

    /** Invalidates loaded loot bounds after a resource-pack model rebake. */
    static void rebuildGeometryGenerationAfterBake() {
        geometryGeneration = BakedModelGeneration.capture(SPECIAL_MODELS);
        DecoLootBlockEntity.setClientRenderSnapshotUpdater(DecoLootBlockEntityRenderer::updateRenderSnapshot);
        CLIENT_INSTANCES.refreshAll(DecoLootBlockEntityRenderer::updateRenderSnapshot);
    }

    static void clearGeometryGenerationAfterLogout() {
        geometryGeneration = null;
        CLIENT_INSTANCES.clear();
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
        DecoLootBlockEntity.RenderSnapshot snapshot = loot.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof BakedModelGeneration generation)) {
            return;
        }
        for (DecoLootBlockEntity.RenderEntry entry : snapshot.entries()) {
            ItemStack stack = entry.stack();
            poseStack.pushPose();
            try {
                poseStack.translate(entry.x(), entry.y(), entry.z());
                if (isMiniNuke(stack)) {
                    renderMiniNuke(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.NCRPA_HELMET.get())) {
                    renderNcrpaHelmet(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.NCRPA_PLATE.get())) {
                    renderNcrpaPlate(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.NCRPA_LEGS.get())) {
                    renderNcrpaLegs(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.NCRPA_BOOTS.get())) {
                    renderNcrpaBoots(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.TRENCHMASTER_HELMET.get())) {
                    renderTrenchmasterHelmet(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.TRENCHMASTER_PLATE.get())) {
                    renderTrenchmasterPlate(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.TRENCHMASTER_LEGS.get())) {
                    renderTrenchmasterLegs(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (stack.is(HbmItems.TRENCHMASTER_BOOTS.get())) {
                    renderTrenchmasterBoots(generation, poseStack, bufferSource, packedLight, packedOverlay);
                } else if (entry.modelToken() instanceof BakedModel capturedModel) {
                    renderStandardItem(stack, capturedModel, entry.legacyGeometryToken(), entry.legacyLayer(), poseStack,
                            bufferSource, packedLight, packedOverlay);
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
        return loot.renderSnapshot().bounds();
    }

    /** Runs only from the client level thread and publishes one immutable render/bounds view. */
    private static void updateRenderSnapshot(DecoLootBlockEntity loot) {
        CLIENT_INSTANCES.track(loot);
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(loot::refreshClientRenderSnapshot);
            return;
        }

        BakedModelGeneration generation = geometryGeneration();
        java.util.List<DecoLootBlockEntity.LootEntry> sourceEntries = loot.copyEntriesForRenderSnapshot();
        java.util.List<DecoLootBlockEntity.RenderEntry> renderEntries = new java.util.ArrayList<>();
        Bounds bounds = new Bounds(loot.getBlockPos());
        for (DecoLootBlockEntity.LootEntry entry : sourceEntries) {
            ItemStack stack = entry.stack();
            Bounds entryBounds = new Bounds(loot.getBlockPos());
            PoseStack poseStack = new PoseStack();
            poseStack.translate(entry.x(), entry.y(), entry.z());
            BakedModel capturedModel = null;
            LegacyItemGeometry legacyGeometry = LegacyItemGeometry.EMPTY;
            boolean legacyLayer = false;
            try {
                if (isMiniNuke(stack)) {
                    poseStack.scale(0.5F, 0.5F, 0.5F);
                    poseStack.translate(1.0F, 0.5F, 1.0F);
                    includeBlockModel(entryBounds, generation.model(MINI_NUKE), MODEL_STATE, poseStack);
                } else if (stack.is(HbmItems.NCRPA_HELMET.get())) {
                    armorPose(poseStack);
                    includeBlockModel(entryBounds, generation.model(NCRPA_HELMET), MODEL_STATE, poseStack);
                    includeBlockModel(entryBounds, generation.model(NCRPA_EYES), MODEL_STATE, poseStack);
                } else if (stack.is(HbmItems.NCRPA_PLATE.get())) {
                    includeArmorPlate(generation, entryBounds, poseStack, NCRPA_CHEST, NCRPA_LEFT_ARM, NCRPA_RIGHT_ARM);
                } else if (stack.is(HbmItems.NCRPA_LEGS.get())) {
                    includeArmorPair(generation, entryBounds, poseStack, NCRPA_LEFT_LEG, NCRPA_RIGHT_LEG);
                } else if (stack.is(HbmItems.NCRPA_BOOTS.get())) {
                    includeArmorPair(generation, entryBounds, poseStack, NCRPA_LEFT_BOOT, NCRPA_RIGHT_BOOT);
                } else if (stack.is(HbmItems.TRENCHMASTER_HELMET.get())) {
                    armorPose(poseStack);
                    includeBlockModel(entryBounds, generation.model(TRENCHMASTER_HELMET), MODEL_STATE, poseStack);
                    includeBlockModel(entryBounds, generation.model(TRENCHMASTER_LIGHT), MODEL_STATE, poseStack);
                } else if (stack.is(HbmItems.TRENCHMASTER_PLATE.get())) {
                    includeArmorPlate(generation, entryBounds, poseStack, TRENCHMASTER_CHEST,
                            TRENCHMASTER_LEFT_ARM, TRENCHMASTER_RIGHT_ARM);
                } else if (stack.is(HbmItems.TRENCHMASTER_LEGS.get())) {
                    includeArmorPair(generation, entryBounds, poseStack, TRENCHMASTER_LEFT_LEG, TRENCHMASTER_RIGHT_LEG);
                } else if (stack.is(HbmItems.TRENCHMASTER_BOOTS.get())) {
                    includeArmorPair(generation, entryBounds, poseStack, TRENCHMASTER_LEFT_BOOT, TRENCHMASTER_RIGHT_BOOT);
                } else {
                    capturedModel = minecraft.getItemRenderer().getModel(stack, loot.getLevel(), null, 0);
                    if (capturedModel.isCustomRenderer()) {
                        // A BEWLR has no inspectable geometry contract.  Do not publish an
                        // entry that would render geometry absent from its immutable bounds.
                        continue;
                    }
                    legacyGeometry = captureLegacyItemGeometry(capturedModel, stack);
                    legacyLayer = !legacyGeometry.batches().isEmpty();
                    includeStandardItem(entryBounds, stack, poseStack, capturedModel, legacyLayer);
                }
            } catch (RuntimeException ignoredMalformedModel) {
                // Rendering skips the same malformed entry, so it contributes no geometry.
                continue;
            }
            bounds.include(entryBounds);
            renderEntries.add(new DecoLootBlockEntity.RenderEntry(
                    stack, entry.x(), entry.y(), entry.z(), capturedModel, legacyGeometry, legacyLayer));
        }
        loot.publishClientRenderSnapshot(new DecoLootBlockEntity.RenderSnapshot(renderEntries, generation, bounds.toAabb(),
                DecoLootBlockEntity.clientRenderSnapshotRevision()));
    }

    private static void includeArmorPlate(BakedModelGeneration generation, Bounds bounds, PoseStack poseStack,
                                          ModelResourceLocation chest, ModelResourceLocation leftArm,
                                          ModelResourceLocation rightArm) {
        armorPose(poseStack);
        includeBlockModel(bounds, generation.model(chest), MODEL_STATE, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0F));
        includeBlockModel(bounds, generation.model(leftArm), MODEL_STATE, poseStack);
        includeBlockModel(bounds, generation.model(rightArm), MODEL_STATE, poseStack);
        poseStack.popPose();
    }

    private static void includeArmorPair(BakedModelGeneration generation, Bounds bounds, PoseStack poseStack,
                                         ModelResourceLocation left, ModelResourceLocation right) {
        armorPose(poseStack);
        includeBlockModel(bounds, generation.model(left), MODEL_STATE, poseStack);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        includeBlockModel(bounds, generation.model(right), MODEL_STATE, poseStack);
        poseStack.popPose();
    }

    private static void includeStandardItem(Bounds bounds, ItemStack stack, PoseStack poseStack,
                                            BakedModel resolvedModel, boolean legacyLayer) {
        if (legacyLayer) {
            poseStack.translate(0.25F, 0.0F, 0.25F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            includeLegacyCard(bounds, poseStack);
            return;
        }

        poseStack.translate(0.5F, 0.0625F, 0.5F);
        ItemDisplayContext context = resolvedModel.isGui3d()
                ? ItemDisplayContext.GROUND : ItemDisplayContext.FIXED;
        BakedModel transformedModel = resolvedModel.applyTransform(context, poseStack, false);
        poseStack.translate(-0.5F, -0.5F, -0.5F);

        // Custom BEWLR models were excluded before this point.  These quads
        // belong to the exact captured model token consumed by render().
        boolean solidRender = !(stack.getItem() instanceof BlockItem blockItem)
                || (!(blockItem.getBlock() instanceof HalfTransparentBlock)
                && !(blockItem.getBlock() instanceof StainedGlassPaneBlock));
        includeItemModel(bounds, transformedModel, stack, solidRender, poseStack);
    }

    private static LegacyItemGeometry captureLegacyItemGeometry(BakedModel model, ItemStack stack) {
        java.util.ArrayList<LegacyItemBatch> batches = new java.util.ArrayList<>();
        for (BakedModel pass : model.getRenderPasses(stack, true)) {
            for (RenderType renderType : pass.getRenderTypes(stack, true)) {
                Set<LegacyItemLayer> layers = legacyItemLayers(pass, renderType);
                if (!layers.isEmpty()) {
                    batches.add(new LegacyItemBatch(renderType, List.copyOf(layers)));
                }
            }
        }
        return batches.isEmpty() ? LegacyItemGeometry.EMPTY : new LegacyItemGeometry(List.copyOf(batches));
    }

    private static void includeLegacyCard(Bounds bounds, PoseStack poseStack) {
        for (float x : new float[]{0.0F, 1.0F}) {
            for (float y : new float[]{0.0F, 1.0F}) {
                bounds.include(poseStack.last().pose().transformPosition(new Vector3f(x, y, 0.0F)));
                bounds.include(poseStack.last().pose().transformPosition(
                        new Vector3f(x, y, -LEGACY_ITEM_THICKNESS)));
            }
        }
    }

    private static void includeBlockModel(Bounds bounds, BakedModel model, BlockState state,
                                          PoseStack poseStack) {
        RandomSource random = RandomSource.create(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            includeBlockQuads(bounds, model, state, null, renderType, poseStack, random);
            for (Direction side : Direction.values()) {
                includeBlockQuads(bounds, model, state, side, renderType, poseStack, random);
            }
        }
    }

    private static void includeBlockQuads(Bounds bounds, BakedModel model, BlockState state, Direction side,
                                          RenderType renderType, PoseStack poseStack, RandomSource random) {
        random.setSeed(42L);
        includeQuads(bounds, model.getQuads(state, side, random, ModelData.EMPTY, renderType), poseStack);
    }

    private static void includeItemModel(Bounds bounds, BakedModel model, ItemStack stack,
                                         boolean solidRender, PoseStack poseStack) {
        RandomSource random = RandomSource.create(42L);
        for (BakedModel pass : model.getRenderPasses(stack, solidRender)) {
            for (Direction side : Direction.values()) {
                random.setSeed(42L);
                includeQuads(bounds, pass.getQuads(null, side, random), poseStack);
            }
            random.setSeed(42L);
            includeQuads(bounds, pass.getQuads(null, null, random), poseStack);
        }
    }

    private static void includeQuads(Bounds bounds, Iterable<BakedQuad> quads, PoseStack poseStack) {
        for (BakedQuad quad : quads) {
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                bounds.include(poseStack.last().pose().transformPosition(new Vector3f(
                        Float.intBitsToFloat(data[offset]),
                        Float.intBitsToFloat(data[offset + 1]),
                        Float.intBitsToFloat(data[offset + 2]))));
            }
        }
    }

    private static final class Bounds {
        private final BlockPos pos;
        private double minX = Double.POSITIVE_INFINITY;
        private double minY = Double.POSITIVE_INFINITY;
        private double minZ = Double.POSITIVE_INFINITY;
        private double maxX = Double.NEGATIVE_INFINITY;
        private double maxY = Double.NEGATIVE_INFINITY;
        private double maxZ = Double.NEGATIVE_INFINITY;

        private Bounds(BlockPos pos) {
            this.pos = pos;
        }

        private void include(Vector3f point) {
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }

        private void include(Bounds other) {
            if (!Double.isFinite(other.minX)) {
                return;
            }
            minX = Math.min(minX, other.minX);
            minY = Math.min(minY, other.minY);
            minZ = Math.min(minZ, other.minZ);
            maxX = Math.max(maxX, other.maxX);
            maxY = Math.max(maxY, other.maxY);
            maxZ = Math.max(maxZ, other.maxZ);
        }

        private AABB toAabb() {
            if (!Double.isFinite(minX)) {
                double x = pos.getX() + 0.5D;
                double y = pos.getY() + 0.5D;
                double z = pos.getZ() + 0.5D;
                return new AABB(x, y, z, x, y, z);
            }
            return new AABB(pos.getX() + minX, pos.getY() + minY, pos.getZ() + minZ,
                    pos.getX() + maxX, pos.getY() + maxY, pos.getZ() + maxZ);
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

    private static void renderStandardItem(ItemStack stack, BakedModel resolvedModel,
                                           Object legacyGeometryToken, boolean legacyLayer,
                                           PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        ItemRenderer itemRenderer = minecraft.getItemRenderer();

        if (legacyLayer) {
            if (!(legacyGeometryToken instanceof LegacyItemGeometry legacyGeometry)) {
                return;
            }
            poseStack.pushPose();
            poseStack.translate(0.25F, 0.0F, 0.25F);
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.XP.rotationDegrees(90.0F));
            try {
                renderLegacyItemIn2D(stack, legacyGeometry, poseStack,
                        bufferSource, packedLight, packedOverlay);
            } finally {
                poseStack.popPose();
            }
            return;
        }

        poseStack.pushPose();
        try {
            poseStack.translate(0.5F, 0.0625F, 0.5F);
            itemRenderer.render(stack,
                    resolvedModel.isGui3d() ? ItemDisplayContext.GROUND : ItemDisplayContext.FIXED,
                    false, poseStack, bufferSource, packedLight, packedOverlay, resolvedModel);
        } finally {
            poseStack.popPose();
        }
    }

    /**
     * Direct equivalent of the 1.7.10 RenderLoot call to ItemRenderer.renderItemIn2D.
     * Deliberately bypasses every modern ItemDisplayContext transform and its model-centering translation.
     */
    private static void renderLegacyItemIn2D(ItemStack stack, LegacyItemGeometry geometry,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();

        for (LegacyItemBatch batch : geometry.batches()) {
            VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(
                    bufferSource, batch.renderType(), true, stack.hasFoil());
            for (LegacyItemLayer layer : batch.layers()) {
                int color = layer.tintIndex() < 0
                        ? 0xFFFFFFFF
                        : minecraft.getItemColors().getColor(stack, layer.tintIndex());
                renderLegacyIcon(poseStack.last(), consumer, layer.sprite(), color,
                        packedLight, packedOverlay);
            }
        }
    }

    private static Set<LegacyItemLayer> legacyItemLayers(BakedModel model, RenderType renderType) {
        Set<LegacyItemLayer> layers = new LinkedHashSet<>();
        RandomSource random = RandomSource.create(42L);
        collectLegacyItemLayers(model, null, renderType, layers, random);
        for (Direction direction : Direction.values()) {
            collectLegacyItemLayers(model, direction, renderType, layers, random);
        }
        return layers;
    }

    private static void collectLegacyItemLayers(BakedModel model, Direction side, RenderType renderType,
                                                Set<LegacyItemLayer> layers, RandomSource random) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(null, side, random, ModelData.EMPTY, renderType)) {
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

    private record LegacyItemBatch(RenderType renderType, List<LegacyItemLayer> layers) {
        private LegacyItemBatch {
            layers = List.copyOf(layers);
        }
    }

    private record LegacyItemGeometry(List<LegacyItemBatch> batches) {
        private static final LegacyItemGeometry EMPTY = new LegacyItemGeometry(List.of());

        private LegacyItemGeometry {
            batches = List.copyOf(batches);
        }
    }

    private static void renderMiniNuke(BakedModelGeneration generation, PoseStack poseStack,
                                       MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(1.0F, 0.5F, 1.0F);
        MachineModelRenderer.renderUnculled(generation.model(MINI_NUKE), poseStack,
                bufferSource, MODEL_STATE, packedLight, packedOverlay);
    }

    private static void armorPose(PoseStack poseStack) {
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.scale(0.0625F, 0.0625F, 0.0625F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
    }

    private static void renderNcrpaHelmet(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, NCRPA_HELMET, poseStack, bufferSource, packedLight, packedOverlay);
        renderFullBright(generation, NCRPA_EYES, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNcrpaPlate(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, NCRPA_CHEST, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0F));
        render(generation, NCRPA_LEFT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        render(generation, NCRPA_RIGHT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderNcrpaLegs(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, NCRPA_LEFT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(generation, NCRPA_RIGHT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderNcrpaBoots(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, NCRPA_LEFT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(generation, NCRPA_RIGHT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderTrenchmasterHelmet(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, TRENCHMASTER_HELMET, poseStack, bufferSource, packedLight, packedOverlay);
        renderFullBright(generation, TRENCHMASTER_LIGHT, poseStack, bufferSource, packedOverlay);
        poseStack.popPose();
    }

    private static void renderTrenchmasterPlate(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, TRENCHMASTER_CHEST, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-3.0F));
        render(generation, TRENCHMASTER_LEFT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        render(generation, TRENCHMASTER_RIGHT_ARM, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderTrenchmasterLegs(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, TRENCHMASTER_LEFT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(generation, TRENCHMASTER_RIGHT_LEG, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderTrenchmasterBoots(BakedModelGeneration generation, PoseStack poseStack, MultiBufferSource bufferSource,
                                                int packedLight, int packedOverlay) {
        poseStack.pushPose();
        armorPose(poseStack);
        render(generation, TRENCHMASTER_LEFT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(-0.1F));
        render(generation, TRENCHMASTER_RIGHT_BOOT, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void render(BakedModelGeneration generation, ModelResourceLocation location, PoseStack poseStack,
                               MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(generation.model(location), poseStack,
                bufferSource, MODEL_STATE, packedLight, packedOverlay);
    }

    private static void renderFullBright(BakedModelGeneration generation, ModelResourceLocation location, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedOverlay) {
        MachineModelRenderer.renderUnculled(generation.model(location), poseStack,
                bufferSource, MODEL_STATE, LightTexture.FULL_BRIGHT, packedOverlay);
    }

    private static BakedModelGeneration geometryGeneration() {
        BakedModelGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (DecoLootBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = BakedModelGeneration.capture(SPECIAL_MODELS);
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }
}
