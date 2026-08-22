package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.block.ParticleAcceleratorBlock;
import com.reinhardt.hbm.blockentity.ParticleAcceleratorBlockEntity;
import com.reinhardt.hbm.util.LegacyMachineGeometry;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;

public class ParticleAcceleratorBlockEntityRenderer implements BlockEntityRenderer<ParticleAcceleratorBlockEntity> {
    private static final ModelResourceLocation SOURCE = MachineModelRenderer.standalone("block/pa_source");
    private static final ModelResourceLocation BEAMLINE_BODY = MachineModelRenderer.standalone("block/pa_beamline_body");
    private static final ModelResourceLocation BEAMLINE_GLASS = MachineModelRenderer.standalone("block/pa_beamline_glass");
    private static final ModelResourceLocation BEAMLINE_WINDOW = MachineModelRenderer.standalone("block/pa_beamline_window");
    private static final ModelResourceLocation RFC = MachineModelRenderer.standalone("block/pa_rfc");
    private static final ModelResourceLocation QUADRUPOLE = MachineModelRenderer.standalone("block/pa_quadrupole");
    private static final ModelResourceLocation DIPOLE = MachineModelRenderer.standalone("block/pa_dipole");
    private static final ModelResourceLocation DETECTOR = MachineModelRenderer.standalone("block/pa_detector");

    public ParticleAcceleratorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(SOURCE);
        event.register(BEAMLINE_BODY);
        event.register(BEAMLINE_GLASS);
        event.register(BEAMLINE_WINDOW);
        event.register(RFC);
        event.register(QUADRUPOLE);
        event.register(DIPOLE);
        event.register(DETECTOR);
    }

    @Override
    public void render(ParticleAcceleratorBlockEntity accelerator, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = accelerator.getBlockState();
        Direction facing = state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.SOUTH;
        poseStack.pushPose();
        switch (accelerator.kind()) {
            case SOURCE -> {
                poseStack.translate(0.0D, -1.0D, 0.0D);
                MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SOURCE), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case BEAMLINE -> {
                MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
                if (accelerator.hasBeamlineWindow()) {
                    MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BEAMLINE_WINDOW), poseStack, bufferSource, state, packedLight, packedOverlay);
                    int alpha = Math.min(255, Math.max(48, Math.round(accelerator.beamlineLight(partialTick) * 120.0F)));
                    MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(BEAMLINE_GLASS), poseStack, bufferSource, state, LightTexture.FULL_BRIGHT, packedOverlay, (alpha << 24) | 0xE6E6FF);
                } else {
                    MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BEAMLINE_BODY), poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            case RFC -> {
                poseStack.translate(0.0D, -1.0D, 0.0D);
                MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RFC), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case QUADRUPOLE -> {
                poseStack.translate(0.0D, -1.0D, 0.0D);
                MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(QUADRUPOLE), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case DIPOLE -> {
                poseStack.translate(0.5D, -1.0D, 0.5D);
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(DIPOLE), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case DETECTOR -> {
                poseStack.translate(0.0D, -2.0D, 0.0D);
                MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyYaw(facing));
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(DETECTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ParticleAcceleratorBlockEntity accelerator) {
        return switch (accelerator.kind()) {
            case SOURCE, RFC -> new AABB(accelerator.getBlockPos()).inflate(5.0D, 2.0D, 5.0D);
            case DETECTOR -> new AABB(accelerator.getBlockPos()).inflate(5.0D, 3.0D, 5.0D);
            case BEAMLINE, QUADRUPOLE, DIPOLE -> new AABB(accelerator.getBlockPos()).inflate(2.0D, 2.0D, 2.0D);
        };
    }

    private static float legacyYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case SOUTH -> 270.0F;
            case WEST -> 180.0F;
            default -> 0.0F;
        };
    }
}
