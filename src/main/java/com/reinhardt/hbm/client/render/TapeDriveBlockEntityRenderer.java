package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.TapeDriveBlock;
import com.reinhardt.hbm.blockentity.TapeDriveBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Literal RenderTapeDrive frame, twelve drive models and untextured status lights. */
public final class TapeDriveBlockEntityRenderer implements BlockEntityRenderer<TapeDriveBlockEntity> {
    private static final ModelResourceLocation FRAME = MachineModelRenderer.standalone("block/machine_tape_drive_frame");
    private static final ModelResourceLocation DRIVE = MachineModelRenderer.standalone("block/machine_tape_drive_drive");
    private static final ModelResourceLocation LIGHT = MachineModelRenderer.standalone("block/machine_tape_drive_light");

    public TapeDriveBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(FRAME);
        event.register(DRIVE);
        event.register(LIGHT);
    }

    @Override
    public void render(TapeDriveBlockEntity drive, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int packedLight, int packedOverlay) {
        BlockState state = drive.getBlockState();
        if (!state.is(HbmBlocks.MACHINE_TAPE_DRIVE.get()) || !state.hasProperty(TapeDriveBlock.FACING)) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(
                TapeDriveBlock.legacyRotation(state.getValue(TapeDriveBlock.FACING))));
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(FRAME), poseStack, buffers, state, packedLight, packedOverlay);

        for (int slot = 0; slot < TapeDriveBlockEntity.SLOT_COUNT; slot++) {
            byte type = drive.visualSlot(slot);
            if (type == TapeDriveBlockEntity.SLOT_EMPTY) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.25F - 0.5F * (slot / 6),
                    0.3125F - (slot % 6) * 0.125F);
            MachineModelRenderer.renderUnculled(
                    MachineModelRenderer.model(DRIVE), poseStack, buffers, state, packedLight, packedOverlay);
            MachineModelRenderer.renderUnculledTintedLightning(
                    MachineModelRenderer.model(LIGHT), poseStack, buffers, state, packedOverlay, lightColor(type));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** Literal ItemRenderBase/RenderTapeDrive inventory and held-item transform. */
    public static void renderItem(ItemDisplayContext context, PoseStack poseStack, MultiBufferSource buffers,
                                  int packedLight, int packedOverlay) {
        BlockState state = HbmBlocks.MACHINE_TAPE_DRIVE.get().defaultBlockState();
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
        MachineModelRenderer.renderUnculled(
                MachineModelRenderer.model(FRAME), poseStack, buffers, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(TapeDriveBlockEntity drive) {
        BlockState state = drive.getBlockState();
        if (!state.is(HbmBlocks.MACHINE_TAPE_DRIVE.get()) || !state.hasProperty(TapeDriveBlock.FACING)) {
            throw new IllegalStateException("Tape-drive renderer received an invalid block state: " + state);
        }
        Direction facing = state.getValue(TapeDriveBlock.FACING);
        return (switch (facing) {
            case NORTH -> new AABB(0.0D, 0.0D, 0.25D, 1.0D, 1.0D, 1.0D);
            case SOUTH -> new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 0.75D);
            case WEST -> new AABB(0.25D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);
            case EAST -> new AABB(0.0D, 0.0D, 0.0D, 0.75D, 1.0D, 1.0D);
            case UP, DOWN -> throw new IllegalStateException("Tape-drive facing is vertical: " + facing);
        }).move(drive.getBlockPos());
    }

    private static int lightColor(byte type) {
        return switch (type) {
            case TapeDriveBlockEntity.SLOT_ANY -> 0xFFFF0000;
            case TapeDriveBlockEntity.SLOT_EMPTY_DRIVE -> 0xFFFFBF00;
            case TapeDriveBlockEntity.SLOT_FILLED_DRIVE -> 0xFF00FF00;
            default -> 0xFFFFFFFF;
        };
    }
}
