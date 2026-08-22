package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.SoyuzCapsuleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SoyuzCapsuleEntityRenderer extends EntityRenderer<SoyuzCapsuleEntity> {
    private static final ModelResourceLocation MODEL = MachineModelRenderer.standalone("entity/soyuz_lander");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public SoyuzCapsuleEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(SoyuzCapsuleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        double time = entity.level().getGameTime() + partialTick;
        float swayZ = Mth.sin((float) (time * 0.05D)) * 5.0F;
        float swayX = Mth.sin((float) (time * 0.05D + Math.PI * 0.5D)) * 5.0F;
        poseStack.translate(0.0D, 7.0D, 0.0D);
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(swayZ));
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(swayX));
        poseStack.translate(0.0D, -7.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MODEL), poseStack, bufferSource, RENDER_STATE, packedLight, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SoyuzCapsuleEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
