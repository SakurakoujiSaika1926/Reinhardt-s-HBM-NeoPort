package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.AmmoPressBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class AmmoPressBlockEntityRenderer implements BlockEntityRenderer<AmmoPressBlockEntity> {
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_ammo_press_frame_world");
    private static final ModelResourceLocation PRESS = MachineModelRenderer.standalone("block/machine_ammo_press_press_world");
    private static final ModelResourceLocation SHELLS = MachineModelRenderer.standalone("block/machine_ammo_press_shells_world");
    private static final ModelResourceLocation BULLETS = MachineModelRenderer.standalone("block/machine_ammo_press_bullets_world");

    public AmmoPressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(net.neoforged.neoforge.client.event.ModelEvent.RegisterAdditional event) {
        event.register(FRAME);
        event.register(PRESS);
        event.register(SHELLS);
        event.register(BULLETS);
    }

    @Override
    public void render(AmmoPressBlockEntity ammoPress, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = ammoPress.getBlockState();
        Direction facing = state.getValue(LargeMachineBlock.FACING);
        float lift = ammoPress.lift(partialTick);
        float press = ammoPress.press(partialTick);

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(legacyAmmoPressYaw(facing)), 0.0F, 1.0F, 0.0F)));

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, -press * 0.25F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRESS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, lift * 0.5F - 0.5F, 0.0F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SHELLS), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (ammoPress.showBullets()) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BULLETS), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(AmmoPressBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 2.0D, 2.0D);
    }

    // Directly transcribed from RenderAmmoPress after mapping its legacy metadata to the modern facing state.
    private static float legacyAmmoPressYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }
}
