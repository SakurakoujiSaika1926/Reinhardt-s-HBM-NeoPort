package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.registry.HbmItems;
import com.reinhardt.hbm.util.ArmorModHandler;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/** The original back-mounted coil attached to the animated player body. */
public final class TeslaBackLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("armor/back_tesla");

    public TeslaBackLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, AbstractClientPlayer player,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack chest = player.getInventory().getArmor(2);
        ItemStack[] mods = ArmorModHandler.pryMods(chest, player.registryAccess());
        if (mods.length <= ArmorModHandler.PLATE_ONLY || !mods[ArmorModHandler.PLATE_ONLY].is(HbmItems.BACK_TESLA.get())) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().body.translateAndRotate(poseStack);
        poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
    }
}
