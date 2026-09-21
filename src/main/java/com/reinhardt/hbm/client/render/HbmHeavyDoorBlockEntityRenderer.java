package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.door.HbmDoorDecl;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class HbmHeavyDoorBlockEntityRenderer implements BlockEntityRenderer<HbmHeavyDoorBlockEntity> {
    private static final ModelResourceLocation FIRE_FRAME = model("fire_door_frame");
    private static final ModelResourceLocation FIRE_DOOR = model("fire_door_door");
    // Transition Seal keeps one complete OBJ asset; TE rendering selects its roots through visibility variants.
    private static final ModelResourceLocation TRANSITION_SEAL = MachineModelRenderer.standalone("block/transition_seal");
    private static final ModelResourceLocation TRANSITION_SEAL_FRAME = MachineModelRenderer.standalone("block/doors/transition_seal_frame");
    private static final ModelResourceLocation TRANSITION_SEAL_MOVING = MachineModelRenderer.standalone("block/doors/transition_seal_moving");
    private static final List<TransitionSealPart> TRANSITION_SEAL_PARTS = List.of(
            transitionSealPart("door.006"),
            transitionSealPart("ring.002"),
            transitionSealPart("door.004"),
            transitionSealPart("door.003"),
            transitionSealPart("ring.004"),
            transitionSealPart("door.008"),
            transitionSealPart("door.005"),
            transitionSealPart("door.007"),
            transitionSealPart("Cylinder.011"),
            transitionSealPart("Cylinder.010"),
            transitionSealPart("Cylinder.009"),
            transitionSealPart("Circle"),
            transitionSealPart("Cylinder.008"),
            transitionSealPart("Cylinder.007"),
            transitionSealPart("Cube.006"),
            transitionSealPart("Cylinder.005"),
            transitionSealPart("Cylinder.003"),
            transitionSealPart("Cylinder.001"),
            transitionSealPart("door")
    );
    private static final ModelResourceLocation SLIDE_FRAME = model("sliding_blast_door_frame");
    private static final ModelResourceLocation SLIDE_LEFT = model("sliding_blast_door_left_door");
    private static final ModelResourceLocation SLIDE_RIGHT = model("sliding_blast_door_right_door");
    private static final ModelResourceLocation SLIDE_LEFT_LOCK = model("sliding_blast_door_left_lock");
    private static final ModelResourceLocation SLIDE_RIGHT_LOCK = model("sliding_blast_door_right_lock");
    private static final ModelResourceLocation QE_SLIDING_FRAME = model("qe_sliding_door_frame");
    private static final ModelResourceLocation QE_SLIDING_LEFT = model("qe_sliding_door_left_door");
    private static final ModelResourceLocation QE_SLIDING_RIGHT = model("qe_sliding_door_right_door");
    private static final ModelResourceLocation SLIDING_GATE_FRAME = model("sliding_gate_door_frame");
    private static final ModelResourceLocation SLIDING_GATE_DOOR = model("sliding_gate_door_door");
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
        List<ModelResourceLocation> models = new ArrayList<>(List.of(
                FIRE_FRAME, FIRE_DOOR, TRANSITION_SEAL, TRANSITION_SEAL_FRAME, TRANSITION_SEAL_MOVING,
                SLIDE_FRAME, SLIDE_LEFT, SLIDE_RIGHT, SLIDE_LEFT_LOCK, SLIDE_RIGHT_LOCK,
                QE_SLIDING_FRAME, QE_SLIDING_LEFT, QE_SLIDING_RIGHT,
                SLIDING_GATE_FRAME, SLIDING_GATE_DOOR,
                QE_CONTAINMENT_FRAME, QE_CONTAINMENT_DOOR,
                SEAL_FRAME, SEAL_DOOR,
                SECURE_FRAME, SECURE_DOOR,
                AIRLOCK_FRAME, AIRLOCK_LEFT, AIRLOCK_RIGHT,
                VEHICLE_FRAME, VEHICLE_LEFT, VEHICLE_RIGHT,
                VAULT_FRAME, VAULT_DOOR, VAULT_LABEL,
                WATER_FRAME, WATER_DOOR, WATER_BOLT, WATER_SPINNY_UPPER, WATER_SPINNY_LOWER,
                SILO_FRAME, SILO_HATCH,
                SILO_LARGE_FRAME, SILO_LARGE_HATCH
        ));
        for (TransitionSealPart part : TRANSITION_SEAL_PARTS) {
            models.add(part.model());
        }
        for (ModelResourceLocation model : models) {
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
        if (blockEntity.getBlockState().getBlock() instanceof HbmHeavyDoorBlock doorBlock
                && doorBlock.decl() == HbmDoorDecl.TRANSITION_SEAL) {
            /*
             * RenderDoorGeneric marked this door as a global renderer.  Its
             * legacy mesh is 26 blocks wide and rises above the 24-block
             * frame while opening; the ordinary eight-block BER box clips
             * both the top assembly and the outer frame.  Keep this bound
             * local to the giant door instead of changing the shared render
             * distance or applying a generic model transform.
             */
            BlockPos pos = blockEntity.getBlockPos();
            return new AABB(
                    pos.getX() - 14.0D, pos.getY(), pos.getZ() - 14.0D,
                    pos.getX() + 15.0D, pos.getY() + 48.0D, pos.getZ() + 15.0D
            );
        }
        return new AABB(blockEntity.getBlockPos()).inflate(8.0D, 8.0D, 8.0D);
    }

    private static void renderDoor(HbmDoorDecl decl, HbmHeavyDoorBlockEntity door, float ticks, float progress, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        switch (decl) {
            case FIRE_DOOR -> renderFireDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case TRANSITION_SEAL -> renderTransitionSeal(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SLIDING_BLAST_DOOR, SLIDING_BLAST_DOOR_2 -> renderSlidingBlastDoor(progress, door.state(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case SLIDING_GATE_DOOR -> renderSlidingGateDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case QE_SLIDING_DOOR -> renderQeSlidingDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case QE_CONTAINMENT -> renderQeContainmentDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case SLIDING_SEAL_DOOR -> renderSealDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SECURE_ACCESS_DOOR -> renderSecureDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case ROUND_AIRLOCK_DOOR -> renderAirlockDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case LARGE_VEHICLE_DOOR -> renderVehicleDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            case VAULT_DOOR -> renderVaultDoor(vaultPull(door, ticks), vaultSlide(door, ticks), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case WATER_DOOR -> renderWaterDoor(waterDoorProgress(door, ticks), waterBoltProgress(door, ticks), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            case SILO_HATCH -> renderSiloHatch(ticks, SILO_FRAME, SILO_HATCH, 1.875F, poseStack, bufferSource, state, packedLight, packedOverlay);
            case SILO_HATCH_LARGE -> renderSiloHatch(ticks, SILO_LARGE_FRAME, SILO_LARGE_HATCH, 2.875F, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
    }

    private static void renderFireDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // RenderFireDoor used glRotate(90) at the already translated TESR
        // origin.  Do not rotate around the block centre here: that adds a
        // compensating translation which the 1.7.10 renderer never had.
        poseStack.mulPose(MachineModelRenderer.yawQuaternion(90.0F));
        poseStack.translate(-0.5F, 0.0F, 0.0F);
        render(skinned("fire_door_frame", skinIndex, 5), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, Mth.clamp(progress * 2.75F, 0.0F, 2.75F), 0.0F);
        render(skinned("fire_door_door", skinIndex, 5), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderTransitionSeal(float progress, PoseStack poseStack, MultiBufferSource bufferSource,
                                             BlockState state, int packedLight, int packedOverlay) {
        /*
         * RenderDoorGeneric.doOffsetTransform() applies the old half-block Z
         * offset before the animated model.  The frame is not part of the
         * Collada animation; every other OBJ root is rendered separately so
         * its own legacy DAE transform can be applied.
         */
        poseStack.translate(0.0F, 0.0F, 0.5F);
        render(TRANSITION_SEAL_FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);

        for (TransitionSealPart part : TRANSITION_SEAL_PARTS) {
            poseStack.pushPose();
            if (!TransitionSealAnimation.apply(poseStack, part.objectName(), progress)) {
                /*
                 * Keep a deterministic fallback for a resource-pack failure:
                 * this is the original DoorDecl fallback, scoped only to this
                 * door and never shared with other renderers.
                 */
                poseStack.translate(0.0F, Mth.clamp(progress * 3.5F, 0.0F, 3.5F), 0.0F);
            }
            render(part.model(), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    private static void renderSlidingBlastDoor(float progress, byte stateValue, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        // RenderSlidingBlastDoor uses staggered BusAnimation keyframes, not a
        // single linear 24-tick interpolation.  The doors remain shut for
        // 350 ms, move 0.05 blocks over 200 ms, then finish over 650 ms with
        // SIN_UP easing.  The locks open in the first 200 ms (and close only
        // after a 1000 ms hold).
        float openProgress;
        float lockProgress;
        if (progress >= 1.0F) {
            openProgress = 1.0F;
            lockProgress = 1.0F;
        } else {
            float ticks = progress * 24.0F;
            if (stateValue == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
                // Closing is the reverse BusAnimation: the door slides shut
                // first, while the lock stays open for 1000 ms and then
                // rotates closed over the final 200 ms.
                float elapsed = 24.0F - ticks;
                openProgress = elapsed < 13.0F ? 1.0F - 0.95F * sinUp(elapsed / 13.0F)
                        : elapsed < 17.0F ? 0.05F * (1.0F - sinLinear((elapsed - 13.0F) / 4.0F))
                        : 0.0F;
                lockProgress = elapsed < 20.0F ? 1.0F : 1.0F - sinLinear((elapsed - 20.0F) / 4.0F);
            } else {
                openProgress = ticks < 7.0F ? 0.0F
                        : ticks < 11.0F ? 0.05F * sinLinear((ticks - 7.0F) / 4.0F)
                        : 0.05F + 0.95F * sinUp((ticks - 11.0F) / 13.0F);
                lockProgress = sinLinear(Math.min(1.0F, ticks / 4.0F));
            }
        }
        float open = Mth.clamp(openProgress * 2.125F, 0.0F, 2.125F);
        float lock = Mth.clamp(lockProgress, 0.0F, 1.0F) * 90.0F;
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

    private static void renderSlidingGateDoor(float progress, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        // 1.12 DoorDecl.SLIDING_GATE_DOOR reuses the seal-door mesh, offsets
        // it by 0.375 blocks and slides only the door root one block in Z.
        poseStack.translate(0.375F, 0.0F, 0.0F);
        render(SLIDING_GATE_FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.0F, smooth(progress));
        render(SLIDING_GATE_DOOR, poseStack, bufferSource, state, packedLight, packedOverlay);
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
        // RenderVehicleDoor used a direct glRotate(90) at the legacy origin.
        poseStack.mulPose(MachineModelRenderer.yawQuaternion(90.0F));
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
        // RenderWaterDoor used a direct glRotate(90) after its +0.375 X
        // translation; rotating around the block centre would move the frame.
        poseStack.mulPose(MachineModelRenderer.yawQuaternion(90.0F));
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

    private static void renderSiloHatch(float ticks, ModelResourceLocation frame, ModelResourceLocation hatch, float hingeZ, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        render(frame, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.875F, -hingeZ);
        float translation = smooth(openWindow(ticks, 0.0F, 10.0F));
        float rotation = smooth(openWindow(ticks, 20.0F, 100.0F));
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation * -240.0F), 1.0F, 0.0F, 0.0F)));
        poseStack.translate(0.0F, -0.875F + 0.25F * translation, hingeZ);
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
            return sinFull(openWindow(ticks, 0.0F, 40.0F));
        }
        return sinFull(openWindow(ticks, 0.0F, 40.0F));
    }

    private static float vaultSlide(HbmHeavyDoorBlockEntity door, float ticks) {
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPEN) {
            return 1.0F;
        }
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            return openWindow(ticks, 0.0F, 80.0F);
        }
        return openWindow(ticks, 40.0F, 120.0F);
    }

    private static float waterDoorProgress(HbmHeavyDoorBlockEntity door, float ticks) {
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPEN) {
            return 1.0F;
        }
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            return sinFull(openWindow(ticks, 0.0F, 30.0F));
        }
        return sinFull(openWindow(ticks, 30.0F, 60.0F));
    }

    private static float waterBoltProgress(HbmHeavyDoorBlockEntity door, float ticks) {
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_OPEN) {
            return 1.0F;
        }
        if (door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING) {
            return sinFull(openWindow(ticks, 6.0F, 36.0F));
        }
        return sinFull(openWindow(ticks, 0.0F, 30.0F));
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

    private static float sinLinear(float value) {
        return Mth.clamp(value, 0.0F, 1.0F);
    }

    private static float sinUp(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return (float) (-Math.sin((t * Mth.PI + Mth.PI) / 2.0F) + 1.0F);
    }

    private static float sinFull(float value) {
        float t = Mth.clamp(value, 0.0F, 1.0F);
        return (float) ((-Math.cos(t * Mth.PI) + 1.0F) / 2.0F);
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

    private static TransitionSealPart transitionSealPart(String objectName) {
        return new TransitionSealPart(
                objectName,
                MachineModelRenderer.standalone("block/doors/transition_seal_part_"
                        + objectName.replace('.', '_').toLowerCase(Locale.ROOT))
        );
    }

    private record TransitionSealPart(String objectName, ModelResourceLocation model) {
    }

    static List<ModelResourceLocation> doorItemModels() {
        List<ModelResourceLocation> models = new ArrayList<>();
        for (HbmDoorDecl decl : HbmDoorDecl.CREATIVE_ORDER) {
            models.add(MachineModelRenderer.standalone("block/" + decl.id()));
        }
        return models;
    }
}
