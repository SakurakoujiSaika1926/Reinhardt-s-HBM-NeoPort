package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.render.ObjMachineItemRenderer;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Routes complete OBJ machine items through the shared centered item renderer. */
public final class ObjMachineItemModel implements IDynamicBakedModel {
    private static final ItemTransforms TRANSFORMS = identityTransforms();

    private final BakedModel delegate;

    private ObjMachineItemModel(BakedModel delegate) {
        this.delegate = delegate;
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        int replaced = 0;
        int missing = 0;
        Set<String> replacedIds = new LinkedHashSet<>();
        for (String id : ObjMachineItemRenderer.inventoryModelIds()) {
            int count = replace(models, id);
            if (count > 0) {
                replaced++;
                replacedIds.add(id);
            }
            if (models.get(new ModelResourceLocation(ReinhardtsHBM.id(id), ModelResourceLocation.INVENTORY_VARIANT)) == null) {
                missing++;
            }
        }
        // MiningLaserBlockItem owns its legacy placement behavior and its
        // dedicated renderer. It still needs a custom baked model marker or
        // Minecraft falls back to the builtin/entity placeholder.
        if (replace(models, "machine_mining_laser") > 0) {
            replaced++;
            replacedIds.add("machine_mining_laser");
        }
        if (models.get(new ModelResourceLocation(ReinhardtsHBM.id("machine_mining_laser"), ModelResourceLocation.INVENTORY_VARIANT)) == null) {
            missing++;
        }
        if (replaced > 0 || missing > 0) {
            ReinhardtsHBM.LOGGER.info("Installed complete OBJ item renderers for {} items ({} inventory models missing)", replaced, missing);
            if (!replacedIds.isEmpty()) {
                ReinhardtsHBM.LOGGER.info("OBJ item renderer routes: {}", replacedIds);
            }
            if (missing > 0) {
                ReinhardtsHBM.LOGGER.warn("OBJ item inventory models not found: {}", ObjMachineItemRenderer.missingInventoryModels(models));
            }
        }
    }

    private static int replace(Map<ModelResourceLocation, BakedModel> models, String id) {
        ModelResourceLocation location = new ModelResourceLocation(
                ReinhardtsHBM.id(id), ModelResourceLocation.INVENTORY_VARIANT
        );
        BakedModel model = models.get(location);
        if (model != null && !(model instanceof ObjMachineItemModel)) {
            models.put(location, new ObjMachineItemModel(model));
            return 1;
        }
        return 0;
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
        return TRANSFORMS;
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    private static ItemTransforms identityTransforms() {
        // ObjMachineItemRenderer centers and sizes every model itself. Applying
        // a second generic display transform here made every profile smaller
        // than its 1.7.10 inventory transform and pushed offsets out of sync.
        ItemTransform identity = transform(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        return new ItemTransforms(identity, identity, identity, identity, identity, identity, identity, identity);
    }

    private static ItemTransform transform(float rotX, float rotY, float rotZ, float x, float y, float z, float scale) {
        return new ItemTransform(
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(x, y, z),
                new Vector3f(scale, scale, scale)
        );
    }
}
