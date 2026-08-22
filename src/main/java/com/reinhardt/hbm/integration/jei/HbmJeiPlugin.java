package com.reinhardt.hbm.integration.jei;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.client.screen.ArcWelderScreen;
import com.reinhardt.hbm.client.screen.ArcFurnaceScreen;
import com.reinhardt.hbm.client.screen.AmmoPressScreen;
import com.reinhardt.hbm.client.screen.CompressorScreen;
import com.reinhardt.hbm.client.screen.CyclotronScreen;
import com.reinhardt.hbm.client.screen.ExposureChamberScreen;
import com.reinhardt.hbm.client.screen.FusionMachineScreen;
import com.reinhardt.hbm.client.screen.HydrotreaterScreen;
import com.reinhardt.hbm.client.screen.IronFurnaceScreen;
import com.reinhardt.hbm.client.screen.MixerScreen;
import com.reinhardt.hbm.client.screen.ParticleAcceleratorScreen;
import com.reinhardt.hbm.client.screen.PrecisionAssemblerScreen;
import com.reinhardt.hbm.client.screen.PyroOvenScreen;
import com.reinhardt.hbm.client.screen.SteelFurnaceScreen;
import com.reinhardt.hbm.recipe.ArcWelderRecipe;
import com.reinhardt.hbm.recipe.ArcFurnaceRecipe;
import com.reinhardt.hbm.recipe.AmmoPressRecipe;
import com.reinhardt.hbm.recipe.AssemblyMachineRecipe;
import com.reinhardt.hbm.recipe.BlastFurnaceFuelRecipe;
import com.reinhardt.hbm.recipe.BlastFurnaceRecipe;
import com.reinhardt.hbm.recipe.BreederReactorRecipe;
import com.reinhardt.hbm.recipe.CentrifugeRecipe;
import com.reinhardt.hbm.recipe.ChemicalPlantRecipe;
import com.reinhardt.hbm.recipe.CokerRecipe;
import com.reinhardt.hbm.recipe.CombinationOvenRecipe;
import com.reinhardt.hbm.recipe.CompressorRecipe;
import com.reinhardt.hbm.recipe.CrackingRecipe;
import com.reinhardt.hbm.recipe.CrystallizerRecipe;
import com.reinhardt.hbm.recipe.CrucibleRecipe;
import com.reinhardt.hbm.recipe.CyclotronRecipe;
import com.reinhardt.hbm.recipe.ElectrolyzerFluidRecipe;
import com.reinhardt.hbm.recipe.ElectrolyzerMetalRecipe;
import com.reinhardt.hbm.recipe.ExposureChamberRecipe;
import com.reinhardt.hbm.recipe.FractionTowerRecipe;
import com.reinhardt.hbm.recipe.FuelPoolRecipe;
import com.reinhardt.hbm.recipe.FusionBreederFluidRecipe;
import com.reinhardt.hbm.recipe.FusionRecipe;
import com.reinhardt.hbm.recipe.GasCentrifugeRecipe;
import com.reinhardt.hbm.recipe.LiquefactionRecipe;
import com.reinhardt.hbm.recipe.MixerRecipe;
import com.reinhardt.hbm.recipe.ParticleAcceleratorRecipe;
import com.reinhardt.hbm.recipe.PlasmaForgeRecipe;
import com.reinhardt.hbm.recipe.PrecisionAssemblerRecipe;
import com.reinhardt.hbm.recipe.PressRecipe;
import com.reinhardt.hbm.recipe.PyroOvenRecipe;
import com.reinhardt.hbm.recipe.PurexRecipe;
import com.reinhardt.hbm.recipe.RefineryRecipe;
import com.reinhardt.hbm.recipe.ReformingRecipe;
import com.reinhardt.hbm.recipe.HydrotreatingRecipe;
import com.reinhardt.hbm.recipe.RbmkOutgasserRecipe;
import com.reinhardt.hbm.recipe.RotaryFurnaceRecipe;
import com.reinhardt.hbm.recipe.ShredderRecipe;
import com.reinhardt.hbm.recipe.SilexRecipe;
import com.reinhardt.hbm.recipe.SolidificationRecipe;
import com.reinhardt.hbm.recipe.SolderingStationRecipe;
import com.reinhardt.hbm.recipe.VacuumDistillRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilConstructionRecipe;
import com.reinhardt.hbm.recipe.anvil.AnvilSmithingRecipe;
import com.reinhardt.hbm.recipe.anvil.HbmAnvilRecipes;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.registry.HbmRecipeTypes;
import com.reinhardt.hbm.util.Wavelength;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;

@JeiPlugin
public class HbmJeiPlugin implements IModPlugin {
    public static final RecipeType<RecipeHolder<ShredderRecipe>> SHREDDER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("shredder"));
    public static final RecipeType<SawmillJeiRecipe> SAWMILL =
            RecipeType.create(ReinhardtsHBM.MOD_ID, "sawmill", SawmillJeiRecipe.class);
    public static final RecipeType<RecipeHolder<AssemblyMachineRecipe>> ASSEMBLY_MACHINE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("assembly_machine"));
    public static final RecipeType<RecipeHolder<PrecisionAssemblerRecipe>> PRECISION_ASSEMBLER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("precision_assembler"));
    public static final RecipeType<RecipeHolder<ChemicalPlantRecipe>> CHEMICAL_PLANT =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("chemical_plant"));
    public static final RecipeType<RecipeHolder<SolderingStationRecipe>> SOLDERING_STATION =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("soldering_station"));
    public static final RecipeType<RecipeHolder<ArcWelderRecipe>> ARC_WELDER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("arc_welder"));
    public static final RecipeType<RecipeHolder<CompressorRecipe>> COMPRESSOR =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("compressor"));
    public static final RecipeType<RecipeHolder<MixerRecipe>> MIXER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("mixer"));
    public static final RecipeType<RecipeHolder<CrystallizerRecipe>> CRYSTALLIZER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("crystallizer"));
    public static final RecipeType<RecipeHolder<CyclotronRecipe>> CYCLOTRON =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("cyclotron"));
    public static final RecipeType<RecipeHolder<ExposureChamberRecipe>> EXPOSURE_CHAMBER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("exposure_chamber"));
    public static final RecipeType<RecipeHolder<ParticleAcceleratorRecipe>> PARTICLE_ACCELERATOR =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("particle_accelerator"));
    public static final RecipeType<RecipeHolder<SilexRecipe>> SILEX =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("silex"));
    public static final RecipeType<RecipeHolder<SilexRecipe>> SILEX_IR =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("silex_ir"));
    public static final RecipeType<RecipeHolder<SilexRecipe>> SILEX_VISIBLE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("silex_visible"));
    public static final RecipeType<RecipeHolder<SilexRecipe>> SILEX_UV =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("silex_uv"));
    public static final RecipeType<RecipeHolder<SilexRecipe>> SILEX_GAMMA =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("silex_gamma"));
    public static final RecipeType<RecipeHolder<SilexRecipe>> SILEX_DIGAMMA =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("silex_digamma"));
    public static final RecipeType<RecipeHolder<ElectrolyzerFluidRecipe>> ELECTROLYZER_FLUID =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("electrolyzer_fluid"));
    public static final RecipeType<RecipeHolder<ElectrolyzerMetalRecipe>> ELECTROLYZER_METAL =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("electrolyzer_metal"));
    public static final RecipeType<RecipeHolder<PressRecipe>> PRESS =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("press"));
    public static final RecipeType<RecipeHolder<PyroOvenRecipe>> PYRO_OVEN =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("pyrolysis"));
    public static final RecipeType<RecipeHolder<AmmoPressRecipe>> AMMO_PRESS =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("ammo_press"));
    public static final RecipeType<RecipeHolder<CentrifugeRecipe>> CENTRIFUGE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("centrifuge"));
    public static final RecipeType<RecipeHolder<GasCentrifugeRecipe>> GAS_CENTRIFUGE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("gas_centrifuge"));
    public static final RecipeType<RecipeHolder<BreederReactorRecipe>> BREEDER_REACTOR =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("breeder_reactor"));
    public static final RecipeType<RecipeHolder<FuelPoolRecipe>> FUEL_POOL =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("fuel_pool"));
    public static final RecipeType<StorageDrumJeiRecipe> STORAGE_DRUM =
            RecipeType.create(ReinhardtsHBM.MOD_ID, "storage_drum", StorageDrumJeiRecipe.class);
    public static final RecipeType<BoilingJeiRecipe> BOILING =
            RecipeType.create(ReinhardtsHBM.MOD_ID, "boiling", BoilingJeiRecipe.class);
    public static final RecipeType<RecipeHolder<PurexRecipe>> PUREX =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("purex"));
    public static final RecipeType<RecipeHolder<FusionRecipe>> FUSION =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("fusion"));
    public static final RecipeType<RecipeHolder<PlasmaForgeRecipe>> PLASMA_FORGE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("plasma_forge"));
    public static final RecipeType<RecipeHolder<FusionBreederFluidRecipe>> FUSION_BREEDER_FLUID =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("fusion_breeder_fluid"));
    public static final RecipeType<RecipeHolder<RbmkOutgasserRecipe>> RBMK_OUTGASSER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("rbmk_outgasser"));
    public static final RecipeType<RecipeHolder<CrucibleRecipe>> CRUCIBLE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("crucible"));
    public static final RecipeType<RecipeHolder<RefineryRecipe>> REFINERY =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("refinery"));
    public static final RecipeType<RecipeHolder<VacuumDistillRecipe>> VACUUM_DISTILL =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("vacuum_distill"));
    public static final RecipeType<RecipeHolder<CokerRecipe>> COKER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("coking"));
    public static final RecipeType<RecipeHolder<CombinationOvenRecipe>> COMBINATION_OVEN =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("combination_oven"));
    public static final RecipeType<RecipeHolder<RotaryFurnaceRecipe>> ROTARY_FURNACE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("rotary_furnace"));
    public static final RecipeType<RecipeHolder<ArcFurnaceRecipe>> ARC_FURNACE_SOLID =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("arc_furnace_solid"));
    public static final RecipeType<RecipeHolder<ArcFurnaceRecipe>> ARC_FURNACE_LIQUID =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("arc_furnace_liquid"));
    public static final RecipeType<RecipeHolder<SolidificationRecipe>> SOLIDIFICATION =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("solidification"));
    public static final RecipeType<RecipeHolder<LiquefactionRecipe>> LIQUEFACTION =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("liquefaction"));
    public static final RecipeType<RecipeHolder<FractionTowerRecipe>> FRACTION_TOWER =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("fraction_tower"));
    public static final RecipeType<RecipeHolder<CrackingRecipe>> CRACKING =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("cracking"));
    public static final RecipeType<RecipeHolder<ReformingRecipe>> REFORMING =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("reforming"));
    public static final RecipeType<RecipeHolder<HydrotreatingRecipe>> HYDROTREATING =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("hydrotreating"));
    public static final RecipeType<RecipeHolder<BlastFurnaceRecipe>> BLAST_FURNACE =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("blast_furnace"));
    public static final RecipeType<RecipeHolder<BlastFurnaceFuelRecipe>> BLAST_FURNACE_FUEL =
            RecipeType.createRecipeHolderType(ReinhardtsHBM.id("blast_furnace_fuel"));
    public static final RecipeType<FoundryCastingJeiRecipe> CRUCIBLE_CASTING =
            RecipeType.create(ReinhardtsHBM.MOD_ID, "crucible_foundry", FoundryCastingJeiRecipe.class);
    public static final RecipeType<AnvilConstructionRecipe> ANVIL_CONSTRUCTION =
            RecipeType.create(ReinhardtsHBM.MOD_ID, "anvil_construction", AnvilConstructionRecipe.class);
    public static final RecipeType<AnvilSmithingRecipe> ANVIL_SMITHING =
            RecipeType.create(ReinhardtsHBM.MOD_ID, "anvil_smithing", AnvilSmithingRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return ReinhardtsHBM.id("jei");
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime runtime) {
        runtime.getIngredientManager().removeIngredientsAtRuntime(
                VanillaTypes.ITEM_STACK,
                List.of(new ItemStack(HbmBlocks.MACHINE_LARGE_TURBINE.get()))
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(
                new ShredderRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SawmillRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AssemblyMachineRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PrecisionAssemblerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ChemicalPlantRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SolderingStationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ArcWelderRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CompressorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new MixerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AmmoPressRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrystallizerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CyclotronRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ExposureChamberRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ParticleAcceleratorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new SilexRecipeCategory(registration.getJeiHelpers().getGuiHelper(), SILEX, "container.reinhardtshbm.machine_silex"),
                new SilexRecipeCategory(registration.getJeiHelpers().getGuiHelper(), SILEX_IR, "jei.silexinfr"),
                new SilexRecipeCategory(registration.getJeiHelpers().getGuiHelper(), SILEX_VISIBLE, "jei.silexvisible"),
                new SilexRecipeCategory(registration.getJeiHelpers().getGuiHelper(), SILEX_UV, "jei.silexuv"),
                new SilexRecipeCategory(registration.getJeiHelpers().getGuiHelper(), SILEX_GAMMA, "jei.silexgamma"),
                new SilexRecipeCategory(registration.getJeiHelpers().getGuiHelper(), SILEX_DIGAMMA, "jei.silexdigamma"),
                new ElectrolyzerFluidRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ElectrolyzerMetalRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PressRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PyroOvenRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CentrifugeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new GasCentrifugeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BreederReactorRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FuelPoolRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new StorageDrumRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BoilingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PurexRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FusionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new PlasmaForgeRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FusionBreederFluidRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RbmkOutgasserRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrucibleRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RefineryRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new VacuumDistillRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CokerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CombinationOvenRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new RotaryFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ArcFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper(), false),
                new ArcFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper(), true),
                new SolidificationRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new LiquefactionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FractionTowerRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new CrackingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new ReformingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new HydrotreatingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BlastFurnaceRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new BlastFurnaceFuelRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new FoundryCastingRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AnvilConstructionRecipeCategory(registration.getJeiHelpers().getGuiHelper()),
                new AnvilSmithingRecipeCategory(registration.getJeiHelpers().getGuiHelper())
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(ANVIL_CONSTRUCTION, HbmAnvilRecipes.construction());
        registration.addRecipes(ANVIL_SMITHING, HbmAnvilRecipes.smithing());
        registration.addRecipes(BOILING, BoilingJeiRecipe.createAll());
        registration.addRecipes(SAWMILL, SawmillJeiRecipe.createAll());

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        List<RecipeHolder<ShredderRecipe>> recipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SHREDDER.get());
        registration.addRecipes(SHREDDER, recipes);
        List<RecipeHolder<AssemblyMachineRecipe>> assemblyRecipes = AssemblyMachineRecipe.activeVariants(
                minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ASSEMBLY_MACHINE.get())
        );
        registration.addRecipes(ASSEMBLY_MACHINE, assemblyRecipes);
        List<RecipeHolder<PrecisionAssemblerRecipe>> precisionAssemblerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRECISION_ASSEMBLER.get());
        registration.addRecipes(PRECISION_ASSEMBLER, precisionAssemblerRecipes);
        List<RecipeHolder<ChemicalPlantRecipe>> chemicalPlantRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CHEMICAL_PLANT.get());
        registration.addRecipes(CHEMICAL_PLANT, chemicalPlantRecipes);
        List<RecipeHolder<SolderingStationRecipe>> solderingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SOLDERING_STATION.get());
        registration.addRecipes(SOLDERING_STATION, solderingRecipes);
        List<RecipeHolder<ArcWelderRecipe>> arcWelderRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_WELDER.get());
        registration.addRecipes(ARC_WELDER, arcWelderRecipes);
        List<RecipeHolder<CompressorRecipe>> compressorRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.COMPRESSOR.get());
        registration.addRecipes(COMPRESSOR, compressorRecipes);
        List<RecipeHolder<MixerRecipe>> mixerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.MIXER.get());
        registration.addRecipes(MIXER, mixerRecipes);
        List<RecipeHolder<CrystallizerRecipe>> crystallizerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRYSTALLIZER.get());
        registration.addRecipes(CRYSTALLIZER, crystallizerRecipes);
        List<RecipeHolder<CyclotronRecipe>> cyclotronRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CYCLOTRON.get());
        registration.addRecipes(CYCLOTRON, cyclotronRecipes);
        List<RecipeHolder<ExposureChamberRecipe>> exposureChamberRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.EXPOSURE_CHAMBER.get());
        registration.addRecipes(EXPOSURE_CHAMBER, exposureChamberRecipes);
        List<RecipeHolder<ParticleAcceleratorRecipe>> particleAcceleratorRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PARTICLE_ACCELERATOR.get());
        registration.addRecipes(PARTICLE_ACCELERATOR, particleAcceleratorRecipes);
        List<RecipeHolder<SilexRecipe>> silexRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SILEX.get());
        registration.addRecipes(SILEX, silexRecipes);
        registration.addRecipes(SILEX_IR, filterSilex(silexRecipes, Wavelength.IR));
        registration.addRecipes(SILEX_VISIBLE, filterSilex(silexRecipes, Wavelength.VISIBLE));
        registration.addRecipes(SILEX_UV, filterSilex(silexRecipes, Wavelength.UV));
        registration.addRecipes(SILEX_GAMMA, filterSilex(silexRecipes, Wavelength.GAMMA));
        registration.addRecipes(SILEX_DIGAMMA, filterSilex(silexRecipes, Wavelength.DRX));
        List<RecipeHolder<ElectrolyzerFluidRecipe>> electrolyzerFluidRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_FLUID.get());
        registration.addRecipes(ELECTROLYZER_FLUID, electrolyzerFluidRecipes);
        List<RecipeHolder<ElectrolyzerMetalRecipe>> electrolyzerMetalRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ELECTROLYZER_METAL.get());
        registration.addRecipes(ELECTROLYZER_METAL, electrolyzerMetalRecipes);
        List<RecipeHolder<PressRecipe>> pressRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PRESS.get());
        registration.addRecipes(PRESS, pressRecipes);
        List<RecipeHolder<PyroOvenRecipe>> pyroOvenRecipes = PyroOvenJeiRecipes.appendSolidFuelRecipes(
                minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PYRO_OVEN.get())
        );
        registration.addRecipes(PYRO_OVEN, pyroOvenRecipes);
        List<RecipeHolder<AmmoPressRecipe>> ammoPressRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.AMMO_PRESS.get());
        registration.addRecipes(AMMO_PRESS, ammoPressRecipes);
        List<RecipeHolder<CentrifugeRecipe>> centrifugeRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CENTRIFUGE.get());
        registration.addRecipes(CENTRIFUGE, centrifugeRecipes);
        List<RecipeHolder<GasCentrifugeRecipe>> gasCentrifugeRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.GAS_CENTRIFUGE.get());
        registration.addRecipes(GAS_CENTRIFUGE, gasCentrifugeRecipes);
        List<RecipeHolder<BreederReactorRecipe>> breederRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BREEDER_REACTOR.get());
        registration.addRecipes(BREEDER_REACTOR, breederRecipes);
        List<RecipeHolder<FuelPoolRecipe>> fuelPoolRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUEL_POOL.get());
        registration.addRecipes(FUEL_POOL, fuelPoolRecipes);
        registration.addRecipes(STORAGE_DRUM, StorageDrumJeiRecipe.createAll());
        List<RecipeHolder<PurexRecipe>> purexRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PUREX.get());
        registration.addRecipes(PUREX, purexRecipes);
        List<RecipeHolder<FusionRecipe>> fusionRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUSION.get());
        registration.addRecipes(FUSION, fusionRecipes);
        List<RecipeHolder<PlasmaForgeRecipe>> plasmaForgeRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.PLASMA_FORGE.get());
        registration.addRecipes(PLASMA_FORGE, plasmaForgeRecipes);
        List<RecipeHolder<FusionBreederFluidRecipe>> fusionBreederFluidRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FUSION_BREEDER_FLUID.get());
        registration.addRecipes(FUSION_BREEDER_FLUID, fusionBreederFluidRecipes);
        List<RecipeHolder<RbmkOutgasserRecipe>> outgasserRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.RBMK_OUTGASSER.get()).stream()
                .filter(holder -> !holder.value().fusionOnly())
                .toList();
        registration.addRecipes(RBMK_OUTGASSER, outgasserRecipes);
        List<RecipeHolder<CrucibleRecipe>> crucibleRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRUCIBLE.get());
        registration.addRecipes(CRUCIBLE, crucibleRecipes);
        List<RecipeHolder<RefineryRecipe>> refineryRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.REFINERY.get());
        registration.addRecipes(REFINERY, refineryRecipes);
        List<RecipeHolder<VacuumDistillRecipe>> vacuumDistillRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.VACUUM_DISTILL.get());
        registration.addRecipes(VACUUM_DISTILL, vacuumDistillRecipes);
        List<RecipeHolder<CokerRecipe>> cokerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.COKER.get());
        registration.addRecipes(COKER, cokerRecipes);
        List<RecipeHolder<CombinationOvenRecipe>> combinationOvenRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.COMBINATION_OVEN.get());
        registration.addRecipes(COMBINATION_OVEN, combinationOvenRecipes);
        List<RecipeHolder<RotaryFurnaceRecipe>> rotaryFurnaceRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ROTARY_FURNACE.get());
        registration.addRecipes(ROTARY_FURNACE, rotaryFurnaceRecipes);
        List<RecipeHolder<ArcFurnaceRecipe>> arcFurnaceRecipes = ArcFurnaceJeiRecipes.appendDynamic(
                minecraft.level.getRecipeManager(),
                minecraft.level.registryAccess(),
                minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.ARC_FURNACE.get())
        );
        registration.addRecipes(ARC_FURNACE_SOLID, arcFurnaceRecipes.stream()
                .filter(holder -> holder.value().hasSolidOutput()).toList());
        registration.addRecipes(ARC_FURNACE_LIQUID, arcFurnaceRecipes.stream()
                .filter(holder -> holder.value().hasLiquidOutput()).toList());
        List<RecipeHolder<SolidificationRecipe>> solidificationRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.SOLIDIFICATION.get());
        registration.addRecipes(SOLIDIFICATION, solidificationRecipes);
        List<RecipeHolder<LiquefactionRecipe>> liquefactionRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.LIQUEFACTION.get());
        registration.addRecipes(LIQUEFACTION, liquefactionRecipes);
        List<RecipeHolder<FractionTowerRecipe>> fractionTowerRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.FRACTION_TOWER.get());
        registration.addRecipes(FRACTION_TOWER, fractionTowerRecipes);
        List<RecipeHolder<CrackingRecipe>> crackingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.CRACKING.get());
        registration.addRecipes(CRACKING, crackingRecipes);
        List<RecipeHolder<ReformingRecipe>> reformingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.REFORMING.get());
        registration.addRecipes(REFORMING, reformingRecipes);
        List<RecipeHolder<HydrotreatingRecipe>> hydrotreatingRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.HYDROTREATING.get());
        registration.addRecipes(HYDROTREATING, hydrotreatingRecipes);
        List<RecipeHolder<BlastFurnaceRecipe>> blastFurnaceRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE.get()).stream()
                .filter(holder -> !holder.value().hidden())
                .toList();
        registration.addRecipes(BLAST_FURNACE, blastFurnaceRecipes);
        List<RecipeHolder<BlastFurnaceFuelRecipe>> blastFurnaceFuelRecipes = minecraft.level.getRecipeManager().getAllRecipesFor(HbmRecipeTypes.BLAST_FURNACE_FUEL.get());
        registration.addRecipes(BLAST_FURNACE_FUEL, blastFurnaceFuelRecipes);
        registration.addRecipes(CRUCIBLE_CASTING, FoundryCastingJeiRecipe.createAll());
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addRecipeClickArea(IronFurnaceScreen.class, 52, 36, 70, 5, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(ArcWelderScreen.class, 72, 28, 33, 14, ARC_WELDER);
        registration.addRecipeClickArea(ArcFurnaceScreen.class, 7, 17, 18, 18, ARC_FURNACE_SOLID, ARC_FURNACE_LIQUID);
        registration.addRecipeClickArea(CompressorScreen.class, 42, 26, 55, 17, COMPRESSOR);
        registration.addRecipeClickArea(MixerScreen.class, 62, 36, 53, 44, MIXER);
        registration.addRecipeClickArea(AmmoPressScreen.class, 116, 18, 54, 72, AMMO_PRESS);
        registration.addRecipeClickArea(PyroOvenScreen.class, 57, 47, 27, 12, PYRO_OVEN);
        registration.addRecipeClickArea(CyclotronScreen.class, 48, 27, 34, 34, CYCLOTRON);
        registration.addRecipeClickArea(ExposureChamberScreen.class, 36, 39, 42, 10, EXPOSURE_CHAMBER);
        registration.addRecipeClickArea(ParticleAcceleratorScreen.class, 62, 18, 36, 45, PARTICLE_ACCELERATOR);
        registration.addRecipeClickArea(PrecisionAssemblerScreen.class, 62, 126, 70, 16, PRECISION_ASSEMBLER);
        registration.addRecipeClickArea(FusionMachineScreen.class, 43, 80, 18, 18, FUSION);
        registration.addRecipeClickArea(FusionMachineScreen.class, 7, 80, 18, 18, PLASMA_FORGE);
        registration.addRecipeClickArea(FusionMachineScreen.class, 67, 46, 42, 14, FUSION_BREEDER_FLUID);
        registration.addRecipeClickArea(HydrotreaterScreen.class, 35, 18, 124, 52, HYDROTREATING);
        registration.addRecipeClickArea(SteelFurnaceScreen.class, 54, 18, 68, 5, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(SteelFurnaceScreen.class, 54, 36, 68, 5, RecipeTypes.SMELTING);
        registration.addRecipeClickArea(SteelFurnaceScreen.class, 54, 54, 68, 5, RecipeTypes.SMELTING);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SHREDDER.get(), SHREDDER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SAWMILL.get(), SAWMILL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ASSEMBLY_MACHINE.get(), ASSEMBLY_MACHINE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ASSEMBLY_FACTORY.get(), ASSEMBLY_MACHINE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PRECASS.get(), PRECISION_ASSEMBLER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CHEMICAL_PLANT.get(), CHEMICAL_PLANT);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CHEMICAL_FACTORY.get(), CHEMICAL_PLANT);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SOLDERING_STATION.get(), SOLDERING_STATION);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ARC_WELDER.get(), ARC_WELDER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_COMPRESSOR.get(), COMPRESSOR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_COMPRESSOR_COMPACT.get(), COMPRESSOR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_MIXER.get(), MIXER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_AMMO_PRESS.get(), AMMO_PRESS);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CRYSTALLIZER.get(), CRYSTALLIZER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CYCLOTRON.get(), CYCLOTRON);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_EXPOSURE_CHAMBER.get(), EXPOSURE_CHAMBER);
        registration.addRecipeCatalyst(HbmBlocks.PA_DETECTOR.get(), PARTICLE_ACCELERATOR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SILEX.get(), SILEX);
        addOptionalItemCatalyst(registration, "laser_crystal_co2", SILEX_IR);
        addOptionalItemCatalyst(registration, "laser_crystal_bismuth", SILEX_VISIBLE);
        addOptionalItemCatalyst(registration, "laser_crystal_cmb", SILEX_UV);
        addOptionalItemCatalyst(registration, "laser_crystal_bale", SILEX_GAMMA);
        addOptionalItemCatalyst(registration, "laser_crystal_digamma", SILEX_DIGAMMA);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ELECTROLYSER.get(), ELECTROLYZER_FLUID);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ELECTROLYSER.get(), ELECTROLYZER_METAL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PRESS.get(), PRESS);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_EPRESS.get(), PRESS);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PYROOVEN.get(), PYRO_OVEN);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CENTRIFUGE.get(), CENTRIFUGE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_GASCENT.get(), GAS_CENTRIFUGE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_REACTOR_BREEDING.get(), BREEDER_REACTOR);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_WASTE_DRUM.get(), FUEL_POOL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_STORAGE_DRUM.get(), STORAGE_DRUM);
        registration.addRecipeCatalyst(HbmBlocks.HEAT_BOILER.get(), BOILING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_INDUSTRIAL_BOILER.get(), BOILING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_PUREX.get(), PUREX);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_TORUS.get(), FUSION);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_PLASMA_FORGE.get(), PLASMA_FORGE);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_BREEDER.get(), FUSION_BREEDER_FLUID);
        registration.addRecipeCatalyst(HbmBlocks.FUSION_BREEDER.get(), RBMK_OUTGASSER);
        registration.addRecipeCatalyst(HbmBlocks.RBMK_OUTGASSER.get(), RBMK_OUTGASSER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CRUCIBLE.get(), CRUCIBLE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_REFINERY.get(), REFINERY);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_VACUUM_DISTILL.get(), VACUUM_DISTILL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_COKER.get(), COKER);
        registration.addRecipeCatalyst(HbmBlocks.FURNACE_COMBINATION.get(), COMBINATION_OVEN);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ROTARY_FURNACE.get(), ROTARY_FURNACE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ARC_FURNACE.get(), ARC_FURNACE_SOLID);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_ARC_FURNACE.get(), ARC_FURNACE_LIQUID);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_SOLIDIFIER.get(), SOLIDIFICATION);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_LIQUEFACTOR.get(), LIQUEFACTION);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_FRACTION_TOWER.get(), FRACTION_TOWER);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CATALYTIC_CRACKER.get(), CRACKING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_CATALYTIC_REFORMER.get(), REFORMING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_HYDROTREATER.get(), HYDROTREATING);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_DIFURNACE_OFF.get(), BLAST_FURNACE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_DIFURNACE_OFF.get(), BLAST_FURNACE_FUEL);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_BLAST_FURNACE.get(), BLAST_FURNACE);
        registration.addRecipeCatalyst(HbmBlocks.MACHINE_BLAST_FURNACE.get(), BLAST_FURNACE_FUEL);
        registration.addRecipeCatalyst(HbmBlocks.FURNACE_IRON.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(HbmBlocks.FURNACE_STEEL.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(HbmBlocks.FOUNDRY_MOLD.get(), CRUCIBLE_CASTING);
        registration.addRecipeCatalyst(HbmBlocks.FOUNDRY_BASIN.get(), CRUCIBLE_CASTING);
        for (var anvil : HbmBlocks.ANVIL_BLOCKS) {
            registration.addRecipeCatalyst(anvil.get(), ANVIL_CONSTRUCTION);
            registration.addRecipeCatalyst(anvil.get(), ANVIL_SMITHING);
        }
    }

    private static List<RecipeHolder<SilexRecipe>> filterSilex(List<RecipeHolder<SilexRecipe>> recipes, Wavelength wavelength) {
        return recipes.stream()
                .filter(holder -> holder.value().wavelength() == wavelength)
                .toList();
    }

    private static void addOptionalItemCatalyst(IRecipeCatalystRegistration registration, String itemId, RecipeType<RecipeHolder<SilexRecipe>> recipeType) {
        BuiltInRegistries.ITEM.getOptional(ReinhardtsHBM.id(itemId))
                .ifPresent(item -> registration.addRecipeCatalyst(new ItemStack(item), recipeType));
    }
}
