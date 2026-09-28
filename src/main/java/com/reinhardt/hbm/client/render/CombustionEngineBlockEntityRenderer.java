package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.CombustionEngineBlock;
import com.reinhardt.hbm.blockentity.CombustionEngineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class CombustionEngineBlockEntityRenderer implements LongRangeBlockEntityRenderer<CombustionEngineBlockEntity> {
    private static final ModelResourceLocation ENGINE = MachineModelRenderer.standalone("block/machine_combustion_engine_engine");
    private static final ModelResourceLocation CANISTER = MachineModelRenderer.standalone("block/machine_combustion_engine_canister");
    private static final ModelResourceLocation HATCH = MachineModelRenderer.standalone("block/machine_combustion_engine_hatch");
    private static final AABB[] LOCAL_BOUNDS = buildCombustionEngineBounds();

    public CombustionEngineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(ENGINE);
        event.register(CANISTER);
        event.register(HATCH);
    }

    @Override
    public void render(CombustionEngineBlockEntity engine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = engine.getBlockState();
        Direction facing = combustionEngineFacing(state);
        poseStack.pushPose();
        applyCombustionEngineTransform(poseStack, facing);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ENGINE), poseStack, bufferSource, state, packedLight, packedOverlay);
        int canisterColor = engine.fuelTank().type().canisterColorRgbOr(0xFFFFFF);
        MachineModelRenderer.renderUnculledTinted(
                MachineModelRenderer.model(CANISTER), poseStack, bufferSource, state, packedLight, packedOverlay,
                ((canisterColor >>> 16) & 0xFF) / 256.0F,
                ((canisterColor >>> 8) & 0xFF) / 256.0F,
                (canisterColor & 0xFF) / 256.0F,
                1.0F);

        // Exact RenderCombustionEngine hinge: translate to (1, 0, -2.6875),
        // rotate around negative Y, then translate the model back.
        poseStack.pushPose();
        poseStack.translate(1.0F, 0.0F, -2.6875F);
        poseStack.mulPose(Axis.YN.rotationDegrees(engine.doorAngle(partialTick)));
        poseStack.translate(-1.0F, 0.0F, 2.6875F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(HATCH), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(CombustionEngineBlockEntity blockEntity) {
        Direction facing = combustionEngineFacing(blockEntity.getBlockState());
        return LOCAL_BOUNDS[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static AABB[] buildCombustionEngineBounds() {
        AABB[] bounds = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyCombustionEngineTransform(poseStack, facing);
            bounds[facing.ordinal()] = RenderBounds.transformedBox(
                    net.minecraft.core.BlockPos.ZERO, poseStack,
                    -1.028219F, -0.025198F, -5.5F,
                    1.625F, 2.025198F, 0.625F);
        }
        return bounds;
    }

    private static void applyCombustionEngineTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case EAST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Combustion engine facing must be horizontal: " + facing);
        }
        poseStack.translate(-0.5F, 0.0F, 3.0F);
    }

    private static Direction combustionEngineFacing(BlockState state) {
        if (!state.hasProperty(CombustionEngineBlock.FACING)) {
            throw new IllegalStateException("Combustion engine state is missing its owned facing property: " + state);
        }
        return state.getValue(CombustionEngineBlock.FACING);
    }
}
