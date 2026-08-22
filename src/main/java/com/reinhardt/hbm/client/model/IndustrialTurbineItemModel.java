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

public final class IndustrialTurbineItemModel implements IDynamicBakedModel {
    private static final ItemTransforms ITEM_TRANSFORMS = itemTransforms();

    private final BakedModel delegate;

    private IndustrialTurbineItemModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        ModelResourceLocation location = new ModelResourceLocation(ReinhardtsHBM.id("machine_industrial_turbine"), ModelResourceLocation.INVENTORY_VARIANT);
        BakedModel model = models.get(location);
        if (model != null && !(model instanceof IndustrialTurbineItemModel)) {
            models.put(location, new IndustrialTurbineItemModel(model));
            ReinhardtsHBM.LOGGER.info("Installed animated item renderer hook for Industrial Turbine item model");
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

    private static ItemTransforms itemTransforms() {
        ItemTransform gui = transform(30.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F);
        ItemTransform thirdPerson = transform(75.0F, 45.0F, 0.0F, 0.0F, 2.5F / 16.0F, 0.0F, 0.375F);
        ItemTransform firstPerson = transform(0.0F, 45.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.4F);
        ItemTransform ground = transform(0.0F, 0.0F, 0.0F, 0.0F, 3.0F / 16.0F, 0.0F, 0.25F);
        ItemTransform head = transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
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
