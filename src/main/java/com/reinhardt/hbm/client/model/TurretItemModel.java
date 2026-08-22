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

public final class TurretItemModel implements IDynamicBakedModel {
    private static final Set<String> TURRET_ITEMS = Set.of(
            "turret_jeremy",
            "turret_chekhov",
            "turret_friendly",
            "turret_fritz",
            "turret_howard",
            "turret_howard_damaged",
            "turret_maxwell",
            "turret_richard",
            "turret_tauon",
            "turret_arty",
            "turret_himars",
            "turret_sentry",
            "turret_sentry_damaged"
    );
    private static final ItemTransforms IDENTITY_TRANSFORMS = identityTransforms();

    private final BakedModel delegate;

    private TurretItemModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        int replaced = 0;
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!isTurretInventory(location)) {
                continue;
            }
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof TurretItemModel)) {
                models.put(location, new TurretItemModel(model));
                replaced++;
            }
        }
        if (replaced > 0) {
            ReinhardtsHBM.LOGGER.info("Installed legacy item renderer hooks for {} turret item models", replaced);
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
        return IDENTITY_TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    private static boolean isTurretInventory(ModelResourceLocation location) {
        return ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant())
                && ReinhardtsHBM.MOD_ID.equals(location.id().getNamespace())
                && TURRET_ITEMS.contains(location.id().getPath());
    }

    private static ItemTransforms identityTransforms() {
        ItemTransform identity = transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        return new ItemTransforms(identity, identity, identity, identity, identity, identity, identity, identity);
    }

    private static ItemTransform transform(float rotX, float rotY, float rotZ, float translateX, float translateY, float translateZ, float scale) {
        return new ItemTransform(
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(translateX, translateY, translateZ),
                new Vector3f(scale, scale, scale)
        );
    }
}
