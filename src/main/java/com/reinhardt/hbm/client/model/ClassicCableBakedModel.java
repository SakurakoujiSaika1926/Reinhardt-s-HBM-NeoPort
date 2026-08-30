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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/** Exact 5.5/16 to 10.5/16 geometry from RenderCableClassic. */
public final class ClassicCableBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation ID = ReinhardtsHBM.id("red_cable_classic");
    private static final ResourceLocation TEXTURE = ReinhardtsHBM.id("block/red_cable_classic");
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel fallback;
    private final TextureAtlasSprite sprite;
    private final boolean item;
    private final List<BakedQuad>[] worldQuads;
    private final List<BakedQuad> itemQuads;

    @SuppressWarnings("unchecked")
    private ClassicCableBakedModel(BakedModel fallback, TextureAtlasSprite sprite, boolean item) {
        this.fallback = fallback;
        this.sprite = sprite;
        this.item = item;
        this.worldQuads = item ? new List[0] : bakeWorld(sprite);
        // RenderCableClassic#renderInventoryBlock renders all six arms, not
        // merely the horizontal four used by the first 1.21 port.
        this.itemQuads = item ? bake(sprite, Set.of(Direction.NORTH, Direction.SOUTH,
                Direction.EAST, Direction.WEST, Direction.UP, Direction.DOWN)) : List.of();
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models, Function<Material, TextureAtlasSprite> textures) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(location -> ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        ModelResourceLocation itemLocation = new ModelResourceLocation(ID, ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel fallback = locations.stream().map(models::get).filter(Objects::nonNull).findFirst().orElse(models.get(itemLocation));
        if (fallback == null) {
            return;
        }
        TextureAtlasSprite sprite = textures.apply(new Material(TextureAtlas.LOCATION_BLOCKS, TEXTURE));
        ClassicCableBakedModel world = new ClassicCableBakedModel(fallback, sprite, false);
        for (ModelResourceLocation location : locations) {
            models.put(location, world);
        }
        if (models.containsKey(itemLocation)) {
            models.put(itemLocation, new ClassicCableBakedModel(models.get(itemLocation), sprite, true));
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random, ModelData data,
                                    @Nullable RenderType renderType) {
        if (side != null) {
            return List.of();
        }
        return this.item ? this.itemQuads : this.worldQuads[mask(state)];
    }

    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) { return RENDER_TYPES; }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return sprite; }
    @Override public TextureAtlasSprite getParticleIcon() { return sprite; }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return true; }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad>[] bakeWorld(TextureAtlasSprite sprite) {
        List<BakedQuad>[] baked = new List[64];
        for (int mask = 0; mask < baked.length; mask++) {
            Set<Direction> connected = new HashSet<>();
            if ((mask & 1) != 0) connected.add(Direction.EAST);
            if ((mask & 2) != 0) connected.add(Direction.WEST);
            if ((mask & 4) != 0) connected.add(Direction.UP);
            if ((mask & 8) != 0) connected.add(Direction.DOWN);
            if ((mask & 16) != 0) connected.add(Direction.SOUTH);
            if ((mask & 32) != 0) connected.add(Direction.NORTH);
            baked[mask] = bake(sprite, connected);
        }
        return baked;
    }

    private static List<BakedQuad> bake(TextureAtlasSprite sprite, Set<Direction> connected) {
        float lower = 5.5F / 16.0F;
        float upper = 10.5F / 16.0F;
        EnumMap<Direction, TextureAtlasSprite> sprites = CableModelGeometry.all(sprite);
        List<BakedQuad> quads = new ArrayList<>();
        CableModelGeometry.addBox(quads, lower, lower, lower, upper, upper, upper, sprites, connected);
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
        return (state.getValue(EnergyCableBlock.EAST) ? 1 : 0)
                | (state.getValue(EnergyCableBlock.WEST) ? 2 : 0)
                | (state.getValue(EnergyCableBlock.UP) ? 4 : 0)
                | (state.getValue(EnergyCableBlock.DOWN) ? 8 : 0)
                | (state.getValue(EnergyCableBlock.SOUTH) ? 16 : 0)
                | (state.getValue(EnergyCableBlock.NORTH) ? 32 : 0);
    }
}
