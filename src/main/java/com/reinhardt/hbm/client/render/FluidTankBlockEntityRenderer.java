package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.FluidTankBlock;
import com.reinhardt.hbm.blockentity.FluidTankBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
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

public class FluidTankBlockEntityRenderer implements LongRangeBlockEntityRenderer<FluidTankBlockEntity> {
    private static final ResourceLocation OBJ_LOCATION = ReinhardtsHBM.id("models/obj/machines/fluidtank.obj");
    private static final ResourceLocation DAMAGED_OBJ_LOCATION = ReinhardtsHBM.id("models/obj/machines/fluidtank_exploded.obj");
    private static final ResourceLocation FRAME_TEXTURE = ReinhardtsHBM.id("textures/models/machines/tank.png");
    private static final ResourceLocation INNER_TEXTURE = ReinhardtsHBM.id("textures/models/tank/tank_inner.png");
    private static final ResourceLocation NONE_TEXTURE = tankTexture("none");
    private static final ResourceLocation DANGER_TEXTURE = tankTexture("danger");
    private static final TextureChoice NONE_TEXTURE_CHOICE = new TextureChoice(NONE_TEXTURE, 0xFFFFFFFF);
    private static final TextureChoice DANGER_TEXTURE_CHOICE = new TextureChoice(DANGER_TEXTURE, 0xFFFFFFFF);
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ObjMesh MESH = ObjMesh.load(OBJ_LOCATION);
    private static final ObjMesh DAMAGED_MESH = ObjMesh.load(DAMAGED_OBJ_LOCATION);
    private static final List<ObjFace> FRAME_FACES = MESH.faces("Frame");
    private static final List<ObjFace> TANK_FACES = MESH.faces("Tank");
    private static final List<ObjFace> DAMAGED_FRAME_FACES = DAMAGED_MESH.faces("Frame");
    private static final List<ObjFace> DAMAGED_INNER_FACES = DAMAGED_MESH.faces("TankInner");
    private static final List<ObjFace> DAMAGED_TANK_FACES = DAMAGED_MESH.faces("Tank");
    private static final Map<ResourceLocation, Boolean> TEXTURE_EXISTS = new ConcurrentHashMap<>();
    private static final Map<HbmFluidDefinition, TextureChoice> FLUID_TEXTURES = new ConcurrentHashMap<>();
    private static final Map<TextureChoice, TankGeometry[]> TANK_GEOMETRIES = new ConcurrentHashMap<>();
    private static final AABB[] LOCAL_BOUNDS = buildFluidTankBounds(false);
    private static final AABB[] DAMAGED_LOCAL_BOUNDS = buildFluidTankBounds(true);

    public FluidTankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FluidTankBlockEntity tank, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = tank.getBlockState();
        Direction facing = fluidTankFacing(state);

        poseStack.pushPose();
        applyFluidTankTransform(poseStack, facing);
        renderModel(tank.tank().type(), tank.isDamaged(), poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FluidTankBlockEntity blockEntity) {
        Direction facing = fluidTankFacing(blockEntity.getBlockState());
        // Face-used Frame+Tank union from the same custom-loaded OBJ used by renderModel.
        AABB[] bounds = blockEntity.isDamaged() ? DAMAGED_LOCAL_BOUNDS : LOCAL_BOUNDS;
        return bounds[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    static void renderModel(HbmFluidDefinition fluid, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderModel(fluid, false, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderModel(HbmFluidDefinition fluid, boolean damaged, PoseStack poseStack, MultiBufferSource bufferSource,
                            int packedLight, int packedOverlay) {
        TextureChoice tankTexture = tankTexture(fluid);
        TankGeometry[] variants = TANK_GEOMETRIES.computeIfAbsent(
                tankTexture, FluidTankBlockEntityRenderer::buildTankGeometries);
        STATIC_MESHES.renderDirect(variants[damaged ? 1 : 0], poseStack, packedLight, packedOverlay);
    }

    private static TankGeometry[] buildTankGeometries(TextureChoice tankTexture) {
        TankGeometry normal = new TankGeometry(List.of(
                new TankLayer(RenderType.entityCutoutNoCull(FRAME_TEXTURE), MESH, FRAME_FACES, 0xFFFFFFFF),
                new TankLayer(RenderType.entityCutoutNoCull(tankTexture.texture()), MESH, TANK_FACES,
                        tankTexture.color())
        ));
        TankGeometry damaged = new TankGeometry(List.of(
                new TankLayer(RenderType.entityCutoutNoCull(FRAME_TEXTURE), DAMAGED_MESH,
                        DAMAGED_FRAME_FACES, 0xFFFFFFFF),
                new TankLayer(RenderType.entityCutoutNoCull(INNER_TEXTURE), DAMAGED_MESH,
                        DAMAGED_INNER_FACES, 0xFFFFFFFF),
                new TankLayer(RenderType.entityCutoutNoCull(tankTexture.texture()), DAMAGED_MESH,
                        DAMAGED_TANK_FACES, tankTexture.color())
        ));
        return new TankGeometry[] {normal, damaged};
    }

    private static void renderFaces(ObjMesh mesh, List<ObjFace> faces, PoseStack poseStack, VertexConsumer consumer,
                                    int packedLight, int packedOverlay, int argb) {
        int alpha = (argb >>> 24) & 0xFF;
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        PoseStack.Pose pose = poseStack.last();
        for (ObjFace face : faces) {
            if (face.vertices().size() == 3) {
                emitQuad(mesh, face, 0, 1, 2, 2, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            } else if (face.vertices().size() == 4) {
                emitQuad(mesh, face, 0, 1, 2, 3, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
            } else {
                for (int i = 1; i < face.vertices().size() - 1; i++) {
                    emitQuad(mesh, face, 0, i, i + 1, i + 1, pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
                }
            }
        }
    }

    private static void emitQuad(ObjMesh mesh, ObjFace face, int a, int b, int c, int d, PoseStack.Pose pose,
                                 VertexConsumer consumer, int packedLight, int packedOverlay,
                                 int red, int green, int blue, int alpha) {
        emitVertex(mesh, face.vertices().get(a), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        emitVertex(mesh, face.vertices().get(b), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        emitVertex(mesh, face.vertices().get(c), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
        emitVertex(mesh, face.vertices().get(d), pose, consumer, packedLight, packedOverlay, red, green, blue, alpha);
    }

    private static void emitVertex(ObjMesh mesh, ObjVertex vertex, PoseStack.Pose pose, VertexConsumer consumer,
                                   int packedLight, int packedOverlay, int red, int green, int blue, int alpha) {
        Vec3 position = mesh.position(vertex.position());
        Uv uv = mesh.uv(vertex.uv());
        Vec3 normal = mesh.normal(vertex.normal());
        consumer.addVertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .setColor(red, green, blue, alpha)
                .setUv(uv.u(), uv.v())
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    static TextureChoice tankTexture(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return NONE_TEXTURE_CHOICE;
        }
        return FLUID_TEXTURES.computeIfAbsent(fluid, FluidTankBlockEntityRenderer::resolveTankTexture);
    }

    private static TextureChoice resolveTankTexture(HbmFluidDefinition fluid) {
        if (fluid.hasTrait(HbmFluidTrait.ANTIMATTER) || isHighlyCorrosive(fluid)) {
            return DANGER_TEXTURE_CHOICE;
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

    private static AABB[] buildFluidTankBounds(boolean damaged) {
        AABB[] bounds = new AABB[Direction.values().length];
        float minX = -2.75F;
        float minY = damaged ? -0.011965F : 0.0F;
        float minZ = damaged ? -1.67975F : -1.5F;
        float maxX = damaged ? 2.911498F : 2.75F;
        float maxY = 3.28125F;
        float maxZ = damaged ? 1.628433F : 1.5F;
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyFluidTankTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    minX, minY, minZ, maxX, maxY, maxZ);
        }
        return bounds;
    }

    private static void applyFluidTankTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            // RenderFluidTank metadata 2/3/4/5: 180/0/270/90 degrees.
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Fluid tank facing must be horizontal: " + facing);
        }
    }

    private static Direction fluidTankFacing(BlockState state) {
        if (!state.hasProperty(FluidTankBlock.FACING)) {
            throw new IllegalStateException("Fluid tank state is missing its owned facing property: " + state);
        }
        return state.getValue(FluidTankBlock.FACING);
    }

    record TextureChoice(ResourceLocation texture, int color) {
    }

    private record TankGeometry(List<TankLayer> layers)
            implements StaticMachineVboCache.DirectGeometry {
    }

    private record TankLayer(RenderType renderType, ObjMesh mesh, List<ObjFace> faces, int argb)
            implements StaticMachineVboCache.DirectLayer {
        @Override
        public void emit(PoseStack poseStack, VertexConsumer consumer,
                         int packedLight, int packedOverlay) {
            renderFaces(this.mesh, this.faces, poseStack, consumer,
                    packedLight, packedOverlay, this.argb);
        }
    }

    private static final class ObjMesh {
        private static final ObjMesh EMPTY = new ObjMesh(List.of(), List.of(), List.of(), Map.of());
        private static final Vec3 DEFAULT_NORMAL = new Vec3(0.0D, 1.0D, 0.0D);

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
            return index >= 0 && index < this.normals.size() ? this.normals.get(index) : DEFAULT_NORMAL;
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
