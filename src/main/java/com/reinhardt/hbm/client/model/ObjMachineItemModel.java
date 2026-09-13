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
    /*
     * RenderPipe is an ISBRH block renderer in 1.7.10, so its inventory pass
     * was surrounded by the normal block-item isometric transform supplied by
     * RenderDecoItem.  The OBJ machine routes otherwise use identity because
     * their BEWLR applies ItemRenderBase itself; pipes are the one explicit
     * exception and must retain the vanilla block-item pose.
     */
    private static final ItemTransforms DECORATIVE_PIPE_TRANSFORMS = decorativePipeTransforms();

    private final BakedModel delegate;
    private final ItemTransforms transforms;

    private ObjMachineItemModel(BakedModel delegate, String itemId) {
        this.delegate = delegate;
        this.transforms = itemId.startsWith("deco_pipe")
                ? DECORATIVE_PIPE_TRANSFORMS
                : TRANSFORMS;
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
            models.put(location, new ObjMachineItemModel(model, id));
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
        return this.transforms;
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

    private static ItemTransforms decorativePipeTransforms() {
        // Exact modern equivalent of the ordinary 3D block-item display pose
        // used around RenderPipe#renderInventoryBlock.  This is a per-profile
        // transform, not a shared geometry offset or measured auto-fit.
        ItemTransform gui = transform(30.0F, 225.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.625F);
        ItemTransform thirdPerson = transform(75.0F, 45.0F, 0.0F,
                0.0F, 2.5F / 16.0F, 0.0F, 0.375F);
        ItemTransform firstPerson = transform(0.0F, 45.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.4F);
        ItemTransform ground = transform(0.0F, 0.0F, 0.0F,
                0.0F, 3.0F / 16.0F, 0.0F, 0.25F);
        ItemTransform head = transform(0.0F, 0.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 1.0F);
        ItemTransform fixed = transform(0.0F, 180.0F, 0.0F,
                0.0F, 0.0F, 0.0F, 0.5F);
        return new ItemTransforms(thirdPerson, thirdPerson, firstPerson, firstPerson,
                head, gui, ground, fixed);
    }

    private static ItemTransform transform(float rotX, float rotY, float rotZ, float x, float y, float z, float scale) {
        return new ItemTransform(
                new Vector3f(rotX, rotY, rotZ),
                new Vector3f(x, y, z),
                new Vector3f(scale, scale, scale)
        );
    }
}
