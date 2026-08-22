package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.ConveyorPressBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

/** Exact object groups and metadata rotation table from RenderConveyorPress. */
public final class ConveyorPressBlockEntityRenderer implements BlockEntityRenderer<ConveyorPressBlockEntity> {
    static final ModelResourceLocation PRESS = MachineModelRenderer.standalone("block/machine_conveyor_press_world");
    static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/machine_conveyor_press_piston");
    static final ModelResourceLocation BELT = MachineModelRenderer.standalone("block/machine_conveyor_press_belt");
    private static final ResourceLocation BELT_TEXTURE = ReinhardtsHBM.id("textures/models/machines/conveyor_press_belt.png");

    public ConveyorPressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PRESS);
        event.register(PISTON);
        event.register(BELT);
    }

    @Override
    public void render(
            ConveyorPressBlockEntity press,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = press.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING)
                : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(conveyorPressYaw(facing)));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PRESS), poseStack, bufferSource, state, packedLight, packedOverlay);

        // 1.7.10 only rendered the movable piston when a stamp was installed.
        if (press.hasStamp()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, -press.press(partialTick) * 0.75D, 0.0D);
            MachineModelRenderer.renderUnculled(
                    MachineModelRenderer.model(PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }

        long worldTime = press.getLevel() == null ? 0L : press.getLevel().getGameTime();
        float beltVOffset = ((worldTime % 16L) - 2L) / 16.0F;
        MachineModelRenderer.renderUnculledUv(
                MachineModelRenderer.model(BELT), poseStack, bufferSource, state, packedLight, packedOverlay,
                BELT_TEXTURE, 0.0F, beltVOffset);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ConveyorPressBlockEntity press) {
        // TileEntityConveyorPress#getRenderBoundingBox.
        return new AABB(press.getBlockPos()).inflate(1.0D, 0.0D, 1.0D).expandTowards(0.0D, 2.0D, 0.0D);
    }

    static void renderItemParts(
            BlockState state,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PRESS), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(BELT), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float conveyorPressYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            case EAST -> 0.0F;
            default -> 0.0F;
        };
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }
}
