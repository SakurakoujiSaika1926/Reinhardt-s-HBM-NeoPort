package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
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

    public SoyuzLauncherBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOYUZ_LEGS);
        event.register(SOYUZ_TABLE);
        event.register(SOYUZ_TOWER_BASE);
        event.register(SOYUZ_TOWER);
        event.register(SOYUZ_SUPPORT_BASE);
        event.register(SOYUZ_SUPPORT);
    }

    @Override
    public void render(SoyuzLauncherBlockEntity launcher, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = launcher.getBlockState();
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_LEGS), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_TABLE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_TOWER_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_TOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_SUPPORT_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOYUZ_SUPPORT), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    @Override
    public AABB getRenderBoundingBox(SoyuzLauncherBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(8.0D, 54.0D, 10.0D);
    }
}
