package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.EnergyCableBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class RedCableNeoBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation BLOCK_ID = ReinhardtsHBM.id("red_cable");
    private static final ResourceLocation OBJ_LOCATION = ReinhardtsHBM.id("models/obj/red_cable/cable_neo.obj");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("block/cable_neo");
    private static final Transform WORLD_TRANSFORM = new Transform(1.0F, 0.0F, 0.0F, 0.0F, 0.0F, true);
    private static final Transform ITEM_TRANSFORM = new Transform(1.0F, 0.5F, 1.0F / 16.0F, 0.5F, (float) Math.PI, false);
    private static final ChunkRenderTypeSet CUTOUT_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final Uv[] DEFAULT_UVS = {
            new Uv(0.0F, 0.0F),
            new Uv(0.0F, 1.0F),
            new Uv(1.0F, 1.0F),
            new Uv(1.0F, 0.0F)
    };

    private final BakedModel fallbackModel;
    private final ObjMesh mesh;
    private final TextureAtlasSprite sprite;
    private final boolean forBlock;
    private final List<BakedQuad>[] blockQuads;
    private final List<BakedQuad> itemQuads;

    @SuppressWarnings("unchecked")
    private RedCableNeoBakedModel(BakedModel fallbackModel, ObjMesh mesh, TextureAtlasSprite sprite, boolean forBlock) {
        this.fallbackModel = fallbackModel;
        this.mesh = mesh;
        this.sprite = sprite;
        this.forBlock = forBlock;
        if (forBlock) {
            this.blockQuads = precomputeBlockQuads();
            this.itemQuads = List.of();
        } else {
            this.blockQuads = new List[0];
            this.itemQuads = bakeParts(Set.of("Core", "posX", "negX", "posZ", "negZ"), ITEM_TRANSFORM);
        }
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textureGetter) {
        List<ModelResourceLocation> blockLocations = models.keySet().stream()
                .filter(RedCableNeoBakedModel::isBlockModel)
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(BLOCK_ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel blockFallback = blockLocations.stream()
                .map(models::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(models.get(itemLocation));
        if (blockFallback == null) {
            ReinhardtsHBM.LOGGER.warn("Unable to install red copper cable OBJ model: no baked model was found");
            return;
        }

        ObjMesh mesh = ObjMesh.load(OBJ_LOCATION);
        if (mesh.isEmpty()) {
            ReinhardtsHBM.LOGGER.warn("Unable to install red copper cable OBJ model: {} did not load", OBJ_LOCATION);
            return;
        }

        TextureAtlasSprite sprite = textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, TEXTURE));
        RedCableNeoBakedModel blockModel = new RedCableNeoBakedModel(blockFallback, mesh, sprite, true);
        for (ModelResourceLocation location : blockLocations) {
            models.put(location, blockModel);
        }

        BakedModel itemFallback = models.getOrDefault(itemLocation, blockFallback);
        if (models.containsKey(itemLocation)) {
            models.put(itemLocation, new RedCableNeoBakedModel(itemFallback, mesh, sprite, false));
        }
        ReinhardtsHBM.LOGGER.info("Installed red copper cable OBJ model for {} baked block variants", blockLocations.size());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        if (side != null) {
            return Collections.emptyList();
        }
        if (!this.forBlock) {
            return this.itemQuads;
        }

        int mask = connectionMask(state);
        return this.blockQuads[mask];
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return this.forBlock ? CUTOUT_RENDER_TYPES : this.fallbackModel.getRenderTypes(state, rand, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return this.sprite;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.sprite;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return this.fallbackModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.fallbackModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.fallbackModel.isCustomRenderer();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.fallbackModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.fallbackModel.getOverrides();
    }

    @SuppressWarnings("unchecked")
    private List<BakedQuad>[] precomputeBlockQuads() {
        List<BakedQuad>[] quads = new List[64];
        for (int mask = 0; mask < quads.length; mask++) {
            quads[mask] = bakeParts(partsFor(mask), WORLD_TRANSFORM);
        }
        return quads;
    }

    private List<BakedQuad> bakeParts(Set<String> parts, Transform transform) {
        List<ObjFace> faces = this.mesh.faces(parts);
        List<BakedQuad> quads = new ArrayList<>(faces.size());
        for (ObjFace face : faces) {
            quads.add(bakeFace(face, transform));
        }
        return List.copyOf(quads);
    }

    private BakedQuad bakeFace(ObjFace face, Transform transform) {
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(this.sprite);
        baker.setTintIndex(-1);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);

        Vec3 faceNormal = transformedFaceNormal(face, transform);
        baker.setDirection(Direction.getNearest(faceNormal.x(), faceNormal.y(), faceNormal.z()));
        for (int i = 0; i < 4; i++) {
            ObjVertex vertex = face.vertices().get(Math.min(i, face.vertices().size() - 1));
            Vec3 position = transform.position(this.mesh.position(vertex.position()));
            Vec3 normal = vertex.normal() >= 0 ? transform.normal(this.mesh.normal(vertex.normal())).normalize() : faceNormal;
            Uv uv = vertex.uv() >= 0 ? this.mesh.uv(vertex.uv()) : DEFAULT_UVS[i];

            baker.addVertex(position.x(), position.y(), position.z());
            baker.setColor(255, 255, 255, 255);
            baker.setUv(this.sprite.getU(uv.u()), this.sprite.getV(uv.v()));
            baker.setLight(0);
            baker.setNormal(normal.x(), normal.y(), normal.z());
        }
        return baker.bakeQuad();
    }

    private Vec3 transformedFaceNormal(ObjFace face, Transform transform) {
        if (!face.vertices().isEmpty() && face.vertices().getFirst().normal() >= 0) {
            return transform.normal(this.mesh.normal(face.vertices().getFirst().normal())).normalize();
        }
        Vec3 a = transform.position(this.mesh.position(face.vertices().get(0).position()));
        Vec3 b = transform.position(this.mesh.position(face.vertices().get(1).position()));
        Vec3 c = transform.position(this.mesh.position(face.vertices().get(2).position()));
        return b.subtract(a).cross(c.subtract(a)).normalize();
    }

    private static int connectionMask(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof EnergyCableBlock)) {
            return 0;
        }
        boolean pX = state.getValue(EnergyCableBlock.EAST);
        boolean nX = state.getValue(EnergyCableBlock.WEST);
        boolean pY = state.getValue(EnergyCableBlock.UP);
        boolean nY = state.getValue(EnergyCableBlock.DOWN);
        boolean pZ = state.getValue(EnergyCableBlock.SOUTH);
        boolean nZ = state.getValue(EnergyCableBlock.NORTH);
        return (pX ? 1 : 0) | (nX ? 2 : 0) | (pY ? 4 : 0) | (nY ? 8 : 0) | (pZ ? 16 : 0) | (nZ ? 32 : 0);
    }

    private static Set<String> partsFor(int mask) {
        boolean pX = (mask & 1) != 0;
        boolean nX = (mask & 2) != 0;
        boolean pY = (mask & 4) != 0;
        boolean nY = (mask & 8) != 0;
        boolean pZ = (mask & 16) != 0;
        boolean nZ = (mask & 32) != 0;

        Set<String> parts = new HashSet<>();
        if (pX && nX && !pY && !nY && !pZ && !nZ) {
            parts.add("CX");
        } else if (!pX && !nX && pY && nY && !pZ && !nZ) {
            parts.add("CY");
        } else if (!pX && !nX && !pY && !nY && pZ && nZ) {
            parts.add("CZ");
        } else {
            parts.add("Core");
            if (pX) {
                parts.add("posX");
            }
            if (nX) {
                parts.add("negX");
            }
            if (pY) {
                parts.add("posY");
            }
            if (nY) {
                parts.add("negY");
            }
            if (nZ) {
                parts.add("posZ");
            }
            if (pZ) {
                parts.add("negZ");
            }
        }
        return parts;
    }

    private static boolean isBlockModel(ModelResourceLocation location) {
        return BLOCK_ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant());
    }

    private static final class ObjMesh {
        private static final ObjMesh EMPTY = new ObjMesh(List.of(), List.of(), List.of(), Map.of());

        private final List<Vec3> positions;
        private final List<Uv> uvs;
        private final List<Vec3> normals;
        private final Map<String, List<ObjFace>> groups;

        private ObjMesh(List<Vec3> positions, List<Uv> uvs, List<Vec3> normals, Map<String, List<ObjFace>> groups) {
            this.positions = positions;
            this.uvs = uvs;
            this.normals = normals;
            this.groups = groups;
        }

        private static ObjMesh load(ResourceLocation location) {
            String resourcePath = "assets/" + location.getNamespace() + "/" + location.getPath();
            try (InputStream stream = RedCableNeoBakedModel.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    return EMPTY;
                }
                return parse(stream);
            } catch (IOException exception) {
                ReinhardtsHBM.LOGGER.warn("Failed to load OBJ mesh {}", location, exception);
                return EMPTY;
            }
        }

        private static ObjMesh parse(InputStream stream) throws IOException {
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
            }
            return new ObjMesh(List.copyOf(positions), List.copyOf(uvs), List.copyOf(normals), freezeGroups(groups));
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

        private static Map<String, List<ObjFace>> freezeGroups(Map<String, List<ObjFace>> groups) {
            Map<String, List<ObjFace>> frozen = new HashMap<>();
            for (Map.Entry<String, List<ObjFace>> entry : groups.entrySet()) {
                frozen.put(entry.getKey(), List.copyOf(entry.getValue()));
            }
            return Map.copyOf(frozen);
        }

        private boolean isEmpty() {
            return this.groups.values().stream().allMatch(List::isEmpty);
        }

        private List<ObjFace> faces(Set<String> names) {
            List<ObjFace> faces = new ArrayList<>();
            for (String name : names) {
                faces.addAll(this.groups.getOrDefault(name, List.of()));
            }
            return faces;
        }

        private Vec3 position(int index) {
            return this.positions.get(index);
        }

        private Uv uv(int index) {
            return this.uvs.get(index);
        }

        private Vec3 normal(int index) {
            return this.normals.get(index);
        }
    }

    private record Transform(float scale, float tx, float ty, float tz, float yaw, boolean centerToBlock) {
        private Vec3 position(Vec3 input) {
            Vec3 rotated = rotateY(input, this.yaw);
            float x = rotated.x();
            float y = rotated.y();
            float z = rotated.z();
            if (this.centerToBlock) {
                x += 0.5F;
                y += 0.5F;
                z += 0.5F;
            }
            return new Vec3(x * this.scale + this.tx, y * this.scale + this.ty, z * this.scale + this.tz);
        }

        private Vec3 normal(Vec3 input) {
            return rotateY(input, this.yaw);
        }
    }

    private record ObjFace(List<ObjVertex> vertices) {
    }

    private record ObjVertex(int position, int uv, int normal) {
    }

    private record Vec3(float x, float y, float z) {
        private Vec3 subtract(Vec3 other) {
            return new Vec3(this.x - other.x, this.y - other.y, this.z - other.z);
        }

        private Vec3 cross(Vec3 other) {
            return new Vec3(
                    this.y * other.z - this.z * other.y,
                    this.z * other.x - this.x * other.z,
                    this.x * other.y - this.y * other.x
            );
        }

        private Vec3 normalize() {
            float length = (float) Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
            if (length <= 0.0001F) {
                return new Vec3(0.0F, 1.0F, 0.0F);
            }
            return new Vec3(this.x / length, this.y / length, this.z / length);
        }
    }

    private record Uv(float u, float v) {
    }

    private static Vec3 rotateY(Vec3 input, float angle) {
        if (angle == 0.0F) {
            return input;
        }
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float x = input.x() * cos + input.z() * sin;
        float z = -input.x() * sin + input.z() * cos;
        return new Vec3(x, input.y(), z);
    }
}
