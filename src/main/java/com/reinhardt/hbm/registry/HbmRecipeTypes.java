package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.recipe.ArcWelderRecipe;
import com.reinhardt.hbm.recipe.ArcFurnaceRecipe;
import com.reinhardt.hbm.recipe.AmmoPressRecipe;
import com.reinhardt.hbm.recipe.BlastFurnaceFuelRecipe;
import com.reinhardt.hbm.recipe.BlastFurnaceRecipe;
import com.reinhardt.hbm.recipe.BdclRecipe;
import com.reinhardt.hbm.recipe.BreederReactorRecipe;
import com.reinhardt.hbm.recipe.CargoShellRecipe;
import com.reinhardt.hbm.recipe.CentrifugeRecipe;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
import com.reinhardt.hbm.recipe.CokerRecipe;
import com.reinhardt.hbm.recipe.CombinationOvenRecipe;
import com.reinhardt.hbm.recipe.CompressorRecipe;
import com.reinhardt.hbm.recipe.ConveyorExpressRecipe;
import com.reinhardt.hbm.recipe.CrackingRecipe;
import com.reinhardt.hbm.recipe.CrystallizerRecipe;
import com.reinhardt.hbm.recipe.CrucibleRecipe;
import com.reinhardt.hbm.recipe.CyclotronRecipe;
import com.reinhardt.hbm.recipe.ElectrolyzerFluidRecipe;
import com.reinhardt.hbm.recipe.ElectrolyzerMetalRecipe;
import com.reinhardt.hbm.recipe.ExposureChamberRecipe;
import com.reinhardt.hbm.recipe.FractionTowerRecipe;
import com.reinhardt.hbm.recipe.FluidDuctRetypeRecipe;
import com.reinhardt.hbm.recipe.FuelPoolRecipe;
import com.reinhardt.hbm.recipe.FusionBreederFluidRecipe;
import com.reinhardt.hbm.recipe.FusionBreederItemRecipe;
import com.reinhardt.hbm.recipe.FusionRecipe;
import com.reinhardt.hbm.recipe.GasCentrifugeRecipe;
import com.reinhardt.hbm.recipe.LiquefactionRecipe;
import com.reinhardt.hbm.recipe.LubricantCanisterRecipe;
import com.reinhardt.hbm.recipe.MixerRecipe;
import com.reinhardt.hbm.recipe.ParticleAcceleratorRecipe;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.recipe.PressRecipe;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
import com.reinhardt.hbm.recipe.PyroOvenRecipe;
import com.reinhardt.hbm.recipe.PurexRecipe;
import com.reinhardt.hbm.recipe.RefineryRecipe;
import com.reinhardt.hbm.recipe.ReformingRecipe;
import com.reinhardt.hbm.recipe.HydrotreatingRecipe;
import com.reinhardt.hbm.recipe.RbmkOutgasserRecipe;
import com.reinhardt.hbm.recipe.RotaryFurnaceRecipe;
import com.reinhardt.hbm.recipe.ShredderRecipe;
import com.reinhardt.hbm.recipe.SilexRecipe;
import com.reinhardt.hbm.recipe.SolderingStationRecipe;
import com.reinhardt.hbm.recipe.SolidificationRecipe;
import com.reinhardt.hbm.recipe.StorageCrateUpgradeRecipe;
import com.reinhardt.hbm.recipe.VacuumDistillRecipe;
import com.reinhardt.hbm.recipe.UniversalGrenadeRecipe;
import com.reinhardt.hbm.recipe.ingredient.FoundryMaterialIngredient;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class HbmRecipeTypes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(BuiltInRegistries.RECIPE_TYPE, ReinhardtsHBM.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, ReinhardtsHBM.MOD_ID);
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, ReinhardtsHBM.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<FoundryMaterialIngredient>> FOUNDRY_MATERIAL_INGREDIENT =
            INGREDIENT_TYPES.register("foundry_material", () -> new IngredientType<>(FoundryMaterialIngredient.CODEC));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ShredderRecipe>> SHREDDER =
            RECIPE_TYPES.register("shredder", () -> RecipeType.simple(ReinhardtsHBM.id("shredder")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<AssemblyMachineRecipe>> ASSEMBLY_MACHINE =
            RECIPE_TYPES.register("assembly_machine", () -> RecipeType.simple(ReinhardtsHBM.id("assembly_machine")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PrecisionAssemblerRecipe>> PRECISION_ASSEMBLER =
            RECIPE_TYPES.register("precision_assembler", () -> RecipeType.simple(ReinhardtsHBM.id("precision_assembler")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ChemicalPlantRecipe>> CHEMICAL_PLANT =
            RECIPE_TYPES.register("chemical_plant", () -> RecipeType.simple(ReinhardtsHBM.id("chemical_plant")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RefineryRecipe>> REFINERY =
            RECIPE_TYPES.register("refinery", () -> RecipeType.simple(ReinhardtsHBM.id("refinery")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<VacuumDistillRecipe>> VACUUM_DISTILL =
            RECIPE_TYPES.register("vacuum_distill", () -> RecipeType.simple(ReinhardtsHBM.id("vacuum_distill")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CokerRecipe>> COKER =
            RECIPE_TYPES.register("coking", () -> RecipeType.simple(ReinhardtsHBM.id("coking")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CombinationOvenRecipe>> COMBINATION_OVEN =
            RECIPE_TYPES.register("combination_oven", () -> RecipeType.simple(ReinhardtsHBM.id("combination_oven")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RotaryFurnaceRecipe>> ROTARY_FURNACE =
            RECIPE_TYPES.register("rotary_furnace", () -> RecipeType.simple(ReinhardtsHBM.id("rotary_furnace")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SolidificationRecipe>> SOLIDIFICATION =
            RECIPE_TYPES.register("solidification", () -> RecipeType.simple(ReinhardtsHBM.id("solidification")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<LiquefactionRecipe>> LIQUEFACTION =
            RECIPE_TYPES.register("liquefaction", () -> RecipeType.simple(ReinhardtsHBM.id("liquefaction")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FractionTowerRecipe>> FRACTION_TOWER =
            RECIPE_TYPES.register("fraction_tower", () -> RecipeType.simple(ReinhardtsHBM.id("fraction_tower")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrackingRecipe>> CRACKING =
            RECIPE_TYPES.register("cracking", () -> RecipeType.simple(ReinhardtsHBM.id("cracking")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ReformingRecipe>> REFORMING =
            RECIPE_TYPES.register("reforming", () -> RecipeType.simple(ReinhardtsHBM.id("reforming")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<HydrotreatingRecipe>> HYDROTREATING =
            RECIPE_TYPES.register("hydrotreating", () -> RecipeType.simple(ReinhardtsHBM.id("hydrotreating")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SolderingStationRecipe>> SOLDERING_STATION =
            RECIPE_TYPES.register("soldering_station", () -> RecipeType.simple(ReinhardtsHBM.id("soldering_station")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ArcWelderRecipe>> ARC_WELDER =
            RECIPE_TYPES.register("arc_welder", () -> RecipeType.simple(ReinhardtsHBM.id("arc_welder")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ArcFurnaceRecipe>> ARC_FURNACE =
            RECIPE_TYPES.register("arc_furnace", () -> RecipeType.simple(ReinhardtsHBM.id("arc_furnace")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CompressorRecipe>> COMPRESSOR =
            RECIPE_TYPES.register("compressor", () -> RecipeType.simple(ReinhardtsHBM.id("compressor")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<MixerRecipe>> MIXER =
            RECIPE_TYPES.register("mixer", () -> RecipeType.simple(ReinhardtsHBM.id("mixer")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PressRecipe>> PRESS =
            RECIPE_TYPES.register("press", () -> RecipeType.simple(ReinhardtsHBM.id("press")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PyroOvenRecipe>> PYRO_OVEN =
            RECIPE_TYPES.register("pyrolysis", () -> RecipeType.simple(ReinhardtsHBM.id("pyrolysis")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<AmmoPressRecipe>> AMMO_PRESS =
            RECIPE_TYPES.register("ammo_press", () -> RecipeType.simple(ReinhardtsHBM.id("ammo_press")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CentrifugeRecipe>> CENTRIFUGE =
            RECIPE_TYPES.register("centrifuge", () -> RecipeType.simple(ReinhardtsHBM.id("centrifuge")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<GasCentrifugeRecipe>> GAS_CENTRIFUGE =
            RECIPE_TYPES.register("gas_centrifuge", () -> RecipeType.simple(ReinhardtsHBM.id("gas_centrifuge")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrystallizerRecipe>> CRYSTALLIZER =
            RECIPE_TYPES.register("crystallizer", () -> RecipeType.simple(ReinhardtsHBM.id("crystallizer")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CyclotronRecipe>> CYCLOTRON =
            RECIPE_TYPES.register("cyclotron", () -> RecipeType.simple(ReinhardtsHBM.id("cyclotron")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ExposureChamberRecipe>> EXPOSURE_CHAMBER =
            RECIPE_TYPES.register("exposure_chamber", () -> RecipeType.simple(ReinhardtsHBM.id("exposure_chamber")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ParticleAcceleratorRecipe>> PARTICLE_ACCELERATOR =
            RECIPE_TYPES.register("particle_accelerator", () -> RecipeType.simple(ReinhardtsHBM.id("particle_accelerator")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<SilexRecipe>> SILEX =
            RECIPE_TYPES.register("silex", () -> RecipeType.simple(ReinhardtsHBM.id("silex")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ElectrolyzerFluidRecipe>> ELECTROLYZER_FLUID =
            RECIPE_TYPES.register("electrolyzer_fluid", () -> RecipeType.simple(ReinhardtsHBM.id("electrolyzer_fluid")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<ElectrolyzerMetalRecipe>> ELECTROLYZER_METAL =
            RECIPE_TYPES.register("electrolyzer_metal", () -> RecipeType.simple(ReinhardtsHBM.id("electrolyzer_metal")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BreederReactorRecipe>> BREEDER_REACTOR =
            RECIPE_TYPES.register("breeder_reactor", () -> RecipeType.simple(ReinhardtsHBM.id("breeder_reactor")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FuelPoolRecipe>> FUEL_POOL =
            RECIPE_TYPES.register("fuel_pool", () -> RecipeType.simple(ReinhardtsHBM.id("fuel_pool")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PurexRecipe>> PUREX =
            RECIPE_TYPES.register("purex", () -> RecipeType.simple(ReinhardtsHBM.id("purex")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionRecipe>> FUSION =
            RECIPE_TYPES.register("fusion", () -> RecipeType.simple(ReinhardtsHBM.id("fusion")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<PlasmaForgeRecipe>> PLASMA_FORGE =
            RECIPE_TYPES.register("plasma_forge", () -> RecipeType.simple(ReinhardtsHBM.id("plasma_forge")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionBreederFluidRecipe>> FUSION_BREEDER_FLUID =
            RECIPE_TYPES.register("fusion_breeder_fluid", () -> RecipeType.simple(ReinhardtsHBM.id("fusion_breeder_fluid")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<FusionBreederItemRecipe>> FUSION_BREEDER_ITEM =
            RECIPE_TYPES.register("fusion_breeder_item", () -> RecipeType.simple(ReinhardtsHBM.id("fusion_breeder_item")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<RbmkOutgasserRecipe>> RBMK_OUTGASSER =
            RECIPE_TYPES.register("rbmk_outgasser", () -> RecipeType.simple(ReinhardtsHBM.id("rbmk_outgasser")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrucibleRecipe>> CRUCIBLE =
            RECIPE_TYPES.register("crucible", () -> RecipeType.simple(ReinhardtsHBM.id("crucible")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlastFurnaceRecipe>> BLAST_FURNACE =
            RECIPE_TYPES.register("blast_furnace", () -> RecipeType.simple(ReinhardtsHBM.id("blast_furnace")));

    public static final DeferredHolder<RecipeType<?>, RecipeType<BlastFurnaceFuelRecipe>> BLAST_FURNACE_FUEL =
            RECIPE_TYPES.register("blast_furnace_fuel", () -> RecipeType.simple(ReinhardtsHBM.id("blast_furnace_fuel")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShredderRecipe>> SHREDDER_SERIALIZER =
            RECIPE_SERIALIZERS.register("shredder", ShredderRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AssemblyMachineRecipe>> ASSEMBLY_MACHINE_SERIALIZER =
            RECIPE_SERIALIZERS.register("assembly_machine", AssemblyMachineRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PrecisionAssemblerRecipe>> PRECISION_ASSEMBLER_SERIALIZER =
            RECIPE_SERIALIZERS.register("precision_assembler", PrecisionAssemblerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ChemicalPlantRecipe>> CHEMICAL_PLANT_SERIALIZER =
            RECIPE_SERIALIZERS.register("chemical_plant", ChemicalPlantRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RefineryRecipe>> REFINERY_SERIALIZER =
            RECIPE_SERIALIZERS.register("refinery", RefineryRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<VacuumDistillRecipe>> VACUUM_DISTILL_SERIALIZER =
            RECIPE_SERIALIZERS.register("vacuum_distill", VacuumDistillRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CokerRecipe>> COKER_SERIALIZER =
            RECIPE_SERIALIZERS.register("coking", CokerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CombinationOvenRecipe>> COMBINATION_OVEN_SERIALIZER =
            RECIPE_SERIALIZERS.register("combination_oven", CombinationOvenRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RotaryFurnaceRecipe>> ROTARY_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("rotary_furnace", RotaryFurnaceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolidificationRecipe>> SOLIDIFICATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("solidification", SolidificationRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LiquefactionRecipe>> LIQUEFACTION_SERIALIZER =
            RECIPE_SERIALIZERS.register("liquefaction", LiquefactionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FractionTowerRecipe>> FRACTION_TOWER_SERIALIZER =
            RECIPE_SERIALIZERS.register("fraction_tower", FractionTowerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrackingRecipe>> CRACKING_SERIALIZER =
            RECIPE_SERIALIZERS.register("cracking", CrackingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BdclRecipe>> BDCL_SERIALIZER =
            RECIPE_SERIALIZERS.register("bdcl", BdclRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReformingRecipe>> REFORMING_SERIALIZER =
            RECIPE_SERIALIZERS.register("reforming", ReformingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<HydrotreatingRecipe>> HYDROTREATING_SERIALIZER =
            RECIPE_SERIALIZERS.register("hydrotreating", HydrotreatingRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SolderingStationRecipe>> SOLDERING_STATION_SERIALIZER =
            RECIPE_SERIALIZERS.register("soldering_station", SolderingStationRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArcWelderRecipe>> ARC_WELDER_SERIALIZER =
            RECIPE_SERIALIZERS.register("arc_welder", ArcWelderRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ArcFurnaceRecipe>> ARC_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("arc_furnace", ArcFurnaceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CompressorRecipe>> COMPRESSOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("compressor", CompressorRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MixerRecipe>> MIXER_SERIALIZER =
            RECIPE_SERIALIZERS.register("mixer", MixerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PressRecipe>> PRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("press", PressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PyroOvenRecipe>> PYRO_OVEN_SERIALIZER =
            RECIPE_SERIALIZERS.register("pyrolysis", PyroOvenRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AmmoPressRecipe>> AMMO_PRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("ammo_press", AmmoPressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CentrifugeRecipe>> CENTRIFUGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("centrifuge", CentrifugeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GasCentrifugeRecipe>> GAS_CENTRIFUGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("gas_centrifuge", GasCentrifugeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrystallizerRecipe>> CRYSTALLIZER_SERIALIZER =
            RECIPE_SERIALIZERS.register("crystallizer", CrystallizerRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CyclotronRecipe>> CYCLOTRON_SERIALIZER =
            RECIPE_SERIALIZERS.register("cyclotron", CyclotronRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ExposureChamberRecipe>> EXPOSURE_CHAMBER_SERIALIZER =
            RECIPE_SERIALIZERS.register("exposure_chamber", ExposureChamberRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ParticleAcceleratorRecipe>> PARTICLE_ACCELERATOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("particle_accelerator", ParticleAcceleratorRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SilexRecipe>> SILEX_SERIALIZER =
            RECIPE_SERIALIZERS.register("silex", SilexRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElectrolyzerFluidRecipe>> ELECTROLYZER_FLUID_SERIALIZER =
            RECIPE_SERIALIZERS.register("electrolyzer_fluid", ElectrolyzerFluidRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ElectrolyzerMetalRecipe>> ELECTROLYZER_METAL_SERIALIZER =
            RECIPE_SERIALIZERS.register("electrolyzer_metal", ElectrolyzerMetalRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BreederReactorRecipe>> BREEDER_REACTOR_SERIALIZER =
            RECIPE_SERIALIZERS.register("breeder_reactor", BreederReactorRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FuelPoolRecipe>> FUEL_POOL_SERIALIZER =
            RECIPE_SERIALIZERS.register("fuel_pool", FuelPoolRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PurexRecipe>> PUREX_SERIALIZER =
            RECIPE_SERIALIZERS.register("purex", PurexRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionRecipe>> FUSION_SERIALIZER =
            RECIPE_SERIALIZERS.register("fusion", FusionRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PlasmaForgeRecipe>> PLASMA_FORGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("plasma_forge", PlasmaForgeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionBreederFluidRecipe>> FUSION_BREEDER_FLUID_SERIALIZER =
            RECIPE_SERIALIZERS.register("fusion_breeder_fluid", FusionBreederFluidRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FusionBreederItemRecipe>> FUSION_BREEDER_ITEM_SERIALIZER =
            RECIPE_SERIALIZERS.register("fusion_breeder_item", FusionBreederItemRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<RbmkOutgasserRecipe>> RBMK_OUTGASSER_SERIALIZER =
            RECIPE_SERIALIZERS.register("rbmk_outgasser", RbmkOutgasserRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrucibleRecipe>> CRUCIBLE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crucible", CrucibleRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastFurnaceRecipe>> BLAST_FURNACE_SERIALIZER =
            RECIPE_SERIALIZERS.register("blast_furnace", BlastFurnaceRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BlastFurnaceFuelRecipe>> BLAST_FURNACE_FUEL_SERIALIZER =
            RECIPE_SERIALIZERS.register("blast_furnace_fuel", BlastFurnaceFuelRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidDuctRetypeRecipe>> FLUID_DUCT_RETYPE_SERIALIZER =
            RECIPE_SERIALIZERS.register("fluid_duct_retype", FluidDuctRetypeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LubricantCanisterRecipe>> LUBRICANT_CANISTER_SERIALIZER =
            RECIPE_SERIALIZERS.register("lubricant_canister", LubricantCanisterRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ConveyorExpressRecipe>> CONVEYOR_EXPRESS_SERIALIZER =
            RECIPE_SERIALIZERS.register("conveyor_express", ConveyorExpressRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CargoShellRecipe>> CARGO_SHELL_SERIALIZER =
            RECIPE_SERIALIZERS.register("cargo_shell", CargoShellRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UniversalGrenadeRecipe>> UNIVERSAL_GRENADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("universal_grenade", UniversalGrenadeRecipe.Serializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> CRATE_DESH_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crate_desh_upgrade", StorageCrateUpgradeRecipe.DeshSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> CRATE_TUNGSTEN_UPGRADE_SERIALIZER =
            RECIPE_SERIALIZERS.register("crate_tungsten_upgrade", StorageCrateUpgradeRecipe.TungstenSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> SAFE_SERIALIZER =
            RECIPE_SERIALIZERS.register("safe", StorageCrateUpgradeRecipe.SafeSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> MASS_STORAGE_SERIALIZER =
            RECIPE_SERIALIZERS.register("mass_storage", StorageCrateUpgradeRecipe.MassStorageSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> MASS_STORAGE_DESH_SERIALIZER =
            RECIPE_SERIALIZERS.register("mass_storage_desh", StorageCrateUpgradeRecipe.MassStorageDeshSerializer::new);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<StorageCrateUpgradeRecipe>> MASS_STORAGE_RESISTANT_SERIALIZER =
            RECIPE_SERIALIZERS.register("mass_storage_resistant", StorageCrateUpgradeRecipe.MassStorageResistantSerializer::new);

    private HbmRecipeTypes() {
    }

    public static void register(IEventBus eventBus) {
        RECIPE_TYPES.register(eventBus);
        RECIPE_SERIALIZERS.register(eventBus);
        INGREDIENT_TYPES.register(eventBus);
    }
}
