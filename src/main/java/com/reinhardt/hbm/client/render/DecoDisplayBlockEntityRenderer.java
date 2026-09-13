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
            poseStack.translate(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(computerRotation(state.getValue(DecoModelBlock.FACING))));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(COMPUTER), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (state.is(HbmBlocks.DECO_CRT.get())) {
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.0F, 0.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(crtRotation(state.getValue(DecoCrtBlock.FACING))));
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

    private static float computerRotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 180.0F;
        };
    }

    private static float crtRotation(net.minecraft.core.Direction facing) {
        return switch (facing) {
            // RenderCRT metadata 0/1/2/3 maps to NORTH/EAST/WEST/SOUTH;
            // its inventory/world rotations are 90/0/270/180 degrees.
            case NORTH -> 90.0F;
            case EAST -> 0.0F;
            case WEST -> 270.0F;
            default -> 180.0F;
        };
    }
}
