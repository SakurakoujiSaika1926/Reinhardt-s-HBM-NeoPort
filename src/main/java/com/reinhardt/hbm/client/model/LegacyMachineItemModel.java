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

/** Routes legacy machine items through their OBJ item renderer instead of a flat baked quad. */
public final class LegacyMachineItemModel implements IDynamicBakedModel {
    /*
     * LegacyMachineItemRenderer reproduces ItemRenderBase itself.  Feeding it
     * modern block-item transforms as well changes the old inventory scale and
     * produces a different result for every OBJ coordinate system.
     */
    private static final ItemTransforms ITEM_TRANSFORMS = identityTransforms();

    private final BakedModel delegate;

    private LegacyMachineItemModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        for (String name : List.of(
                "machine_annihilator", "machine_autosaw", "machine_forcefield", "machine_missile_assembly",
                "machine_microwave", "machine_orbus", "machine_precass", "machine_pyrooven", "machine_radar", "machine_radar_large",
                "machine_radgen", "machine_radiolysis", "machine_rtg_grey", "machine_sawmill", "machine_turbofan",
                "machine_thresher", "machine_lpw2")) {
            ModelResourceLocation location = new ModelResourceLocation(
                    ReinhardtsHBM.id(name), ModelResourceLocation.INVENTORY_VARIANT
            );
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof LegacyMachineItemModel)) {
                models.put(location, new LegacyMachineItemModel(model));
            }
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand,
                                    ModelData extraData, @Nullable RenderType renderType) {
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

    private static ItemTransforms identityTransforms() {
        ItemTransform identity = transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        return new ItemTransforms(identity, identity, identity, identity, identity, identity, identity, identity);
    }

    private static ItemTransform transform(float rotX, float rotY, float rotZ, float translateX,
                                            float translateY, float translateZ, float scale) {
        return new ItemTransform(
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(translateX, translateY, translateZ),
                new Vector3f(scale, scale, scale)
        );
    }
}
