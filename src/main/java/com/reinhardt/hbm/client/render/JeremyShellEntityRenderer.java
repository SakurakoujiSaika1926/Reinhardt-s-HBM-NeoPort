package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.JeremyShellEntity;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

public class JeremyShellEntityRenderer extends EntityRenderer<JeremyShellEntity> {
    private static final ModelResourceLocation SHELL = MachineModelRenderer.standalone("entity/jeremy_shell");
    private static final BlockState RENDER_STATE = Blocks.STONE.defaultBlockState();

    public JeremyShellEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SHELL);
    }

    @Override
    public void render(JeremyShellEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float yaw = Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot());
        float pitch = Mth.lerp(partialTick, entity.xRotO, entity.getXRot());
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw - 90.0F));
        poseStack.mulPose(Axis.ZP.rotationDegrees(pitch + 180.0F));
        poseStack.scale(0.25F, 0.25F, 0.25F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SHELL), poseStack, bufferSource, RENDER_STATE, packedLight, 0);
        poseStack.popPose();
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(JeremyShellEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
