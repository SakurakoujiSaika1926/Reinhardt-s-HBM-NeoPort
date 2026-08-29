package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** 3D item renderer for the legacy pole models; never falls back to a flat icon. */
public final class PoleItemRenderer extends BlockEntityWithoutLevelRenderer {
    private final boolean satellite;
    private LegacyPoleTopModel poleTop;
    private LegacyPoleSatelliteReceiverModel satelliteReceiver;

    public PoleItemRenderer(boolean satellite) {
        // NeoForge creates item extensions before the client model set exists.
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), null);
        this.satellite = satellite;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (!ensureModels()) {
            return;
        }
        PoleBlockEntityRenderer.renderItem(satellite, Direction.SOUTH, poseStack, bufferSource,
                packedLight, packedOverlay, poleTop, satelliteReceiver);
    }

    private boolean ensureModels() {
        if (poleTop != null && satelliteReceiver != null) {
            return true;
        }
        var entityModels = Minecraft.getInstance().getEntityModels();
        if (entityModels == null) {
            return false;
        }
        poleTop = new LegacyPoleTopModel(entityModels.bakeLayer(LegacyPoleTopModel.LAYER));
        satelliteReceiver = new LegacyPoleSatelliteReceiverModel(
                entityModels.bakeLayer(LegacyPoleSatelliteReceiverModel.LAYER));
        return true;
    }
}
