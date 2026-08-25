package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.entity.LegacyDeliveryDroneEntity;
import com.reinhardt.hbm.entity.LegacyRequestDroneEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Uses the original three-part drone OBJ rather than a generated entity cube. */
public final class LegacyDroneEntityRenderer<T extends LegacyDeliveryDroneEntity> extends EntityRenderer<T> {
    private static final ModelResourceLocation DRONE = MachineModelRenderer.standalone("entity/drone");
    private static final ModelResourceLocation DRONE_EXPRESS = MachineModelRenderer.standalone("entity/drone_express");
    private static final ModelResourceLocation DRONE_REQUEST = MachineModelRenderer.standalone("entity/drone_request");
    private static final ModelResourceLocation CRATE = MachineModelRenderer.standalone("entity/drone_crate");
    private static final ModelResourceLocation BARREL = MachineModelRenderer.standalone("entity/drone_barrel");
    private static final BlockState RENDER_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public LegacyDroneEntityRenderer(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(DRONE);
        event.register(DRONE_EXPRESS);
        event.register(DRONE_REQUEST);
        event.register(CRATE);
        event.register(BARREL);
    }

    @Override
    public void render(T drone, float entityYaw, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(droneModel(drone)), poseStack, bufferSource,
                RENDER_STATE, packedLight, 0);
        if (drone.appearance() == 1) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRATE), poseStack, bufferSource,
                    RENDER_STATE, packedLight, 0);
        } else if (drone.appearance() == 2) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BARREL), poseStack, bufferSource,
                    RENDER_STATE, packedLight, 0);
        }
        poseStack.popPose();
        super.render(drone, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    private static ModelResourceLocation droneModel(LegacyDeliveryDroneEntity drone) {
        if (drone instanceof LegacyRequestDroneEntity) {
            return DRONE_REQUEST;
        }
        return drone.express() ? DRONE_EXPRESS : DRONE;
    }

    @Override
    public ResourceLocation getTextureLocation(LegacyDeliveryDroneEntity entity) {
        return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;
    }
}
