package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.LargeMachineBlock;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.fluid.HbmFluidTank;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ModelEvent;
import com.mojang.math.Axis;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Legacy 1.7.10 rendering tables for Pyro Oven, Conveyor Press and Autosaw. */
public final class LegacyMachineBlockEntityRenderer implements LongRangeBlockEntityRenderer<LegacyMachineBlockEntity> {
    private static final long JAVA_RANDOM_MULTIPLIER = 0x5DEECE66DL;
    private static final long JAVA_RANDOM_ADDEND = 0xBL;
    private static final long JAVA_RANDOM_MASK = (1L << 48) - 1L;
    private static final ModelResourceLocation PYRO_OVEN = MachineModelRenderer.standalone("block/machine_pyrooven_world");
    private static final ModelResourceLocation PYRO_SLIDER = MachineModelRenderer.standalone("block/machine_pyrooven_slider");
    private static final ModelResourceLocation PYRO_FAN = MachineModelRenderer.standalone("block/machine_pyrooven_fan");
    private static final ModelResourceLocation AUTOSAW_BASE = MachineModelRenderer.standalone("block/machine_autosaw_base");
    private static final ModelResourceLocation AUTOSAW_MAIN = MachineModelRenderer.standalone("block/machine_autosaw_main");
    private static final ModelResourceLocation AUTOSAW_ENGINE = MachineModelRenderer.standalone("block/machine_autosaw_engine");
    private static final ModelResourceLocation AUTOSAW_UPPER = MachineModelRenderer.standalone("block/machine_autosaw_arm_upper");
    private static final ModelResourceLocation AUTOSAW_LOWER = MachineModelRenderer.standalone("block/machine_autosaw_arm_lower");
    private static final ModelResourceLocation AUTOSAW_TIP = MachineModelRenderer.standalone("block/machine_autosaw_arm_tip");
    private static final ModelResourceLocation AUTOSAW_BLADE = MachineModelRenderer.standalone("block/machine_autosaw_blade");
    private static final ModelResourceLocation SAWMILL_MAIN = MachineModelRenderer.standalone("block/machine_sawmill_world");
    private static final ModelResourceLocation SAWMILL_BLADE = MachineModelRenderer.standalone("block/machine_sawmill_blade");
    private static final ModelResourceLocation SAWMILL_GEAR_LEFT = MachineModelRenderer.standalone("block/machine_sawmill_gear_left");
    private static final ModelResourceLocation SAWMILL_GEAR_RIGHT = MachineModelRenderer.standalone("block/machine_sawmill_gear_right");
    private static final ModelResourceLocation RTG_GEN = MachineModelRenderer.standalone("block/machine_rtg_grey_world");
    private static final ModelResourceLocation RTG_CONNECTOR = MachineModelRenderer.standalone("block/machine_rtg_grey_connector");
    // RenderAnnihilator in 1.7.10 renders these three original OBJ groups independently.
    private static final ModelResourceLocation ANNIHILATOR_BASE = MachineModelRenderer.standalone("block/machine_annihilator_base");
    private static final ModelResourceLocation ANNIHILATOR_ROLLER = MachineModelRenderer.standalone("block/machine_annihilator_roller");
    private static final ModelResourceLocation ANNIHILATOR_BELT = MachineModelRenderer.standalone("block/machine_annihilator_belt");
    private static final ResourceLocation ANNIHILATOR_BELT_TEXTURE = ReinhardtsHBM.id("textures/models/machines/annihilator_belt.png");
    private static final ModelResourceLocation FORCEFIELD_BASE = MachineModelRenderer.standalone("block/machine_forcefield_base");
    private static final ModelResourceLocation FORCEFIELD_TOP = MachineModelRenderer.standalone("block/machine_forcefield_top");
    private static final Map<Integer, ForcefieldSphereGeometry> FORCEFIELD_SPHERES = new ConcurrentHashMap<>();
    private static final ModelResourceLocation ORBUS = MachineModelRenderer.standalone("block/machine_orbus");
    private static final ModelResourceLocation ORBUS_FLUID_SPHERE = MachineModelRenderer.standalone("block/dfc_core_sphere_uv");
    private static final ModelResourceLocation PRECASS_BASE = MachineModelRenderer.standalone("block/machine_precass_base");
    private static final ModelResourceLocation PRECASS_FRAME = MachineModelRenderer.standalone("block/machine_precass_frame");
    private static final ModelResourceLocation PRECASS_RING = MachineModelRenderer.standalone("block/machine_precass_ring");
    private static final ModelResourceLocation PRECASS_RING2 = MachineModelRenderer.standalone("block/machine_precass_ring2");
    private static final ModelResourceLocation PRECASS_ARM_LOWER = MachineModelRenderer.standalone("block/machine_precass_armlower1");
    private static final ModelResourceLocation PRECASS_ARM_UPPER = MachineModelRenderer.standalone("block/machine_precass_armupper1");
    private static final ModelResourceLocation PRECASS_HEAD = MachineModelRenderer.standalone("block/machine_precass_head1");
    private static final ModelResourceLocation PRECASS_SPIKE = MachineModelRenderer.standalone("block/machine_precass_spike1");
    private static final ModelResourceLocation MISSILE_ASSEMBLY = MachineModelRenderer.standalone("block/machine_missile_assembly");
    private static final ModelResourceLocation MISSILE_STRUT = MachineModelRenderer.standalone("block/missile_strut");
    // Exact 1.7.10 RenderRadar / RenderRadarLarge object groups. Keeping the
    // base and dish independent prevents a static full OBJ from masking motion.
    private static final ModelResourceLocation RADAR_BASE = MachineModelRenderer.standalone("block/machine_radar_base");
    private static final ModelResourceLocation RADAR_DISH = MachineModelRenderer.standalone("block/machine_radar_dish");
    private static final ModelResourceLocation RADAR_LARGE_BASE = MachineModelRenderer.standalone("block/machine_radar_large_base");
    private static final ModelResourceLocation RADAR_LARGE_DISH = MachineModelRenderer.standalone("block/machine_radar_large_dish");
    private static final float RADAR_SWEEP_EXTENT = continuousRotationExtent(1.5461646096066226D);
    private static final float RADAR_LARGE_SWEEP_EXTENT = continuousRotationExtent(3.2621467720659045D);
    // RenderRadGen renders the old OBJ in four independent groups.
    private static final ModelResourceLocation RADGEN_BASE = MachineModelRenderer.standalone("block/machine_radgen_base");
    private static final ModelResourceLocation RADGEN_ROTOR = MachineModelRenderer.standalone("block/machine_radgen_rotor");
    private static final ModelResourceLocation RADGEN_LIGHT = MachineModelRenderer.standalone("block/machine_radgen_light");
    private static final ModelResourceLocation RADGEN_GLASS = MachineModelRenderer.standalone("block/machine_radgen_glass");
    private static final ModelResourceLocation RADIOLYSIS = MachineModelRenderer.standalone("block/machine_radiolysis");
    private static final ModelResourceLocation BREEDER = MachineModelRenderer.standalone("block/machine_reactor_breeding");
    // RenderTurbofan renders its original Body, Blades and Afterburner groups separately.
    private static final ModelResourceLocation TURBOFAN_BODY = MachineModelRenderer.standalone("block/machine_turbofan_body");
    private static final ModelResourceLocation TURBOFAN_BLADES = MachineModelRenderer.standalone("block/machine_turbofan_blades");
    private static final ModelResourceLocation TURBOFAN_AFTERBURNER_OFF = MachineModelRenderer.standalone("block/machine_turbofan_afterburner_off");
    private static final ModelResourceLocation TURBOFAN_AFTERBURNER_ON = MachineModelRenderer.standalone("block/machine_turbofan_afterburner_on");
    private static final ModelResourceLocation THRESHER_BASE = MachineModelRenderer.standalone("block/machine_thresher_base");
    private static final ModelResourceLocation THRESHER_ENGINE = MachineModelRenderer.standalone("block/machine_thresher_engine");
    private static final ModelResourceLocation THRESHER_UPPER = MachineModelRenderer.standalone("block/machine_thresher_arm_upper");
    private static final ModelResourceLocation THRESHER_LOWER = MachineModelRenderer.standalone("block/machine_thresher_arm_lower");
    private static final ModelResourceLocation THRESHER_FRONT = MachineModelRenderer.standalone("block/machine_thresher_front");
    private static final ModelResourceLocation THRESHER_WHEEL = MachineModelRenderer.standalone("block/machine_thresher_wheel");
    private static final ModelResourceLocation LPW2_WIRE_LEFT = MachineModelRenderer.standalone("block/machine_lpw2_wire_left");
    private static final ModelResourceLocation LPW2_WIRE_RIGHT = MachineModelRenderer.standalone("block/machine_lpw2_wire_right");
    private static final ModelResourceLocation LPW2_CENTER = MachineModelRenderer.standalone("block/machine_lpw2_center");
    private static final ModelResourceLocation LPW2_SUSPENSION_BACK_CENTER = MachineModelRenderer.standalone("block/machine_lpw2_suspension_back_center");
    private static final ModelResourceLocation LPW2_SUSPENSION_BACK_OUTER = MachineModelRenderer.standalone("block/machine_lpw2_suspension_back_outer");
    private static final ModelResourceLocation LPW2_SUSPENSION_LEFT = MachineModelRenderer.standalone("block/machine_lpw2_suspension_left");
    private static final ModelResourceLocation LPW2_SUSPENSION_RIGHT = MachineModelRenderer.standalone("block/machine_lpw2_suspension_right");
    private static final ModelResourceLocation LPW2_SUSPENSION_BOTTOM = MachineModelRenderer.standalone("block/machine_lpw2_suspension_bottom");
    private static final ModelResourceLocation LPW2_SUSPENSION_TOP = MachineModelRenderer.standalone("block/machine_lpw2_suspension_top");
    private static final ModelResourceLocation LPW2_SUSPENSION_COVER_BACK = MachineModelRenderer.standalone("block/machine_lpw2_suspension_cover_back");
    private static final ModelResourceLocation LPW2_SERVER1 = MachineModelRenderer.standalone("block/machine_lpw2_server1");
    private static final ModelResourceLocation LPW2_SERVER2 = MachineModelRenderer.standalone("block/machine_lpw2_server2");
    private static final ModelResourceLocation LPW2_SERVER3 = MachineModelRenderer.standalone("block/machine_lpw2_server3");
    private static final ModelResourceLocation LPW2_SERVER4 = MachineModelRenderer.standalone("block/machine_lpw2_server4");
    private static final ModelResourceLocation LPW2_MONITOR = MachineModelRenderer.standalone("block/machine_lpw2_monitor");
    private static final ModelResourceLocation LPW2_SCREEN = MachineModelRenderer.standalone("block/machine_lpw2_screen");
    private static final ModelResourceLocation LPW2_SUSPENSION_COVER_FRONT = MachineModelRenderer.standalone("block/machine_lpw2_suspension_cover_front");
    private static final ModelResourceLocation LPW2_COVER = MachineModelRenderer.standalone("block/machine_lpw2_cover");
    private static final ModelResourceLocation LPW2_PISTON = MachineModelRenderer.standalone("block/machine_lpw2_piston");
    private static final ModelResourceLocation LPW2_FLAP = MachineModelRenderer.standalone("block/machine_lpw2_flap");
    private static final ModelResourceLocation LPW2_TURBINE_BACK = MachineModelRenderer.standalone("block/machine_lpw2_turbine_back");
    private static final ModelResourceLocation LPW2_TURBINE_FRONT = MachineModelRenderer.standalone("block/machine_lpw2_turbine_front");
    private static final ModelResourceLocation LPW2_ROTOR = MachineModelRenderer.standalone("block/machine_lpw2_rotor");
    private static final ModelResourceLocation LPW2_SHROUD_V = MachineModelRenderer.standalone("block/machine_lpw2_shroud_v");
    private static final ModelResourceLocation LPW2_SHROUD_H = MachineModelRenderer.standalone("block/machine_lpw2_shroud_h");
    private static final ModelResourceLocation LPW2_ENGINE = MachineModelRenderer.standalone("block/machine_lpw2_engine");
    private static final ModelResourceLocation LPW2_FRAME = MachineModelRenderer.standalone("block/machine_lpw2_frame");
    private static final ResourceLocation LPW2_ERROR_TEXTURE = ReinhardtsHBM.id("textures/models/machines/lpw2_term_error.png");

    public LegacyMachineBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        LegacyMachineBlockEntity.setClientPrecisionIconSnapshotUpdater(
                LegacyMachineBlockEntityRenderer::updatePrecisionIconSnapshot);
    }

    static void invalidatePrecisionIconSnapshotsAfterBake() {
        LegacyMachineBlockEntity.setClientPrecisionIconSnapshotUpdater(
                LegacyMachineBlockEntityRenderer::updatePrecisionIconSnapshot);
    }

    public static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        event.register(PYRO_OVEN);
        event.register(PYRO_SLIDER);
        event.register(PYRO_FAN);
        event.register(AUTOSAW_BASE);
        event.register(AUTOSAW_MAIN);
        event.register(AUTOSAW_ENGINE);
        event.register(AUTOSAW_UPPER);
        event.register(AUTOSAW_LOWER);
        event.register(AUTOSAW_TIP);
        event.register(AUTOSAW_BLADE);
        event.register(SAWMILL_MAIN);
        event.register(SAWMILL_BLADE);
        event.register(SAWMILL_GEAR_LEFT);
        event.register(SAWMILL_GEAR_RIGHT);
        event.register(RTG_GEN);
        event.register(RTG_CONNECTOR);
        event.register(ANNIHILATOR_BASE);
        event.register(ANNIHILATOR_ROLLER);
        event.register(ANNIHILATOR_BELT);
        event.register(FORCEFIELD_BASE);
        event.register(FORCEFIELD_TOP);
        event.register(ORBUS);
        event.register(ORBUS_FLUID_SPHERE);
        event.register(PRECASS_BASE);
        event.register(PRECASS_FRAME);
        event.register(PRECASS_RING);
        event.register(PRECASS_RING2);
        event.register(PRECASS_ARM_LOWER);
        event.register(PRECASS_ARM_UPPER);
        event.register(PRECASS_HEAD);
        event.register(PRECASS_SPIKE);
        event.register(MISSILE_ASSEMBLY);
        event.register(MISSILE_STRUT);
        MissileMultipartRenderer.registerAdditionalModels(event);
        event.register(RADAR_BASE);
        event.register(RADAR_DISH);
        event.register(RADAR_LARGE_BASE);
        event.register(RADAR_LARGE_DISH);
        event.register(RADGEN_BASE);
        event.register(RADGEN_ROTOR);
        event.register(RADGEN_LIGHT);
        event.register(RADGEN_GLASS);
        event.register(RADIOLYSIS);
        event.register(BREEDER);
        event.register(TURBOFAN_BODY);
        event.register(TURBOFAN_BLADES);
        event.register(TURBOFAN_AFTERBURNER_OFF);
        event.register(TURBOFAN_AFTERBURNER_ON);
        event.register(THRESHER_BASE);
        event.register(THRESHER_ENGINE);
        event.register(THRESHER_UPPER);
        event.register(THRESHER_LOWER);
        event.register(THRESHER_FRONT);
        event.register(THRESHER_WHEEL);
        event.register(LPW2_WIRE_LEFT);
        event.register(LPW2_WIRE_RIGHT);
        event.register(LPW2_CENTER);
        event.register(LPW2_SUSPENSION_BACK_CENTER);
        event.register(LPW2_SUSPENSION_BACK_OUTER);
        event.register(LPW2_SUSPENSION_LEFT);
        event.register(LPW2_SUSPENSION_RIGHT);
        event.register(LPW2_SUSPENSION_BOTTOM);
        event.register(LPW2_SUSPENSION_TOP);
        event.register(LPW2_SUSPENSION_COVER_BACK);
        event.register(LPW2_SERVER1);
        event.register(LPW2_SERVER2);
        event.register(LPW2_SERVER3);
        event.register(LPW2_SERVER4);
        event.register(LPW2_MONITOR);
        event.register(LPW2_SCREEN);
        event.register(LPW2_SUSPENSION_COVER_FRONT);
        event.register(LPW2_COVER);
        event.register(LPW2_PISTON);
        event.register(LPW2_FLAP);
        event.register(LPW2_TURBINE_BACK);
        event.register(LPW2_TURBINE_FRONT);
        event.register(LPW2_ROTOR);
        event.register(LPW2_SHROUD_V);
        event.register(LPW2_SHROUD_H);
        event.register(LPW2_ENGINE);
        event.register(LPW2_FRAME);
    }

    @Override
    public void render(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        switch (machine.machineId()) {
            case "machine_pyrooven" -> renderPyroOven(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_autosaw" -> renderAutosaw(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_sawmill" -> renderSawmill(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_rtg_grey" -> renderRtg(machine, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_annihilator" -> renderAnnihilator(machine, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_forcefield" -> renderForcefield(machine, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_orbus" -> renderOrbus(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_precass" -> renderPrecisionAssembler(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_missile_assembly" -> renderMissileAssembly(machine, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_radar" -> renderRadar(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_radar_large" -> renderRadarLarge(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_radgen" -> renderRadGen(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_radiolysis" -> renderRadiolysis(machine, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_turbofan" -> renderTurbofan(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_thresher" -> renderThresher(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            case "machine_lpw2" -> renderLPW2(machine, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
            default -> {
            }
        }
    }

    @Override
    public AABB getRenderBoundingBox(LegacyMachineBlockEntity machine) {
        return switch (machine.machineId()) {
            /*
             * Exhaustive union of all 214,192 legal optional
             * thruster/fuselage/warhead/fins combinations.  Every referenced
             * baked-float OBJ vertex was transformed through the exact
             * T(0,1.5)*Rz(pi)*T(-height/2)*Rx(-pi/2)*Rz(-pi/2) hierarchy;
             * struts use the renderer's integer range/step loop.  These are
             * the resulting emitted-float extrema before the outer facing.
             */
            case "machine_missile_assembly" -> missileAssemblyBounds(machine);
            /*
             * These constants are extrema of the face-referenced OBJ vertices
             * after the complete renderer PoseStack hierarchy.  Revolving
             * parts use their analytic circle radius; articulated parts use
             * the extrema over every legal animation segment.
             */
            case "machine_autosaw" -> autosawBounds(machine);
            case "machine_pyrooven" -> pyroOvenBounds(machine);
            case "machine_sawmill" -> sawmillBounds(machine);
            case "machine_rtg_grey" -> rtgBounds(machine);
            case "machine_annihilator" -> annihilatorBounds(machine);
            case "machine_forcefield" -> forcefieldBounds(machine.getBlockPos(), machine.forcefieldRenderSnapshot());
            case "machine_orbus" -> orbusBounds(machine);
            case "machine_precass" -> {
                LegacyMachineBlockEntity.PrecisionIconRenderSnapshot snapshot = machine.precisionIconRenderSnapshot();
                AABB machineBounds = precisionAssemblerBounds(machine, snapshot.state());
                yield machineBounds.minmax(snapshot.bounds());
            }
            case "machine_radar" -> radarBounds(machine);
            case "machine_radar_large" -> radarLargeBounds(machine);
            case "machine_radgen" -> radgenBounds(machine);
            case "machine_radiolysis" -> radiolysisBounds(machine);
            case "machine_turbofan" -> turbofanBounds(machine);
            case "machine_thresher" -> thresherBounds(machine);
            case "machine_lpw2" -> lpw2Bounds(machine);
            default -> new AABB(machine.getBlockPos());
        };
    }

    private static double bakedMin(double value) {
        return Math.nextDown((float) value);
    }

    private static double bakedMax(double value) {
        return Math.nextUp((float) value);
    }

    /** Tight analytic full-Y-rotation envelope plus a proved float-operation allowance. */
    private static float continuousRotationExtent(double exactFaceRadius) {
        double radius = Math.nextUp(exactFaceRadius);
        double unitRoundoff = 0x1.0p-24;
        double gamma32 = 32.0D * unitRoundoff / (1.0D - 32.0D * unitRoundoff);
        return Math.nextUp((float) (radius + radius * gamma32));
    }

    private static AABB forcefieldBounds(net.minecraft.core.BlockPos pos,
                                         LegacyMachineBlockEntity.ForcefieldRenderSnapshot snapshot) {
        double horizontalX = 0.75D;
        double horizontalZ = 0.75D;
        double minY = 0.0D;
        double maxY = 2.0D;
        if (snapshot.renderable() && snapshot.radius() > 0) {
            int latitudes = Math.max(1, 16 + snapshot.radius() / 8);
            if ((latitudes & 1) == 0) {
                horizontalX = Math.max(horizontalX, snapshot.radius());
                horizontalZ = Math.max(horizontalZ, snapshot.radius());
            } else {
                double gridMaximum = Math.cos(Math.PI / (2.0D * latitudes));
                horizontalX = Math.max(horizontalX, snapshot.radius() * gridMaximum * gridMaximum);
                horizontalZ = Math.max(horizontalZ, snapshot.radius() * gridMaximum);
            }
            minY = Math.min(minY, 0.5D - snapshot.radius());
            maxY = Math.max(maxY, 0.5D + snapshot.radius());
        }
        // spherePoint is converted to float before PoseStack transformation.
        float xExtent = Math.nextUp((float) horizontalX);
        float zExtent = Math.nextUp((float) horizontalZ);
        PoseStack poseStack = new PoseStack();
        applyForcefieldTransform(poseStack);
        return RenderBounds.transformedBox(pos, poseStack,
                -xExtent, (float) minY, -zExtent, xExtent, (float) maxY, zExtent);
    }

    private static AABB missileAssemblyBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyMissileAssemblyTransform(poseStack, missileAssemblyFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -11.249998F, -0.24999976F, -4.6249995F,
                11.25F, 3.2499998F, 4.6249995F);
    }

    private static AABB autosawBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyAutosawTransform(poseStack);
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                (float) bakedMin(-11.125000538926983D), 0.0F, (float) bakedMin(-11.125000538926983D),
                (float) bakedMax(11.125000538926983D), (float) bakedMax(6.112798974949326D),
                (float) bakedMax(11.125000538926983D));
    }

    private static AABB pyroOvenBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyPyroOvenTransform(poseStack, pyroOvenFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -2.5F, 0.0F, -3.5F, 2.5F, 3.125F, 3.5F);
    }

    private static AABB sawmillBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applySawmillTransform(poseStack, sawmillFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -1.5F, 0.0F, -1.5F, 1.5F, (float) bakedMax(2.5000006244376047D), 1.5F);
    }

    private static AABB rtgBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyRtgTransform(poseStack);
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -0.5F, 0.0F, -0.5F, 0.5F, 1.0F, 0.5F);
    }

    private static AABB annihilatorBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyAnnihilatorTransform(poseStack, annihilatorFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -4.5F, 0.0F, -1.5F, 4.5F, 8.5F, 1.5F);
    }

    private static AABB precisionAssemblerBounds(LegacyMachineBlockEntity machine, BlockState state) {
        PoseStack poseStack = new PoseStack();
        applyPrecisionAssemblerTransform(poseStack, precisionAssemblerFacing(state));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                (float) bakedMin(-1.5977268591000684D), 0.0F, (float) bakedMin(-1.5977268591000684D),
                (float) bakedMax(1.5977268591000684D), (float) bakedMax(3.016965827560998D),
                (float) bakedMax(1.5977268591000684D));
    }

    private static AABB radarBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyRadarTransform(poseStack);
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -RADAR_SWEEP_EXTENT, 0.0F, -RADAR_SWEEP_EXTENT,
                RADAR_SWEEP_EXTENT, 2.25F, RADAR_SWEEP_EXTENT);
    }

    private static AABB radarLargeBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyRadarLargeTransform(poseStack);
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -RADAR_LARGE_SWEEP_EXTENT, 0.0F, -RADAR_LARGE_SWEEP_EXTENT,
                RADAR_LARGE_SWEEP_EXTENT, 10.0F, RADAR_LARGE_SWEEP_EXTENT);
    }

    private static AABB radgenBounds(LegacyMachineBlockEntity machine) {
        /*
         * Face-used Base/Glass/Light union is
         * [-3.5,0,-1.635184F]..[2.625,3,1.635184F].  The Rotor's full X
         * sweep about y=1.5 has radius 1.1267353279239476 and stays strictly
         * inside that fixed shell, so on/off never alters section coverage.
         */
        PoseStack poseStack = new PoseStack();
        applyRadGenTransform(poseStack, radGenFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -3.5F, 0.0F, -1.635184F, 2.625F, 3.0F, 1.635184F);
    }

    private static AABB radiolysisBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyRadiolysisTransform(poseStack, radiolysisFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -1.50562F, -0.008619F, -1.50562F, 1.50562F, 3.000036F, 1.50562F);
    }

    private static AABB turbofanBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyTurbofanTransform(poseStack, turbofanFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -1.5F, 0.0F, -3.5F, 1.5F, 3.0F, 3.125F);
    }

    private static AABB thresherBounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyThresherTransform(poseStack, thresherFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                (float) bakedMin(-3.24D), (float) bakedMin(-0.000000861831259912D),
                (float) bakedMin(-11.500000861831257D), (float) bakedMax(3.26D),
                (float) bakedMax(4.878360703579186D), 0.75F);
    }

    private static AABB lpw2Bounds(LegacyMachineBlockEntity machine) {
        PoseStack poseStack = new PoseStack();
        applyLpw2Transform(poseStack, lpw2Facing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                (float) bakedMin(-3.557874398363747D), (float) bakedMin(-0.057874398363747304D), -10.5F,
                (float) bakedMax(3.5578743983637464D), (float) bakedMax(7.057874398363747D),
                (float) bakedMax(10.48966587690142D));
    }

    private static AABB orbusBounds(LegacyMachineBlockEntity machine) {
        /*
         * orbus.obj uses every vertex and is exactly [-2,2] x [0,5.000001]
         * x [-2,2].  The full-fill sphere, bob, and the complete random beam
         * sweep remain inside that shell.  RenderOrbus does not rotate it; it
         * only applies this facing-specific legacy origin translation.
         */
        PoseStack poseStack = new PoseStack();
        applyOrbusTransform(poseStack, orbusFacing(machine.getBlockState()));
        return RenderBounds.transformedBox(machine.getBlockPos(), poseStack,
                -2.0F, 0.0F, -2.0F, 2.0F, 5.000001F, 2.0F);
    }

    static void renderItemPyro(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderPyroParts(0.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemAutosaw(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderAutosawParts(0.0D, 80.0D, System.currentTimeMillis() % 3600L * 0.1D, 0.0D, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemSawmill(BlockState state, boolean hasBlade, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        renderSawmillParts(System.currentTimeMillis() % 3600L * 0.1F, hasBlade, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemRtg(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RTG_GEN), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderThresher(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyThresherTransform(poseStack, thresherFacing(state));
        double angle = machine.thresherAngle(partialTick);
        double spin = machine.thresherSpin(partialTick);
        double engine = machine.thresherOn()
                ? Math.sin(machine.clientRenderGameTime() * 2.0D + partialTick) : 0.0D;
        renderThresherParts(82.5D - angle, spin, engine, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderThresherParts(double angle, double spin, double engine, BlockState state,
                                            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(THRESHER_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, engine * 0.01D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(THRESHER_ENGINE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.translate(0.0D, 0.5D, -1.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) angle));
        poseStack.translate(0.0D, -0.5D, 1.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(THRESHER_UPPER), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0D, 0.5D, -5.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) (-angle * 2.0D)));
        poseStack.translate(0.0D, -0.5D, 5.0D);
        poseStack.translate(-0.01D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(THRESHER_LOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.01D, 0.0D, 0.0D);

        poseStack.translate(0.0D, 0.5D, -9.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) angle));
        poseStack.translate(0.0D, -0.5D, 9.0D);
        poseStack.translate(0.01D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(THRESHER_FRONT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(-0.01D, 0.0D, 0.0D);

        poseStack.translate(0.0D, 0.5D, -11.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) -spin));
        poseStack.translate(0.0D, -0.5D, 11.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(THRESHER_WHEEL), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static void renderLPW2(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                   MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyLpw2Transform(poseStack, lpw2Facing(state));
        double time = machine.clientRenderGameTime() + partialTick;
        renderLPW2Parts(time, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLPW2Parts(double time, BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        double swayTimer = (time / 3.0D) % (Math.PI * 4.0D);
        double sway = (Math.sin(swayTimer) + Math.sin(swayTimer * 2.0D) + Math.sin(swayTimer * 4.0D) + 2.23255D) * 0.5D;
        double bellTimer = (time / 5.0D) % (Math.PI * 4.0D);
        double h = (Math.sin(bellTimer + Math.PI) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        double v = (Math.sin(bellTimer) + Math.sin(bellTimer * 1.5D)) / 1.90596D;
        double pistonTimer = (time / 5.0D) % (Math.PI * 2.0D);
        double piston = lpw2Sps(pistonTimer);
        double rotorTimer = (time / 5.0D) % (Math.PI * 16.0D);
        double rotor = (lpw2Sps(rotorTimer) + rotorTimer / 2.0D - 1.0D) / 25.1327412287D;
        double turbine = (time % 100.0D) / 100.0D;

        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderLPW2MainAssembly(sway, h, v, piston, rotor, turbine, state, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(-2.9375D, 0.0D, 2.375D);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (sway * 10.0D)));
        poseStack.translate(2.9375D, 0.0D, -2.375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_WIRE_LEFT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(2.9375D, 0.0D, 2.375D);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (sway * -10.0D)));
        poseStack.translate(-2.9375D, 0.0D, -2.375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_WIRE_RIGHT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        double coverTimer = (time / 5.0D) % (Math.PI * 4.0D);
        double cover = (Math.sin(coverTimer) + Math.sin(coverTimer * 2.0D) + Math.sin(coverTimer * 4.0D)) * 0.5D;
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -cover * 0.125D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_COVER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, 3.5D);
        poseStack.scale(1.0F, 1.0F, (float) ((3.0D + cover * 0.125D) / 3.0D));
        poseStack.translate(0.0D, 0.0D, -3.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_SUSPENSION_COVER_FRONT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -5.5D);
        poseStack.scale(1.0F, 1.0F, (float) ((1.5D - cover * 0.125D) / 1.5D));
        poseStack.translate(0.0D, 0.0D, 5.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_SUSPENSION_COVER_BACK), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -9.0D);
        poseStack.scale(1.0F, 1.0F, (float) ((1.25D - sway * 0.125D) / 1.25D));
        poseStack.translate(0.0D, 0.0D, 9.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_SUSPENSION_BACK_OUTER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -9.5D);
        poseStack.scale(1.0F, 1.0F, (float) ((1.75D - sway * 0.125D) / 1.75D));
        poseStack.translate(0.0D, 0.0D, 9.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_SUSPENSION_BACK_CENTER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        double serverTimer = (time / 2.0D) % (Math.PI * 4.0D);
        double sx = (Math.sin(serverTimer + Math.PI) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
        double sy = (Math.sin(serverTimer) + Math.sin(serverTimer * 1.5D)) / 1.90596D;
        double serverSway = 0.0625D * 0.25D;
        renderLPW2Translated(LPW2_SERVER1, sx * serverSway, 0.0D, sy * serverSway, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Translated(LPW2_SERVER2, -sy * serverSway, 0.0D, sx * serverSway, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Translated(LPW2_SERVER3, sy * serverSway, 0.0D, -sx * serverSway, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Translated(LPW2_SERVER4, -sx * serverSway, 0.0D, -sy * serverSway, state, poseStack, bufferSource, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(sy * serverSway, 0.0D, sx * serverSway);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_MONITOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        double errorTimer = time / 3.0D;
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(LPW2_SCREEN), poseStack, bufferSource, state,
                packedLight, packedOverlay, LPW2_ERROR_TEXTURE,
                0.0F, (float) ((lpw2Sps(errorTimer) + errorTimer / 2.0D) % 1.0D));
        poseStack.popPose();
    }

    private static void renderLPW2MainAssembly(double sway, double h, double v, double piston, double rotor, double turbine,
                                               BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                               int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, -sway * 0.125D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_CENTER), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        poseStack.translate(0.0D, 3.5D, 0.0D);
        poseStack.pushPose();
        poseStack.mulPose(MachineModelRenderer.zQuaternion((float) (-rotor * 360.0D)));
        poseStack.translate(0.0D, -3.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_ROTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(MachineModelRenderer.zQuaternion((float) (turbine * 360.0D)));
        poseStack.translate(0.0D, -3.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_TURBINE_FRONT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.mulPose(MachineModelRenderer.zQuaternion((float) (-turbine * 360.0D)));
        poseStack.translate(0.0D, -3.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_TURBINE_BACK), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(0.0D, 0.0D, piston * 0.375D + 0.375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_PISTON), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        renderLPW2Bell(h, v, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
        renderLPW2Shroud(h, v, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderLPW2Bell(double h, double v, BlockState state, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 3.5D, 2.75D);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) (v * 2.0D)));
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) (h * 2.0D)));
        poseStack.translate(0.0D, -3.5D, -2.75D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_ENGINE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLPW2Shroud(double h, double v, BlockState state, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        double magnitude = 0.125D;
        double rotation = 5.0D;
        double offset = 10.0D;
        poseStack.pushPose();
        poseStack.translate(0.0D, -h * magnitude, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_SHROUD_H), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderLPW2Flap(112.5D, rotation * v + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Flap(67.5D, rotation * v + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Flap(292.5D, rotation * -v + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Flap(247.5D, rotation * -v + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(v * magnitude, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_SHROUD_V), poseStack, bufferSource, state, packedLight, packedOverlay);
        renderLPW2Flap(22.5D, rotation * h + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Flap(-22.5D, rotation * h + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Flap(202.5D, rotation * -h + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Flap(157.5D, rotation * -h + offset, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();

        double length = 0.6875D;
        renderLPW2Scaled(LPW2_SUSPENSION_LEFT, -2.625D, 0.0D, 0.0D,
                (float) ((length + v * magnitude) / length), 1.0F, 1.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Scaled(LPW2_SUSPENSION_RIGHT, 2.625D, 0.0D, 0.0D,
                (float) ((length - v * magnitude) / length), 1.0F, 1.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Scaled(LPW2_SUSPENSION_TOP, 0.0D, 6.125D, 0.0D,
                1.0F, (float) ((length + h * magnitude) / length), 1.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderLPW2Scaled(LPW2_SUSPENSION_BOTTOM, 0.0D, 0.875D, 0.0D,
                1.0F, (float) ((length - h * magnitude) / length), 1.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    private static void renderLPW2Flap(double position, double rotation, BlockState state, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 3.5D, 0.0D);
        poseStack.mulPose(MachineModelRenderer.zQuaternion((float) position));
        poseStack.translate(0.0D, -3.5D, 0.0D);
        poseStack.translate(0.0D, 6.96875D, 8.5D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) rotation));
        poseStack.translate(0.0D, -6.96875D, -8.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_FLAP), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLPW2Translated(ModelResourceLocation model, double x, double y, double z, BlockState state,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderLPW2Scaled(ModelResourceLocation model, double x, double y, double z, float scaleX, float scaleY, float scaleZ,
                                         BlockState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(x, y, z);
        poseStack.scale(scaleX, scaleY, scaleZ);
        poseStack.translate(-x, -y, -z);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static double lpw2Sps(double x) {
        return Math.sin(Math.PI / 2.0D * Math.cos(x));
    }

    private static void renderForcefield(LegacyMachineBlockEntity machine, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        LegacyMachineBlockEntity.ForcefieldRenderSnapshot snapshot = machine.forcefieldRenderSnapshot();
        poseStack.pushPose();
        applyForcefieldTransform(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FORCEFIELD_BASE), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        if (snapshot.renderable()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.5D, 0.0D);
            renderForcefieldSphere(poseStack.last(), bufferSource.getBuffer(RenderType.lines()),
                    snapshot.radius(), snapshot.color());
            poseStack.popPose();
        }
        poseStack.pushPose();
        if (snapshot.renderable()) {
            float rotation = (System.currentTimeMillis() / 10.0F) % 360.0F;
            poseStack.mulPose(Axis.YP.rotationDegrees(-rotation));
        }
        poseStack.translate(0.0D, 1.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FORCEFIELD_TOP), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    private static void renderForcefieldHardware(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                                 int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FORCEFIELD_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        // RenderMachineForceField places the cap one block above radar_body.
        poseStack.translate(0.0D, 1.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FORCEFIELD_TOP), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderOrbus(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        HbmFluidTank tank = machine.tank(0);
        float fill = tank == null || tank.capacity() <= 0 ? 0.0F : (float) tank.amount() / tank.capacity();
        poseStack.pushPose();
        // RenderOrbus is the exception among the old machine renderers: it
        // uses the core's un-centred block origin and applies a facing-specific
        // translation. The core was deliberately offset by BlockDummyable,
        // so treating it as a generic centred OBJ shifts the whole tank.
        applyOrbusTransform(poseStack, orbusFacing(state));
        if (fill > 0.0F && tank != null) {
            double time = machine.clientRenderGameTime() + partialTick;
            poseStack.pushPose();
            poseStack.translate(0.0D, 2.5D + Math.sin(time * 0.1D) * 0.125D * fill, 0.0D);
            poseStack.scale(fill, fill, fill);
            MachineModelRenderer.renderUnculledTinted(
                    MachineModelRenderer.model(ORBUS_FLUID_SPHERE), poseStack, bufferSource, state,
                    LightTexture.FULL_BRIGHT, packedOverlay, 0xFF000000 | tank.type().color());
            poseStack.popPose();
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ORBUS), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (fill > 0.0F) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.0D, 0.0D);
            renderOrbusBeams(machine, fill, poseStack.last(), bufferSource.getBuffer(RenderType.lightning()));
            poseStack.popPose();
        }
        poseStack.popPose();
    }

    /** Exact RenderOrbus metadata table after BlockDummyable.offset. */
    private static void applyOrbusTransform(PoseStack poseStack, Direction facing) {
        switch (facing) {
            case NORTH -> poseStack.translate(1.0D, 0.0D, 1.0D);
            case WEST -> poseStack.translate(1.0D, 0.0D, 0.0D);
            case EAST -> poseStack.translate(0.0D, 0.0D, 1.0D);
            case SOUTH -> {
                // The old SOUTH case has no additional translation.
            }
            case UP, DOWN -> throw new IllegalArgumentException("Orbus facing must be horizontal: " + facing);
        }
    }

    private static void renderOrbusBeams(LegacyMachineBlockEntity machine, float fill, PoseStack.Pose pose,
                                         VertexConsumer consumer) {
        long time = machine.clientRenderGameTime();
        renderOrbusBeam(pose, consumer, false, 0, 1, 0.0D, 6, fill * 0.5D, 0x101020);
        renderOrbusBeam(pose, consumer, true, (int) (time / 2L % 1000L), 6, fill, 2, 0.0625D * fill, 0x202060);
        renderOrbusBeam(pose, consumer, true, (int) (time / 4L % 1000L), 6, fill, 2, 0.0625D * fill, 0x202060);
    }

    private static void renderOrbusBeam(PoseStack.Pose pose, VertexConsumer consumer, boolean randomWave, int phase,
                                        int segments, double waveRadius, int layers, double thickness, int color) {
        long randomState = (phase ^ JAVA_RANDOM_MULTIPLIER) & JAVA_RANDOM_MASK;
        double segmentHeight = 3.0D / segments;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= segments; index++) {
            double angle;
            if (randomWave) {
                randomState = advanceJavaRandom(randomState);
                float first = (float) (randomState >>> 24) * 0x1.0p-24F;
                randomState = advanceJavaRandom(randomState);
                float second = (float) (randomState >>> 24) * 0x1.0p-24F;
                angle = Math.PI * 2.0D * first + Math.PI * 2.0D * second;
            } else {
                angle = Math.toRadians(phase + 45.0D * index);
            }
            double currentX = Math.cos(angle) * waveRadius;
            double currentY = segmentHeight * index;
            double currentZ = Math.sin(angle) * waveRadius;
            if (index > 0) {
                renderOrbusBeamSegment(pose, consumer, previousX, previousY, previousZ,
                        currentX, currentY, currentZ, layers, thickness, color);
            }
            previousX = currentX;
            previousY = currentY;
            previousZ = currentZ;
        }
    }

    private static long advanceJavaRandom(long state) {
        return (state * JAVA_RANDOM_MULTIPLIER + JAVA_RANDOM_ADDEND) & JAVA_RANDOM_MASK;
    }

    private static void renderOrbusBeamSegment(PoseStack.Pose pose, VertexConsumer consumer,
                                               double fromX, double fromY, double fromZ,
                                               double toX, double toY, double toZ,
                                               int layers, double thickness, int color) {
        int outerRed = color >>> 16 & 255;
        int outerGreen = color >>> 8 & 255;
        int outerBlue = color & 255;
        double radius = thickness / layers;
        for (int layer = 1; layer <= layers; layer++) {
            double offset = radius * layer;
            renderOrbusQuad(pose, consumer,
                    fromX + offset, fromY, fromZ + offset,
                    fromX + offset, fromY, fromZ - offset,
                    toX + offset, toY, toZ - offset,
                    toX + offset, toY, toZ + offset,
                    outerRed, outerGreen, outerBlue);
            renderOrbusQuad(pose, consumer,
                    fromX - offset, fromY, fromZ + offset,
                    fromX - offset, fromY, fromZ - offset,
                    toX - offset, toY, toZ - offset,
                    toX - offset, toY, toZ + offset,
                    outerRed, outerGreen, outerBlue);
            renderOrbusQuad(pose, consumer,
                    fromX + offset, fromY, fromZ + offset,
                    fromX - offset, fromY, fromZ + offset,
                    toX - offset, toY, toZ + offset,
                    toX + offset, toY, toZ + offset,
                    outerRed, outerGreen, outerBlue);
            renderOrbusQuad(pose, consumer,
                    fromX + offset, fromY, fromZ - offset,
                    fromX - offset, fromY, fromZ - offset,
                    toX - offset, toY, toZ - offset,
                    toX + offset, toY, toZ - offset,
                    outerRed, outerGreen, outerBlue);
        }
    }

    private static void renderOrbusQuad(PoseStack.Pose pose, VertexConsumer consumer,
                                        double ax, double ay, double az, double bx, double by, double bz,
                                        double cx, double cy, double cz, double dx, double dy, double dz,
                                        int red, int green, int blue) {
        renderOrbusVertex(pose, consumer, ax, ay, az, red, green, blue);
        renderOrbusVertex(pose, consumer, bx, by, bz, red, green, blue);
        renderOrbusVertex(pose, consumer, cx, cy, cz, red, green, blue);
        renderOrbusVertex(pose, consumer, dx, dy, dz, red, green, blue);
    }

    private static void renderOrbusVertex(PoseStack.Pose pose, VertexConsumer consumer,
                                          double x, double y, double z, int red, int green, int blue) {
        consumer.addVertex(pose, (float) x, (float) y, (float) z)
                .setColor(red, green, blue, 255)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(pose, 0.0F, 1.0F, 0.0F);
    }

    private static void renderForcefieldSphere(PoseStack.Pose pose, VertexConsumer consumer, int radius, int color) {
        ForcefieldSphereGeometry geometry = FORCEFIELD_SPHERES.computeIfAbsent(radius,
                LegacyMachineBlockEntityRenderer::buildForcefieldSphereGeometry);
        float[] lines = geometry.lines();
        int red = color >>> 16 & 255;
        int green = color >>> 8 & 255;
        int blue = color & 255;
        for (int offset = 0; offset < lines.length; offset += 9) {
            consumer.addVertex(pose, lines[offset], lines[offset + 1], lines[offset + 2])
                    .setColor(red, green, blue, 255)
                    .setNormal(pose, lines[offset + 6], lines[offset + 7], lines[offset + 8]);
            consumer.addVertex(pose, lines[offset + 3], lines[offset + 4], lines[offset + 5])
                    .setColor(red, green, blue, 255)
                    .setNormal(pose, lines[offset + 6], lines[offset + 7], lines[offset + 8]);
        }
    }

    private static ForcefieldSphereGeometry buildForcefieldSphereGeometry(int radius) {
        int latitudes = Math.max(1, 16 + radius / 8);
        int longitudes = latitudes * 2;
        int lineCount = longitudes * latitudes + longitudes * (latitudes - 1);
        float[] lines = new float[lineCount * 9];
        int offset = 0;
        for (int longitude = 0; longitude < longitudes; longitude++) {
            double azimuth = Math.PI * 2.0D * longitude / longitudes;
            for (int latitude = 0; latitude < latitudes; latitude++) {
                double from = Math.PI * latitude / latitudes;
                double to = Math.PI * (latitude + 1) / latitudes;
                offset = storeForcefieldLine(lines, offset,
                        spherePoint(radius, from, azimuth), spherePoint(radius, to, azimuth));
            }
        }
        for (int latitude = 1; latitude < latitudes; latitude++) {
            double polar = Math.PI * latitude / latitudes;
            for (int longitude = 0; longitude < longitudes; longitude++) {
                double from = Math.PI * 2.0D * longitude / longitudes;
                double to = Math.PI * 2.0D * (longitude + 1) / longitudes;
                offset = storeForcefieldLine(lines, offset,
                        spherePoint(radius, polar, from), spherePoint(radius, polar, to));
            }
        }
        if (offset != lines.length) {
            throw new IllegalStateException("Forcefield sphere emitted " + offset + " floats into " + lines.length);
        }
        return new ForcefieldSphereGeometry(lines);
    }

    private static net.minecraft.world.phys.Vec3 spherePoint(int radius, double polar, double azimuth) {
        double horizontal = radius * Math.sin(polar);
        return new net.minecraft.world.phys.Vec3(horizontal * Math.sin(azimuth), radius * Math.cos(polar), horizontal * Math.cos(azimuth));
    }

    private static int storeForcefieldLine(float[] lines, int offset, net.minecraft.world.phys.Vec3 from,
                                           net.minecraft.world.phys.Vec3 to) {
        net.minecraft.world.phys.Vec3 normal = to.subtract(from).normalize();
        lines[offset++] = (float) from.x;
        lines[offset++] = (float) from.y;
        lines[offset++] = (float) from.z;
        lines[offset++] = (float) to.x;
        lines[offset++] = (float) to.y;
        lines[offset++] = (float) to.z;
        lines[offset++] = (float) normal.x;
        lines[offset++] = (float) normal.y;
        lines[offset++] = (float) normal.z;
        return offset;
    }

    private record ForcefieldSphereGeometry(float[] lines) {
    }

    static void renderItemAnnihilator(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                       int packedLight, int packedOverlay) {
        renderAnnihilatorParts(0.0F, 0.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemForcefield(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        renderForcefieldHardware(state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemMissileAssembly(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MISSILE_ASSEMBLY),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    static void renderItemOrbus(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ORBUS),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    static void renderItemTurbofan(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        renderTurbofanParts(0.0F, false, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemGasTurbine(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(GasTurbineBlockEntityRenderer.MODEL),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    static void renderItemThresher(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                    int packedLight, int packedOverlay) {
        renderThresherParts(80.0D, System.currentTimeMillis() % 3600L * 0.25D, 0.0D,
                state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemLpw2(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        renderLPW2Parts(System.currentTimeMillis() / 50.0D,
                state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemRadiationGenerator(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                              int packedLight, int packedOverlay) {
        renderRadGenParts(true, 0.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
    }

    static void renderItemRadiolysis(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                      int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADIOLYSIS),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    static void renderItemBreedingReactor(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                           int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(BREEDER),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    /** Direct port of RenderRadGen, including the opaque glass redraw after the translucent pass. */
    private static void renderRadGen(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyRadGenTransform(poseStack, radGenFacing(state));
        float rotorDegrees = machine.radgenOn()
                ? ((machine.clientRenderGameTime() + partialTick) % 72.0F) * -5.0F
                : 0.0F;
        renderRadGenParts(machine.radgenOn(), rotorDegrees, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderRadGenParts(boolean on, float rotorDegrees, BlockState state, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADGEN_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.pushPose();
        if (rotorDegrees != 0.0F) {
            poseStack.translate(0.0D, 1.5D, 0.0D);
            poseStack.mulPose(MachineModelRenderer.xQuaternion(rotorDegrees));
            poseStack.translate(0.0D, -1.5D, 0.0D);
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADGEN_ROTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        MachineModelRenderer.renderUnculledTintedUvEyes(MachineModelRenderer.model(RADGEN_LIGHT), poseStack, bufferSource,
                state, packedOverlay, on ? 0xFF00FF00 : 0xFF001A00, 0.0F, 0.0F);
        MachineModelRenderer.renderUnculledTintedTranslucent(MachineModelRenderer.model(RADGEN_GLASS), poseStack, bufferSource,
                state, packedLight, packedOverlay, 0x4D80BFFF);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADGEN_GLASS), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    /** Exact RenderPrecAss part hierarchy; the four arms are the same exported arm rotated around the ring. */
    private static void renderPrecisionAssembler(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        LegacyMachineBlockEntity.PrecisionIconRenderSnapshot recipeIcon = machine.precisionIconRenderSnapshot();
        BlockState state = recipeIcon.state();
        poseStack.pushPose();
        applyPrecisionAssemblerTransform(poseStack, precisionAssemblerFacing(state));
        renderPrecisionAssemblerParts(machine, partialTick, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPrecisionAssemblerRecipeIcon(machine, recipeIcon, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPrecisionAssemblerParts(LegacyMachineBlockEntity machine, float partialTick, BlockState state,
                                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (machine.renderNeighborSnapshot().precisionFrameVisible()) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((float) machine.precisionAssemblerRing(partialTick)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING2), poseStack, bufferSource, state, packedLight, packedOverlay);
        for (int arm = 0; arm < 4; arm++) {
            renderPrecisionAssemblerArm(machine, arm, partialTick, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        }
        poseStack.popPose();
    }

    private static void renderPrecisionAssemblerArm(LegacyMachineBlockEntity machine, int arm, float partialTick, BlockState state,
                                                     PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.625D, 0.9375D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) machine.precisionAssemblerArmAngle(0, partialTick)));
        poseStack.translate(0.0D, -1.625D, -0.9375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_ARM_LOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, 2.375D, 0.9375D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) machine.precisionAssemblerArmAngle(1, partialTick)));
        poseStack.translate(0.0D, -2.375D, -0.9375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_ARM_UPPER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, 2.375D, 0.4375D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) machine.precisionAssemblerArmAngle(2, partialTick)));
        poseStack.translate(0.0D, -2.375D, -0.4375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_HEAD), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, machine.precisionAssemblerStriker(arm, partialTick), 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_SPIKE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPrecisionAssemblerRecipeIcon(LegacyMachineBlockEntity machine,
                                                            LegacyMachineBlockEntity.PrecisionIconRenderSnapshot snapshot,
                                                            PoseStack poseStack,
                                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null
                || minecraft.player.distanceToSqr(machine.getBlockPos().getX() + 0.5D, machine.getBlockPos().getY() + 1.0D, machine.getBlockPos().getZ() + 0.5D) >= 1225.0D) return;
        ItemStack icon = snapshot.renderedItem();
        if (icon.isEmpty() || !(snapshot.modelToken() instanceof net.minecraft.client.resources.model.BakedModel model)) return;
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.0625D, 0.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-90.0F), 0.0F, 0.0F, 1.0F)));
        poseStack.scale(1.25F, 1.25F, 1.25F);
        minecraft.getItemRenderer().render(icon, ItemDisplayContext.FIXED, false,
                poseStack, bufferSource, packedLight, packedOverlay, model);
        poseStack.popPose();
    }

    /** Main-thread recipe/model capture; section workers only read the published record. */
    private static void updatePrecisionIconSnapshot(LegacyMachineBlockEntity machine) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!minecraft.isSameThread()) {
            minecraft.execute(machine::refreshClientPrecisionIconSnapshot);
            return;
        }
        Level level = machine.getLevel();
        if (level == null || !machine.machineId().equals("machine_precass")) {
            return;
        }

        RecipeHolder<PrecisionAssemblerRecipe> holder = machine.selectedPrecisionAssemblerRecipe(level).orElse(null);
        ItemStack source = holder == null || holder.value().outputs().isEmpty()
                ? ItemStack.EMPTY : holder.value().outputs().getFirst().stack().copyWithCount(1);
        BlockState state = machine.getBlockState();
        long generation = com.reinhardt.hbm.recipe.MachineRecipeCache.generation();
        long revision = LegacyMachineBlockEntity.clientPrecisionIconSnapshotRevision();
        LegacyMachineBlockEntity.PrecisionIconRenderSnapshot previous = machine.precisionIconRenderSnapshot();
        if (previous.revision() == revision
                && previous.recipeGeneration() == generation
                && previous.state() == state
                && java.util.Objects.equals(previous.recipeId(), holder == null ? null : holder.id())
                && ItemStack.isSameItemSameComponents(previous.sourceItem(), source)) {
            machine.publishClientPrecisionIconSnapshot(previous);
            return;
        }

        PoseStack itemPose = new PoseStack();
        applyPrecisionAssemblerTransform(itemPose, precisionAssemblerFacing(state));
        itemPose.translate(0.0F, 1.0625F, 0.0F);
        itemPose.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
        itemPose.mulPose(new Quaternionf(new AxisAngle4f(
                (float) Math.toRadians(-90.0F), 0.0F, 0.0F, 1.0F)));
        itemPose.scale(1.25F, 1.25F, 1.25F);
        BakedItemGeometrySnapshot.Result geometry = BakedItemGeometrySnapshot.build(
                source, itemPose, machine.getBlockPos(), level);
        machine.publishClientPrecisionIconSnapshot(new LegacyMachineBlockEntity.PrecisionIconRenderSnapshot(
                state, holder == null ? null : holder.id(), geometry.sourceItem(), geometry.renderedItem(),
                geometry.modelToken(), geometry.bounds(), generation, revision));
    }

    static void renderItemPrecisionAssembler(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                              int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING2), poseStack, bufferSource, state, packedLight, packedOverlay);
        for (int arm = 0; arm < 4; arm++) {
            renderItemPrecisionAssemblerArm(state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
    }

    private static void renderItemPrecisionAssemblerArm(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                                        int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.625D, 0.9375D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion(45.0F));
        poseStack.translate(0.0D, -1.625D, -0.9375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_ARM_LOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, 2.375D, 0.9375D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion(-30.0F));
        poseStack.translate(0.0D, -2.375D, -0.9375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_ARM_UPPER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0D, 2.375D, 0.4375D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion(45.0F));
        poseStack.translate(0.0D, -2.375D, -0.4375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_HEAD), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_SPIKE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderRadiolysis(LegacyMachineBlockEntity machine,
                                         PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyRadiolysisTransform(poseStack, radiolysisFacing(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADIOLYSIS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of RenderRadar: base is fixed, dish turns around the old negative-Y axis. */
    private static void renderRadar(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyRadarTransform(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees(-machine.radarRotation(partialTick)));
        poseStack.translate(-0.125D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_DISH), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of RenderRadarLarge: only Dish rotates; Radar remains fixed. */
    private static void renderRadarLarge(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyRadarLargeTransform(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees(-machine.radarRotation(partialTick)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_DISH), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    static void renderItemRadar(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_BASE),
                poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(System.currentTimeMillis() % 3600L) * 0.1F));
        poseStack.translate(-0.125D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_DISH),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    static void renderItemLargeRadar(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                     int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_BASE),
                poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees(-(System.currentTimeMillis() % 3600L) * 0.1F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_DISH),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    /** Direct port of RenderMissileAssembly, including the gantry-relative missile transform. */
    private static void renderMissileAssembly(LegacyMachineBlockEntity machine, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        ItemStack warhead = machine.getItem(1);
        ItemStack fuselage = machine.getItem(2);
        ItemStack fins = machine.getItem(3);
        ItemStack thruster = machine.getItem(4);
        float height = MissileMultipartRenderer.height(warhead, fuselage, thruster);
        poseStack.pushPose();
        applyMissileAssemblyTransform(poseStack, missileAssemblyFacing(state));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MISSILE_ASSEMBLY), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (height > 0.0F) {
            renderMissileStruts(height, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.translate(0.0F, 1.5F, 0.0F);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.PI, 0.0F, 0.0F, 1.0F)));
            poseStack.translate(-height / 2.0F, 0.0F, 0.0F);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) -Math.PI / 2.0F, 1.0F, 0.0F, 0.0F)));
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) -Math.PI / 2.0F, 0.0F, 0.0F, 1.0F)));
            MissileMultipartRenderer.render(state, poseStack, bufferSource, packedLight, packedOverlay, warhead, fuselage, fins, thruster);
        }
        poseStack.popPose();
    }

    /** Direct port of RenderMissileAssembly's height-dependent support gantry. */
    private static void renderMissileStruts(float height, BlockState state, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        int range = (int) (height / 2.0F - 1.0F);
        int step = range >= 2 ? 2 : 1;
        for (int offset = -range; offset <= range; offset += step) {
            if (offset == 0) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(offset, 0.0F, 0.0F);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MISSILE_STRUT), poseStack,
                    bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
    }

    /** Direct transform port of RenderTurbofan, including the negative-Z blade axis. */
    private static void renderTurbofan(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyTurbofanTransform(poseStack, turbofanFacing(state));
        renderTurbofanParts(machine.turbofanSpin(partialTick), machine.turbofanAfterburner() > 0,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderTurbofanParts(float spin, boolean afterburning, BlockState state, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TURBOFAN_BODY), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.5D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(spin), 0.0F, 0.0F, -1.0F)));
        poseStack.translate(0.0D, -1.5D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(TURBOFAN_BLADES), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(
                afterburning ? TURBOFAN_AFTERBURNER_ON : TURBOFAN_AFTERBURNER_OFF),
                poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    /** Exact hierarchy of 1.7.10 RenderAnnihilator: fixed frame, rolling wheel and scrolling belt. */
    private static void renderAnnihilator(LegacyMachineBlockEntity machine, PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyAnnihilatorTransform(poseStack, annihilatorFacing(state));
        float rollerDegrees = (float) ((System.currentTimeMillis() * 0.15D) % 360.0D);
        float beltOffset = (float) -((System.currentTimeMillis() / 3000.0D) % 1.0D);
        renderAnnihilatorParts(rollerDegrees, beltOffset, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAnnihilatorParts(float rollerDegrees, float beltOffset, BlockState state, PoseStack poseStack,
                                               MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ANNIHILATOR_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.75D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-rollerDegrees), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(0.0D, -1.75D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ANNIHILATOR_ROLLER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        // The 1.7.10 belt was an ordinary opaque model with only its texture
        // matrix scrolling.  Rendering it through a translucent/tinted type
        // causes depth fighting with the rollers in modern chunk rendering.
        MachineModelRenderer.renderUnculledUv(MachineModelRenderer.model(ANNIHILATOR_BELT), poseStack,
                bufferSource, state, packedLight, packedOverlay, ANNIHILATOR_BELT_TEXTURE, beltOffset, 0.0F);
    }

    private static void renderPyroOven(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyPyroOvenTransform(poseStack, pyroOvenFacing(state));
        renderPyroParts(machine.pyroAnimation() + partialTick, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPyroParts(float animation, BlockState state, PoseStack poseStack,
                                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PYRO_OVEN), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(Math.sin(Math.PI / 2.0D * Math.cos(animation * 0.125D)) / 2.0D - 0.5D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PYRO_SLIDER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(1.5D, 0.0D, 1.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(animation * 45.0F % 360.0F));
        poseStack.translate(-1.5D, 0.0D, -1.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PYRO_FAN), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAutosaw(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        double engine = machine.autosawOn()
                ? Math.sin(machine.clientRenderGameTime() * 2.0D + partialTick)
                : 0.0D;
        poseStack.pushPose();
        applyAutosawTransform(poseStack);
        renderAutosawParts(machine.autosawYaw(partialTick), 80.0D - machine.autosawPitch(partialTick), machine.autosawSpin(partialTick), engine,
                state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct transform port of RenderSawmill#renderCommon. */
    private static void renderSawmill(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        float rotation = machine.sawmillRotation() + machine.sawmillRotationSpeed() * partialTick;
        poseStack.pushPose();
        applySawmillTransform(poseStack, sawmillFacing(state));
        renderSawmillParts(rotation, machine.sawmillHasBlade(), state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderSawmillParts(float rotation, boolean hasBlade, BlockState state, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SAWMILL_MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (hasBlade) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 1.375D, 0.0D);
            poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-rotation * 2.0F), 0.0F, 0.0F, 1.0F)));
            poseStack.translate(0.0D, -1.375D, 0.0D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SAWMILL_BLADE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
        }
        poseStack.pushPose();
        poseStack.translate(0.5625D, 1.375D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(rotation), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(-0.5625D, -1.375D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SAWMILL_GEAR_LEFT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.pushPose();
        poseStack.translate(-0.5625D, 1.375D, 0.0D);
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-rotation), 0.0F, 0.0F, 1.0F)));
        poseStack.translate(0.5625D, -1.375D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(SAWMILL_GEAR_RIGHT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of RenderRTG: the connector is shown only on a connected face. */
    private static void renderRtg(LegacyMachineBlockEntity machine, PoseStack poseStack,
                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        applyRtgTransform(poseStack);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RTG_GEN), poseStack, bufferSource, state, packedLight, packedOverlay);
        LegacyMachineBlockEntity.RenderNeighborSnapshot neighbors = machine.renderNeighborSnapshot();
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (neighbors.rtgConnector(direction)) {
                poseStack.pushPose();
                applyRtgConnectorTransform(poseStack, direction);
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RTG_CONNECTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    private static void renderAutosawParts(double turn, double angle, double spin, double engine, BlockState state,
                                           PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) -turn));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_MAIN), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0D, engine * 0.01D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_ENGINE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.translate(0.0D, 1.75D, 0.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) angle));
        poseStack.translate(0.0D, -1.75D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_UPPER), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0D, 1.75D, -4.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) (-angle * 2.0D)));
        poseStack.translate(0.0D, -1.75D, 4.0D);
        poseStack.translate(-0.01D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_LOWER), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.01D, 0.0D, 0.0D);

        poseStack.translate(0.0D, 1.75D, -8.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion((float) angle));
        poseStack.translate(0.0D, -1.75D, 8.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_TIP), poseStack, bufferSource, state, packedLight, packedOverlay);

        poseStack.translate(0.0D, 1.75D, -10.0D);
        poseStack.mulPose(Axis.YP.rotationDegrees((float) -spin));
        poseStack.translate(0.0D, -1.75D, 10.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_BLADE), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static Direction missileAssemblyFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Missile assembly state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction pyroOvenFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Pyro oven state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction sawmillFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Sawmill state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction annihilatorFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Annihilator state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction precisionAssemblerFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Precision assembler state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction radGenFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("RadGen state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction radiolysisFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Radiolysis state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction turbofanFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Turbofan state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction thresherFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Thresher state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction lpw2Facing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("LPW2 state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    private static Direction orbusFacing(BlockState state) {
        if (!state.hasProperty(LargeMachineBlock.FACING)) {
            throw new IllegalStateException("Orbus state is missing its dedicated facing property");
        }
        return state.getValue(LargeMachineBlock.FACING);
    }

    /** RenderPyroOven metadata-minus-offset table: 2/4/3/5 -> 180/270/0/90. */
    private static void applyPyroOvenTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> { }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Pyro oven facing must be horizontal: " + facing);
        }
    }

    /** RenderSawmill metadata-minus-offset table, kept independent from the pyro oven. */
    private static void applySawmillTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> { }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Sawmill facing must be horizontal: " + facing);
        }
    }

    /** RenderAutosaw has no metadata rotation; its turret supplies its own dynamic yaw. */
    private static void applyAutosawTransform(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
    }

    /** RenderRTG always rotates its exported body by 180 degrees. */
    private static void applyRtgTransform(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    /** RenderAnnihilator's fixed 90-degree rotation followed by its metadata table. */
    private static void applyAnnihilatorTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Annihilator facing must be horizontal: " + facing);
        }
    }

    /** RenderMachineForceField always uses a fixed 180-degree body transform. */
    private static void applyForcefieldTransform(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    /** RenderPrecAss's fixed 90-degree rotation followed by its metadata table. */
    private static void applyPrecisionAssemblerTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Precision assembler facing must be horizontal: " + facing);
        }
    }

    /** RenderMissileAssembly metadata table: 2/4/3/5 -> 180/270/0/90. */
    private static void applyMissileAssemblyTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> { }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Missile assembly facing must be horizontal: " + facing);
        }
    }

    /** RenderRadar always rotates the fixed base by 180 degrees. */
    private static void applyRadarTransform(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    /** RenderRadarLarge uses the same fixed transform, retained as its own renderer rule. */
    private static void applyRadarLargeTransform(PoseStack poseStack) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
    }

    /** RenderRadGen metadata-minus-offset table: 2/4/3/5 -> 90/180/270/0. */
    private static void applyRadGenTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("RadGen facing must be horizontal: " + facing);
        }
    }

    /** RenderRadiolysis metadata-minus-offset table: 2/3/4/5 -> 180/0/90/270. */
    private static void applyRadiolysisTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Radiolysis facing must be horizontal: " + facing);
        }
    }

    /** RenderTurbofan metadata-minus-10 table: 2/4/3/5 -> 90/180/270/0. */
    private static void applyTurbofanTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Turbofan facing must be horizontal: " + facing);
        }
    }

    /** RenderThresher metadata table: 3/5/2/4 -> 180/270/0/90. */
    private static void applyThresherTransform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case SOUTH -> { }
            case EAST -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("Thresher facing must be horizontal: " + facing);
        }
    }

    /** RenderLPW2 metadata-minus-offset table: 2/4/3/5 -> 90/180/270/0. */
    private static void applyLpw2Transform(PoseStack poseStack, Direction facing) {
        poseStack.translate(0.5D, 0.0D, 0.5D);
        switch (facing) {
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(270.0F));
            case UP, DOWN -> throw new IllegalArgumentException("LPW2 facing must be horizontal: " + facing);
        }
    }

    /** RenderRTG connector directions after the body's fixed 180-degree rotation. */
    private static void applyRtgConnectorTransform(PoseStack poseStack, Direction direction) {
        switch (direction) {
            case EAST -> { }
            case WEST -> poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
            case NORTH -> poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            case SOUTH -> poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
            case UP, DOWN -> throw new IllegalArgumentException("RTG connector direction must be horizontal: " + direction);
        }
    }
}
