package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class SoyuzLauncherBlockEntityRenderer implements BlockEntityRenderer<SoyuzLauncherBlockEntity> {
    private static final ModelResourceLocation SOYUZ_LEGS = MachineModelRenderer.standalone("block/soyuz_launcher_legs");
    private static final ModelResourceLocation SOYUZ_TABLE = MachineModelRenderer.standalone("block/soyuz_launcher_table");
    private static final ModelResourceLocation SOYUZ_TOWER_BASE = MachineModelRenderer.standalone("block/soyuz_launcher_tower_base");
    private static final ModelResourceLocation SOYUZ_TOWER = MachineModelRenderer.standalone("block/soyuz_launcher_tower");
    private static final ModelResourceLocation SOYUZ_SUPPORT_BASE = MachineModelRenderer.standalone("block/soyuz_launcher_support_base");
    private static final ModelResourceLocation SOYUZ_SUPPORT = MachineModelRenderer.standalone("block/soyuz_launcher_support");
    private static final ModelResourceLocation SOYUZ = MachineModelRenderer.standalone("entity/soyuz");
    private static final BlockState SOYUZ_STATE = Blocks.IRON_BLOCK.defaultBlockState();

    public SoyuzLauncherBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOYUZ_LEGS);
        event.register(SOYUZ_TABLE);
        event.register(SOYUZ_TOWER_BASE);
        event.register(SOYUZ_TOWER);
        event.register(SOYUZ_SUPPORT_BASE);
        event.register(SOYUZ_SUPPORT);
        event.register(SOYUZ);
    }

    @Override
    public void render(SoyuzLauncherBlockEntity launcher, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = launcher.getBlockState();
        double open = 45.0D;
        int timer = 20;
        double rotation = launcher.renderRocketType() >= 0 ? 0.0D : open;
        if (launcher.renderStarting() && launcher.renderCountdown() < timer) {
            rotation = (timer - launcher.renderCountdown() + partialTick) * open / timer;
        }

        // RenderSoyuzLauncher translates the complete authored assembly to
        // the block origin four blocks below the core.
        poseStack.pushPose();
        poseStack.translate(0.5D, -4.0D, 0.5D);
        render(SOYUZ_LEGS, poseStack, bufferSource, state, packedLight, packedOverlay);
        render(SOYUZ_TABLE, poseStack, bufferSource, state, packedLight, packedOverlay);
        render(SOYUZ_TOWER_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 5.5D, 5.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) rotation));
        poseStack.translate(0.0D, -5.5D, -5.5D);
        render(SOYUZ_TOWER, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        render(SOYUZ_SUPPORT_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 5.5D, -6.5D);
        poseStack.mulPose(Axis.XP.rotationDegrees((float) -rotation));
        poseStack.translate(0.0D, -5.5D, 6.5D);
        render(SOYUZ_SUPPORT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        if (launcher.renderRocketType() >= 0) {
            poseStack.translate(0.0D, 5.0D, 0.0D);
            render(SOYUZ, poseStack, bufferSource, SOYUZ_STATE, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource,
                                BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource,
                state, packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(SoyuzLauncherBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(8.0D, 54.0D, 10.0D);
    }

    @Override
    public boolean shouldRenderOffScreen(SoyuzLauncherBlockEntity blockEntity) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }
}
