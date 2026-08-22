package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.CoatedEnergyCableBlock;
import com.mojang.math.Transformation;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.SimpleModelState;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

public final class CoatedCableCtmModel implements IDynamicBakedModel {
    private static final ModelProperty<CtmData> CTM_DATA = new ModelProperty<>(value -> value != null);
    private static final SimpleModelState IDENTITY_MODEL_STATE = new SimpleModelState(Transformation.identity());
    private static final int[] SUBMAP_OFFSETS = {4, 5, 1, 0};
    private static final Dir[][] SUBMAP_MAP = {
            {Dir.BOTTOM, Dir.LEFT, Dir.BOTTOM_LEFT},
            {Dir.BOTTOM, Dir.RIGHT, Dir.BOTTOM_RIGHT},
            {Dir.TOP, Dir.RIGHT, Dir.TOP_RIGHT},
            {Dir.TOP, Dir.LEFT, Dir.TOP_LEFT}
    };
    private static final Submap[] SUBMAPS = {
            new Submap(false, 0, 0, 4, 4),
            new Submap(false, 4, 0, 4, 4),
            new Submap(false, 8, 0, 4, 4),
            new Submap(false, 12, 0, 4, 4),
            new Submap(false, 0, 4, 4, 4),
            new Submap(false, 4, 4, 4, 4),
            new Submap(false, 8, 4, 4, 4),
            new Submap(false, 12, 4, 4, 4),
            new Submap(false, 0, 8, 4, 4),
            new Submap(false, 4, 8, 4, 4),
            new Submap(false, 8, 8, 4, 4),
            new Submap(false, 12, 8, 4, 4),
            new Submap(false, 0, 12, 4, 4),
            new Submap(false, 4, 12, 4, 4),
            new Submap(false, 8, 12, 4, 4),
            new Submap(false, 12, 12, 4, 4),
            new Submap(true, 0, 0, 8, 8),
            new Submap(true, 8, 0, 8, 8),
            new Submap(true, 0, 8, 8, 8),
            new Submap(true, 8, 8, 8, 8)
    };
    private static final net.minecraft.resources.ResourceLocation BLOCK_ID = ReinhardtsHBM.id("red_wire_coated");

    private final BakedModel fallbackModel;
    private final TextureAtlasSprite baseSprite;
    private final TextureAtlasSprite ctmSprite;
    private final List<BakedQuad>[][] faceQuads;

    private CoatedCableCtmModel(BakedModel fallbackModel, TextureAtlasSprite baseSprite, TextureAtlasSprite ctmSprite) {
        this.fallbackModel = fallbackModel;
        this.baseSprite = baseSprite;
        this.ctmSprite = ctmSprite;
        this.faceQuads = bakeFaceQuads(baseSprite, ctmSprite);
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textureGetter) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(CoatedCableCtmModel::isCoatedCableModel)
                .toList();
        BakedModel fallback = locations.stream()
                .map(models::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(models.get(modelLocation(0)));
        if (fallback == null) {
            ReinhardtsHBM.LOGGER.warn("Unable to install coated red copper cable CTM model: no baked block model was found");
            return;
        }

        TextureAtlasSprite baseSprite = textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/red_wire_coated")));
        TextureAtlasSprite ctmSprite = textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/red_wire_coated_ctm")));
        CoatedCableCtmModel ctmModel = new CoatedCableCtmModel(fallback, baseSprite, ctmSprite);
        for (ModelResourceLocation location : locations) {
            models.put(location, ctmModel);
        }
        for (int mask = 0; mask < 64; mask++) {
            models.put(modelLocation(mask), ctmModel);
        }
        ReinhardtsHBM.LOGGER.info("Installed coated red copper cable CTM model for {} baked variants", Math.max(locations.size(), 64));
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        if (side == null) {
            return this.fallbackModel.getQuads(state, null, rand, ModelData.EMPTY, renderType);
        }

        CtmFace face = getFace(state, side, extraData);
        return this.faceQuads[side.ordinal()][face.key()];
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        CtmFace[] faces = new CtmFace[Direction.values().length];
        for (Direction face : Direction.values()) {
            faces[face.ordinal()] = computeFace(level, pos, face);
        }
        return modelData.derive().with(CTM_DATA, new CtmData(faces)).build();
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return this.fallbackModel.getRenderTypes(state, rand, data);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return this.baseSprite;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.baseSprite;
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.fallbackModel.useAmbientOcclusion();
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
    private static List<BakedQuad>[][] bakeFaceQuads(TextureAtlasSprite baseSprite, TextureAtlasSprite ctmSprite) {
        List<BakedQuad>[][] baked = new List[Direction.values().length][256];
        FaceBakery bakery = new FaceBakery();
        for (Direction side : Direction.values()) {
            for (int map = 0; map < 256; map++) {
                int[] submaps = createSubmapIndices(map);
                List<BakedQuad> quads = new ArrayList<>(4);
                for (int quadrant = 0; quadrant < 4; quadrant++) {
                    Submap submap = SUBMAPS[submaps[quadrant]];
                    TextureAtlasSprite sprite = submap.baseTexture() ? baseSprite : ctmSprite;
                    QuadrantBounds bounds = quadrantBounds(quadrant);
                    Vector3f from = faceFrom(side, bounds);
                    Vector3f to = faceTo(side, bounds);
                    quads.add(bakery.bakeQuad(
                            from,
                            to,
                            new BlockElementFace(null, BlockElementFace.NO_TINT, "#ctm", new BlockFaceUV(submap.uv(), 0)),
                            sprite,
                            side,
                            IDENTITY_MODEL_STATE,
                            null,
                            true
                    ));
                }
                baked[side.ordinal()][map] = List.copyOf(quads);
            }
        }
        return baked;
    }

    private static CtmFace getFace(@Nullable BlockState state, Direction face, ModelData data) {
        CtmData ctmData = data.get(CTM_DATA);
        if (ctmData != null) {
            return ctmData.face(face);
        }
        return state == null ? CtmFace.EMPTY : computeFace(state, face);
    }

    private static CtmFace computeFace(BlockAndTintGetter level, BlockPos pos, Direction face) {
        int map = 0;
        for (Dir dir : Dir.VALUES) {
            BlockPos target = pos.offset(dir.offset(face));
            if (level.getBlockState(target).getBlock() instanceof CoatedEnergyCableBlock
                    && !isObscured(level, pos, target, face)) {
                map |= 1 << dir.ordinal();
            }
        }
        return new CtmFace(map);
    }

    private static CtmFace computeFace(BlockState state, Direction face) {
        int map = 0;
        for (Dir dir : Dir.VALUES) {
            Direction[] directions = dir.directions(face);
            if (directions.length == 1
                    && state.hasProperty(CoatedEnergyCableBlock.propertyFor(directions[0]))
                    && state.getValue(CoatedEnergyCableBlock.propertyFor(directions[0]))) {
                map |= 1 << dir.ordinal();
            }
        }
        return new CtmFace(map);
    }

    private static boolean isObscured(BlockAndTintGetter level, BlockPos pos, BlockPos target, Direction face) {
        BlockPos obscuringPos = target.relative(face);
        return level.getBlockState(obscuringPos).getBlock() instanceof CoatedEnergyCableBlock
                && !(level.getBlockState(pos.relative(face)).getBlock() instanceof CoatedEnergyCableBlock);
    }

    private static int[] createSubmapIndices(int connectionMap) {
        int[] submaps = {18, 19, 17, 16};
        for (int i = 0; i < 4; i++) {
            Dir[] dirs = SUBMAP_MAP[i];
            boolean first = connected(connectionMap, dirs[0]);
            boolean second = connected(connectionMap, dirs[1]);
            boolean diagonal = connected(connectionMap, dirs[2]);
            if (first || second) {
                if (first && second && diagonal) {
                    submaps[i] = SUBMAP_OFFSETS[i];
                } else {
                    submaps[i] = SUBMAP_OFFSETS[i] + (first ? 2 : 0) + (second ? 8 : 0);
                }
            }
        }
        return submaps;
    }

    private static boolean connected(int map, Dir dir) {
        return ((map >> dir.ordinal()) & 1) == 1;
    }

    private static QuadrantBounds quadrantBounds(int quadrant) {
        return switch (quadrant) {
            case 0 -> new QuadrantBounds(0.0F, 0.5F, 0.5F, 1.0F);
            case 1 -> new QuadrantBounds(0.5F, 0.5F, 1.0F, 1.0F);
            case 2 -> new QuadrantBounds(0.5F, 0.0F, 1.0F, 0.5F);
            default -> new QuadrantBounds(0.0F, 0.0F, 0.5F, 0.5F);
        };
    }

    private static Vector3f faceFrom(Direction face, QuadrantBounds bounds) {
        return switch (face) {
            case DOWN -> new Vector3f(bounds.minU() * 16.0F, 0.0F, (1.0F - bounds.maxV()) * 16.0F);
            case UP -> new Vector3f(bounds.minU() * 16.0F, 16.0F, bounds.minV() * 16.0F);
            case NORTH -> new Vector3f((1.0F - bounds.maxU()) * 16.0F, (1.0F - bounds.maxV()) * 16.0F, 0.0F);
            case SOUTH -> new Vector3f(bounds.minU() * 16.0F, (1.0F - bounds.maxV()) * 16.0F, 16.0F);
            case WEST -> new Vector3f(0.0F, (1.0F - bounds.maxV()) * 16.0F, bounds.minU() * 16.0F);
            case EAST -> new Vector3f(16.0F, (1.0F - bounds.maxV()) * 16.0F, (1.0F - bounds.maxU()) * 16.0F);
        };
    }

    private static Vector3f faceTo(Direction face, QuadrantBounds bounds) {
        return switch (face) {
            case DOWN -> new Vector3f(bounds.maxU() * 16.0F, 0.0F, (1.0F - bounds.minV()) * 16.0F);
            case UP -> new Vector3f(bounds.maxU() * 16.0F, 16.0F, bounds.maxV() * 16.0F);
            case NORTH -> new Vector3f((1.0F - bounds.minU()) * 16.0F, (1.0F - bounds.minV()) * 16.0F, 0.0F);
            case SOUTH -> new Vector3f(bounds.maxU() * 16.0F, (1.0F - bounds.minV()) * 16.0F, 16.0F);
            case WEST -> new Vector3f(0.0F, (1.0F - bounds.minV()) * 16.0F, bounds.maxU() * 16.0F);
            case EAST -> new Vector3f(16.0F, (1.0F - bounds.minV()) * 16.0F, (1.0F - bounds.minU()) * 16.0F);
        };
    }

    private static boolean isCoatedCableModel(ModelResourceLocation location) {
        return BLOCK_ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant());
    }

    private static ModelResourceLocation modelLocation(int mask) {
        return new ModelResourceLocation(BLOCK_ID, variantString(mask));
    }

    private static String variantString(int mask) {
        return "north=" + has(mask, Direction.NORTH)
                + ",east=" + has(mask, Direction.EAST)
                + ",south=" + has(mask, Direction.SOUTH)
                + ",west=" + has(mask, Direction.WEST)
                + ",up=" + has(mask, Direction.UP)
                + ",down=" + has(mask, Direction.DOWN);
    }

    private static boolean has(int mask, Direction direction) {
        return (mask & bit(direction)) != 0;
    }

    private static int bit(Direction direction) {
        return switch (direction) {
            case NORTH -> 1;
            case EAST -> 2;
            case SOUTH -> 4;
            case WEST -> 8;
            case UP -> 16;
            case DOWN -> 32;
        };
    }

    private record CtmData(CtmFace[] faces) {
        private CtmFace face(Direction face) {
            return this.faces[face.ordinal()];
        }
    }

    private record CtmFace(int key) {
        private static final CtmFace EMPTY = new CtmFace(0);
    }

    private record Submap(boolean baseTexture, float x, float y, float width, float height) {
        private float[] uv() {
            return new float[]{this.x, this.y, this.x + this.width, this.y + this.height};
        }
    }

    private record QuadrantBounds(float minU, float minV, float maxU, float maxV) {
    }

    private enum Dir {
        TOP(Direction.UP),
        TOP_RIGHT(Direction.UP, Direction.EAST),
        RIGHT(Direction.EAST),
        BOTTOM_RIGHT(Direction.DOWN, Direction.EAST),
        BOTTOM(Direction.DOWN),
        BOTTOM_LEFT(Direction.DOWN, Direction.WEST),
        LEFT(Direction.WEST),
        TOP_LEFT(Direction.UP, Direction.WEST);

        private static final Dir[] VALUES = values();
        private static final Direction NORMAL = Direction.SOUTH;

        private final Direction[] directions;

        Dir(Direction... directions) {
            this.directions = directions;
        }

        private BlockPos offset(Direction normal) {
            BlockPos offset = BlockPos.ZERO;
            for (Direction direction : directions(normal)) {
                offset = offset.relative(direction);
            }
            return offset;
        }

        private Direction[] directions(Direction normal) {
            Direction[] rotated = new Direction[this.directions.length];
            if (normal == NORMAL) {
                System.arraycopy(this.directions, 0, rotated, 0, this.directions.length);
                return rotated;
            }
            if (normal == NORMAL.getOpposite()) {
                for (int i = 0; i < this.directions.length; i++) {
                    Direction direction = this.directions[i];
                    rotated[i] = direction.getStepY() != 0 ? direction : direction.getOpposite();
                }
                return rotated;
            }

            Direction axis = normal.getStepY() == 0
                    ? (normal == rotateAround(NORMAL, Direction.UP) ? Direction.UP : Direction.DOWN)
                    : (normal == Direction.UP ? rotateCounterClockwiseY(NORMAL) : rotateClockwiseY(NORMAL));
            for (int i = 0; i < this.directions.length; i++) {
                rotated[i] = rotateAround(this.directions[i], axis);
            }
            return rotated;
        }
    }

    private static Direction rotateAround(Direction facing, Direction axisFacing) {
        Direction.Axis axis = axisFacing.getAxis();
        boolean positive = axisFacing.getAxisDirection() == Direction.AxisDirection.POSITIVE;
        if (positive) {
            return rotateClockwise(facing, axis);
        }

        if (facing.getAxis() == axis) {
            return facing;
        }
        return switch (axis) {
            case X -> switch (facing) {
                case NORTH -> Direction.UP;
                case DOWN -> Direction.NORTH;
                case SOUTH -> Direction.DOWN;
                case UP -> Direction.SOUTH;
                default -> facing;
            };
            case Y -> rotateCounterClockwiseY(facing);
            case Z -> switch (facing) {
                case EAST -> Direction.EAST;
                case WEST -> Direction.WEST;
                case UP -> Direction.DOWN;
                case DOWN -> Direction.UP;
                default -> facing;
            };
        };
    }

    private static Direction rotateClockwise(Direction facing, Direction.Axis axis) {
        if (facing.getAxis() == axis) {
            return facing;
        }
        return switch (axis) {
            case X -> switch (facing) {
                case NORTH -> Direction.DOWN;
                case DOWN -> Direction.SOUTH;
                case SOUTH -> Direction.UP;
                case UP -> Direction.NORTH;
                default -> facing;
            };
            case Y -> rotateClockwiseY(facing);
            case Z -> switch (facing) {
                case EAST -> Direction.EAST;
                case WEST -> Direction.WEST;
                case UP -> Direction.DOWN;
                case DOWN -> Direction.UP;
                default -> facing;
            };
        };
    }

    private static Direction rotateClockwiseY(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case EAST -> Direction.SOUTH;
            case SOUTH -> Direction.WEST;
            case WEST -> Direction.NORTH;
            default -> facing;
        };
    }

    private static Direction rotateCounterClockwiseY(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case WEST -> Direction.SOUTH;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            default -> facing;
        };
    }
}
