package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Dedicated inventory geometry for the Soyuz capsule.
 *
 * <p>The constants below are the exact single-precision bounds of every face-used
 * vertex in {@code models/obj/block/soyuz_capsule.obj}. They freeze the previous
 * capsule-only result without retaining a cross-machine bounds fitter.</p>
 */
public final class SoyuzCapsuleItemModel implements IDynamicBakedModel {
    private static final float CENTER_X = -9.536743E-7F;
    private static final float CENTER_Y = 6.80364F;
    private static final float CENTER_Z = 0.0F;
    private static final float SCALE = 0.06026186F;

    private final BakedModel delegate;
    private final Map<Direction, List<BakedQuad>> sideQuads;
    private final List<BakedQuad> unculledQuads;

    private SoyuzCapsuleItemModel(BakedModel delegate) {
        this.delegate = delegate;
        this.unculledQuads = transformSoyuzCapsuleQuads(
                delegate.getQuads(null, null, RandomSource.create(42L), ModelData.EMPTY, null));
        EnumMap<Direction, List<BakedQuad>> bakedSides = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            bakedSides.put(direction, transformSoyuzCapsuleQuads(
                    delegate.getQuads(null, direction, RandomSource.create(42L), ModelData.EMPTY, null)));
        }
        this.sideQuads = Map.copyOf(bakedSides);
    }

    public static void replaceModel(Map<ModelResourceLocation, BakedModel> models) {
        ModelResourceLocation location = new ModelResourceLocation(
                ReinhardtsHBM.id("soyuz_capsule"), ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel model = models.get(location);
        if (model != null && !(model instanceof SoyuzCapsuleItemModel)) {
            models.put(location, new SoyuzCapsuleItemModel(model));
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData extraData, @Nullable RenderType renderType) {
        return side == null ? this.unculledQuads : this.sideQuads.getOrDefault(side, List.of());
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
    public ItemTransforms getTransforms() {
        return this.delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    private static List<BakedQuad> transformSoyuzCapsuleQuads(List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return quads;
        }
        return quads.stream().map(SoyuzCapsuleItemModel::transformSoyuzCapsuleQuad).toList();
    }

    private static BakedQuad transformSoyuzCapsuleQuad(BakedQuad quad) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            vertices[offset] = Float.floatToRawIntBits((x - CENTER_X) * SCALE + 0.5F);
            vertices[offset + 1] = Float.floatToRawIntBits((y - CENTER_Y) * SCALE + 0.5F);
            vertices[offset + 2] = Float.floatToRawIntBits((z - CENTER_Z) * SCALE + 0.5F);
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }
}
