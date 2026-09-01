package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.BrokenItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Restores BrokenItem's two legacy render passes: original item, then damage overlay. */
public final class BrokenItemBakedModel implements IDynamicBakedModel {
    private final BakedModel overlay;

    private BrokenItemBakedModel(BakedModel overlay) {
        this.overlay = overlay;
    }

    public static void replaceModel(Map<ModelResourceLocation, BakedModel> models) {
        ModelResourceLocation location = ModelResourceLocation.inventory(ReinhardtsHBM.id("broken_item"));
        BakedModel model = models.get(location);
        if (model != null && !(model instanceof BrokenItemBakedModel)) {
            models.put(location, new BrokenItemBakedModel(model));
        }
    }

    @Override
    public List<BakedModel> getRenderPasses(ItemStack stack, boolean fabulous) {
        ItemStack original = BrokenItem.originalStack(stack);
        if (original.isEmpty() || original.getItem() instanceof BrokenItem) {
            return List.of(this.overlay);
        }

        BakedModel originalModel = Minecraft.getInstance().getItemRenderer().getModel(original, null, null, 0);
        List<BakedModel> passes = new ArrayList<>(originalModel.getRenderPasses(original, fabulous));
        passes.add(this.overlay);
        return List.copyOf(passes);
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource random,
                                    ModelData modelData, @Nullable RenderType renderType) {
        return this.overlay.getQuads(state, side, random, modelData, renderType);
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.overlay.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.overlay.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.overlay.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return false;
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.overlay.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        return this.overlay.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.overlay.getOverrides();
    }
}
