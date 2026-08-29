package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.GlyphidBaseBlock;
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
import java.util.function.Function;

/** Recreates BlockGlyphid's position-stable two-texture selection from 1.7.10. */
public final class GlyphidBaseBakedModel implements IDynamicBakedModel {
    private static final ResourceLocation BLOCK_ID = ReinhardtsHBM.id("glyphid_base");
    private static final ModelProperty<Integer> TEXTURE = new ModelProperty<>(value -> value != null);
    private static final ChunkRenderTypeSet RENDER_TYPES = ChunkRenderTypeSet.of(RenderType.solid());

    private final BakedModel fallback;
    private final TextureAtlasSprite[][] sprites;
    private final List<BakedQuad>[][] quads;

    private GlyphidBaseBakedModel(BakedModel fallback, TextureAtlasSprite[][] sprites) {
        this.fallback = fallback;
        this.sprites = sprites;
        this.quads = bakeQuads(sprites);
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models,
                                     Function<Material, TextureAtlasSprite> textureGetter) {
        List<ModelResourceLocation> locations = models.keySet().stream()
                .filter(location -> BLOCK_ID.equals(location.id())
                        && !ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()))
                .toList();
        for (ModelResourceLocation location : locations) {
            BakedModel fallback = models.get(location);
            if (fallback == null || fallback instanceof GlyphidBaseBakedModel) {
                continue;
            }
            TextureAtlasSprite[][] sprites = new TextureAtlasSprite[][]{
                    {textureGetter.apply(sprite("glyphid_base")), textureGetter.apply(sprite("glyphid_base_alt"))},
                    {textureGetter.apply(sprite("glyphid_base_infested")), textureGetter.apply(sprite("glyphid_base_infested_alt"))},
                    {textureGetter.apply(sprite("glyphid_base_rad")), textureGetter.apply(sprite("glyphid_base_rad_alt"))}
            };
            models.put(location, new GlyphidBaseBakedModel(fallback, sprites));
        }
    }

    private static Material sprite(String name) {
        return new Material(TextureAtlas.LOCATION_BLOCKS, ReinhardtsHBM.id("block/" + name));
    }

    @SuppressWarnings("unchecked")
    private static List<BakedQuad>[][] bakeQuads(TextureAtlasSprite[][] sprites) {
        List<BakedQuad>[][] baked = new List[3][2];
        for (int variant = 0; variant < sprites.length; variant++) {
            for (int texture = 0; texture < sprites[variant].length; texture++) {
                List<BakedQuad> faces = new ArrayList<>(Direction.values().length);
                for (Direction direction : Direction.values()) {
                    faces.add(bakeFace(direction, sprites[variant][texture]));
                }
                baked[variant][texture] = List.copyOf(faces);
            }
        }
        return baked;
    }

    private static BakedQuad bakeFace(Direction direction, TextureAtlasSprite sprite) {
        QuadBakingVertexConsumer baker = new QuadBakingVertexConsumer();
        baker.setSprite(sprite);
        baker.setTintIndex(-1);
        baker.setShade(true);
        baker.setHasAmbientOcclusion(true);
        baker.setDirection(direction);
        for (Vertex vertex : vertices(direction)) {
            baker.addVertex(vertex.x(), vertex.y(), vertex.z());
            baker.setColor(255, 255, 255, 255);
            baker.setUv(sprite.getU(vertex.u()), sprite.getV(vertex.v()));
            baker.setLight(0);
            baker.setNormal(direction.getStepX(), direction.getStepY(), direction.getStepZ());
        }
        return baker.bakeQuad();
    }

    private static Vertex[] vertices(Direction face) {
        return switch (face) {
            case DOWN -> new Vertex[]{
                    new Vertex(0.0F, 0.0F, 1.0F, 0.0F, 0.0F), new Vertex(0.0F, 0.0F, 0.0F, 0.0F, 1.0F),
                    new Vertex(1.0F, 0.0F, 0.0F, 1.0F, 1.0F), new Vertex(1.0F, 0.0F, 1.0F, 1.0F, 0.0F)};
            case UP -> new Vertex[]{
                    new Vertex(1.0F, 1.0F, 1.0F, 0.0F, 0.0F), new Vertex(1.0F, 1.0F, 0.0F, 0.0F, 1.0F),
                    new Vertex(0.0F, 1.0F, 0.0F, 1.0F, 1.0F), new Vertex(0.0F, 1.0F, 1.0F, 1.0F, 0.0F)};
            case NORTH -> new Vertex[]{
                    new Vertex(0.0F, 1.0F, 0.0F, 0.0F, 0.0F), new Vertex(1.0F, 1.0F, 0.0F, 1.0F, 0.0F),
                    new Vertex(1.0F, 0.0F, 0.0F, 1.0F, 1.0F), new Vertex(0.0F, 0.0F, 0.0F, 0.0F, 1.0F)};
            case SOUTH -> new Vertex[]{
                    new Vertex(1.0F, 1.0F, 1.0F, 0.0F, 0.0F), new Vertex(0.0F, 1.0F, 1.0F, 1.0F, 0.0F),
                    new Vertex(0.0F, 0.0F, 1.0F, 1.0F, 1.0F), new Vertex(1.0F, 0.0F, 1.0F, 0.0F, 1.0F)};
            case WEST -> new Vertex[]{
                    new Vertex(0.0F, 1.0F, 1.0F, 0.0F, 0.0F), new Vertex(0.0F, 1.0F, 0.0F, 1.0F, 0.0F),
                    new Vertex(0.0F, 0.0F, 0.0F, 1.0F, 1.0F), new Vertex(0.0F, 0.0F, 1.0F, 0.0F, 1.0F)};
            case EAST -> new Vertex[]{
                    new Vertex(1.0F, 1.0F, 0.0F, 0.0F, 0.0F), new Vertex(1.0F, 1.0F, 1.0F, 1.0F, 0.0F),
                    new Vertex(1.0F, 0.0F, 1.0F, 1.0F, 1.0F), new Vertex(1.0F, 0.0F, 0.0F, 0.0F, 1.0F)};
        };
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        int variant = state.getBlock() instanceof GlyphidBaseBlock
                ? state.getValue(GlyphidBaseBlock.VARIANT) : 0;
        long hash = ((long) pos.getX() * 3129871L) ^ ((long) pos.getY() * 116129781L) ^ (long) pos.getZ();
        hash = hash * hash * 42317861L + hash * 11L;
        int texture = (int) ((hash >> 16) & 3L) % 2;
        return modelData.derive().with(TEXTURE, variant * 2 + texture).build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData data, @Nullable RenderType renderType) {
        int choice = data.get(TEXTURE) == null ? 0 : data.get(TEXTURE);
        int variant = Math.max(0, Math.min(2, choice / 2));
        int texture = choice & 1;
        if (side == null) {
            return quads[variant][texture];
        }
        return List.of(quads[variant][texture].get(side.ordinal()));
    }

    @Override public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) { return RENDER_TYPES; }
    @Override public TextureAtlasSprite getParticleIcon(ModelData data) { return sprites[0][0]; }
    @Override public TextureAtlasSprite getParticleIcon() { return sprites[0][0]; }
    @Override public boolean useAmbientOcclusion() { return fallback.useAmbientOcclusion(); }
    @Override public boolean isGui3d() { return fallback.isGui3d(); }
    @Override public boolean usesBlockLight() { return fallback.usesBlockLight(); }
    @Override public boolean isCustomRenderer() { return false; }
    @Override public ItemTransforms getTransforms() { return fallback.getTransforms(); }
    @Override public ItemOverrides getOverrides() { return fallback.getOverrides(); }

    private record Vertex(float x, float y, float z, float u, float v) { }
}
