package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.DigammaSpearEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class DigammaSpearEntityRenderer extends EntityRenderer<DigammaSpearEntity> {
    private static final ModelResourceLocation LANCE = MachineModelRenderer.standalone("entity/digamma_spear");
    private static final BlockState RENDER_STATE = Blocks.BEACON.defaultBlockState();

    public DigammaSpearEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(LANCE);
    }

    @Override
    public void render(DigammaSpearEntity spear, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 15.0D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0D), 1.0F, 0.0F, 0.0F)));
        poseStack.scale(2.0F, 2.0F, 2.0F);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(LANCE),
                poseStack,
                bufferSource,
                RENDER_STATE,
                0xF000F0,
                0
        );
        poseStack.popPose();
        super.render(spear, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(DigammaSpearEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
