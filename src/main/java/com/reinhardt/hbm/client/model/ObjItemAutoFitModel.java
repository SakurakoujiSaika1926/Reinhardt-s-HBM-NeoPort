package com.reinhardt.hbm.client.model;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.render.ObjMachineItemRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import java.util.Set;

public final class ObjItemAutoFitModel implements IDynamicBakedModel {
    private static final float DEFAULT_TARGET_SIZE = 0.95F;

    /*
     * These models inherit an explicit item transform from their block JSON.
     * Their OBJ coordinates are already authored for the old inventory pose;
     * fitting the vertices as well applies the scale twice and makes the item
     * appear tiny or displaced.
     */
    private static final Set<String> EXPLICIT_ITEM_TRANSFORMS = Set.of(
            "anvil_arsenic_bronze", "anvil_bismuth_bronze", "anvil_desh", "anvil_dnt",
            "anvil_ferrouranium", "anvil_iron", "anvil_lead", "anvil_murky",
            "anvil_osmiridium", "anvil_saturnite", "anvil_schrabidate", "anvil_steel",
            "chimney_brick", "chimney_industrial", "dfc_core", "foundry_basin",
            "foundry_outlet", "foundry_slagtap", "furnace_combination", "machine_boiler",
            "heater_electric", "heater_firebox", "heater_heatex", "heater_oilburner",
            "heater_oven", "machine_ammo_press", "machine_ashpit", "machine_assembly_machine",
            "machine_battery_redd", "machine_centrifuge", "machine_chemical_plant",
            "battery_pack_battery_lead", "battery_pack_battery_lithium",
            "battery_pack_battery_quantum", "battery_pack_battery_redstone",
            "battery_pack_battery_schrabidium", "battery_pack_battery_sodium",
            "battery_pack_capacitor_bismuth", "battery_pack_capacitor_copper",
            "battery_pack_capacitor_gold", "battery_pack_capacitor_niobium",
            "battery_pack_capacitor_spark", "battery_pack_capacitor_tantalum",
            "machine_chungus", "machine_combustion_engine", "machine_condenser_powered",
            "machine_crucible", "machine_diesel", "machine_epress", "machine_fracking_tower",
            "machine_gascent", "machine_industrial_boiler",
            "machine_industrial_turbine", "machine_intake", "machine_large_turbine",
            "machine_press", "machine_reactor_breeding", "machine_solar_boiler",
            "machine_soldering_station", "machine_steam_engine", "machine_stirling",
            "machine_stirling_creative", "machine_stirling_steel", "machine_tower_large",
            "machine_tower_small", "machine_turbinegas", "machine_well", "machine_wood_burner",
            "solar_mirror", "zirnox_destroyed",
            // Every generic HBM door has its legacy ItemRenderLibrary transform
            // encoded in the item model JSON. Do not fit these OBJ vertices again:
            // that would apply a second scale/centering pass and corrupt the item pose.
            "fire_door", "sliding_blast_door", "sliding_blast_door_2",
            "sliding_gate_door", "qe_sliding_door", "qe_containment",
            "sliding_seal_door", "secure_access_door", "round_airlock_door",
            "large_vehicle_door", "vault_door", "water_door", "silo_hatch", "silo_hatch_large"
    );

    /* These items render their complete legacy OBJ assembly through BEWLR. */
    private static final Set<String> CUSTOM_RENDERED_ITEMS = Set.of(
            "machine_annihilator", "machine_autosaw", "machine_forcefield", "machine_microwave",
            "machine_missile_assembly", "machine_orbus", "machine_precass", "machine_pyrooven",
            "machine_radar", "machine_radar_large", "machine_radgen", "machine_radiolysis",
            "machine_rtg_grey", "machine_sawmill", "machine_turbofan", "machine_thresher", "machine_lpw2",
            "pump_steam", "pump_electric", "machine_bat9000", "machine_bigasstank",
            // These decorative items are rendered through their explicit
            // legacy BEWLR paths; never run their inventory marker through
            // the generic bounds auto-fit pass first.
            "deco_toaster", "deco_crt",
            "transition_seal",
            "red_connector", "red_connector_super", "red_pylon_medium_wood",
            "red_pylon_medium_wood_transformer", "red_pylon_medium_steel", "red_pylon_medium_steel_transformer",
            "red_pylon_large", "substation"
    );

    private final BakedModel delegate;
    private final Fit fit;
    private final Map<Direction, List<BakedQuad>> sideQuads;
    private final List<BakedQuad> unculledQuads;

    private ObjItemAutoFitModel(BakedModel delegate, ModelResourceLocation location) {
        this.delegate = delegate;
        this.fit = Fit.from(delegate, targetSize(location));
        if (this.fit.valid()) {
            this.unculledQuads = transform(this.delegate.getQuads(null, null, RandomSource.create(42L), ModelData.EMPTY, null));
            EnumMap<Direction, List<BakedQuad>> bakedSides = new EnumMap<>(Direction.class);
            for (Direction direction : Direction.values()) {
                bakedSides.put(direction, transform(this.delegate.getQuads(null, direction, RandomSource.create(42L), ModelData.EMPTY, null)));
            }
            this.sideQuads = Map.copyOf(bakedSides);
        } else {
            this.unculledQuads = List.of();
            this.sideQuads = Map.of();
        }
    }

    public static void replaceModels(Map<ModelResourceLocation, BakedModel> models) {
        int replaced = 0;
        for (ModelResourceLocation location : List.copyOf(models.keySet())) {
            if (!shouldWrap(location)) {
                continue;
            }
            BakedModel model = models.get(location);
            if (model != null && !(model instanceof ObjItemAutoFitModel)) {
                models.put(location, new ObjItemAutoFitModel(model, location));
                replaced++;
            }
        }
        if (replaced > 0) {
            ReinhardtsHBM.LOGGER.info("Installed auto-fit item transforms for {} OBJ machine item models", replaced);
        }
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        if (!this.fit.valid()) {
            return this.delegate.getQuads(state, side, rand, extraData, renderType);
        }
        if (side == null) {
            return this.unculledQuads;
        }
        return this.sideQuads.getOrDefault(side, List.of());
    }

    @Override
    public boolean useAmbientOcclusion() {
        return this.delegate.useAmbientOcclusion();
    }

    @Override
    public boolean isGui3d() {
        return this.delegate.isGui3d();
    }

    @Override
    public boolean usesBlockLight() {
        return this.delegate.usesBlockLight();
    }

    @Override
    public boolean isCustomRenderer() {
        return this.delegate.isCustomRenderer();
    }

    @Override
    public TextureAtlasSprite getParticleIcon() {
        return this.delegate.getParticleIcon();
    }

    @Override
    public ItemTransforms getTransforms() {
        // The legacy item JSON files carry machine-specific transforms.  Auto-fitting
        // the OBJ vertices must not replace those transforms with the block defaults.
        return this.delegate.getTransforms();
    }

    @Override
    public ItemOverrides getOverrides() {
        return this.delegate.getOverrides();
    }

    private List<BakedQuad> transform(List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return quads;
        }
        return quads.stream().map(this::transform).toList();
    }

    private BakedQuad transform(BakedQuad quad) {
        int[] vertices = quad.getVertices().clone();
        int stride = vertices.length / 4;
        for (int vertex = 0; vertex < 4; vertex++) {
            int offset = vertex * stride;
            float x = Float.intBitsToFloat(vertices[offset]);
            float y = Float.intBitsToFloat(vertices[offset + 1]);
            float z = Float.intBitsToFloat(vertices[offset + 2]);
            vertices[offset] = Float.floatToRawIntBits(this.fit.transformX(x));
            vertices[offset + 1] = Float.floatToRawIntBits(this.fit.transformY(y));
            vertices[offset + 2] = Float.floatToRawIntBits(this.fit.transformZ(z));
        }
        return new BakedQuad(vertices, quad.getTintIndex(), quad.getDirection(), quad.getSprite(), quad.isShade());
    }

    private static boolean shouldWrap(ModelResourceLocation location) {
        if (!ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()) || !ReinhardtsHBM.MOD_ID.equals(location.id().getNamespace())) {
            return false;
        }
        String path = location.id().getPath();
        if (EXPLICIT_ITEM_TRANSFORMS.contains(path)
                || CUSTOM_RENDERED_ITEMS.contains(path)
                || DedicatedItemRendererModel.owns(path)) {
            return false;
        }
        // OBJ machine items have a BEWLR that renders their complete legacy
        // assembly. Wrapping their inventory model here would make the later
        // OBJ route skip them and leave the flat fallback icon active.
        if (ObjMachineItemRenderer.itemIds().contains(path)) {
            return false;
        }
        return path.equals("machine_rtg_grey")
                || path.startsWith("heater_")
                || path.equals("machine_wood_burner")
                || path.equals("machine_combustion_engine")
                || path.equals("machine_flare")
                || path.equals("machine_electrolyser")
                || path.equals("machine_boiler")
                || path.equals("machine_centrifuge")
                || path.equals("machine_gascent")
                || path.equals("machine_crystallizer")
                || path.equals("machine_cyclotron")
                || path.equals("machine_exposure_chamber")
                || path.equals("machine_deuterium_tower")
                || path.equals("machine_hephaestus")
                || path.equals("machine_silex")
                || path.equals("machine_fel")
                || path.startsWith("battery_pack_")
                || path.equals("pa_source")
                || path.equals("pa_beamline")
                || path.equals("pa_rfc")
                || path.equals("pa_quadrupole")
                || path.equals("pa_dipole")
                || path.equals("pa_detector")
                || path.equals("dfc_core")
                || path.equals("dfc_emitter")
                || path.equals("dfc_receiver")
                || path.equals("dfc_injector")
                || path.equals("dfc_stabilizer")
                || path.equals("machine_reactor_small")
                || path.equals("machine_reactor_breeding")
                || path.equals("nuke_boy")
                || path.equals("compact_launcher")
                || path.equals("launch_pad")
                || path.equals("launch_pad_large")
                || path.equals("launch_pad_rusted")
                || path.equals("launch_table")
                || path.equals("soyuz_capsule")
                || path.equals("watz")
                || path.equals("watz_pump")
                || path.equals("zirnox_destroyed")
                || path.equals("fusion_torus")
                || path.equals("fusion_klystron")
                || path.equals("fusion_klystron_creative")
                || path.equals("fusion_boiler")
                || path.equals("fusion_breeder")
                || path.equals("fusion_collector")
                || path.equals("fusion_mhdt")
                || path.equals("fusion_coupler")
                || path.equals("fusion_plasma_forge")
                || path.equals("machine_assembly_machine")
                || path.equals("machine_chemical_plant")
                || path.equals("machine_soldering_station")
                || path.equals("machine_arc_furnace")
                || path.equals("machine_storage_drum")
                || path.equals("machine_compressor")
                || path.equals("machine_compressor_compact")
                || path.equals("machine_mixer")
                || path.equals("machine_tower_small")
                || path.equals("machine_tower_large")
                || path.equals("machine_crucible")
                || path.equals("machine_strand_caster")
                || path.equals("foundry_basin")
                || path.equals("barrel_plastic")
                || path.equals("barrel_corroded")
                || path.equals("barrel_steel")
                || path.equals("barrel_tcalloy")
                || path.equals("barrel_antimatter")
                || path.equals("machine_battery_redd")
                || path.equals("machine_battery_socket")
                || path.equals("machine_press")
                || path.equals("machine_epress")
                || path.equals("pump_steam")
                || path.equals("pump_electric")
                || path.equals("machine_large_turbine")
                || path.equals("machine_chungus")
                || path.equals("machine_turbinegas")
                || path.equals("machine_condenser_powered")
                || path.equals("machine_well")
                || path.equals("machine_pumpjack")
                || path.equals("machine_fracking_tower")
                || path.equals("machine_refinery")
                || path.equals("machine_vacuum_distill")
                || path.equals("machine_industrial_boiler")
                || path.equals("machine_coker")
                || path.equals("machine_intake")
                || path.equals("furnace_combination")
                || path.equals("machine_solidifier")
                || path.equals("machine_liquefactor")
                || path.equals("machine_blast_furnace")
                || path.equals("machine_excavator")
                || path.equals("machine_ore_slopper")
                || path.equals("furnace_iron")
                || path.equals("furnace_steel")
                || path.equals("machine_solar_boiler")
                || path.equals("solar_mirror")
                || path.equals("machine_fraction_tower")
                || path.equals("fraction_spacer")
                || path.equals("machine_catalytic_cracker")
                || path.equals("machine_catalytic_reformer")
                || path.equals("machine_hydrotreater")
                || path.equals("chimney_brick")
                || path.equals("chimney_industrial")
                || path.equals("red_connector")
                || path.equals("red_connector_super")
                || path.equals("red_pylon_medium_wood")
                || path.equals("red_pylon_medium_wood_transformer")
                || path.equals("red_pylon_medium_steel")
                || path.equals("red_pylon_medium_steel_transformer")
                || path.equals("red_pylon_large")
                || path.equals("substation")
                || path.equals("fire_door")
                || path.equals("sliding_blast_door")
                || path.equals("sliding_blast_door_2")
                || path.equals("sliding_gate_door")
                || path.equals("qe_sliding_door")
                || path.equals("qe_containment")
                || path.equals("sliding_seal_door")
                || path.equals("secure_access_door")
                || path.equals("round_airlock_door")
                || path.equals("large_vehicle_door")
                || path.equals("vault_door")
                || path.equals("water_door")
                || path.equals("silo_hatch")
                || path.equals("silo_hatch_large")
                ;
    }

    private record Fit(boolean valid, float centerX, float centerY, float centerZ, float scale) {
        private static Fit from(BakedModel model, float targetSize) {
            Bounds bounds = new Bounds();
            RandomSource random = RandomSource.create(42L);
            bounds.add(model.getQuads(null, null, random));
            for (Direction side : Direction.values()) {
                random.setSeed(42L);
                bounds.add(model.getQuads(null, side, random));
            }
            if (!bounds.valid()) {
                return invalid();
            }
            float sizeX = bounds.maxX - bounds.minX;
            float sizeY = bounds.maxY - bounds.minY;
            float sizeZ = bounds.maxZ - bounds.minZ;
            float largest = Math.max(sizeX, Math.max(sizeY, sizeZ));
            if (largest <= 0.0001F) {
                return invalid();
            }
            return new Fit(
                    true,
                    (bounds.minX + bounds.maxX) * 0.5F,
                    (bounds.minY + bounds.maxY) * 0.5F,
                    (bounds.minZ + bounds.maxZ) * 0.5F,
                    targetSize / largest
            );
        }

        private static Fit invalid() {
            return new Fit(false, 0.5F, 0.5F, 0.5F, 1.0F);
        }

        private float transformX(float x) {
            return (x - this.centerX) * this.scale + 0.5F;
        }

        private float transformY(float y) {
            return (y - this.centerY) * this.scale + 0.5F;
        }

        private float transformZ(float z) {
            return (z - this.centerZ) * this.scale + 0.5F;
        }
    }

    private static float targetSize(ModelResourceLocation location) {
        if (location == null) {
            return DEFAULT_TARGET_SIZE;
        }
        if (location.id().getPath().startsWith("battery_pack_")) {
            return 0.85F;
        }
        return switch (location.id().getPath()) {
            case "machine_wood_burner" -> 1.12F;
            case "machine_intake" -> 1.08F;
            case "machine_combustion_engine" -> 1.18F;
            case "machine_flare" -> 1.12F;
            case "machine_electrolyser" -> 1.24F;
            case "machine_blast_furnace" -> 1.22F;
            case "machine_large_turbine" -> 1.25F;
            case "machine_chungus" -> 1.28F;
            case "machine_turbinegas" -> 1.35F;
            case "machine_condenser_powered" -> 1.18F;
            case "machine_vacuum_distill" -> 1.18F;
            case "machine_industrial_boiler" -> 1.18F;
            case "red_pylon_large" -> 1.18F;
            case "battery_pack_battery_lead", "battery_pack_battery_lithium",
                    "battery_pack_battery_quantum", "battery_pack_battery_redstone",
                    "battery_pack_battery_schrabidium", "battery_pack_battery_sodium" -> 0.92F;
            case "battery_pack_capacitor_bismuth", "battery_pack_capacitor_copper",
                    "battery_pack_capacitor_gold", "battery_pack_capacitor_niobium",
                    "battery_pack_capacitor_spark", "battery_pack_capacitor_tantalum" -> 0.92F;
            case "soyuz_capsule" -> 0.82F;
            case "machine_strand_caster" -> 1.05F;
            case "machine_battery_redd" -> 1.22F;
            case "machine_arc_furnace" -> 1.18F;
            case "machine_storage_drum" -> 1.00F;
            case "machine_compressor" -> 1.18F;
            case "machine_compressor_compact" -> 1.12F;
            case "machine_mixer" -> 1.08F;
            case "machine_deuterium_tower" -> 0.98F;
            case "machine_annihilator" -> 0.82F;
            case "machine_autosaw" -> 1.00F;
            case "machine_missile_assembly" -> 0.88F;
            case "machine_orbus" -> 0.72F;
            case "machine_precass" -> 0.92F;
            case "machine_pyrooven" -> 0.90F;
            case "machine_radar" -> 0.98F;
            case "machine_radar_large" -> 0.82F;
            case "machine_radgen" -> 0.86F;
            case "machine_radiolysis" -> 0.90F;
            case "machine_rtg_grey" -> 1.00F;
            case "machine_sawmill" -> 0.96F;
            case "machine_turbofan" -> 0.86F;
            case "machine_excavator" -> 1.12F;
            case "watz" -> 1.04F;
            case "watz_pump" -> 1.08F;
            case "fusion_torus" -> 0.92F;
            case "fusion_klystron", "fusion_klystron_creative" -> 1.02F;
            case "fusion_boiler" -> 1.02F;
            case "fusion_breeder", "fusion_collector" -> 0.98F;
            case "fusion_mhdt" -> 1.04F;
            case "fusion_coupler" -> 1.0F;
            case "fusion_plasma_forge" -> 0.96F;
            case "dfc_core" -> 0.78F;
            case "chimney_brick" -> 0.95F;
            case "chimney_industrial" -> 1.03F;
            case "sliding_gate_door", "qe_sliding_door", "sliding_seal_door" -> 1.05F;
            case "silo_hatch", "silo_hatch_large" -> 0.92F;
            case "large_vehicle_door" -> 1.08F;
            case "secure_access_door", "round_airlock_door", "vault_door", "water_door" -> 1.0F;
            default -> DEFAULT_TARGET_SIZE;
        };
    }

    private static final class Bounds {
        private float minX = Float.POSITIVE_INFINITY;
        private float minY = Float.POSITIVE_INFINITY;
        private float minZ = Float.POSITIVE_INFINITY;
        private float maxX = Float.NEGATIVE_INFINITY;
        private float maxY = Float.NEGATIVE_INFINITY;
        private float maxZ = Float.NEGATIVE_INFINITY;

        private void add(List<BakedQuad> quads) {
            for (BakedQuad quad : quads) {
                add(quad);
            }
        }

        private void add(BakedQuad quad) {
            int[] vertices = quad.getVertices();
            int stride = vertices.length / 4;
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * stride;
                add(
                        Float.intBitsToFloat(vertices[offset]),
                        Float.intBitsToFloat(vertices[offset + 1]),
                        Float.intBitsToFloat(vertices[offset + 2])
                );
            }
        }

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
