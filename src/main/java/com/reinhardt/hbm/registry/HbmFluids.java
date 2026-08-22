package com.reinhardt.hbm.registry;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.fluid.HbmFluidDefinitions;
import com.reinhardt.hbm.fluid.HbmFluidTrait;
import com.reinhardt.hbm.fluid.HbmNeoFluidType;
import com.reinhardt.hbm.fluid.CoriumFlowingFluid;
import com.reinhardt.hbm.fluid.LegacyHazardFlowingFluid;
import com.reinhardt.hbm.fluid.VolcanicFlowingFluid;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public final class HbmFluids {
    public static final DeferredRegister<net.neoforged.neoforge.fluids.FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, ReinhardtsHBM.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, ReinhardtsHBM.MOD_ID);

    private static final List<HbmFluidDefinition> DEFINITIONS = new ArrayList<>();
    private static final List<HbmFluidDefinition> NICE_ORDER = new ArrayList<>();
    private static final Map<Integer, HbmFluidDefinition> BY_OLD_ID = new HashMap<>();
    private static final Map<String, HbmFluidDefinition> BY_NAME = new LinkedHashMap<>();
    private static final Map<String, HbmFluidEntry> ENTRIES = new LinkedHashMap<>();
    private static final Map<Fluid, HbmFluidDefinition> VANILLA_FLUIDS = new IdentityHashMap<>();
    private static boolean bootstrapped;
    private static DeferredHolder<Fluid, FlowingFluid> coriumSource;
    private static DeferredHolder<Fluid, FlowingFluid> coriumFlowing;
    private static DeferredHolder<Fluid, FlowingFluid> volcanicLavaSource;
    private static DeferredHolder<Fluid, FlowingFluid> volcanicLavaFlowing;

    static {
        VANILLA_FLUIDS.put(Fluids.WATER, null);
        VANILLA_FLUIDS.put(Fluids.FLOWING_WATER, null);
        VANILLA_FLUIDS.put(Fluids.LAVA, null);
        VANILLA_FLUIDS.put(Fluids.FLOWING_LAVA, null);
    }

    private HbmFluids() {
    }

    public static synchronized void bootstrap() {
        if (bootstrapped) {
            return;
        }
        loadDefinitions();
        registerNeoForgeEntries();
        bootstrapped = true;
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }

    public static List<HbmFluidDefinition> definitions() {
        return Collections.unmodifiableList(DEFINITIONS);
    }

    public static List<HbmFluidDefinition> niceOrder() {
        return Collections.unmodifiableList(NICE_ORDER);
    }

    public static List<HbmFluidEntry> entries() {
        return List.copyOf(ENTRIES.values());
    }

    public static HbmFluidDefinition none() {
        bootstrap();
        return BY_NAME.get("none");
    }

    public static Optional<HbmFluidDefinition> byName(String name) {
        bootstrap();
        if (name == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_NAME.get(name.toLowerCase(Locale.ROOT)));
    }

    public static Optional<HbmFluidDefinition> byOldId(int id) {
        bootstrap();
        return Optional.ofNullable(BY_OLD_ID.get(id));
    }

    public static Optional<HbmFluidEntry> entry(String name) {
        bootstrap();
        return Optional.ofNullable(ENTRIES.get(name.toLowerCase(Locale.ROOT)));
    }

    public static Optional<HbmFluidDefinition> fromNeoFluid(Fluid fluid) {
        bootstrap();
        if (fluid == null) {
            return Optional.empty();
        }
        HbmFluidDefinition vanilla = VANILLA_FLUIDS.get(fluid);
        if (vanilla != null) {
            return Optional.of(vanilla);
        }
        for (HbmFluidEntry entry : ENTRIES.values()) {
            if (entry.source().get() == fluid || entry.flowing().get() == fluid) {
                return Optional.of(entry.definition());
            }
        }
        return Optional.empty();
    }

    public static Fluid toNeoFluid(HbmFluidDefinition definition) {
        bootstrap();
        if (definition == null || definition.isNone()) {
            return Fluids.EMPTY;
        }
        if (definition.name().equals("water")) {
            return Fluids.WATER;
        }
        if (definition.name().equals("lava")) {
            return Fluids.LAVA;
        }
        HbmFluidEntry entry = ENTRIES.get(definition.name());
        return entry == null ? Fluids.EMPTY : entry.source().get();
    }

    public static FlowingFluid coriumSource() {
        bootstrap();
        if (coriumSource == null) {
            throw new IllegalStateException("corium_fluid was not registered");
        }
        return coriumSource.get();
    }

    public static FlowingFluid volcanicLavaSource() {
        bootstrap();
        if (volcanicLavaSource == null) {
            throw new IllegalStateException("volcanic_lava_fluid was not registered");
        }
        return volcanicLavaSource.get();
    }

    public static FlowingFluid source(String name) {
        bootstrap();
        HbmFluidEntry entry = ENTRIES.get(name);
        if (entry == null) {
            throw new IllegalStateException(name + " was not registered");
        }
        return entry.source().get();
    }

    public static FluidStack toNeoStack(HbmFluidDefinition definition, int amount) {
        Fluid fluid = toNeoFluid(definition);
        if (fluid == Fluids.EMPTY || amount <= 0) {
            return FluidStack.EMPTY;
        }
        return new FluidStack(fluid, amount);
    }

    private static void loadDefinitions() {
        for (HbmFluidDefinition definition : HbmFluidDefinitions.all()) {
            DEFINITIONS.add(definition);
            NICE_ORDER.add(definition);
            BY_OLD_ID.put(definition.oldId(), definition);
            BY_NAME.put(definition.name(), definition);
        }

        HbmFluidDefinition water = BY_NAME.get("water");
        HbmFluidDefinition lava = BY_NAME.get("lava");
        VANILLA_FLUIDS.put(Fluids.WATER, water);
        VANILLA_FLUIDS.put(Fluids.FLOWING_WATER, water);
        VANILLA_FLUIDS.put(Fluids.LAVA, lava);
        VANILLA_FLUIDS.put(Fluids.FLOWING_LAVA, lava);
    }

    private static void registerNeoForgeEntries() {
        for (HbmFluidDefinition definition : DEFINITIONS) {
            if (!definition.registersNeoForgeFluid()) {
                continue;
            }

            String path = definition.name();
            BaseFlowingFluid.Properties[] properties = new BaseFlowingFluid.Properties[1];

            DeferredHolder<net.neoforged.neoforge.fluids.FluidType, HbmNeoFluidType> fluidType =
                    FLUID_TYPES.register(path, () -> new HbmNeoFluidType(definition));
            DeferredHolder<Fluid, FlowingFluid>[] source = new DeferredHolder[1];
            DeferredHolder<Fluid, FlowingFluid>[] flowing = new DeferredHolder[1];
            if (definition.name().equals("corium_fluid")) {
                source[0] = FLUIDS.register(path, () -> new CoriumFlowingFluid.Source(properties[0]));
                flowing[0] = FLUIDS.register("flowing_" + path, () -> new CoriumFlowingFluid.Flowing(properties[0]));
            } else if (definition.name().equals("volcanic_lava_fluid")) {
                source[0] = FLUIDS.register(path, () -> new VolcanicFlowingFluid.Source(properties[0]));
                flowing[0] = FLUIDS.register("flowing_" + path, () -> new VolcanicFlowingFluid.Flowing(properties[0]));
            } else if (isLegacyHazardFluid(path)) {
                source[0] = FLUIDS.register(path, () -> new LegacyHazardFlowingFluid.Source(properties[0]));
                flowing[0] = FLUIDS.register("flowing_" + path, () -> new LegacyHazardFlowingFluid.Flowing(properties[0]));
            } else {
                source[0] = FLUIDS.register(path, () -> new BaseFlowingFluid.Source(properties[0]));
                flowing[0] = FLUIDS.register("flowing_" + path, () -> new BaseFlowingFluid.Flowing(properties[0]));
            }

            DeferredBlock<LiquidBlock> block = null;
            DeferredItem<Item> bucket = null;
            if (definition.name().equals("corium_fluid")) {
                block = coriumBlockEntry();
            } else if (definition.name().equals("volcanic_lava_fluid")) {
                block = volcanicLavaBlockEntry();
            } else if (legacyBlockEntry(path) != null) {
                block = legacyBlockEntry(path);
            } else if (definition.allowsBucket()) {
                block = HbmBlocks.BLOCKS.register(
                        "fluid_" + path,
                        () -> new LiquidBlock(source[0].get(), fluidBlockProperties(definition))
                );
            }
            if (definition.allowsBucket()) {
                bucket = HbmItems.ITEMS.register(
                        legacyBucketId(path),
                        () -> new BucketItem(source[0].get(), new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1))
                );
            }

            if (definition.name().equals("corium_fluid")) {
                coriumSource = source[0];
                coriumFlowing = flowing[0];
            } else if (definition.name().equals("volcanic_lava_fluid")) {
                volcanicLavaSource = source[0];
                volcanicLavaFlowing = flowing[0];
            }

            BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(fluidType, source[0], flowing[0])
                    .slopeFindDistance(definition.hasTrait(HbmFluidTrait.VISCOUS) ? 2 : 4)
                    .levelDecreasePerBlock(definition.hasTrait(HbmFluidTrait.VISCOUS) ? 2 : 1)
                    .tickRate(legacyTickRate(path, definition));
            if (definition.name().equals("volcanic_lava_fluid")) {
                props.explosionResistance(500.0F);
            }
            if (block != null) {
                props.block(block);
            }
            if (bucket != null) {
                props.bucket(bucket);
            }
            properties[0] = props;

            ENTRIES.put(path, new HbmFluidEntry(definition, fluidType, source[0], flowing[0], block, bucket));
        }
    }

    private static BlockBehaviour.Properties fluidBlockProperties(HbmFluidDefinition definition) {
        return BlockBehaviour.Properties.of()
                .mapColor(definition.hasTrait(HbmFluidTrait.GASEOUS) ? MapColor.NONE : MapColor.WATER)
                .replaceable()
                .noCollission()
                .strength(100.0F)
                .noLootTable()
                .pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> definition.temperatureCelsius() >= 1000 ? 8 : 0);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DeferredBlock<LiquidBlock> coriumBlockEntry() {
        return (DeferredBlock) HbmBlocks.CORIUM_BLOCK;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DeferredBlock<LiquidBlock> volcanicLavaBlockEntry() {
        return (DeferredBlock) HbmBlocks.VOLCANIC_LAVA_BLOCK;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static DeferredBlock<LiquidBlock> legacyBlockEntry(String name) {
        return switch (name) {
            case "mud_fluid" -> (DeferredBlock) HbmBlocks.MUD_BLOCK;
            case "acid_fluid" -> (DeferredBlock) HbmBlocks.ACID_BLOCK;
            case "toxic_fluid" -> (DeferredBlock) HbmBlocks.TOXIC_BLOCK;
            case "schrabidic" -> (DeferredBlock) HbmBlocks.SCHRABIDIC_BLOCK;
            case "rad_lava_fluid" -> (DeferredBlock) HbmBlocks.RAD_LAVA_BLOCK;
            case "sulfuric_acid" -> (DeferredBlock) HbmBlocks.SULFURIC_ACID_BLOCK;
            default -> null;
        };
    }

    private static boolean isLegacyHazardFluid(String name) {
        return switch (name) {
            case "mud_fluid", "acid_fluid", "toxic_fluid", "schrabidic", "rad_lava_fluid", "sulfuric_acid" -> true;
            default -> false;
        };
    }

    private static int legacyTickRate(String name, HbmFluidDefinition definition) {
        return switch (name) {
            case "corium_fluid" -> 30;
            case "acid_fluid", "rad_lava_fluid", "sulfuric_acid" -> 5;
            case "mud_fluid", "toxic_fluid", "schrabidic" -> 15;
            default -> definition.hasTrait(HbmFluidTrait.VISCOUS) ? 12 : 5;
        };
    }

    private static String legacyBucketId(String name) {
        return switch (name) {
            case "mud_fluid" -> "bucket_mud";
            case "acid_fluid" -> "bucket_acid";
            case "toxic_fluid" -> "bucket_toxic";
            case "schrabidic" -> "bucket_schrabidic_acid";
            case "sulfuric_acid" -> "bucket_sulfuric_acid";
            default -> name + "_bucket";
        };
    }

    public record HbmFluidEntry(
            HbmFluidDefinition definition,
            DeferredHolder<net.neoforged.neoforge.fluids.FluidType, HbmNeoFluidType> fluidType,
            DeferredHolder<Fluid, FlowingFluid> source,
            DeferredHolder<Fluid, FlowingFluid> flowing,
            DeferredBlock<LiquidBlock> block,
            DeferredItem<Item> bucket
    ) {
        public Optional<DeferredItem<Item>> bucketItem() {
            return Optional.ofNullable(bucket);
        }

        public Optional<DeferredBlock<LiquidBlock>> blockEntry() {
            return Optional.ofNullable(block);
        }

        public ResourceLocation fluidId() {
            return ReinhardtsHBM.id(definition.name());
        }
    }
}
