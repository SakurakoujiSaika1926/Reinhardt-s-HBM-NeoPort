package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.EnergyCableBlock;
import com.reinhardt.hbm.block.PowerCableBoxBlock;
import com.reinhardt.hbm.item.PowerCableBoxBlockItem;
import net.minecraft.client.multiplayer.ClientLevel;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Modern multipart renderer for 1.7.10 PowerCableBox. */
public final class PowerCableBoxBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation ID = ReinhardtsHBM.id("red_cable_box");
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel fallback;
    private final Textures textures;
    private final boolean item;
    private final List<BakedQuad>[][] worldQuads;
    private final List<BakedQuad> itemQuads;
    @Nullable
    private ItemOverrides itemOverrides;

    @SuppressWarnings("unchecked")
    private PowerCableBoxBakedModel(BakedModel fallback, Textures textures, boolean item, int itemSize) {
        this.fallback = fallback;
        this.textures = textures;
        this.item = item;
        this.worldQuads = item ? new List[0][0] : bakeWorld(textures);
        this.itemQuads = item ? bakeItem(textures, itemSize) : List.of();
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textureGetter) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(location -> ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel fallback = locations.stream().map(models::get).filter(Objects::nonNull).findFirst().orElse(models.get(itemLocation));
        if (fallback == null) {
            return;
        }
        Textures textures = Textures.load(textureGetter);
        PowerCableBoxBakedModel world = new PowerCableBoxBakedModel(fallback, textures, false, 0);
        for (ModelResourceLocation location : locations) {
            models.put(location, world);
        }
        if (models.containsKey(itemLocation)) {
            BakedModel itemFallback = models.get(itemLocation);
            PowerCableBoxBakedModel[] variants = new PowerCableBoxBakedModel[5];
            for (int size = 0; size < variants.length; size++) {
                variants[size] = new PowerCableBoxBakedModel(itemFallback, textures, true, size);
            }
            ItemOverrides overrides = new CableBoxItemOverrides(variants);
            for (PowerCableBoxBakedModel variant : variants) {
                variant.itemOverrides = overrides;
            }
            models.put(itemLocation, variants[0]);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        if (side != null) return List.of();
        if (item) return itemQuads;
        int size = state == null ? 0 : state.getValue(PowerCableBoxBlock.SIZE);
        return worldQuads[size][mask(state)];
    }

    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) { return RENDER_TYPES; }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return textures.straight; }
    @Override public TextureAtlasSprite getParticleIcon() { return textures.straight; }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return item && itemOverrides != null ? itemOverrides : fallback.getOverrides(); }

    /** Resolves the former five metadata variants from the block item's saved size. */
    private static final class CableBoxItemOverrides extends ItemOverrides {
        private final BakedModel[] variants;

        private CableBoxItemOverrides(BakedModel[] variants) {
            super();
            this.variants = variants;
        }

        @Override
        public BakedModel resolve(BakedModel originalModel, ItemStack stack, @Nullable ClientLevel level,
                                  @Nullable LivingEntity entity, int seed) {
            return variants[PowerCableBoxBlockItem.size(stack)];
        }
    }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad>[][] bakeWorld(Textures textures) {
        List<BakedQuad>[][] baked = new List[5][64];
        for (int size = 0; size < 5; size++) {
            for (int mask = 0; mask < 64; mask++) {
                baked[size][mask] = bake(textures, size, mask);
            }
        }
        return baked;
    }

    /** Exact RenderBoxDuct inventory geometry: a Z-axis segment with old UV rotations. */
    private static List<BakedQuad> bakeItem(Textures textures, int size) {
        EnumMap<Direction, TextureAtlasSprite> sprites = textures.sprites(size, 0b000011);
        EnumMap<Direction, Integer> rotations = rotations(Direction.WEST, 1, Direction.EAST, 2);
        List<BakedQuad> quads = new ArrayList<>();
        float lower = (2.0F + size) / 16.0F;
        float upper = (14.0F - size) / 16.0F;
        CableModelGeometry.addLegacyBox(quads, lower, lower, 0.0F, upper, upper, 1.0F,
                sprites, rotations);
        return List.copyOf(quads);
    }

    private static List<BakedQuad> bake(Textures textures, int size, int mask) {
        boolean east = connected(mask, Direction.EAST);
        boolean west = connected(mask, Direction.WEST);
        boolean up = connected(mask, Direction.UP);
        boolean down = connected(mask, Direction.DOWN);
        boolean south = connected(mask, Direction.SOUTH);
        boolean north = connected(mask, Direction.NORTH);
        int count = Integer.bitCount(mask);

        EnumMap<Direction, TextureAtlasSprite> sprites = textures.sprites(size, mask);
        EnumMap<Direction, Integer> rotations = new EnumMap<>(Direction.class);
        List<BakedQuad> quads = new ArrayList<>();
        float lower = (2.0F + size) / 16.0F;
        float upper = (14.0F - size) / 16.0F;

        if ((mask & 0b001111) == 0 && mask > 0) {
            rotations.put(Direction.UP, 1);
            rotations.put(Direction.DOWN, 1);
            rotations.put(Direction.NORTH, 2);
            rotations.put(Direction.SOUTH, 1);
            CableModelGeometry.addLegacyBox(quads, 0.0F, lower, lower, 1.0F, upper, upper,
                    sprites, rotations);
            return List.copyOf(quads);
        }
        if ((mask & 0b111100) == 0 && mask > 0) {
            rotations.put(Direction.WEST, 1);
            rotations.put(Direction.EAST, 2);
            CableModelGeometry.addLegacyBox(quads, lower, lower, 0.0F, upper, upper, 1.0F,
                    sprites, rotations);
            return List.copyOf(quads);
        }
        if ((mask & 0b110011) == 0 && mask > 0) {
            CableModelGeometry.addLegacyBox(quads, lower, 0.0F, lower, upper, 1.0F, upper,
                    sprites, rotations);
            return List.copyOf(quads);
        }

        if (count == 2) {
            if ((down || up) && (east || west)) {
                rotations.put(Direction.UP, 1);
                rotations.put(Direction.DOWN, 1);
            }
            if (!down && !up) {
                rotations.put(Direction.WEST, 1);
                rotations.put(Direction.EAST, 2);
                rotations.put(Direction.NORTH, 2);
                rotations.put(Direction.SOUTH, 1);
            }
            addCurve(quads, sprites, rotations, mask, lower, upper);
            return List.copyOf(quads);
        }

        addJunction(quads, sprites, rotations, mask, lower, upper);
        return List.copyOf(quads);
    }

    private static void addCurve(List<BakedQuad> quads,
                                 EnumMap<Direction, TextureAtlasSprite> sprites,
                                 EnumMap<Direction, Integer> rotations, int mask,
                                 float lower, float upper) {
        CableModelGeometry.addLegacyBox(quads, lower, lower, lower, upper, upper, upper,
                sprites, rotations);
        if (connected(mask, Direction.DOWN)) {
            CableModelGeometry.addLegacyBox(quads, lower, 0.0F, lower, upper, lower, upper,
                    sprites, rotations);
        }
        if (connected(mask, Direction.UP)) {
            CableModelGeometry.addLegacyBox(quads, lower, upper, lower, upper, 1.0F, upper,
                    sprites, rotations);
        }
        if (connected(mask, Direction.WEST)) {
            CableModelGeometry.addLegacyBox(quads, 0.0F, lower, lower, lower, upper, upper,
                    sprites, rotations);
        }
        if (connected(mask, Direction.EAST)) {
            CableModelGeometry.addLegacyBox(quads, upper, lower, lower, 1.0F, upper, upper,
                    sprites, rotations);
        }
        if (connected(mask, Direction.NORTH)) {
            CableModelGeometry.addLegacyBox(quads, lower, lower, 0.0F, upper, upper, lower,
                    sprites, rotations);
        }
        if (connected(mask, Direction.SOUTH)) {
            CableModelGeometry.addLegacyBox(quads, lower, lower, upper, upper, upper, 1.0F,
                    sprites, rotations);
        }
    }

    private static void addJunction(List<BakedQuad> quads,
                                    EnumMap<Direction, TextureAtlasSprite> sprites,
                                    EnumMap<Direction, Integer> rotations, int mask,
                                    float lower, float upper) {
        CableModelGeometry.addLegacyBox(quads, lower, lower, lower, upper, upper, upper,
                sprites, rotations);
        if (connected(mask, Direction.DOWN)) {
            CableModelGeometry.addLegacyBox(quads, lower, 0.0F, lower, upper, lower, upper,
                    sprites, Set.of(Direction.UP), rotations);
        }
        if (connected(mask, Direction.UP)) {
            CableModelGeometry.addLegacyBox(quads, lower, upper, lower, upper, 1.0F, upper,
                    sprites, Set.of(Direction.DOWN), rotations);
        }
        if (connected(mask, Direction.WEST)) {
            CableModelGeometry.addLegacyBox(quads, 0.0F, lower, lower, lower, upper, upper,
                    sprites, Set.of(Direction.EAST), rotations);
        }
        if (connected(mask, Direction.EAST)) {
            CableModelGeometry.addLegacyBox(quads, upper, lower, lower, 1.0F, upper, upper,
                    sprites, Set.of(Direction.WEST), rotations);
        }
        if (connected(mask, Direction.NORTH)) {
            CableModelGeometry.addLegacyBox(quads, lower, lower, 0.0F, upper, upper, lower,
                    sprites, Set.of(Direction.SOUTH), rotations);
        }
        if (connected(mask, Direction.SOUTH)) {
            CableModelGeometry.addLegacyBox(quads, lower, lower, upper, upper, upper, 1.0F,
                    sprites, Set.of(Direction.NORTH), rotations);
        }
    }

    private static boolean connected(int mask, Direction direction) {
        int bit = switch (direction) {
            case EAST -> 32;
            case WEST -> 16;
            case UP -> 8;
            case DOWN -> 4;
            case SOUTH -> 2;
            case NORTH -> 1;
        };
        return (mask & bit) != 0;
    }

    private static EnumMap<Direction, Integer> rotations(Direction first, int firstRotation,
                                                          Direction second, int secondRotation) {
        EnumMap<Direction, Integer> result = new EnumMap<>(Direction.class);
        result.put(first, firstRotation);
        result.put(second, secondRotation);
        return result;
    }

    private static int mask(@Nullable BlockState state) {
        if (state == null || !(state.getBlock() instanceof EnergyCableBlock)) return 0;
        return (state.getValue(EnergyCableBlock.EAST) ? 32 : 0)
                | (state.getValue(EnergyCableBlock.WEST) ? 16 : 0)
                | (state.getValue(EnergyCableBlock.UP) ? 8 : 0)
                | (state.getValue(EnergyCableBlock.DOWN) ? 4 : 0)
                | (state.getValue(EnergyCableBlock.SOUTH) ? 2 : 0)
                | (state.getValue(EnergyCableBlock.NORTH) ? 1 : 0);
    }

    private record Textures(TextureAtlasSprite straight, TextureAtlasSprite[] ends, TextureAtlasSprite curveTl,
                            TextureAtlasSprite curveTr, TextureAtlasSprite curveBl, TextureAtlasSprite curveBr,
                            TextureAtlasSprite junction) {
        private static Textures load(Function<Material, TextureAtlasSprite> textures) {
            TextureAtlasSprite[] ends = new TextureAtlasSprite[5];
            for (int index = 0; index < ends.length; index++) {
                ends[index] = sprite(textures, "boxduct_cable_end_" + index);
            }
            return new Textures(sprite(textures, "boxduct_cable_straight"), ends,
                    sprite(textures, "boxduct_cable_curve_tl"), sprite(textures, "boxduct_cable_curve_tr"),
                    sprite(textures, "boxduct_cable_curve_bl"), sprite(textures, "boxduct_cable_curve_br"),
                    sprite(textures, "boxduct_cable_junction"));
        }

        private EnumMap<Direction, TextureAtlasSprite> sprites(int size, int mask) {
            EnumMap<Direction, TextureAtlasSprite> result = CableModelGeometry.all(junction);
            int count = Integer.bitCount(mask);
            if ((mask & 0b001111) == 0 && mask > 0) {
                for (Direction side : Direction.values()) {
                    result.put(side, side.getAxis() == Direction.Axis.X ? ends[size] : straight);
                }
                return result;
            }
            if ((mask & 0b111100) == 0 && mask > 0) {
                for (Direction side : Direction.values()) {
                    result.put(side, side.getAxis() == Direction.Axis.Z ? ends[size] : straight);
                }
                return result;
            }
            if ((mask & 0b110011) == 0 && mask > 0) {
                for (Direction side : Direction.values()) {
                    result.put(side, side.getAxis() == Direction.Axis.Y ? ends[size] : straight);
                }
                return result;
            }

            for (Direction side : Direction.values()) {
                if (connected(mask, side)) {
                    result.put(side, ends[size]);
                    continue;
                }
                if (count != 2) {
                    continue;
                }
                if (connected(mask, side.getOpposite())) {
                    result.put(side, straight);
                    continue;
                }
                result.put(side, curve(mask, side));
            }
            return result;
        }

        private TextureAtlasSprite curve(int mask, Direction side) {
            boolean east = connected(mask, Direction.EAST);
            boolean west = connected(mask, Direction.WEST);
            boolean up = connected(mask, Direction.UP);
            boolean down = connected(mask, Direction.DOWN);
            boolean south = connected(mask, Direction.SOUTH);
            boolean north = connected(mask, Direction.NORTH);

            if (down && south) return side == Direction.WEST ? curveBr : curveBl;
            if (down && north) return side == Direction.EAST ? curveBr : curveBl;
            if (down && east) return side == Direction.SOUTH ? curveBr : curveBl;
            if (down && west) return side == Direction.NORTH ? curveBr : curveBl;
            if (up && south) return side == Direction.WEST ? curveTr : curveTl;
            if (up && north) return side == Direction.EAST ? curveTr : curveTl;
            if (up && east) return side == Direction.SOUTH ? curveTr : curveTl;
            if (up && west) return side == Direction.NORTH ? curveTr : curveTl;
            if (east && north) return curveTr;
            if (east && south) return curveBr;
            if (west && north) return curveTl;
            if (west && south) return curveBl;
            return junction;
        }

        private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> textures, String path) {
            return textures.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/" + path)));
        }
    }
}
