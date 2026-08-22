package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.LauncherBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class LauncherBlockEntityRenderer implements BlockEntityRenderer<LauncherBlockEntity> {
    private static final ModelResourceLocation PAD_SILO = MachineModelRenderer.standalone("block/launch_pad_world");
    private static final ModelResourceLocation PAD_RUSTED = MachineModelRenderer.standalone("block/launch_pad_rusted_world");
    private static final ModelResourceLocation PAD_LARGE = MachineModelRenderer.standalone("block/launch_pad_large_world");
    private static final ModelResourceLocation COMPACT = MachineModelRenderer.standalone("block/compact_launcher_world");
    private static final ModelResourceLocation TABLE_BASE = MachineModelRenderer.standalone("block/launch_table_base");
    private static final ModelResourceLocation TABLE_SMALL_PAD = MachineModelRenderer.standalone("block/launch_table_small_pad");
    private static final ModelResourceLocation TABLE_SMALL_SCAFFOLD_BASE = MachineModelRenderer.standalone("block/launch_table_small_scaffold_base");
    private static final ModelResourceLocation SOYUZ_LEGS = MachineModelRenderer.standalone("block/soyuz_launcher_legs");
    private static final ModelResourceLocation SOYUZ_TABLE = MachineModelRenderer.standalone("block/soyuz_launcher_table");
    private static final ModelResourceLocation SOYUZ_TOWER_BASE = MachineModelRenderer.standalone("block/soyuz_launcher_tower_base");
    private static final ModelResourceLocation SOYUZ_TOWER = MachineModelRenderer.standalone("block/soyuz_launcher_tower");
    private static final ModelResourceLocation SOYUZ_SUPPORT_BASE = MachineModelRenderer.standalone("block/soyuz_launcher_support_base");
    private static final ModelResourceLocation SOYUZ_SUPPORT = MachineModelRenderer.standalone("block/soyuz_launcher_support");

    public LauncherBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PAD_SILO);
        event.register(PAD_RUSTED);
        event.register(PAD_LARGE);
        event.register(COMPACT);
        event.register(TABLE_BASE);
        event.register(TABLE_SMALL_PAD);
        event.register(TABLE_SMALL_SCAFFOLD_BASE);
        event.register(SOYUZ_LEGS);
        event.register(SOYUZ_TABLE);
        event.register(SOYUZ_TOWER_BASE);
        event.register(SOYUZ_TOWER);
        event.register(SOYUZ_SUPPORT_BASE);
        event.register(SOYUZ_SUPPORT);
    }

    @Override
    public void render(LauncherBlockEntity launcher, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = launcher.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
        poseStack.pushPose();
        switch (launcher.kind()) {
            case PAD_SMALL -> renderOriented(PAD_SILO, facing, state, poseStack, bufferSource, packedLight, packedOverlay);
            case PAD_RUSTED -> renderOriented(PAD_RUSTED, facing, state, poseStack, bufferSource, packedLight, packedOverlay);
            case PAD_LARGE -> renderOriented(PAD_LARGE, facing, state, poseStack, bufferSource, packedLight, packedOverlay);
            case COMPACT -> {
                poseStack.translate(0.5D, 0.0D, 0.5D);
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(COMPACT), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case TABLE -> renderTable(facing, state, poseStack, bufferSource, packedLight, packedOverlay);
            case SOYUZ -> renderSoyuz(state, poseStack, bufferSource, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LauncherBlockEntity blockEntity) {
        return switch (blockEntity.kind()) {
            case SOYUZ -> new AABB(blockEntity.getBlockPos()).inflate(8.0D, 54.0D, 10.0D);
            case PAD_LARGE, TABLE -> new AABB(blockEntity.getBlockPos()).inflate(6.0D, 14.0D, 6.0D);
            case COMPACT -> new AABB(blockEntity.getBlockPos()).inflate(2.0D, 3.0D, 2.0D);
            default -> new AABB(blockEntity.getBlockPos()).inflate(2.0D, 3.0D, 2.0D);
        };
    }

    private static void renderOriented(ModelResourceLocation model, Direction facing, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, launchPadYaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderTable(Direction facing, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, launchTableYaw(facing));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TABLE_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TABLE_SMALL_PAD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.0F, 2.5F);
        for (int i = 0; i <= 10; i++) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TABLE_SMALL_SCAFFOLD_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(0.0F, 1.0F, 0.0F);
        }
        poseStack.popPose();
    }

    private static void renderSoyuz(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_LEGS), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_TABLE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_TOWER_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_TOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_SUPPORT_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_SUPPORT), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float launchPadYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    private static float launchTableYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 0.0F;
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }
}
