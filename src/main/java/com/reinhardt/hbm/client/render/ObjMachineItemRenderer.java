package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.DecoCrtBlockItem;
import com.reinhardt.hbm.item.FilingCabinetBlockItem;
import com.reinhardt.hbm.item.VendingMachineBlockItem;
import com.reinhardt.hbm.item.CrashedBombBlockItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.event.ModelEvent;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Renders the actual OBJ geometry for machine items and centers it from measured model bounds. */
public final class ObjMachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final RandomSource BOUNDS_RANDOM = RandomSource.create();
    private static final List<String> BATTERY_PACK_VARIANTS = List.of(
            "battery_redstone", "battery_lead", "battery_lithium", "battery_sodium",
            "battery_schrabidium", "battery_quantum", "capacitor_copper", "capacitor_gold",
            "capacitor_niobium", "capacitor_tantalum", "capacitor_bismuth", "capacitor_spark"
    );
    private static final Map<String, Profile> PROFILES = createProfiles();
    private static final Map<String, LegacyPose> LEGACY_POSES = createLegacyPoses();
    private static final Map<String, Fit> FIT_CACHE = new LinkedHashMap<>();
    private static final Set<String> DIAGNOSTICS = ConcurrentHashMap.newKeySet();

    public ObjMachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static Set<String> itemIds() {
        return PROFILES.keySet();
    }

    /** Inventory model keys are registry item keys, not CustomModelData variant names. */
    public static Set<String> inventoryModelIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        PROFILES.keySet().stream()
                .filter(id -> !id.startsWith("battery_pack_")
                        && !id.startsWith("deco_crt_")
                        && !id.startsWith("filing_cabinet_")
                        && !id.startsWith("vending_machine_")
                        && !id.startsWith("crashed_bomb_"))
                .forEach(ids::add);
        ids.add("battery_pack");
        ids.add("deco_crt");
        ids.add("filing_cabinet");
        ids.add("crashed_bomb");
        return Set.copyOf(ids);
    }

    public static Set<String> missingInventoryModels(Map<ModelResourceLocation, BakedModel> models) {
        return inventoryModelIds().stream()
                .filter(id -> models.get(new ModelResourceLocation(
                        ReinhardtsHBM.id(id), ModelResourceLocation.INVENTORY_VARIANT)) == null)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    static void registerAdditionalModels(ModelEvent.RegisterAdditional event) {
        PROFILES.values().stream()
                .flatMap(profile -> profile.models().stream())
                .distinct()
                .forEach(event::register);
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        String profileId = stack.getItem() instanceof FilingCabinetBlockItem cabinet
                ? "filing_cabinet_" + (cabinet.variant(stack) == 1 ? "steel" : "green")
                : stack.getItem() instanceof CrashedBombBlockItem dud
                ? "crashed_bomb_" + dud.variant(stack)
                : stack.getItem() instanceof BatteryPackItem
                ? "battery_pack_" + BatteryPackItem.variantId(stack)
                : stack.getItem() instanceof DecoCrtBlockItem crt
                ? "deco_crt_" + crt.variantId(stack)
                : stack.getItem() instanceof VendingMachineBlockItem vending
                ? "vending_machine_" + (vending.variantIndex(stack) == 1 ? "snacks" : "soda")
                : id;
        Profile profile = PROFILES.get(profileId);
        if (profile == null) {
            return;
        }

        BlockState state = stack.getItem() instanceof BlockItem blockItem
                ? blockItem.getBlock().defaultBlockState()
                : Blocks.IRON_BLOCK.defaultBlockState();
        List<BakedModel> models = profile.models().stream()
                .map(MachineModelRenderer::model)
                .toList();
        logGeometryOnce(id, profileId, profile, models, state);

        if (profileId.equals("fan")) {
            renderFanItem(models, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("floodlight")) {
            renderFloodlightItem(models, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("cargo_elevator")) {
            renderCargoElevatorItem(models, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("bomb_multi")) {
            renderBombMultiItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.startsWith("crashed_bomb_")) {
            renderCrashedBombItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay, profileId);
            return;
        }

        LegacyPose legacyPose = LEGACY_POSES.get(profileId);
        if (legacyPose != null) {
            renderLegacyPose(models, state, context, poseStack, bufferSource, packedLight, packedOverlay, legacyPose);
            return;
        }
        Fit fit = FIT_CACHE.computeIfAbsent(profileId, ignored -> Fit.measure(models, state));

        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        if (profile.pitch() != 0.0F) {
            poseStack.mulPose(Axis.XP.rotationDegrees(profile.pitch()));
        }
        if (profile.yaw() != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(profile.yaw()));
        }
        float target = context == ItemDisplayContext.GUI ? profile.guiTarget() : profile.otherTarget();
        float scale = target / fit.longestSide();
        poseStack.scale(scale, scale, scale);
        poseStack.translate(-fit.centerX(), -fit.centerY(), -fit.centerZ());
        poseStack.translate(profile.offsetX(), profile.offsetY(), profile.offsetZ());
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderFanItem(List<BakedModel> models, ItemDisplayContext context, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource,
                    Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderFloodlightItem(List<BakedModel> models, ItemDisplayContext context, PoseStack poseStack,
                                             MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.5F, 0.0F);
            poseStack.scale(6.5F, 6.5F, 6.5F);
        }
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-30.0F));
        poseStack.translate(0.0F, -0.5F, 0.0F);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(models.get(2), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void renderCargoElevatorItem(List<BakedModel> models, ItemDisplayContext context, PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.75F, 0.0F);
            poseStack.scale(3.25F, 3.25F, 3.25F);
        }
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(models.get(2), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.0F, 0.0F);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(models.get(2), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.0F, 0.0F);
        MachineModelRenderer.renderUnculled(models.get(2), poseStack, bufferSource,
                Blocks.IRON_BLOCK.defaultBlockState(), packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct ItemRenderLibrary transform for BombMulti; it is not a generic bounds-fitted machine. */
    private static void renderBombMultiItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                            int packedOverlay) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        poseStack.translate(0.75F, 0.0F, 0.0F);
        poseStack.scale(3.0F, 3.0F, 3.0F);
        poseStack.translate(0.0F, 0.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(180.0F));
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Direct ItemRenderBase transform from RenderCrashedBomb, including each dud's authored offset. */
    private static void renderCrashedBombItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                              int packedOverlay, String profileId) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, 3.0F, 0.0F);
            poseStack.scale(2.125F, 2.125F, 2.125F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        float offset = switch (profileId) {
            case "crashed_bomb_conventional" -> -0.5F;
            case "crashed_bomb_nuke" -> 1.25F;
            case "crashed_bomb_salted" -> 0.5F;
            default -> 0.0F;
        };
        poseStack.translate(0.0F, 0.0F, offset);
        MachineModelRenderer.renderUnculled(models.getFirst(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /**
     * Direct translation of the individual 1.7.10 ItemRenderBase transforms.
     * These machines deliberately do not use the generic bounds fit: their OBJ
     * files use different authoring origins and the legacy item renderers gave
     * each one a distinct inventory pose.
     */
    private static void renderLegacyPose(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                         PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                         int packedOverlay, LegacyPose legacyPose) {
        poseStack.pushPose();
        LegacyMachineItemRenderer.applyItemRenderBasePose(context, poseStack);
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(legacyPose.inventoryX(), legacyPose.inventoryY(), legacyPose.inventoryZ());
            poseStack.scale(legacyPose.inventoryScale(), legacyPose.inventoryScale(), legacyPose.inventoryScale());
        }
        poseStack.translate(legacyPose.commonX(), legacyPose.commonY(), legacyPose.commonZ());
        if (legacyPose.commonYaw() != 0.0F) {
            poseStack.mulPose(Axis.YP.rotationDegrees(legacyPose.commonYaw()));
        }
        if (legacyPose.commonScale() != 1.0F) {
            poseStack.scale(legacyPose.commonScale(), legacyPose.commonScale(), legacyPose.commonScale());
        }
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void logGeometryOnce(String itemId, String profileId, Profile profile,
                                        List<BakedModel> models, BlockState state) {
        if (!DIAGNOSTICS.add(profileId)) {
            return;
        }
        int[] quadCounts = new int[models.size()];
        int total = 0;
        for (int index = 0; index < models.size(); index++) {
            BakedModel model = models.get(index);
            int count = model.getQuads(state, null, BOUNDS_RANDOM).size();
            for (Direction side : Direction.values()) {
                count += model.getQuads(state, side, BOUNDS_RANDOM).size();
            }
            quadCounts[index] = count;
            total += count;
        }
        if (total == 0) {
            ReinhardtsHBM.LOGGER.error("OBJ item renderer resolved no geometry: item={}, profile={}, models={}, quads={}",
                    itemId, profileId, profile.models(), java.util.Arrays.toString(quadCounts));
        } else {
            ReinhardtsHBM.LOGGER.info("OBJ item renderer active: item={}, profile={}, models={}, quads={}",
                    itemId, profileId, profile.models(), java.util.Arrays.toString(quadCounts));
        }
    }

    private static Map<String, Profile> createProfiles() {
        Map<String, Profile> profiles = new LinkedHashMap<>();

        // These are static item assemblies. Every path below points at a real
        // baked OBJ model; empty blockstates used by dummyable machines are
        // deliberately excluded.
        add(profiles, "machine_arc_furnace", 0.0F, 0.92F,
                "block/machine_arc_furnace", "block/machine_arc_furnace_lid",
                "block/machine_arc_furnace_ring1", "block/machine_arc_furnace_ring2",
                "block/machine_arc_furnace_ring3", "block/machine_arc_furnace_electrode1",
                "block/machine_arc_furnace_electrode2", "block/machine_arc_furnace_electrode3",
                "block/machine_arc_furnace_cable1", "block/machine_arc_furnace_cable2",
                "block/machine_arc_furnace_cable3");
        // This animated 23x13 door has no compact block mesh.  Render the real
        // closed OBJ assembly in item space and fit it from its measured bounds.
        add(profiles, "transition_seal", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/transition_seal");
        add(profiles, "boltgun", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/boltgun");
        add(profiles, "icf", 0.0F, 0.90F, "block/icf");
        add(profiles, "bomb_multi", 0.0F, 0.90F, "block/bomb_multi_world");
        add(profiles, "nuke_gadget", 0.0F, 0.90F, "block/nuke_gadget_world");
        add(profiles, "nuke_man", 180.0F, 0.90F, "block/nuke_man_world");
        add(profiles, "nuke_mike", 0.0F, 0.90F, "block/nuke_mike_world");
        add(profiles, "nuke_tsar", 0.0F, 0.90F, "block/nuke_tsar_world");
        add(profiles, "nuke_fleija", 90.0F, 0.90F, "block/nuke_fleija_world");
        add(profiles, "nuke_prototype", 90.0F, 0.90F, "block/nuke_prototype_world");
        add(profiles, "nuke_solinium", 90.0F, 0.90F, "block/nuke_solinium_world");
        add(profiles, "nuke_n2", 0.0F, 0.90F, "block/nuke_n2_world");
        add(profiles, "nuke_custom", 0.0F, 0.90F, "block/nuke_custom_world");
        add(profiles, "nuke_fstbmb", 0.0F, 0.90F, "block/nuke_fstbmb_world");
        add(profiles, "crashed_bomb_balefire", 0.0F, 0.90F, "block/crashed_bomb_balefire");
        add(profiles, "crashed_bomb_conventional", 0.0F, 0.90F, "block/crashed_bomb_conventional");
        add(profiles, "crashed_bomb_nuke", 0.0F, 0.90F, "block/crashed_bomb_nuke");
        add(profiles, "crashed_bomb_salted", 0.0F, 0.90F, "block/crashed_bomb_salted");
        // These inventory entries are builtin/entity markers.  The actual
        // OBJ geometry must therefore come from the independent block model.
        add(profiles, "machine_ammo_press", 90.0F, 0.92F, "block/machine_ammo_press");
        add(profiles, "machine_ashpit", 0.0F, 0.92F,
                "block/machine_ashpit_world", "block/machine_ashpit_door");
        add(profiles, "machine_compressor", 0.0F, 0.92F,
                "block/machine_compressor_world", "block/machine_compressor_pump", "block/machine_compressor_fan");
        add(profiles, "machine_compressor_compact", 0.0F, 0.92F,
                "block/machine_compressor_compact_world", "block/machine_compressor_compact_fan1",
                "block/machine_compressor_compact_fan2");
        add(profiles, "machine_mixer", 180.0F, 0.92F,
                "block/machine_mixer_world", "block/machine_mixer_blade");
        add(profiles, "machine_electrolyser", 0.0F, 0.92F, "block/machine_electrolyser_world");
        add(profiles, "machine_arc_welder", 0.0F, 0.90F, "block/machine_arc_welder_world");
        add(profiles, "machine_soldering_station", 0.0F, 0.80F,
                "block/machine_soldering_station_world");
        add(profiles, "machine_assembly_machine", 90.0F, 0.61F,
                "block/machine_assembly_machine");
        add(profiles, "machine_funnel", 0.0F, 1.12F, "block/machine_funnel");
        add(profiles, "machine_cyclotron", 0.0F, 0.90F,
                "block/machine_cyclotron_body", "block/machine_cyclotron_b1",
                "block/machine_cyclotron_b2", "block/machine_cyclotron_b3", "block/machine_cyclotron_b4");
        add(profiles, "machine_exposure_chamber", 90.0F, 0.88F,
                "block/machine_exposure_chamber_chamber", "block/machine_exposure_chamber_magnets",
                "block/machine_exposure_chamber_core");
        add(profiles, "machine_deuterium_tower", 180.0F, 0.90F, "block/machine_deuterium_tower_world");
        add(profiles, "radar_screen", 0.0F, 0.90F, "block/radar_screen_world");
        add(profiles, "machine_radar", 0.0F, 0.90F,
                "block/machine_radar_base", "block/machine_radar_dish");
        add(profiles, "machine_radar_large", 180.0F, 0.90F,
                "block/machine_radar_large_base", "block/machine_radar_large_dish");
        add(profiles, "machine_radgen", 0.0F, 0.82F,
                "block/machine_radgen_base", "block/machine_radgen_rotor",
                "block/machine_radgen_light", "block/machine_radgen_glass");
        add(profiles, "pa_source", 90.0F, 0.90F, "block/pa_source");
        add(profiles, "pa_beamline", 90.0F, 0.90F, "block/pa_beamline_body");
        add(profiles, "pa_rfc", 90.0F, 0.90F, "block/pa_rfc");
        add(profiles, "pa_quadrupole", 90.0F, 0.90F, "block/pa_quadrupole");
        add(profiles, "pa_dipole", 0.0F, 0.90F, "block/pa_dipole");
        add(profiles, "pa_detector", 90.0F, 0.90F, "block/pa_detector");
        add(profiles, "machine_combustion_engine", 90.0F, 0.54F, "block/machine_combustion_engine");
        add(profiles, "machine_battery_socket", 0.0F, 0.86F,
                "block/machine_battery_socket_socket");
        add(profiles, "capacitor_copper", 225.0F, 30.0F, 0.86F, 0.90F,
                0.0F, 0.0F, 0.0F, "block/capacitor_copper");
        add(profiles, "charger", 0.0F, 0.86F,
                "block/charger_base", "block/charger_slide");
        add(profiles, "machine_battery_redd", -90.0F, 0.90F,
                "block/machine_battery_redd_base", "block/machine_battery_redd_wheel",
                "block/machine_battery_redd_lights");
        add(profiles, "machine_excavator", 90.0F, 0.90F,
                "block/machine_excavator_main", "block/machine_excavator_crusher1",
                "block/machine_excavator_crusher2", "block/machine_excavator_drillbit", "block/machine_excavator_shaft");
        add(profiles, "machine_blast_furnace", 0.0F, 0.90F, "block/machine_blast_furnace");
        add(profiles, "machine_rtg_grey", 0.0F, 0.90F, "block/machine_rtg_grey_world");
        add(profiles, "machine_crystallizer", 0.0F, 0.90F,
                "block/machine_crystallizer_body", "block/machine_crystallizer_spinner");
        add(profiles, "machine_ore_slopper", 0.0F, 0.90F,
                "block/machine_ore_slopper_base", "block/machine_ore_slopper_slider",
                "block/machine_ore_slopper_hydraulics", "block/machine_ore_slopper_bucket",
                "block/machine_ore_slopper_blades_left", "block/machine_ore_slopper_blades_right",
                "block/machine_ore_slopper_fan");
        add(profiles, "machine_silex", 90.0F, 0.90F, "block/machine_silex");
        add(profiles, "machine_fel", 90.0F, 0.90F, "block/machine_fel");
        add(profiles, "machine_rotary_furnace", 90.0F, 0.90F, "block/machine_rotary_furnace");
        add(profiles, "tesla", 180.0F, 0.90F, "block/tesla");
        add(profiles, "fan", 0.0F, 0.90F, "block/fan_frame", "block/fan_blades");
        add(profiles, "floodlight", 0.0F, 0.90F,
                "block/floodlight_base", "block/floodlight_lights", "block/floodlight_lamps");
        addThermalObj(profiles, "spotlight_incandescent", "block/spotlight_incandescent_world");
        addThermalObj(profiles, "spotlight_incandescent_off", "block/spotlight_incandescent_off_world");
        addThermalObj(profiles, "spotlight_fluoro", "block/spotlight_fluoro_single_world");
        addThermalObj(profiles, "spotlight_fluoro_off", "block/spotlight_fluoro_single_off_world");
        addThermalObj(profiles, "spotlight_halogen", "block/spotlight_halogen_world");
        addThermalObj(profiles, "spotlight_halogen_off", "block/spotlight_halogen_off_world");
        add(profiles, "cargo_elevator", 0.0F, 0.82F,
                "block/cargo_elevator_base", "block/cargo_elevator_piston",
                "block/cargo_elevator_guides", "block/cargo_elevator_platform");
        add(profiles, "sat_dock", 0.0F, 0.90F, "block/sat_dock");
        add(profiles, "lamp_demon", 0.0F, 0.90F, "block/lamp_demon");
        add(profiles, "refueler", 0.0F, 0.90F, "block/refueler_body");
        add(profiles, "radio_autocal", 0.0F, 0.90F, "block/radio_autocal");
        add(profiles, "radio_torch_sender", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/radio_torch_sender");
        add(profiles, "radio_torch_receiver", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/radio_torch_receiver");
        add(profiles, "radio_torch_counter", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/radio_torch_counter");
        add(profiles, "radio_torch_logic", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/radio_torch_logic");
        add(profiles, "radio_torch_reader", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/radio_torch_reader");
        add(profiles, "radio_torch_controller", 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/radio_torch_controller");
        add(profiles, "vending_machine_soda", 0.0F, 0.90F, "block/vending_machine_soda");
        add(profiles, "vending_machine_snacks", 0.0F, 0.90F, "block/vending_machine_snacks");
        addThermalObj(profiles, "tape_recorder", "block/tape_recorder");
        add(profiles, "skeleton_holder", 90.0F, 0.88F, "block/skeleton_holder_world");
        add(profiles, "filing_cabinet_green", 180.0F, 30.0F, 0.88F, 0.92F,
                0.0F, -0.08F, 0.0F, "block/filing_cabinet_green_item");
        add(profiles, "filing_cabinet_steel", 180.0F, 30.0F, 0.88F, 0.92F,
                0.0F, -0.08F, 0.0F, "block/filing_cabinet_steel_item");

        // Thermal equipment uses world-scale OBJ files. Inventory rendering
        // must fit the baked geometry, rather than inherit world transforms.
        addThermalObj(profiles, "chimney_brick", "block/chimney_brick");
        addThermalObj(profiles, "chimney_industrial", "block/chimney_industrial");
        addThermalObj(profiles, "heater_firebox", "block/heater_firebox");
        addThermalObj(profiles, "heater_oven", "block/heater_oven");
        addThermalObj(profiles, "heater_oilburner", "block/heater_oilburner");
        addThermalObj(profiles, "heater_electric", "block/heater_electric");
        addThermalObj(profiles, "heater_heatex", "block/heater_heatex");
        addThermalObj(profiles, "heat_boiler", "block/heat_boiler_world");
        add(profiles, "machine_industrial_boiler", 315.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, "block/machine_industrial_boiler_world");
        addThermalObj(profiles, "machine_solar_boiler", "block/machine_solar_boiler_world");
        add(profiles, "pipe_anchor", 0.0F, 0.90F, "block/pipe_anchor");
        add(profiles, "piston_inserter", 0.0F, 0.90F, "block/piston_inserter");
        addThermalObj(profiles, "solar_mirror", "block/solar_mirror_item");
        addThermalObj(profiles, "machine_condenser", "block/machine_condenser");
        addThermalObj(profiles, "machine_condenser_powered", "block/machine_condenser_powered");
        addThermalObj(profiles, "machine_tower_small", "block/machine_tower_small_world");
        addThermalObj(profiles, "machine_tower_large", "block/machine_tower_large_world");
        addThermalObj(profiles, "machine_blast_furnace", "block/machine_blast_furnace");
        addThermalObj(profiles, "furnace_combination", "block/furnace_combination");
        addThermalObj(profiles, "furnace_iron", "block/furnace_iron");
        addThermalObj(profiles, "furnace_steel", "block/furnace_steel");
        // RenderBarrel is a real inventory block renderer in 1.7.10, rather
        // than a flat item sprite. Keep its five barrel variants on the same
        // isometric inventory pose while retaining their original OBJ assets.
        addInventoryBarrel(profiles, "barrel_plastic", "block/barrel_plastic");
        addInventoryBarrel(profiles, "barrel_corroded", "block/barrel_corroded");
        addInventoryBarrel(profiles, "barrel_steel", "block/barrel_steel");
        addInventoryBarrel(profiles, "barrel_tcalloy", "block/barrel_tcalloy");
        addInventoryBarrel(profiles, "barrel_antimatter", "block/barrel_antimatter");

        // ItemRenderBatteryPack in 1.7.10 renders these exact BatterySocket
        // OBJ groups rather than a generated item icon.  Unlike the original
        // screen-space renderer, their modern pose is centered from measured
        // geometry so every variant remains inside its inventory slot.
        for (String variant : BATTERY_PACK_VARIANTS) {
            addBatteryVariant(profiles, variant);
        }

        add(profiles, "machine_well", "block/machine_well", 0.0F, 0.90F);
        add(profiles, "machine_pumpjack", "block/machine_pumpjack", 0.0F, 0.90F);
        add(profiles, "machine_fracking_tower", "block/machine_fracking_tower", 0.0F, 0.90F);
        add(profiles, "machine_refinery", "block/machine_refinery", 0.0F, 0.90F);
        add(profiles, "machine_vacuum_distill", "block/machine_vacuum_distill", 0.0F, 0.90F);
        add(profiles, "machine_coker", "block/machine_coker", 0.0F, 0.90F);
        add(profiles, "machine_flare", "block/machine_flare", 0.0F, 0.90F);
        add(profiles, "machine_solidifier", "block/machine_solidifier", 0.0F, 0.90F);
        add(profiles, "machine_liquefactor", "block/machine_liquefactor", 0.0F, 0.90F);
        add(profiles, "machine_fraction_tower", "block/machine_fraction_tower", 0.0F, 0.90F);
        add(profiles, "fraction_spacer", "block/fraction_spacer", 0.0F, 0.90F);
        add(profiles, "machine_catalytic_cracker", "block/machine_catalytic_cracker", 0.0F, 0.90F);
        add(profiles, "machine_catalytic_reformer", "block/machine_catalytic_reformer", 0.0F, 0.90F);
        add(profiles, "machine_hydrotreater", "block/machine_hydrotreater", 0.0F, 0.90F);

        add(profiles, "deco_computer", 0.0F, 0.90F, "block/deco_computer");
        add(profiles, "boat", 0.0F, 0.92F, "block/boat");
        add(profiles, "deco_crt_clean", 0.0F, 0.90F, "block/deco_crt_clean");
        add(profiles, "deco_crt_broken", 0.0F, 0.90F, "block/deco_crt_broken");
        add(profiles, "deco_crt_blinking", 0.0F, 0.90F, "block/deco_crt_blinking");
        add(profiles, "deco_crt_bsod", 0.0F, 0.90F, "block/deco_crt_bsod");

        return Map.copyOf(profiles);
    }

    private static Map<String, LegacyPose> createLegacyPoses() {
        Map<String, LegacyPose> poses = new LinkedHashMap<>();

        // RenderArcFurnace#getRenderer
        legacy(poses, "machine_arc_furnace", 0.0F, -3.0F, 0.0F, 3.5F,
                0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
        // RenderICF#getRenderer
        legacy(poses, "icf", 0.0F, -1.5F, 0.0F, 2.125F,
                0.0F, 0.0F, 0.0F, 90.0F, 0.5F);
        // RenderCompressor#getRenderer and RenderCompressorCompact#getRenderer
        legacy(poses, "machine_compressor", 0.0F, -4.0F, 0.0F, 3.0F,
                0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
        legacy(poses, "machine_compressor_compact", -1.0F, -1.0F, 0.0F, 2.75F,
                0.5F, 0.0F, 0.0F, 0.0F, 0.75F);
        // RenderMixer#getRenderer
        legacy(poses, "machine_mixer", 0.0F, -5.0F, 0.0F, 5.0F,
                0.0F, 0.0F, 0.0F, 180.0F, 1.0F);
        // RenderElectrolyser#getRenderer
        legacy(poses, "machine_electrolyser", -1.0F, -1.0F, 0.0F, 2.5F,
                0.0F, 0.0F, 0.0F, 0.0F, 0.5F);
        // RenderSolderingStation#getRenderer and RenderAssemblyMachine#getRenderer
        legacy(poses, "machine_soldering_station", 0.0F, -1.0F, 0.0F, 5.0F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        legacy(poses, "machine_assembly_machine", 0.0F, -2.75F, 0.0F, 4.5F,
                0.0F, 0.0F, 0.0F, 90.0F, 0.75F);
        // RenderAshpit#getRenderer
        legacy(poses, "machine_ashpit", 0.0F, -1.0F, 0.0F, 3.25F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        // ItemRenderLibrary#machine_cyclotron
        legacy(poses, "machine_cyclotron", 0.0F, 0.0F, 0.0F, 2.25F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        // RenderExposureChamber#getRenderer
        legacy(poses, "machine_exposure_chamber", 0.0F, -1.5F, 0.0F, 3.0F,
                1.5F, 0.0F, 0.0F, 90.0F, 0.5F);
        // RenderPASource/Beamline/RFC/Quadrupole/Dipole/Detector#getRenderer
        legacy(poses, "pa_source", 0.0F, -1.0F, 0.0F, 4.0F,
                0.0F, 0.0F, 0.0F, 90.0F, 0.5F);
        legacy(poses, "pa_beamline", 0.0F, 0.0F, 0.0F, 4.0F,
                0.0F, 0.0F, 0.0F, 90.0F, 1.0F);
        legacy(poses, "pa_rfc", 0.0F, -1.0F, 0.0F, 4.0F,
                0.0F, 0.0F, 0.0F, 90.0F, 0.5F);
        legacy(poses, "pa_quadrupole", 0.0F, -3.5F, 0.0F, 4.0F,
                0.0F, 0.0F, 0.0F, 90.0F, 1.0F);
        legacy(poses, "pa_dipole", 0.0F, -3.0F, 0.0F, 3.5F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        legacy(poses, "pa_detector", 0.0F, -1.0F, 0.0F, 3.0F,
                0.0F, 0.0F, 0.0F, 90.0F, 0.5F);
        // RenderBatterySocket/Excavator/OreSlopper/CombustionEngine#getRenderer
        legacy(poses, "machine_battery_socket", 0.0F, -2.0F, 0.0F, 5.0F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        legacy(poses, "machine_excavator", 0.0F, -2.0F, 0.0F, 3.0F,
                0.0F, 0.0F, 0.0F, 90.0F, 0.5F);
        legacy(poses, "machine_ore_slopper", 0.0F, -3.0F, 0.0F, 3.75F,
                0.0F, 0.0F, 0.0F, -90.0F, 0.5F);
        // ItemRenderLibrary#machine_silex and #machine_fel
        legacy(poses, "machine_silex", 0.0F, -2.5F, 0.0F, 3.25F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        legacy(poses, "machine_fel", 0.0F, -1.0F, 0.0F, 2.0F,
                1.0F, 0.0F, 0.0F, -90.0F, 1.0F);
        // RenderCrystallizer#getRenderer
        legacy(poses, "machine_crystallizer", 0.0F, -4.0F, 0.0F, 2.0F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        legacy(poses, "machine_combustion_engine", 0.0F, -1.0F, 0.0F, 2.75F,
                0.0F, 0.0F, 2.75F, 90.0F, 1.0F);
        // RenderRotaryFurnace#getRenderer
        legacy(poses, "machine_rotary_furnace", 0.0F, -2.0F, 0.0F, 3.5F,
                0.0F, 0.0F, 0.0F, 90.0F, 0.625F);
        // ItemRenderLibrary#charger: translate(0,-7,0), scale(10), then the
        // common renderer scales the OBJ assembly by 2 and shifts it by .5 X.
        legacy(poses, "charger", 0.0F, -7.0F, 0.0F, 10.0F,
                0.5F, 0.0F, 0.0F, 0.0F, 2.0F);
        legacy(poses, "pipe_anchor", 0.0F, -3.5F, 0.0F, 10.0F,
                0.0F, 0.0F, 0.0F, 0.0F, 1.0F);
        legacy(poses, "piston_inserter", 0.0F, -2.5F, 0.0F, 5.0F,
                0.0F, 0.0F, 0.0F, 0.0F, 2.0F);

        return Map.copyOf(poses);
    }

    private static void legacy(Map<String, LegacyPose> poses, String id, float inventoryX, float inventoryY,
                               float inventoryZ, float inventoryScale, float commonX, float commonY,
                               float commonZ, float commonYaw, float commonScale) {
        poses.put(id, new LegacyPose(inventoryX, inventoryY, inventoryZ, inventoryScale,
                commonX, commonY, commonZ, commonYaw, commonScale));
    }

    private static void add(Map<String, Profile> profiles, String id, String modelPath, float yaw, float guiTarget) {
        add(profiles, id, yaw, guiTarget, modelPath);
    }

    private static void add(Map<String, Profile> profiles, String id, float yaw, float guiTarget, String... modelPaths) {
        add(profiles, id, yaw, guiTarget, 0.0F, 0.0F, 0.0F, modelPaths);
    }

    private static void add(Map<String, Profile> profiles, String id, float yaw, float guiTarget,
                            float offsetX, float offsetY, float offsetZ, String... modelPaths) {
        add(profiles, id, yaw, 0.0F, guiTarget, 0.85F, offsetX, offsetY, offsetZ, modelPaths);
    }

    private static void addInventoryBarrel(Map<String, Profile> profiles, String id, String modelPath) {
        add(profiles, id, 225.0F, 30.0F, 0.78F, 0.82F,
                0.0F, 0.0F, 0.0F, modelPath);
    }

    private static void addThermalObj(Map<String, Profile> profiles, String id, String modelPath) {
        add(profiles, id, 225.0F, 30.0F, 0.82F, 0.85F,
                0.0F, 0.0F, 0.0F, modelPath);
    }

    private static void add(Map<String, Profile> profiles, String id, float yaw, float pitch,
                            float guiTarget, float otherTarget, float offsetX, float offsetY,
                            float offsetZ, String... modelPaths) {
        profiles.put(id, new Profile(
                List.of(modelPaths).stream()
                        .map(path -> ModelResourceLocation.standalone(ReinhardtsHBM.id(path)))
                .toList(),
                yaw,
                pitch,
                guiTarget,
                otherTarget,
                offsetX,
                offsetY,
                offsetZ
        ));
    }

    private static void addBatteryVariant(Map<String, Profile> profiles, String variant) {
        profiles.put("battery_pack_" + variant, new Profile(
                List.of(ModelResourceLocation.standalone(
                        ReinhardtsHBM.id("block/machine_battery_socket_" + variant)
                )),
                225.0F,
                30.0F,
                0.58F,
                0.85F,
                0.0F,
                0.0F,
                0.0F
        ));
    }

    private record Profile(List<ModelResourceLocation> models, float yaw, float pitch, float guiTarget, float otherTarget,
                           float offsetX, float offsetY, float offsetZ) {
    }

    private record LegacyPose(float inventoryX, float inventoryY, float inventoryZ, float inventoryScale,
                              float commonX, float commonY, float commonZ, float commonYaw, float commonScale) {
    }

    private record Fit(float centerX, float centerY, float centerZ, float longestSide) {
        private static Fit measure(List<BakedModel> models, BlockState state) {
            Bounds bounds = new Bounds();
            for (BakedModel model : models) {
                add(bounds, model.getQuads(state, null, seededRandom()));
                for (Direction side : Direction.values()) {
                    add(bounds, model.getQuads(state, side, seededRandom()));
                }
            }
            if (!bounds.valid()) {
                return new Fit(0.5F, 0.5F, 0.5F, 1.0F);
            }
            float sizeX = bounds.maxX - bounds.minX;
            float sizeY = bounds.maxY - bounds.minY;
            float sizeZ = bounds.maxZ - bounds.minZ;
            float longest = Math.max(sizeX, Math.max(sizeY, sizeZ));
            if (longest <= 0.0001F) {
                return new Fit(0.5F, 0.5F, 0.5F, 1.0F);
            }
            return new Fit(
                    (bounds.minX + bounds.maxX) * 0.5F,
                    (bounds.minY + bounds.maxY) * 0.5F,
                    (bounds.minZ + bounds.maxZ) * 0.5F,
                    longest
            );
        }

        private static RandomSource seededRandom() {
            BOUNDS_RANDOM.setSeed(42L);
            return BOUNDS_RANDOM;
        }

        private static void add(Bounds bounds, List<BakedQuad> quads) {
            for (BakedQuad quad : quads) {
                int[] vertices = quad.getVertices();
                int stride = vertices.length / 4;
                for (int vertex = 0; vertex < 4; vertex++) {
                    int offset = vertex * stride;
                    bounds.add(
                            Float.intBitsToFloat(vertices[offset]),
                            Float.intBitsToFloat(vertices[offset + 1]),
                            Float.intBitsToFloat(vertices[offset + 2])
                    );
                }
            }
        }
    }

    private static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void add(float x, float y, float z) {
            this.minX = Math.min(this.minX, x);
            this.minY = Math.min(this.minY, y);
            this.minZ = Math.min(this.minZ, z);
            this.maxX = Math.max(this.maxX, x);
            this.maxY = Math.max(this.maxY, y);
            this.maxZ = Math.max(this.maxZ, z);
        }

        private boolean valid() {
            return Float.isFinite(this.minX) && Float.isFinite(this.minY) && Float.isFinite(this.minZ)
                    && Float.isFinite(this.maxX) && Float.isFinite(this.maxY) && Float.isFinite(this.maxZ);
        }
    }
}
