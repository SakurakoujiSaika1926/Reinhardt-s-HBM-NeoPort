package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.door.HbmDoorDecl;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;

public class HbmHeavyDoorBlockEntityRenderer implements BlockEntityRenderer<HbmHeavyDoorBlockEntity> {
    private static final ModelResourceLocation FIRE_FRAME = model("fire_door_frame");
    private static final ModelResourceLocation FIRE_DOOR = model("fire_door_door");
    private static final ModelResourceLocation SLIDE_FRAME = model("sliding_blast_door_frame");
    private static final ModelResourceLocation SLIDE_LEFT = model("sliding_blast_door_left_door");
    private static final ModelResourceLocation SLIDE_RIGHT = model("sliding_blast_door_right_door");
    private static final ModelResourceLocation SLIDE_LEFT_LOCK = model("sliding_blast_door_left_lock");
    private static final ModelResourceLocation SLIDE_RIGHT_LOCK = model("sliding_blast_door_right_lock");
    private static final ModelResourceLocation QE_SLIDING_FRAME = model("qe_sliding_door_frame");
    private static final ModelResourceLocation QE_SLIDING_LEFT = model("qe_sliding_door_left_door");
    private static final ModelResourceLocation QE_SLIDING_RIGHT = model("qe_sliding_door_right_door");
    private static final ModelResourceLocation QE_CONTAINMENT_FRAME = model("qe_containment_door_frame");
    private static final ModelResourceLocation QE_CONTAINMENT_DOOR = model("qe_containment_door_door");
    private static final ModelResourceLocation SEAL_FRAME = model("sliding_seal_door_frame");
    private static final ModelResourceLocation SEAL_DOOR = model("sliding_seal_door_door");
    private static final ModelResourceLocation SECURE_FRAME = model("secure_access_door_frame");
    private static final ModelResourceLocation SECURE_DOOR = model("secure_access_door_door");
    private static final ModelResourceLocation AIRLOCK_FRAME = model("round_airlock_door_frame");
    private static final ModelResourceLocation AIRLOCK_LEFT = model("round_airlock_door_left");
    private static final ModelResourceLocation AIRLOCK_RIGHT = model("round_airlock_door_right");
    private static final ModelResourceLocation VEHICLE_FRAME = model("large_vehicle_door_frame");
    private static final ModelResourceLocation VEHICLE_LEFT = model("large_vehicle_door_left");
    private static final ModelResourceLocation VEHICLE_RIGHT = model("large_vehicle_door_right");
    private static final ModelResourceLocation VAULT_FRAME = model("vault_door_frame");
    private static final ModelResourceLocation VAULT_DOOR = model("vault_door_door");
    private static final ModelResourceLocation VAULT_LABEL = model("vault_door_label");
    private static final ModelResourceLocation WATER_FRAME = model("water_door_frame");
    private static final ModelResourceLocation WATER_DOOR = model("water_door_door");
    private static final ModelResourceLocation WATER_BOLT = model("water_door_bolt");
    private static final ModelResourceLocation WATER_SPINNY_UPPER = model("water_door_spinny_upper");
    private static final ModelResourceLocation WATER_SPINNY_LOWER = model("water_door_spinny_lower");
    private static final ModelResourceLocation SILO_FRAME = model("silo_hatch_frame");
    private static final ModelResourceLocation SILO_HATCH = model("silo_hatch_hatch");
    private static final ModelResourceLocation SILO_LARGE_FRAME = model("silo_hatch_large_frame");
    private static final ModelResourceLocation SILO_LARGE_HATCH = model("silo_hatch_large_hatch");

    public HbmHeavyDoorBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        for (ModelResourceLocation model : List.of(
                FIRE_FRAME, FIRE_DOOR,
                SLIDE_FRAME, SLIDE_LEFT, SLIDE_RIGHT, SLIDE_LEFT_LOCK, SLIDE_RIGHT_LOCK,
                QE_SLIDING_FRAME, QE_SLIDING_LEFT, QE_SLIDING_RIGHT,
                QE_CONTAINMENT_FRAME, QE_CONTAINMENT_DOOR,
                SEAL_FRAME, SEAL_DOOR,
                SECURE_FRAME, SECURE_DOOR,
                AIRLOCK_FRAME, AIRLOCK_LEFT, AIRLOCK_RIGHT,
                VEHICLE_FRAME, VEHICLE_LEFT, VEHICLE_RIGHT,
                VAULT_FRAME, VAULT_DOOR, VAULT_LABEL,
                WATER_FRAME, WATER_DOOR, WATER_BOLT, WATER_SPINNY_UPPER, WATER_SPINNY_LOWER,
                SILO_FRAME, SILO_HATCH,
                SILO_LARGE_FRAME, SILO_LARGE_HATCH
        )) {
            event.register(model);
        }
        registerSkinned(event, List.of("fire_door_frame", "fire_door_door"), 5);
        registerSkinned(event, List.of("qe_containment_door_frame", "qe_containment_door_door"), 3);
        registerSkinned(event, List.of("secure_access_door_frame", "secure_access_door_door"), 4);
        registerSkinned(event, List.of("round_airlock_door_frame", "round_airlock_door_left", "round_airlock_door_right"), 3);
        registerSkinned(event, List.of("vault_door_frame", "vault_door_door", "vault_door_label"), 7);
        registerSkinned(event, List.of("water_door_frame", "water_door_door", "water_door_bolt", "water_door_spinny_upper", "water_door_spinny_lower"), 2);
    }

    @Override
    public void render(HbmHeavyDoorBlockEntity door, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = door.getBlockState();
        if (!(state.getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return;
        }

        Direction facing = state.getValue(HbmHeavyDoorBlock.FACING);
        float ticks = openTicks(door, partialTick, doorBlock.decl().timeToOpen());
        float progress = doorBlock.decl().openProgress(ticks);

        poseStack.pushPose();
        MachineModelRenderer.orientLegacyWavefrontOriginYaw(poseStack, legacyDoorYaw(facing));
        renderDoor(doorBlock.decl(), door, ticks, progress, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HbmHeavyDoorBlockEntity blockEntity) {
        return new AABB(blockEntity.getBlockPos()).inflate(8.0D, 8.0D, 8.0D);
    }

    private static void renderDoor(HbmDoorDecl decl, HbmHeavyDoorBlockEntity door, float ticks, float progress, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        switch (decl) {
            case FIRE_DOOR -> renderFireDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case SLIDING_BLAST_DOOR, SLIDING_BLAST_DOOR_2 -> renderSlidingBlastDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SLIDING_GATE_DOOR, QE_SLIDING -> renderQeSlidingDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case QE_CONTAINMENT -> renderQeContainmentDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case SLIDING_SEAL_DOOR -> renderSealDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SECURE_ACCESS_DOOR -> renderSecureDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case ROUND_AIRLOCK_DOOR -> renderAirlockDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case LARGE_VEHICLE_DOOR -> renderVehicleDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case VAULT_DOOR -> renderVaultDoor(vaultPull(door, ticks), vaultSlide(door, ticks), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case WATER_DOOR -> renderWaterDoor(waterDoorProgress(door, ticks), waterBoltProgress(door, ticks), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case SILO_HATCH -> renderSiloHatch(smooth(openWindow(ticks, 20.0F, 60.0F)), SILO_FRAME, SILO_HATCH, 1.875F, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SILO_HATCH_LARGE -> renderSiloHatch(smooth(openWindow(ticks, 20.0F, 60.0F)), SILO_LARGE_FRAME, SILO_LARGE_HATCH, 2.875F, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderFireDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        MachineModelRenderer.orientYaw(poseStack, 90.0F);
        poseStack.translate(-0.5F, 0.0F, 0.0F);
        render(skinned("fire_door_frame", skinIndex, 5), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, Mth.clamp(progress * 2.75F, 0.0F, 2.75F), 0.0F);
        render(skinned("fire_door_door", skinIndex, 5), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderSlidingBlastDoor(float progress, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float open = Mth.clamp(progress * 2.125F, 0.0F, 2.125F);
        float lock = Mth.clamp(progress, 0.0F, 1.0F) * 90.0F;
        render(SLIDE_FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, open);
        render(SLIDE_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateXAt(poseStack, 0.0F, 1.8125F, 0.0F, 90.0F + lock);
        render(SLIDE_RIGHT_LOCK, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -open);
        render(SLIDE_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateXAt(poseStack, 0.0F, 1.8125F, 0.0F, 90.0F + lock);
        render(SLIDE_LEFT_LOCK, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderQeSlidingDoor(float progress, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float open = Mth.clamp(progress * 0.95F, 0.0F, 0.95F);
        poseStack.pushPose();
        poseStack.translate(0.53125F, 0.001F, 0.5F);
        render(QE_SLIDING_FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, open);
        render(QE_SLIDING_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -open);
        render(QE_SLIDING_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderQeContainmentDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.translate(0.25F, 0.0F, 0.0F);
        render(skinned("qe_containment_door_frame", skinIndex, 3), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, Mth.clamp(progress * 2.25F, 0.0F, 2.25F), 0.0F);
        render(skinned("qe_containment_door_door", skinIndex, 3), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderSealDoor(float progress, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.translate(0.5F, 0.0F, 0.0F);
        render(SEAL_FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.0F, smooth(progress) * 0.9F);
        render(SEAL_DOOR, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderSecureDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.translate(0.0F, 1.0F, 0.0F);
        render(skinned("secure_access_door_frame", skinIndex, 4), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, Mth.clamp(progress * 3.5F, 0.0F, 3.5F), 0.0F);
        render(skinned("secure_access_door_door", skinIndex, 4), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderAirlockDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float open = Mth.clamp(progress * 1.5F, 0.0F, 1.5F);
        poseStack.translate(0.0F, 0.0F, 0.5F);
        render(skinned("round_airlock_door_frame", skinIndex, 3), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, open);
        render(skinned("round_airlock_door_left", skinIndex, 3), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -open);
        render(skinned("round_airlock_door_right", skinIndex, 3), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderVehicleDoor(float progress, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float open = Mth.clamp(progress * 3.0F, 0.0F, 3.0F);
        MachineModelRenderer.orientYaw(poseStack, 90.0F);
        render(VEHICLE_FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(-open, 0.0F, 0.0F);
        render(VEHICLE_LEFT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(open, 0.0F, 0.0F);
        render(VEHICLE_RIGHT, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderVaultDoor(float pull, float slide, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        render(skinned("vault_door_frame", skinIndex, 7), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(-pull, 0.0F, slide * 5.0F);
        float roll = 360.0F * (slide * 5.0F) / (4.25F * Mth.PI);
        rotateXAt(poseStack, 0.0F, 2.5F, 0.0F, roll);
        render(skinned("vault_door_door", skinIndex, 7), poseStack, bufferSource, state, packedLight, packedOverlay);
        render(skinned("vault_door_label", skinIndex, 7), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderWaterDoor(float doorProgress, float boltProgress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.translate(0.375F, 0.0F, 0.0F);
        MachineModelRenderer.orientYaw(poseStack, 90.0F);
        render(skinned("water_door_frame", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(-1.1875F, 0.0F, 0.0F);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-doorProgress * 120.0F), 0.0F, 1.0F, 0.0F)));
        poseStack.translate(1.1875F, 0.0F, 0.0F);
        render(skinned("water_door_door", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(-0.4F * boltProgress, 0.0F, 0.0F);
        render(skinned("water_door_bolt", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        rotateZAt(poseStack, 0.40625F, 2.28125F, 0.0F, boltProgress * 360.0F);
        render(skinned("water_door_spinny_upper", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        rotateZAt(poseStack, 0.40625F, 0.71875F, 0.0F, boltProgress * 360.0F);
        render(skinned("water_door_spinny_lower", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderSiloHatch(float progress, ModelResourceLocation frame, ModelResourceLocation hatch, float hingeZ, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        render(frame, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.875F, -hingeZ);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(progress * -240.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0F, -0.875F + 0.25F * Mth.clamp(progress, 0.0F, 1.0F), hingeZ);
        render(hatch, poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static float openTicks(HbmHeavyDoorBlockEntity door, float partialTick, int maxTicks) {
        float ticks = door.openTicks();
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPENING) {
            ticks += partialTick;
        } else if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            ticks -= partialTick;
        }
        return Mth.clamp(ticks, 0.0F, maxTicks);
    }

    private static float vaultPull(HbmHeavyDoorBlockEntity door, float ticks) {
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPEN) {
            return 1.0F;
        }
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            return 1.0F - smooth(openWindow(ticks, 0.0F, 40.0F));
        }
        return smooth(openWindow(ticks, 0.0F, 40.0F));
    }

    private static float vaultSlide(HbmHeavyDoorBlockEntity door, float ticks) {
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPEN) {
            return 1.0F;
        }
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            return 1.0F - openWindow(ticks, 0.0F, 80.0F);
        }
        return openWindow(ticks, 40.0F, 120.0F);
    }

    private static float waterDoorProgress(HbmHeavyDoorBlockEntity door, float ticks) {
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPEN) {
            return 1.0F;
        }
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            return 1.0F - smooth(openWindow(ticks, 0.0F, 30.0F));
        }
        return smooth(openWindow(ticks, 30.0F, 60.0F));
    }

    private static float waterBoltProgress(HbmHeavyDoorBlockEntity door, float ticks) {
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPEN) {
            return 1.0F;
        }
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            return 1.0F - smooth(openWindow(ticks, 30.0F, 60.0F));
        }
        return smooth(openWindow(ticks, 0.0F, 30.0F));
    }

    private static float openWindow(float ticks, float min, float max) {
        if (max <= min) {
            return ticks >= min ? 1.0F : 0.0F;
        }
        return Mth.clamp((ticks - min) / (max - min), 0.0F, 1.0F);
    }

    private static float smooth(float value) {
        return value * value * (3.0F - 2.0F * value);
    }

    private static void rotateXAt(PoseStack poseStack, float x, float y, float z, float degrees) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(-x, -y, -z);
    }

    private static void rotateZAt(PoseStack poseStack, float x, float y, float z, float degrees) {
        poseStack.translate(x, y, z);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(-x, -y, -z);
    }

    private static void render(ModelResourceLocation model, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void registerSkinned(ModelEvent.RegisterAdditional event, List<String> baseModels, int skinCount) {
        for (String baseModel : baseModels) {
            for (int skin = 1; skin < skinCount; skin++) {
                event.register(model(baseModel + "_skin_" + skin));
            }
        }
    }

    private static ModelResourceLocation skinned(String baseModel, byte skinIndex, int skinCount) {
        if (skinCount <= 1) {
            return model(baseModel);
        }
        int skin = Math.floorMod((int) skinIndex, skinCount);
        return skin == 0 ? model(baseModel) : model(baseModel + "_skin_" + skin);
    }

    private static float legacyDoorYaw(Direction facing) {
        return switch (facing) {
            case EAST -> 0.0F;
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            default -> 270.0F;
        };
    }

    private static ModelResourceLocation model(String name) {
        return MachineModelRenderer.standalone("block/doors/" + name);
    }

    static List<ModelResourceLocation> doorItemModels() {
        List<ModelResourceLocation> models = new ArrayList<>();
        for (HbmDoorDecl decl : HbmDoorDecl.CREATIVE_ORDER) {
            models.add(MachineModelRenderer.standalone("block/" + decl.id()));
        }
        return models;
    }
}
