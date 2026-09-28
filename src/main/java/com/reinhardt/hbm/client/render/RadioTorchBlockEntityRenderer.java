package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RadioTorchBlock;
import com.reinhardt.hbm.blockentity.RadioTorchBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

/** Exact per-variant rtty.obj world poses from the 1.7.10 RenderRTTY metadata table. */
public final class RadioTorchBlockEntityRenderer implements BlockEntityRenderer<RadioTorchBlockEntity> {
    private static final ModelResourceLocation SENDER_MODEL = model("sender");
    private static final ModelResourceLocation RECEIVER_MODEL = model("receiver");
    private static final ModelResourceLocation COUNTER_MODEL = model("counter");
    private static final ModelResourceLocation LOGIC_MODEL = model("logic");
    private static final ModelResourceLocation READER_MODEL = model("reader");
    private static final ModelResourceLocation CONTROLLER_MODEL = model("controller");

    private static final ResourceLocation SENDER_OFF = texture("rtty_sender_off");
    private static final ResourceLocation SENDER_ON = texture("rtty_sender_on");
    private static final ResourceLocation RECEIVER_OFF = texture("rtty_rec_off");
    private static final ResourceLocation RECEIVER_ON = texture("rtty_rec_on");
    private static final ResourceLocation COUNTER = texture("rtty_counter");
    private static final ResourceLocation LOGIC_OFF = texture("rtty_logic_off");
    private static final ResourceLocation LOGIC_ON = texture("rtty_logic_on");
    private static final ResourceLocation READER = texture("rtty_reader");
    private static final ResourceLocation CONTROLLER = texture("rtty_controller");

    public RadioTorchBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SENDER_MODEL);
        event.register(RECEIVER_MODEL);
        event.register(COUNTER_MODEL);
        event.register(LOGIC_MODEL);
        event.register(READER_MODEL);
        event.register(CONTROLLER_MODEL);
    }

    @Override
    public void render(RadioTorchBlockEntity radio, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = radio.getBlockState();
        Direction facing = state.getValue(RadioTorchBlock.FACING);
        switch (radio.kind()) {
            case SENDER -> renderSender(radio, state, facing, poseStack, bufferSource, packedLight, packedOverlay);
            case RECEIVER -> renderReceiver(radio, state, facing, poseStack, bufferSource, packedLight, packedOverlay);
            case COUNTER -> renderCounter(state, facing, poseStack, bufferSource, packedLight, packedOverlay);
            case LOGIC -> renderLogic(radio, state, facing, poseStack, bufferSource, packedLight, packedOverlay);
            case READER -> renderReader(state, facing, poseStack, bufferSource, packedLight, packedOverlay);
            case CONTROLLER -> renderController(state, facing, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderSender(RadioTorchBlockEntity radio, BlockState state, Direction facing,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applySenderPose(poseStack, facing);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(SENDER_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay,
                radio.signal() > 0 ? SENDER_ON : SENDER_OFF, 0.0F, 0.0F);
        poseStack.popPose();
    }

    private static void renderReceiver(RadioTorchBlockEntity radio, BlockState state, Direction facing,
                                       PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyReceiverPose(poseStack, facing);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(RECEIVER_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay,
                radio.signal() > 0 ? RECEIVER_ON : RECEIVER_OFF, 0.0F, 0.0F);
        poseStack.popPose();
    }

    private static void renderCounter(BlockState state, Direction facing, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyCounterPose(poseStack, facing);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(COUNTER_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay, COUNTER, 0.0F, 0.0F);
        poseStack.popPose();
    }

    private static void renderLogic(RadioTorchBlockEntity radio, BlockState state, Direction facing,
                                    PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyLogicPose(poseStack, facing);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(LOGIC_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay,
                radio.signal() > 0 ? LOGIC_ON : LOGIC_OFF, 0.0F, 0.0F);
        poseStack.popPose();
    }

    private static void renderReader(BlockState state, Direction facing, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyReaderPose(poseStack, facing);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(READER_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay, READER, 0.0F, 0.0F);
        poseStack.popPose();
    }

    private static void renderController(BlockState state, Direction facing, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyControllerPose(poseStack, facing);
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(CONTROLLER_MODEL), poseStack,
                bufferSource, state, packedLight, packedOverlay, CONTROLLER, 0.0F, 0.0F);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RadioTorchBlockEntity radio) {
        Direction facing = radio.getBlockState().getValue(RadioTorchBlock.FACING);
        return switch (radio.kind()) {
            case SENDER -> senderBounds(radio.getBlockPos(), facing);
            case RECEIVER -> receiverBounds(radio.getBlockPos(), facing);
            case COUNTER -> counterBounds(radio.getBlockPos(), facing);
            case LOGIC -> logicBounds(radio.getBlockPos(), facing);
            case READER -> readerBounds(radio.getBlockPos(), facing);
            case CONTROLLER -> controllerBounds(radio.getBlockPos(), facing);
        };
    }

    private static AABB senderBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applySenderPose(poseStack, facing);
        return rttyBounds(pos, poseStack);
    }

    private static AABB receiverBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyReceiverPose(poseStack, facing);
        return rttyBounds(pos, poseStack);
    }

    private static AABB counterBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyCounterPose(poseStack, facing);
        return rttyBounds(pos, poseStack);
    }

    private static AABB logicBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyLogicPose(poseStack, facing);
        return rttyBounds(pos, poseStack);
    }

    private static AABB readerBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyReaderPose(poseStack, facing);
        return rttyBounds(pos, poseStack);
    }

    private static AABB controllerBounds(BlockPos pos, Direction facing) {
        PoseStack poseStack = new PoseStack();
        applyControllerPose(poseStack, facing);
        return rttyBounds(pos, poseStack);
    }

    private static AABB rttyBounds(BlockPos pos, PoseStack poseStack) {
        // Exact min/max of models/obj/blocks/rtty.obj; the same baked-float
        // envelope is transformed by the variant's actual world PoseStack.
        return RenderBounds.transformedBox(pos, poseStack,
                -0.125F, -0.5F, -0.125F, 0.125F, 0.125F, 0.125F);
    }

    private static void applySenderPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applyReceiverPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applyCounterPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applyLogicPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applyReaderPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static void applyControllerPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.5D, 0.5D);
        switch (facing) {
            case DOWN -> poseStack.mulPose(Axis.ZP.rotationDegrees(180.0F));
            case UP -> { }
            case NORTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case SOUTH -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case WEST -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }
            case EAST -> poseStack.mulPose(Axis.ZP.rotationDegrees(-90.0F));
        }
    }

    private static ModelResourceLocation model(String kind) {
        return MachineModelRenderer.standalone("block/radio_torch_" + kind);
    }

    private static ResourceLocation texture(String name) {
        return ReinhardtsHBM.id("textures/block/" + name + ".png");
    }
}
