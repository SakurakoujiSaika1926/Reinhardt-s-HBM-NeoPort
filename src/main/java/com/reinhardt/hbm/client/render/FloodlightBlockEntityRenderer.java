package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.FloodlightBlock;
import com.reinhardt.hbm.blockentity.FloodlightBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class FloodlightBlockEntityRenderer implements LongRangeBlockEntityRenderer<FloodlightBlockEntity> {
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/floodlight_base");
    private static final ModelResourceLocation LIGHTS = MachineModelRenderer.standalone("block/floodlight_lights");
    private static final ModelResourceLocation LAMPS = MachineModelRenderer.standalone("block/floodlight_lamps");
    private static final int LAMP_GLOW = 0xB0FFF2C0;
    /** One immutable post-bake generation shared by both rendering and culling. */
    private static volatile GeometryGeneration geometryGeneration;

    public FloodlightBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        FloodlightBlockEntity.setClientRenderSnapshotUpdater(
                FloodlightBlockEntityRenderer::updateRenderSnapshot);
    }

    /** Called on the client thread after the new model bake has completed. */
    static void rebuildGeometryTemplatesAfterBake() {
        geometryGeneration = captureGeometryGeneration();
        FloodlightBlockEntity.setClientRenderSnapshotUpdater(
                FloodlightBlockEntityRenderer::updateRenderSnapshot);
        FloodlightBlockEntity.invalidateClientRenderSnapshotsAfterBake();
    }

    private static GeometryGeneration captureGeometryGeneration() {
        List<GeometryVariant> variants = new ArrayList<>(
                Collections.nCopies(Direction.values().length * 2, null));
        BlockState defaultState = HbmBlocks.FLOODLIGHT.get().defaultBlockState();
        BakedModel baseModel = MachineModelRenderer.model(BASE);
        BakedModel lightsModel = MachineModelRenderer.model(LIGHTS);
        BakedModel lampsModel = MachineModelRenderer.model(LAMPS);
        for (boolean flipped : new boolean[] {false, true}) {
            for (Direction facing : Direction.values()) {
                BlockState state = defaultState
                        .setValue(FloodlightBlock.FACING, facing)
                        .setValue(FloodlightBlock.FLIPPED, flipped);
                variants.set(legacyMeta(state), new GeometryVariant(
                        captureGeometry(baseModel, state),
                        captureGeometry(lightsModel, state),
                        captureGeometry(lampsModel, state)));
            }
        }
        return new GeometryGeneration(List.copyOf(variants));
    }

    /** Clears world-owned snapshots while retaining the still-current resource generation. */
    static void clearClientStateAfterLogout() {
        FloodlightBlockEntity.clearClientRenderSnapshotsAfterLogout();
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(LIGHTS);
        event.register(LAMPS);
    }

    @Override
    public void render(FloodlightBlockEntity light, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        FloodlightBlockEntity.RenderSnapshot snapshot = light.renderSnapshot();
        if (!(snapshot.geometryToken() instanceof GeometryGeneration generation)) {
            return;
        }
        poseStack.pushPose();
        int meta = snapshot.meta();
        GeometryVariant variant = generation.variants().get(meta);
        applyBasePose(poseStack, meta);
        variant.base().renderDefault(poseStack, bufferSource, packedLight, packedOverlay);
        applyHeadPose(poseStack, meta, snapshot.rotation());
        variant.lights().renderDefault(poseStack, bufferSource, packedLight, packedOverlay);
        if (snapshot.on()) {
            variant.lamps().renderDefault(poseStack, bufferSource,
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay);
            variant.lamps().renderTintedOverride(poseStack, bufferSource,
                    RenderType.eyes(TextureAtlas.LOCATION_BLOCKS),
                    net.minecraft.client.renderer.LightTexture.FULL_BRIGHT, packedOverlay, LAMP_GLOW);
        } else {
            variant.lamps().renderTintedTranslucent(
                    poseStack, bufferSource, packedLight, packedOverlay, 0xFF404040);
        }
        poseStack.popPose();
    }

    private static int legacyMeta(BlockState state) {
        return state.getValue(FloodlightBlock.FACING).ordinal()
                + (state.getValue(FloodlightBlock.FLIPPED) ? 6 : 0);
    }

    private static void applyBasePose(PoseStack poseStack, int meta) {
        // RenderFloodlight starts at (x + 0.5, y + 0.5, z + 0.5).  The
        // block-entity pose is block-local, so restore that origin before
        // applying the legacy metadata rotations and offsets.
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // RenderFloodlight used the complete legacy metadata (facing ordinal,
        // with flipped variants 6/7) rather than a generic direction transform.
        switch (meta) {
            case 0, 6 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(180.0F));
            case 1, 7 -> { }
            case 2 -> {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(180.0F));
            }
            case 3 -> poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
            case 4 -> {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(90.0F));
            }
            case 5 -> {
                poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90.0F));
                poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(270.0F));
            }
        }
        poseStack.translate(0.0F, -0.5F, 0.0F);
        if (meta != 0 && meta != 1) {
            poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void applyHeadPose(PoseStack poseStack, int meta, float currentRotation) {
        poseStack.translate(0.0F, 0.5F, 0.0F);
        float rotation = currentRotation;
        if (meta == 0 || meta == 6) rotation -= 90.0F;
        if (meta == 1 || meta == 7) rotation += 90.0F;
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(rotation));
        poseStack.translate(0.0F, -0.5F, 0.0F);
    }

    @Override
    public AABB getRenderBoundingBox(FloodlightBlockEntity light) {
        return light.renderSnapshot().bounds();
    }

    private static void updateRenderSnapshot(FloodlightBlockEntity light) {
        GeometryGeneration generation = geometryGeneration();
        BlockState state = light.getBlockState();
        int meta = legacyMeta(state);
        GeometryVariant variant = generation.variants().get(meta);
        RenderBounds.FloatBuilder bounds = new RenderBounds.FloatBuilder(light.getBlockPos());
        PoseStack poseStack = new PoseStack();
        applyBasePose(poseStack, meta);
        variant.base().includeTransformed(bounds, poseStack);
        applyHeadPose(poseStack, meta, light.rotation());
        variant.lights().includeTransformed(bounds, poseStack);
        variant.lamps().includeTransformed(bounds, poseStack);
        light.publishClientRenderSnapshot(
                new FloodlightBlockEntity.RenderSnapshot(meta, light.rotation(), light.isOn(), generation,
                        bounds.build(),
                        FloodlightBlockEntity.clientRenderSnapshotRevision()));
    }

    /** See CrashedBombBlockEntityRenderer.geometryGeneration(). */
    private static GeometryGeneration geometryGeneration() {
        GeometryGeneration generation = geometryGeneration;
        if (generation == null) {
            synchronized (FloodlightBlockEntityRenderer.class) {
                generation = geometryGeneration;
                if (generation == null) {
                    generation = captureGeometryGeneration();
                    geometryGeneration = generation;
                }
            }
        }
        return generation;
    }

    /** Freezes exactly the render types, quads and face-used floats consumed by renderUnculled. */
    private static CapturedGeometry captureGeometry(BakedModel model, BlockState state) {
        List<CapturedBatch> batches = new ArrayList<>();
        List<Float> coordinates = new ArrayList<>();
        RandomSource random = RandomSource.create(42L);
        random.setSeed(42L);
        for (RenderType renderType : model.getRenderTypes(state, random, ModelData.EMPTY)) {
            List<BakedQuad> quads = new ArrayList<>();
            collectQuads(quads, coordinates, model, state, null, renderType, random);
            for (Direction side : Direction.values()) {
                collectQuads(quads, coordinates, model, state, side, renderType, random);
            }
            if (!quads.isEmpty()) {
                batches.add(new CapturedBatch(
                        RenderTypeHelper.getEntityRenderType(renderType, false),
                        RenderTypeHelper.getEntityRenderType(renderType, true),
                        List.copyOf(quads)));
            }
        }
        float[] vertices = new float[coordinates.size()];
        for (int i = 0; i < coordinates.size(); i++) {
            vertices[i] = coordinates.get(i);
        }
        return new CapturedGeometry(List.copyOf(batches), vertices);
    }

    private static void collectQuads(List<BakedQuad> quads, List<Float> coordinates,
                                     BakedModel model, BlockState state, Direction side,
                                     RenderType renderType, RandomSource random) {
        random.setSeed(42L);
        for (BakedQuad quad : model.getQuads(state, side, random, ModelData.EMPTY, renderType)) {
            quads.add(quad);
            int[] data = quad.getVertices();
            int stride = data.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                coordinates.add(Float.intBitsToFloat(data[offset]));
                coordinates.add(Float.intBitsToFloat(data[offset + 1]));
                coordinates.add(Float.intBitsToFloat(data[offset + 2]));
            }
        }
    }

    private record GeometryGeneration(List<GeometryVariant> variants) {
    }

    private record GeometryVariant(CapturedGeometry base, CapturedGeometry lights,
                                   CapturedGeometry lamps) {
    }

    private record CapturedBatch(RenderType entityRenderType,
                                 RenderType translucentEntityRenderType,
                                 List<BakedQuad> quads) {
    }

    private static final class CapturedGeometry {
        private final List<CapturedBatch> batches;
        private final float[] vertices;

        private CapturedGeometry(List<CapturedBatch> batches, float[] vertices) {
            this.batches = batches;
            this.vertices = vertices.clone();
        }

        private void renderDefault(PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay) {
            for (CapturedBatch batch : batches) {
                MachineModelRenderer.renderCapturedQuads(batch.quads(), poseStack,
                        bufferSource.getBuffer(batch.entityRenderType()), packedLight, packedOverlay);
            }
        }

        private void renderTintedTranslucent(PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay, int argb) {
            for (CapturedBatch batch : batches) {
                MachineModelRenderer.emitCapturedQuadsTinted(batch.quads(), poseStack,
                        bufferSource.getBuffer(batch.translucentEntityRenderType()),
                        packedLight, packedOverlay, argb);
            }
        }

        private void renderTintedOverride(PoseStack poseStack, MultiBufferSource bufferSource,
                                          RenderType renderType, int packedLight,
                                          int packedOverlay, int argb) {
            var consumer = bufferSource.getBuffer(renderType);
            for (CapturedBatch batch : batches) {
                MachineModelRenderer.emitCapturedQuadsTinted(batch.quads(), poseStack,
                        consumer, packedLight, packedOverlay, argb);
            }
        }

        private void includeTransformed(RenderBounds.FloatBuilder bounds, PoseStack poseStack) {
            for (int i = 0; i < vertices.length; i += 3) {
                bounds.include(poseStack.last().pose().transformPosition(
                        new Vector3f(vertices[i], vertices[i + 1], vertices[i + 2])));
            }
        }
    }

}
