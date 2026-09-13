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

public final class StirlingGeneratorItemModel implements IDynamicBakedModel {
    /*
     * The custom renderer is the complete 1.7.10 RenderStirling item path.
     * Keeping the display transforms that used to live here applies them once
     * in ItemRenderer and then applies the same transforms again in
     * StirlingGeneratorItemRenderer.  An identity transform is required so
     * only that renderer's literal legacy values reach the OBJ geometry.
     */
    private static final ItemTransforms ITEM_TRANSFORMS = identityTransforms();

    private final BakedModel delegate;

    private StirlingGeneratorItemModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        int replaced = 0;
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!isStirlingInventory(location)) {
                continue;
            }
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof StirlingGeneratorItemModel)) {
                models.put(location, new StirlingGeneratorItemModel(model));
                replaced++;
            }
        }
        if (replaced > 0) {
            ReinhardtsHBM.LOGGER.info("Installed animated item renderer hooks for {} Stirling generator item models", replaced);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
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
        return ITEM_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    private static boolean isStirlingInventory(ModelResourceLocation location) {
        if (!ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()) || !ReinhardtsHBM.MOD_ID.equals(location.id().getNamespace())) {
            return false;
        }
        String path = location.id().getPath();
        return path.equals("machine_stirling")
                || path.equals("machine_stirling_steel")
                || path.equals("machine_stirling_creative");
    }

    private static ItemTransforms identityTransforms() {
        ItemTransform identity = new ItemTransform(
                new Vector3f(), new Vector3f(), new Vector3f(1.0F, 1.0F, 1.0F)
        );
        return new ItemTransforms(identity, identity, identity, identity,
                identity, identity, identity, identity);
    }
}
