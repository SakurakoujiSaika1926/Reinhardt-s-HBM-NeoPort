package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class LegacyTurretBlockEntityRenderer implements BlockEntityRenderer<LegacyTurretBlockEntity> {
    private static final ModelResourceLocation CHEKHOV_BASE = model("turret_chekhov_base");
    private static final ModelResourceLocation CHEKHOV_CARRIAGE = model("turret_chekhov_carriage");
    private static final ModelResourceLocation CHEKHOV_BODY = model("turret_chekhov_body");
    private static final ModelResourceLocation CHEKHOV_BARRELS = model("turret_chekhov_barrels");
    private static final ModelResourceLocation CHEKHOV_CONNECTORS = model("turret_chekhov_connectors");
    private static final ModelResourceLocation FRIENDLY_BASE = model("turret_friendly_base");
    private static final ModelResourceLocation FRIENDLY_CARRIAGE = model("turret_friendly_carriage");
    private static final ModelResourceLocation FRITZ_GUN = model("turret_fritz_gun");
    private static final ModelResourceLocation HOWARD_CARRIAGE = model("turret_howard_carriage");
    private static final ModelResourceLocation HOWARD_BODY = model("turret_howard_body");
    private static final ModelResourceLocation HOWARD_BARRELS_TOP = model("turret_howard_barrels_top");
    private static final ModelResourceLocation HOWARD_BARRELS_BOTTOM = model("turret_howard_barrels_bottom");
    private static final ModelResourceLocation HOWARD_DAMAGED_BASE = model("turret_howard_damaged_base");
    private static final ModelResourceLocation HOWARD_DAMAGED_CARRIAGE = model("turret_howard_damaged_carriage");
    private static final ModelResourceLocation HOWARD_DAMAGED_BODY = model("turret_howard_damaged_body");
    private static final ModelResourceLocation HOWARD_DAMAGED_BARRELS_TOP = model("turret_howard_damaged_barrels_top");
    private static final ModelResourceLocation HOWARD_DAMAGED_BARRELS_BOTTOM = model("turret_howard_damaged_barrels_bottom");
    private static final ModelResourceLocation MAXWELL_MICROWAVE = model("turret_maxwell_microwave");
    private static final ModelResourceLocation RICHARD_LAUNCHER = model("turret_richard_launcher");
    private static final ModelResourceLocation RICHARD_MISSILE = model("turret_richard_missile_loaded");
    private static final ModelResourceLocation TAUON_CANNON = model("turret_tauon_cannon");
    private static final ModelResourceLocation TAUON_ROTOR = model("turret_tauon_rotor");
    private static final ModelResourceLocation ARTY_BASE = model("turret_arty_base");
    private static final ModelResourceLocation ARTY_CARRIAGE = model("turret_arty_carriage");
    private static final ModelResourceLocation ARTY_CANNON = model("turret_arty_cannon");
    private static final ModelResourceLocation ARTY_BARREL = model("turret_arty_barrel");
    private static final ModelResourceLocation HIMARS_CARRIAGE = model("turret_himars_carriage");
    private static final ModelResourceLocation HIMARS_LAUNCHER = model("turret_himars_launcher");
    private static final ModelResourceLocation HIMARS_CRANE = model("turret_himars_crane");
    private static final ModelResourceLocation HIMARS_TUBE_STANDARD = model("turret_himars_tube_standard");
    private static final ModelResourceLocation HIMARS_CAP_STANDARD_1 = model("turret_himars_cap_standard_1");
    private static final ModelResourceLocation HIMARS_CAP_STANDARD_2 = model("turret_himars_cap_standard_2");
    private static final ModelResourceLocation HIMARS_CAP_STANDARD_3 = model("turret_himars_cap_standard_3");
    private static final ModelResourceLocation HIMARS_CAP_STANDARD_4 = model("turret_himars_cap_standard_4");
    private static final ModelResourceLocation HIMARS_CAP_STANDARD_5 = model("turret_himars_cap_standard_5");
    private static final ModelResourceLocation HIMARS_CAP_STANDARD_6 = model("turret_himars_cap_standard_6");
    private static final ModelResourceLocation HIMARS_TUBE_SINGLE = model("turret_himars_tube_single");
    private static final ModelResourceLocation HIMARS_CAP_SINGLE = model("turret_himars_cap_single");
    private static final ModelResourceLocation[] HIMARS_TUBES = new ModelResourceLocation[LegacyProjectileUtil.HimarsType.values().length];
    private static final ModelResourceLocation[] HIMARS_SINGLE_CAPS = new ModelResourceLocation[LegacyProjectileUtil.HimarsType.values().length];
    private static final ModelResourceLocation[][] HIMARS_STANDARD_CAPS = new ModelResourceLocation[LegacyProjectileUtil.HimarsType.values().length][6];
    private static final ModelResourceLocation SENTRY_BASE = model("turret_sentry_base");
    private static final ModelResourceLocation SENTRY_PIVOT = model("turret_sentry_pivot");
    private static final ModelResourceLocation SENTRY_BODY = model("turret_sentry_body");
    private static final ModelResourceLocation SENTRY_DRUM = model("turret_sentry_drum");
    private static final ModelResourceLocation SENTRY_BARREL_L = model("turret_sentry_barrel_l");
    private static final ModelResourceLocation SENTRY_BARREL_R = model("turret_sentry_barrel_r");
    private static final ModelResourceLocation SENTRY_DAMAGED_BASE = model("turret_sentry_damaged_base");
    private static final ModelResourceLocation SENTRY_DAMAGED_PIVOT = model("turret_sentry_damaged_pivot");
    private static final ModelResourceLocation SENTRY_DAMAGED_BODY = model("turret_sentry_damaged_body");
    private static final ModelResourceLocation SENTRY_DAMAGED_DRUM = model("turret_sentry_damaged_drum");
    private static final ModelResourceLocation SENTRY_DAMAGED_BARREL_L = model("turret_sentry_damaged_barrel_l");
    private static final ModelResourceLocation SENTRY_DAMAGED_BARREL_R = model("turret_sentry_damaged_barrel_r");

    private static final List<ModelResourceLocation> ALL_MODELS;

    static {
        for (LegacyProjectileUtil.HimarsType type : LegacyProjectileUtil.HimarsType.values()) {
            int modelData = type.modelData();
            if (type.modelType() == 0) {
                HIMARS_TUBES[modelData] = model("turret_himars_tube_standard_" + type.id());
                for (int cap = 0; cap < 6; cap++) {
                    HIMARS_STANDARD_CAPS[modelData][cap] = model("turret_himars_cap_standard_" + (cap + 1) + "_" + type.id());
                }
            } else {
                HIMARS_TUBES[modelData] = model("turret_himars_tube_single_" + type.id());
                HIMARS_SINGLE_CAPS[modelData] = model("turret_himars_cap_single_" + type.id());
            }
        }
        ALL_MODELS = buildModelList();
    }

    public LegacyTurretBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        ALL_MODELS.forEach(event::register);
    }

    @Override
    public void render(LegacyTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        LegacyTurretType type = turret.type();
        if (type.layout() == LegacyTurretType.Layout.SENTRY) {
            renderSentry(turret, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        } else if (type.layout() == LegacyTurretType.Layout.ARTILLERY) {
            renderArtillery(turret, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            renderNt(turret, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(LegacyTurretBlockEntity turret) {
        return AABB.INFINITE;
    }

    @Override
    public boolean shouldRenderOffScreen(LegacyTurretBlockEntity turret) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    private static void renderNt(LegacyTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turret.getBlockState();
        LegacyTurretType type = turret.type();
        VecOffset offset = ntOffset(turret.facing());
        poseStack.pushPose();
        poseStack.translate(offset.x, 0.0F, offset.z);
        renderConnectors(turret, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(baseModel(type), poseStack, bufferSource, state, packedLight, packedOverlay);

        double yaw = -Math.toDegrees(turret.renderYaw(partialTick)) - 90.0D;
        double pitch = Math.toDegrees(turret.renderPitch(partialTick));
        poseStack.mulPose(yaw((float) yaw));
        renderPart(carriageModel(type), poseStack, bufferSource, state, packedLight, packedOverlay);

        if (type == LegacyTurretType.HOWARD || type == LegacyTurretType.HOWARD_DAMAGED) {
            renderHowardTurret(turret, type, partialTick, pitch, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else {
            poseStack.translate(0.0F, 1.5F, 0.0F);
            poseStack.mulPose(roll((float) pitch));
            poseStack.translate(0.0F, -1.5F, 0.0F);
            renderNtBody(type, turret, partialTick, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderHowardTurret(
            LegacyTurretBlockEntity turret,
            LegacyTurretType type,
            float partialTick,
            double pitch,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            BlockState state,
            int packedLight,
            int packedOverlay
    ) {
        poseStack.translate(0.0F, 2.25F, 0.0F);
        poseStack.mulPose(roll((float) pitch));
        poseStack.translate(0.0F, -2.25F, 0.0F);
        renderPart(type == LegacyTurretType.HOWARD_DAMAGED ? HOWARD_DAMAGED_BODY : HOWARD_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        float spin = turret.renderSpin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.5F, 0.0F);
        poseStack.mulPose(pitchX(-spin));
        poseStack.translate(0.0F, -2.5F, 0.0F);
        renderPart(type == LegacyTurretType.HOWARD_DAMAGED ? HOWARD_DAMAGED_BARRELS_TOP : HOWARD_BARRELS_TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        if (type != LegacyTurretType.HOWARD_DAMAGED) {
            poseStack.translate(0.0F, 2.0F, 0.0F);
            poseStack.mulPose(pitchX(spin));
            poseStack.translate(0.0F, -2.0F, 0.0F);
        }
        renderPart(type == LegacyTurretType.HOWARD_DAMAGED ? HOWARD_DAMAGED_BARRELS_BOTTOM : HOWARD_BARRELS_BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderNtBody(LegacyTurretType type, LegacyTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        switch (type) {
            case FRIENDLY -> {
                renderPart(CHEKHOV_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
                poseStack.pushPose();
                poseStack.translate(0.0F, 1.5F, 0.0F);
                poseStack.mulPose(pitchX(-turret.renderSpin(partialTick)));
                poseStack.translate(0.0F, -1.5F, 0.0F);
                renderPart(CHEKHOV_BARRELS, poseStack, bufferSource, state, packedLight, packedOverlay);
                poseStack.popPose();
            }
            case FRITZ -> renderPart(FRITZ_GUN, poseStack, bufferSource, state, packedLight, packedOverlay);
            case MAXWELL -> {
                renderPart(MAXWELL_MICROWAVE, poseStack, bufferSource, state, packedLight, packedOverlay);
                if (turret.beamTicks() > 0) {
                    poseStack.pushPose();
                    poseStack.translate(type.barrelLength(), 2.0D, 0.0D);
                    renderMaxwellBeam(turret, partialTick, poseStack, bufferSource);
                    poseStack.popPose();
                }
            }
            case RICHARD -> {
                renderPart(RICHARD_LAUNCHER, poseStack, bufferSource, state, packedLight, packedOverlay);
                poseStack.translate(0.0F, 0.375F, 0.1875F);
                int missiles = Math.max(0, Math.min(17, turret.loadedAmmo()));
                for (int i = 0; i < missiles; i++) {
                    renderPart(RICHARD_MISSILE, poseStack, bufferSource, state, packedLight, packedOverlay);
                    if (i == 2 || i == 6 || i == 9 || i == 13) {
                        poseStack.translate(0.0F, -0.1875F, 0.46875F);
                    } else {
                        poseStack.translate(0.0F, 0.0F, -0.1875F);
                    }
                }
            }
            case TAUON -> {
                renderPart(TAUON_CANNON, poseStack, bufferSource, state, packedLight, packedOverlay);
                if (turret.beamTicks() > 0) {
                    poseStack.pushPose();
                    poseStack.translate(0.0F, 1.5F, 0.0F);
                    renderTauonBeam(turret, poseStack, bufferSource);
                    poseStack.popPose();
                }
                poseStack.pushPose();
                poseStack.translate(0.0F, 1.375F, 0.0F);
                poseStack.mulPose(pitchX(-turret.renderSpin(partialTick)));
                poseStack.translate(0.0F, -1.375F, 0.0F);
                renderPart(TAUON_ROTOR, poseStack, bufferSource, state, packedLight, packedOverlay);
                poseStack.popPose();
            }
            default -> {
            }
        }
    }

    private static void renderArtillery(LegacyTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turret.getBlockState();
        VecOffset offset = ntOffset(turret.facing());
        poseStack.pushPose();
        poseStack.translate(offset.x, 0.0F, offset.z);
        double yaw = -Math.toDegrees(turret.renderYaw(partialTick)) - 90.0D;
        double pitch = Math.toDegrees(turret.renderPitch(partialTick));
        renderPart(ARTY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) yaw - 90.0F));
        if (turret.type() == LegacyTurretType.ARTY) {
            renderPart(ARTY_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(0.0F, 3.0F, 0.0F);
            poseStack.mulPose(pitchX((float) pitch));
            poseStack.translate(0.0F, -3.0F, 0.0F);
            renderPart(ARTY_CANNON, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(0.0F, 0.0F, turret.renderArtilleryBarrel(partialTick) * 2.5F);
            renderPart(ARTY_BARREL, poseStack, bufferSource, state, packedLight, packedOverlay);
        } else {
            renderPart(HIMARS_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.translate(0.0F, 2.25F, 2.0F);
            poseStack.mulPose(pitchX((float) pitch));
            poseStack.translate(0.0F, -2.25F, -2.0F);
            renderPart(HIMARS_LAUNCHER, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.pushPose();
            poseStack.translate(0.0F, 0.0F, turret.renderCrane(partialTick) * -5.0F);
            renderPart(HIMARS_CRANE, poseStack, bufferSource, state, packedLight, packedOverlay);
            renderHimarsLoad(turret, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderHimarsLoad(LegacyTurretBlockEntity turret, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        int modelData = turret.spareAmmoModelData();
        if (modelData < 0 || modelData >= LegacyProjectileUtil.HimarsType.values().length) {
            return;
        }
        LegacyProjectileUtil.HimarsType type = LegacyProjectileUtil.HimarsType.byModelData(modelData);
        ModelResourceLocation tube = HIMARS_TUBES[modelData];
        if (tube == null) {
            return;
        }
        renderPart(tube, poseStack, bufferSource, state, packedLight, packedOverlay);
        int loaded = Math.max(0, Math.min(type.amount(), turret.loadedAmmo()));
        if (type.modelType() == 0) {
            for (int i = 0; i < loaded; i++) {
                int capIndex = 5 - i;
                ModelResourceLocation cap = HIMARS_STANDARD_CAPS[modelData][capIndex];
                if (cap != null) {
                    renderPart(cap, poseStack, bufferSource, state, packedLight, packedOverlay);
                }
            }
            return;
        }
        ModelResourceLocation cap = HIMARS_SINGLE_CAPS[modelData];
        if (loaded > 0 && cap != null) {
            renderPart(cap, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderSentry(LegacyTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = turret.getBlockState();
        boolean damaged = turret.type() == LegacyTurretType.SENTRY_DAMAGED;
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        renderPart(damaged ? SENTRY_DAMAGED_BASE : SENTRY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) -Math.toDegrees(turret.renderYaw(partialTick))));
        renderPart(damaged ? SENTRY_DAMAGED_PIVOT : SENTRY_PIVOT, poseStack, bufferSource, state, packedLight, packedOverlay);
        float pitch = (float) -Math.toDegrees(turret.renderPitch(partialTick));
        poseStack.translate(0.0F, 1.25F, 0.0F);
        poseStack.mulPose(pitchX(pitch));
        poseStack.translate(0.0F, -1.25F, 0.0F);
        renderPart(damaged ? SENTRY_DAMAGED_BODY : SENTRY_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(damaged ? SENTRY_DAMAGED_DRUM : SENTRY_DRUM, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, turret.renderLeftBarrel(partialTick) * -0.5F);
        renderPart(damaged ? SENTRY_DAMAGED_BARREL_L : SENTRY_BARREL_L, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        if (damaged) {
            poseStack.translate(0.0F, 1.5F, 0.5F);
            poseStack.mulPose(pitchX(25.0F));
            poseStack.translate(0.0F, -1.5F, -0.5F);
        } else {
            poseStack.translate(0.0F, 0.0F, turret.renderRightBarrel(partialTick) * -0.5F);
        }
        renderPart(damaged ? SENTRY_DAMAGED_BARREL_R : SENTRY_BARREL_R, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderConnectors(LegacyTurretBlockEntity turret, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        if (turret.getLevel() == null) {
            return;
        }
        if (turret.type().damaged()) {
            return;
        }
        for (LegacyTurretBlockEntity.ConnectorSpec spec : LegacyTurretBlockEntity.connectorSpecs(turret.getBlockPos(), turret.facing(), turret.type())) {
            Direction side = spec.machineSide();
            if (!canRenderConnector(turret, spec, side)) {
                continue;
            }
            poseStack.pushPose();
            poseStack.mulPose(yaw(sideToDegrees(side)));
            poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
            renderPart(CHEKHOV_CONNECTORS, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static boolean canRenderConnector(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.ConnectorSpec spec, Direction side) {
        if (PowerNetworkManager.canCableConnectTo(turret.getLevel(), spec.pos(), side.getOpposite())) {
            return true;
        }
        return turret.type() == LegacyTurretType.FRITZ
                && turret.getLevel().getCapability(Capabilities.FluidHandler.BLOCK, spec.pos(), side.getOpposite()) != null;
    }

    private static void renderTauonBeam(LegacyTurretBlockEntity turret, PoseStack poseStack, MultiBufferSource bufferSource) {
        double length = turret.beamDistance();
        int phase = (int) (((turret.getLevel() == null ? 0L : turret.getLevel().getGameTime()) / 5L) % 360L);
        renderRandomLineBeam(poseStack.last(), bufferSource.getBuffer(RenderType.lines()), new Vec3(length, 0.0D, 0.0D), phase, Math.max(1, (int) length + 1), 0.1D);
    }

    private static void renderMaxwellBeam(LegacyTurretBlockEntity turret, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource) {
        double length = turret.beamDistance() - turret.type().barrelLength();
        double gameTime = (turret.getLevel() == null ? 0L : turret.getLevel().getGameTime()) + partialTick;
        int segments = Math.max(1, (int) (turret.beamDistance() + 1.0D));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        for (int index = 0; index < 8; index++) {
            int phase = (int) ((gameTime * -50.0D + index * 45.0D) % 360.0D);
            renderSolidSpiralBeam(
                    poseStack.last(),
                    consumer,
                    new Vec3(length, 0.0D, 0.0D),
                    0x2020FF,
                    0x2020FF,
                    phase,
                    segments,
                    0.375D,
                    2,
                    0.05D
            );
        }
    }

    private static void renderRandomLineBeam(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Vec3 skeleton,
            int phase,
            int segments,
            double size
    ) {
        BeamAxes axes = beamAxes(skeleton);
        Random beamRandom = new Random(phase);
        int safeSegments = Math.max(1, segments);
        double segmentLength = skeleton.length() / safeSegments;
        Vec3 previous = null;
        for (int index = 0; index <= safeSegments; index++) {
            double angle = Math.PI * 2.0D * beamRandom.nextFloat();
            angle += Math.PI * 2.0D * beamRandom.nextFloat();
            Vec3 spinner = axes.crossX.scale(Math.cos(angle) * size).add(axes.crossZ.scale(Math.sin(angle) * size));
            Vec3 point = axes.forward.scale(segmentLength * index).add(spinner);
            if (previous != null) {
                renderLineSegment(pose, consumer, previous, point, 0xFFA200);
            }
            previous = point;
        }
        renderLineSegment(pose, consumer, Vec3.ZERO, skeleton, 0xFFD000);
    }

    private static void renderSolidSpiralBeam(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            Vec3 skeleton,
            int outerColor,
            int innerColor,
            int phase,
            int segments,
            double size,
            int layers,
            double thickness
    ) {
        BeamAxes axes = beamAxes(skeleton);
        int safeSegments = Math.max(1, segments);
        int safeLayers = Math.max(1, layers);
        double segmentLength = skeleton.length() / safeSegments;
        Vec3 previous = null;
        for (int index = 0; index <= safeSegments; index++) {
            double angle = Math.toRadians(phase + 45.0D * index);
            Vec3 spinner = axes.crossX.scale(Math.cos(angle) * size).add(axes.crossZ.scale(Math.sin(angle) * size));
            Vec3 point = axes.forward.scale(segmentLength * index).add(spinner);
            if (previous != null) {
                for (int layer = 1; layer <= safeLayers; layer++) {
                    double interpolation = safeLayers == 1 ? 0.0D : (double) (layer - 1) / (double) (safeLayers - 1);
                    int color = interpolateColor(outerColor, innerColor, interpolation);
                    double radius = thickness / safeLayers * layer;
                    renderBeamSegment(pose, consumer, previous, point, axes.crossX, axes.crossZ, radius, color);
                }
            }
            previous = point;
        }
    }

    private static BeamAxes beamAxes(Vec3 skeleton) {
        Vec3 forward = skeleton.normalize();
        Vec3 crossX = new Vec3(-forward.z, 0.0D, forward.x);
        if (crossX.lengthSqr() < 1.0E-5D) {
            crossX = new Vec3(-1.0D, 0.0D, 0.0D);
        }
        crossX = crossX.normalize();
        return new BeamAxes(forward, crossX, crossX.cross(forward).normalize());
    }

    private static int interpolateColor(int outerColor, int innerColor, double interpolation) {
        int red = (int) (((outerColor >>> 16) & 0xFF) + (((innerColor >>> 16) & 0xFF) - ((outerColor >>> 16) & 0xFF)) * interpolation);
        int green = (int) (((outerColor >>> 8) & 0xFF) + (((innerColor >>> 8) & 0xFF) - ((outerColor >>> 8) & 0xFF)) * interpolation);
        int blue = (int) ((outerColor & 0xFF) + ((innerColor & 0xFF) - (outerColor & 0xFF)) * interpolation);
        return red << 16 | green << 8 | blue;
    }

    private static void renderLineSegment(PoseStack.Pose pose, VertexConsumer consumer, Vec3 start, Vec3 end, int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        Vec3 normal = end.subtract(start).normalize();
        consumer.addVertex(pose, (float) start.x, (float) start.y, (float) start.z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose, (float) end.x, (float) end.y, (float) end.z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    private static void renderBeamSegment(PoseStack.Pose pose, VertexConsumer consumer, Vec3 previous, Vec3 point, Vec3 axisX, Vec3 axisZ, double thickness, int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        Vec3 x = axisX.scale(thickness);
        Vec3 z = axisZ.scale(thickness);
        beamQuad(consumer, pose, previous.add(x).add(z), previous.add(x).subtract(z), point.add(x).subtract(z), point.add(x).add(z), red, green, blue);
        beamQuad(consumer, pose, previous.subtract(x).add(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.subtract(x).add(z), red, green, blue);
        beamQuad(consumer, pose, previous.add(x).add(z), previous.subtract(x).add(z), point.subtract(x).add(z), point.add(x).add(z), red, green, blue);
        beamQuad(consumer, pose, previous.add(x).subtract(z), previous.subtract(x).subtract(z), point.subtract(x).subtract(z), point.add(x).subtract(z), red, green, blue);
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int red, int green, int blue) {
        beamVertex(consumer, pose, a, red, green, blue);
        beamVertex(consumer, pose, b, red, green, blue);
        beamVertex(consumer, pose, c, red, green, blue);
        beamVertex(consumer, pose, d, red, green, blue);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose, Vec3 pos, int red, int green, int blue) {
        consumer.addVertex(pose, (float) pos.x, (float) pos.y, (float) pos.z)
                .setColor(red, green, blue, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static ModelResourceLocation baseModel(LegacyTurretType type) {
        return switch (type) {
            case FRIENDLY -> FRIENDLY_BASE;
            case HOWARD_DAMAGED -> HOWARD_DAMAGED_BASE;
            default -> CHEKHOV_BASE;
        };
    }

    private static ModelResourceLocation carriageModel(LegacyTurretType type) {
        return switch (type) {
            case FRIENDLY -> FRIENDLY_CARRIAGE;
            case HOWARD, MAXWELL -> HOWARD_CARRIAGE;
            case HOWARD_DAMAGED -> HOWARD_DAMAGED_CARRIAGE;
            default -> CHEKHOV_CARRIAGE;
        };
    }

    private static void renderPart(ModelResourceLocation location, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(location), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static VecOffset ntOffset(Direction facing) {
        return switch (facing) {
            case NORTH -> new VecOffset(1.0F, 1.0F);
            case WEST -> new VecOffset(1.0F, 0.0F);
            case EAST -> new VecOffset(0.0F, 1.0F);
            default -> new VecOffset(0.0F, 0.0F);
        };
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/" + name);
    }

    private static List<ModelResourceLocation> buildModelList() {
        ArrayList<ModelResourceLocation> models = new ArrayList<>(List.of(
                CHEKHOV_BASE, CHEKHOV_CARRIAGE, CHEKHOV_BODY, CHEKHOV_BARRELS, CHEKHOV_CONNECTORS,
                FRIENDLY_BASE, FRIENDLY_CARRIAGE, FRITZ_GUN,
                HOWARD_CARRIAGE, HOWARD_BODY, HOWARD_BARRELS_TOP, HOWARD_BARRELS_BOTTOM,
                HOWARD_DAMAGED_BASE,
                HOWARD_DAMAGED_CARRIAGE, HOWARD_DAMAGED_BODY, HOWARD_DAMAGED_BARRELS_TOP, HOWARD_DAMAGED_BARRELS_BOTTOM,
                MAXWELL_MICROWAVE, RICHARD_LAUNCHER, RICHARD_MISSILE, TAUON_CANNON, TAUON_ROTOR,
                ARTY_BASE, ARTY_CARRIAGE, ARTY_CANNON, ARTY_BARREL,
                HIMARS_CARRIAGE, HIMARS_LAUNCHER, HIMARS_CRANE, HIMARS_TUBE_STANDARD,
                HIMARS_CAP_STANDARD_1, HIMARS_CAP_STANDARD_2, HIMARS_CAP_STANDARD_3, HIMARS_CAP_STANDARD_4, HIMARS_CAP_STANDARD_5, HIMARS_CAP_STANDARD_6,
                HIMARS_TUBE_SINGLE, HIMARS_CAP_SINGLE,
                SENTRY_BASE, SENTRY_PIVOT, SENTRY_BODY, SENTRY_DRUM, SENTRY_BARREL_L, SENTRY_BARREL_R,
                SENTRY_DAMAGED_BASE, SENTRY_DAMAGED_PIVOT, SENTRY_DAMAGED_BODY, SENTRY_DAMAGED_DRUM, SENTRY_DAMAGED_BARREL_L, SENTRY_DAMAGED_BARREL_R
        ));
        for (LegacyProjectileUtil.HimarsType type : LegacyProjectileUtil.HimarsType.values()) {
            int modelData = type.modelData();
            if (type.modelType() == 0) {
                models.add(HIMARS_TUBES[modelData]);
                for (int cap = 0; cap < 6; cap++) {
                    models.add(HIMARS_STANDARD_CAPS[modelData][cap]);
                }
            } else {
                models.add(HIMARS_TUBES[modelData]);
                models.add(HIMARS_SINGLE_CAPS[modelData]);
            }
        }
        return List.copyOf(models);
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static Quaternionf pitchX(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F));
    }

    private static Quaternionf roll(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F));
    }

    private static float sideToDegrees(Direction side) {
        return switch (side) {
            case WEST -> 0.0F;
            case SOUTH -> 90.0F;
            case EAST -> 180.0F;
            case NORTH -> 270.0F;
            default -> 0.0F;
        };
    }

    private record VecOffset(float x, float z) {
    }

    private record BeamAxes(Vec3 forward, Vec3 crossX, Vec3 crossZ) {
    }
}
