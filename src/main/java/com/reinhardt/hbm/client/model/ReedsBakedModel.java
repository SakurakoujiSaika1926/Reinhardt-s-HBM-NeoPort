package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.ReedsBlock;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.client.model.pipeline.QuadBakingVertexConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Recreates RenderReeds from 1.7.10. A single reed block is rendered from the surface down to the
 * first non-water block, with the top/middle/bottom source sprites in the same order as the legacy renderer.
 */
public final class ReedsBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation BLOCK_ID = ReinhardtsHBM.id("plant_reeds");
    private static final ModelProperty<Integer> DEPTH = new ModelProperty<>(depth -> depth != null && depth > 0);
    private static final ChunkRenderTypeSet CUTOUT = ChunkRenderTypeSet.of(RenderType.cutout());

    private final BakedModel fallback;
    private final TextureAtlasSprite top;
    private final TextureAtlasSprite middle;
    private final TextureAtlasSprite bottom;
    private final Map<Integer, List<BakedQuad>> quadsByDepth = new ConcurrentHashMap<>();

    private ReedsBakedModel(BakedModel fallback, TextureAtlasSprite top, TextureAtlasSprite middle, TextureAtlasSprite bottom) {
        this.fallback = fallback;
        this.top = top;
        this.middle = middle;
        this.bottom = bottom;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textureGetter) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(location -> BLOCK_ID.equals(location.id()) && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        BakedModel fallback = locations.stream().map(models::get).filter(Objects::nonNull).findFirst().orElse(null);
        if (fallback == null) {
            ReinhardtsHBM.LOGGER.warn("Unable to install 1.7.10 reeds model: no baked block variant found");
            return;
        }

        ReedsBakedModel model = new ReedsBakedModel(
                fallback,
                textureGetter.apply(sprite("reeds_top")),
                textureGetter.apply(sprite("reeds_mid")),
                textureGetter.apply(sprite("reeds_bottom"))
        );
        for (ModelResourceLocation location : locations) {
            models.put(location, model);
        }
    }

    private static Material sprite(String path) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/" + path));
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        if (!(state.getBlock() instanceof ReedsBlock)) {
            return modelData;
        }
        int depth = 1;
        for (BlockPos cursor = pos.below(); cursor.getY() >= level.getMinBuildHeight() && level.getBlockState(cursor).is(Blocks.WATER); cursor = cursor.below()) {
            depth++;
        }
        return modelData.derive().with(DEPTH, depth).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        if (side != null) {
            return List.of();
        }
        Integer storedDepth = data.get(DEPTH);
        int depth = storedDepth == null ? 1 : storedDepth;
        return quadsByDepth.computeIfAbsent(depth, this::bakeReeds);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
        return CUTOUT;
    }

    private List<BakedQuad> bakeReeds(int depth) {
        List<BakedQuad> quads = new ArrayList<>(Math.max(1, depth) * 4);
        for (int segment = 0; segment < Math.max(1, depth); segment++) {
            TextureAtlasSprite sprite = segment == 0 ? top : (segment == depth - 1 ? bottom : middle);
            float low = -segment;
            float high = 1.0F - segment;
            addCross(quads, low, high, sprite);
        }
        return List.copyOf(quads);
    }

    private static void addCross(List<BakedQuad> quads, float low, float high, TextureAtlasSprite sprite) {
        addQuad(quads, sprite, Direction.NORTH,
                new Vertex(0.0F, high, 0.0F, 0.0F, 0.0F), new Vertex(1.0F, high, 1.0F, 16.0F, 0.0F),
                new Vertex(1.0F, low, 1.0F, 16.0F, 16.0F), new Vertex(0.0F, low, 0.0F, 0.0F, 16.0F));
        addQuad(quads, sprite, Direction.SOUTH,
                new Vertex(1.0F, high, 1.0F, 0.0F, 0.0F), new Vertex(0.0F, high, 0.0F, 16.0F, 0.0F),
                new Vertex(0.0F, low, 0.0F, 16.0F, 16.0F), new Vertex(1.0F, low, 1.0F, 0.0F, 16.0F));
        addQuad(quads, sprite, Direction.WEST,
                new Vertex(1.0F, high, 0.0F, 0.0F, 0.0F), new Vertex(0.0F, high, 1.0F, 16.0F, 0.0F),
                new Vertex(0.0F, low, 1.0F, 16.0F, 16.0F), new Vertex(1.0F, low, 0.0F, 0.0F, 16.0F));
        addQuad(quads, sprite, Direction.EAST,
                new Vertex(0.0F, high, 1.0F, 0.0F, 0.0F), new Vertex(1.0F, high, 0.0F, 16.0F, 0.0F),
                new Vertex(1.0F, low, 0.0F, 16.0F, 16.0F), new Vertex(0.0F, low, 1.0F, 0.0F, 16.0F));
    }

    private static void addQuad(List<BakedQuad> quads, TextureAtlasSprite sprite, Direction normal, Vertex... vertices) {
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(0);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(false);
        baker.setDirection(normal);
        for (Vertex vertex : vertices) {
            baker.addVertex(vertex.x(), vertex.y(), vertex.z());
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(vertex.u() / 16.0F), sprite.getV(vertex.v() / 16.0F));
            baker.setLight(0);
            baker.setNormal(normal.getStepX(), normal.getStepY(), normal.getStepZ());
        }
        quads.add(baker.bakeQuad());
    }

    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return top; }
    @Override public TextureAtlasSprite getParticleIcon() { return top; }
    @Override public boolean useAmbientOcclusion() { return false; }
    @Override public boolean isGui3d() { return fallback.isGui3d(); }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }

    private record Vertex(float x, float y, float z, float u, float v) { }
}
