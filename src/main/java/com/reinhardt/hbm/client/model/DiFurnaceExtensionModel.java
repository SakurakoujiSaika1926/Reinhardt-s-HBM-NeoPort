package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DiFurnaceExtensionModel implements IDynamicBakedModel {
    private static final ResourceLocation BLOCK_ID = ReinhardtsHBM.id("machine_difurnace_ext");
    private static final ItemTransforms ITEM_TRANSFORMS = itemTransforms();

    private final BakedModel delegate;
    private final boolean itemModel;
    private final Map<Direction, List<BakedQuad>> sideQuads;
    private final List<BakedQuad> unculledQuads;

    private DiFurnaceExtensionModel(BakedModel delegate, boolean itemModel) {
        this.delegate = delegate;
        this.itemModel = itemModel;
        this.unculledQuads = translateLegacyOrigin(this.delegate.getQuads(null, null, RandomSource.create(42L), ModelData.EMPTY, null), this.itemModel);
        EnumMap<Direction, List<BakedQuad>> bakedSides = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            bakedSides.put(direction, translateLegacyOrigin(this.delegate.getQuads(null, direction, RandomSource.create(42L), ModelData.EMPTY, null), this.itemModel));
        }
        this.sideQuads = Map.copyOf(bakedSides);
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        int replaced = 0;
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!isTarget(location)) {
                continue;
            }
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof DiFurnaceExtensionModel)) {
                boolean item = ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant());
                models.put(location, new DiFurnaceExtensionModel(model, item));
                replaced++;
            }
        }
        if (replaced > 0) {
            ReinhardtsHBM.LOGGER.info("Installed 1.7.10 origin adapter for {} blast furnace extension models", replaced);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        if (side == null) {
            return this.unculledQuads;
        }
        return this.sideQuads.getOrDefault(side, List.of());
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return this.delegate.getRenderTypes(state, rand, data);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.delegate.getParticleIcon();
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        return this.delegate.getParticleIcon(data);
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.itemModel ? ITEM_TRANSFORMS : this.delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    private static boolean isTarget(ModelResourceLocation location) {
        return BLOCK_ID.equals(location.id());
    }

    private static List<BakedQuad> translateLegacyOrigin(List<BakedQuad> quads, boolean itemModel) {
        if (quads.isEmpty()) {
            return quads;
        }
        return quads.stream().map(quad -> translateLegacyOrigin(quad, itemModel)).toList();
    }

    private static BakedQuad translateLegacyOrigin(BakedQuad quad, boolean itemModel) {
        float translateX = 0.5F;
        float translateY = 0.0F;
        float translateZ = 0.5F;
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            vertices[offset] = Float.floatToRawIntBits(x + translateX);
            vertices[offset + 1] = Float.floatToRawIntBits(y + translateY);
            vertices[offset + 2] = Float.floatToRawIntBits(z + translateZ);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    private static ItemTransforms itemTransforms() {
        ItemTransform thirdPerson = transform(75.0F, 45.0F, 0.0F, 0.0F, 2.5F / 16.0F, 0.0F, 0.375F);
        ItemTransform firstPerson = transform(0.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F);
        ItemTransform head = transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        ItemTransform gui = transform(30.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F);
        ItemTransform ground = transform(0.0F, 0.0F, 0.0F, 0.0F, 3.0F / 16.0F, 0.0F, 0.25F);
        ItemTransform fixed = transform(0.0F, 180.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
        return new ItemTransforms(thirdPerson, thirdPerson, firstPerson, firstPerson, head, gui, ground, fixed);
    }

    private static ItemTransform transform(float rotX, float rotY, float rotZ, float translateX, float translateY, float translateZ, float scale) {
        return new ItemTransform(
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(translateX, translateY, translateZ),
                new Vector3f(scale, scale, scale)
        );
    }
}
