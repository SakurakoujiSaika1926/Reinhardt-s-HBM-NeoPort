package com.reinhardt.hbm.client.model;

import com.mojang.math.Transformation;
import com.reinhardt.hbm.ReinhardtsHBM;
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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
import java.util.Set;
import java.util.function.Function;

public final class DecorativeCtmBlockModel implements IDynamicBakedModel {
    private static final ModelProperty<CtmData> CTM_DATA = new ModelProperty<>(value -> value != null);
    private static final SimpleModelState IDENTITY_MODEL_STATE = new SimpleModelState(Transformation.identity());
    private static final int[] SUBMAP_OFFSETS = {4, 5, 1, 0};
    private static final ChunkRenderTypeSet CUTOUT_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());
    private static final ChunkRenderTypeSet TRANSLUCENT_RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.translucent());
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
    private static final List<Entry> ENTRIES = List.of(
            same("block_actinium"),
            same("block_advanced_alloy"),
            same("block_aluminium"),
            same("block_australium"),
            same("block_bakelite"),
            same("block_beryllium"),
            same("block_cobalt"),
            same("block_combine_steel"),
            same("block_lanthanium"),
            same("block_lead"),
            same("block_polymer"),
            same("block_red_copper"),
            same("block_rubber"),
            same("block_schrabidium"),
            same("block_steel"),
            same("block_titanium"),
            same("block_tungsten"),
            same("cmb_brick"),
            same("deco_aluminium"),
            same("deco_beryllium"),
            same("deco_lead"),
            same("deco_red_copper"),
            same("deco_tungsten"),
            entry("deco_steel", "deco_steel_ctm", "deco_steel", "deco_rusty_steel"),
            entry("deco_rusty_steel", "deco_rusty_steel_ctm", "deco_steel", "deco_rusty_steel"),
            same("fluid_duct_solid"),
            same("fluid_duct_solid_sealed"),
            translucent("glass_ash"),
            cutout("glass_boron"),
            cutout("glass_lead"),
            cutout("glass_polarized"),
            translucent("glass_polonium"),
            cutout("glass_quartz"),
            translucent("glass_trinitite"),
            translucent("glass_uranium"),
            same("hadron_coil_alloy"),
            same("hadron_coil_chlorophyte"),
            same("hadron_coil_gold"),
            same("hadron_coil_magtung"),
            same("hadron_coil_mese"),
            same("hadron_coil_neodymium"),
            same("hadron_coil_schrabidate"),
            same("hadron_coil_schrabidium"),
            same("hadron_coil_starmetal"),
            ctmVariants("icf_block", "icf_block_ct", "icf_block_port", "icf_block_port_ct", "icf_block", "icf_controller"),
            same("red_wire_sealed"),
            same("reinforced_brick"),
            cutout("reinforced_glass"),
            translucent("reinforced_laminate"),
            same("reinforced_light"),
            same("reinforced_stone")
    );

    private final BakedModel fallbackModel;
    private final Entry entry;
    private final TextureAtlasSprite baseSprite;
    private final List<BakedQuad>[][] faceQuads;

    private DecorativeCtmBlockModel(BakedModel fallbackModel, Entry entry, TextureAtlasSprite baseSprite, TextureAtlasSprite ctmSprite) {
        this.fallbackModel = fallbackModel;
        this.entry = entry;
        this.baseSprite = baseSprite;
        this.faceQuads = bakeFaceQuads(baseSprite, ctmSprite);
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textureGetter) {
        for (Entry entry : ENTRIES) {
            List<ModelResourceLocation> locations = models.keySet().stream()
                    .filter(location -> entry.id().equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                    .toList();
            if (locations.isEmpty()) {
                ReinhardtsHBM.LOGGER.warn("Unable to install decorative CTM model for {}: no baked block model was found", entry.id());
                continue;
            }
            for (ModelResourceLocation location : locations) {
                BakedModel fallback = models.get(location);
                if (fallback == null) {
                    continue;
                }
                TextureAtlasSprite baseSprite = textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, entry.baseTexture(location)));
                TextureAtlasSprite ctmSprite = textureGetter.apply(new Material(TextureAtlas.LOCATION_BLOCKS, entry.ctmTexture(location)));
                models.put(location, new DecorativeCtmBlockModel(fallback, entry, baseSprite, ctmSprite));
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData data, @Nullable RenderType renderType) {
        if (side == null) {
            return this.fallbackModel.getQuads(state, null, rand, ModelData.EMPTY, renderType);
        }
        CtmData ctmData = data.get(CTM_DATA);
        CtmFace face = ctmData == null ? CtmFace.EMPTY : ctmData.face(side);
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
        return switch (this.entry.renderLayer()) {
            case DEFAULT -> this.fallbackModel.getRenderTypes(state, rand, data);
            case CUTOUT -> CUTOUT_RENDER_TYPES;
            case TRANSLUCENT -> TRANSLUCENT_RENDER_TYPES;
        };
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

    private CtmFace computeFace(BlockAndTintGetter level, BlockPos pos, Direction face) {
        int map = 0;
        for (Dir dir : Dir.VALUES) {
            BlockPos target = pos.offset(dir.offset(face));
            if (this.entry.connects(level.getBlockState(target)) && !isObscured(level, pos, target, face)) {
                map |= 1 << dir.ordinal();
            }
        }
        return new CtmFace(map);
    }

    private boolean isObscured(BlockAndTintGetter level, BlockPos pos, BlockPos target, Direction face) {
        return this.entry.connects(level.getBlockState(target.relative(face)))
                && !this.entry.connects(level.getBlockState(pos.relative(face)));
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
                    quads.add(bakery.bakeQuad(
                            faceFrom(side, bounds),
                            faceTo(side, bounds),
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

    private static int[] createSubmapIndices(int connectionMap) {
        int[] submaps = {18, 19, 17, 16};
        for (int i = 0; i < 4; i++) {
            Dir[] dirs = SUBMAP_MAP[i];
            boolean first = connected(connectionMap, dirs[0]);
            boolean second = connected(connectionMap, dirs[1]);
            boolean diagonal = connected(connectionMap, dirs[2]);
            if (first || second) {
                submaps[i] = first && second && diagonal
                        ? SUBMAP_OFFSETS[i]
                        : SUBMAP_OFFSETS[i] + (first ? 2 : 0) + (second ? 8 : 0);
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

    private static Entry same(String path) {
        return entry(path, path + "_ctm", RenderLayer.DEFAULT, path);
    }

    private static Entry cutout(String path) {
        return entry(path, path + "_ctm", RenderLayer.CUTOUT, path);
    }

    private static Entry translucent(String path) {
        return entry(path, path + "_ctm", RenderLayer.TRANSLUCENT, path);
    }

    private static Entry ctmVariants(String path, String ctmTexture, String alternatePath,
                                     String alternateCtmTexture, String... connectsTo) {
        ResourceLocation id = ReinhardtsHBM.id(path);
        return new Entry(id, ReinhardtsHBM.id("block/" + path), ReinhardtsHBM.id("block/" + ctmTexture),
                connectedIds(connectsTo), RenderLayer.DEFAULT,
                ReinhardtsHBM.id("block/" + alternatePath), ReinhardtsHBM.id("block/" + alternateCtmTexture));
    }

    private static Entry entry(String path, String ctmTexture, String... connectsTo) {
        return entry(path, ctmTexture, RenderLayer.DEFAULT, connectsTo);
    }

    private static Entry entry(String path, String ctmTexture, RenderLayer renderLayer, String... connectsTo) {
        ResourceLocation id = ReinhardtsHBM.id(path);
        return new Entry(id, ReinhardtsHBM.id("block/" + path), ReinhardtsHBM.id("block/" + ctmTexture),
                connectedIds(connectsTo), renderLayer, null, null);
    }

    private static Set<ResourceLocation> connectedIds(String... connectsTo) {
        return java.util.Arrays.stream(connectsTo)
                .map(ReinhardtsHBM::id)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private record Entry(ResourceLocation id, ResourceLocation baseTexture, ResourceLocation ctmTexture,
                         Set<ResourceLocation> connectsTo, RenderLayer renderLayer,
                         @Nullable ResourceLocation alternateBaseTexture, @Nullable ResourceLocation alternateCtmTexture) {
        private ResourceLocation baseTexture(ModelResourceLocation location) {
            return alternateBaseTexture != null && location.variant().contains("variant=1")
                    ? alternateBaseTexture : baseTexture;
        }

        private ResourceLocation ctmTexture(ModelResourceLocation location) {
            return alternateCtmTexture != null && location.variant().contains("variant=1")
                    ? alternateCtmTexture : ctmTexture;
        }

        private boolean connects(BlockState state) {
            return this.connectsTo.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        }
    }

    private enum RenderLayer {
        DEFAULT,
        CUTOUT,
        TRANSLUCENT
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
                case EAST, WEST -> facing;
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
                case EAST, WEST -> facing;
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
