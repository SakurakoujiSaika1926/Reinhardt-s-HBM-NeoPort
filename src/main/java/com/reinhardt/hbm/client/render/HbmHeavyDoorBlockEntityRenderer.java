package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.reinhardt.hbm.block.HbmHeavyDoorBlock;
import com.reinhardt.hbm.blockentity.HbmHeavyDoorBlockEntity;
import com.reinhardt.hbm.door.HbmDoorDecl;
import net.minecraft.client.renderer.MultiBufferSource;
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
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HbmHeavyDoorBlockEntityRenderer implements LongRangeBlockEntityRenderer<HbmHeavyDoorBlockEntity> {
    private static final StaticMachineVboCache STATIC_MESHES = new StaticMachineVboCache();
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
    private static final ModelResourceLocation CARGO_FRAME = model("cargo_door_frame");
    private static final ModelResourceLocation CARGO_TOP = model("cargo_door_top");
    private static final ModelResourceLocation CARGO_BOTTOM = model("cargo_door_bottom");
    private static final ModelResourceLocation SILO_FRAME = model("silo_hatch_frame");
    private static final ModelResourceLocation SILO_HATCH = model("silo_hatch_hatch");
    private static final ModelResourceLocation SILO_LARGE_FRAME = model("silo_hatch_large_frame");
    private static final ModelResourceLocation SILO_LARGE_HATCH = model("silo_hatch_large_hatch");
    private static final Map<HbmDoorDecl, AABB[]> LOCAL_BOUNDS = buildDoorBounds();

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
                 CARGO_FRAME, CARGO_TOP, CARGO_BOTTOM,
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
        registerSkinned(event, List.of("sliding_blast_door_frame", "sliding_blast_door_left_door", "sliding_blast_door_right_door", "sliding_blast_door_left_lock", "sliding_blast_door_right_lock"), 2);
        registerSkinned(event, List.of("qe_sliding_door_frame", "qe_sliding_door_left_door", "qe_sliding_door_right_door"), 5);
        registerSkinned(event, List.of("qe_containment_door_frame", "qe_containment_door_door"), 3);
        registerSkinned(event, List.of("sliding_seal_door_frame", "sliding_seal_door_door"), 3);
        registerSkinned(event, List.of("secure_access_door_frame", "secure_access_door_door"), 4);
        registerSkinned(event, List.of("round_airlock_door_frame", "round_airlock_door_left", "round_airlock_door_right"), 3);
        registerSkinned(event, List.of("large_vehicle_door_frame", "large_vehicle_door_left", "large_vehicle_door_right"), 4);
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
        switch (doorBlock.decl()) {
            case FIRE_DOOR -> {
                applyFireDoorWorldPose(poseStack, facing);
                renderFireDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case TRANSITION_SEAL -> {
                applyTransitionSealWorldPose(poseStack, facing);
                renderTransitionSeal(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SLIDING_BLAST_DOOR -> {
                applySlidingBlastDoorWorldPose(poseStack, facing);
                renderSlidingBlastDoor(progress, door.state(), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SLIDING_BLAST_DOOR_2 -> {
                applySlidingBlastDoor2WorldPose(poseStack, facing);
                renderSlidingBlastDoor(progress, door.state(), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SLIDING_GATE_DOOR -> {
                applySlidingGateDoorWorldPose(poseStack, facing);
                renderSlidingGateDoor(progress, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case QE_SLIDING_DOOR -> {
                applyQeSlidingDoorWorldPose(poseStack, facing);
                renderQeSlidingDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case QE_CONTAINMENT -> {
                applyQeContainmentWorldPose(poseStack, facing);
                renderQeContainmentDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SLIDING_SEAL_DOOR -> {
                applySlidingSealDoorWorldPose(poseStack, facing);
                renderSealDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SECURE_ACCESS_DOOR -> {
                applySecureAccessDoorWorldPose(poseStack, facing);
                renderSecureDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case ROUND_AIRLOCK_DOOR -> {
                applyRoundAirlockDoorWorldPose(poseStack, facing);
                renderAirlockDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case LARGE_VEHICLE_DOOR -> {
                applyLargeVehicleDoorWorldPose(poseStack, facing);
                renderVehicleDoor(progress, door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case VAULT_DOOR -> {
                applyVaultDoorWorldPose(poseStack, facing);
                renderVaultDoor(vaultPull(door, ticks), vaultSlide(door, ticks), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case WATER_DOOR -> {
                applyWaterDoorWorldPose(poseStack, facing);
                renderWaterDoor(waterDoorProgress(door, ticks), waterBoltProgress(door, ticks), door.skinIndex(), poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case CARGO_DOOR -> {
                applyCargoDoorWorldPose(poseStack, facing);
                renderCargoDoor(door, ticks, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SILO_HATCH -> {
                applySiloHatchWorldPose(poseStack, facing);
                renderSiloHatch(ticks, SILO_FRAME, SILO_HATCH, 1.875F, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
            case SILO_HATCH_LARGE -> {
                applySiloHatchLargeWorldPose(poseStack, facing);
                renderSiloHatch(ticks, SILO_LARGE_FRAME, SILO_LARGE_HATCH, 2.875F, poseStack, bufferSource, state, packedLight, packedOverlay);
            }
        }
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(HbmHeavyDoorBlockEntity blockEntity) {
        if (!(blockEntity.getBlockState().getBlock() instanceof HbmHeavyDoorBlock doorBlock)) {
            return new AABB(blockEntity.getBlockPos());
        }
        Direction facing = blockEntity.getBlockState().getValue(HbmHeavyDoorBlock.FACING);
        return LOCAL_BOUNDS.get(doorBlock.decl())[facing.ordinal()].move(blockEntity.getBlockPos());
    }

    private static Map<HbmDoorDecl, AABB[]> buildDoorBounds() {
        EnumMap<HbmDoorDecl, AABB[]> bounds = new EnumMap<>(HbmDoorDecl.class);
        bounds.put(HbmDoorDecl.FIRE_DOOR, buildFireDoorBounds());
        bounds.put(HbmDoorDecl.TRANSITION_SEAL, buildTransitionSealBounds());
        bounds.put(HbmDoorDecl.SLIDING_BLAST_DOOR, buildSlidingBlastDoorBounds());
        bounds.put(HbmDoorDecl.SLIDING_BLAST_DOOR_2, buildSlidingBlastDoor2Bounds());
        bounds.put(HbmDoorDecl.SLIDING_GATE_DOOR, buildSlidingGateDoorBounds());
        bounds.put(HbmDoorDecl.QE_SLIDING_DOOR, buildQeSlidingDoorBounds());
        bounds.put(HbmDoorDecl.QE_CONTAINMENT, buildQeContainmentBounds());
        bounds.put(HbmDoorDecl.SLIDING_SEAL_DOOR, buildSlidingSealDoorBounds());
        bounds.put(HbmDoorDecl.SECURE_ACCESS_DOOR, buildSecureAccessDoorBounds());
        bounds.put(HbmDoorDecl.ROUND_AIRLOCK_DOOR, buildRoundAirlockDoorBounds());
        bounds.put(HbmDoorDecl.LARGE_VEHICLE_DOOR, buildLargeVehicleDoorBounds());
        bounds.put(HbmDoorDecl.VAULT_DOOR, buildVaultDoorBounds());
        bounds.put(HbmDoorDecl.WATER_DOOR, buildWaterDoorBounds());
        bounds.put(HbmDoorDecl.CARGO_DOOR, buildCargoDoorBounds());
        bounds.put(HbmDoorDecl.SILO_HATCH, buildSiloHatchBounds());
        bounds.put(HbmDoorDecl.SILO_HATCH_LARGE, buildSiloHatchLargeBounds());
        return bounds;
    }

    private static double[] doorEnvelope(HbmDoorDecl decl) {
        return switch (decl) {
            case FIRE_DOOR -> new double[] {-0.5D, 0.0D, -1.5D, 0.5D, 5.75D, 2.5D};
            case TRANSITION_SEAL -> new double[] {
                    bakedMin(-1.4173260000000003D), bakedMin(-0.000003D), -12.5D,
                    bakedMax(8.475050071175868D), bakedMax(46.40530200456391D), 13.5D};
            case SLIDING_BLAST_DOOR -> new double[] {
                    -0.5D, 0.0D, bakedMin(-4.625001D), 0.5D, 4.0D, 4.625D};
            case SLIDING_BLAST_DOOR_2 -> new double[] {
                    -0.5D, 0.0D, bakedMin(-4.625001D), 0.5D, 4.0D, 4.625D};
            case QE_SLIDING_DOOR -> new double[] {
                    0.28125D, bakedMin(0.001D), bakedMin(-1.45D),
                    0.46875D, bakedMax(2.001D), bakedMax(2.45D)};
            case SLIDING_GATE_DOOR -> new double[] {0.125D, 0.0D, -0.5D, 0.375D, 2.0D, 1.4375D};
            case QE_CONTAINMENT -> new double[] {0.0D, 0.0D, -1.5D, 0.5D, 4.75D, 1.5D};
            case SLIDING_SEAL_DOOR -> new double[] {0.25D, 0.0D, -0.5D, 0.5D, 2.0D, 1.3375D};
            case SECURE_ACCESS_DOOR -> new double[] {-0.5D, 0.0D, -2.5D, 0.5D, 8.5D, 2.5D};
            case ROUND_AIRLOCK_DOOR -> new double[] {-0.5625D, 0.0D, -2.9375D, 0.5625D, 4.0D, 3.9375D};
            case LARGE_VEHICLE_DOOR -> new double[] {
                    bakedMin(-0.562501D), 0.0D, -6.3125D,
                    bakedMax(0.562501D), 6.0D, 6.3125D};
            case VAULT_DOOR -> new double[] {-1.5D, 0.0D, -2.5D, 0.5D, 5.0D, bakedMax(7.1606857040168865D)};
            case WATER_DOOR -> new double[] {
                    -0.125D, 0.0D, bakedMin(-1.5116028602111482D),
                    bakedMax(2.9241597794153664D), 3.0D, bakedMax(2.5807082132563766D)};
            case CARGO_DOOR -> new double[] {-0.3125D, 0.0D, -1.5D, 0.3125D, 3.9375D, 1.5D};
            case SILO_HATCH -> new double[] {
                    -2.5D, 0.0D, bakedMin(-4.012259526419164D),
                    2.5D, bakedMax(4.519344934278313D), 2.5D};
            case SILO_HATCH_LARGE -> new double[] {
                    -3.5D, 0.0D, bakedMin(-6.012259526419163D),
                    3.5D, bakedMax(6.512486141889841D), 3.5D};
        };
    }

    private static AABB[] buildFireDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.FIRE_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyFireDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildTransitionSealBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.TRANSITION_SEAL);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyTransitionSealWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildSlidingBlastDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.SLIDING_BLAST_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applySlidingBlastDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildSlidingBlastDoor2Bounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.SLIDING_BLAST_DOOR_2);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applySlidingBlastDoor2WorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildSlidingGateDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.SLIDING_GATE_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applySlidingGateDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildQeSlidingDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.QE_SLIDING_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyQeSlidingDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildQeContainmentBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.QE_CONTAINMENT);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyQeContainmentWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildSlidingSealDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.SLIDING_SEAL_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applySlidingSealDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildSecureAccessDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.SECURE_ACCESS_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applySecureAccessDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildRoundAirlockDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.ROUND_AIRLOCK_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyRoundAirlockDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildLargeVehicleDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.LARGE_VEHICLE_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyLargeVehicleDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildVaultDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.VAULT_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyVaultDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildWaterDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.WATER_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyWaterDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildCargoDoorBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.CARGO_DOOR);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applyCargoDoorWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildSiloHatchBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.SILO_HATCH);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applySiloHatchWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB[] buildSiloHatchLargeBounds() {
        double[] envelope = doorEnvelope(HbmDoorDecl.SILO_HATCH_LARGE);
        AABB[] facings = new AABB[Direction.values().length];
        for (Direction facing : new Direction[] {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST}) {
            PoseStack poseStack = new PoseStack();
            applySiloHatchLargeWorldPose(poseStack, facing);
            facings[facing.ordinal()] = transformedDoorEnvelope(poseStack, envelope);
        }
        return facings;
    }

    private static AABB transformedDoorEnvelope(PoseStack poseStack, double[] envelope) {
        return RenderBounds.transformedBox(net.minecraft.core.BlockPos.ZERO, poseStack,
                (float) envelope[0], (float) envelope[1], (float) envelope[2],
                (float) envelope[3], (float) envelope[4], (float) envelope[5]);
    }

    private static void applyFireDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Fire door requires a horizontal facing: " + facing);
        }
    }

    private static void applyTransitionSealWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Transition seal requires a horizontal facing: " + facing);
        }
    }

    private static void applySlidingBlastDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Sliding blast door requires a horizontal facing: " + facing);
        }
    }

    private static void applySlidingBlastDoor2WorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Sliding blast door 2 requires a horizontal facing: " + facing);
        }
    }

    private static void applySlidingGateDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Sliding gate door requires a horizontal facing: " + facing);
        }
    }

    private static void applyQeSlidingDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("QE sliding door requires a horizontal facing: " + facing);
        }
    }

    private static void applyQeContainmentWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("QE containment door requires a horizontal facing: " + facing);
        }
    }

    private static void applySlidingSealDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Sliding seal door requires a horizontal facing: " + facing);
        }
    }

    private static void applySecureAccessDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Secure access door requires a horizontal facing: " + facing);
        }
    }

    private static void applyRoundAirlockDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Round airlock door requires a horizontal facing: " + facing);
        }
    }

    private static void applyLargeVehicleDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Large vehicle door requires a horizontal facing: " + facing);
        }
    }

    private static void applyVaultDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Vault door requires a horizontal facing: " + facing);
        }
    }

    private static void applyWaterDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Water door requires a horizontal facing: " + facing);
        }
    }

    private static void applyCargoDoorWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Cargo door requires a horizontal facing: " + facing);
        }
    }

    private static void applySiloHatchWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Silo hatch requires a horizontal facing: " + facing);
        }
    }

    private static void applySiloHatchLargeWorldPose(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5F, 0.0F, 0.5F);
        switch (facing) {
            case NORTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case SOUTH -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(270.0F));
            case WEST -> poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(180.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Large silo hatch requires a horizontal facing: " + facing);
        }
    }

    private static double bakedMin(double value) {
        return Math.nextDown((float) value);
    }

    private static double bakedMax(double value) {
        return Math.nextUp((float) value);
    }

    private static void renderFireDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        // RenderFireDoor used glRotate(90) at the already translated TESR
        // origin.  Do not rotate around the block centre here: that adds a
        // compensating translation which the 1.7.10 renderer never had.
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
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

    private static void renderSlidingBlastDoor(float progress, byte stateValue, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
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
        render(skinned("sliding_blast_door_frame", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, open);
        render(skinned("sliding_blast_door_left_door", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateXAt(poseStack, 0.0F, 1.8125F, 0.0F, 90.0F + lock);
        render(skinned("sliding_blast_door_right_lock", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -open);
        render(skinned("sliding_blast_door_right_door", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);
        rotateXAt(poseStack, 0.0F, 1.8125F, 0.0F, 90.0F + lock);
        render(skinned("sliding_blast_door_left_lock", skinIndex, 2), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderQeSlidingDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float open = Mth.clamp(progress * 0.95F, 0.0F, 0.95F);
        poseStack.pushPose();
        poseStack.translate(0.53125F, 0.001F, 0.5F);
        render(skinned("qe_sliding_door_frame", skinIndex, 5), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, open);
        render(skinned("qe_sliding_door_left_door", skinIndex, 5), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.0F, -open);
        render(skinned("qe_sliding_door_right_door", skinIndex, 5), poseStack, bufferSource, state, packedLight, packedOverlay);
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

    private static void renderSealDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        poseStack.translate(0.5F, 0.0F, 0.0F);
        render(skinned("sliding_seal_door_frame", skinIndex, 3), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.0F, smooth(progress) * 0.9F);
        render(skinned("sliding_seal_door_door", skinIndex, 3), poseStack, bufferSource, state, packedLight, packedOverlay);
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

    private static void renderVehicleDoor(float progress, byte skinIndex, PoseStack poseStack, MultiBufferSource bufferSource, BlockState state, int packedLight, int packedOverlay) {
        float open = Mth.clamp(progress * 3.0F, 0.0F, 3.0F);
        // RenderVehicleDoor used a direct glRotate(90) at the legacy origin.
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
        render(skinned("large_vehicle_door_frame", skinIndex, 4), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(-open, 0.0F, 0.0F);
        render(skinned("large_vehicle_door_left", skinIndex, 4), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(open, 0.0F, 0.0F);
        render(skinned("large_vehicle_door_right", skinIndex, 4), poseStack, bufferSource, state, packedLight, packedOverlay);
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
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(90.0F));
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

    private static void renderCargoDoor(HbmHeavyDoorBlockEntity door, float ticks, PoseStack poseStack,
                                        MultiBufferSource bufferSource, BlockState state,
                                        int packedLight, int packedOverlay) {
        float bottomMove = openWindow(ticks, 0.0F, 60.0F) * 2.0F;
        float topMove = door.state() == HbmHeavyDoorBlockEntity.STATE_CLOSING
                ? openWindow(ticks, 0.0F, 30.0F)
                : openWindow(ticks, 30.0F, 60.0F);

        render(CARGO_FRAME, poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0F, topMove, 0.0F);
        render(CARGO_TOP, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0F, bottomMove, 0.0F);
        render(CARGO_BOTTOM, poseStack, bufferSource, state, packedLight, packedOverlay);
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
        STATIC_MESHES.render(model, poseStack, state, packedLight, packedOverlay);
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
