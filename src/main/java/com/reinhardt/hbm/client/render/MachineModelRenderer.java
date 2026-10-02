package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MachineModelRenderer {
    private static final RandomSource RANDOM = RandomSource.create();
    private static final Direction[] DIRECTIONS = Direction.values();
    private static final Map<ModelResourceLocation, BakedModel> MODEL_CACHE = new ConcurrentHashMap<>();
    private static final ThreadLocal<RenderScratch> RENDER_SCRATCH = ThreadLocal.withInitial(RenderScratch::new);

    private MachineModelRenderer() {
    }

    static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id(path));
    }

    static BakedModel model(ModelResourceLocation location) {
        return MODEL_CACHE.computeIfAbsent(
                location,
                key -> Minecraft.getInstance().getModelManager().getModel(key)
        );
    }

    static void invalidateModelCache() {
        MODEL_CACHE.clear();
    }

    /** Combines the level view transform with a block entity's local pose for direct VBO draws. */
    static Matrix4f composeModelView(PoseStack poseStack) {
        return RENDER_SCRATCH.get().modelView
                .set(RenderSystem.getModelViewMatrix())
                .mul(poseStack.last().pose());
    }

    static Quaternionf xQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F));
    }

    static Quaternionf zQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F));
    }

    static void render(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(renderType, false);
            VertexConsumer consumer = bufferSource.getBuffer(entityRenderType);
            Minecraft.getInstance().getBlockRenderer().getModelRenderer().renderModel(
                    poseStack.last(),
                    consumer,
                    state,
                    model,
                    1.0F,
                    1.0F,
                    1.0F,
                    packedLight,
                    packedOverlay,
                    ModelData.EMPTY,
                    renderType
            );
        }
    }

    static void renderFullBright(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        render(model, poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay);
    }

    static void renderUnculled(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(renderType, false);
            VertexConsumer consumer = bufferSource.getBuffer(entityRenderType);
            renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType);
        }
    }

    /** Emits one source layer into a caller-owned static buffer. */
    static void emitUnculledLayer(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                                  BlockState state, int packedLight, int packedOverlay,
                                  RenderType sourceRenderType) {
        renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, sourceRenderType);
    }

    /** Emits one source layer through the legacy standalone-texture UV path into a static buffer. */
    static void emitUnculledStandaloneTextureLayer(BakedModel model, PoseStack poseStack,
                                                    VertexConsumer consumer, BlockState state,
                                                    int packedLight, int packedOverlay,
                                                    RenderType sourceRenderType) {
        renderQuadsStandaloneTexture(model, poseStack, consumer, state, packedLight, packedOverlay,
                sourceRenderType, 0.0F, 0.0F);
    }

    /** Renders an opaque model with the old GL texture-matrix UV offset. */
    static void renderUnculledUv(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                 int packedLight, int packedOverlay, float uOffset, float vOffset) {
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(renderType, false);
            VertexConsumer consumer = bufferSource.getBuffer(entityRenderType);
            renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType,
                    1.0F, 1.0F, 1.0F, 1.0F, uOffset, vOffset, 1.0F, 1.0F);
        }
    }

    /** Repeats a standalone texture with the same coordinate transform as the legacy texture matrix. */
    static void renderUnculledUv(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                 int packedLight, int packedOverlay, ResourceLocation texture,
                                 float uOffset, float vOffset) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(texture));
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            renderQuadsStandaloneTexture(model, poseStack, consumer, state, packedLight, packedOverlay,
                    renderType, uOffset, vOffset);
        }
    }

    /** Renders a full-bright standalone texture with the old additive glow pass semantics. */
    static void renderUnculledUvEyes(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state,
                                     int packedOverlay, ResourceLocation texture, float red, float green, float blue, float alpha) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.eyes(texture));
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            renderQuadsStandaloneTexture(model, poseStack, consumer, state, LightTexture.FULL_BRIGHT, packedOverlay,
                    renderType, 0.0F, 0.0F, red, green, blue, alpha);
        }
    }

    static void renderExceptDirections(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, EnumSet<Direction> excludedDirections) {
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(renderType, false);
            VertexConsumer consumer = bufferSource.getBuffer(entityRenderType);
            renderQuadsExceptDirections(model, poseStack, consumer, state, packedLight, packedOverlay, renderType, excludedDirections);
        }
    }

    static void renderUnculledFullBright(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        renderUnculled(model, poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay);
    }

    static void renderUnculledCutoutNoCull(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.entityCutoutNoCull(TextureAtlas.LOCATION_BLOCKS));
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType);
        }
    }

    static void renderUnculledCutoutNoCullFullBright(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay) {
        renderUnculledCutoutNoCull(model, poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay);
    }

    static void renderUnculledTinted(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, int argb) {
        renderUnculledTintedUv(model, poseStack, bufferSource, state, packedLight, packedOverlay, argb, 0.0F, 0.0F);
    }

    static void renderUnculledTinted(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                      BlockState state, int packedLight, int packedOverlay,
                                      float red, float green, float blue, float alpha) {
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            RenderType entityRenderType = RenderTypeHelper.getEntityRenderType(renderType, false);
            VertexConsumer consumer = bufferSource.getBuffer(entityRenderType);
            renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType,
                    red, green, blue, alpha);
        }
    }

    static void renderUnculledTintedTranslucent(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, int argb) {
        renderUnculledTintedUv(model, poseStack, bufferSource, state, packedLight, packedOverlay, argb,
                0.0F, 0.0F, 1.0F, 1.0F, RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    }

    /**
     * Emits the same unculled tinted model data as
     * {@link #renderUnculledTintedTranslucent}, but into an already selected
     * vertex consumer. This is used when immutable machine geometry is baked
     * once into a static vertex buffer instead of being rebuilt every frame.
     */
    static void emitUnculledTinted(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                                   BlockState state, int packedLight, int packedOverlay, int argb) {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType,
                    red, green, blue, alpha);
        }
    }

    /** Untextured full-bright colour pass for legacy GL geometry rendered with texture disabled. */
    static void renderUnculledTintedLightning(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource,
                                              BlockState state, int packedOverlay, int argb) {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            renderQuads(model, poseStack, consumer, state, LightTexture.FULL_BRIGHT, packedOverlay, renderType,
                    red, green, blue, alpha);
        }
    }

    static void renderUnculledTintedUv(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, int argb, float uOffset, float vOffset) {
        renderUnculledTintedUv(model, poseStack, bufferSource, state, packedLight, packedOverlay, argb, uOffset, vOffset, 1.0F, 1.0F, null);
    }

    static void renderUnculledTintedUv(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, int argb, float uOffset, float vOffset, float uScale, float vScale) {
        renderUnculledTintedUv(model, poseStack, bufferSource, state, packedLight, packedOverlay, argb, uOffset, vOffset, uScale, vScale, null);
    }

    static void renderUnculledTintedUvEyes(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedOverlay, int argb, float uOffset, float vOffset) {
        renderUnculledTintedUv(model, poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, argb, uOffset, vOffset, 1.0F, 1.0F, RenderType.eyes(TextureAtlas.LOCATION_BLOCKS));
    }

    private static void renderUnculledTintedUv(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, int argb, float uOffset, float vOffset, float uScale, float vScale, RenderType overrideRenderType) {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        for (RenderType renderType : model.getRenderTypes(state, RANDOM, ModelData.EMPTY)) {
            RenderType entityRenderType = overrideRenderType == null ? RenderTypeHelper.getEntityRenderType(renderType, true) : overrideRenderType;
            VertexConsumer consumer = bufferSource.getBuffer(entityRenderType);
            renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType, red, green, blue, alpha, uOffset, vOffset, uScale, vScale);
        }
    }

    private static void renderQuads(BakedModel model, PoseStack poseStack, VertexConsumer consumer, BlockState state, int packedLight, int packedOverlay, RenderType renderType) {
        renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderQuads(BakedModel model, PoseStack poseStack, VertexConsumer consumer, BlockState state, int packedLight, int packedOverlay, RenderType renderType, float red, float green, float blue, float alpha) {
        renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType, red, green, blue, alpha, 0.0F, 0.0F);
    }

    private static void renderQuads(BakedModel model, PoseStack poseStack, VertexConsumer consumer, BlockState state, int packedLight, int packedOverlay, RenderType renderType, float red, float green, float blue, float alpha, float uOffset, float vOffset) {
        renderQuads(model, poseStack, consumer, state, packedLight, packedOverlay, renderType, red, green, blue, alpha, uOffset, vOffset, 1.0F, 1.0F);
    }

    private static void renderQuads(BakedModel model, PoseStack poseStack, VertexConsumer consumer, BlockState state, int packedLight, int packedOverlay, RenderType renderType, float red, float green, float blue, float alpha, float uOffset, float vOffset, float uScale, float vScale) {
        PoseStack.Pose pose = poseStack.last();
        RANDOM.setSeed(42L);
        renderQuadList(model.getQuads(state, null, RANDOM, ModelData.EMPTY, renderType), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha, uOffset, vOffset, uScale, vScale);
        for (Direction side : DIRECTIONS) {
            RANDOM.setSeed(42L);
            renderQuadList(model.getQuads(state, side, RANDOM, ModelData.EMPTY, renderType), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha, uOffset, vOffset, uScale, vScale);
        }
    }

    private static void renderQuadsExceptDirections(BakedModel model, PoseStack poseStack, VertexConsumer consumer, BlockState state, int packedLight, int packedOverlay, RenderType renderType, EnumSet<Direction> excludedDirections) {
        PoseStack.Pose pose = poseStack.last();
        RANDOM.setSeed(42L);
        renderQuadList(model.getQuads(state, null, RANDOM, ModelData.EMPTY, renderType), pose, consumer, packedLight, packedOverlay);
        for (Direction side : DIRECTIONS) {
            if (excludedDirections.contains(side)) {
                continue;
            }
            RANDOM.setSeed(42L);
            renderQuadList(model.getQuads(state, side, RANDOM, ModelData.EMPTY, renderType), pose, consumer, packedLight, packedOverlay);
        }
    }

    private static void renderQuadsStandaloneTexture(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                                                     BlockState state, int packedLight, int packedOverlay,
                                                     RenderType renderType, float uOffset, float vOffset) {
        renderQuadsStandaloneTexture(model, poseStack, consumer, state, packedLight, packedOverlay, renderType,
                uOffset, vOffset, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderQuadsStandaloneTexture(BakedModel model, PoseStack poseStack, VertexConsumer consumer,
                                                     BlockState state, int packedLight, int packedOverlay,
                                                     RenderType renderType, float uOffset, float vOffset,
                                                     float red, float green, float blue, float alpha) {
        PoseStack.Pose pose = poseStack.last();
        RANDOM.setSeed(42L);
        renderStandaloneTextureQuadList(model.getQuads(state, null, RANDOM, ModelData.EMPTY, renderType),
                pose, consumer, packedLight, packedOverlay, uOffset, vOffset, red, green, blue, alpha);
        for (Direction side : DIRECTIONS) {
            RANDOM.setSeed(42L);
            renderStandaloneTextureQuadList(model.getQuads(state, side, RANDOM, ModelData.EMPTY, renderType),
                    pose, consumer, packedLight, packedOverlay, uOffset, vOffset, red, green, blue, alpha);
        }
    }

    private static void renderQuadList(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay) {
        renderQuadList(quads, pose, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Emits an immutable main-thread-captured quad list without resolving a model again. */
    static void renderCapturedQuads(List<BakedQuad> quads, PoseStack poseStack, VertexConsumer consumer,
                                    int packedLight, int packedOverlay) {
        renderQuadList(quads, poseStack.last(), consumer, packedLight, packedOverlay);
    }

    /** Emits immutable main-thread-captured quads with the legacy translucent structure tint. */
    static void emitCapturedQuadsTinted(List<BakedQuad> quads, PoseStack poseStack, VertexConsumer consumer,
                                        int packedLight, int packedOverlay, int argb) {
        float alpha = ((argb >>> 24) & 0xFF) / 255.0F;
        float red = ((argb >>> 16) & 0xFF) / 255.0F;
        float green = ((argb >>> 8) & 0xFF) / 255.0F;
        float blue = (argb & 0xFF) / 255.0F;
        renderQuadList(quads, poseStack.last(), consumer, packedLight, packedOverlay,
                red, green, blue, alpha);
    }

    /** Emits captured standalone-model quads through the same sprite-normalized UV path as renderUnculledUv. */
    static void renderCapturedQuadsStandaloneTexture(List<BakedQuad> quads, PoseStack poseStack,
                                                     VertexConsumer consumer, int packedLight, int packedOverlay,
                                                     float uOffset, float vOffset) {
        renderStandaloneTextureQuadList(quads, poseStack.last(), consumer, packedLight, packedOverlay,
                uOffset, vOffset);
    }

    private static void renderQuadList(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        renderQuadList(quads, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha, 0.0F, 0.0F);
    }

    private static void renderQuadList(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float uOffset, float vOffset) {
        renderQuadList(quads, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha, uOffset, vOffset, 1.0F, 1.0F);
    }

    private static void renderQuadList(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha, float uOffset, float vOffset, float uScale, float vScale) {
        for (BakedQuad quad : quads) {
            if (uOffset == 0.0F && vOffset == 0.0F && uScale == 1.0F && vScale == 1.0F) {
                putBulkDataAllocationFree(consumer, pose, quad, red, green, blue, alpha, packedLight, packedOverlay);
            } else {
                putBulkDataUvTransform(consumer, pose, quad, red, green, blue, alpha, packedLight, packedOverlay, uOffset, vOffset, uScale, vScale);
            }
        }
    }

    /**
     * Allocation-free equivalent of NeoForge's no-per-vertex-shading
     * {@code putBulkData(..., true)} overload.  The platform overload creates
     * two four-element arrays per quad and vanilla additionally creates JOML
     * vectors for the face and every vertex.  Machine block entities submit
     * the same immutable baked quads every frame, so those short-lived objects
     * are pure GC pressure.
     */
    private static void putBulkDataAllocationFree(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad,
                                                   float red, float green, float blue, float alpha,
                                                   int packedLight, int packedOverlay) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        RenderScratch scratch = RENDER_SCRATCH.get();
        Vector3f position = scratch.position;
        Vector3f normal = scratch.normal;
        pose.transformNormal(
                quad.getDirection().getStepX(),
                quad.getDirection().getStepY(),
                quad.getDirection().getStepZ(),
                normal
        );

        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * stride;
            pose.pose().transformPosition(
                    Float.intBitsToFloat(vertices[base]),
                    Float.intBitsToFloat(vertices[base + 1]),
                    Float.intBitsToFloat(vertices[base + 2]),
                    position
            );

            int packedColor = vertices[base + 3];
            int color = FastColor.ARGB32.color(
                    (int) ((alpha * ((packedColor >>> 24) & 0xFF) / 255.0F) * 255.0F),
                    (int) ((packedColor & 0xFF) * red),
                    (int) (((packedColor >>> 8) & 0xFF) * green),
                    (int) (((packedColor >>> 16) & 0xFF) * blue)
            );
            int light = mergePackedLight(packedLight, stride > 6 ? vertices[base + 6] : 0);

            int packedNormal = packedNormal(vertices, base, stride);
            if ((packedNormal & 0x00FFFFFF) != 0) {
                normal.set(
                        unpackNormalX(packedNormal),
                        unpackNormalY(packedNormal),
                        unpackNormalZ(packedNormal)
                ).mul(pose.normal());
            }

            consumer.addVertex(
                    position.x(), position.y(), position.z(),
                    color,
                    Float.intBitsToFloat(vertices[base + 4]),
                    Float.intBitsToFloat(vertices[base + 5]),
                    packedOverlay,
                    light,
                    normal.x(), normal.y(), normal.z()
            );
        }
    }

    private static void putBulkDataUvTransform(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, float uOffset, float vOffset, float uScale, float vScale) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        RenderScratch scratch = RENDER_SCRATCH.get();
        Vector3f position = scratch.position;
        Vector3f normal = scratch.normal;
        int color = FastColor.ARGB32.color(
                Math.round(alpha * 255.0F),
                Math.round(red * 255.0F),
                Math.round(green * 255.0F),
                Math.round(blue * 255.0F)
        );
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * stride;
            float x = Float.intBitsToFloat(vertices[base]);
            float y = Float.intBitsToFloat(vertices[base + 1]);
            float z = Float.intBitsToFloat(vertices[base + 2]);
            float u = transformSpriteU(quad.getSprite(), Float.intBitsToFloat(vertices[base + 4]), uOffset, uScale);
            float v = transformSpriteV(quad.getSprite(), Float.intBitsToFloat(vertices[base + 5]), vOffset, vScale);
            int packedNormal = packedNormal(vertices, base, stride);
            float normalX = unpackNormalX(packedNormal);
            float normalY = unpackNormalY(packedNormal);
            float normalZ = unpackNormalZ(packedNormal);
            if (normalX == 0.0F && normalY == 0.0F && normalZ == 0.0F) {
                normalX = quad.getDirection().getStepX();
                normalY = quad.getDirection().getStepY();
                normalZ = quad.getDirection().getStepZ();
            }
            pose.pose().transformPosition(x, y, z, position);
            pose.transformNormal(normalX, normalY, normalZ, normal);
            consumer.addVertex(position.x(), position.y(), position.z(), color, u, v,
                    packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
        }
    }

    private static void renderStandaloneTextureQuadList(List<BakedQuad> quads, PoseStack.Pose pose,
                                                        VertexConsumer consumer, int packedLight, int packedOverlay,
                                                        float uOffset, float vOffset) {
        renderStandaloneTextureQuadList(quads, pose, consumer, packedLight, packedOverlay, uOffset, vOffset,
                1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void renderStandaloneTextureQuadList(List<BakedQuad> quads, PoseStack.Pose pose,
                                                        VertexConsumer consumer, int packedLight, int packedOverlay,
                                                        float uOffset, float vOffset, float red, float green, float blue, float alpha) {
        RenderScratch scratch = RENDER_SCRATCH.get();
        Vector3f position = scratch.position;
        Vector3f normal = scratch.normal;
        int color = FastColor.ARGB32.color(
                Math.round(alpha * 255.0F),
                Math.round(red * 255.0F),
                Math.round(green * 255.0F),
                Math.round(blue * 255.0F)
        );
        for (BakedQuad quad : quads) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            TextureAtlasSprite sprite = quad.getSprite();
            for (int vertex = 0; vertex < 4; vertex++) {
                int base = vertex * stride;
                float x = Float.intBitsToFloat(vertices[base]);
                float y = Float.intBitsToFloat(vertices[base + 1]);
                float z = Float.intBitsToFloat(vertices[base + 2]);
                float u = normalizeSpriteCoordinate(Float.intBitsToFloat(vertices[base + 4]), sprite.getU0(), sprite.getU1()) + uOffset;
                float v = normalizeSpriteCoordinate(Float.intBitsToFloat(vertices[base + 5]), sprite.getV0(), sprite.getV1()) + vOffset;
                int packedNormal = packedNormal(vertices, base, stride);
                float normalX = unpackNormalX(packedNormal);
                float normalY = unpackNormalY(packedNormal);
                float normalZ = unpackNormalZ(packedNormal);
                if (normalX == 0.0F && normalY == 0.0F && normalZ == 0.0F) {
                    normalX = quad.getDirection().getStepX();
                    normalY = quad.getDirection().getStepY();
                    normalZ = quad.getDirection().getStepZ();
                }
                pose.pose().transformPosition(x, y, z, position);
                pose.transformNormal(normalX, normalY, normalZ, normal);
                consumer.addVertex(position.x(), position.y(), position.z(), color, u, v,
                        packedOverlay, packedLight, normal.x(), normal.y(), normal.z());
            }
        }
    }

    private static int mergePackedLight(int requested, int baked) {
        int block = Math.max(requested & 0xFFFF, baked & 0xFFFF);
        int sky = Math.max((requested >>> 16) & 0xFFFF, (baked >>> 16) & 0xFFFF);
        return block | (sky << 16);
    }

    private static float normalizeSpriteCoordinate(float value, float min, float max) {
        float width = max - min;
        return width <= 0.0F ? value : (value - min) / width;
    }

    private static int packedNormal(int[] vertices, int base, int stride) {
        return stride > 7 ? vertices[base + 7] : 0;
    }

    private static float unpackNormalX(int packed) {
        return (byte) (packed & 0xFF) / 127.0F;
    }

    private static float unpackNormalY(int packed) {
        return (byte) ((packed >>> 8) & 0xFF) / 127.0F;
    }

    private static float unpackNormalZ(int packed) {
        return (byte) ((packed >>> 16) & 0xFF) / 127.0F;
    }

    private static float transformSpriteU(TextureAtlasSprite sprite, float u, float offset, float scale) {
        float min = sprite.getU0();
        float max = sprite.getU1();
        return transformSpriteCoordinate(u, offset, scale, min, max);
    }

    private static float transformSpriteV(TextureAtlasSprite sprite, float v, float offset, float scale) {
        float min = sprite.getV0();
        float max = sprite.getV1();
        return transformSpriteCoordinate(v, offset, scale, min, max);
    }

    private static final class RenderScratch {
        private final Matrix4f modelView = new Matrix4f();
        private final Vector3f position = new Vector3f();
        private final Vector3f normal = new Vector3f();
    }

    private static float transformSpriteCoordinate(float value, float offset, float scale, float min, float max) {
        if (offset == 0.0F && scale == 1.0F) {
            return value;
        }
        float width = max - min;
        if (width <= 0.0F) {
            return value;
        }
        float normalized = (value - min) / width;
        normalized = (normalized + offset) * scale;
        normalized = normalized - (float) Math.floor(normalized);
        return min + normalized * width;
    }

}
