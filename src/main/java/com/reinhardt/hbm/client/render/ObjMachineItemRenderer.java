package com.reinhardt.hbm.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.item.BatteryPackItem;
import com.reinhardt.hbm.item.DecoCrtBlockItem;
import com.reinhardt.hbm.item.FilingCabinetBlockItem;
import com.reinhardt.hbm.item.VendingMachineBlockItem;
import com.reinhardt.hbm.item.CrashedBombBlockItem;
import com.reinhardt.hbm.block.DecoCrtBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
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

/**
 * Renders complete OBJ machine items. Every item owns a literal pose; this
 * class deliberately contains no shared bounds fitting or fallback pose.
 */
public final class ObjMachineItemRenderer extends BlockEntityWithoutLevelRenderer {
    private static final RandomSource BOUNDS_RANDOM = RandomSource.create();
    private static final List<String> BATTERY_PACK_VARIANTS = List.of(
            "battery_redstone", "battery_lead", "battery_lithium", "battery_sodium",
            "battery_schrabidium", "battery_quantum", "capacitor_copper", "capacitor_gold",
            "capacitor_niobium", "capacitor_tantalum", "capacitor_bismuth", "capacitor_spark"
    );
    private static final List<String> DECORATIVE_PIPE_IDS = List.of(
            "deco_pipe", "deco_pipe_rusted", "deco_pipe_green", "deco_pipe_green_rusted",
            "deco_pipe_red", "deco_pipe_marked", "deco_pipe_rim", "deco_pipe_rim_rusted",
            "deco_pipe_rim_green", "deco_pipe_rim_green_rusted", "deco_pipe_rim_red",
            "deco_pipe_rim_marked", "deco_pipe_framed", "deco_pipe_framed_rusted",
            "deco_pipe_framed_green", "deco_pipe_framed_green_rusted", "deco_pipe_framed_red",
            "deco_pipe_framed_marked", "deco_pipe_quad", "deco_pipe_quad_rusted",
            "deco_pipe_quad_green", "deco_pipe_quad_green_rusted", "deco_pipe_quad_red",
            "deco_pipe_quad_marked"
    );
    private static final Map<String, ModelProfile> MODEL_PROFILES = createModelProfiles();
    private static final Set<String> DIAGNOSTICS = ConcurrentHashMap.newKeySet();

    public ObjMachineItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    public static Set<String> itemIds() {
        return MODEL_PROFILES.keySet();
    }

    /** Inventory model keys are registry item keys, not CustomModelData variant names. */
    public static Set<String> inventoryModelIds() {
        java.util.LinkedHashSet<String> ids = new java.util.LinkedHashSet<>();
        MODEL_PROFILES.keySet().stream()
                // The six capacitors are independent registry items. Only the
                // remaining battery metadata profiles share battery_pack's model id.
                .filter(id -> !id.startsWith("battery_pack_battery_")
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
        MODEL_PROFILES.values().stream()
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
                // DecoCrtBlockItem.variantId already returns "crt_clean",
                // "crt_broken", etc.  Adding another "crt_" made every
                // profile miss and left the old CRT item invisible.
                ? "deco_" + crt.variantId(stack)
                : stack.getItem() instanceof VendingMachineBlockItem vending
                ? "vending_machine_" + (vending.variantIndex(stack) == 1 ? "snacks" : "soda")
                : id;
        if (id.equals("gear_large_steel")) {
            profileId = "gear_large";
        }
        ModelProfile profile = MODEL_PROFILES.get(profileId);
        if (LauncherBlockEntityRenderer.ITEM_IDS.contains(id)) {
            BlockState launcherState = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            switch (id) {
                case "launch_pad" -> LauncherBlockEntityRenderer.renderSmallLaunchPadItem(
                        launcherState, context, poseStack, bufferSource, packedLight, packedOverlay);
                case "launch_pad_rusted" -> LauncherBlockEntityRenderer.renderRustedLaunchPadItem(
                        launcherState, context, poseStack, bufferSource, packedLight, packedOverlay);
                case "compact_launcher" -> LauncherBlockEntityRenderer.renderCompactLauncherItem(
                        launcherState, context, poseStack, bufferSource, packedLight, packedOverlay);
                case "launch_pad_large" -> LauncherBlockEntityRenderer.renderLargeLaunchPadItem(
                        launcherState, context, poseStack, bufferSource, packedLight, packedOverlay);
                case "launch_table" -> LauncherBlockEntityRenderer.renderLaunchTableItem(
                        launcherState, context, poseStack, bufferSource, packedLight, packedOverlay);
                default -> throw new IllegalStateException("Unhandled launcher item: " + id);
            }
            return;
        }
        if (profile == null) {
            return;
        }

        BlockState state = stack.getItem() instanceof BlockItem blockItem
                ? blockItem.getBlock().defaultBlockState()
                : Blocks.IRON_BLOCK.defaultBlockState();
        if (stack.getItem() instanceof DecoCrtBlockItem crt) {
            state = state.setValue(DecoCrtBlock.VARIANT, crt.variantIndex(stack));
        }
        List<BakedModel> models = profile.models().stream()
                .map(MachineModelRenderer::model)
                .toList();
        logGeometryOnce(id, profileId, profile, models, state);

        if (profileId.equals("machine_satlink")) {
            SatelliteGroundStationBlockEntityRenderer.renderItem(
                    context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_tape_drive")) {
            TapeDriveBlockEntityRenderer.renderItem(
                    context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_supercomputer")) {
            SupercomputerBlockEntityRenderer.renderItem(
                    context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        if (profileId.equals("deco_crt_clean")) {
            renderCleanCrtItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("deco_crt_broken")) {
            renderBrokenCrtItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("deco_crt_blinking")) {
            renderBlinkingCrtItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("deco_crt_bsod")) {
            renderBsodCrtItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("deco_computer")) {
            renderDecoComputerItem(models.getFirst(), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.startsWith("deco_pipe")) {
            dispatchDecorativePipeItem(profileId, models, state, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.startsWith("battery_pack_")) {
            dispatchBatteryPackItem(profileId, models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_battery_redd")) {
            renderBatteryReddItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_reactor_small")) {
            renderResearchReactorItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("watz")) {
            renderWatzItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("watz_pump")) {
            renderWatzPumpItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
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
        if (profileId.equals("crashed_bomb_balefire")) {
            renderBalefireCrashedBombItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("crashed_bomb_conventional")) {
            renderConventionalCrashedBombItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("crashed_bomb_nuke")) {
            renderNuclearCrashedBombItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("crashed_bomb_salted")) {
            renderSaltedCrashedBombItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_chungus")) {
            renderLeviathanItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_pumpjack")) {
            renderPumpjackItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_combustion_engine")) {
            renderCombustionEngineItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("radio_autocal")) {
            renderRadioAutocalItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("radio_telex")) {
            renderRadioTelexItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("machine_solar_boiler")) {
            renderSolarBoilerItem(models.getFirst(), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("solar_mirror")) {
            renderSolarMirrorItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("furnace_iron")) {
            renderIronFurnaceItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("furnace_steel")) {
            renderSteelFurnaceItem(models.getFirst(), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("gear_large")) {
            if (id.equals("gear_large_steel")) {
                renderSteelLargeGearItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            } else {
                renderLargeGearItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            }
            return;
        }

        if (profileId.equals("machine_drain")) {
            renderDrainItem(models.get(0), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        if (profileId.equals("fusion_torus")) {
            renderFusionTorusItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_klystron")) {
            renderFusionKlystronItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_klystron_creative")) {
            renderCreativeFusionKlystronItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_breeder")) {
            renderFusionBreederItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_collector")) {
            renderFusionCollectorItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_boiler")) {
            renderFusionBoilerItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_mhdt")) {
            renderFusionMhdtItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_coupler")) {
            renderFusionCouplerItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("fusion_plasma_forge")) {
            renderFusionPlasmaForgeItem(models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        if (profileId.equals("dfc_emitter")) {
            renderDfcEmitterItem(models.getFirst(), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("dfc_receiver")) {
            renderDfcReceiverItem(models.getFirst(), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("dfc_injector")) {
            renderDfcInjectorItem(models.getFirst(), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }
        if (profileId.equals("dfc_stabilizer")) {
            renderDfcStabilizerItem(models.getFirst(), state, context, poseStack, bufferSource, packedLight, packedOverlay);
            return;
        }

        DedicatedItemRender itemRender = new DedicatedItemRender(
                models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
        switch (profileId) {
            case "nuke_boy" -> renderNukeBoyItem(itemRender);
            case "nuke_gadget" -> renderNukeGadgetItem(itemRender);
            case "nuke_man" -> renderNukeManItem(itemRender);
            case "nuke_mike" -> renderNukeMikeItem(itemRender);
            case "nuke_tsar" -> renderNukeTsarItem(itemRender);
            case "nuke_fleija" -> renderNukeFleijaItem(itemRender);
            case "nuke_prototype" -> renderNukePrototypeItem(itemRender);
            case "nuke_solinium" -> renderNukeSoliniumItem(itemRender);
            case "nuke_n2" -> renderNukeN2Item(itemRender);
            case "nuke_custom" -> renderNukeCustomItem(itemRender);
            case "nuke_fstbmb" -> renderNukeFstbmbItem(itemRender);
            case "machine_arc_furnace" -> renderArcFurnaceItem(itemRender);
            case "icf" -> renderIcfItem(itemRender);
            case "machine_compressor" -> renderCompressorItem(itemRender);
            case "machine_compressor_compact" -> renderCompactCompressorItem(itemRender);
            case "machine_mixer" -> renderMixerItem(itemRender);
            case "machine_electrolyser" -> renderElectrolyserItem(itemRender);
            case "machine_arc_welder" -> renderArcWelderItem(itemRender);
            case "machine_zirnox" -> renderZirnoxItem(itemRender);
            case "machine_soldering_station" -> renderSolderingStationItem(itemRender);
            case "machine_assembly_machine" -> renderAssemblyMachineItem(itemRender);
            case "machine_tacz_ammo_assembler" -> renderTaczAmmoAssemblerItem(itemRender);
            case "machine_ashpit" -> renderAshpitItem(itemRender);
            case "heater_firebox" -> renderFireboxItem(itemRender);
            case "heater_oven" -> renderHeatingOvenItem(itemRender);
            case "heater_oilburner" -> renderOilburnerItem(itemRender);
            case "heater_electric" -> renderElectricHeaterItem(itemRender);
            case "heater_heatex" -> renderHeatExchangerItem(itemRender);
            case "machine_cyclotron" -> renderCyclotronItem(itemRender);
            case "machine_exposure_chamber" -> renderExposureChamberItem(itemRender);
            case "pa_source" -> renderParticleSourceItem(itemRender);
            case "pa_beamline" -> renderParticleBeamlineItem(itemRender);
            case "pa_rfc" -> renderParticleRfcItem(itemRender);
            case "pa_quadrupole" -> renderParticleQuadrupoleItem(itemRender);
            case "pa_dipole" -> renderParticleDipoleItem(itemRender);
            case "pa_detector" -> renderParticleDetectorItem(itemRender);
            case "machine_battery_socket" -> renderBatterySocketItem(itemRender);
            case "machine_excavator" -> renderExcavatorItem(itemRender);
            case "machine_ore_slopper" -> renderOreSlopperItem(itemRender);
            case "pile_loader" -> renderPileLoaderItem(itemRender);
            case "pile_vent" -> renderPileVentItem(itemRender);
            case "pile_control" -> renderPileControlItem(itemRender);
            case "machine_rockmill" -> renderRockMillItem(itemRender);
            case "machine_silex" -> renderSilexItem(itemRender);
            case "machine_fel" -> renderFelItem(itemRender);
            case "machine_crystallizer" -> renderCrystallizerItem(itemRender);
            case "machine_rotary_furnace" -> renderRotaryFurnaceItem(itemRender);
            case "machine_well" -> renderOilDerrickItem(itemRender);
            case "machine_fracking_tower" -> renderFrackingTowerItem(itemRender);
            case "machine_flare" -> renderFlareItem(itemRender);
            case "machine_refinery" -> renderRefineryItem(itemRender);
            case "machine_vacuum_distill" -> renderVacuumDistillItem(itemRender);
            case "machine_coker" -> renderCokerItem(itemRender);
            case "machine_fraction_tower" -> renderFractionTowerItem(itemRender);
            case "fraction_spacer" -> renderFractionSpacerItem(itemRender);
            case "machine_catalytic_cracker" -> renderCatalyticCrackerItem(itemRender);
            case "machine_catalytic_reformer" -> renderCatalyticReformerItem(itemRender);
            case "machine_liquefactor" -> renderLiquefactorItem(itemRender);
            case "machine_solidifier" -> renderSolidifierItem(itemRender);
            case "machine_hydrotreater" -> renderHydrotreaterItem(itemRender);
            case "machine_crucible" -> renderCrucibleItem(itemRender);
            case "furnace_combination" -> renderCombinationFurnaceItem(itemRender);
            case "machine_strand_caster" -> renderStrandCasterItem(itemRender);
            case "pipe_anchor" -> renderPipeAnchorItem(itemRender);
            case "piston_inserter" -> renderPistonInserterItem(itemRender);
            default -> dispatchFixedGeometryItem(profileId, itemRender);
        }
    }

    /** Exact RenderCRT#renderInventoryBlock transform owned by the clean CRT. */
    private static void renderCleanCrtItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                           PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyCleanCrtItemDisplayPose(context, poseStack);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact RenderCRT#renderInventoryBlock transform owned by the broken CRT. */
    private static void renderBrokenCrtItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                            PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyBrokenCrtItemDisplayPose(context, poseStack);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact RenderCRT#renderInventoryBlock transform owned by the blinking CRT. */
    private static void renderBlinkingCrtItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyBlinkingCrtItemDisplayPose(context, poseStack);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact RenderCRT#renderInventoryBlock transform owned by the BSOD CRT. */
    private static void renderBsodCrtItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                          PoseStack poseStack,
                                          MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyBsodCrtItemDisplayPose(context, poseStack);
        poseStack.translate(0.0F, -0.5F, 0.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact RenderBlockDecoModel#renderInventoryBlock transform from 1.7.10. */
    private static void renderDecoComputerItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                               PoseStack poseStack,
                                               MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        applyDecoComputerItemDisplayPose(context, poseStack);
        // IBM 300PL was translated by +0.1 on Y and scaled by 1.2 in the
        // legacy inventory renderer.  This is deliberately per-item, not a
        // measured-bounds fit or a shared offset.
        poseStack.translate(0.0F, 0.1F, 0.0F);
        poseStack.scale(1.2F, 1.2F, 1.2F);
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void applyCleanCrtItemDisplayPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                // crt.obj's Screen group faces +X; the local -90 degree CRT
                // turn therefore requires the +45 degree GUI basis.
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // The CRT's local -90 degree turn maps its authored +X screen
                // normal directly to +Z, facing the first-person camera.
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case HEAD -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            default -> {
                // NONE is not normally sent to ItemRenderer.
            }
        }
    }

    /** Mathless dispatch; every decorative-pipe kind owns its identity item pose. */
    private static void dispatchDecorativePipeItem(String profileId, List<BakedModel> models,
                                                   BlockState state, PoseStack poseStack,
                                                   MultiBufferSource bufferSource, int packedLight,
                                                   int packedOverlay) {
        DedicatedItemRender item = new DedicatedItemRender(models, state, ItemDisplayContext.NONE, poseStack,
                bufferSource, packedLight, packedOverlay);
        switch (profileId) {
            case "deco_pipe" -> renderPlainDecorativePipeItem(item);
            case "deco_pipe_rusted" -> renderRustedDecorativePipeItem(item);
            case "deco_pipe_green" -> renderGreenDecorativePipeItem(item);
            case "deco_pipe_green_rusted" -> renderRustedGreenDecorativePipeItem(item);
            case "deco_pipe_red" -> renderRedDecorativePipeItem(item);
            case "deco_pipe_marked" -> renderMarkedDecorativePipeItem(item);
            case "deco_pipe_rim" -> renderRimDecorativePipeItem(item);
            case "deco_pipe_rim_rusted" -> renderRustedRimDecorativePipeItem(item);
            case "deco_pipe_rim_green" -> renderGreenRimDecorativePipeItem(item);
            case "deco_pipe_rim_green_rusted" -> renderRustedGreenRimDecorativePipeItem(item);
            case "deco_pipe_rim_red" -> renderRedRimDecorativePipeItem(item);
            case "deco_pipe_rim_marked" -> renderMarkedRimDecorativePipeItem(item);
            case "deco_pipe_framed" -> renderFramedDecorativePipeItem(item);
            case "deco_pipe_framed_rusted" -> renderRustedFramedDecorativePipeItem(item);
            case "deco_pipe_framed_green" -> renderGreenFramedDecorativePipeItem(item);
            case "deco_pipe_framed_green_rusted" -> renderRustedGreenFramedDecorativePipeItem(item);
            case "deco_pipe_framed_red" -> renderRedFramedDecorativePipeItem(item);
            case "deco_pipe_framed_marked" -> renderMarkedFramedDecorativePipeItem(item);
            case "deco_pipe_quad" -> renderQuadDecorativePipeItem(item);
            case "deco_pipe_quad_rusted" -> renderRustedQuadDecorativePipeItem(item);
            case "deco_pipe_quad_green" -> renderGreenQuadDecorativePipeItem(item);
            case "deco_pipe_quad_green_rusted" -> renderRustedGreenQuadDecorativePipeItem(item);
            case "deco_pipe_quad_red" -> renderRedQuadDecorativePipeItem(item);
            case "deco_pipe_quad_marked" -> renderMarkedQuadDecorativePipeItem(item);
            default -> throw new IllegalStateException("Unhandled decorative pipe item: " + profileId);
        }
    }

    private static void applyBrokenCrtItemDisplayPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                // Broken CRT uses the same authored +X screen face.
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // The variant shares crt.obj's +X screen normal.
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case HEAD -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            default -> {
            }
        }
    }

    private static void applyBlinkingCrtItemDisplayPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                // Blinking CRT uses the same authored +X screen face.
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // The variant shares crt.obj's +X screen normal.
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case HEAD -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            default -> {
            }
        }
    }

    private static void applyBsodCrtItemDisplayPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                // BSOD CRT uses the same authored +X screen face.
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // The variant shares crt.obj's +X screen normal.
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case HEAD -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            default -> {
            }
        }
    }

    private static void applyDecoComputerItemDisplayPose(ItemDisplayContext context, PoseStack poseStack) {
        poseStack.translate(0.5F, 0.5F, 0.5F);
        switch (context) {
            case GUI -> {
                poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
                // puter.obj's monitor face is authored on +Z. The ordinary
                // 225 degree block-item basis shows its rear, while 45 keeps
                // that monitor face toward the GUI camera.
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND -> {
                poseStack.translate(0.0F, 2.5F / 16.0F, 0.0F);
                poseStack.mulPose(Axis.XP.rotationDegrees(75.0F));
                poseStack.mulPose(Axis.YP.rotationDegrees(45.0F));
                poseStack.scale(0.375F, 0.375F, 0.375F);
            }
            case FIRST_PERSON_RIGHT_HAND, FIRST_PERSON_LEFT_HAND -> {
                // puter.obj's monitor face is authored on +Z already, so no
                // first-person Y turn is required to face the player.
                poseStack.scale(0.4F, 0.4F, 0.4F);
            }
            case GROUND -> {
                poseStack.translate(0.0F, 3.0F / 16.0F, 0.0F);
                poseStack.scale(0.25F, 0.25F, 0.25F);
            }
            case FIXED -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.5F, 0.5F, 0.5F);
            }
            case HEAD -> {
                poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
                poseStack.scale(0.625F, 0.625F, 0.625F);
            }
            default -> {
            }
        }
    }

    private static void renderPlainDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderGreenDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedGreenDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderMarkedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRimDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedRimDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderGreenRimDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedGreenRimDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRedRimDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderMarkedRimDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderFramedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedFramedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderGreenFramedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedGreenFramedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRedFramedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderMarkedFramedDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderQuadDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedQuadDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderGreenQuadDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRustedGreenQuadDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderRedQuadDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderMarkedQuadDecorativePipeItem(DedicatedItemRender item) {
        item.poseStack().pushPose();
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    /** Mathless dispatch; every battery/capacitor variant owns its item pose. */
    private static void dispatchBatteryPackItem(String profileId, List<BakedModel> models, BlockState state,
                                                ItemDisplayContext context, PoseStack poseStack,
                                                MultiBufferSource bufferSource, int packedLight,
                                                int packedOverlay) {
        DedicatedItemRender item = new DedicatedItemRender(
                models, state, context, poseStack, bufferSource, packedLight, packedOverlay);
        switch (profileId) {
            case "battery_pack_battery_redstone" -> renderRedstoneBatteryPackItem(item);
            case "battery_pack_battery_lead" -> renderLeadBatteryPackItem(item);
            case "battery_pack_battery_lithium" -> renderLithiumBatteryPackItem(item);
            case "battery_pack_battery_sodium" -> renderSodiumBatteryPackItem(item);
            case "battery_pack_battery_schrabidium" -> renderSchrabidiumBatteryPackItem(item);
            case "battery_pack_battery_quantum" -> renderQuantumBatteryPackItem(item);
            case "battery_pack_capacitor_copper" -> renderCopperCapacitorPackItem(item);
            case "battery_pack_capacitor_gold" -> renderGoldCapacitorPackItem(item);
            case "battery_pack_capacitor_niobium" -> renderNiobiumCapacitorPackItem(item);
            case "battery_pack_capacitor_tantalum" -> renderTantalumCapacitorPackItem(item);
            case "battery_pack_capacitor_bismuth" -> renderBismuthCapacitorPackItem(item);
            case "battery_pack_capacitor_spark" -> renderSparkCapacitorPackItem(item);
            default -> throw new IllegalStateException("Unhandled battery-pack item: " + profileId);
        }
    }

    private static void renderRedstoneBatteryPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderLeadBatteryPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderLithiumBatteryPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderSodiumBatteryPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderSchrabidiumBatteryPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderQuantumBatteryPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderCopperCapacitorPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderGoldCapacitorPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderNiobiumCapacitorPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderTantalumCapacitorPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderBismuthCapacitorPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderSparkCapacitorPackItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    /** Exact 1.7.10 RenderBatteryREDD item renderer transform and assembly. */
    private static void renderBatteryReddItem(List<BakedModel> models, BlockState state,
                                              ItemDisplayContext context, PoseStack poseStack,
                                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(2.5F, 2.5F, 2.5F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        poseStack.scale(0.5F, 0.5F, 0.5F);
        if (!models.isEmpty()) {
            MachineModelRenderer.renderUnculledCutoutNoCull(models.get(0), poseStack, bufferSource,
                    state, packedLight, packedOverlay);
        }
        if (models.size() > 1) {
            MachineModelRenderer.renderUnculledCutoutNoCull(models.get(1), poseStack, bufferSource,
                    state, packedLight, packedOverlay);
        }
        if (models.size() > 2) {
            MachineModelRenderer.renderUnculledCutoutNoCullFullBright(models.get(2), poseStack, bufferSource,
                    state, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact 1.7.10 ItemRenderLibrary research-reactor item assembly. */
    private static void renderResearchReactorItem(List<BakedModel> models, BlockState state,
                                                  ItemDisplayContext context, PoseStack poseStack,
                                                  MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -4.0F, 0.0F);
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact 1.7.10 RenderWatz item transform. */
    private static void renderWatzItem(List<BakedModel> models, BlockState state,
                                       ItemDisplayContext context, PoseStack poseStack,
                                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(2.0F, 2.0F, 2.0F);
        }
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact 1.7.10 RenderWatzPump item transform. */
    private static void renderWatzPumpItem(List<BakedModel> models, BlockState state,
                                           ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.5F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    private static void renderFanItem(List<BakedModel> models, ItemDisplayContext context, PoseStack poseStack,
                                      MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
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
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
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
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
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
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
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

    /** Direct RenderCrashedBomb pose owned by the balefire dud. */
    private static void renderBalefireCrashedBombItem(List<BakedModel> models, BlockState state,
                                                      ItemDisplayContext context, PoseStack poseStack,
                                                      MultiBufferSource bufferSource, int packedLight,
                                                      int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, 3.0F, 0.0F);
            poseStack.scale(2.125F, 2.125F, 2.125F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.getFirst(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct RenderCrashedBomb pose owned by the conventional dud. */
    private static void renderConventionalCrashedBombItem(List<BakedModel> models, BlockState state,
                                                          ItemDisplayContext context, PoseStack poseStack,
                                                          MultiBufferSource bufferSource, int packedLight,
                                                          int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, 3.0F, 0.0F);
            poseStack.scale(2.125F, 2.125F, 2.125F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 0.0F, -0.5F);
        MachineModelRenderer.renderUnculled(models.getFirst(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct RenderCrashedBomb pose owned by the nuclear dud. */
    private static void renderNuclearCrashedBombItem(List<BakedModel> models, BlockState state,
                                                     ItemDisplayContext context, PoseStack poseStack,
                                                     MultiBufferSource bufferSource, int packedLight,
                                                     int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, 3.0F, 0.0F);
            poseStack.scale(2.125F, 2.125F, 2.125F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 0.0F, 1.25F);
        MachineModelRenderer.renderUnculled(models.getFirst(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Direct RenderCrashedBomb pose owned by the salted dud. */
    private static void renderSaltedCrashedBombItem(List<BakedModel> models, BlockState state,
                                                    ItemDisplayContext context, PoseStack poseStack,
                                                    MultiBufferSource bufferSource, int packedLight,
                                                    int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, 3.0F, 0.0F);
            poseStack.scale(2.125F, 2.125F, 2.125F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 0.0F, 0.5F);
        MachineModelRenderer.renderUnculled(models.getFirst(), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Every method below owns one machine's complete legacy item pose. */
    private static void renderNukeBoyItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        pose.translate(-1.0F, 0.0F, 0.0F);
        endDedicatedItem(item);
    }

    private static void renderNukeGadgetItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        int modelCount = Minecraft.getInstance().options.graphicsMode().get().getId() == 0
                ? 1 : item.models().size();
        renderModels(item, modelCount);
        pose.popPose();
    }

    private static void renderNukeManItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.translate(-0.75F, 0.0F, 0.0F);
        endDedicatedItem(item);
    }

    private static void renderNukeMikeItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -5.0F, 0.0F);
            pose.scale(2.25F, 2.25F, 2.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderNukeTsarItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(2.25F, 2.25F, 2.25F);
        }
        pose.translate(1.5F, 0.0F, 0.0F);
        endDedicatedItem(item);
    }

    private static void renderNukeFleijaItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(6.8F, 6.8F, 6.8F);
        }
        pose.translate(0.125F, 0.0F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        endDedicatedItem(item);
    }

    private static void renderNukePrototypeItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, 0.125F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(0.0F, 0.125F, 0.0F);
        endDedicatedItem(item);
    }

    private static void renderNukeSoliniumItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -0.125F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.translate(0.0F, -0.125F, 0.0F);
        endDedicatedItem(item);
    }

    private static void renderNukeN2Item(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -5.0F, 0.0F);
            pose.scale(2.25F, 2.25F, 2.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderNukeCustomItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        pose.translate(-1.0F, 0.0F, 0.0F);
        endDedicatedItem(item);
    }

    private static void renderNukeFstbmbItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(2.25F, 2.25F, 2.25F);
        }
        pose.translate(1.0F, 0.0F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        endDedicatedItem(item);
    }

    private static void renderArcFurnaceItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(3.5F, 3.5F, 3.5F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderIcfItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.5F, 0.0F);
            pose.scale(2.125F, 2.125F, 2.125F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderCompressorItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.0F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderCompactCompressorItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(-1.0F, -1.0F, 0.0F);
            pose.scale(2.75F, 2.75F, 2.75F);
        }
        pose.translate(0.5F, 0.0F, 0.0F);
        pose.scale(0.75F, 0.75F, 0.75F);
        endDedicatedItem(item);
    }

    private static void renderMixerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -5.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        endDedicatedItem(item);
    }

    private static void renderElectrolyserItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(-1.0F, -1.0F, 0.0F);
            pose.scale(2.5F, 2.5F, 2.5F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderArcWelderItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.0F, 0.0F);
            pose.scale(4.0F, 4.0F, 4.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderZirnoxItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.0F, 0.0F);
            pose.scale(2.8F, 2.8F, 2.8F);
        }
        pose.scale(0.75F, 0.75F, 0.75F);
        endDedicatedItem(item);
    }

    private static void renderSolderingStationItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderAssemblyMachineItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.75F, 0.0F);
            pose.scale(4.5F, 4.5F, 4.5F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.75F, 0.75F, 0.75F);
        endDedicatedItem(item);
    }

    private static void renderTaczAmmoAssemblerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.75F, 0.0F);
            pose.scale(4.5F, 4.5F, 4.5F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.75F, 0.75F, 0.75F);
        endDedicatedItem(item);
    }

    private static void renderAshpitItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        pose.translate(-0.5F, 0.0F, -0.5F);
        endDedicatedItem(item);
    }

    private static void renderFireboxItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        pose.translate(-0.5F, 0.0F, -0.5F);
        endDedicatedItem(item);
    }

    private static void renderHeatingOvenItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        pose.translate(-0.5F, 0.0F, -0.5F);
        endDedicatedItem(item);
    }

    private static void renderOilburnerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.5F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        pose.translate(-0.5F, 0.0F, -0.5F);
        endDedicatedItem(item);
    }

    private static void renderElectricHeaterItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.translate(-1.0F, 0.0F, -0.5F);
        endDedicatedItem(item);
    }

    private static void renderHeatExchangerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        pose.translate(-0.5F, 0.0F, -0.5F);
        endDedicatedItem(item);
    }

    private static void renderCyclotronItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(2.25F, 2.25F, 2.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderExposureChamberItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.5F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.translate(1.5F, 0.0F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderParticleSourceItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(4.0F, 4.0F, 4.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderParticleBeamlineItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(4.0F, 4.0F, 4.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        endDedicatedItem(item);
    }

    private static void renderParticleRfcItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(4.0F, 4.0F, 4.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderParticleQuadrupoleItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.5F, 0.0F);
            pose.scale(4.0F, 4.0F, 4.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        endDedicatedItem(item);
    }

    private static void renderParticleDipoleItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(3.5F, 3.5F, 3.5F);
        }
        endDedicatedItem(item);
    }

    private static void renderParticleDetectorItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderBatterySocketItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.0F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderExcavatorItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.0F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderOreSlopperItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(3.75F, 3.75F, 3.75F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    /** Exact RenderPileLoader item transform, adapted to modern item contexts. */
    private static void renderPileLoaderItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            pose.translate(0.0F, -3.5F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        pose.scale(2.0F, 2.0F, 2.0F);
        endDedicatedItem(item);
    }

    /** Exact RenderPileLoader item transform for the vent metadata, flattened to its own item. */
    private static void renderPileVentItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            pose.translate(0.0F, -3.5F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        pose.scale(2.0F, 2.0F, 2.0F);
        endDedicatedItem(item);
    }

    /** Exact RenderPileLoader item transform for the control metadata, flattened to its own item. */
    private static void renderPileControlItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            pose.translate(0.0F, -3.5F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        pose.scale(2.0F, 2.0F, 2.0F);
        endDedicatedItem(item);
    }

    /** Exact legacy rock-mill item transform, adapted to modern item contexts. */
    private static void renderRockMillItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
            pose.translate(0.0F, -1.5F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        pose.scale(0.75F, 0.75F, 0.75F);
        endDedicatedItem(item);
    }

    private static void renderSilexItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.5F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderFelItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.0F, 0.0F);
            pose.scale(2.0F, 2.0F, 2.0F);
        }
        pose.translate(1.0F, 0.0F, 0.0F);
        pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
        endDedicatedItem(item);
    }

    private static void renderCrystallizerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.0F, 0.0F);
            pose.scale(2.0F, 2.0F, 2.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderRotaryFurnaceItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.0F, 0.0F);
            pose.scale(3.5F, 3.5F, 3.5F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.625F, 0.625F, 0.625F);
        endDedicatedItem(item);
    }

    private static void renderOilDerrickItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.0F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderFrackingTowerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.5F, 0.0F);
            pose.scale(2.5F, 2.5F, 2.5F);
        }
        pose.scale(0.25F, 0.25F, 0.25F);
        endDedicatedItem(item);
    }

    private static void renderFlareItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.0F, 0.0F);
            pose.scale(2.25F, 2.25F, 2.25F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderRefineryItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.0F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderVacuumDistillItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.0F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderCokerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -5.0F, 0.0F);
            pose.scale(2.75F, 2.75F, 2.75F);
        }
        pose.scale(0.25F, 0.25F, 0.25F);
        endDedicatedItem(item);
    }

    private static void renderFractionTowerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.5F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderFractionSpacerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderCatalyticCrackerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.5F, 0.0F);
            pose.scale(1.8F, 1.8F, 1.8F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderCatalyticReformerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.0F, 0.0F);
            pose.scale(3.5F, 3.5F, 3.5F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderLiquefactorItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.5F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderSolidifierItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.5F, 0.0F);
            pose.scale(3.0F, 3.0F, 3.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderHydrotreaterItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -4.0F, 0.0F);
            pose.scale(4.0F, 4.0F, 4.0F);
        }
        pose.scale(0.5F, 0.5F, 0.5F);
        endDedicatedItem(item);
    }

    private static void renderCrucibleItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.5F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderCombinationFurnaceItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -1.5F, 0.0F);
            pose.scale(3.25F, 3.25F, 3.25F);
        }
        endDedicatedItem(item);
    }

    private static void renderStrandCasterItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(2.0F, 0.0F, 2.0F);
            pose.scale(2.0F, 2.0F, 2.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderPipeAnchorItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -3.5F, 0.0F);
            pose.scale(10.0F, 10.0F, 10.0F);
        }
        endDedicatedItem(item);
    }

    private static void renderPistonInserterItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        if (item.context() == ItemDisplayContext.GUI) {
            pose.mulPose(Axis.XP.rotationDegrees(30.0F));
            pose.mulPose(Axis.YP.rotationDegrees(225.0F));
            pose.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            pose.translate(0.0F, 11.3F, -11.3F);
        } else {
            pose.translate(0.5F, 0.25F, 0.0F);
            pose.scale(0.25F, 0.25F, 0.25F);
            if (item.context() != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                pose.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (item.context() == ItemDisplayContext.GUI) {
            pose.translate(0.0F, -2.5F, 0.0F);
            pose.scale(5.0F, 5.0F, 5.0F);
        }
        pose.scale(2.0F, 2.0F, 2.0F);
        endDedicatedItem(item);
    }

    private static void endDedicatedItem(DedicatedItemRender item) {
        renderModels(item, item.models().size());
        item.poseStack().popPose();
    }

    private static void renderModels(DedicatedItemRender item, int modelCount) {
        for (int index = 0; index < modelCount; index++) {
            MachineModelRenderer.renderUnculled(item.models().get(index), item.poseStack(), item.bufferSource(),
                    item.state(), item.packedLight(), item.packedOverlay());
        }
    }

    /** Mathless dispatch only; every fixed-geometry item owns its pose below. */
    private static void dispatchFixedGeometryItem(String profileId, DedicatedItemRender item) {
        switch (profileId) {
            case "transition_seal" -> renderTransitionSealItem(item);
            case "boltgun" -> renderBoltgunItem(item);
            case "machine_ammo_press" -> renderAmmoPressItem(item);
            case "machine_radar" -> renderRadarItem(item);
            case "machine_radar_large" -> renderLargeRadarItem(item);
            case "machine_radgen" -> renderRadgenItem(item);
            case "machine_blast_furnace" -> renderBlastFurnaceItem(item);
            case "tesla" -> renderTeslaItem(item);
            case "spotlight_incandescent" -> renderIncandescentSpotlightItem(item);
            case "spotlight_incandescent_off" -> renderUnlitIncandescentSpotlightItem(item);
            case "spotlight_fluoro" -> renderFluorescentSpotlightItem(item);
            case "spotlight_fluoro_off" -> renderUnlitFluorescentSpotlightItem(item);
            case "spotlight_halogen" -> renderHalogenSpotlightItem(item);
            case "spotlight_halogen_off" -> renderUnlitHalogenSpotlightItem(item);
            case "sat_dock" -> renderSatelliteDockItem(item);
            case "vending_machine_soda" -> renderSodaVendingMachineItem(item);
            case "vending_machine_snacks" -> renderSnackVendingMachineItem(item);
            case "tape_recorder" -> renderTapeRecorderItem(item);
            case "skeleton_holder" -> renderSkeletonHolderItem(item);
            case "chimney_brick" -> renderBrickChimneyItem(item);
            case "chimney_industrial" -> renderIndustrialChimneyItem(item);
            case "machine_boiler" -> renderBoilerItem(item);
            case "machine_industrial_boiler" -> renderIndustrialBoilerItem(item);
            case "machine_condenser_powered" -> renderPoweredCondenserItem(item);
            case "machine_tower_small" -> renderSmallCoolingTowerItem(item);
            case "machine_tower_large" -> renderLargeCoolingTowerItem(item);
            case "barrel_corroded" -> renderCorrodedBarrelItem(item);
            case "barrel_antimatter" -> renderAntimatterBarrelItem(item);
            case "boat" -> renderBoatItem(item);
            default -> throw new IllegalStateException("Missing dedicated OBJ item pose: " + profileId);
        }
    }

    private static void renderTransitionSealItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 30.1927032F;
        pose.scale(scale, scale, scale);
        pose.translate(-3.29187918F, -15.0963478F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderBoltgunItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 8.25F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -0.0410540104F, -0.375F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderAmmoPressItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.92F : 0.85F) / 3.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -1.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderRadarItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.90F : 0.85F) / 3.0F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.0625F, -1.125F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderLargeRadarItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.90F : 0.85F) / 10.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -5.0F, -0.171875F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderRadgenItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 6.125F;
        pose.scale(scale, scale, scale);
        pose.translate(0.4375F, -1.5F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderBlastFurnaceItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 7.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -3.5F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderTeslaItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(180.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.90F : 0.85F) / 1.9375F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -0.96875F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderIncandescentSpotlightItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 0.5F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.1015625F, 0.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderUnlitIncandescentSpotlightItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 0.5F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.1015625F, 0.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderFluorescentSpotlightItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 0.75F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.0625F, 0.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderUnlitFluorescentSpotlightItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 0.75F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.0625F, 0.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderHalogenSpotlightItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 0.625F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.15625F, 0.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderUnlitHalogenSpotlightItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 0.625F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.15625F, 0.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderSatelliteDockItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.90F : 0.85F) / 3.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -0.375F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderSodaVendingMachineItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.90F : 0.85F) / 2.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -1.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderSnackVendingMachineItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.90F : 0.85F) / 2.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -1.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderTapeRecorderItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0625F, -0.5F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderSkeletonHolderItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.YP.rotationDegrees(90.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.88F : 0.85F) / 1.42579401F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0847695023F, -0.712897003F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderBrickChimneyItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 13.0F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.5F, -6.5F, -0.5F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderIndustrialChimneyItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 23.0F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.5F, -11.5F, -0.5F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderBoilerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 4.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -2.0F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderIndustrialBoilerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(315.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 5.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -2.5F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderPoweredCondenserItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 7.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -1.5F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderSmallCoolingTowerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 19.5F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -9.75F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderLargeCoolingTowerItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.82F : 0.85F) / 13.0F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -6.5F, 0.0F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderCorrodedBarrelItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = item.context() == ItemDisplayContext.GUI ? 0.78F : 0.82F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.5F, -0.5F, -0.5F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderAntimatterBarrelItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        pose.mulPose(Axis.XP.rotationDegrees(30.0F));
        pose.mulPose(Axis.YP.rotationDegrees(225.0F));
        float scale = item.context() == ItemDisplayContext.GUI ? 0.78F : 0.82F;
        pose.scale(scale, scale, scale);
        pose.translate(-0.5F, -0.5F, -0.5F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    private static void renderBoatItem(DedicatedItemRender item) {
        PoseStack pose = item.poseStack();
        pose.pushPose();
        pose.translate(0.5F, 0.5F, 0.5F);
        float scale = (item.context() == ItemDisplayContext.GUI ? 0.92F : 0.85F) / 21.75F;
        pose.scale(scale, scale, scale);
        pose.translate(0.0F, -2.86756134F, -2.375F);
        renderModels(item, item.models().size());
        pose.popPose();
    }

    /** Exact 1.7.10 RenderDrain item renderer transform. */
    private static void renderDrainItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                        PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                        int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(-1.0F, -1.0F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        poseStack.translate(0.75F, 0.0F, 0.0F);
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact ItemRenderLibrary transform for RenderPumpjack's inventory renderer. */
    private static void renderPumpjackItem(List<BakedModel> models, BlockState state,
                                           ItemDisplayContext context, PoseStack poseStack,
                                           MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            poseStack.scale(4.0F, 4.0F, 4.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.translate(0.0F, 0.0F, 3.0F);
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact ItemRenderLibrary transform for the Leviathan's three static OBJ groups. */
    private static void renderLeviathanItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                            PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                            int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.5F, 0.0F, 0.0F);
            poseStack.scale(2.5F, 2.5F, 2.5F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /**
     * RenderCombustionEngine rotates before its positive Z translation. Keeping
     * that order is essential: translating first shifts the icon on the wrong
     * screen axis after the inventory yaw is applied.
     */
    private static void renderCombustionEngineItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                                   PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                   int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(2.75F, 2.75F, 2.75F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        poseStack.translate(0.0F, 0.0F, 2.75F);
        for (BakedModel model : models) {
            MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        poseStack.popPose();
    }

    /** Exact RenderAUTOCAL#getRenderer inventory transform. */
    private static void renderRadioAutocalItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                               int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -4.0F, 0.0F);
            poseStack.scale(6.25F, 6.25F, 6.25F);
        }
        AutocalBlockEntityRenderer.renderModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact RenderTelex#getRenderer inventory transform and common offset. */
    private static void renderRadioTelexItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                             int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
        }
        poseStack.translate(0.0F, 0.0F, -0.5F);
        RadioTelexBlockEntityRenderer.renderModel(state, poseStack, bufferSource, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact ItemRenderLibrary transform for the 1.7.10 solar boiler. */
    private static void renderSolarBoilerItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                              int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(3.25F, 3.25F, 3.25F);
        }
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact ItemRenderLibrary transform and mirror pose for the 1.7.10 heliostat. */
    private static void renderSolarMirrorItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                              int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(8.0F, 8.0F, 8.0F);
        }
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.translate(0.0F, 1.0F, 0.0F);
        poseStack.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        poseStack.translate(0.0F, -1.0F, 0.0F);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact ItemRenderLibrary transform for the 1.7.10 iron furnace. */
    private static void renderIronFurnaceItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                              int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact RenderFurnaceSteel item transform from 1.7.10. */
    private static void renderSteelFurnaceItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                               int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.5F, 0.0F);
            poseStack.scale(3.25F, 3.25F, 3.25F);
        }
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact ItemRenderLibrary transform owned by the ordinary large gear. */
    private static void renderLargeGearItem(List<BakedModel> models, BlockState state,
                                            ItemDisplayContext context, PoseStack poseStack,
                                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -7.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.translate(0.0F, 1.375F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees((System.currentTimeMillis() % 3600L) * 0.1F));
            poseStack.translate(0.0F, -1.375F, 0.0F);
        }
        poseStack.translate(0.0F, 0.0F, -0.875F);
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact ItemRenderLibrary transform owned by the steel large gear. */
    private static void renderSteelLargeGearItem(List<BakedModel> models, BlockState state,
                                                 ItemDisplayContext context, PoseStack poseStack,
                                                 MultiBufferSource bufferSource, int packedLight,
                                                 int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -7.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(-45.0F));
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.translate(0.0F, 1.375F, 0.0F);
            poseStack.mulPose(Axis.ZP.rotationDegrees((System.currentTimeMillis() % 3600L) * 0.1F));
            poseStack.translate(0.0F, -1.375F, 0.0F);
        }
        poseStack.translate(0.0F, 0.0F, -0.875F);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource,
                state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact RenderFusionTorus#getRenderer item assembly and inventory pose. */
    private static void renderFusionTorusItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                              int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.scale(2.0F, 2.0F, 2.0F);
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees((System.currentTimeMillis() / 5L) % 360L));
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** Exact RenderFusionKlystron#getRenderer item assembly and inventory pose. */
    private static void renderFusionKlystronItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                                 PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                 int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 1.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((System.currentTimeMillis() / 10L) % 360L));
        poseStack.translate(0.0F, -2.5F, 0.0F);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** Exact RenderFusionBreeder#getRenderer item assembly and inventory pose. */
    private static void renderFusionBreederItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                                PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact RenderFusionCollector#getRenderer item assembly and inventory pose. */
    private static void renderFusionCollectorItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                                  PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                  int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.0F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact RenderFusionBoiler#getRenderer item assembly and inventory pose. */
    private static void renderFusionBoilerItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                               PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                               int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact RenderFusionMHDT#getRenderer item assembly and inventory pose. */
    private static void renderFusionMhdtItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                             int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.scale(2.5F, 2.5F, 2.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((System.currentTimeMillis() / 5L) % 30L - 15.0F));
        poseStack.translate(0.0F, -1.5F, 0.0F);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** Exact RenderFusionCoupler#getRenderer item assembly and inventory pose. */
    private static void renderFusionCouplerItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                                PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 0.0F);
            poseStack.scale(6.0F, 6.0F, 6.0F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact RenderFusionPlasmaForge#getRenderer static item assembly and inventory pose. */
    private static void renderFusionPlasmaForgeItem(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                                    PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                    int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -1.0F, 0.0F);
            poseStack.scale(2.75F, 2.75F, 2.75F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        for (int index = 0; index < models.size() - 1; index++) {
            MachineModelRenderer.renderUnculled(models.get(index), poseStack, bufferSource, state, packedLight, packedOverlay);
        }
        MachineModelRenderer.renderUnculledTintedLightning(models.get(models.size() - 1), poseStack, bufferSource,
                state, packedOverlay, 0xFF000000);
        poseStack.popPose();
    }

    /** Exact legacy item transform owned by the DFC emitter. */
    private static void renderDfcEmitterItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                             int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact creative-klystron item pose, kept independent from the ordinary klystron. */
    private static void renderCreativeFusionKlystronItem(List<BakedModel> models, BlockState state,
                                                         ItemDisplayContext context, PoseStack poseStack,
                                                         MultiBufferSource bufferSource, int packedLight,
                                                         int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -3.0F, 1.0F);
            poseStack.scale(3.5F, 3.5F, 3.5F);
            poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(models.get(0), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.pushPose();
        poseStack.translate(0.0F, 2.5F, 0.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees((System.currentTimeMillis() / 10L) % 360L));
        poseStack.translate(0.0F, -2.5F, 0.0F);
        MachineModelRenderer.renderUnculled(models.get(1), poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
        poseStack.popPose();
    }

    /** Exact legacy item transform owned by the DFC receiver. */
    private static void renderDfcReceiverItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                              int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact legacy item transform owned by the DFC injector. */
    private static void renderDfcInjectorItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                              PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                              int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    /** Exact legacy item transform owned by the DFC stabilizer. */
    private static void renderDfcStabilizerItem(BakedModel model, BlockState state, ItemDisplayContext context,
                                                PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                                int packedOverlay) {
        poseStack.pushPose();
        if (context == ItemDisplayContext.GUI) {
            poseStack.mulPose(Axis.XP.rotationDegrees(30.0F));
            poseStack.mulPose(Axis.YP.rotationDegrees(225.0F));
            poseStack.scale(1.0F / 16.0F, 1.0F / 16.0F, 1.0F / 16.0F);
            poseStack.translate(0.0F, 11.3F, -11.3F);
        } else {
            poseStack.translate(0.5F, 0.25F, 0.0F);
            poseStack.scale(0.25F, 0.25F, 0.25F);
            if (context != ItemDisplayContext.THIRD_PERSON_RIGHT_HAND) {
                poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
            }
        }
        if (context == ItemDisplayContext.GUI) {
            poseStack.translate(0.0F, -2.5F, 0.0F);
            poseStack.scale(5.0F, 5.0F, 5.0F);
        }
        poseStack.scale(2.0F, 2.0F, 2.0F);
        poseStack.mulPose(Axis.YP.rotationDegrees(90.0F));
        MachineModelRenderer.renderUnculled(model, poseStack, bufferSource, state, packedLight, packedOverlay);
        poseStack.popPose();
    }

    private static void logGeometryOnce(String itemId, String profileId, ModelProfile profile,
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

    private static Map<String, ModelProfile> createModelProfiles() {
        Map<String, ModelProfile> profiles = new LinkedHashMap<>();
        models(profiles, "launch_pad", "block/launch_pad_world");
        models(profiles, "launch_pad_rusted", "block/launch_pad_rusted_world");
        models(profiles, "launch_pad_large", "block/launcher_port/pad_base");
        models(profiles, "compact_launcher", "block/compact_launcher_world");
        models(profiles, "launch_table", "block/launch_table_base");
        models(profiles, "machine_arc_furnace", "block/machine_arc_furnace", "block/machine_arc_furnace_lid", "block/machine_arc_furnace_ring1", "block/machine_arc_furnace_ring2", "block/machine_arc_furnace_ring3", "block/machine_arc_furnace_electrode1", "block/machine_arc_furnace_electrode2", "block/machine_arc_furnace_electrode3", "block/machine_arc_furnace_cable1", "block/machine_arc_furnace_cable2", "block/machine_arc_furnace_cable3");
        models(profiles, "transition_seal", "block/transition_seal");
        models(profiles, "boltgun", "block/boltgun");
        models(profiles, "icf", "block/icf");
        models(profiles, "bomb_multi", "block/bomb_multi_world");
        models(profiles, "nuke_boy", "block/nuke_boy_world");
        models(profiles, "nuke_gadget", "block/nuke_gadget_body", "block/nuke_gadget_wires");
        models(profiles, "nuke_man", "block/nuke_man_world");
        models(profiles, "nuke_mike", "block/nuke_mike_world");
        models(profiles, "nuke_tsar", "block/nuke_tsar_world");
        models(profiles, "nuke_fleija", "block/nuke_fleija_world");
        models(profiles, "nuke_prototype", "block/nuke_prototype_world");
        models(profiles, "nuke_solinium", "block/nuke_solinium_world");
        models(profiles, "nuke_n2", "block/nuke_n2_world");
        models(profiles, "nuke_custom", "block/nuke_custom_world");
        models(profiles, "nuke_fstbmb", "block/nuke_fstbmb_world");
        models(profiles, "crashed_bomb_balefire", "block/crashed_bomb_balefire");
        models(profiles, "crashed_bomb_conventional", "block/crashed_bomb_conventional");
        models(profiles, "crashed_bomb_nuke", "block/crashed_bomb_nuke");
        models(profiles, "crashed_bomb_salted", "block/crashed_bomb_salted");
        models(profiles, "machine_ammo_press", "block/machine_ammo_press");
        models(profiles, "machine_ashpit", "block/machine_ashpit_world", "block/machine_ashpit_door");
        models(profiles, "machine_compressor", "block/machine_compressor_world", "block/machine_compressor_pump", "block/machine_compressor_fan");
        models(profiles, "machine_compressor_compact", "block/machine_compressor_compact_world", "block/machine_compressor_compact_fan1", "block/machine_compressor_compact_fan2");
        models(profiles, "machine_mixer", "block/machine_mixer_world", "block/machine_mixer_blade");
        models(profiles, "machine_electrolyser", "block/machine_electrolyser_world");
        models(profiles, "machine_arc_welder", "block/machine_arc_welder_world");
        models(profiles, "machine_zirnox", "block/machine_zirnox_world");
        models(profiles, "machine_drain", "block/machine_drain");
        models(profiles, "machine_soldering_station", "block/machine_soldering_station_world");
        models(profiles, "machine_assembly_machine", "block/machine_assembly_machine");
        models(profiles, "machine_tacz_ammo_assembler", "block/machine_assembly_machine");
        models(profiles, "machine_cyclotron", "block/machine_cyclotron_body", "block/machine_cyclotron_b1", "block/machine_cyclotron_b2", "block/machine_cyclotron_b3", "block/machine_cyclotron_b4");
        models(profiles, "machine_exposure_chamber", "block/machine_exposure_chamber_chamber", "block/machine_exposure_chamber_magnets", "block/machine_exposure_chamber_core");
        models(profiles, "machine_radar", "block/machine_radar_base", "block/machine_radar_dish");
        models(profiles, "machine_radar_large", "block/machine_radar_large_base", "block/machine_radar_large_dish");
        models(profiles, "machine_radgen", "block/machine_radgen_base", "block/machine_radgen_rotor", "block/machine_radgen_light", "block/machine_radgen_glass");
        models(profiles, "pa_source", "block/pa_source");
        models(profiles, "pa_beamline", "block/pa_beamline_body");
        models(profiles, "pa_rfc", "block/pa_rfc");
        models(profiles, "pa_quadrupole", "block/pa_quadrupole");
        models(profiles, "pa_dipole", "block/pa_dipole");
        models(profiles, "pa_detector", "block/pa_detector");
        models(profiles, "machine_chungus", "block/machine_chungus_body", "block/machine_chungus_lever", "block/machine_chungus_blades");
        models(profiles, "machine_combustion_engine", "block/machine_combustion_engine");
        models(profiles, "machine_battery_socket", "block/machine_battery_socket_socket");
        models(profiles, "machine_battery_redd", "block/machine_battery_redd_base", "block/machine_battery_redd_wheel", "block/machine_battery_redd_lights");
        models(profiles, "machine_reactor_small", "block/machine_reactor_small_base", "block/machine_reactor_small_rods");
        models(profiles, "watz", "block/watz_world");
        models(profiles, "watz_pump", "block/watz_pump_world");
        models(profiles, "fusion_torus", "block/fusion_torus_torus", "block/fusion_torus_magnet");
        models(profiles, "fusion_klystron", "block/fusion_klystron_body", "block/fusion_klystron_rotor");
        models(profiles, "fusion_klystron_creative", "block/fusion_klystron_creative_body", "block/fusion_klystron_creative_rotor");
        models(profiles, "fusion_breeder", "block/fusion_breeder");
        models(profiles, "fusion_collector", "block/fusion_collector");
        models(profiles, "fusion_boiler", "block/fusion_boiler");
        models(profiles, "fusion_mhdt", "block/fusion_mhdt_turbine", "block/fusion_mhdt_coils");
        models(profiles, "fusion_coupler", "block/fusion_coupler");
        models(profiles, "fusion_plasma_forge", "block/fusion_plasma_forge_body", "block/fusion_plasma_forge_slider_striker", "block/fusion_plasma_forge_arm_lower_striker", "block/fusion_plasma_forge_arm_upper_striker", "block/fusion_plasma_forge_striker_mount", "block/fusion_plasma_forge_striker_left", "block/fusion_plasma_forge_striker_right", "block/fusion_plasma_forge_piston_left", "block/fusion_plasma_forge_piston_right", "block/fusion_plasma_forge_slider_jet", "block/fusion_plasma_forge_arm_lower_jet", "block/fusion_plasma_forge_arm_upper_jet", "block/fusion_plasma_forge_jet", "block/fusion_plasma_forge_plasma");
        models(profiles, "dfc_emitter", "block/dfc_emitter");
        models(profiles, "dfc_receiver", "block/dfc_receiver");
        models(profiles, "dfc_injector", "block/dfc_injector");
        models(profiles, "dfc_stabilizer", "block/dfc_stabilizer");
        models(profiles, "machine_excavator", "block/machine_excavator_main", "block/machine_excavator_crusher1", "block/machine_excavator_crusher2", "block/machine_excavator_drillbit", "block/machine_excavator_shaft");
        models(profiles, "machine_blast_furnace", "block/machine_blast_furnace");
        models(profiles, "machine_crucible", "block/machine_crucible_world");
        models(profiles, "machine_strand_caster", "block/machine_strand_caster_item");
        models(profiles, "machine_crystallizer", "block/machine_crystallizer_body", "block/machine_crystallizer_spinner");
        models(profiles, "machine_ore_slopper", "block/machine_ore_slopper_base", "block/machine_ore_slopper_slider", "block/machine_ore_slopper_hydraulics", "block/machine_ore_slopper_bucket", "block/machine_ore_slopper_blades_left", "block/machine_ore_slopper_blades_right", "block/machine_ore_slopper_fan");
        models(profiles, "pile_loader", "block/pile_loader_all");
        models(profiles, "pile_vent", "block/pile_vent_all");
        models(profiles, "pile_control", "block/pile_control_all");
        models(profiles, "machine_rockmill", "block/machine_rockmill_base", "block/machine_rockmill_frame", "block/machine_rockmill_wheel");
        models(profiles, "machine_satlink", "block/machine_satlink_base", "block/machine_satlink_rotor", "block/machine_satlink_dish");
        models(profiles, "machine_tape_drive", "block/machine_tape_drive_frame");
        models(profiles, "machine_supercomputer", "block/machine_supercomputer_computer", "block/machine_supercomputer_lights");
        models(profiles, "machine_silex", "block/machine_silex");
        models(profiles, "machine_fel", "block/machine_fel");
        models(profiles, "machine_rotary_furnace", "block/machine_rotary_furnace");
        models(profiles, "tesla", "block/tesla");
        models(profiles, "fan", "block/fan_frame", "block/fan_blades");
        models(profiles, "floodlight", "block/floodlight_base", "block/floodlight_lights", "block/floodlight_lamps");
        models(profiles, "spotlight_incandescent", "block/spotlight_incandescent_world");
        models(profiles, "spotlight_incandescent_off", "block/spotlight_incandescent_off_world");
        models(profiles, "spotlight_fluoro", "block/spotlight_fluoro_single_world");
        models(profiles, "spotlight_fluoro_off", "block/spotlight_fluoro_single_off_world");
        models(profiles, "spotlight_halogen", "block/spotlight_halogen_world");
        models(profiles, "spotlight_halogen_off", "block/spotlight_halogen_off_world");
        models(profiles, "cargo_elevator", "block/cargo_elevator_base", "block/cargo_elevator_piston", "block/cargo_elevator_guides", "block/cargo_elevator_platform");
        models(profiles, "sat_dock", "block/sat_dock");
        models(profiles, "radio_autocal", "block/radio_autocal");
        models(profiles, "radio_telex", "block/radio_telex_item");
        models(profiles, "vending_machine_soda", "block/vending_machine_soda");
        models(profiles, "vending_machine_snacks", "block/vending_machine_snacks");
        models(profiles, "tape_recorder", "block/tape_recorder");
        models(profiles, "skeleton_holder", "block/skeleton_holder_world");
        models(profiles, "chimney_brick", "block/chimney_brick");
        models(profiles, "chimney_industrial", "block/chimney_industrial");
        models(profiles, "heater_firebox", "block/heater_firebox_world", "block/heater_firebox_door");
        models(profiles, "heater_oven", "block/heater_oven_world", "block/heater_oven_door");
        models(profiles, "heater_oilburner", "block/heater_oilburner");
        models(profiles, "heater_electric", "block/heater_electric");
        models(profiles, "heater_heatex", "block/heater_heatex");
        models(profiles, "machine_boiler", "block/heat_boiler_world");
        models(profiles, "machine_industrial_boiler", "block/machine_industrial_boiler_world");
        models(profiles, "machine_solar_boiler", "block/machine_solar_boiler_base");
        models(profiles, "pipe_anchor", "block/pipe_anchor");
        models(profiles, "piston_inserter", "block/piston_inserter");
        models(profiles, "solar_mirror", "block/solar_mirror_base", "block/solar_mirror_mirror");
        models(profiles, "machine_condenser_powered", "block/machine_condenser_powered");
        models(profiles, "machine_tower_small", "block/machine_tower_small_world");
        models(profiles, "machine_tower_large", "block/machine_tower_large_world");
        models(profiles, "machine_blast_furnace", "block/machine_blast_furnace");
        models(profiles, "furnace_combination", "block/furnace_combination");
        models(profiles, "furnace_iron", "block/furnace_iron_main", "block/furnace_iron_off");
        models(profiles, "furnace_steel", "block/furnace_steel");
        models(profiles, "gear_large", "block/gear_large_1710", "block/gear_large_steel_1710");
        models(profiles, "barrel_corroded", "block/barrel_corroded");
        models(profiles, "barrel_antimatter", "block/barrel_antimatter");
        models(profiles, "machine_well", "block/machine_well");
        models(profiles, "machine_pumpjack", "block/machine_pumpjack");
        models(profiles, "machine_fracking_tower", "block/machine_fracking_tower");
        models(profiles, "machine_refinery", "block/machine_refinery");
        models(profiles, "machine_vacuum_distill", "block/machine_vacuum_distill");
        models(profiles, "machine_coker", "block/machine_coker");
        models(profiles, "machine_flare", "block/machine_flare");
        models(profiles, "machine_solidifier", "block/machine_solidifier");
        models(profiles, "machine_liquefactor", "block/machine_liquefactor");
        models(profiles, "machine_fraction_tower", "block/machine_fraction_tower");
        models(profiles, "fraction_spacer", "block/fraction_spacer");
        models(profiles, "machine_catalytic_cracker", "block/machine_catalytic_cracker");
        models(profiles, "machine_catalytic_reformer", "block/machine_catalytic_reformer");
        models(profiles, "machine_hydrotreater", "block/machine_hydrotreater");
        models(profiles, "deco_computer", "block/deco_computer");
        models(profiles, "boat", "block/boat");
        models(profiles, "deco_crt_clean", "block/deco_crt_clean_monitor", "block/deco_crt_clean_screen");
        models(profiles, "deco_crt_broken", "block/deco_crt_broken_monitor", "block/deco_crt_broken_screen");
        models(profiles, "deco_crt_blinking", "block/deco_crt_blinking_monitor", "block/deco_crt_blinking_screen");
        models(profiles, "deco_crt_bsod", "block/deco_crt_bsod_monitor", "block/deco_crt_bsod_screen");
        for (String variant : BATTERY_PACK_VARIANTS) {
            models(profiles, "battery_pack_" + variant, "block/machine_battery_socket_" + variant);
        }
        for (String id : DECORATIVE_PIPE_IDS) {
            models(profiles, id, "block/" + id);
        }
        return Map.copyOf(profiles);
    }

    private static void models(Map<String, ModelProfile> profiles, String id, String... modelPaths) {
        profiles.put(id, new ModelProfile(
                java.util.Arrays.stream(modelPaths)
                        .map(path -> ModelResourceLocation.standalone(ReinhardtsHBM.id(path)))
                        .toList()
        ));
    }

    private record ModelProfile(List<ModelResourceLocation> models) {
    }

    private record DedicatedItemRender(List<BakedModel> models, BlockState state, ItemDisplayContext context,
                                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                       int packedOverlay) {
    }

}
