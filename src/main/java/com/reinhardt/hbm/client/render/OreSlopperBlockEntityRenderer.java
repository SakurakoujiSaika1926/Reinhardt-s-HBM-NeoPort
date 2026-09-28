package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.OreSlopperBlock;
import com.reinhardt.hbm.blockentity.OreSlopperBlockEntity;
import com.reinhardt.hbm.item.BedrockOreItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class OreSlopperBlockEntityRenderer implements LongRangeBlockEntityRenderer<OreSlopperBlockEntity> {
    private static volatile ItemStack renderedLiftedOre = ItemStack.EMPTY;
    private static volatile AABB[] liftedOreBounds = emptyLiftedOreBounds();
    private static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_ore_slopper_base");
    private static final ModelResourceLocation SLIDER = MachineModelRenderer.standalone("block/machine_ore_slopper_slider");
    private static final ModelResourceLocation HYDRAULICS = MachineModelRenderer.standalone("block/machine_ore_slopper_hydraulics");
    private static final ModelResourceLocation BUCKET = MachineModelRenderer.standalone("block/machine_ore_slopper_bucket");
    private static final ModelResourceLocation BLADES_LEFT = MachineModelRenderer.standalone("block/machine_ore_slopper_blades_left");
    private static final ModelResourceLocation BLADES_RIGHT = MachineModelRenderer.standalone("block/machine_ore_slopper_blades_right");
    private static final ModelResourceLocation FAN = MachineModelRenderer.standalone("block/machine_ore_slopper_fan");
    private static final AABB[] MACHINE_BOUNDS = buildOreSlopperBounds();

    public OreSlopperBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        rebuildLiftedOreGeometryAfterBake();
    }

    static void rebuildLiftedOreGeometryAfterBake() {
        ItemStack candidate = BedrockOreItem.stackFor(HbmItems.BEDROCK_ORE_NEW,
                BedrockOreItem.Grade.BASE, BedrockOreItem.Type.LIGHT_METAL);
        try {
            if (Minecraft.getInstance().getItemRenderer().getModel(candidate, null, null, 0).isCustomRenderer()) {
                renderedLiftedOre = ItemStack.EMPTY;
                liftedOreBounds = emptyLiftedOreBounds();
                return;
            }
        } catch (RuntimeException malformedModel) {
            renderedLiftedOre = ItemStack.EMPTY;
            liftedOreBounds = emptyLiftedOreBounds();
            return;
        }
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            AABB envelope = null;
            for (float slide : new float[]{0.0F, -3.0F}) {
                for (float bucketY : new float[]{0.0F, -2.5F}) {
                    AABB candidateBounds = captureLiftedOre(candidate, facing, slide, bucketY);
                    envelope = envelope == null ? candidateBounds : envelope.minmax(candidateBounds);
                }
            }
            bounds[facing.ordinal()] = envelope;
        }
        renderedLiftedOre = candidate.copy();
        liftedOreBounds = bounds;
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(SLIDER);
        event.register(HYDRAULICS);
        event.register(BUCKET);
        event.register(BLADES_LEFT);
        event.register(BLADES_RIGHT);
        event.register(FAN);
    }

    @Override
    public void render(OreSlopperBlockEntity slopper, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = slopper.getBlockState();
        Direction facing = oreSlopperFacing(state);

        poseStack.pushPose();
        applyOreSlopperTransform(poseStack, facing);

        renderPart(BASE, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        float slide = slopper.slider(partialTick);
        poseStack.translate(0.0F, 0.0F, slide * -3.0F);
        renderPart(SLIDER, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        float extend = slopper.bucket(partialTick) * 1.5F;
        poseStack.translate(0.0F, -Mth.clamp(extend - 0.25F, 0.0F, 1.25F), 0.0F);
        renderPart(HYDRAULICS, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, -Mth.clamp(extend, 0.0F, 1.25F), 0.0F);
        renderPart(BUCKET, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (slopper.clientLifting()) {
            renderLiftedOre(renderedLiftedOre, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
        poseStack.popPose();

        float blades = slopper.blades(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.375F, 2.75F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(blades));
        poseStack.translate(-0.375F, -2.75F, 0.0F);
        renderPart(BLADES_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(-0.375F, 2.75F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-blades));
        poseStack.translate(0.375F, -2.75F, 0.0F);
        renderPart(BLADES_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.875F, -1.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-slopper.fan(partialTick)));
        poseStack.translate(0.0F, -1.875F, 1.0F);
        renderPart(FAN, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(OreSlopperBlockEntity blockEntity) {
        Direction facing = oreSlopperFacing(blockEntity.getBlockState());
        /*
         * Face-used union after every legal animation extreme:
         * base X -1.982091..1.5 and Z -3.5..3.5; slider Y reaches 5.8125;
         * bucket translations reach -2.5 Y and -3 Z; full blade/fan rotations
         * and the 1.75x lifted generated-item model remain inside that union.
         */
        AABB machine = MACHINE_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
        AABB item = liftedOreBounds[facing.ordinal()].move(blockEntity.getBlockPos());
        return machine.minmax(item);
    }

    private static void renderLiftedOre(ItemStack stack, PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        poseStack.translate(0.0625F, 4.3125F, 2.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.mulPose(Axis.XP.rotationDegrees(-90.0F));
        poseStack.scale(1.75F, 1.75F, 1.75F);
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

    private static AABB captureLiftedOre(ItemStack stack, Direction facing, float slide, float bucketY) {
        PoseStack poseStack = new PoseStack();
        applyOreSlopperTransform(poseStack, facing);
        poseStack.translate(0.0F, bucketY, slide);
        GeometryCaptureBufferSource capture = new GeometryCaptureBufferSource();
        renderLiftedOre(stack, poseStack, capture, 0, 0);
        return capture.bounds(BlockPos.ZERO);
    }

    private static AABB[] emptyLiftedOreBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        java.util.Arrays.fill(bounds, new AABB(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D));
        return bounds;
    }

    private static AABB[] buildOreSlopperBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyOreSlopperTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    BlockPos.ZERO, poseStack,
                    -1.982091F, 0.0F, -3.5F, 1.5F, 5.8125F, 3.5F);
        }
        return bounds;
    }

    private static void applyOreSlopperTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> { }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Ore slopper facing must be horizontal: " + facing);
        }
    }

    private static void renderPart(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static Direction oreSlopperFacing(BlockState state) {
        if (!state.hasProperty(OreSlopperBlock.FACING)) {
            throw new IllegalStateException("Ore slopper block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(OreSlopperBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Ore slopper block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
