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
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Marks the small set of legacy items that own an individual BEWLR.
 *
 * This deliberately performs no fitting or transform adjustment. Each target's
 * renderer contains the literal 1.7.10 inventory transform for that model.
 */
public final class DedicatedItemRendererModel implements IDynamicBakedModel {
    private static final Set<String> ITEM_IDS = Set.of(
            "geiger",
            "machine_funnel",
            "radar_screen",
            "lamp_demon",
            "machine_deuterium_tower"
    );
    private static final ItemTransforms IDENTITY_TRANSFORMS = identityTransforms();

    private final BakedModel delegate;

    private DedicatedItemRendererModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    public static boolean owns(String itemId) {
        return ITEM_IDS.contains(itemId);
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        for (String id : ITEM_IDS) {
            ModelResourceLocation location = new ModelResourceLocation(
                    ReinhardtsHBM.id(id), ModelResourceLocation.INVENTORY_VARIANT
            );
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof DedicatedItemRendererModel)) {
                models.put(location, new DedicatedItemRendererModel(model));
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData modelData, @Nullable RenderType renderType) {
        return Collections.emptyList();
    }

    @Override
    public boolean useAmbientOcclusion() {
        return false;
    }

    @Override
    public boolean isGui3d() {
        return true;
    }

    @Override
    public boolean usesBlockLight() {
        return this.delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return true;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return IDENTITY_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    private static ItemTransforms identityTransforms() {
        ItemTransform identity = new ItemTransform(
                new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F)
        );
        return new ItemTransforms(identity, identity, identity, identity,
                identity, identity, identity, identity);
    }
}
