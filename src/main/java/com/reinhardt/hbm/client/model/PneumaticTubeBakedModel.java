package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PneumaticTubeBlock;
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
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * Connection-aware pneumatic tube model.
 *
 * <p>The 1.7.10 renderer drew a 6-pixel-wide center, extended only the sides
 * that were actually connected, used the straight texture for a straight run,
 * and used connector caps for the inventory item.  The old block was a tile
 * entity, but the 1.21 migration stores the visible connectivity in the six
 * block-state booleans; this model consumes those booleans directly and keeps
 * the rendering path independent from the generic item auto-fit code.</p>
 */
public final class PneumaticTubeBakedModel implements IDynamicBakedModel {
    private static final Set<ResourceLocation> BLOCK_IDS = Set.of(
            ReinhardtsHBM.id("pneumatic_tube"),
            ReinhardtsHBM.id("pneumatic_tube_paintable")
    );
    private static final ChunkRenderTypeSet CUTOUT_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final Set<Direction> ALL_FACES = EnumSet.allOf(Direction.class);
    private static final Set<Direction> X_SIDE_FACES = faces(Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH);
    private static final Set<Direction> Y_SIDE_FACES = faces(Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    private static final Set<Direction> Z_SIDE_FACES = faces(Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST);

    private final BakedModel fallbackModel;
    private final TextureAtlasSprite baseSprite;
    private final TextureAtlasSprite straightSprite;
    private final TextureAtlasSprite connectorSprite;
    private final boolean forBlock;
    private final List<BakedQuad>[] blockQuads;
    private final List<BakedQuad> itemQuads;

    @SuppressWarnings("unchecked")
    private PneumaticTubeBakedModel(BakedModel fallbackModel,
                                    TextureAtlasSprite baseSprite,
                                    TextureAtlasSprite straightSprite,
                                    TextureAtlasSprite connectorSprite,
                                    boolean forBlock) {
        this.fallbackModel = fallbackModel;
        this.baseSprite = baseSprite;
        this.straightSprite = straightSprite;
        this.connectorSprite = connectorSprite;
        this.forBlock = forBlock;
        if (forBlock) {
            this.blockQuads = new List[64];
            for (int mask = 0; mask < blockQuads.length; mask++) {
                blockQuads[mask] = bakeBlockMask(mask);
            }
            this.itemQuads = List.of();
        } else {
            this.blockQuads = new List[0];
            this.itemQuads = bakeItem();
        }
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textureGetter) {
        int blockCount = 0;
        int itemCount = 0;
        for (ResourceLocation blockId : BLOCK_IDS) {
            List<ModelResourceLocation> blockLocations = models.keySet().stream()
                    .filter(location -> blockId.equals(location.id())
                            && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                    .toList();
            ModelResourceLocation itemLocation = new ModelResourceLocation(
                    blockId, ModelResourceLocation.INVENTORY_VARIANT);
            BakedModel fallback = blockLocations.stream()
                    .map(models::get)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElseGet(() -> models.get(itemLocation));
            if (fallback == null) {
                ReinhardtsHBM.LOGGER.warn("Unable to install pneumatic tube model for {}: no baked fallback", blockId);
                continue;
            }

            boolean paintable = "pneumatic_tube_paintable".equals(blockId.getPath());
            TextureAtlasSprite base = textureGetter.apply(new Material(
                    TextureAtlas.LOCATION_BLOCKS,
                    ReinhardtsHBM.id("block/" + blockId.getPath())));
            TextureAtlasSprite straight = paintable
                    ? base
                    : textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                    ReinhardtsHBM.id("block/pneumatic_tube_straight")));
            TextureAtlasSprite connector = paintable
                    ? base
                    : textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS,
                    ReinhardtsHBM.id("block/pneumatic_tube_connector")));

            PneumaticTubeBakedModel blockModel = new PneumaticTubeBakedModel(
                    fallback, base, straight, connector, true);
            for (ModelResourceLocation location : blockLocations) {
                models.put(location, blockModel);
                blockCount++;
            }

            if (models.containsKey(itemLocation)) {
                BakedModel itemFallback = models.get(itemLocation);
                models.put(itemLocation, new PneumaticTubeBakedModel(
                        itemFallback == null ? fallback : itemFallback,
                        base, straight, connector, false));
                itemCount++;
            }
        }
        if (blockCount > 0 || itemCount > 0) {
            ReinhardtsHBM.LOGGER.info(
                    "Installed legacy pneumatic tube connection models for {} block variants and {} item models",
                    blockCount, itemCount);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData extraData, @Nullable RenderType renderType) {
        if (side != null) {
            return List.of();
        }
        if (!forBlock) {
            return itemQuads;
        }
        if (state == null || !(state.getBlock() instanceof PneumaticTubeBlock)) {
            return fallbackModel.getQuads(state, null, random, extraData, renderType);
        }
        return blockQuads[connectionMask(state)];
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return forBlock ? CUTOUT_RENDER_TYPES : fallbackModel.getRenderTypes(state, random, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return baseSprite;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return baseSprite;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return fallbackModel.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return fallbackModel.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return fallbackModel.isCustomRenderer();
    }

    @Override
    public ItemTransforms getTransforms() {
        return fallbackModel.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return fallbackModel.getOverrides();
    }

    private List<BakedQuad> bakeBlockMask(int mask) {
        if (mask == 0b110000) {
            return bakeStraight(0.0F, 0.3125F, 0.3125F,
                    1.0F, 0.6875F, 0.6875F, X_SIDE_FACES);
        }
        if (mask == 0b001100) {
            return bakeStraight(0.3125F, 0.0F, 0.3125F,
                    0.6875F, 1.0F, 0.6875F, Y_SIDE_FACES);
        }
        if (mask == 0b000011) {
            return bakeStraight(0.3125F, 0.3125F, 0.0F,
                    0.6875F, 0.6875F, 1.0F, Z_SIDE_FACES);
        }

        List<BakedQuad> quads = new ArrayList<>();
        Set<Direction> centerFaces = EnumSet.copyOf(ALL_FACES);
        if ((mask & 32) != 0) centerFaces.remove(Direction.EAST);
        if ((mask & 16) != 0) centerFaces.remove(Direction.WEST);
        if ((mask & 8) != 0) centerFaces.remove(Direction.UP);
        if ((mask & 4) != 0) centerFaces.remove(Direction.DOWN);
        if ((mask & 2) != 0) centerFaces.remove(Direction.SOUTH);
        if ((mask & 1) != 0) centerFaces.remove(Direction.NORTH);
        addCube(quads, 0.3125F, 0.3125F, 0.3125F,
                0.6875F, 0.6875F, 0.6875F, baseSprite, centerFaces);

        if ((mask & 32) != 0) {
            addCube(quads, 0.6875F, 0.3125F, 0.3125F,
                    1.0F, 0.6875F, 0.6875F, baseSprite, X_SIDE_FACES);
        }
        if ((mask & 16) != 0) {
            addCube(quads, 0.0F, 0.3125F, 0.3125F,
                    0.3125F, 0.6875F, 0.6875F, baseSprite, X_SIDE_FACES);
        }
        if ((mask & 8) != 0) {
            addCube(quads, 0.3125F, 0.6875F, 0.3125F,
                    0.6875F, 1.0F, 0.6875F, baseSprite, Y_SIDE_FACES);
        }
        if ((mask & 4) != 0) {
            addCube(quads, 0.3125F, 0.0F, 0.3125F,
                    0.6875F, 0.3125F, 0.6875F, baseSprite, Y_SIDE_FACES);
        }
        if ((mask & 2) != 0) {
            addCube(quads, 0.3125F, 0.3125F, 0.6875F,
                    0.6875F, 0.6875F, 1.0F, baseSprite, Z_SIDE_FACES);
        }
        if ((mask & 1) != 0) {
            addCube(quads, 0.3125F, 0.3125F, 0.0F,
                    0.6875F, 0.6875F, 0.3125F, baseSprite, Z_SIDE_FACES);
        }
        return List.copyOf(quads);
    }

    private List<BakedQuad> bakeStraight(float minX, float minY, float minZ,
                                         float maxX, float maxY, float maxZ,
                                         Set<Direction> visibleFaces) {
        List<BakedQuad> quads = new ArrayList<>();
        Direction.Axis axis = maxX - minX > maxY - minY
                ? (maxX - minX > maxZ - minZ ? Direction.Axis.X : Direction.Axis.Z)
                : (maxY - minY > maxZ - minZ ? Direction.Axis.Y : Direction.Axis.Z);
        addCube(quads, minX, minY, minZ, maxX, maxY, maxZ, straightSprite, visibleFaces, null, axis);
        return List.copyOf(quads);
    }

    private List<BakedQuad> bakeItem() {
        List<BakedQuad> quads = new ArrayList<>();
        float lower = 5.0F / 16.0F;
        float upper = 11.0F / 16.0F;
        addCube(quads, lower, lower, 0.0F, upper, upper, 1.0F,
                straightSprite, ALL_FACES, connectorSprite, Direction.Axis.Z);
        return List.copyOf(quads);
    }

    private static void addCube(List<BakedQuad> quads, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, TextureAtlasSprite sprite,
                                Set<Direction> visibleFaces) {
        addCube(quads, minX, minY, minZ, maxX, maxY, maxZ, sprite, visibleFaces, null);
    }

    private static void addCube(List<BakedQuad> quads, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, TextureAtlasSprite sprite,
                                Set<Direction> visibleFaces, @Nullable TextureAtlasSprite connector) {
        addCube(quads, minX, minY, minZ, maxX, maxY, maxZ, sprite, visibleFaces, connector, null);
    }

    private static void addCube(List<BakedQuad> quads, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, TextureAtlasSprite sprite,
                                Set<Direction> visibleFaces, @Nullable TextureAtlasSprite connector,
                                @Nullable Direction.Axis straightAxis) {
        for (Direction direction : visibleFaces) {
            TextureAtlasSprite faceSprite = connector != null
                    && (direction == Direction.NORTH || direction == Direction.SOUTH)
                    ? connector : sprite;
            int uvRotation = straightAxis == null ? 0 : straightUvRotation(straightAxis, direction);
            addFace(quads, minX, minY, minZ, maxX, maxY, maxZ, direction, faceSprite, uvRotation);
        }
    }

    private static void addFace(List<BakedQuad> quads, float minX, float minY, float minZ,
                                float maxX, float maxY, float maxZ, Direction direction,
                                TextureAtlasSprite sprite, int uvRotation) {
        float[][] vertices = switch (direction) {
            case DOWN -> new float[][]{
                    {minX, minY, maxZ, 0.0F, 0.0F}, {minX, minY, minZ, 0.0F, 1.0F},
                    {maxX, minY, minZ, 1.0F, 1.0F}, {maxX, minY, maxZ, 1.0F, 0.0F}};
            case UP -> new float[][]{
                    {maxX, maxY, maxZ, 0.0F, 0.0F}, {maxX, maxY, minZ, 0.0F, 1.0F},
                    {minX, maxY, minZ, 1.0F, 1.0F}, {minX, maxY, maxZ, 1.0F, 0.0F}};
            case NORTH -> new float[][]{
                    {minX, maxY, minZ, 0.0F, 0.0F}, {maxX, maxY, minZ, 1.0F, 0.0F},
                    {maxX, minY, minZ, 1.0F, 1.0F}, {minX, minY, minZ, 0.0F, 1.0F}};
            case SOUTH -> new float[][]{
                    {minX, maxY, maxZ, 0.0F, 0.0F}, {minX, minY, maxZ, 1.0F, 0.0F},
                    {maxX, minY, maxZ, 1.0F, 1.0F}, {maxX, maxY, maxZ, 0.0F, 1.0F}};
            case WEST -> new float[][]{
                    {minX, maxY, maxZ, 0.0F, 0.0F}, {minX, maxY, minZ, 1.0F, 0.0F},
                    {minX, minY, minZ, 1.0F, 1.0F}, {minX, minY, maxZ, 0.0F, 1.0F}};
            case EAST -> new float[][]{
                    {maxX, minY, maxZ, 0.0F, 0.0F}, {maxX, minY, minZ, 1.0F, 0.0F},
                    {maxX, maxY, minZ, 1.0F, 1.0F}, {maxX, maxY, maxZ, 0.0F, 1.0F}};
        };

        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(-1);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(direction);
        for (int index = 0; index < vertices.length; index++) {
            float u = vertices[index][3];
            float v = vertices[index][4];
            if (uvRotation != 0) {
                float rotatedU = u;
                float rotatedV = v;
                switch (uvRotation & 3) {
                    case 1 -> {
                        rotatedU = 1.0F - v;
                        rotatedV = u;
                    }
                    case 2 -> {
                        rotatedU = 1.0F - u;
                        rotatedV = 1.0F - v;
                    }
                    case 3 -> {
                        rotatedU = v;
                        rotatedV = 1.0F - u;
                    }
                    default -> {
                    }
                }
                u = rotatedU;
                v = rotatedV;
            }
            baker.addVertex(vertices[index][0], vertices[index][1], vertices[index][2]);
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(u), sprite.getV(v));
            baker.setLight(0);
            baker.setNormal(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        }
        quads.add(baker.bakeQuad());
    }

    private static int straightUvRotation(Direction.Axis axis, Direction face) {
        if (axis == Direction.Axis.Z) {
            return face == Direction.UP ? 2 : face == Direction.DOWN ? 1 : 0;
        }
        if (axis == Direction.Axis.Y && face.getAxis() != Direction.Axis.Y) {
            return 2;
        }
        return 0;
    }

    private static int connectionMask(BlockState state) {
        int mask = 0;
        if (state.getValue(PneumaticTubeBlock.EAST)) mask |= 32;
        if (state.getValue(PneumaticTubeBlock.WEST)) mask |= 16;
        if (state.getValue(PneumaticTubeBlock.UP)) mask |= 8;
        if (state.getValue(PneumaticTubeBlock.DOWN)) mask |= 4;
        if (state.getValue(PneumaticTubeBlock.SOUTH)) mask |= 2;
        if (state.getValue(PneumaticTubeBlock.NORTH)) mask |= 1;
        return mask;
    }

    private static Set<Direction> faces(Direction... directions) {
        EnumSet<Direction> faces = EnumSet.noneOf(Direction.class);
        java.util.Collections.addAll(faces, directions);
        return Set.copyOf(faces);
    }
}
