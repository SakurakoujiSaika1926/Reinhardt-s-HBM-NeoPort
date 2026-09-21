package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.PressBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class PressBlockEntityRenderer implements BlockEntityRenderer<PressBlockEntity> {
    private static final ModelResourceLocation PRESS_BODY = MachineModelRenderer.standalone("block/machine_press_body_world");
    private static final ModelResourceLocation PRESS_HEAD = MachineModelRenderer.standalone("block/machine_press_head_world");
    private static final ModelResourceLocation EPRESS_BODY = MachineModelRenderer.standalone("block/machine_epress_body_world");
    private static final ModelResourceLocation EPRESS_HEAD = MachineModelRenderer.standalone("block/machine_epress_head_world");

    public PressBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PRESS_BODY);
        event.register(PRESS_HEAD);
        event.register(EPRESS_BODY);
        event.register(EPRESS_HEAD);
    }

    @Override
    public void render(PressBlockEntity press, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = press.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        boolean electric = press.kind() == PressBlockEntity.Kind.ELECTRIC;
        float headTravel = press.clientHeadTravel(partialTick);

        poseStack.pushPose();
        orient(poseStack, facing, electric);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(electric ? EPRESS_BODY : PRESS_BODY), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        if (electric) {
            poseStack.translate(0.0F, 1.875F - headTravel, 0.0F);
        } else {
            poseStack.translate(0.0F, 0.875F - headTravel, 0.0F);
            poseStack.scale(0.95F, 1.0F, 0.95F);
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(electric ? EPRESS_HEAD : PRESS_HEAD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        renderInputStack(press.visualInput(), electric, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(PressBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(2.0D, 3.0D, 2.0D);
    }

    private static void orient(PoseStack poseStack, Direction facing, boolean electric) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        rotateY(poseStack, electric ? legacyEPressYaw(facing) : 180.0F);
    }

    private static void renderInputStack(
            ItemStack stack,
            boolean electric,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (stack.isEmpty() || stack.getItem() instanceof BlockItem) {
            return;
        }

        poseStack.pushPose();
        if (electric) {
            poseStack.translate(0.0F, 1.0F, 0.0F);
            rotateY(poseStack, 90.0F);
            rotateX(poseStack, -90.0F);
            poseStack.translate(1.0F, 1.0F - 0.0625F * 165.0F / 100.0F, 0.0F);
            poseStack.translate(-1.0F, -1.15F, 0.0F);
            poseStack.translate(0.0F, 0.125F, 0.0F);
        } else {
            poseStack.translate(0.0F, 1.0F, -1.0F);
            rotateY(poseStack, 180.0F);
            rotateX(poseStack, -90.0F);
            poseStack.translate(0.0F, 1.0F, 0.0F);
        }
        rotateY(poseStack, 180.0F);
        poseStack.scale(0.5F, 0.5F, 0.5F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                null,
                0
        );
        poseStack.popPose();
    }

    private static float legacyEPressYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }

    private static void rotateX(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F)));
    }

    private static void rotateY(PoseStack poseStack, float degrees) {
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F)));
    }
}
