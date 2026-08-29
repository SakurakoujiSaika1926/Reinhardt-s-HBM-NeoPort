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
import java.util.HashSet;
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
        this.itemQuads = item ? bake(textures, itemSize, 0b000011) : List.of();
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

    private static List<BakedQuad> bake(Textures textures, int size, int mask) {
        boolean east = (mask & 32) != 0;
        boolean west = (mask & 16) != 0;
        boolean up = (mask & 8) != 0;
        boolean down = (mask & 4) != 0;
        boolean south = (mask & 2) != 0;
        boolean north = (mask & 1) != 0;
        Set<Direction> connected = new HashSet<>();
        if (north) connected.add(Direction.NORTH);
        if (south) connected.add(Direction.SOUTH);
        if (west) connected.add(Direction.WEST);
        if (east) connected.add(Direction.EAST);
        if (up) connected.add(Direction.UP);
        if (down) connected.add(Direction.DOWN);

        EnumMap<Direction, TextureAtlasSprite> sprites = textures.sprites(size, connected);
        List<BakedQuad> quads = new ArrayList<>();
        // RenderBoxDuct uses exactly the same 2/16..14/16 envelope as the
        // collision code, narrowing it by one pixel for each metadata value.
        float lower = (2.0F + size) / 16.0F;
        float upper = (14.0F - size) / 16.0F;
        if (mask == 0) {
            CableModelGeometry.addBox(quads, lower, lower, lower, upper, upper, upper, sprites, Set.of());
            return List.copyOf(quads);
        }
        if (mask == 0b100000 || mask == 0b010000 || mask == 0b110000) {
            CableModelGeometry.addBox(quads, 0.0F, lower, lower, 1.0F, upper, upper, sprites, Set.of());
            return List.copyOf(quads);
        }
        if (mask == 0b001000 || mask == 0b000100 || mask == 0b001100) {
            CableModelGeometry.addBox(quads, lower, 0.0F, lower, upper, 1.0F, upper, sprites, Set.of());
            return List.copyOf(quads);
        }
        if (mask == 0b000010 || mask == 0b000001 || mask == 0b000011) {
            CableModelGeometry.addBox(quads, lower, lower, 0.0F, upper, upper, 1.0F, sprites, Set.of());
            return List.copyOf(quads);
        }

        boolean junction = connected.size() > 2;
        float coreLower = lower;
        float coreUpper = upper;
        CableModelGeometry.addBox(quads, coreLower, coreLower, coreLower, coreUpper, coreUpper, coreUpper, sprites, connected);
        for (Direction direction : connected) {
            float minX = direction == Direction.WEST ? 0.0F : lower;
            float maxX = direction == Direction.EAST ? 1.0F : upper;
            float minY = direction == Direction.DOWN ? 0.0F : lower;
            float maxY = direction == Direction.UP ? 1.0F : upper;
            float minZ = direction == Direction.NORTH ? 0.0F : lower;
            float maxZ = direction == Direction.SOUTH ? 1.0F : upper;
            CableModelGeometry.addBox(quads, minX, minY, minZ, maxX, maxY, maxZ, sprites, Set.of(direction.getOpposite()));
        }
        return List.copyOf(quads);
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

        private EnumMap<Direction, TextureAtlasSprite> sprites(int size, Set<Direction> connected) {
            EnumMap<Direction, TextureAtlasSprite> result = CableModelGeometry.all(junction);
            if (connected.isEmpty()) return result;
            if (connected.size() == 2 && connected.stream().anyMatch(direction -> connected.contains(direction.getOpposite()))) {
                Direction first = connected.iterator().next();
                for (Direction direction : Direction.values()) {
                    result.put(direction, direction.getAxis() == first.getAxis() ? ends[size] : straight);
                }
                return result;
            }
            for (Direction direction : connected) result.put(direction, ends[size]);
            if (connected.size() == 2) {
                TextureAtlasSprite curve = curve(connected);
                for (Direction direction : Direction.values()) {
                    if (!connected.contains(direction)) result.put(direction, curve);
                }
            }
            return result;
        }

        private TextureAtlasSprite curve(Set<Direction> connected) {
            if (connected.contains(Direction.UP)) {
                return connected.contains(Direction.SOUTH) || connected.contains(Direction.EAST) ? curveTr : curveTl;
            }
            if (connected.contains(Direction.DOWN)) {
                return connected.contains(Direction.SOUTH) || connected.contains(Direction.EAST) ? curveBr : curveBl;
            }
            if (connected.contains(Direction.EAST)) return connected.contains(Direction.NORTH) ? curveTr : curveBr;
            return connected.contains(Direction.NORTH) ? curveTl : curveBl;
        }

        private static TextureAtlasSprite sprite(Function<Material, TextureAtlasSprite> textures, String path) {
            return textures.apply(new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/" + path)));
        }
    }
}
