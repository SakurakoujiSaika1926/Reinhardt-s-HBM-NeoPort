package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.SawbladeEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Direct transform port of RenderSawblade. */
public final class SawbladeEntityRenderer extends EntityRenderer<SawbladeEntity> {
    private static final ModelResourceLocation BLADE = MachineModelRenderer.standalone("block/machine_sawmill_blade");

    public SawbladeEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BLADE);
    }

    @Override
    public void render(SawbladeEntity blade, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        switch (Math.floorMod(blade.orientation(), 6)) {
            case 5 -> poseStack.mulPose(yaw(90.0F));
            case 2 -> poseStack.mulPose(yaw(180.0F));
            case 4 -> poseStack.mulPose(yaw(270.0F));
            default -> {
            }
        }
        poseStack.translate(0.0D, 0.0D, -1.0D);
        if (blade.orientation() < 6) {
            float angle = ((blade.tickCount + partialTick) * 12.0F) % 360.0F;
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(angle), 0.0F, 0.0F, -1.0F)));
        }
        poseStack.translate(0.0D, -1.375D, 0.0D);
        BlockState state = HbmBlocks.MACHINE_SAWMILL.get().defaultBlockState();
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BLADE), poseStack, bufferSource, state, packedLight, 0);
        poseStack.popPose();
        super.render(blade, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(SawbladeEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
