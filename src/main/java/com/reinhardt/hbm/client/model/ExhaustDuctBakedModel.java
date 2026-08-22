package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.FluidDuctBlock;
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

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

public final class ExhaustDuctBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation BLOCK_ID = ReinhardtsHBM.id("fluid_duct_exhaust");
    private static final Set<ResourceLocation> IDS = Set.of(BLOCK_ID);
    private static final Set<ResourceLocation> BLOCK_MODEL_IDS = Set.of(
            ReinhardtsHBM.id("block/fluid_duct_exhaust"),
            ReinhardtsHBM.id("block/fluid_duct_exhaust_center"),
            ReinhardtsHBM.id("block/fluid_duct_exhaust_arm")
    );
    private static final ChunkRenderTypeSet CUTOUT_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final float LOWER = 2.0F;
    private static final float UPPER = 14.0F;
    private static final float J_LOWER = 1.0F;
    private static final float J_UPPER = 15.0F;
    private static final FaceRotations NO_ROTATION = new FaceRotations(Map.of());
    private static final FaceRotations INVENTORY_ROTATION = new FaceRotations(Map.of(
            Direction.WEST, 1,
            Direction.EAST, 2
    ));
    private static final FaceRotations STRAIGHT_X_ROTATION = new FaceRotations(Map.of(
            Direction.UP, 1,
            Direction.DOWN, 1,
            Direction.NORTH, 2,
            Direction.SOUTH, 1
    ));
    private static final FaceRotations STRAIGHT_Z_ROTATION = new FaceRotations(Map.of(
            Direction.WEST, 1,
            Direction.EAST, 2
    ));
    private static final FaceRotations CURVE_HORIZONTAL_ROTATION = new FaceRotations(Map.of(
            Direction.NORTH, 2,
            Direction.SOUTH, 1,
            Direction.EAST, 2,
            Direction.WEST, 1
    ));
    private static final FaceRotations CURVE_VERTICAL_X_ROTATION = new FaceRotations(Map.of(
            Direction.UP, 1,
            Direction.DOWN, 1
    ));

    private final BakedModel fallbackModel;
    private final TextureAtlasSprite straight;
    private final TextureAtlasSprite end;
    private final TextureAtlasSprite curveTl;
    private final TextureAtlasSprite curveTr;
    private final TextureAtlasSprite curveBl;
    private final TextureAtlasSprite curveBr;
    private final TextureAtlasSprite[] junctions;
    private final boolean forBlock;
    private final List<BakedQuad>[] blockQuads;
    private final List<BakedQuad> itemQuads;

    @SuppressWarnings("unchecked")
    private ExhaustDuctBakedModel(BakedModel fallbackModel, Sprites sprites, boolean forBlock) {
        this.fallbackModel = fallbackModel;
        this.straight = sprites.straight();
        this.end = sprites.end();
        this.curveTl = sprites.curveTl();
        this.curveTr = sprites.curveTr();
        this.curveBl = sprites.curveBl();
        this.curveBr = sprites.curveBr();
        this.junctions = sprites.junctions();
        this.forBlock = forBlock;
        if (forBlock) {
            this.blockQuads = precomputeBlockQuads();
            this.itemQuads = List.of();
        } else {
            this.blockQuads = new List[0];
            this.itemQuads = bakeItemQuads();
        }
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textureGetter) {
        List<ModelResourceLocation> blockLocations = models.keySet().stream()
                .filter(ExhaustDuctBakedModel::isBlockModel)
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(BLOCK_ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel blockFallback = blockLocations.stream()
                .map(models::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(models.get(itemLocation));
        if (blockFallback == null) {
            ReinhardtsHBM.LOGGER.warn("Unable to install exhaust duct box model: no baked model was found");
            return;
        }

        Sprites sprites = Sprites.load(textureGetter);
        ExhaustDuctBakedModel blockModel = new ExhaustDuctBakedModel(blockFallback, sprites, true);
        for (ModelResourceLocation location : blockLocations) {
            models.put(location, blockModel);
        }
        if (models.containsKey(itemLocation)) {
            models.put(itemLocation, new ExhaustDuctBakedModel(models.getOrDefault(itemLocation, blockFallback), sprites, false));
        }
        ReinhardtsHBM.LOGGER.info("Installed 1.7.10 exhaust boxduct model for {} baked block variants", blockLocations.size());
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        if (side != null) {
            return List.of();
        }
        if (!this.forBlock) {
            return this.itemQuads;
        }
        return this.blockQuads[connectionMask(state)];
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return CUTOUT_RENDER_TYPES;
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return this.straight;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.straight;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return this.fallbackModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
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
            quads[mask] = bakeWorldQuads(mask);
        }
        return quads;
    }

    private List<BakedQuad> bakeItemQuads() {
        List<BakedQuad> quads = new ArrayList<>();
        bakeBox(quads, LOWER, LOWER, 0.0F, UPPER, UPPER, 16.0F, itemSprites());
        return List.copyOf(quads);
    }

    private List<BakedQuad> bakeWorldQuads(int mask) {
        boolean pX = (mask & 32) != 0;
        boolean nX = (mask & 16) != 0;
        boolean pY = (mask & 8) != 0;
        boolean nY = (mask & 4) != 0;
        boolean pZ = (mask & 2) != 0;
        boolean nZ = (mask & 1) != 0;
        int count = count(pX, nX, pY, nY, pZ, nZ);

        List<BakedQuad> quads = new ArrayList<>();
        if (mask == 0) {
            bakeBox(quads, J_LOWER, J_LOWER, J_LOWER, J_UPPER, J_UPPER, J_UPPER, junctionSprites(0));
        } else if ((mask & 0b001111) == 0) {
            bakeBox(quads, 0.0F, LOWER, LOWER, 16.0F, UPPER, UPPER, straightSprites(Direction.Axis.X));
        } else if ((mask & 0b111100) == 0) {
            bakeBox(quads, LOWER, LOWER, 0.0F, UPPER, UPPER, 16.0F, straightSprites(Direction.Axis.Z));
        } else if ((mask & 0b110011) == 0) {
            bakeBox(quads, LOWER, 0.0F, LOWER, UPPER, 16.0F, UPPER, straightSprites(Direction.Axis.Y));
        } else if (count == 2) {
            FaceSprites sprites = curveSprites(pX, nX, pY, nY, pZ, nZ);
            bakeBox(quads, LOWER, LOWER, LOWER, UPPER, UPPER, UPPER, sprites);
            if (nY) bakeBox(quads, LOWER, 0.0F, LOWER, UPPER, LOWER, UPPER, sprites);
            if (pY) bakeBox(quads, LOWER, UPPER, LOWER, UPPER, 16.0F, UPPER, sprites);
            if (nX) bakeBox(quads, 0.0F, LOWER, LOWER, LOWER, UPPER, UPPER, sprites);
            if (pX) bakeBox(quads, UPPER, LOWER, LOWER, 16.0F, UPPER, UPPER, sprites);
            if (nZ) bakeBox(quads, LOWER, LOWER, 0.0F, UPPER, UPPER, LOWER, sprites);
            if (pZ) bakeBox(quads, LOWER, LOWER, UPPER, UPPER, UPPER, 16.0F, sprites);
        } else {
            FaceSprites sprites = junctionSprites(0);
            bakeBox(quads, J_LOWER, J_LOWER, J_LOWER, J_UPPER, J_UPPER, J_UPPER, sprites);
            if (nY) bakeBox(quads, LOWER, 0.0F, LOWER, UPPER, J_LOWER, UPPER, sprites);
            if (pY) bakeBox(quads, LOWER, J_UPPER, LOWER, UPPER, 16.0F, UPPER, sprites);
            if (nX) bakeBox(quads, 0.0F, LOWER, LOWER, J_LOWER, UPPER, UPPER, sprites);
            if (pX) bakeBox(quads, J_UPPER, LOWER, LOWER, 16.0F, UPPER, UPPER, sprites);
            if (nZ) bakeBox(quads, LOWER, LOWER, 0.0F, UPPER, UPPER, J_LOWER, sprites);
            if (pZ) bakeBox(quads, LOWER, LOWER, J_UPPER, UPPER, UPPER, 16.0F, sprites);
        }
        return List.copyOf(quads);
    }

    private FaceSprites itemSprites() {
        return new FaceSprites(Map.of(
                Direction.UP, this.straight,
                Direction.DOWN, this.straight,
                Direction.EAST, this.straight,
                Direction.WEST, this.straight,
                Direction.NORTH, this.end,
                Direction.SOUTH, this.end
        ), INVENTORY_ROTATION);
    }

    private FaceSprites straightSprites(Direction.Axis axis) {
        EnumMap<Direction, TextureAtlasSprite> sprites = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            sprites.put(direction, direction.getAxis() == axis ? this.end : this.straight);
        }
        FaceRotations rotations = switch (axis) {
            case X -> STRAIGHT_X_ROTATION;
            case Z -> STRAIGHT_Z_ROTATION;
            case Y -> NO_ROTATION;
        };
        return new FaceSprites(Map.copyOf(sprites), rotations);
    }

    private FaceSprites junctionSprites(int index) {
        TextureAtlasSprite sprite = this.junctions[Math.max(0, Math.min(index, this.junctions.length - 1))];
        EnumMap<Direction, TextureAtlasSprite> sprites = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            sprites.put(direction, sprite);
        }
        return new FaceSprites(Map.copyOf(sprites), NO_ROTATION);
    }

    private FaceSprites curveSprites(boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        EnumMap<Direction, TextureAtlasSprite> sprites = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            sprites.put(direction, curveSpriteFor(direction, pX, nX, pY, nY, pZ, nZ));
        }
        FaceRotations rotations = !nY && !pY
                ? CURVE_HORIZONTAL_ROTATION
                : ((pX || nX) ? CURVE_VERTICAL_X_ROTATION : NO_ROTATION);
        return new FaceSprites(Map.copyOf(sprites), rotations);
    }

    private TextureAtlasSprite curveSpriteFor(Direction side, boolean pX, boolean nX, boolean pY, boolean nY, boolean pZ, boolean nZ) {
        if (side == Direction.DOWN && nY || side == Direction.UP && pY
                || side == Direction.NORTH && nZ || side == Direction.SOUTH && pZ
                || side == Direction.WEST && nX || side == Direction.EAST && pX) {
            return this.end;
        }
        if (side == Direction.UP && nY || side == Direction.DOWN && pY
                || side == Direction.SOUTH && nZ || side == Direction.NORTH && pZ
                || side == Direction.EAST && nX || side == Direction.WEST && pX) {
            return this.straight;
        }
        if (nY && pZ) return side == Direction.WEST ? this.curveBr : this.curveBl;
        if (nY && nZ) return side == Direction.EAST ? this.curveBr : this.curveBl;
        if (nY && pX) return side == Direction.SOUTH ? this.curveBr : this.curveBl;
        if (nY && nX) return side == Direction.NORTH ? this.curveBr : this.curveBl;
        if (pY && pZ) return side == Direction.WEST ? this.curveTr : this.curveTl;
        if (pY && nZ) return side == Direction.EAST ? this.curveTr : this.curveTl;
        if (pY && pX) return side == Direction.SOUTH ? this.curveTr : this.curveTl;
        if (pY && nX) return side == Direction.NORTH ? this.curveTr : this.curveTl;
        if (pX && nZ) return this.curveTr;
        if (pX && pZ) return this.curveBr;
        if (nX && nZ) return this.curveTl;
        if (nX && pZ) return this.curveBl;
        return this.junctions[0];
    }

    private void bakeBox(List<BakedQuad> quads, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, FaceSprites sprites) {
        if (minX >= maxX || minY >= maxY || minZ >= maxZ) {
            return;
        }
        for (Direction side : Direction.values()) {
            quads.add(bakeFace(side, minX / 16.0F, minY / 16.0F, minZ / 16.0F, maxX / 16.0F, maxY / 16.0F, maxZ / 16.0F,
                    sprites.sprite(side), sprites.rotation(side)));
        }
    }

    private static BakedQuad bakeFace(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ,
                                      TextureAtlasSprite sprite, int oldRotation) {
        Vertex[] vertices = verticesFor(face, minX, minY, minZ, maxX, maxY, maxZ, uvFor(face, minX, minY, minZ, maxX, maxY, maxZ, oldRotation));
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(-1);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(face);
        for (Vertex vertex : vertices) {
            baker.addVertex(vertex.x(), vertex.y(), vertex.z());
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(vertex.u() / 16.0F), sprite.getV(vertex.v() / 16.0F));
            baker.setLight(0);
            baker.setNormal(face.getStepX(), face.getStepY(), face.getStepZ());
        }
        return baker.bakeQuad();
    }

    private static Vertex[] verticesFor(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, FaceUv uv) {
        return switch (face) {
            case DOWN -> new Vertex[]{
                    new Vertex(minX, minY, maxZ, uv.u0(), uv.v0()),
                    new Vertex(minX, minY, minZ, uv.u1(), uv.v1()),
                    new Vertex(maxX, minY, minZ, uv.u2(), uv.v2()),
                    new Vertex(maxX, minY, maxZ, uv.u3(), uv.v3())
            };
            case UP -> new Vertex[]{
                    new Vertex(maxX, maxY, maxZ, uv.u0(), uv.v0()),
                    new Vertex(maxX, maxY, minZ, uv.u1(), uv.v1()),
                    new Vertex(minX, maxY, minZ, uv.u2(), uv.v2()),
                    new Vertex(minX, maxY, maxZ, uv.u3(), uv.v3())
            };
            case NORTH -> new Vertex[]{
                    new Vertex(minX, maxY, minZ, uv.u0(), uv.v0()),
                    new Vertex(maxX, maxY, minZ, uv.u1(), uv.v1()),
                    new Vertex(maxX, minY, minZ, uv.u2(), uv.v2()),
                    new Vertex(minX, minY, minZ, uv.u3(), uv.v3())
            };
            case SOUTH -> new Vertex[]{
                    new Vertex(minX, maxY, maxZ, uv.u0(), uv.v0()),
                    new Vertex(minX, minY, maxZ, uv.u1(), uv.v1()),
                    new Vertex(maxX, minY, maxZ, uv.u2(), uv.v2()),
                    new Vertex(maxX, maxY, maxZ, uv.u3(), uv.v3())
            };
            case WEST -> new Vertex[]{
                    new Vertex(minX, maxY, maxZ, uv.u0(), uv.v0()),
                    new Vertex(minX, maxY, minZ, uv.u1(), uv.v1()),
                    new Vertex(minX, minY, minZ, uv.u2(), uv.v2()),
                    new Vertex(minX, minY, maxZ, uv.u3(), uv.v3())
            };
            case EAST -> new Vertex[]{
                    new Vertex(maxX, minY, maxZ, uv.u0(), uv.v0()),
                    new Vertex(maxX, minY, minZ, uv.u1(), uv.v1()),
                    new Vertex(maxX, maxY, minZ, uv.u2(), uv.v2()),
                    new Vertex(maxX, maxY, maxZ, uv.u3(), uv.v3())
            };
        };
    }

    private static FaceUv uvFor(Direction face, float minX, float minY, float minZ, float maxX, float maxY, float maxZ, int oldRotation) {
        return switch (face) {
            case DOWN -> yFaceUv(minX, minZ, maxX, maxZ, oldRotation);
            case UP -> yFaceUv(minX, minZ, maxX, maxZ, oldRotation);
            case NORTH -> zNegUv(minX, minY, maxX, maxY, oldRotation);
            case SOUTH -> zPosUv(minX, minY, maxX, maxY, oldRotation);
            case WEST -> xNegUv(minZ, minY, maxZ, maxY, oldRotation);
            case EAST -> xPosUv(minZ, minY, maxZ, maxY, oldRotation);
        };
    }

    private static FaceUv yFaceUv(float minX, float minZ, float maxX, float maxZ, int oldRotation) {
        float minU = minX * 16.0F;
        float maxU = maxX * 16.0F;
        float minV = minZ * 16.0F;
        float maxV = maxZ * 16.0F;
        FaceUv uv = FaceUv.of(minU, maxV, minU, minV, maxU, minV, maxU, maxV);
        return rotateUv(uv, oldRotation);
    }

    private static FaceUv zNegUv(float minX, float minY, float maxX, float maxY, int oldRotation) {
        FaceUv uv = FaceUv.of(
                16.0F - minX * 16.0F, 16.0F - maxY * 16.0F,
                16.0F - maxX * 16.0F, 16.0F - maxY * 16.0F,
                16.0F - maxX * 16.0F, 16.0F - minY * 16.0F,
                16.0F - minX * 16.0F, 16.0F - minY * 16.0F);
        return rotateUv(uv, oldRotation);
    }

    private static FaceUv zPosUv(float minX, float minY, float maxX, float maxY, int oldRotation) {
        FaceUv uv = FaceUv.of(
                minX * 16.0F, 16.0F - maxY * 16.0F,
                minX * 16.0F, 16.0F - minY * 16.0F,
                maxX * 16.0F, 16.0F - minY * 16.0F,
                maxX * 16.0F, 16.0F - maxY * 16.0F);
        return rotateUv(uv, oldRotation);
    }

    private static FaceUv xNegUv(float minZ, float minY, float maxZ, float maxY, int oldRotation) {
        FaceUv uv = FaceUv.of(
                maxZ * 16.0F, 16.0F - maxY * 16.0F,
                minZ * 16.0F, 16.0F - maxY * 16.0F,
                minZ * 16.0F, 16.0F - minY * 16.0F,
                maxZ * 16.0F, 16.0F - minY * 16.0F);
        return rotateUv(uv, oldRotation);
    }

    private static FaceUv xPosUv(float minZ, float minY, float maxZ, float maxY, int oldRotation) {
        FaceUv uv = FaceUv.of(
                16.0F - maxZ * 16.0F, 16.0F - minY * 16.0F,
                16.0F - minZ * 16.0F, 16.0F - minY * 16.0F,
                16.0F - minZ * 16.0F, 16.0F - maxY * 16.0F,
                16.0F - maxZ * 16.0F, 16.0F - maxY * 16.0F);
        return rotateUv(uv, oldRotation);
    }

    private static FaceUv rotateUv(FaceUv uv, int oldRotation) {
        return switch (oldRotation) {
            case 1 -> uv.map((u, v) -> new Uv(16.0F - v, 16.0F - u));
            case 2 -> uv.map((u, v) -> new Uv(v, u));
            case 3 -> uv.map((u, v) -> new Uv(16.0F - u, 16.0F - v));
            default -> uv;
        };
    }

    private static int connectionMask(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof FluidDuctBlock)) {
            return 0;
        }
        boolean pX = state.getValue(FluidDuctBlock.EAST);
        boolean nX = state.getValue(FluidDuctBlock.WEST);
        boolean pY = state.getValue(FluidDuctBlock.UP);
        boolean nY = state.getValue(FluidDuctBlock.DOWN);
        boolean pZ = state.getValue(FluidDuctBlock.SOUTH);
        boolean nZ = state.getValue(FluidDuctBlock.NORTH);
        return (pX ? 32 : 0) | (nX ? 16 : 0) | (pY ? 8 : 0) | (nY ? 4 : 0) | (pZ ? 2 : 0) | (nZ ? 1 : 0);
    }

    private static int count(boolean... values) {
        int count = 0;
        for (boolean value : values) {
            if (value) {
                count++;
            }
        }
        return count;
    }

    private static boolean isBlockModel(ModelResourceLocation location) {
        if (ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant())) {
            return false;
        }
        return IDS.contains(location.id()) || BLOCK_MODEL_IDS.contains(location.id());
    }

    private record FaceSprites(Map<Direction, TextureAtlasSprite> sprites, FaceRotations rotations) {
        private TextureAtlasSprite sprite(Direction side) {
            return this.sprites.get(side);
        }

        private int rotation(Direction side) {
            return this.rotations.rotation(side);
        }
    }

    private record FaceRotations(Map<Direction, Integer> rotations) {
        private int rotation(Direction side) {
            return this.rotations.getOrDefault(side, 0);
        }
    }

    private record Vertex(float x, float y, float z, float u, float v) {
    }

    private record Uv(float u, float v) {
    }

    @FunctionalInterface
    private interface UvTransform {
        Uv apply(float u, float v);
    }

    private record FaceUv(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
        private static FaceUv of(float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3) {
            return new FaceUv(u0, v0, u1, v1, u2, v2, u3, v3);
        }

        private FaceUv map(UvTransform transform) {
            Uv uv0 = transform.apply(this.u0, this.v0);
            Uv uv1 = transform.apply(this.u1, this.v1);
            Uv uv2 = transform.apply(this.u2, this.v2);
            Uv uv3 = transform.apply(this.u3, this.v3);
            return new FaceUv(uv0.u(), uv0.v(), uv1.u(), uv1.v(), uv2.u(), uv2.v(), uv3.u(), uv3.v());
        }
    }

    private record Sprites(TextureAtlasSprite straight, TextureAtlasSprite end, TextureAtlasSprite curveTl,
                           TextureAtlasSprite curveTr, TextureAtlasSprite curveBl, TextureAtlasSprite curveBr,
                           TextureAtlasSprite[] junctions) {
        private static Sprites load(Function<Material, TextureAtlasSprite> getter) {
            TextureAtlasSprite[] junctions = new TextureAtlasSprite[5];
            for (int index = 0; index < junctions.length; index++) {
                junctions[index] = sprite(getter, "boxduct_exhaust_junction_" + index);
            }
            return new Sprites(
                    sprite(getter, "boxduct_exhaust_straight"),
                    sprite(getter, "boxduct_exhaust_end"),
                    sprite(getter, "boxduct_exhaust_curve_tl"),
                    sprite(getter, "boxduct_exhaust_curve_tr"),
                    sprite(getter, "boxduct_exhaust_curve_bl"),
                    sprite(getter, "boxduct_exhaust_curve_br"),
                    junctions
            );
        }

        private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> getter, String path) {
            return getter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/" + path)));
        }
    }
}
