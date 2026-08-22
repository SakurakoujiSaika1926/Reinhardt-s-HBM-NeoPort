package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
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
import java.util.concurrent.ConcurrentHashMap;

public class FluidTankBlockEntityRenderer implements BlockEntityRenderer<FluidTankBlockEntity> {
    private static final ResourceLocation OBJ_LOCATION = ReinhardtsHBM.id("models/obj/machines/fluidtank.obj");
    private static final ResourceLocation FRAME_TEXTURE = ReinhardtsHBM.id("textures/models/machines/tank.png");
    private static final ResourceLocation NONE_TEXTURE = tankTexture("none");
    private static final ResourceLocation DANGER_TEXTURE = tankTexture("danger");
    private static final ObjMesh MESH = ObjMesh.load(OBJ_LOCATION);
    private static final List<ObjFace> FRAME_FACES = MESH.faces("Frame");
    private static final List<ObjFace> TANK_FACES = MESH.faces("Tank");
    private static final Map<ResourceLocation, Boolean> TEXTURE_EXISTS = new ConcurrentHashMap<>();

    public FluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidTankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tank.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
        renderModel(tank.tank().type(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FluidTankBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(3.0D, 4.0D, 2.0D);
    }

    static void renderModel(HbmFluidDefinition fluid, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        TextureChoice tankTexture = tankTexture(fluid);
        renderFaces(FRAME_FACES, poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(FRAME_TEXTURE)), packedLight, packedOverlay, 0xFFFFFFFF);
        renderFaces(TANK_FACES, poseStack, bufferSource.getBuffer(RenderType.entityCutoutNoCull(tankTexture.texture())), packedLight, packedOverlay, tankTexture.color());
    }

    private static void renderFaces(List<ObjFace> faces, PoseStack poseStack, VertexConsumer consumer, int packedLight, int packedOverlay, int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        PoseStack.Pose pose = poseStack.last();
        for (ObjFace face : faces) {
            if (face.vertices().size() == 3) {
                emitQuad(face, 0, 1, 2, 2, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            } else if (face.vertices().size() == 4) {
                emitQuad(face, 0, 1, 2, 3, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            } else {
                for (int i = 1; i < face.vertices().size() - 1; i++) {
                    emitQuad(face, 0, i, i + 1, i + 1, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
                }
            }
        }
    }

    private static void emitQuad(ObjFace face, int a, int b, int c, int d, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, int red, int green, int blue, int alpha) {
        emitVertex(face.vertices().get(a), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        emitVertex(face.vertices().get(b), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        emitVertex(face.vertices().get(c), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        emitVertex(face.vertices().get(d), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void emitVertex(ObjVertex vertex, PoseStack.Pose pose, VertexConsumer consumer, int packedLight, int packedOverlay, int red, int green, int blue, int alpha) {
        Vec3 position = MESH.position(vertex.position());
        Uv uv = MESH.uv(vertex.uv());
        Vec3 normal = MESH.normal(vertex.normal());
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(uv.u(), uv.v())
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    static TextureChoice tankTexture(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return new TextureChoice(NONE_TEXTURE, 0xFFFFFFFF);
        }
        if (fluid.hasTrait(HbmFluidTrait.ANTIMATTER) || isHighlyCorrosive(fluid)) {
            return new TextureChoice(DANGER_TEXTURE, 0xFFFFFFFF);
        }
        ResourceLocation texture = tankTexture(fluid.name());
        if (resourceExists(texture)) {
            return new TextureChoice(texture, 0xFFFFFFFF);
        }
        return new TextureChoice(NONE_TEXTURE, 0xFF000000 | fluid.color());
    }

    private static ResourceLocation tankTexture(String name) {
        return ReinhardtsHBM.id("textures/models/tank/tank_" + name + ".png");
    }

    private static boolean resourceExists(ResourceLocation texture) {
        return TEXTURE_EXISTS.computeIfAbsent(texture, key -> Minecraft.getInstance().getResourceManager().getResource(key).isPresent());
    }

    private static boolean isHighlyCorrosive(HbmFluidDefinition fluid) {
        String rawTraits = fluid.rawTraits();
        if (rawTraits.isBlank()) {
            return false;
        }
        for (String token : rawTraits.split("\\|")) {
            String[] parts = token.split(":");
            if (parts.length < 2 || !parts[0].equals("CORROSIVE")) {
                continue;
            }
            try {
                return Double.parseDouble(parts[1]) > 50.0D;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }

    record TextureChoice(ResourceLocation texture, int color) {
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
            try (InputStream stream = FluidTankBlockEntityRenderer.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    return EMPTY;
                }
                return parse(stream);
            } catch (IOException exception) {
                ReinhardtsHBM.LOGGER.warn("Failed to load fluid tank OBJ mesh {}", location, exception);
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

        private List<ObjFace> faces(String name) {
            return this.groups.getOrDefault(name, List.of());
        }

        private Vec3 position(int index) {
            return index >= 0 && index < this.positions.size() ? this.positions.get(index) : Vec3.ZERO;
        }

        private Uv uv(int index) {
            return index >= 0 && index < this.uvs.size() ? this.uvs.get(index) : Uv.ZERO;
        }

        private Vec3 normal(int index) {
            return index >= 0 && index < this.normals.size() ? this.normals.get(index) : new Vec3(0.0D, 1.0D, 0.0D);
        }
    }

    private record ObjFace(List<ObjVertex> vertices) {
    }

    private record ObjVertex(int position, int uv, int normal) {
    }

    private record Uv(float u, float v) {
        private static final Uv ZERO = new Uv(0.0F, 0.0F);
    }
}
