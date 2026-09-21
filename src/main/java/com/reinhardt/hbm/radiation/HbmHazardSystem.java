package com.reinhardt.hbm.radiation;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.foundry.FoundryMaterial;
import com.reinhardt.hbm.foundry.FoundryMaterialItems;
import com.reinhardt.hbm.foundry.FoundryMaterialStack;
import com.reinhardt.hbm.foundry.FoundryShape;
import com.reinhardt.hbm.item.FluidIconItem;
import com.reinhardt.hbm.item.HbmFluidContainerItem;
import com.reinhardt.hbm.item.LegacyVariantBlockItem;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.OreBasaltBlockItem;
import com.reinhardt.hbm.item.RbmkFuelRodItem;
import com.reinhardt.hbm.item.RtgDepletedPelletItem;
import com.reinhardt.hbm.item.ScrapsItem;
import com.reinhardt.hbm.item.WatzPelletItem;
import com.reinhardt.hbm.item.ZirnoxRodItem;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class HbmHazardSystem {
    private static final String IMMERSIVE_ENGINEERING_MOD_ID = "immersiveengineering";
    private static final Map<String, HbmHazardData> CACHE = new ConcurrentHashMap<>();
    private static final Map<Item, HbmHazardData> EXTERNAL_URANIUM_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Double> ASBESTOS_LEVEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Double> COAL_DUST_LEVEL_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Integer> TOXIC_LEVEL_CACHE = new ConcurrentHashMap<>();

    /*
     * 1.7.10 used OreDictionary keys for the respiratory hazards.  NeoForge's
     * common item tags are the corresponding cross-mod contract in 1.21.1;
     * keep the forge: aliases as well so older ports which still publish those
     * names continue to behave like the original dictionary lookup.
     */
    private static final TagKey<Item>[] ASBESTOS_DUST_TAGS = tags("dusts/asbestos");
    private static final TagKey<Item>[] ASBESTOS_INGOT_TAGS = tags("ingots/asbestos");
    private static final TagKey<Item>[] ASBESTOS_ORE_TAGS = tags("ores/asbestos");
    private static final TagKey<Item>[] ASBESTOS_BLOCK_TAGS = tags("storage_blocks/asbestos");
    private static final TagKey<Item>[] COAL_DUST_TAGS = tags("dusts/coal");
    private static final TagKey<Item>[] LIGNITE_DUST_TAGS = tags("dusts/lignite");
    private static final TagKey<Item>[] COAL_TINY_DUST_TAGS = tags("tiny_dusts/coal");
    private static final TagKey<Item>[] LIGNITE_TINY_DUST_TAGS = tags("tiny_dusts/lignite");

    /*
     * HBM's normal radiation inference is intentionally restricted to the
     * reinhardtshbm namespace.  Uranium from other mods therefore needs an
     * explicit cross-mod shape contract.  Immersive Engineering publishes
     * these common tags for its uranium items, so this stays optional and does
     * not create a hard dependency on IE.
     */
    private static final TagKey<Item>[] URANIUM_NUGGET_TAGS = tags("nuggets/uranium");
    private static final TagKey<Item>[] URANIUM_INGOT_TAGS = tags("ingots/uranium");
    private static final TagKey<Item>[] URANIUM_DUST_TAGS = tags("dusts/uranium");
    private static final TagKey<Item>[] URANIUM_ORE_TAGS = tags("ores/uranium");
    private static final TagKey<Item>[] URANIUM_RAW_TAGS = tags("raw_materials/uranium");
    private static final TagKey<Item>[] URANIUM_PLATE_TAGS = tags("plates/uranium");
    private static final TagKey<Item>[] URANIUM_SHEETMETAL_TAGS = tags("sheetmetals/uranium");
    private static final TagKey<Item>[] URANIUM_STORAGE_BLOCK_TAGS = tags("storage_blocks/uranium");
    private static final TagKey<Item>[] URANIUM_RAW_STORAGE_BLOCK_TAGS = tags("storage_blocks/raw_uranium");

    private HbmHazardSystem() {
    }

    public static HbmHazardData hazards(ItemStack stack) {
        return hazardsPerItem(stack).multiply(stack.getCount());
    }

    public static HbmHazardData hazardsPerItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return HbmHazardData.EMPTY;
        }

        HbmHazardData dynamicHazard = dynamicHazards(stack);
        if (!dynamicHazard.isEmpty()) {
            return dynamicHazard;
        }

        HbmHazardData containerHazard = fluidContainerHazards(stack);
        if (!containerHazard.isEmpty()) {
            return containerHazard;
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return HbmHazardData.EMPTY;
        }
        if (!key.getNamespace().equals(ReinhardtsHBM.MOD_ID)) {
            return externalUraniumHazards(stack);
        }

        HbmHazardData hazard = CACHE.computeIfAbsent(key.getPath(), HbmHazardSystem::inferHazards);
        if (!hazard.isEmpty() || !(stack.getItem() instanceof LegacyVariantItem variantItem)) {
            return hazard;
        }
        return CACHE.computeIfAbsent(variantItem.variant(stack).id(), HbmHazardSystem::inferHazards);
    }

    public static double rawRadiation(ItemStack stack) {
        return hazardsPerItem(stack).radiation();
    }

    public static double rawRadiationForId(String id) {
        return inferHazards(id).radiation();
    }

    /**
     * Returns the legacy 1.7.10 asbestos hazard level for an inventory stack.
     * These hazards were registered through the old ore-dictionary system and
     * therefore cannot be represented by the radiation-only hazard record.
     */
    public static double asbestosLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }
        String cacheKey = respiratoryCacheKey(stack);
        if (!cacheKey.isEmpty()) {
            return ASBESTOS_LEVEL_CACHE.computeIfAbsent(cacheKey, unused -> computeAsbestosLevel(stack));
        }
        return computeAsbestosLevel(stack);
    }

    private static double computeAsbestosLevel(ItemStack stack) {
        String path = itemPath(stack);
        if (!path.isEmpty()) {
            if (path.equals("ore_basalt") && stack.getItem() instanceof OreBasaltBlockItem) {
                if (OreBasaltBlockItem.variantName(stack).equals("asbestos")) {
                    return 1.0D;
                }
            }
            double direct = switch (path) {
                case "brick_asbestos", "tile_lab_broken", "ingot_asbestos", "ore_asbestos",
                        "ore_gneiss_asbestos", "ore_deepslate_asbestos", "stone_resource_asbestos" -> 1.0D;
                case "powder_asbestos", "powder_coltan_ore" -> 3.0D;
                case "block_asbestos" -> 10.0D;
                default -> 0.0D;
            };
            if (direct > 0.0D) {
                return direct;
            }
        }

        if (hasAnyTag(stack, ASBESTOS_DUST_TAGS)) {
            return 3.0D;
        }
        if (hasAnyTag(stack, ASBESTOS_BLOCK_TAGS)) {
            return 10.0D;
        }
        if (hasAnyTag(stack, ASBESTOS_INGOT_TAGS) || hasAnyTag(stack, ASBESTOS_ORE_TAGS)) {
            return 1.0D;
        }
        return 0.0D;
    }

    /** Returns the legacy 1.7.10 coal-dust hazard level for an inventory stack. */
    public static double coalDustLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0.0D;
        }
        String cacheKey = respiratoryCacheKey(stack);
        if (!cacheKey.isEmpty()) {
            return COAL_DUST_LEVEL_CACHE.computeIfAbsent(cacheKey, unused -> computeCoalDustLevel(stack));
        }
        return computeCoalDustLevel(stack);
    }

    private static double computeCoalDustLevel(ItemStack stack) {
        String path = itemPath(stack);
        if (!path.isEmpty()) {
            double direct = switch (path) {
                case "powder_coal", "powder_lignite" -> 3.0D;
                case "powder_coal_tiny", "powder_lignite_tiny" -> 0.3D;
                default -> 0.0D;
            };
            if (direct > 0.0D) {
                return direct;
            }
        }

        if (hasAnyTag(stack, COAL_DUST_TAGS) || hasAnyTag(stack, LIGNITE_DUST_TAGS)) {
            return 3.0D;
        }
        if (hasAnyTag(stack, COAL_TINY_DUST_TAGS) || hasAnyTag(stack, LIGNITE_TINY_DUST_TAGS)) {
            return 0.3D;
        }
        return 0.0D;
    }

    /**
     * Returns the exact unstacked toxicity assigned by the pre-refactor HBM
     * ItemHazard registrations.  Unlike radiation and coal dust, the old
     * toxic handler deliberately did not multiply its level by stack size.
     */
    public static int toxicLevel(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        String path = itemPath(stack);
        if (path.isEmpty()) {
            return 0;
        }

        if (path.equals("pellet_rtg_depleted") && stack.getItem() instanceof RtgDepletedPelletItem) {
            CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            return switch (tag.getString("rtg_depleted_material")) {
                case "lead" -> 2;
                case "mercury" -> 4;
                default -> 0;
            };
        }
        if (path.equals("watz_pellet") && WatzPelletItem.isActivePellet(stack)) {
            return WatzPelletItem.type(stack) == WatzPelletItem.Type.LEAD ? 7 : 0;
        }
        if (path.equals("apple_lead")) {
            CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
            int variant = Math.max(0, model == null ? 0 : model.value());
            return switch (variant) {
                case 0 -> 2;
                case 1 -> 4;
                default -> 8;
            };
        }

        return TOXIC_LEVEL_CACHE.computeIfAbsent(path, HbmHazardSystem::inferToxicLevel);
    }

    private static int inferToxicLevel(String path) {
        return switch (path) {
            case "ingot_lead", "ingot_pb209", "ingot_radspice", "billet_pb209", "plate_lead" -> 2;
            case "ingot_arsenic" -> 16;
            case "nugget_lead", "nugget_pb209", "nugget_radspice", "powder_radspice_tiny" -> 1;
            case "nugget_arsenic", "powder_radspice" -> 3;
            case "nugget_mercury" -> 2;
            case "bottle_mercury" -> 6;
            case "powder_lead", "powder_pb209", "powder_pb209_tiny", "pellet_rtg_lead" -> 4;
            case "powder_cloud" -> 14;
            case "powder_poison" -> 30;
            case "pellet_mercury" -> 25;
            case "crystal_lead" -> 12;
            default -> 0;
        };
    }

    public static void appendTooltip(ItemStack stack, List<Component> tooltip) {
        HbmHazardData perItem = hazardsPerItem(stack);
        int toxicLevel = toxicLevel(stack);
        if (toxicLevel > 0) {
            String adjective = toxicLevel > 16 ? "extreme"
                    : toxicLevel > 8 ? "veryhigh"
                    : toxicLevel > 4 ? "high"
                    : toxicLevel > 2 ? "medium"
                    : "little";
            tooltip.add(Component.translatable(
                    "trait.reinhardtshbm.toxic.level",
                    Component.translatable("adjective.reinhardtshbm." + adjective),
                    Component.translatable("trait.reinhardtshbm.toxic")
            ).withStyle(ChatFormatting.GREEN));
        }
        if (asbestosLevel(stack) > 0.0D) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.asbestos").withStyle(ChatFormatting.WHITE));
        }
        if (coalDustLevel(stack) > 0.0D) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.coal").withStyle(ChatFormatting.DARK_GRAY));
        }
        if (perItem.isEmpty()) {
            return;
        }

        if (perItem.radiation() > 0.0D) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.radioactive").withStyle(ChatFormatting.GREEN));
            tooltip.add(Component.translatable(
                    "desc.reinhardtshbm.rads",
                    formatRadiation(perItem.radiation())
            ).withStyle(ChatFormatting.YELLOW));
            if (stack.getCount() > 1) {
                tooltip.add(Component.translatable(
                        "desc.reinhardtshbm.stack_rads",
                        formatRadiation(perItem.radiation() * stack.getCount())
                ).withStyle(ChatFormatting.YELLOW));
            }
        }
        if (perItem.digamma() > 0.0D) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.digamma").withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(
                    "desc.reinhardtshbm.digamma",
                    formatDigamma(perItem.digamma())
            ).withStyle(ChatFormatting.DARK_RED));
            if (stack.getCount() > 1) {
                tooltip.add(Component.translatable(
                        "desc.reinhardtshbm.stack_digamma",
                        formatDigamma(perItem.digamma() * stack.getCount())
                ).withStyle(ChatFormatting.DARK_RED));
            }
        }
        if (perItem.hot() > 0.0D) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.hot").withStyle(ChatFormatting.GOLD));
        }
        if (perItem.blinding() > 0.0D) {
            tooltip.add(Component.translatable("trait.reinhardtshbm.blinding").withStyle(ChatFormatting.DARK_AQUA));
        }
    }

    public static double fluidRadiation(HbmFluidDefinition fluid) {
        if (fluid == null || fluid.isNone()) {
            return 0.0D;
        }
        for (String token : fluid.rawTraits().split("\\|")) {
            String clean = token.trim();
            if (clean.startsWith("RADIATION:")) {
                String value = clean.substring("RADIATION:".length());
                int nextColon = value.indexOf(':');
                if (nextColon >= 0) {
                    value = value.substring(0, nextColon);
                }
                try {
                    return Double.parseDouble(value);
                } catch (NumberFormatException ignored) {
                    return 0.0D;
                }
            }
        }
        return 0.0D;
    }

    private static HbmHazardData dynamicHazards(ItemStack stack) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId != null && itemId.getNamespace().equals(ReinhardtsHBM.MOD_ID)) {
            Optional<FoundryMaterialStack> materialStack = FoundryMaterialItems.materialStackFromIndependentItemPath(itemId.getPath());
            if (materialStack.isPresent()) {
                FoundryMaterialStack contents = materialStack.get();
                return foundryMaterialHazards(contents.material(), contents.amount());
            }
        }
        if (stack.getItem() instanceof ScrapsItem) {
            FoundryMaterialStack contents = ScrapsItem.contents(stack);
            return contents == null
                    ? HbmHazardData.EMPTY
                    : foundryMaterialHazards(contents.material(), contents.amount());
        }
        if (stack.getItem() instanceof RbmkFuelRodItem rod) {
            return rbmkFuelRodHazards(stack, rod);
        }
        if (stack.getItem() instanceof ZirnoxRodItem rod) {
            return zirnoxFuelRodHazards(stack, rod);
        }
        if (stack.is(com.reinhardt.hbm.registry.HbmItems.ROD_ZIRNOX_DEPLETED.get())
                && stack.getItem() instanceof LegacyVariantItem depleted) {
            return depletedZirnoxRodHazards(depleted.variant(stack).id());
        }
        if (stack.getItem() instanceof FluidIconItem) {
            Optional<HbmFluidDefinition> fluid = FluidIconItem.fluid(stack);
            int amount = FluidIconItem.amount(stack);
            if (fluid.isPresent() && amount > 0) {
                double radiation = fluidRadiation(fluid.get()) * amount / 1000.0D;
                return radiation <= 0.0D ? HbmHazardData.EMPTY : rad(radiation);
            }
        }
        return HbmHazardData.EMPTY;
    }

    private static HbmHazardData foundryMaterialHazards(FoundryMaterial material, int quanta) {
        if (material == null || quanta <= 0) {
            return HbmHazardData.EMPTY;
        }
        double base = materialBase(material.name());
        if (base <= 0.0D) {
            base = materialBase(material.itemSuffix());
        }
        if (base <= 0.0D) {
            return HbmHazardData.EMPTY;
        }
        double multiplier = quanta / (double) FoundryShape.INGOT.quanta();
        String materialName = normalize(material.name());
        return new HbmHazardData(
                base * multiplier,
                0.0D,
                0.0D,
                hotForMaterial(materialName),
                blindingForMaterial(materialName, multiplier)
        );
    }

    private static HbmHazardData rbmkFuelRodHazards(ItemStack stack, RbmkFuelRodItem rod) {
        String id = rod.variant(stack).id();
        double depletion = Math.max(0.0D, Math.min(1.0D, RbmkFuelRodItem.depletion(stack)));
        double freshRadiation = rbmkFreshRadiation(id);
        double wasteRadiation = rbmkWasteRadiation(id);
        double radiation = freshRadiation + (Math.max(freshRadiation, wasteRadiation) - freshRadiation) * depletion;
        double contamination = rbmkWasteContamination(id) * depletion;
        double digamma = id.equals("drx") ? 25.0D * Math.max(0.1D, depletion) : 0.0D;
        double hot = Math.max(RbmkFuelRodItem.coreHeat(stack), RbmkFuelRodItem.hullHeat(stack)) >= 50.0F ? 1.0D : 0.0D;
        return new HbmHazardData(radiation, contamination, digamma, hot, 0.0D);
    }

    private static HbmHazardData zirnoxFuelRodHazards(ItemStack stack, ZirnoxRodItem rod) {
        String id = rod.variant(stack).id();
        ZirnoxRodItem.Fuel fuel = rod.fuel(stack);
        double depletion = Math.max(0.0D, Math.min(1.0D,
                ZirnoxRodItem.life(stack) / (double) Math.max(1, fuel.maxLife())));
        depletion = Math.pow(depletion, 0.4D);
        double fresh = zirnoxFreshRadiation(id);
        double spent = zirnoxSpentRadiation(id);
        return rad(fresh + (spent - fresh) * depletion);
    }

    private static HbmHazardData depletedZirnoxRodHazards(String id) {
        double blinding = id.equals("les_fuel") ? 20.0D : 0.0D;
        return new HbmHazardData(zirnoxSpentRadiation(id), 0.0D, 0.0D, 0.0D, blinding);
    }

    private static double zirnoxFreshRadiation(String id) {
        return switch (id) {
            case "natural_uranium_fuel" -> HbmRadiationConstants.U;
            case "uranium_fuel" -> HbmRadiationConstants.UF;
            case "th232_fuel" -> HbmRadiationConstants.TH232;
            case "thorium_fuel" -> HbmRadiationConstants.THF;
            case "mox_fuel", "zfb_mox_fuel" -> HbmRadiationConstants.MOX;
            case "plutonium_fuel" -> HbmRadiationConstants.PUF;
            case "u233_fuel" -> HbmRadiationConstants.U233;
            case "u235_fuel" -> HbmRadiationConstants.U235;
            case "les_fuel" -> HbmRadiationConstants.SAF;
            case "lithium_fuel" -> 0.0D;
            default -> 0.0D;
        };
    }

    private static double zirnoxSpentRadiation(String id) {
        return switch (id) {
            case "natural_uranium_fuel" -> HbmRadiationConstants.WST * 11.5D;
            case "uranium_fuel" -> HbmRadiationConstants.WST * 10.0D;
            case "th232_fuel" -> HbmRadiationConstants.THF;
            case "thorium_fuel" -> HbmRadiationConstants.WST * 7.5D;
            case "mox_fuel" -> HbmRadiationConstants.WST * 10.0D;
            case "plutonium_fuel" -> HbmRadiationConstants.WST * 12.5D;
            case "u233_fuel" -> HbmRadiationConstants.WST * 10.0D;
            case "u235_fuel" -> HbmRadiationConstants.WST * 11.0D;
            case "les_fuel" -> HbmRadiationConstants.WST * 15.0D;
            case "lithium_fuel" -> 0.001D;
            case "zfb_mox_fuel" -> HbmRadiationConstants.WST * 5.0D;
            default -> 0.0D;
        };
    }

    private static double rbmkFreshRadiation(String id) {
        return switch (id) {
            case "ueu", "meu" -> HbmRadiationConstants.UF * HbmRadiationConstants.BILLET * 4.0D;
            case "heu233" -> HbmRadiationConstants.U233 * HbmRadiationConstants.BILLET * 4.0D;
            case "heu235" -> HbmRadiationConstants.U235 * HbmRadiationConstants.BILLET * 4.0D;
            case "uzh" -> HbmRadiationConstants.UZH * HbmRadiationConstants.BILLET * 4.0D;
            case "thmeu" -> (HbmRadiationConstants.TH232 + HbmRadiationConstants.UF) * HbmRadiationConstants.BILLET * 4.0D;
            case "lep", "mep", "hep" -> HbmRadiationConstants.PU239 * HbmRadiationConstants.BILLET * 4.0D;
            case "hep241" -> HbmRadiationConstants.PU241 * HbmRadiationConstants.BILLET * 4.0D;
            case "lea", "mea", "hea241" -> HbmRadiationConstants.AM241 * HbmRadiationConstants.BILLET * 4.0D;
            case "hea242" -> HbmRadiationConstants.AM242 * HbmRadiationConstants.BILLET * 4.0D;
            case "men", "hen" -> HbmRadiationConstants.NPF * HbmRadiationConstants.BILLET * 4.0D;
            case "mox", "zfb_bismuth", "zfb_pu241", "zfb_am_mix" -> HbmRadiationConstants.MOX * HbmRadiationConstants.BILLET * 4.0D;
            case "les", "mes", "hes" -> HbmRadiationConstants.SA326 * HbmRadiationConstants.BILLET * 4.0D;
            case "leaus", "heaus", "balefire_gold" -> HbmRadiationConstants.AU198 * HbmRadiationConstants.BILLET * 4.0D;
            case "po210be" -> HbmRadiationConstants.PO210 * HbmRadiationConstants.BILLET * 4.0D;
            case "ra226be" -> HbmRadiationConstants.RA226 * HbmRadiationConstants.BILLET * 4.0D;
            case "pu238be" -> HbmRadiationConstants.PU238 * HbmRadiationConstants.BILLET * 4.0D;
            case "flashlead" -> HbmRadiationConstants.PB209 * HbmRadiationConstants.BILLET * 4.0D;
            case "balefire", "drx" -> HbmRadiationConstants.BF * HbmRadiationConstants.BILLET;
            default -> HbmRadiationConstants.UF * HbmRadiationConstants.BILLET * 4.0D;
        };
    }

    private static double rbmkWasteRadiation(String id) {
        double multiplier = switch (id) {
            case "ueu", "meu", "uzh" -> 11.5D;
            case "thmeu" -> 7.5D;
            case "mox", "zfb_bismuth", "zfb_pu241", "zfb_am_mix" -> 10.0D;
            case "lep", "mep", "hep", "hep241" -> 12.5D;
            case "heu233" -> 10.0D;
            case "heu235" -> 11.0D;
            case "les", "mes", "hes" -> 15.0D;
            case "po210be", "ra226be", "pu238be" -> 20.0D;
            case "balefire_gold", "flashlead", "balefire", "drx" -> 40.0D;
            default -> 15.0D;
        };
        return HbmRadiationConstants.WST * HbmRadiationConstants.BILLET * multiplier * 0.075D;
    }

    private static double rbmkWasteContamination(String id) {
        return switch (id) {
            case "les", "mes", "hes", "balefire_gold", "flashlead", "balefire", "drx" -> 40.0D;
            case "po210be", "ra226be", "pu238be" -> 25.0D;
            default -> 15.0D;
        };
    }

    private static HbmHazardData fluidContainerHazards(ItemStack stack) {
        if (stack.getItem() instanceof HbmFluidContainerItem item && item.isFilledContainer()) {
            HbmFluidDefinition fluid = HbmFluidContainerItem.fluid(stack);
            double radiation = fluidRadiation(fluid) * item.kind().capacity() / 1000.0D;
            return radiation <= 0.0D ? HbmHazardData.EMPTY : rad(radiation);
        }

        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null || !key.getNamespace().equals(ReinhardtsHBM.MOD_ID) || !key.getPath().endsWith("_bucket")) {
            return HbmHazardData.EMPTY;
        }

        String fluidName = key.getPath().substring(0, key.getPath().length() - "_bucket".length());
        Optional<HbmFluidDefinition> fluid = HbmFluids.byName(fluidName);
        double radiation = fluid.map(HbmHazardSystem::fluidRadiation).orElse(0.0D);
        return radiation <= 0.0D ? HbmHazardData.EMPTY : rad(radiation);
    }

    /**
     * Applies the same uranium material/shape multipliers used by HBM to
     * external items which publish the common uranium tags.  This is kept
     * separate from {@link #inferHazards(String)} so an external item can
     * never collide with HBM's path-only cache.
     */
    private static HbmHazardData externalUraniumHazards(ItemStack stack) {
        return EXTERNAL_URANIUM_CACHE.computeIfAbsent(stack.getItem(), unused -> computeExternalUraniumHazards(stack));
    }

    private static HbmHazardData computeExternalUraniumHazards(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key != null && key.getNamespace().equals(IMMERSIVE_ENGINEERING_MOD_ID)) {
            HbmHazardData direct = immersiveEngineeringUraniumHazards(key.getPath());
            if (!direct.isEmpty()) {
                return direct;
            }
        }

        if (hasAnyTag(stack, URANIUM_NUGGET_TAGS)) {
            return rad(HbmRadiationConstants.U * HbmRadiationConstants.NUGGET);
        }
        if (hasAnyTag(stack, URANIUM_INGOT_TAGS)) {
            return rad(HbmRadiationConstants.U * HbmRadiationConstants.INGOT);
        }
        if (hasAnyTag(stack, URANIUM_DUST_TAGS)) {
            return rad(HbmRadiationConstants.U * HbmRadiationConstants.POWDER);
        }
        if (hasAnyTag(stack, URANIUM_RAW_TAGS) || hasAnyTag(stack, URANIUM_ORE_TAGS)) {
            return rad(HbmRadiationConstants.U * HbmRadiationConstants.ORE);
        }
        if (hasAnyTag(stack, URANIUM_PLATE_TAGS)) {
            return rad(HbmRadiationConstants.U * HbmRadiationConstants.PLATE);
        }
        if (hasAnyTag(stack, URANIUM_SHEETMETAL_TAGS)) {
            // Immersive Engineering sheetmetal is crafted from four plates.
            return rad(HbmRadiationConstants.U * HbmRadiationConstants.PLATE * 4.0D);
        }
        if (hasAnyTag(stack, URANIUM_STORAGE_BLOCK_TAGS)
                || hasAnyTag(stack, URANIUM_RAW_STORAGE_BLOCK_TAGS)) {
            // Match HBM's block multiplier for both finished and raw uranium
            // storage blocks.
            return rad(HbmRadiationConstants.U * HbmRadiationConstants.BLOCK);
        }
        return HbmHazardData.EMPTY;
    }

    /**
     * Keep the IE 1.21.1 IDs explicit as a fallback in case another data pack
     * replaces a common uranium tag.  The tag path above remains the primary
     * cross-mod contract for other mods and future IE additions.
     */
    private static HbmHazardData immersiveEngineeringUraniumHazards(String path) {
        return switch (path) {
            case "nugget_uranium" -> rad(HbmRadiationConstants.U * HbmRadiationConstants.NUGGET);
            case "ingot_uranium" -> rad(HbmRadiationConstants.U * HbmRadiationConstants.INGOT);
            case "dust_uranium" -> rad(HbmRadiationConstants.U * HbmRadiationConstants.POWDER);
            case "ore_uranium", "deepslate_ore_uranium",
                    "raw_uranium" -> rad(HbmRadiationConstants.U * HbmRadiationConstants.ORE);
            case "plate_uranium" -> rad(HbmRadiationConstants.U * HbmRadiationConstants.PLATE);
            case "sheetmetal_uranium" -> rad(
                    HbmRadiationConstants.U * HbmRadiationConstants.PLATE * 4.0D
            );
            case "storage_uranium", "raw_block_uranium" -> rad(
                    HbmRadiationConstants.U * HbmRadiationConstants.BLOCK
            );
            default -> HbmHazardData.EMPTY;
        };
    }

    private static HbmHazardData inferHazards(String path) {
        String id = normalize(path);
        HbmHazardData exact = exactHazards(id);
        if (!exact.isEmpty()) {
            return exact;
        }

        double base = materialBase(id);
        if (base <= 0.0D) {
            return HbmHazardData.EMPTY;
        }

        double multiplier = shapeMultiplier(id);
        String material = cleanMaterialName(stripShape(id));
        return new HbmHazardData(
                base * multiplier,
                0.0D,
                0.0D,
                hotForMaterial(material),
                blindingForMaterial(material, multiplier)
        );
    }

    private static HbmHazardData exactHazards(String id) {
        return switch (id) {
            case "nuke_fstbmb" -> new HbmHazardData(0.0D, 0.0D, 0.01D, 0.0D, 0.0D);
            case "powder_balefire" -> rad(500.0D);
            case "cell_balefire" -> rad(50.0D);
            case "egg_balefire_shard" -> rad(HbmRadiationConstants.BF * HbmRadiationConstants.NUGGET);
            case "egg_balefire" -> rad(HbmRadiationConstants.BF);
            case "solid_fuel_bf" -> rad(1000.0D);
            case "solid_fuel_presto_bf" -> rad(2000.0D);
            case "solid_fuel_presto_triplet_bf" -> rad(6000.0D);
            case "gem_rad", "ore_sellafield_radgem" -> rad(25.0D);
            case "scrap_nuclear" -> rad(1.0D);
            case "trinitite" -> new HbmHazardData(HbmRadiationConstants.TRN, 5.0D, 0.0D, 0.0D, 0.0D);
            case "block_trinitite" -> rad(HbmRadiationConstants.TRN * HbmRadiationConstants.BLOCK);
            case "ancient_scrap", "block_corium", "block_corium_cobble" -> rad(150.0D);
            case "debris_graphite" -> new HbmHazardData(70.0D, 0.0D, 0.0D, 5.0D, 0.0D);
            case "debris_fuel" -> new HbmHazardData(500.0D, 0.0D, 0.0D, 5.0D, 0.0D);
            case "debris_metal" -> rad(5.0D);
            case "debris_concrete" -> rad(30.0D);
            case "debris_exchanger" -> rad(25.0D);
            case "debris_element" -> rad(100.0D);
            case "debris_shrapnel" -> rad(2.5D);
            case "nuclear_waste_long" -> rad(5.0D);
            case "nuclear_waste_long_tiny" -> rad(0.5D);
            case "nuclear_waste_short" -> new HbmHazardData(30.0D, 0.0D, 0.0D, 5.0D, 0.0D);
            case "nuclear_waste_short_tiny" -> new HbmHazardData(3.0D, 0.0D, 0.0D, 5.0D, 0.0D);
            case "nuclear_waste_long_depleted" -> rad(0.5D);
            case "nuclear_waste_long_depleted_tiny" -> rad(0.05D);
            case "nuclear_waste_short_depleted" -> rad(3.0D);
            case "nuclear_waste_short_depleted_tiny" -> rad(0.3D);
            case "nuclear_waste" -> new HbmHazardData(
                    HbmRadiationConstants.WST,
                    HbmRadiationConstants.WST * HbmRadiationConstants.POWDER,
                    0.0D,
                    0.0D,
                    0.0D
            );
            case "nuclear_waste_tiny" -> new HbmHazardData(
                    HbmRadiationConstants.WST * HbmRadiationConstants.NUGGET,
                    HbmRadiationConstants.WST * HbmRadiationConstants.POWDER_TINY,
                    0.0D,
                    0.0D,
                    0.0D
            );
            case "nuclear_waste_vitrified" -> new HbmHazardData(
                    HbmRadiationConstants.WSTV,
                    HbmRadiationConstants.WSTV * HbmRadiationConstants.POWDER,
                    0.0D,
                    0.0D,
                    0.0D
            );
            case "nuclear_waste_vitrified_tiny" -> new HbmHazardData(
                    HbmRadiationConstants.WSTV * HbmRadiationConstants.NUGGET,
                    HbmRadiationConstants.WSTV * HbmRadiationConstants.POWDER_TINY,
                    0.0D,
                    0.0D,
                    0.0D
            );
            case "billet_nuclear_waste" -> new HbmHazardData(
                    HbmRadiationConstants.WST * HbmRadiationConstants.BILLET,
                    HbmRadiationConstants.WST * HbmRadiationConstants.BILLET,
                    0.0D,
                    0.0D,
                    0.0D
            );
            case "block_waste", "block_waste_painted" -> rad(HbmRadiationConstants.WST * HbmRadiationConstants.BLOCK);
            case "block_waste_vitrified" -> rad(HbmRadiationConstants.WSTV * HbmRadiationConstants.BLOCK);
            case "yellow_barrel" -> rad(HbmRadiationConstants.WST * 10.0D);
            case "powder_yellowcake" -> new HbmHazardData(
                    HbmRadiationConstants.YC * HbmRadiationConstants.POWDER,
                    HbmRadiationConstants.YC * HbmRadiationConstants.POWDER,
                    0.0D,
                    0.0D,
                    0.0D
            );
            case "block_yellowcake", "block_fallout" -> rad(
                    HbmRadiationConstants.YC * HbmRadiationConstants.BLOCK * HbmRadiationConstants.POWDER
            );
            case "falloutitem" -> new HbmHazardData(
                    HbmRadiationConstants.FO * HbmRadiationConstants.POWDER,
                    HbmRadiationConstants.FO,
                    0.0D,
                    0.0D,
                    0.0D
            );
            case "fallout" -> rad(HbmRadiationConstants.FO * HbmRadiationConstants.POWDER * 2.0D);
            case "crystal_trixite" -> rad(HbmRadiationConstants.TRX * HbmRadiationConstants.CRYSTAL);
            case "powder_caesium" -> new HbmHazardData(0.0D, 0.0D, 0.0D, 3.0D, 0.0D);
            case "gadget_core", "man_core" -> rad(HbmRadiationConstants.PU239 * HbmRadiationConstants.NUGGET * 10.0D);
            case "boy_target" -> rad(HbmRadiationConstants.U235 * HbmRadiationConstants.INGOT * 2.0D);
            case "boy_bullet" -> rad(HbmRadiationConstants.U235 * HbmRadiationConstants.INGOT);
            case "mike_core" -> rad(HbmRadiationConstants.U238 * HbmRadiationConstants.NUGGET * 10.0D);
            case "tsar_core" -> rad(HbmRadiationConstants.PU239 * HbmRadiationConstants.NUGGET * 15.0D);
            case "fleija_propellant" -> new HbmHazardData(15.0D, 0.0D, 0.0D, 0.0D, 50.0D);
            case "fleija_core" -> rad(10.0D);
            case "solinium_core" -> new HbmHazardData(
                    HbmRadiationConstants.SA327 * HbmRadiationConstants.NUGGET * 8.0D,
                    0.0D,
                    0.0D,
                    0.0D,
                    45.0D
            );
            case "plate_fuel_u233" -> rad(HbmRadiationConstants.U233);
            case "plate_fuel_u235" -> rad(HbmRadiationConstants.U235);
            case "plate_fuel_mox" -> rad(HbmRadiationConstants.MOX);
            case "plate_fuel_pu239" -> rad(HbmRadiationConstants.PU239);
            case "plate_fuel_sa326" -> new HbmHazardData(HbmRadiationConstants.SA326, 0.0D, 0.0D, 0.0D, 20.0D);
            case "plate_fuel_ra226be" -> rad(HbmRadiationConstants.RA226 * 3.0D * HbmRadiationConstants.BILLET);
            case "plate_fuel_pu238be" -> rad(HbmRadiationConstants.PU238 * 3.0D * HbmRadiationConstants.BILLET);
            case "waste_plate_u233" -> rad(HbmRadiationConstants.WST * 13.0D);
            case "waste_plate_u235" -> rad(HbmRadiationConstants.WST * 10.0D);
            case "waste_plate_mox" -> rad(HbmRadiationConstants.WST * 16.0D);
            case "waste_plate_pu239" -> rad(HbmRadiationConstants.WST * 13.5D);
            case "waste_plate_sa326" -> rad(HbmRadiationConstants.WST * 10.0D);
            case "waste_plate_ra226be" -> rad(HbmRadiationConstants.PO210 * 3.0D * HbmRadiationConstants.NUGGET * 3.0D);
            case "waste_plate_pu238be" -> rad(HbmRadiationConstants.PU238 * 3.0D * HbmRadiationConstants.NUGGET);
            case "waste_natural_uranium" -> otherWaste(11.5D, 15.0D);
            case "waste_uranium" -> otherWaste(10.0D, 15.0D);
            case "waste_thorium" -> otherWaste(7.5D, 10.0D);
            case "waste_mox" -> otherWaste(10.0D, 15.0D);
            case "waste_plutonium" -> otherWaste(12.5D, 15.0D);
            case "waste_u233" -> otherWaste(10.0D, 15.0D);
            case "waste_u235" -> otherWaste(11.0D, 15.0D);
            case "waste_schrabidium" -> otherWaste(15.0D, 40.0D);
            case "waste_zfb_mox" -> otherWaste(5.0D, 10.0D);
            case "nugget_uranium_fuel" -> rad(HbmRadiationConstants.UF * HbmRadiationConstants.NUGGET);
            case "billet_uranium_fuel" -> rad(HbmRadiationConstants.UF * HbmRadiationConstants.BILLET);
            case "ingot_uranium_fuel" -> rad(HbmRadiationConstants.UF);
            case "block_uranium_fuel" -> rad(HbmRadiationConstants.UF * HbmRadiationConstants.BLOCK);
            case "billet_uzh" -> rad(HbmRadiationConstants.UZH * HbmRadiationConstants.BILLET);
            case "billet_balefire_gold" -> new HbmHazardData(
                    HbmRadiationConstants.AU198 * HbmRadiationConstants.BILLET,
                    0.0D,
                    0.0D,
                    5.0D,
                    0.0D
            );
            case "billet_flashlead" -> new HbmHazardData(
                    HbmRadiationConstants.PB209 * 1.25D * HbmRadiationConstants.BILLET,
                    0.0D,
                    0.0D,
                    7.0D,
                    50.0D
            );
            case "billet_po210be" -> rad(HbmRadiationConstants.PO210 * 3.0D * HbmRadiationConstants.BILLET);
            case "billet_ra226be" -> rad(HbmRadiationConstants.RA226 * 3.0D * HbmRadiationConstants.BILLET);
            case "billet_pu238be" -> rad(HbmRadiationConstants.PU238 * 3.0D * HbmRadiationConstants.BILLET);
            case "pellet_rtg" -> rtg(HbmRadiationConstants.PU238, 3.0D, 0.0D);
            case "pellet_rtg_radium" -> rtg(HbmRadiationConstants.RA226, 0.0D, 0.0D);
            case "pellet_rtg_weak" -> rad((HbmRadiationConstants.PU238 + HbmRadiationConstants.U238 * 2.0D) * HbmRadiationConstants.BILLET);
            case "pellet_rtg_strontium" -> rtg(HbmRadiationConstants.SR90, 0.0D, 0.0D);
            case "pellet_rtg_cobalt" -> rtg(HbmRadiationConstants.CO60, 0.0D, 0.0D);
            case "pellet_rtg_actinium" -> rtg(HbmRadiationConstants.AC227, 0.0D, 0.0D);
            case "pellet_rtg_polonium" -> rtg(HbmRadiationConstants.PO210, 3.0D, 0.0D);
            case "pellet_rtg_lead" -> rtg(HbmRadiationConstants.PB209, 7.0D, 50.0D);
            case "pellet_rtg_gold" -> rtg(HbmRadiationConstants.AU198, 5.0D, 0.0D);
            case "pellet_rtg_americium" -> rtg(HbmRadiationConstants.AM241, 0.0D, 0.0D);
            case "pellet_rtg_depleted" -> rtg(HbmRadiationConstants.NP237, 0.0D, 0.0D);
            case "pile_rod_uranium" -> rad(HbmRadiationConstants.U * HbmRadiationConstants.BILLET * 3.0D);
            case "pile_rod_pu239" -> rad(HbmRadiationConstants.PURG * HbmRadiationConstants.BILLET
                    + HbmRadiationConstants.PU239 * HbmRadiationConstants.BILLET
                    + HbmRadiationConstants.U * HbmRadiationConstants.BILLET);
            case "pile_rod_plutonium" -> rad(HbmRadiationConstants.PURG * HbmRadiationConstants.BILLET * 2.0D
                    + HbmRadiationConstants.U * HbmRadiationConstants.BILLET);
            case "pile_rod_source" -> rad(HbmRadiationConstants.RA226 * 3.0D * HbmRadiationConstants.BILLET * 3.0D);
            case "rod_zirnox_tritium" -> rad(0.001D);
            default -> HbmHazardData.EMPTY;
        };
    }

    private static HbmHazardData otherWaste(double multiplier, double contaminating) {
        return new HbmHazardData(
                HbmRadiationConstants.WST * HbmRadiationConstants.BILLET * multiplier * 0.075D,
                contaminating,
                0.0D,
                0.0D,
                0.0D
        );
    }

    private static HbmHazardData rtg(double base, double hot, double blinding) {
        return new HbmHazardData(base * HbmRadiationConstants.RTG, 0.0D, 0.0D, hot, blinding);
    }

    private static HbmHazardData rad(double radiation) {
        return radiation <= 0.0D ? HbmHazardData.EMPTY : new HbmHazardData(radiation, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    private static double shapeMultiplier(String id) {
        if (id.startsWith("nugget_")) {
            return HbmRadiationConstants.NUGGET;
        }
        if (id.startsWith("ingot_")) {
            return HbmRadiationConstants.INGOT;
        }
        if (id.startsWith("powder_tiny_") || id.startsWith("dust_tiny_") || id.endsWith("_tiny")) {
            return HbmRadiationConstants.POWDER_TINY;
        }
        if (id.startsWith("powder_") || id.startsWith("dust_") || id.equals("dust")) {
            return HbmRadiationConstants.POWDER;
        }
        if (id.startsWith("gem_")) {
            return HbmRadiationConstants.GEM;
        }
        if (id.startsWith("wire_dense_")) {
            return HbmRadiationConstants.DENSE_WIRE;
        }
        if (id.startsWith("wire_fine_") || id.startsWith("wire_")) {
            return HbmRadiationConstants.WIRE;
        }
        if (id.startsWith("bolt_")) {
            return HbmRadiationConstants.BOLT;
        }
        if (id.startsWith("billet_")) {
            return HbmRadiationConstants.BILLET;
        }
        if (id.startsWith("plate_cast_")) {
            return HbmRadiationConstants.PLATE_CAST;
        }
        if (id.startsWith("plate_welded_")) {
            return HbmRadiationConstants.PLATE_WELDED;
        }
        if (id.startsWith("plate_")) {
            return HbmRadiationConstants.PLATE;
        }
        if (id.startsWith("pipe_") || id.startsWith("pipes_")) {
            return HbmRadiationConstants.PIPE;
        }
        if (id.startsWith("shell_")) {
            return HbmRadiationConstants.SHELL;
        }
        if (id.startsWith("block_")) {
            return HbmRadiationConstants.BLOCK;
        }
        if (id.startsWith("ore_") || id.startsWith("raw_")) {
            return HbmRadiationConstants.ORE;
        }
        if (id.startsWith("crystal_")) {
            return HbmRadiationConstants.CRYSTAL;
        }
        if (id.startsWith("fragment_")) {
            return 0.25D;
        }
        return HbmRadiationConstants.INGOT;
    }

    private static double materialBase(String id) {
        String material = cleanMaterialName(stripShape(id));
        return switch (material) {
            case "co60", "cobalt60" -> HbmRadiationConstants.CO60;
            case "sr90", "strontium90" -> HbmRadiationConstants.SR90;
            case "tc99", "technetium" -> HbmRadiationConstants.TC99;
            case "i131" -> HbmRadiationConstants.I131;
            case "xe135" -> HbmRadiationConstants.XE135;
            case "cs137", "caesium" -> HbmRadiationConstants.CS137;
            case "au198" -> HbmRadiationConstants.AU198;
            case "pb209" -> HbmRadiationConstants.PB209;
            case "at209", "astatine" -> HbmRadiationConstants.AT209;
            case "po210", "polonium" -> HbmRadiationConstants.PO210;
            case "ra226" -> HbmRadiationConstants.RA226;
            case "ac227", "actinium" -> HbmRadiationConstants.AC227;
            case "th232", "thorium" -> HbmRadiationConstants.TH232;
            case "thorium_fuel" -> HbmRadiationConstants.THF;
            case "uranium" -> HbmRadiationConstants.U;
            case "u233" -> HbmRadiationConstants.U233;
            case "u235" -> HbmRadiationConstants.U235;
            case "u238" -> HbmRadiationConstants.U238;
            case "uranium_fuel" -> HbmRadiationConstants.UF;
            case "uzh" -> HbmRadiationConstants.UZH;
            case "neptunium", "np237" -> HbmRadiationConstants.NP237;
            case "neptunium_fuel" -> HbmRadiationConstants.NPF;
            case "plutonium" -> HbmRadiationConstants.PU;
            case "pu_mix" -> HbmRadiationConstants.PURG;
            case "pu238" -> HbmRadiationConstants.PU238;
            case "pu239" -> HbmRadiationConstants.PU239;
            case "pu240" -> HbmRadiationConstants.PU240;
            case "pu241" -> HbmRadiationConstants.PU241;
            case "plutonium_fuel" -> HbmRadiationConstants.PUF;
            case "am241", "americium" -> HbmRadiationConstants.AM241;
            case "am242" -> HbmRadiationConstants.AM242;
            case "am_mix" -> HbmRadiationConstants.AMRG;
            case "americium_fuel" -> HbmRadiationConstants.AMF;
            case "mox_fuel" -> HbmRadiationConstants.MOX;
            case "schrabidium", "sa326" -> HbmRadiationConstants.SA326;
            case "solinium", "sa327" -> HbmRadiationConstants.SA327;
            case "schrabidate", "sbd" -> HbmRadiationConstants.SBD;
            case "schraranium", "srn" -> HbmRadiationConstants.SRN;
            case "schrabidium_fuel", "hes", "les" -> HbmRadiationConstants.SAF;
            case "sas3" -> HbmRadiationConstants.SAS3;
            case "gh336" -> HbmRadiationConstants.GH336;
            case "mud" -> HbmRadiationConstants.MUD;
            case "waste", "nuclear_waste" -> HbmRadiationConstants.WST;
            case "waste_vitrified" -> HbmRadiationConstants.WSTV;
            case "yellowcake" -> HbmRadiationConstants.YC;
            case "balefire" -> HbmRadiationConstants.BF;
            case "trixite" -> HbmRadiationConstants.TRX;
            case "po210be" -> HbmRadiationConstants.PO210 * 3.0F;
            case "ra226be" -> HbmRadiationConstants.RA226 * 3.0F;
            case "pu238be" -> HbmRadiationConstants.PU238 * 3.0F;
            default -> 0.0D;
        };
    }

    private static double hotForMaterial(String material) {
        return switch (cleanMaterialName(material)) {
            case "co60", "i131" -> 1.0D;
            case "pu238", "po210", "polonium", "cs137", "caesium" -> 3.0D;
            case "au198" -> 5.0D;
            case "pb209" -> 7.0D;
            case "xe135" -> 10.0D;
            case "at209", "astatine" -> 20.0D;
            default -> 0.0D;
        };
    }

    private static double blindingForMaterial(String material, double multiplier) {
        return switch (cleanMaterialName(material)) {
            case "pb209", "schrabidium", "sa326", "solinium", "sa327", "schrabidate", "sbd", "schraranium", "srn" -> 50.0D;
            case "schrabidium_fuel" -> 5.0D * multiplier;
            default -> 0.0D;
        };
    }

    private static String stripShape(String id) {
        String material = id;
        String[] prefixes = {
                "ore_deepslate_",
                "ore_",
                "raw_",
                "nugget_",
                "ingot_",
                "powder_tiny_",
                "powder_",
                "dust_tiny_",
                "dust_",
                "gem_",
                "wire_fine_",
                "wire_dense_",
                "wire_",
                "bolt_",
                "billet_",
                "plate_cast_",
                "plate_welded_",
                "plate_",
                "pipe_",
                "pipes_",
                "shell_",
                "block_",
                "crystal_",
                "fragment_"
        };
        for (String prefix : prefixes) {
            if (material.startsWith(prefix)) {
                return material.substring(prefix.length());
            }
        }
        return material;
    }

    private static String cleanMaterialName(String material) {
        String clean = normalize(material);
        String[] prefixes = {"deepslate_", "nether_", "gneiss_", "sellafield_"};
        boolean changed;
        do {
            changed = false;
            for (String prefix : prefixes) {
                if (clean.startsWith(prefix)) {
                    clean = clean.substring(prefix.length());
                    changed = true;
                }
            }
        } while (changed);
        if (clean.endsWith("_scorched")) {
            clean = clean.substring(0, clean.length() - "_scorched".length());
        }
        return clean;
    }

    public static String formatRadiation(double radiation) {
        double value;
        String suffix;
        if (radiation < 1_000_000.0D) {
            value = radiation;
            suffix = "";
        } else if (radiation < 1_000_000_000.0D) {
            value = radiation * 0.000001D;
            suffix = "M";
        } else {
            value = radiation * 0.000000001D;
            suffix = "G";
        }
        return String.format(Locale.ROOT, "%.3f%s", value, suffix);
    }

    private static String formatDigamma(double digamma) {
        double millidrx = Math.round(digamma * 10_000.0D) / 10.0D;
        return String.format(Locale.ROOT, "%.1f", millidrx);
    }

    private static String normalize(String id) {
        return id.toLowerCase(Locale.ROOT).replace('-', '_');
    }

    private static String itemPath(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return key == null || !key.getNamespace().equals(ReinhardtsHBM.MOD_ID) ? "" : key.getPath();
    }

    private static String respiratoryCacheKey(ItemStack stack) {
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (key == null) {
            return "";
        }
        StringBuilder result = new StringBuilder(key.toString());
        if (stack.getItem() instanceof OreBasaltBlockItem) {
            result.append("#ore_basalt=").append(OreBasaltBlockItem.variantName(stack));
        } else if (stack.getItem() instanceof LegacyVariantBlockItem variant) {
            result.append("#legacy_block=").append(variant.variant(stack));
        } else if (stack.getItem() instanceof LegacyVariantItem variant) {
            result.append("#legacy_item=").append(variant.variant(stack).id());
        }
        return result.toString();
    }

    private static TagKey<Item>[] tags(String path) {
        @SuppressWarnings("unchecked")
        TagKey<Item>[] result = new TagKey[2];
        result[0] = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", path));
        result[1] = TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", path));
        return result;
    }

    private static boolean hasAnyTag(ItemStack stack, TagKey<Item>[] tags) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }
}
