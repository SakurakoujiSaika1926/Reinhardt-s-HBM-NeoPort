package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.RenderTypeHelper;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.EnumSet;
import java.util.List;

final class MachineModelRenderer {
    private static final RandomSource RANDOM = RandomSource.create();

    private MachineModelRenderer() {
    }

    static ModelResourceLocation standalone(String path) {
        return ModelResourceLocation.standalone(ReinhardtsHBM.id(path));
    }

    static BakedModel model(ModelResourceLocation location) {
        return Minecraft.getInstance().getModelManager().getModel(location);
    }

    static void orient(PoseStack poseStack, Direction facing, float extraDegrees) {
        orientYaw(poseStack, yaw(facing) + extraDegrees);
    }

    static void orientLegacySouthZero(PoseStack poseStack, Direction facing) {
        orientYaw(poseStack, 180.0F - yaw(facing));
    }

    static void orientLegacyStirlingParts(PoseStack poseStack, Direction facing) {
        orientYaw(poseStack, legacyStirlingYaw(facing));
    }

    static void orientLegacyHeaterParts(PoseStack poseStack, Direction facing) {
        orientYaw(poseStack, legacyHeaterYaw(facing));
    }

    static void orientLegacyGasTurbineParts(PoseStack poseStack, Direction facing) {
        orientLegacyWavefrontOriginYaw(poseStack, legacyGasTurbineYaw(facing));
    }

    static void orientYaw(PoseStack poseStack, float degrees) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(yawQuaternion(degrees));
        poseStack.translate(-0.5F, 0.0F, -0.5F);
    }

    static void orientLegacyWavefrontOriginYaw(PoseStack poseStack, float degrees) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(yawQuaternion(degrees));
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

    static void renderUnculledTintedTranslucent(BakedModel model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay, int argb) {
        renderUnculledTintedUv(model, poseStack, bufferSource, state, packedLight, packedOverlay, argb,
                0.0F, 0.0F, 1.0F, 1.0F, RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
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
        for (Direction side : Direction.values()) {
            RANDOM.setSeed(42L);
            renderQuadList(model.getQuads(state, side, RANDOM, ModelData.EMPTY, renderType), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha, uOffset, vOffset, uScale, vScale);
        }
    }

    private static void renderQuadsExceptDirections(BakedModel model, PoseStack poseStack, VertexConsumer consumer, BlockState state, int packedLight, int packedOverlay, RenderType renderType, EnumSet<Direction> excludedDirections) {
        PoseStack.Pose pose = poseStack.last();
        RANDOM.setSeed(42L);
        renderQuadList(model.getQuads(state, null, RANDOM, ModelData.EMPTY, renderType), pose, consumer, packedLight, packedOverlay);
        for (Direction side : Direction.values()) {
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
        PoseStack.Pose pose = poseStack.last();
        RANDOM.setSeed(42L);
        renderStandaloneTextureQuadList(model.getQuads(state, null, RANDOM, ModelData.EMPTY, renderType),
                pose, consumer, packedLight, packedOverlay, uOffset, vOffset);
        for (Direction side : Direction.values()) {
            RANDOM.setSeed(42L);
            renderStandaloneTextureQuadList(model.getQuads(state, side, RANDOM, ModelData.EMPTY, renderType),
                    pose, consumer, packedLight, packedOverlay, uOffset, vOffset);
        }
    }

    private static void renderQuadList(List<BakedQuad> quads, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay) {
        renderQuadList(quads, pose, consumer, packedLight, packedOverlay, 1.0F, 1.0F, 1.0F, 1.0F);
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
                consumer.putBulkData(pose, quad, red, green, blue, alpha, packedLight, packedOverlay, true);
            } else {
                putBulkDataUvTransform(consumer, pose, quad, red, green, blue, alpha, packedLight, packedOverlay, uOffset, vOffset, uScale, vScale);
            }
        }
    }

    private static void putBulkDataUvTransform(VertexConsumer consumer, PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue, float alpha, int packedLight, int packedOverlay, float uOffset, float vOffset, float uScale, float vScale) {
        int[] vertices = quad.getVertices();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int base = vertex * stride;
            float x = Float.intBitsToFloat(vertices[base]);
            float y = Float.intBitsToFloat(vertices[base + 1]);
            float z = Float.intBitsToFloat(vertices[base + 2]);
            float u = transformSpriteU(quad.getSprite(), Float.intBitsToFloat(vertices[base + 4]), uOffset, uScale);
            float v = transformSpriteV(quad.getSprite(), Float.intBitsToFloat(vertices[base + 5]), vOffset, vScale);
            float[] normal = unpackNormal(vertices, base, stride, quad.getDirection());
            consumer.addVertex(pose, x, y, z)
                    .setColor(Math.round(red * 255.0F), Math.round(green * 255.0F), Math.round(blue * 255.0F), Math.round(alpha * 255.0F))
                    .setUv(u, v)
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(pose, normal[0], normal[1], normal[2]);
        }
    }

    private static void renderStandaloneTextureQuadList(List<BakedQuad> quads, PoseStack.Pose pose,
                                                        VertexConsumer consumer, int packedLight, int packedOverlay,
                                                        float uOffset, float vOffset) {
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
                float[] normal = unpackNormal(vertices, base, stride, quad.getDirection());
                consumer.addVertex(pose, x, y, z)
                        .setColor(255, 255, 255, 255)
                        .setUv(u, v)
                        .setOverlay(packedOverlay)
                        .setLight(packedLight)
                        .setNormal(pose, normal[0], normal[1], normal[2]);
            }
        }
    }

    private static float normalizeSpriteCoordinate(float value, float min, float max) {
        float width = max - min;
        return width <= 0.0F ? value : (value - min) / width;
    }

    private static float[] unpackNormal(int[] vertices, int base, int stride, Direction fallback) {
        if (stride > 7) {
            int packed = vertices[base + 7];
            float x = (byte) (packed & 0xFF) / 127.0F;
            float y = (byte) ((packed >>> 8) & 0xFF) / 127.0F;
            float z = (byte) ((packed >>> 16) & 0xFF) / 127.0F;
            if (x != 0.0F || y != 0.0F || z != 0.0F) {
                return new float[]{x, y, z};
            }
        }
        return new float[]{fallback.getStepX(), fallback.getStepY(), fallback.getStepZ()};
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

    private static Quaternionf yawQuaternion(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    static float yaw(Direction facing) {
        return LegacyMachineGeometry.yawModernNorth(facing);
    }

    private static float legacyHeaterYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static float legacyGasTurbineYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static float legacyStirlingYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }
}
