package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.block.SteamEngineBlock;
import com.reinhardt.hbm.blockentity.SteamEngineBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SteamEngineBlockEntityRenderer implements LongRangeBlockEntityRenderer<SteamEngineBlockEntity> {
    static final ModelResourceLocation BASE = MachineModelRenderer.standalone("block/machine_steam_engine_world");
    static final ModelResourceLocation FLYWHEEL = MachineModelRenderer.standalone("block/machine_steam_engine_flywheel");
    static final ModelResourceLocation SHAFT = MachineModelRenderer.standalone("block/machine_steam_engine_shaft");
    static final ModelResourceLocation TRANSMISSION = MachineModelRenderer.standalone("block/machine_steam_engine_transmission");
    static final ModelResourceLocation PISTON = MachineModelRenderer.standalone("block/machine_steam_engine_piston");
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final AABB NORTH_RENDER_BOUNDS = createSteamEngineLocalBounds(Direction.NORTH);
    private static final AABB SOUTH_RENDER_BOUNDS = createSteamEngineLocalBounds(Direction.SOUTH);
    private static final AABB WEST_RENDER_BOUNDS = createSteamEngineLocalBounds(Direction.WEST);
    private static final AABB EAST_RENDER_BOUNDS = createSteamEngineLocalBounds(Direction.EAST);

    public SteamEngineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(BASE);
        event.register(FLYWHEEL);
        event.register(SHAFT);
        event.register(TRANSMISSION);
        event.register(PISTON);
    }

    @Override
    public void render(SteamEngineBlockEntity engine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = engine.getBlockState();
        Direction facing = steamEngineFacing(state);

        poseStack.pushPose();
        applySteamEnginePose(poseStack, facing);
        renderParts(engine.rotor(partialTick), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(SteamEngineBlockEntity blockEntity) {
        Direction facing = steamEngineFacing(blockEntity.getBlockState());
        /* Face-used Base vertices supply X/Z and the floor. Across a full rotor
         * cycle the flywheel's farthest vertex is exactly 1.1940616557971369
         * from its (2, 1.375) pivot, supplying the top; shaft, transmission,
         * and piston remain inside this union. The renderer then translates X +2. */
        return steamEngineRenderBounds(blockEntity.getBlockPos(), facing);
    }

    static void renderParts(float rot, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        STATIC_MESHES.render(BASE, poseStack, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(2.0D, 1.375D, 0.0D);
        poseStack.mulPose(Axis.ZN.rotationDegrees(rot));
        poseStack.translate(-2.0D, -1.375D, 0.0D);
        STATIC_MESHES.render(FLYWHEEL, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 1.375D, -0.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees(rot * 2.0F));
        poseStack.translate(0.0D, -1.375D, 0.5D);
        STATIC_MESHES.render(SHAFT, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        double sin = Math.sin(rot * Math.PI / 180.0D) * 0.25D - 0.25D;
        double cos = Math.cos(rot * Math.PI / 180.0D) * 0.25D;
        double ang = Math.acos(cos / 1.875D);

        poseStack.pushPose();
        poseStack.translate(sin, cos, 0.0D);
        poseStack.translate(2.25D, 1.375D, 0.0D);
        poseStack.mulPose(Axis.ZN.rotation((float) (ang - Math.PI / 2.0D)));
        poseStack.translate(-2.25D, -1.375D, 0.0D);
        STATIC_MESHES.render(TRANSMISSION, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        double cath = Math.sqrt(3.515625D - (cos * cos) / 2.0D);
        poseStack.translate(1.875D - cath + sin, 0.0D, 0.0D);
        STATIC_MESHES.render(PISTON, poseStack, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applySteamEnginePose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case WEST -> { }
            case UP, DOWN -> throw new IllegalArgumentException("Steam engine facing must be horizontal: " + facing);
        }
        poseStack.translate(2.0F, 0.0F, 0.0F);
    }

    private static AABB steamEngineRenderBounds(BlockPos pos, Direction facing) {
        AABB local = switch (facing) {
            case NORTH -> NORTH_RENDER_BOUNDS;
            case SOUTH -> SOUTH_RENDER_BOUNDS;
            case WEST -> WEST_RENDER_BOUNDS;
            case EAST -> EAST_RENDER_BOUNDS;
            case UP, DOWN -> throw new IllegalArgumentException("Steam engine facing must be horizontal: " + facing);
        };
        return local.move(pos);
    }

    private static AABB createSteamEngineLocalBounds(Direction facing) {
        PoseStack poseStack = new PoseStack();
        applySteamEnginePose(poseStack, facing);
        return RenderBounds.transformedBox(BlockPos.ZERO, poseStack,
                -3.5F, 0.0F, -1.5F,
                3.5F, (float) 2.5690616557971369D, 1.5F);
    }

    private static Direction steamEngineFacing(BlockState state) {
        if (!state.hasProperty(SteamEngineBlock.FACING)) {
            throw new IllegalStateException("Steam engine block state is missing its facing property: " + state);
        }
        Direction facing = state.getValue(SteamEngineBlock.FACING);
        if (facing.getAxis().isVertical()) {
            throw new IllegalStateException("Steam engine block state has a vertical facing: " + facing);
        }
        return facing;
    }
}
