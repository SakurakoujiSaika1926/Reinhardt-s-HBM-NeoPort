package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.reinhardt.hbm.blockentity.LegacyTurretBlockEntity;
import com.reinhardt.hbm.blockentity.LegacyTurretType;
import com.reinhardt.hbm.entity.LegacyProjectileUtil;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class LegacyTurretBlockEntityRenderer implements LongRangeBlockEntityRenderer<LegacyTurretBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
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
        LegacyTurretBlockEntity.RenderSnapshot snapshot = turret.renderSnapshot();
        switch (snapshot.type()) {
            case FRIENDLY -> renderFriendly(turret, snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case FRITZ -> renderFritz(turret, snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case HOWARD -> renderHoward(turret, snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case HOWARD_DAMAGED -> renderHowardDamaged(snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case MAXWELL -> renderMaxwell(turret, snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case RICHARD -> renderRichard(turret, snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case TAUON -> renderTauon(turret, snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case ARTY -> renderArty(snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case HIMARS -> renderHimars(snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case SENTRY -> renderSentry(snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case SENTRY_DAMAGED -> renderSentryDamaged(snapshot, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    @Override
    public AABB getRenderBoundingBox(LegacyTurretBlockEntity turret) {
        return turret.renderSnapshot().bounds();
    }

    private static void renderFriendly(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                       float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyFriendlyRootPose(poseStack, snapshot.facing());
        renderFriendlyConnectors(turret, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(FRIENDLY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 90.0D)));
        renderPart(FRIENDLY_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        double pitch = Math.toDegrees(snapshot.pitch(partialTick));
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(roll((float) pitch));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        renderPart(CHEKHOV_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(pitchX(-snapshot.spin(partialTick)));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        renderPart(CHEKHOV_BARRELS, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderFritz(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                    float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyFritzRootPose(poseStack, snapshot.facing());
        renderFritzConnectors(turret, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 90.0D)));
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(roll((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        renderPart(FRITZ_GUN, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHoward(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                     float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyHowardRootPose(poseStack, snapshot.facing());
        renderHowardConnectors(turret, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 90.0D)));
        renderPart(HOWARD_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 2.25F, 0.0F);
        poseStack.mulPose(roll((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -2.25F, 0.0F);
        renderPart(HOWARD_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        float spin = snapshot.spin(partialTick);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.5F, 0.0F);
        poseStack.mulPose(pitchX(-spin));
        poseStack.translate(0.0F, -2.5F, 0.0F);
        renderPart(HOWARD_BARRELS_TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.0F, 0.0F);
        poseStack.mulPose(pitchX(spin));
        poseStack.translate(0.0F, -2.0F, 0.0F);
        renderPart(HOWARD_BARRELS_BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderHowardDamaged(LegacyTurretBlockEntity.RenderSnapshot snapshot, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyHowardDamagedRootPose(poseStack, snapshot.facing());
        renderPart(HOWARD_DAMAGED_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 90.0D)));
        renderPart(HOWARD_DAMAGED_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 2.25F, 0.0F);
        poseStack.mulPose(roll((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -2.25F, 0.0F);
        renderPart(HOWARD_DAMAGED_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.5F, 0.0F);
        poseStack.mulPose(pitchX(-snapshot.spin(partialTick)));
        poseStack.translate(0.0F, -2.5F, 0.0F);
        renderPart(HOWARD_DAMAGED_BARRELS_TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        renderPart(HOWARD_DAMAGED_BARRELS_BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderMaxwell(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                      float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyMaxwellRootPose(poseStack, snapshot.facing());
        renderMaxwellConnectors(turret, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 90.0D)));
        renderPart(HOWARD_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(roll((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        renderPart(MAXWELL_MICROWAVE, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (snapshot.beamTicks() > 0) {
            poseStack.pushPose();
            poseStack.translate(LegacyTurretType.MAXWELL.barrelLength(), 2.0D, 0.0D);
            renderMaxwellBeam(snapshot, partialTick, poseStack, bufferSource);
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    private static void renderRichard(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                      float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyRichardRootPose(poseStack, snapshot.facing());
        renderRichardConnectors(turret, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 90.0D)));
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(roll((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        renderPart(RICHARD_LAUNCHER, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.375F, 0.1875F);
        int missiles = Math.max(0, Math.min(17, snapshot.loadedAmmo()));
        for (int i = 0; i < missiles; i++) {
            renderPart(RICHARD_MISSILE, poseStack, bufferSource, state, packedLight, packedOverlay);
            if (i == 2 || i == 6 || i == 9 || i == 13) {
                poseStack.translate(0.0F, -0.1875F, 0.46875F);
            } else {
                poseStack.translate(0.0F, 0.0F, -0.1875F);
            }
        }
        poseStack.popPose();
    }

    private static void renderTauon(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                    float partialTick, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyTauonRootPose(poseStack, snapshot.facing());
        renderTauonConnectors(turret, snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(CHEKHOV_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 90.0D)));
        renderPart(CHEKHOV_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(roll((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        renderPart(TAUON_CANNON, poseStack, bufferSource, state, packedLight, packedOverlay);
        if (snapshot.beamTicks() > 0) {
            poseStack.pushPose();
            poseStack.translate(0.0F, 1.5F, 0.0F);
            renderTauonBeam(snapshot, poseStack, bufferSource);
            poseStack.popPose();
        }
        poseStack.translate(0.0F, 1.375F, 0.0F);
        poseStack.mulPose(pitchX(-snapshot.spin(partialTick)));
        poseStack.translate(0.0F, -1.375F, 0.0F);
        renderPart(TAUON_ROTOR, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderArty(LegacyTurretBlockEntity.RenderSnapshot snapshot, float partialTick,
                                   PoseStack poseStack, MultiBufferSource bufferSource,
                                   int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyArtyRootPose(poseStack, snapshot.facing());
        renderPart(ARTY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 180.0D)));
        renderPart(ARTY_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 3.0F, 0.0F);
        poseStack.mulPose(pitchX((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -3.0F, 0.0F);
        renderPart(ARTY_CANNON, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.0F, snapshot.artilleryBarrel(partialTick) * 2.5F);
        renderPart(ARTY_BARREL, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHimars(LegacyTurretBlockEntity.RenderSnapshot snapshot, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applyHimarsRootPose(poseStack, snapshot.facing());
        renderPart(ARTY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) (-Math.toDegrees(snapshot.yaw(partialTick)) - 180.0D)));
        renderPart(HIMARS_CARRIAGE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 2.25F, 2.0F);
        poseStack.mulPose(pitchX((float) Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -2.25F, -2.0F);
        renderPart(HIMARS_LAUNCHER, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.0F, snapshot.crane(partialTick) * -5.0F);
        renderPart(HIMARS_CRANE, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderHimarsLoad(snapshot, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderHimarsLoad(LegacyTurretBlockEntity.RenderSnapshot snapshot, PoseStack poseStack,
                                         MultiBufferSource bufferSource, BlockState state,
                                         int packedLight, int packedOverlay) {
        int modelData = snapshot.spareAmmoModelData();
        if (modelData < 0 || modelData >= LegacyProjectileUtil.HimarsType.values().length) {
            return;
        }
        LegacyProjectileUtil.HimarsType type = LegacyProjectileUtil.HimarsType.byModelData(modelData);
        ModelResourceLocation tube = HIMARS_TUBES[modelData];
        if (tube == null) {
            return;
        }
        renderPart(tube, poseStack, bufferSource, state, packedLight, packedOverlay);
        int loaded = Math.max(0, Math.min(type.amount(), snapshot.loadedAmmo()));
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

    private static void renderSentry(LegacyTurretBlockEntity.RenderSnapshot snapshot, float partialTick,
                                     PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applySentryRootPose(poseStack, snapshot.facing());
        renderPart(SENTRY_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) -Math.toDegrees(snapshot.yaw(partialTick))));
        renderPart(SENTRY_PIVOT, poseStack, bufferSource, state, packedLight, packedOverlay);
        float pitch = (float) -Math.toDegrees(snapshot.pitch(partialTick));
        poseStack.translate(0.0F, 1.25F, 0.0F);
        poseStack.mulPose(pitchX(pitch));
        poseStack.translate(0.0F, -1.25F, 0.0F);
        renderPart(SENTRY_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(SENTRY_DRUM, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, snapshot.leftBarrel(partialTick) * -0.5F);
        renderPart(SENTRY_BARREL_L, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, snapshot.rightBarrel(partialTick) * -0.5F);
        renderPart(SENTRY_BARREL_R, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderSentryDamaged(LegacyTurretBlockEntity.RenderSnapshot snapshot, float partialTick,
                                            PoseStack poseStack, MultiBufferSource bufferSource,
                                            int packedLight, int packedOverlay) {
        BlockState state = snapshot.blockState();
        poseStack.pushPose();
        applySentryDamagedRootPose(poseStack, snapshot.facing());
        renderPart(SENTRY_DAMAGED_BASE, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) -Math.toDegrees(snapshot.yaw(partialTick))));
        renderPart(SENTRY_DAMAGED_PIVOT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.25F, 0.0F);
        poseStack.mulPose(pitchX((float) -Math.toDegrees(snapshot.pitch(partialTick))));
        poseStack.translate(0.0F, -1.25F, 0.0F);
        renderPart(SENTRY_DAMAGED_BODY, poseStack, bufferSource, state, packedLight, packedOverlay);
        renderPart(SENTRY_DAMAGED_DRUM, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, snapshot.leftBarrel(partialTick) * -0.5F);
        renderPart(SENTRY_DAMAGED_BARREL_L, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.5F);
        poseStack.mulPose(pitchX(25.0F));
        poseStack.translate(0.0F, -1.5F, -0.5F);
        renderPart(SENTRY_DAMAGED_BARREL_R, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderFriendlyConnectors(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                                 PoseStack poseStack, MultiBufferSource bufferSource,
                                                 BlockState state, int packedLight, int packedOverlay) {
        List<LegacyTurretBlockEntity.ConnectorSpec> specs = LegacyTurretBlockEntity.friendlyConnectorSpecs(
                turret.getBlockPos(), snapshot.facing());
        for (int index = 0; index < specs.size(); index++) {
            LegacyTurretBlockEntity.ConnectorSpec spec = specs.get(index);
            if (!snapshot.connector(index)) {
                continue;
            }
            poseStack.pushPose();
            applyFriendlyConnectorPose(poseStack, spec);
            renderPart(CHEKHOV_CONNECTORS, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderFritzConnectors(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                              PoseStack poseStack, MultiBufferSource bufferSource,
                                              BlockState state, int packedLight, int packedOverlay) {
        List<LegacyTurretBlockEntity.ConnectorSpec> specs = LegacyTurretBlockEntity.fritzConnectorSpecs(
                turret.getBlockPos(), snapshot.facing());
        for (int index = 0; index < specs.size(); index++) {
            LegacyTurretBlockEntity.ConnectorSpec spec = specs.get(index);
            if (!snapshot.connector(index)) {
                continue;
            }
            poseStack.pushPose();
            applyFritzConnectorPose(poseStack, spec);
            renderPart(CHEKHOV_CONNECTORS, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderHowardConnectors(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                               PoseStack poseStack, MultiBufferSource bufferSource,
                                               BlockState state, int packedLight, int packedOverlay) {
        List<LegacyTurretBlockEntity.ConnectorSpec> specs = LegacyTurretBlockEntity.howardConnectorSpecs(
                turret.getBlockPos(), snapshot.facing());
        for (int index = 0; index < specs.size(); index++) {
            LegacyTurretBlockEntity.ConnectorSpec spec = specs.get(index);
            if (!snapshot.connector(index)) {
                continue;
            }
            poseStack.pushPose();
            applyHowardConnectorPose(poseStack, spec);
            renderPart(CHEKHOV_CONNECTORS, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderMaxwellConnectors(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                BlockState state, int packedLight, int packedOverlay) {
        List<LegacyTurretBlockEntity.ConnectorSpec> specs = LegacyTurretBlockEntity.maxwellConnectorSpecs(
                turret.getBlockPos(), snapshot.facing());
        for (int index = 0; index < specs.size(); index++) {
            LegacyTurretBlockEntity.ConnectorSpec spec = specs.get(index);
            if (!snapshot.connector(index)) {
                continue;
            }
            poseStack.pushPose();
            applyMaxwellConnectorPose(poseStack, spec);
            renderPart(CHEKHOV_CONNECTORS, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderRichardConnectors(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                                PoseStack poseStack, MultiBufferSource bufferSource,
                                                BlockState state, int packedLight, int packedOverlay) {
        List<LegacyTurretBlockEntity.ConnectorSpec> specs = LegacyTurretBlockEntity.richardConnectorSpecs(
                turret.getBlockPos(), snapshot.facing());
        for (int index = 0; index < specs.size(); index++) {
            LegacyTurretBlockEntity.ConnectorSpec spec = specs.get(index);
            if (!snapshot.connector(index)) {
                continue;
            }
            poseStack.pushPose();
            applyRichardConnectorPose(poseStack, spec);
            renderPart(CHEKHOV_CONNECTORS, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderTauonConnectors(LegacyTurretBlockEntity turret, LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                              PoseStack poseStack, MultiBufferSource bufferSource,
                                              BlockState state, int packedLight, int packedOverlay) {
        List<LegacyTurretBlockEntity.ConnectorSpec> specs = LegacyTurretBlockEntity.tauonConnectorSpecs(
                turret.getBlockPos(), snapshot.facing());
        for (int index = 0; index < specs.size(); index++) {
            LegacyTurretBlockEntity.ConnectorSpec spec = specs.get(index);
            if (!snapshot.connector(index)) {
                continue;
            }
            poseStack.pushPose();
            applyTauonConnectorPose(poseStack, spec);
            renderPart(CHEKHOV_CONNECTORS, poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void applyFriendlyConnectorPose(PoseStack poseStack, LegacyTurretBlockEntity.ConnectorSpec spec) {
        switch (spec.machineSide()) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
    }

    private static void applyFritzConnectorPose(PoseStack poseStack, LegacyTurretBlockEntity.ConnectorSpec spec) {
        switch (spec.machineSide()) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
    }

    private static void applyHowardConnectorPose(PoseStack poseStack, LegacyTurretBlockEntity.ConnectorSpec spec) {
        switch (spec.machineSide()) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
    }

    private static void applyMaxwellConnectorPose(PoseStack poseStack, LegacyTurretBlockEntity.ConnectorSpec spec) {
        switch (spec.machineSide()) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
    }

    private static void applyRichardConnectorPose(PoseStack poseStack, LegacyTurretBlockEntity.ConnectorSpec spec) {
        switch (spec.machineSide()) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
    }

    private static void applyTauonConnectorPose(PoseStack poseStack, LegacyTurretBlockEntity.ConnectorSpec spec) {
        switch (spec.machineSide()) {
            case WEST -> { }
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            default -> { }
        }
        poseStack.translate(spec.renderOffsetX(), 0.0F, spec.renderOffsetZ());
    }

    private static void applyFriendlyRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.friendlyWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyFritzRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.fritzWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyHowardRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.howardWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyHowardDamagedRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.howardDamagedWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyMaxwellRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.maxwellWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyRichardRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.richardWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyTauonRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.tauonWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyArtyRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.artyWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applyHimarsRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.himarsWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applySentryRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.sentryWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void applySentryDamagedRootPose(PoseStack poseStack, Direction facing) {
        Vec3 offset = LegacyTurretBlockEntity.sentryDamagedWorldOrigin(facing);
        poseStack.translate(offset.x, 0.0D, offset.z);
    }

    private static void renderTauonBeam(LegacyTurretBlockEntity.RenderSnapshot snapshot,
                                        PoseStack poseStack, MultiBufferSource bufferSource) {
        double length = snapshot.beamDistance();
        int phase = (int) ((snapshot.gameTime() / 5L) % 360L);
        renderRandomLineBeam(poseStack.last(), bufferSource.getBuffer(RenderType.lines()),
                length, phase, Math.max(1, (int) length + 1), 0.1D);
    }

    private static void renderMaxwellBeam(LegacyTurretBlockEntity.RenderSnapshot snapshot, float partialTick,
                                          PoseStack poseStack, MultiBufferSource bufferSource) {
        double length = snapshot.beamDistance() - LegacyTurretType.MAXWELL.barrelLength();
        double gameTime = snapshot.gameTime() + partialTick;
        int segments = Math.max(1, (int) (snapshot.beamDistance() + 1.0D));
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lightning());
        for (int index = 0; index < 8; index++) {
            int phase = (int) ((gameTime * -50.0D + index * 45.0D) % 360.0D);
            renderSolidSpiralBeam(
                    poseStack.last(),
                    consumer,
                    length,
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
            double length,
            int phase,
            int segments,
            double size
    ) {
        double lengthMagnitude = Math.abs(length);
        if (lengthMagnitude < 1.0E-12D) {
            return;
        }
        double direction = Math.copySign(1.0D, length);
        long randomState = (phase ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        int safeSegments = Math.max(1, segments);
        double segmentLength = lengthMagnitude / safeSegments;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= safeSegments; index++) {
            randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
            double angle = Math.PI * 2.0D * ((float) (randomState >>> 24) / 16777216.0F);
            randomState = (randomState * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
            angle += Math.PI * 2.0D * ((float) (randomState >>> 24) / 16777216.0F);
            double pointX = direction * segmentLength * index;
            double pointY = Math.sin(angle) * size;
            double pointZ = direction * Math.cos(angle) * size;
            if (index > 0) {
                renderLineSegment(pose, consumer,
                        previousX, previousY, previousZ, pointX, pointY, pointZ, 0xFFA200);
            }
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
        renderLineSegment(pose, consumer, 0.0D, 0.0D, 0.0D, length, 0.0D, 0.0D, 0xFFD000);
    }

    private static void renderSolidSpiralBeam(
            PoseStack.Pose pose,
            VertexConsumer consumer,
            double length,
            int outerColor,
            int innerColor,
            int phase,
            int segments,
            double size,
            int layers,
            double thickness
    ) {
        double lengthMagnitude = Math.abs(length);
        if (lengthMagnitude < 1.0E-12D) {
            return;
        }
        double direction = Math.copySign(1.0D, length);
        int safeSegments = Math.max(1, segments);
        int safeLayers = Math.max(1, layers);
        double segmentLength = lengthMagnitude / safeSegments;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= safeSegments; index++) {
            double angle = Math.toRadians(phase + 45.0D * index);
            double pointX = direction * segmentLength * index;
            double pointY = Math.sin(angle) * size;
            double pointZ = direction * Math.cos(angle) * size;
            if (index > 0) {
                for (int layer = 1; layer <= safeLayers; layer++) {
                    double interpolation = safeLayers == 1 ? 0.0D : (double) (layer - 1) / (double) (safeLayers - 1);
                    int color = interpolateColor(outerColor, innerColor, interpolation);
                    double radius = thickness / safeLayers * layer;
                    renderBeamSegment(pose, consumer,
                            previousX, previousY, previousZ,
                            pointX, pointY, pointZ,
                            direction, radius, color);
                }
            }
            previousX = pointX;
            previousY = pointY;
            previousZ = pointZ;
        }
    }

    private static int interpolateColor(int outerColor, int innerColor, double interpolation) {
        int red = (int) (((outerColor >>> 16) & 0xFF) + (((innerColor >>> 16) & 0xFF) - ((outerColor >>> 16) & 0xFF)) * interpolation);
        int green = (int) (((outerColor >>> 8) & 0xFF) + (((innerColor >>> 8) & 0xFF) - ((outerColor >>> 8) & 0xFF)) * interpolation);
        int blue = (int) ((outerColor & 0xFF) + ((innerColor & 0xFF) - (outerColor & 0xFF)) * interpolation);
        return red << 16 | green << 8 | blue;
    }

    private static void renderLineSegment(PoseStack.Pose pose, VertexConsumer consumer,
                                          double startX, double startY, double startZ,
                                          double endX, double endY, double endZ,
                                          int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        double normalX = endX - startX;
        double normalY = endY - startY;
        double normalZ = endZ - startZ;
        double normalLength = Math.sqrt(normalX * normalX + normalY * normalY + normalZ * normalZ);
        if (normalLength >= 1.0E-4D) {
            normalX /= normalLength;
            normalY /= normalLength;
            normalZ /= normalLength;
        } else {
            normalX = 0.0D;
            normalY = 0.0D;
            normalZ = 0.0D;
        }
        consumer.addVertex(pose, (float) startX, (float) startY, (float) startZ)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normalX, (float) normalY, (float) normalZ);
        consumer.addVertex(pose, (float) endX, (float) endY, (float) endZ)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normalX, (float) normalY, (float) normalZ);
    }

    private static void renderBeamSegment(PoseStack.Pose pose, VertexConsumer consumer,
                                          double previousX, double previousY, double previousZ,
                                          double pointX, double pointY, double pointZ,
                                          double direction, double thickness, int color) {
        int red = (color >>> 16) & 0xFF;
        int green = (color >>> 8) & 0xFF;
        int blue = color & 0xFF;
        double xZ = direction * thickness;
        double zY = thickness;
        beamQuad(consumer, pose,
                previousX, previousY + zY, previousZ + xZ,
                previousX, previousY - zY, previousZ + xZ,
                pointX, pointY - zY, pointZ + xZ,
                pointX, pointY + zY, pointZ + xZ,
                red, green, blue);
        beamQuad(consumer, pose,
                previousX, previousY + zY, previousZ - xZ,
                previousX, previousY - zY, previousZ - xZ,
                pointX, pointY - zY, pointZ - xZ,
                pointX, pointY + zY, pointZ - xZ,
                red, green, blue);
        beamQuad(consumer, pose,
                previousX, previousY + zY, previousZ + xZ,
                previousX, previousY + zY, previousZ - xZ,
                pointX, pointY + zY, pointZ - xZ,
                pointX, pointY + zY, pointZ + xZ,
                red, green, blue);
        beamQuad(consumer, pose,
                previousX, previousY - zY, previousZ + xZ,
                previousX, previousY - zY, previousZ - xZ,
                pointX, pointY - zY, pointZ - xZ,
                pointX, pointY - zY, pointZ + xZ,
                red, green, blue);
    }

    private static void beamQuad(VertexConsumer consumer, PoseStack.Pose pose,
                                 double ax, double ay, double az,
                                 double bx, double by, double bz,
                                 double cx, double cy, double cz,
                                 double dx, double dy, double dz,
                                 int red, int green, int blue) {
        beamVertex(consumer, pose, ax, ay, az, red, green, blue);
        beamVertex(consumer, pose, bx, by, bz, red, green, blue);
        beamVertex(consumer, pose, cx, cy, cz, red, green, blue);
        beamVertex(consumer, pose, dx, dy, dz, red, green, blue);
    }

    private static void beamVertex(VertexConsumer consumer, PoseStack.Pose pose,
                                   double x, double y, double z,
                                   int red, int green, int blue) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void renderPart(ModelResourceLocation location, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        STATIC_MESHES.render(location, poseStack, state, packedLight, packedOverlay);
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
        return new Quaternionf().rotationY((float) Math.toRadians(degrees));
    }

    private static Quaternionf pitchX(float degrees) {
        return new Quaternionf().rotationX((float) Math.toRadians(degrees));
    }

    private static Quaternionf roll(float degrees) {
        return new Quaternionf().rotationZ((float) Math.toRadians(degrees));
    }
}
