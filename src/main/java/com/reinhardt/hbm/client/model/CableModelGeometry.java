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
        for (Direction direction : Direction.values()) {
            if (!hiddenFaces.contains(direction)) {
                quads.add(face(direction, minX, minY, minZ, maxX, maxY, maxZ, sprites.get(direction)));
            }
        }
    }

    static BakedQuad face(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                          TextureAtlasSprite sprite) {
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
            baker.setUv(sprite.getU(vertex.u), sprite.getV(vertex.v));
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

    private record Vertex(float x, float y, float z, float u, float v) {
    }
}
