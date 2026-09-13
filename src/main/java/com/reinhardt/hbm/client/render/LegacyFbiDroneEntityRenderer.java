package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyFbiDroneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.Random;

/** FBI Drone uses the original quadcopter OBJ, not a humanoid placeholder. */
public final class LegacyFbiDroneEntityRenderer extends EntityRenderer<LegacyFbiDroneEntity> {
    private static final ModelResourceLocation MODEL = ModelResourceLocation.standalone(
            ReinhardtsHBM.id("entity/quadcopter"));
    private static final BlockState STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyFbiDroneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(MODEL);
    }

    @Override
    public void render(LegacyFbiDroneEntity entity, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F, 0.0F);
        // RenderDrone seeded this fixed yaw from the entity id on every frame.
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (new Random(entity.getId()).nextDouble() * 360.0D)));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(MODEL), poseStack, bufferSource,
                STATE, packedLight, OverlayTexture.NO_OVERLAY,
                ReinhardtsHBM.id("textures/entity/quadcopter.png"), 0.0F, 0.0F);
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyFbiDroneEntity entity) {
        return ReinhardtsHBM.id("textures/entity/quadcopter.png");
    }
}
