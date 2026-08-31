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
                             EnumMap<Direction, TextureAtlasSprite> sprites,
                             EnumMap<Direction, Integer> rotations) {
        addLegacyBox(quads, minX, minY, minZ, maxX, maxY, maxZ, sprites, Set.of(), rotations);
    }

    static void addLegacyBox(List<BakedQuad> quads, float minX, float minY, float minZ,
                             float maxX, float maxY, float maxZ,
                             EnumMap<Direction, TextureAtlasSprite> sprites,
                             Set<Direction> hiddenFaces, EnumMap<Direction, Integer> rotations) {
        for (Direction direction : Direction.values()) {
            if (hiddenFaces.contains(direction)) {
                continue;
            }
            quads.add(legacyFace(direction, minX, minY, minZ, maxX, maxY, maxZ,
                    sprites.get(direction), rotations.getOrDefault(direction, 0)));
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
        float x0 = minX * 16.0F;
        float x1 = maxX * 16.0F;
        float y0 = minY * 16.0F;
        float y1 = maxY * 16.0F;
        float z0 = minZ * 16.0F;
        float z1 = maxZ * 16.0F;
        return switch (face) {
            case DOWN -> switch (oldRotation) {
                case 0 -> uv(x0, z1, x0, z0, x1, z0, x1, z1);
                case 1 -> uv(16.0F - z1, x0, 16.0F - z0, x0,
                        16.0F - z0, x1, 16.0F - z1, x1);
                case 2 -> uv(z1, 16.0F - x0, z0, 16.0F - x0,
                        z0, 16.0F - x1, z1, 16.0F - x1);
                case 3 -> uv(16.0F - x0, 16.0F - z1, 16.0F - x0, 16.0F - z0,
                        16.0F - x1, 16.0F - z0, 16.0F - x1, 16.0F - z1);
                default -> throw invalidRotation(oldRotation);
            };
            case UP -> switch (oldRotation) {
                case 0 -> uv(x1, z1, x1, z0, x0, z0, x0, z1);
                case 1 -> uv(z1, 16.0F - x1, z0, 16.0F - x1,
                        z0, 16.0F - x0, z1, 16.0F - x0);
                case 2 -> uv(16.0F - z1, x1, 16.0F - z0, x1,
                        16.0F - z0, x0, 16.0F - z1, x0);
                case 3 -> uv(16.0F - x1, 16.0F - z1, 16.0F - x1, 16.0F - z0,
                        16.0F - x0, 16.0F - z0, 16.0F - x0, 16.0F - z1);
                default -> throw invalidRotation(oldRotation);
            };
            case NORTH -> switch (oldRotation) {
                case 0 -> uv(x1, 16.0F - y1, x0, 16.0F - y1,
                        x0, 16.0F - y0, x1, 16.0F - y0);
                case 1 -> uv(16.0F - y0, x0, 16.0F - y0, x1,
                        16.0F - y1, x1, 16.0F - y1, x0);
                case 2 -> uv(y0, 16.0F - x0, y0, 16.0F - x1,
                        y1, 16.0F - x1, y1, 16.0F - x0);
                case 3 -> uv(16.0F - x1, y1, 16.0F - x0, y1,
                        16.0F - x0, y0, 16.0F - x1, y0);
                default -> throw invalidRotation(oldRotation);
            };
            case SOUTH -> switch (oldRotation) {
                case 0 -> uv(x0, 16.0F - y1, x0, 16.0F - y0,
                        x1, 16.0F - y0, x1, 16.0F - y1);
                case 1 -> uv(y0, 16.0F - x0, y1, 16.0F - x0,
                        y1, 16.0F - x1, y0, 16.0F - x1);
                case 2 -> uv(16.0F - y0, x0, 16.0F - y1, x0,
                        16.0F - y1, x1, 16.0F - y0, x1);
                case 3 -> uv(16.0F - x0, y1, 16.0F - x0, y0,
                        16.0F - x1, y0, 16.0F - x1, y1);
                default -> throw invalidRotation(oldRotation);
            };
            case WEST -> switch (oldRotation) {
                case 0 -> uv(z1, 16.0F - y1, z0, 16.0F - y1,
                        z0, 16.0F - y0, z1, 16.0F - y0);
                case 1 -> uv(y0, 16.0F - z1, y0, 16.0F - z0,
                        y1, 16.0F - z0, y1, 16.0F - z1);
                case 2 -> uv(16.0F - y0, z1, 16.0F - y0, z0,
                        16.0F - y1, z0, 16.0F - y1, z1);
                case 3 -> uv(16.0F - z1, y1, 16.0F - z0, y1,
                        16.0F - z0, y0, 16.0F - z1, y0);
                default -> throw invalidRotation(oldRotation);
            };
            case EAST -> switch (oldRotation) {
                case 0 -> uv(z0, 16.0F - y0, z1, 16.0F - y0,
                        z1, 16.0F - y1, z0, 16.0F - y1);
                case 1 -> uv(16.0F - y1, z1, 16.0F - y1, z0,
                        16.0F - y0, z0, 16.0F - y0, z1);
                case 2 -> uv(y1, 16.0F - z1, y1, 16.0F - z0,
                        y0, 16.0F - z0, y0, 16.0F - z1);
                case 3 -> uv(16.0F - z0, y0, 16.0F - z1, y0,
                        16.0F - z1, y1, 16.0F - z0, y1);
                default -> throw invalidRotation(oldRotation);
            };
        };
    }

    private static FaceUv uv(float u0, float v0, float u1, float v1,
                             float u2, float v2, float u3, float v3) {
        return new FaceUv(u0, v0, u1, v1, u2, v2, u3, v3);
    }

    private static IllegalArgumentException invalidRotation(int rotation) {
        return new IllegalArgumentException("Unsupported legacy UV rotation: " + rotation);
    }

    private record Vertex(float x, float y, float z, float u, float v) {
    }

    private record Uv(float u, float v) {
    }

    private record FaceUv(float u0, float v0, float u1, float v1,
                          float u2, float v2, float u3, float v3) {
    }
}
