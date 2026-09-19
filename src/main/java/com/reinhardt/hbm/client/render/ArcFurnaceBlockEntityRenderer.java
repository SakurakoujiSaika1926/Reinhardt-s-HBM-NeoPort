package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.ArcFurnaceBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.List;

/** Renders the 1.7.10 arc furnace as one OBJ with the original object visibility and animations. */
public final class ArcFurnaceBlockEntityRenderer implements LongRangeBlockEntityRenderer<ArcFurnaceBlockEntity> {
    private static final ModelResourceLocation FURNACE = part("machine_arc_furnace");
    private static final ModelResourceLocation CONTENTS_HOT = part("machine_arc_furnace_contents_hot");
    private static final ModelResourceLocation CONTENTS_COLD = part("machine_arc_furnace_contents_cold");
    private static final ModelResourceLocation LID = part("machine_arc_furnace_lid");
    private static final ModelResourceLocation RING_1 = part("machine_arc_furnace_ring1");
    private static final ModelResourceLocation RING_2 = part("machine_arc_furnace_ring2");
    private static final ModelResourceLocation RING_3 = part("machine_arc_furnace_ring3");
    private static final ModelResourceLocation ELECTRODE_1 = part("machine_arc_furnace_electrode1");
    private static final ModelResourceLocation ELECTRODE_2 = part("machine_arc_furnace_electrode2");
    private static final ModelResourceLocation ELECTRODE_3 = part("machine_arc_furnace_electrode3");
    private static final ModelResourceLocation ELECTRODE_1_HOT = part("machine_arc_furnace_electrode1_hot");
    private static final ModelResourceLocation ELECTRODE_2_HOT = part("machine_arc_furnace_electrode2_hot");
    private static final ModelResourceLocation ELECTRODE_3_HOT = part("machine_arc_furnace_electrode3_hot");
    private static final ModelResourceLocation ELECTRODE_1_SHORT = part("machine_arc_furnace_electrode1_short");
    private static final ModelResourceLocation ELECTRODE_2_SHORT = part("machine_arc_furnace_electrode2_short");
    private static final ModelResourceLocation ELECTRODE_3_SHORT = part("machine_arc_furnace_electrode3_short");
    private static final ModelResourceLocation CABLE_1 = part("machine_arc_furnace_cable1");
    private static final ModelResourceLocation CABLE_2 = part("machine_arc_furnace_cable2");
    private static final ModelResourceLocation CABLE_3 = part("machine_arc_furnace_cable3");

    private static final List<ModelResourceLocation> MODELS = List.of(
            FURNACE, CONTENTS_HOT, CONTENTS_COLD, LID,
            RING_1, RING_2, RING_3,
            ELECTRODE_1, ELECTRODE_2, ELECTRODE_3,
            ELECTRODE_1_HOT, ELECTRODE_2_HOT, ELECTRODE_3_HOT,
            ELECTRODE_1_SHORT, ELECTRODE_2_SHORT, ELECTRODE_3_SHORT,
            CABLE_1, CABLE_2, CABLE_3
    );

    public ArcFurnaceBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        MODELS.forEach(event::register);
    }

    @Override
    public void render(
            ArcFurnaceBlockEntity furnace,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        BlockState state = furnace.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING)
                ? state.getValue(LargeMachineBlock.FACING)
                : Direction.SOUTH;
        long worldTime = furnace.getLevel() == null ? 0L : furnace.getLevel().getGameTime();

        poseStack.pushPose();
        // This is the same origin and yaw table used by RenderArcFurnace in 1.7.10.
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));

        render(FURNACE, furnace, poseStack, bufferSource, state, packedLight, packedOverlay);

        if (furnace.liquidAmount() > 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, -1.75F + furnace.liquidAmount() * 1.75F / ArcFurnaceBlockEntity.MAX_LIQUID, 0.0F);
            MachineModelRenderer.renderUnculledFullBright(
                    MachineModelRenderer.model(CONTENTS_HOT), poseStack, bufferSource, state, packedOverlay
            );
            poseStack.popPose();
        } else if (furnace.hasMaterial()) {
            render(CONTENTS_COLD, furnace, poseStack, bufferSource, state, packedLight, packedOverlay);
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F * furnace.lid(partialTick), 0.0F);
        if (furnace.working()) {
            poseStack.translate(0.0F, 0.0F, (float) Math.sin(worldTime + partialTick) * 0.005F);
        }

        render(LID, furnace, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderRingAndElectrode(0, RING_1, ELECTRODE_1, ELECTRODE_1_HOT, ELECTRODE_1_SHORT, furnace,
                poseStack, bufferSource, state, packedLight, packedOverlay);
        renderRingAndElectrode(1, RING_2, ELECTRODE_2, ELECTRODE_2_HOT, ELECTRODE_2_SHORT, furnace,
                poseStack, bufferSource, state, packedLight, packedOverlay);
        renderRingAndElectrode(2, RING_3, ELECTRODE_3, ELECTRODE_3_HOT, ELECTRODE_3_SHORT, furnace,
                poseStack, bufferSource, state, packedLight, packedOverlay);
        renderCable(0, CABLE_1, furnace, poseStack, bufferSource, state, packedLight, packedOverlay, worldTime, partialTick);
        renderCable(1, CABLE_2, furnace, poseStack, bufferSource, state, packedLight, packedOverlay, worldTime, partialTick);
        renderCable(2, CABLE_3, furnace, poseStack, bufferSource, state, packedLight, packedOverlay, worldTime, partialTick);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ArcFurnaceBlockEntity furnace) {
        // Exact 1.7.10 RenderArcFurnace bounds: x/z -3..+4 and y 0..+6.
        return new AABB(
                furnace.getBlockPos().getX() - 3.0D,
                furnace.getBlockPos().getY(),
                furnace.getBlockPos().getZ() - 3.0D,
                furnace.getBlockPos().getX() + 4.0D,
                furnace.getBlockPos().getY() + 6.0D,
                furnace.getBlockPos().getZ() + 4.0D
        );
    }

    private static void renderRingAndElectrode(
            int index,
            ModelResourceLocation ring,
            ModelResourceLocation fresh,
            ModelResourceLocation hot,
            ModelResourceLocation shorted,
            ArcFurnaceBlockEntity furnace,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        int electrodeState = furnace.electrodeState(index);
        if (electrodeState == 0) {
            return;
        }
        render(ring, furnace, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (electrodeState == 1) {
            render(fresh, furnace, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else if (electrodeState == 2) {
            MachineModelRenderer.renderUnculledFullBright(
                    MachineModelRenderer.model(hot), poseStack, bufferSource, state, packedOverlay
            );
        } else {
            MachineModelRenderer.renderUnculledFullBright(
                    MachineModelRenderer.model(shorted), poseStack, bufferSource, state, packedOverlay
            );
        }
    }

    private static void renderCable(
            int index,
            ModelResourceLocation cable,
            ArcFurnaceBlockEntity furnace,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay,
            long worldTime,
            float partialTick
    ) {
        if (furnace.electrodeState(index) == 0) {
            return;
        }
        poseStack.pushPose();
        float pivotZ = index == 0 ? 0.5F : index == 2 ? -0.5F : 0.0F;
        poseStack.translate(0.0F, 5.5F, pivotZ);
        if (furnace.working()) {
            poseStack.mulPose(MachineModelRenderer.xQuaternion((float) Math.sin((worldTime + partialTick) / 2.0F) * 30.0F));
        }
        poseStack.translate(0.0F, -5.5F, -pivotZ);
        render(cable, furnace, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void render(
            ModelResourceLocation model,
            ArcFurnaceBlockEntity furnace,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static ModelResourceLocation part(String path) {
        return MachineModelRenderer.standalone("block/" + path);
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 90.0F;
        };
    }
}
