package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class LegacyEntityObjMesh {
    private final List<Vec3> positions;
    private final List<Uv> uvs;
    private final List<Vec3> normals;
    private final Map<String, List<ObjFace>> groups;

    private LegacyEntityObjMesh(List<Vec3> positions, List<Uv> uvs, List<Vec3> normals, Map<String, List<ObjFace>> groups) {
        this.positions = positions;
        this.uvs = uvs;
        this.normals = normals;
        this.groups = groups;
    }

    static LegacyEntityObjMesh load(ResourceLocation location) {
        String resourcePath = "assets/" + location.getNamespace() + "/" + location.getPath();
        try (InputStream stream = LegacyEntityObjMesh.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new IllegalStateException("Missing legacy OBJ mesh: " + location);
            }
            return parse(stream, location);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to load legacy OBJ mesh: " + location, exception);
        }
    }

    void renderAll(PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb) {
        for (List<ObjFace> faces : groups.values()) {
            renderFaces(faces, poseStack, consumer, packedLight, packedOverlay, argb);
        }
    }

    void renderGroup(String name, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb) {
        renderFaces(groups.getOrDefault(name, List.of()), poseStack, consumer, packedLight, packedOverlay, argb);
    }

    private static LegacyEntityObjMesh parse(InputStream stream, ResourceLocation location) throws IOException {
        List<Vec3> positions = new ArrayList<>();
        List<Uv> uvs = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        Map<String, List<ObjFace>> groups = new HashMap<>();
        String group = "";
        groups.put(group, new ArrayList<>());

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                String[] tokens = line.split("\\s+");
                switch (tokens[0]) {
                    case "o", "g" -> {
                        if (tokens.length > 1) {
                            group = tokens[1];
                            groups.computeIfAbsent(group, ignored -> new ArrayList<>());
                        }
                    }
                    case "v" -> positions.add(new Vec3(parseFloat(tokens, 1), parseFloat(tokens, 2), parseFloat(tokens, 3)));
                    case "vt" -> uvs.add(new Uv(parseFloat(tokens, 1), 1.0F - parseFloat(tokens, 2)));
                    case "vn" -> normals.add(new Vec3(parseFloat(tokens, 1), parseFloat(tokens, 2), parseFloat(tokens, 3)).normalize());
                    case "f" -> groups.get(group).add(parseFace(tokens, positions.size(), uvs.size(), normals.size()));
                    default -> {
                    }
                }
            }
        } catch (RuntimeException exception) {
            throw new IllegalStateException("Failed to parse legacy OBJ mesh: " + location, exception);
        }

        Map<String, List<ObjFace>> frozen = new HashMap<>();
        for (Map.Entry<String, List<ObjFace>> entry : groups.entrySet()) {
            frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return new LegacyEntityObjMesh(List.copyOf(positions), List.copyOf(uvs), List.copyOf(normals), Map.copyOf(frozen));
    }

    private void renderFaces(List<ObjFace> faces, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        PoseStack.Pose pose = poseStack.last();
        for (ObjFace face : faces) {
            if (face.vertices().size() < 3) {
                continue;
            }
            for (int i = 1; i < face.vertices().size() - 1; i++) {
                emitVertex(face.vertices().get(0), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
                emitVertex(face.vertices().get(i), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
                emitVertex(face.vertices().get(i + 1), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            }
        }
    }

    private void emitVertex(ObjVertex vertex, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, int red, int green, int blue, int alpha) {
        Vec3 position = positionAt(vertex.position());
        Uv uv = uvAt(vertex.uv());
        Vec3 normal = normalAt(vertex.normal());
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(uv.u(), uv.v())
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private Vec3 positionAt(int index) {
        return index >= 0 && index < positions.size() ? positions.get(index) : Vec3.ZERO;
    }

    private Uv uvAt(int index) {
        return index >= 0 && index < uvs.size() ? uvs.get(index) : Uv.ZERO;
    }

    private Vec3 normalAt(int index) {
        return index >= 0 && index < normals.size() ? normals.get(index) : new Vec3(0.0D, 1.0D, 0.0D);
    }

    private static ObjFace parseFace(String[] tokens, int positionCount, int uvCount, int normalCount) {
        List<ObjVertex> vertices = new ArrayList<>(tokens.length - 1);
        for (int i = 1; i < tokens.length; i++) {
            String[] parts = tokens[i].split("/", -1);
            int position = parseIndex(parts[0], positionCount);
            int uv = parts.length > 1 && !parts[1].isBlank() ? parseIndex(parts[1], uvCount) : -1;
            int normal = parts.length > 2 && !parts[2].isBlank() ? parseIndex(parts[2], normalCount) : -1;
            vertices.add(new ObjVertex(position, uv, normal));
        }
        return new ObjFace(List.copyOf(vertices));
    }

    private static int parseIndex(String raw, int size) {
        int index = Integer.parseInt(raw);
        return index < 0 ? size + index : index - 1;
    }

    private static float parseFloat(String[] tokens, int index) {
        return index < tokens.length ? Float.parseFloat(tokens[index]) : 0.0F;
    }

    private static final class Uv {
        private static final Uv ZERO = new Uv(0.0F, 0.0F);
        private final float u;
        private final float v;

        private Uv(float u, float v) {
            this.u = u;
            this.v = v;
        }

        private float u() { return u; }
        private float v() { return v; }
    }

    private record ObjFace(List<ObjVertex> vertices) {
    }

    private record ObjVertex(int position, int uv, int normal) {
    }
}
