package com.reinhardt.hbm.client.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Set;

/** Shared uncullable box geometry for the 1.7.10 cable renderers. */
final class CableModelGeometry {
    private CableModelGeometry() {
    }

    static List<BakedQuad> box(float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                               EnumMap<Direction, TextureAtlasSprite> sprites, Set<Direction> hiddenFaces) {
        List<BakedQuad> quads = new ArrayList<>();
        addBox(quads, minX, minY, minZ, maxX, maxY, maxZ, sprites, hiddenFaces);
        return List.copyOf(quads);
    }

    static void addBox(List<BakedQuad> quads, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                       EnumMap<Direction, TextureAtlasSprite> sprites, Set<Direction> hiddenFaces) {
        addBox(quads, minX, minY, minZ, maxX, maxY, maxZ, sprites, hiddenFaces, 0.0F, 16.0F, 0.0F, 16.0F);
    }

    /**
     * Adds an atlas-space UV window. Legacy cable icons intentionally pack
     * separate cap and segment tiles into one 16x16 texture.
     */
    static void addBox(List<BakedQuad> quads, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                       EnumMap<Direction, TextureAtlasSprite> sprites, Set<Direction> hiddenFaces,
                       float minU, float maxU, float minV, float maxV) {
        for (Direction direction : Direction.values()) {
            if (!hiddenFaces.contains(direction)) {
                quads.add(face(direction, minX, minY, minZ, maxX, maxY, maxZ, sprites.get(direction),
                        minU, maxU, minV, maxV));
            }
        }
    }

    static void addLegacyBox(List<BakedQuad> quads, float minX, float minY, float minZ,
                             float maxX, float maxY, float maxZ,
                             EnumMap<Direction, TextureAtlasSprite> sprites, Set<Direction> hiddenFaces,
                             int northRotation, int southRotation) {
        for (Direction direction : Direction.values()) {
            if (hiddenFaces.contains(direction)) {
                continue;
            }
            int rotation = direction == Direction.NORTH ? northRotation
                    : direction == Direction.SOUTH ? southRotation : 0;
            quads.add(legacyFace(direction, minX, minY, minZ, maxX, maxY, maxZ,
                    sprites.get(direction), rotation));
        }
    }

    static BakedQuad face(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                          TextureAtlasSprite sprite) {
        return face(face, minX, minY, minZ, maxX, maxY, maxZ, sprite, 0.0F, 16.0F, 0.0F, 16.0F);
    }

    private static BakedQuad face(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                  TextureAtlasSprite sprite, float minU, float maxU, float minV, float maxV) {
        Vertex[] vertices = vertices(face, minX, minY, minZ, maxX, maxY, maxZ);
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(-1);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(face);
        for (Vertex vertex : vertices) {
            baker.addVertex(vertex.x, vertex.y, vertex.z);
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(minU + (maxU - minU) * vertex.u),
                    sprite.getV(minV + (maxV - minV) * vertex.v));
            baker.setLight(0);
            baker.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
        }
        return baker.bakeQuad();
    }

    static EnumMap<Direction, TextureAtlasSprite> all(TextureAtlasSprite sprite) {
        EnumMap<Direction, TextureAtlasSprite> sprites = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            sprites.put(direction, sprite);
        }
        return sprites;
    }

    private static Vertex[] vertices(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return switch (face) {
            case DOWN -> new Vertex[]{
                    new Vertex(minX, minY, maxZ, 0.0F, 0.0F), new Vertex(minX, minY, minZ, 0.0F, 1.0F),
                    new Vertex(maxX, minY, minZ, 1.0F, 1.0F), new Vertex(maxX, minY, maxZ, 1.0F, 0.0F)
            };
            case UP -> new Vertex[]{
                    new Vertex(maxX, maxY, maxZ, 0.0F, 0.0F), new Vertex(maxX, maxY, minZ, 0.0F, 1.0F),
                    new Vertex(minX, maxY, minZ, 1.0F, 1.0F), new Vertex(minX, maxY, maxZ, 1.0F, 0.0F)
            };
            case NORTH -> new Vertex[]{
                    new Vertex(minX, maxY, minZ, 0.0F, 0.0F), new Vertex(maxX, maxY, minZ, 1.0F, 0.0F),
                    new Vertex(maxX, minY, minZ, 1.0F, 1.0F), new Vertex(minX, minY, minZ, 0.0F, 1.0F)
            };
            case SOUTH -> new Vertex[]{
                    new Vertex(maxX, maxY, maxZ, 0.0F, 0.0F), new Vertex(minX, maxY, maxZ, 1.0F, 0.0F),
                    new Vertex(minX, minY, maxZ, 1.0F, 1.0F), new Vertex(maxX, minY, maxZ, 0.0F, 1.0F)
            };
            case WEST -> new Vertex[]{
                    new Vertex(minX, maxY, maxZ, 0.0F, 0.0F), new Vertex(minX, maxY, minZ, 1.0F, 0.0F),
                    new Vertex(minX, minY, minZ, 1.0F, 1.0F), new Vertex(minX, minY, maxZ, 0.0F, 1.0F)
            };
            case EAST -> new Vertex[]{
                    new Vertex(maxX, maxY, minZ, 0.0F, 0.0F), new Vertex(maxX, maxY, maxZ, 1.0F, 0.0F),
                    new Vertex(maxX, minY, maxZ, 1.0F, 1.0F), new Vertex(maxX, minY, minZ, 0.0F, 1.0F)
            };
        };
    }

    private static BakedQuad legacyFace(Direction face, float minX, float minY, float minZ,
                                        float maxX, float maxY, float maxZ,
                                        TextureAtlasSprite sprite, int oldRotation) {
        Vertex[] vertices = legacyVertices(face, minX, minY, minZ, maxX, maxY, maxZ);
        FaceUv uv = legacyUv(face, minX, minY, minZ, maxX, maxY, maxZ, oldRotation);
        Uv[] uvs = {new Uv(uv.u0, uv.v0), new Uv(uv.u1, uv.v1),
                new Uv(uv.u2, uv.v2), new Uv(uv.u3, uv.v3)};
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(-1);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(face);
        for (int index = 0; index < vertices.length; index++) {
            Vertex vertex = vertices[index];
            baker.addVertex(vertex.x, vertex.y, vertex.z);
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(uvs[index].u / 16.0F), sprite.getV(uvs[index].v / 16.0F));
            baker.setLight(0);
            baker.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
        }
        return baker.bakeQuad();
    }

    private static Vertex[] legacyVertices(Direction face, float minX, float minY, float minZ,
                                            float maxX, float maxY, float maxZ) {
        return switch (face) {
            case DOWN -> new Vertex[]{
                    new Vertex(minX, minY, maxZ, 0.0F, 0.0F), new Vertex(minX, minY, minZ, 0.0F, 1.0F),
                    new Vertex(maxX, minY, minZ, 0.0F, 0.0F), new Vertex(maxX, minY, maxZ, 0.0F, 0.0F)
            };
            case UP -> new Vertex[]{
                    new Vertex(maxX, maxY, maxZ, 0.0F, 0.0F), new Vertex(maxX, maxY, minZ, 0.0F, 0.0F),
                    new Vertex(minX, maxY, minZ, 0.0F, 0.0F), new Vertex(minX, maxY, maxZ, 0.0F, 0.0F)
            };
            case NORTH -> new Vertex[]{
                    new Vertex(minX, maxY, minZ, 0.0F, 0.0F), new Vertex(maxX, maxY, minZ, 0.0F, 0.0F),
                    new Vertex(maxX, minY, minZ, 0.0F, 0.0F), new Vertex(minX, minY, minZ, 0.0F, 0.0F)
            };
            case SOUTH -> new Vertex[]{
                    new Vertex(minX, maxY, maxZ, 0.0F, 0.0F), new Vertex(minX, minY, maxZ, 0.0F, 0.0F),
                    new Vertex(maxX, minY, maxZ, 0.0F, 0.0F), new Vertex(maxX, maxY, maxZ, 0.0F, 0.0F)
            };
            case WEST -> new Vertex[]{
                    new Vertex(minX, maxY, maxZ, 0.0F, 0.0F), new Vertex(minX, maxY, minZ, 0.0F, 0.0F),
                    new Vertex(minX, minY, minZ, 0.0F, 0.0F), new Vertex(minX, minY, maxZ, 0.0F, 0.0F)
            };
            case EAST -> new Vertex[]{
                    new Vertex(maxX, minY, maxZ, 0.0F, 0.0F), new Vertex(maxX, minY, minZ, 0.0F, 0.0F),
                    new Vertex(maxX, maxY, minZ, 0.0F, 0.0F), new Vertex(maxX, maxY, maxZ, 0.0F, 0.0F)
            };
        };
    }

    private static FaceUv legacyUv(Direction face, float minX, float minY, float minZ,
                                   float maxX, float maxY, float maxZ, int oldRotation) {
        FaceUv uv = switch (face) {
            case DOWN, UP -> faceUvY(minX, minZ, maxX, maxZ);
            case NORTH -> faceUvNorth(minX, minY, maxX, maxY);
            case SOUTH -> faceUvSouth(minX, minY, maxX, maxY);
            case WEST -> faceUvWest(minZ, minY, maxZ, maxY);
            case EAST -> faceUvEast(minZ, minY, maxZ, maxY);
        };
        return rotateLegacyUv(uv, oldRotation);
    }

    private static FaceUv faceUvY(float minX, float minZ, float maxX, float maxZ) {
        return new FaceUv(minX * 16.0F, maxZ * 16.0F, minX * 16.0F, minZ * 16.0F,
                maxX * 16.0F, minZ * 16.0F, maxX * 16.0F, maxZ * 16.0F);
    }

    private static FaceUv faceUvNorth(float minX, float minY, float maxX, float maxY) {
        return new FaceUv(16.0F - minX * 16.0F, 16.0F - maxY * 16.0F,
                16.0F - maxX * 16.0F, 16.0F - maxY * 16.0F,
                16.0F - maxX * 16.0F, 16.0F - minY * 16.0F,
                16.0F - minX * 16.0F, 16.0F - minY * 16.0F);
    }

    private static FaceUv faceUvSouth(float minX, float minY, float maxX, float maxY) {
        return new FaceUv(minX * 16.0F, 16.0F - maxY * 16.0F,
                minX * 16.0F, 16.0F - minY * 16.0F,
                maxX * 16.0F, 16.0F - minY * 16.0F,
                maxX * 16.0F, 16.0F - maxY * 16.0F);
    }

    private static FaceUv faceUvWest(float minZ, float minY, float maxZ, float maxY) {
        return new FaceUv(maxZ * 16.0F, 16.0F - maxY * 16.0F,
                minZ * 16.0F, 16.0F - maxY * 16.0F,
                minZ * 16.0F, 16.0F - minY * 16.0F,
                maxZ * 16.0F, 16.0F - minY * 16.0F);
    }

    private static FaceUv faceUvEast(float minZ, float minY, float maxZ, float maxY) {
        return new FaceUv(16.0F - maxZ * 16.0F, 16.0F - minY * 16.0F,
                16.0F - minZ * 16.0F, 16.0F - minY * 16.0F,
                16.0F - minZ * 16.0F, 16.0F - maxY * 16.0F,
                16.0F - maxZ * 16.0F, 16.0F - maxY * 16.0F);
    }

    private static FaceUv rotateLegacyUv(FaceUv uv, int rotation) {
        return switch (rotation) {
            case 1 -> uv.map((u, v) -> new Uv(16.0F - v, 16.0F - u));
            case 2 -> uv.map((u, v) -> new Uv(v, u));
            case 3 -> uv.map((u, v) -> new Uv(16.0F - u, 16.0F - v));
            default -> uv;
        };
    }

    private record Vertex(float x, float y, float z, float u, float v) {
    }

    private record Uv(float u, float v) {
    }

    private record FaceUv(float u0, float v0, float u1, float v1,
                          float u2, float v2, float u3, float v3) {
        private FaceUv map(java.util.function.BiFunction<Float, Float, Uv> transform) {
            Uv first = transform.apply(this.u0, this.v0);
            Uv second = transform.apply(this.u1, this.v1);
            Uv third = transform.apply(this.u2, this.v2);
            Uv fourth = transform.apply(this.u3, this.v3);
            return new FaceUv(first.u, first.v, second.u, second.v,
                    third.u, third.v, fourth.u, fourth.v);
        }
    }
}
