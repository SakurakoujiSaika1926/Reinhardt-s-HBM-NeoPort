package com.reinhardt.hbm.client.render;

import com.reinhardt.hbm.block.DecoCrtBlock;
import com.reinhardt.hbm.block.DecoModelBlock;
import com.reinhardt.hbm.blockentity.DecoDisplayBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact world-space transforms from RenderBlockDecoModel and RenderCRT. */
public final class DecoDisplayBlockEntityRenderer implements BlockEntityRenderer<DecoDisplayBlockEntity> {
    private static final ModelResourceLocation COMPUTER = model("block/deco_computer");
    private static final ModelResourceLocation[] CRT_MONITOR = {
            model("block/deco_crt_clean_monitor"),
            model("block/deco_crt_broken_monitor"),
            model("block/deco_crt_blinking_monitor"),
            model("block/deco_crt_bsod_monitor")
    };
    private static final ModelResourceLocation[] CRT_SCREEN = {
            model("block/deco_crt_clean_screen"),
            model("block/deco_crt_broken_screen"),
            model("block/deco_crt_blinking_screen"),
            model("block/deco_crt_bsod_screen")
    };

    public DecoDisplayBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(COMPUTER);
        for (ModelResourceLocation model : CRT_MONITOR) {
            event.register(model);
        }
        for (ModelResourceLocation model : CRT_SCREEN) {
            event.register(model);
        }
    }

    @Override
    public void render(DecoDisplayBlockEntity display, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = display.getBlockState();
        if (state.is(HbmBlocks.DECO_COMPUTER.get())) {
            poseStack.pushPose();
            applyDecoComputerPose(poseStack, decoComputerFacing(state));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(COMPUTER), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (state.is(HbmBlocks.DECO_CRT.get())) {
            poseStack.pushPose();
            applyDecoCrtPose(poseStack, decoCrtFacing(state));
            int variant = Math.max(0, Math.min(CRT_MONITOR.length - 1, state.getValue(DecoCrtBlock.VARIANT)));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRT_MONITOR[variant]), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
            if (state.getValue(DecoCrtBlock.LIT)) {
                MachineModelRenderer.renderUnculledFullBright(MachineModelRenderer.model(CRT_SCREEN[variant]), poseStack,
                        bufferSource, state, packedOverlay);
            } else {
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(CRT_SCREEN[variant]), poseStack,
                        bufferSource, state, packedLight, packedOverlay);
            }
            poseStack.popPose();
        }
    }

    private static ModelResourceLocation model(String path) {
        return MachineModelRenderer.standalone(path);
    }

    private static void applyDecoComputerPose(PoseStack poseStack, net.minecraft.core.Direction facing) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (facing) {
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Decorative computer facing must be horizontal: " + facing);
        }
    }

    private static void applyDecoCrtPose(PoseStack poseStack, net.minecraft.core.Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        // RenderCRT metadata 0/1/2/3 maps to NORTH/EAST/WEST/SOUTH;
        // its inventory/world rotations are 90/0/270/180 degrees.
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Decorative CRT facing must be horizontal: " + facing);
        }
    }

    private static net.minecraft.core.Direction decoComputerFacing(BlockState state) {
        if (!state.hasProperty(DecoModelBlock.FACING)) {
            throw new IllegalStateException("Decorative computer state is missing its facing property: " + state);
        }
        net.minecraft.core.Direction facing = state.getValue(DecoModelBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Decorative computer state has a vertical facing: " + facing);
        }
        return facing;
    }

    private static net.minecraft.core.Direction decoCrtFacing(BlockState state) {
        if (!state.hasProperty(DecoCrtBlock.FACING)) {
            throw new IllegalStateException("Decorative CRT state is missing its facing property: " + state);
        }
        net.minecraft.core.Direction facing = state.getValue(DecoCrtBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Decorative CRT state has a vertical facing: " + facing);
        }
        return facing;
    }
}
