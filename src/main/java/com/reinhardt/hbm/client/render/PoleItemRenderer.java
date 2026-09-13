package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Direct item paths from ItemRenderPoleTop and ItemRenderSatelliteReceiver. */
public final class PoleItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ModelResourceLocation SATELLITE_INVENTORY = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("item/pole_satellite_receiver_inventory"));

    private final boolean satellite;
    private LegacyPoleTopModel poleTop;
    private LegacyPoleSatelliteReceiverModel satelliteReceiver;

    public PoleItemRenderer(boolean satellite) {
        // NeoForge creates item extensions before the client model set exists.
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), null);
        this.satellite = satellite;
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SATELLITE_INVENTORY);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        if (satellite && context == ItemDisplayContext.GUI) {
            MachineModelRenderer.renderUnculledCutoutNoCull(
                    MachineModelRenderer.model(SATELLITE_INVENTORY), poseStack, bufferSource,
                    HbmBlocks.POLE_SATELLITE_RECEIVER.get().defaultBlockState(),
                    packedLight, packedOverlay);
            return;
        }
        ensureModels();
        PoleBlockEntityRenderer.renderItem(satellite, context, poseStack, bufferSource,
                packedLight, packedOverlay, poleTop, satelliteReceiver);
    }

    private void ensureModels() {
        if (poleTop != null && satelliteReceiver != null) {
            return;
        }
        var entityModels = Minecraft.getInstance().getEntityModels();
        if (entityModels == null) {
            throw new IllegalStateException("Pole item renderer has no client entity model set");
        }
        poleTop = new LegacyPoleTopModel(entityModels.bakeLayer(LegacyPoleTopModel.LAYER));
        satelliteReceiver = new LegacyPoleSatelliteReceiverModel(
                entityModels.bakeLayer(LegacyPoleSatelliteReceiverModel.LAYER));
    }
}
