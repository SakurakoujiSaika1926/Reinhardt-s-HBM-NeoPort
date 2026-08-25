package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.entity.LegacyBomberEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Old RenderBomber transforms, using the original Dornier and B-29 OBJ resources. */
public final class LegacyBomberEntityRenderer extends EntityRenderer<LegacyBomberEntity> {
    private static final ModelResourceLocation DORNIER = MachineModelRenderer.standalone("entity/bomber_dornier");
    private static final ModelResourceLocation B29 = MachineModelRenderer.standalone("entity/bomber_b29");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyBomberEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DORNIER);
        event.register(B29);
    }

    @Override
    public void render(LegacyBomberEntity entity, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(entity.getYRot() - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(entity.getXRot()));
        poseStack.mulPose(Axis.XP.rotationDegrees((float) Math.sin((entity.tickCount + partialTick) * 0.05F) * 10.0F));
        boolean b29 = entity.style() >= 5;
        poseStack.scale(b29 ? 30.0F / 3.1F : 5.0F, b29 ? 30.0F / 3.1F : 5.0F, b29 ? 30.0F / 3.1F : 5.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(b29 ? 180.0F : -90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(b29 ? B29 : DORNIER), poseStack,
                bufferSource, RENDER_STATE, packedLight, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyBomberEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
