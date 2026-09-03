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
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import com.reinhardt.hbm.power.PowerNetworkManager;
import net.neoforged.neoforge.client.event.ModelEvent;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;

import java.util.Random;

/** Legacy 1.7.10 rendering tables for Pyro Oven, Conveyor Press and Autosaw. */
public final class LegacyMachineBlockEntityRenderer implements BlockEntityRenderer<LegacyMachineBlockEntity> {
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
            case "machine_radiolysis" -> renderLegacyObj(machine, RADIOLYSIS, radiolysisYaw(facing(state)), poseStack, bufferSource, packedLight, packedOverlay);
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
            // TileEntityMachineMissileAssembly returned INFINITE_EXTENT_AABB:
            // the assembled missile and its supports can extend far beyond the core.
            case "machine_missile_assembly" -> AABB.INFINITE;
            case "machine_autosaw" -> new AABB(machine.getBlockPos()).inflate(12.0D, 6.0D, 12.0D);
            case "machine_pyrooven" -> new AABB(machine.getBlockPos()).inflate(5.0D, 4.0D, 5.0D);
            case "machine_sawmill" -> new AABB(machine.getBlockPos()).inflate(2.0D, 3.0D, 2.0D);
            case "machine_rtg_grey" -> new AABB(machine.getBlockPos()).inflate(1.0D);
            case "machine_annihilator" -> new AABB(machine.getBlockPos()).inflate(5.0D, 9.0D, 5.0D);
            case "machine_forcefield" -> new AABB(machine.getBlockPos()).inflate(2.0D, 2.0D, 2.0D);
            // TileEntityMachineOrbus#getRenderBoundingBox: x/z -2..+3, y 0..+5.
            case "machine_orbus" -> new AABB(
                    machine.getBlockPos().getX() - 2.0D, machine.getBlockPos().getY(), machine.getBlockPos().getZ() - 2.0D,
                    machine.getBlockPos().getX() + 3.0D, machine.getBlockPos().getY() + 5.0D, machine.getBlockPos().getZ() + 3.0D
            );
            case "machine_precass" -> new AABB(machine.getBlockPos()).inflate(2.0D, 4.0D, 2.0D);
            case "machine_radar" -> new AABB(machine.getBlockPos()).inflate(2.0D, 2.0D, 2.0D);
            // TileEntityMachineRadarLarge#getRenderBoundingBox: x/z -5..+6, y 0..+10.
            case "machine_radar_large" -> new AABB(
                    machine.getBlockPos().getX() - 5.0D, machine.getBlockPos().getY(), machine.getBlockPos().getZ() - 5.0D,
                    machine.getBlockPos().getX() + 6.0D, machine.getBlockPos().getY() + 10.0D, machine.getBlockPos().getZ() + 6.0D
            );
            // TileEntityMachineRadGen explicitly returned INFINITE_EXTENT_AABB.
            case "machine_radgen" -> AABB.INFINITE;
            // TileEntityMachineRadiolysis#getRenderBoundingBox: x/z -1..+2, y 0..+3.
            case "machine_radiolysis" -> new AABB(
                    machine.getBlockPos().getX() - 1.0D, machine.getBlockPos().getY(), machine.getBlockPos().getZ() - 1.0D,
                    machine.getBlockPos().getX() + 2.0D, machine.getBlockPos().getY() + 3.0D, machine.getBlockPos().getZ() + 2.0D
            );
            case "machine_turbofan" -> new AABB(machine.getBlockPos()).inflate(4.0D, 4.0D, 4.0D);
            case "machine_thresher", "machine_lpw2" -> new AABB(
                    machine.getBlockPos().getX() - 10.0D, machine.getBlockPos().getY(), machine.getBlockPos().getZ() - 10.0D,
                    machine.getBlockPos().getX() + 11.0D, machine.getBlockPos().getY() + 7.0D, machine.getBlockPos().getZ() + 11.0D
            );
            default -> new AABB(machine.getBlockPos());
        };
    }

    @Override
    public boolean shouldRenderOffScreen(LegacyMachineBlockEntity machine) {
        return machine.machineId().equals("machine_missile_assembly");
    }

    @Override
    public int getViewDistance() {
        // TileEntityMachineMissileAssembly#getMaxRenderDistanceSquared = 65,536.
        return 256;
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(thresherYaw(facing(state))));
        double angle = machine.thresherAngle(partialTick);
        double spin = machine.thresherSpin(partialTick);
        double engine = machine.thresherOn() && machine.getLevel() != null
                ? Math.sin(machine.getLevel().getGameTime() * 2.0D + partialTick) : 0.0D;
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(lpw2Yaw(facing(state))));
        double time = (machine.getLevel() == null ? 0L : machine.getLevel().getGameTime()) + partialTick;
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
        poseStack.mulPose(yaw((float) (sway * 10.0D)));
        poseStack.translate(2.9375D, 0.0D, -2.375D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(LPW2_WIRE_LEFT), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();

        poseStack.pushPose();
        poseStack.translate(2.9375D, 0.0D, 2.375D);
        poseStack.mulPose(yaw((float) (sway * -10.0D)));
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
        poseStack.mulPose(yaw((float) (v * 2.0D)));
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
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(180.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(FORCEFIELD_BASE), poseStack,
                bufferSource, state, packedLight, packedOverlay);
        if (machine.forcefieldRenderable()) {
            poseStack.pushPose();
            poseStack.translate(0.0D, 0.5D, 0.0D);
            renderForcefieldSphere(poseStack.last(), bufferSource.getBuffer(RenderType.lines()),
                    machine.forcefieldRadius(), machine.forcefieldColor());
            poseStack.popPose();
        }
        poseStack.pushPose();
        if (machine.forcefieldRenderable()) {
            float rotation = (System.currentTimeMillis() / 10.0F) % 360.0F;
            poseStack.mulPose(yaw(-rotation));
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
        translateOrbusOrigin(facing(state), poseStack);
        if (fill > 0.0F && tank != null) {
            double time = (machine.getLevel() == null ? 0L : machine.getLevel().getGameTime()) + partialTick;
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
    private static void translateOrbusOrigin(Direction facing, PoseStack poseStack) {
        switch (facing) {
            case NORTH -> poseStack.translate(1.0D, 0.0D, 1.0D);
            case WEST -> poseStack.translate(1.0D, 0.0D, 0.0D);
            case EAST -> poseStack.translate(0.0D, 0.0D, 1.0D);
            case SOUTH -> {
                // The old SOUTH case has no additional translation.
            }
            default -> {
            }
        }
    }

    private static void renderOrbusBeams(LegacyMachineBlockEntity machine, float fill, PoseStack.Pose pose,
                                         VertexConsumer consumer) {
        long time = machine.getLevel() == null ? 0L : machine.getLevel().getGameTime();
        renderOrbusBeam(pose, consumer, false, 0, 1, 0.0D, 6, fill * 0.5D, 0x101020);
        renderOrbusBeam(pose, consumer, true, (int) (time / 2L % 1000L), 6, fill, 2, 0.0625D * fill, 0x202060);
        renderOrbusBeam(pose, consumer, true, (int) (time / 4L % 1000L), 6, fill, 2, 0.0625D * fill, 0x202060);
    }

    private static void renderOrbusBeam(PoseStack.Pose pose, VertexConsumer consumer, boolean randomWave, int phase,
                                        int segments, double waveRadius, int layers, double thickness, int color) {
        Random random = new Random(phase);
        double segmentHeight = 3.0D / segments;
        double previousX = 0.0D;
        double previousY = 0.0D;
        double previousZ = 0.0D;
        for (int index = 0; index <= segments; index++) {
            double angle = randomWave
                    ? Math.PI * 2.0D * random.nextFloat() + Math.PI * 2.0D * random.nextFloat()
                    : Math.toRadians(phase + 45.0D * index);
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
        int latitudes = Math.max(1, 16 + radius / 8);
        int longitudes = latitudes * 2;
        for (int longitude = 0; longitude < longitudes; longitude++) {
            double azimuth = Math.PI * 2.0D * longitude / longitudes;
            for (int latitude = 0; latitude < latitudes; latitude++) {
                double from = Math.PI * latitude / latitudes;
                double to = Math.PI * (latitude + 1) / latitudes;
                forcefieldLine(pose, consumer, spherePoint(radius, from, azimuth), spherePoint(radius, to, azimuth), color);
            }
        }
        for (int latitude = 1; latitude < latitudes; latitude++) {
            double polar = Math.PI * latitude / latitudes;
            for (int longitude = 0; longitude < longitudes; longitude++) {
                double from = Math.PI * 2.0D * longitude / longitudes;
                double to = Math.PI * 2.0D * (longitude + 1) / longitudes;
                forcefieldLine(pose, consumer, spherePoint(radius, polar, from), spherePoint(radius, polar, to), color);
            }
        }
    }

    private static net.minecraft.world.phys.Vec3 spherePoint(int radius, double polar, double azimuth) {
        double horizontal = radius * Math.sin(polar);
        return new net.minecraft.world.phys.Vec3(horizontal * Math.sin(azimuth), radius * Math.cos(polar), horizontal * Math.cos(azimuth));
    }

    private static void forcefieldLine(PoseStack.Pose pose, VertexConsumer consumer, net.minecraft.world.phys.Vec3 from,
                                       net.minecraft.world.phys.Vec3 to, int color) {
        int red = color >>> 16 & 255;
        int green = color >>> 8 & 255;
        int blue = color & 255;
        net.minecraft.world.phys.Vec3 normal = to.subtract(from).normalize();
        consumer.addVertex(pose, (float) from.x, (float) from.y, (float) from.z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
        consumer.addVertex(pose, (float) to.x, (float) to.y, (float) to.z)
                .setColor(red, green, blue, 255)
                .setNormal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }

    static void renderItemLegacy(String id, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                 int packedLight, int packedOverlay, boolean inventory) {
        if (id.equals("machine_precass")) {
            renderItemPrecisionAssembler(state, poseStack, bufferSource, packedLight, packedOverlay, inventory);
            return;
        }
        if (id.equals("machine_turbofan")) {
            poseStack.pushPose();
            if (inventory) {
                poseStack.mulPose(yaw(90.0F));
                poseStack.scale(2.25F, 2.25F, 2.25F);
            }
            renderTurbofanParts(0.0F, false, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (id.equals("machine_turbinegas")) {
            poseStack.pushPose();
            if (inventory) {
                // RenderTurbineGas#getRenderer: the legacy inventory-only pose.
                poseStack.translate(0.0F, -1.0F, 1.5F);
                poseStack.scale(2.5F, 2.5F, 2.5F);
            }
            // RenderTurbineGas#renderCommon applies this scale and rotation
            // after the ItemRenderBase pose in every display context.
            poseStack.scale(0.75F, 0.75F, 0.75F);
            poseStack.mulPose(yaw(90.0F));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(GasTurbineBlockEntityRenderer.MODEL),
                    poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (id.equals("machine_thresher")) {
            poseStack.pushPose();
            if (inventory) {
                poseStack.translate(0.0F, 4.0F, -8.0F);
                poseStack.scale(4.5F, 4.5F, 4.5F);
            }
            poseStack.scale(0.5F, 0.5F, 0.5F);
            poseStack.mulPose(yaw(-90.0F));
            renderThresherParts(80.0D, System.currentTimeMillis() % 3600L * 0.25D, 0.0D,
                    state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (id.equals("machine_lpw2")) {
            poseStack.pushPose();
            // LPW-2 has no old IItemRenderer; fit the complete OBJ assembly
            // from its exact 1.7.10 bounds in the modern item transforms.
            float scale = inventory ? 1.15F : 0.30F;
            poseStack.scale(scale, scale, scale);
            renderLPW2Parts(System.currentTimeMillis() / 50.0D, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (id.equals("machine_forcefield")) {
            poseStack.pushPose();
            if (inventory) {
                poseStack.translate(0.0F, -4.0F, 0.0F);
                poseStack.scale(6.0F, 6.0F, 6.0F);
            }
            renderForcefieldHardware(state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.popPose();
            return;
        }
        if (id.equals("machine_radgen")) {
            renderItemRadGen(state, poseStack, bufferSource, packedLight, packedOverlay, inventory);
            return;
        }
        if (id.equals("machine_missile_assembly")) {
            renderItemMissileAssembly(state, poseStack, bufferSource, packedLight, packedOverlay, inventory);
            return;
        }
        if (id.equals("machine_orbus")) {
            renderItemOrbus(state, poseStack, bufferSource, packedLight, packedOverlay, inventory);
            return;
        }
        ModelResourceLocation model = switch (id) {
            case "machine_radiolysis" -> RADIOLYSIS;
            case "machine_reactor_breeding" -> BREEDER;
            default -> null;
        };
        if (id.equals("machine_radar") || id.equals("machine_radar_large")) {
            renderItemRadar(id, state, poseStack, bufferSource, packedLight, packedOverlay, inventory);
            return;
        }
        if (model == null && !id.equals("machine_annihilator")) {
            return;
        }
        poseStack.pushPose();
        if (id.equals("machine_annihilator")) {
            if (inventory) {
                poseStack.translate(0.0F, -3.0F, 0.0F);
                poseStack.scale(2.75F, 2.75F, 2.75F);
            }
            poseStack.scale(0.5F, 0.5F, 0.5F);
            renderAnnihilatorParts(0.0F, 0.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            if (inventory) {
                // ItemRenderLibrary uses distinct inventory poses for these
                // two complete OBJ assemblies.
                if (id.equals("machine_reactor_breeding")) {
                    poseStack.translate(0.0F, -4.5F, 0.0F);
                    poseStack.scale(4.5F, 4.5F, 4.5F);
                } else {
                    poseStack.translate(0.0F, -2.5F, 0.0F);
                    poseStack.scale(3.0F, 3.0F, 3.0F);
                }
            }
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Direct port of ItemRenderLibrary's radiation generator inventory renderer. */
    private static void renderItemRadGen(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                         int packedLight, int packedOverlay, boolean inventory) {
        poseStack.pushPose();
        if (inventory) {
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        }
        // ItemRenderLibrary applies this model-space scale to every radgen
        // item render before drawing Base, Rotor, and Light.
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.5F, 0.0F, 0.0F);
        renderRadGenParts(true, 0.0F, state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of ItemRenderLibrary's missile assembly inventory transform. */
    private static void renderItemMissileAssembly(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                                  int packedLight, int packedOverlay, boolean inventory) {
        poseStack.pushPose();
        if (inventory) {
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(10.0F, 10.0F, 10.0F);
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(MISSILE_ASSEMBLY), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of ItemRenderLibrary's Orbus inventory transform. */
    private static void renderItemOrbus(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay, boolean inventory) {
        poseStack.pushPose();
        if (inventory) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(2.0F, 2.0F, 2.0F);
        }
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(ORBUS), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of RenderRadGen, including the opaque glass redraw after the translucent pass. */
    private static void renderRadGen(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                     MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(radgenYaw(facing(state))));
        float rotorDegrees = machine.radgenOn()
                ? ((machine.getLevel() == null ? 0.0F : machine.getLevel().getGameTime() + partialTick) % 72.0F) * -5.0F
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
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(precassYaw(facing(state))));
        renderPrecisionAssemblerParts(machine, partialTick, state, poseStack, bufferSource, packedLight, packedOverlay);
        renderPrecisionAssemblerRecipeIcon(machine, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderPrecisionAssemblerParts(LegacyMachineBlockEntity machine, float partialTick, BlockState state,
                                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (machine.getLevel() != null && !machine.getLevel().getBlockState(machine.getBlockPos().above(3)).isAir()) {
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.pushPose();
        poseStack.mulPose(yaw((float) machine.precisionAssemblerRing(partialTick)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING2), poseStack, bufferSource, state, packedLight, packedOverlay);
        for (int arm = 0; arm < 4; arm++) {
            renderPrecisionAssemblerArm(machine, arm, partialTick, state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.mulPose(yaw(-90.0F));
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

    private static void renderPrecisionAssemblerRecipeIcon(LegacyMachineBlockEntity machine, PoseStack poseStack,
                                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || machine.getLevel() == null
                || minecraft.player.distanceToSqr(machine.getBlockPos().getX() + 0.5D, machine.getBlockPos().getY() + 1.0D, machine.getBlockPos().getZ() + 0.5D) >= 1225.0D) return;
        RecipeHolder<PrecisionAssemblerRecipe> holder = machine.selectedPrecisionAssemblerRecipe(machine.getLevel()).orElse(null);
        if (holder == null) return;
        ItemStack icon = holder.value().outputs().getFirst().stack().copyWithCount(1);
        poseStack.pushPose();
        poseStack.translate(0.0D, 1.0625D, 0.0D);
        poseStack.mulPose(MachineModelRenderer.xQuaternion(-90.0F));
        poseStack.mulPose(new Quaternionf(new AxisAngle4f((float) Math.toRadians(-90.0F), 0.0F, 0.0F, 1.0F)));
        poseStack.scale(1.25F, 1.25F, 1.25F);
        minecraft.getItemRenderer().renderStatic(icon, ItemDisplayContext.FIXED, packedLight, packedOverlay, poseStack, bufferSource, null, 0);
        poseStack.popPose();
    }

    private static void renderItemPrecisionAssembler(BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                                     int packedLight, int packedOverlay, boolean inventory) {
        poseStack.pushPose();
        if (inventory) {
            poseStack.translate(0.0F, -2.75F, 0.0F);
            poseStack.scale(4.5F, 4.5F, 4.5F);
        }
        poseStack.mulPose(yaw(90.0F));
        poseStack.scale(0.75F, 0.75F, 0.75F);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_FRAME), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PRECASS_RING2), poseStack, bufferSource, state, packedLight, packedOverlay);
        for (int arm = 0; arm < 4; arm++) {
            renderItemPrecisionAssemblerArm(state, poseStack, bufferSource, packedLight, packedOverlay);
            poseStack.mulPose(yaw(90.0F));
        }
        poseStack.popPose();
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

    private static void renderLegacyObj(LegacyMachineBlockEntity machine, ModelResourceLocation model, float degrees,
                                        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(degrees));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(model), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of RenderRadar: base is fixed, dish turns around the old negative-Y axis. */
    private static void renderRadar(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                    MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(180.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw(-machine.radarRotation(partialTick)));
        poseStack.translate(-0.125D, 0.0D, 0.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_DISH), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct port of RenderRadarLarge: only Dish rotates; Radar remains fixed. */
    private static void renderRadarLarge(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                         MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(180.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw(-machine.radarRotation(partialTick)));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_DISH), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderItemRadar(String id, BlockState state, PoseStack poseStack, MultiBufferSource bufferSource,
                                        int packedLight, int packedOverlay, boolean inventory) {
        poseStack.pushPose();
        if (id.equals("machine_radar_large")) {
            if (inventory) {
                poseStack.translate(0.0F, -5.0F, 0.0F);
                poseStack.scale(3.0F, 3.0F, 3.0F);
            }
            poseStack.mulPose(yaw(180.0F));
            poseStack.scale(0.5F, 0.5F, 0.5F);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.mulPose(yaw(-(System.currentTimeMillis() % 3600L) * 0.1F));
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_LARGE_DISH), poseStack, bufferSource, state, packedLight, packedOverlay);
        } else {
            if (inventory) {
                poseStack.translate(0.0F, -4.0F, 0.0F);
                poseStack.scale(5.0F, 5.0F, 5.0F);
            }
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
            poseStack.mulPose(yaw(-(System.currentTimeMillis() % 3600L) * 0.1F));
            poseStack.translate(-0.125D, 0.0D, 0.0D);
            MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RADAR_DISH), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(missileAssemblyYaw(facing(state))));
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(turbofanYaw(facing(state))));
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(annihilatorYaw(facing(state))));
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(pyroYaw(facing(state))));
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
        poseStack.mulPose(yaw(animation * 45.0F % 360.0F));
        poseStack.translate(-1.5D, 0.0D, -1.5D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(PYRO_FAN), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderAutosaw(LegacyMachineBlockEntity machine, float partialTick, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        BlockState state = machine.getBlockState();
        double engine = machine.autosawOn() && machine.getLevel() != null
                ? Math.sin(machine.getLevel().getGameTime() * 2.0D + partialTick)
                : 0.0D;
        poseStack.pushPose();
        poseStack.translate(0.5D, 0.0D, 0.5D);
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(pyroYaw(facing(state))));
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
        poseStack.translate(0.5D, 0.0D, 0.5D);
        poseStack.mulPose(yaw(180.0F));
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RTG_GEN), poseStack, bufferSource, state, packedLight, packedOverlay);
        if (machine.getLevel() != null) {
            for (Direction direction : Direction.values()) {
                if (!direction.getAxis().isHorizontal()
                        || !PowerNetworkManager.canCableConnectTo(machine.getLevel(), machine.getBlockPos(), direction)) {
                    continue;
                }
                poseStack.pushPose();
                poseStack.mulPose(yaw(rtgConnectorYaw(direction)));
                MachineModelRenderer.renderUnculled(MachineModelRenderer.model(RTG_CONNECTOR), poseStack, bufferSource, state, packedLight, packedOverlay);
                poseStack.popPose();
            }
        }
        poseStack.popPose();
    }

    private static void renderAutosawParts(double turn, double angle, double spin, double engine, BlockState state,
                                           PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_BASE), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.mulPose(yaw((float) -turn));
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
        poseStack.mulPose(yaw((float) -spin));
        poseStack.translate(0.0D, -1.75D, 10.0D);
        MachineModelRenderer.renderUnculled(MachineModelRenderer.model(AUTOSAW_BLADE), poseStack, bufferSource, state, packedLight, packedOverlay);
    }

    private static Direction facing(BlockState state) {
        return state.hasProperty(LargeMachineBlock.FACING) ? state.getValue(LargeMachineBlock.FACING) : Direction.NORTH;
    }

    private static Quaternionf yaw(float degrees) {
        return new Quaternionf(new AxisAngle4f((float) Math.toRadians(degrees), 0.0F, 1.0F, 0.0F));
    }

    private static float pyroYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case WEST -> 270.0F;
            case EAST -> 90.0F;
            default -> 0.0F;
        };
    }

    private static float annihilatorYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH -> 270.0F;
            case EAST -> 0.0F;
            default -> 90.0F;
        };
    }

    private static float precassYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH -> 270.0F;
            case EAST -> 0.0F;
            default -> 90.0F;
        };
    }

    private static float missileAssemblyYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case WEST -> 270.0F;
            case SOUTH -> 0.0F;
            case EAST -> 90.0F;
            default -> 180.0F;
        };
    }

    private static float radgenYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH -> 270.0F;
            case EAST -> 0.0F;
            default -> 90.0F;
        };
    }

    private static float radiolysisYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case WEST -> 90.0F;
            case SOUTH -> 0.0F;
            case EAST -> 270.0F;
            default -> 180.0F;
        };
    }

    private static float turbofanYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case WEST -> 180.0F;
            case SOUTH -> 270.0F;
            case EAST -> 0.0F;
            default -> 90.0F;
        };
    }

    /** RenderThresher's metadata table expressed in modern FACING values. */
    private static float thresherYaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 180.0F;
            case SOUTH -> 0.0F;
            case EAST -> 90.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
    }

    /** RenderLPW2's metadata-minus-offset table expressed in modern FACING values. */
    private static float lpw2Yaw(Direction facing) {
        return switch (facing) {
            case NORTH -> 90.0F;
            case SOUTH -> 270.0F;
            case EAST -> 0.0F;
            case WEST -> 180.0F;
            default -> 0.0F;
        };
    }

    private static float rtgConnectorYaw(Direction direction) {
        return switch (direction) {
            case WEST -> 180.0F;
            case NORTH -> 90.0F;
            case SOUTH -> -90.0F;
            default -> 0.0F;
        };
    }
}
