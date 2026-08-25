package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyTrainEntity;
import com.reinhardt.hbm.item.LegacyTrainItem;
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

/** Original cargo-tram and trailer OBJ bodies. */
public final class LegacyTrainEntityRenderer extends EntityRenderer<LegacyTrainEntity> {
    private static final ModelResourceLocation TRAM = MachineModelRenderer.standalone("entity/tram");
    private static final ModelResourceLocation TRAILER = MachineModelRenderer.standalone("entity/tram_trailer");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyTrainEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 2.5F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(TRAM);
        event.register(TRAILER);
    }

    @Override
    public void render(LegacyTrainEntity train, float yaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.125D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(180.0F - yaw), 0.0F, 1.0F, 0.0F)));
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(train.variant() == LegacyTrainItem.Type.CARGO_TRAM ? TRAM : TRAILER),
                poseStack, buffers, RENDER_STATE, packedLight, 0, texture(train), 0.0F, 0.0F);
        poseStack.popPose();
        super.render(train, yaw, partialTick, poseStack, buffers, packedLight);
    }

    private static ResourceLocation texture(LegacyTrainEntity train) {
        return ReinhardtsHBM.id(train.variant() == LegacyTrainItem.Type.CARGO_TRAM
                ? "textures/models/legacy/tram.png" : "textures/models/legacy/tram_trailer.png");
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyTrainEntity entity) {
        return texture(entity);
    }
}
