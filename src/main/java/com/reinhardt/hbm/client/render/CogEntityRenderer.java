package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.CogEntity;
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

public class CogEntityRenderer extends EntityRenderer<CogEntity> {
    private static final ModelResourceLocation COG = MachineModelRenderer.standalone("block/machine_stirling_cog");
    private static final ModelResourceLocation COG_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog");

    public CogEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(COG);
        event.register(COG_STEEL);
    }

    @Override
    public void render(CogEntity cog, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();

        switch (Math.floorMod(cog.orientation(), 6)) {
            case 5 -> poseStack.mulPose(yaw(90.0F));
            case 2 -> poseStack.mulPose(yaw(180.0F));
            case 4 -> poseStack.mulPose(yaw(270.0F));
            default -> {
            }
        }

        poseStack.translate(0.0D, 0.0D, -1.0D);
        if (cog.orientation() < 6) {
            float angle = ((cog.tickCount + partialTick) * 12.0F) % 360.0F;
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(angle), 0.0F, 0.0F, -1.0F)));
        }
        poseStack.translate(0.0D, -1.375D, 0.0D);

        BlockState state = cog.meta() == 0
                ? HbmBlocks.MACHINE_STIRLING.get().defaultBlockState()
                : HbmBlocks.MACHINE_STIRLING_STEEL.get().defaultBlockState();
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(cog.meta() == 0 ? COG : COG_STEEL),
                poseStack,
                bufferSource,
                state,
                packedLight,
                0
        );

        poseStack.popPose();
        super.render(cog, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(CogEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
