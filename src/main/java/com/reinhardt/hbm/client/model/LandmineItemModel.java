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

public final class LandmineItemModel implements IDynamicBakedModel {
    private static final Set<String> LANDMINES = Set.of("mine_ap", "mine_he", "mine_shrap", "mine_fat", "mine_naval");
    private static final ItemTransforms IDENTITY_TRANSFORMS = identityTransforms();

    private final BakedModel delegate;

    private LandmineItemModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!isLandmineInventory(location)) {
                continue;
            }
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof LandmineItemModel)) {
                models.put(location, new LandmineItemModel(model));
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

    private static boolean isLandmineInventory(ModelResourceLocation location) {
        return ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant())
                && ReinhardtsHBM.MOD_ID.equals(location.id().getNamespace())
                && LANDMINES.contains(location.id().getPath());
    }

    private static ItemTransforms identityTransforms() {
        ItemTransform identity = new ItemTransform(new Vector3f(), new Vector3f(), new Vector3f(1.0F));
        return new ItemTransforms(identity, identity, identity, identity, identity, identity, identity, identity);
    }
}
