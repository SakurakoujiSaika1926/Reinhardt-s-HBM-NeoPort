package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.StirlingGeneratorBlock;
import com.reinhardt.hbm.blockentity.StirlingGeneratorBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

public class StirlingGeneratorBlockEntityRenderer implements LongRangeBlockEntityRenderer<StirlingGeneratorBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final ModelResourceLocation WORLD = MachineModelRenderer.standalone("block/machine_stirling_world");
    private static final ModelResourceLocation COG = MachineModelRenderer.standalone("block/machine_stirling_cog");
    private static final ModelResourceLocation COG_SMALL = MachineModelRenderer.standalone("block/machine_stirling_cog_small");
    private static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/machine_stirling_piston");
    private static final ModelResourceLocation WORLD_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_world");
    private static final ModelResourceLocation COG_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog");
    private static final ModelResourceLocation COG_SMALL_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_cog_small");
    private static final ModelResourceLocation PISTON_STEEL = MachineModelRenderer.standalone("block/machine_stirling_steel_piston");
    private static final ModelResourceLocation WORLD_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_world");
    private static final ModelResourceLocation COG_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_cog");
    private static final ModelResourceLocation COG_SMALL_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_cog_small");
    private static final ModelResourceLocation PISTON_CREATIVE = MachineModelRenderer.standalone("block/machine_stirling_creative_piston");

    public StirlingGeneratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(WORLD);
        event.register(COG);
        event.register(COG_SMALL);
        event.register(PISTON);
        event.register(WORLD_STEEL);
        event.register(COG_STEEL);
        event.register(COG_SMALL_STEEL);
        event.register(PISTON_STEEL);
        event.register(WORLD_CREATIVE);
        event.register(COG_CREATIVE);
        event.register(COG_SMALL_CREATIVE);
        event.register(PISTON_CREATIVE);
    }

    @Override
    public void render(StirlingGeneratorBlockEntity stirling, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = stirling.getBlockState();
        float rot = stirling.getSpin(partialTick);
        if (state.is(HbmBlocks.MACHINE_STIRLING_CREATIVE.get())) {
            renderCreativeStirling(stirling, state, rot, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (state.is(HbmBlocks.MACHINE_STIRLING_STEEL.get())) {
            renderSteelStirling(stirling, state, rot, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderNormalStirling(stirling, state, rot, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    private static void renderNormalStirling(StirlingGeneratorBlockEntity stirling, BlockState state, float rot,
                                             PoseStack poseStack, MultiBufferSource bufferSource,
                                             int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyNormalStirlingWorldPose(poseStack, state.getValue(StirlingGeneratorBlock.FACING));
        STATIC_MESHES.render(WORLD, poseStack, state, packedLight, packedOverlay);
        renderMovingParts(stirling, state, rot, poseStack, bufferSource, packedLight, packedOverlay, COG, COG_SMALL, PISTON);
        poseStack.popPose();
    }

    private static void renderSteelStirling(StirlingGeneratorBlockEntity stirling, BlockState state, float rot,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applySteelStirlingWorldPose(poseStack, state.getValue(StirlingGeneratorBlock.FACING));
        STATIC_MESHES.render(WORLD_STEEL, poseStack, state, packedLight, packedOverlay);
        renderMovingParts(stirling, state, rot, poseStack, bufferSource, packedLight, packedOverlay,
                COG_STEEL, COG_SMALL_STEEL, PISTON_STEEL);
        poseStack.popPose();
    }

    private static void renderCreativeStirling(StirlingGeneratorBlockEntity stirling, BlockState state, float rot,
                                               PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyCreativeStirlingWorldPose(poseStack, state.getValue(StirlingGeneratorBlock.FACING));
        STATIC_MESHES.render(WORLD_CREATIVE, poseStack, state, packedLight, packedOverlay);
        renderMovingParts(stirling, state, rot, poseStack, bufferSource, packedLight, packedOverlay,
                COG_CREATIVE, COG_SMALL_CREATIVE, PISTON_CREATIVE);
        poseStack.popPose();
    }

    private static void renderMovingParts(StirlingGeneratorBlockEntity stirling, BlockState state, float rot,
                                          PoseStack poseStack, MultiBufferSource bufferSource,
                                          int packedLight, int packedOverlay,
                                          ModelResourceLocation cog, ModelResourceLocation cogSmall,
                                          ModelResourceLocation piston) {
        if (stirling.hasCog()) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.375F, 0.0F);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-rot), 0.0F, 0.0F, 1.0F)));
            poseStack.translate(0.0F, -1.375F, 0.0F);
            STATIC_MESHES.render(cog, poseStack, state, packedLight, packedOverlay);
            poseStack.popPose();
        }

        poseStack.pushPose();
        poseStack.translate(0.0F, 1.375F, 0.25F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rot * 2.0F + 3.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0F, -1.375F, -0.25F);
        STATIC_MESHES.render(cogSmall, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(Math.sin(rot * Math.PI / 90.0D) * 0.25D + 0.125D, 0.0D, 0.0D);
        STATIC_MESHES.render(piston, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(StirlingGeneratorBlockEntity blockEntity) {
        BlockState state = blockEntity.getBlockState();
        if (state.is(HbmBlocks.MACHINE_STIRLING_CREATIVE.get())) {
            return creativeStirlingBounds(blockEntity);
        }
        if (state.is(HbmBlocks.MACHINE_STIRLING_STEEL.get())) {
            return steelStirlingBounds(blockEntity);
        }
        return normalStirlingBounds(blockEntity);
    }

    private static AABB normalStirlingBounds(StirlingGeneratorBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        /*
         * Base is exactly +/-1.5 horizontally.  Across the big cog's complete
         * Z-axis rotation, its farthest face-used vertex is
         * 1.1940616557971366 from the Y=1.375 pivot, giving maxY below.  The
         * small cog and full +/- piston stroke remain inside this union.
         */
        return new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 2.5690616557971366D, pos.getZ() + 2.0D);
    }

    private static AABB steelStirlingBounds(StirlingGeneratorBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 2.5690616557971366D, pos.getZ() + 2.0D);
    }

    private static AABB creativeStirlingBounds(StirlingGeneratorBlockEntity blockEntity) {
        var pos = blockEntity.getBlockPos();
        return new AABB(pos.getX() - 1.0D, pos.getY(), pos.getZ() - 1.0D,
                pos.getX() + 2.0D, pos.getY() + 2.5690616557971366D, pos.getZ() + 2.0D);
    }

    private static void applyNormalStirlingWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Normal Stirling generator requires a horizontal facing: " + facing);
        }
    }

    private static void applySteelStirlingWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Steel Stirling generator requires a horizontal facing: " + facing);
        }
    }

    private static void applyCreativeStirlingWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Creative Stirling generator requires a horizontal facing: " + facing);
        }
    }
}
