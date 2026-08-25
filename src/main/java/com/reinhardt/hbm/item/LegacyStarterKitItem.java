package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.fluid.HbmFluidDefinition;
import com.reinhardt.hbm.registry.HbmFluids;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Direct port of 1.7.10 ItemStarterKit's content lists and armor replacement
 * behaviour. Every non-vanilla output is resolved by its original registry id
 * at use time so a missing recursive dependency is reported instead of being
 * silently replaced by a generic item.
 */
public final class LegacyStarterKitItem extends Item {
    private record Entry(String id, int count) {
    }

    private record Kit(List<Entry> entries, int hazmatTier, boolean warning, boolean armorWarning) {
    }

    private final String id;

    public LegacyStarterKitItem(Properties properties, String id) {
        super(properties.stacksTo(1));
        this.id = id;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }
        Kit kit = kitFor(id);
        if (kit == null) {
            return InteractionResultHolder.fail(stack);
        }
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        List<String> missing = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();
        for (Entry entry : kit.entries()) {
            ItemStack output = output(entry);
            if (output.isEmpty()) {
                missing.add(entry.id());
                continue;
            }
            outputs.add(output);
        }
        if (kit.hazmatTier() >= 0) {
            validateHazmat(kit.hazmatTier(), missing);
        }
        if (!missing.isEmpty()) {
            serverPlayer.displayClientMessage(Component.translatable("item.reinhardtshbm.starter_kit.missing", String.join(", ", missing)).withStyle(ChatFormatting.RED), false);
            return InteractionResultHolder.fail(stack);
        }
        for (ItemStack output : outputs) {
            give(serverPlayer, output);
        }
        if (id.equals("stealth_boy")) {
            serverPlayer.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.INVISIBILITY, 30 * 20, 1, true, false));
        }
        if (kit.hazmatTier() >= 0) {
            equipHazmat(serverPlayer, kit.hazmatTier());
        }
        level.playSound(null, player.blockPosition(), HbmSoundEvents.ITEM_UNPACK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        Kit kit = kitFor(id);
        if (kit == null) {
            return;
        }
        if (kit.warning()) {
            tooltip.add(Component.translatable("item.reinhardtshbm.starter_kit.empty_inventory").withStyle(ChatFormatting.YELLOW));
        }
        if (kit.armorWarning()) {
            tooltip.add(Component.translatable("item.reinhardtshbm.starter_kit.displaces_armor").withStyle(ChatFormatting.GRAY));
        }
    }

    private static void validateHazmat(int tier, List<String> missing) {
        String suffix = switch (tier) {
            case 1 -> "_red";
            case 2 -> "_grey";
            default -> "";
        };
        for (String id : List.of("hazmat_helmet" + suffix, "hazmat_plate" + suffix, "hazmat_legs" + suffix, "hazmat_boots" + suffix)) {
            if (resolve(id) == net.minecraft.world.item.Items.AIR) {
                missing.add(id);
            }
        }
    }

    private static void equipHazmat(ServerPlayer player, int tier) {
        String suffix = switch (tier) {
            case 1 -> "_red";
            case 2 -> "_grey";
            default -> "";
        };
        equip(player, EquipmentSlot.HEAD, "hazmat_helmet" + suffix);
        equip(player, EquipmentSlot.CHEST, "hazmat_plate" + suffix);
        equip(player, EquipmentSlot.LEGS, "hazmat_legs" + suffix);
        equip(player, EquipmentSlot.FEET, "hazmat_boots" + suffix);
    }

    private static void equip(ServerPlayer player, EquipmentSlot slot, String id) {
        Item expected = resolve(id);
        ItemStack old = player.getItemBySlot(slot);
        if (!old.isEmpty()) {
            player.drop(old, false);
        }
        player.setItemSlot(slot, new ItemStack(expected));
    }

    private static ItemStack output(Entry entry) {
        String[] parts = entry.id().split("@", 2);
        Item item = resolve(parts[0]);
        if (item == net.minecraft.world.item.Items.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack;
        if (parts.length == 1) {
            stack = new ItemStack(item);
        } else if (item instanceof HbmFluidContainerItem container) {
            HbmFluidDefinition fluid = HbmFluids.byName(parts[1]).orElse(HbmFluids.none());
            if (fluid.isNone() || !container.isFilledContainer()) {
                return ItemStack.EMPTY;
            }
            stack = container.filledStack(fluid);
        } else if (item instanceof LegacyVariantItem variant) {
            stack = LegacyVariantItem.stackFor(variant, parts[1]);
        } else {
            return ItemStack.EMPTY;
        }
        stack.setCount(entry.count());
        return stack;
    }

    private static void give(Player player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static Item resolve(String id) {
        if (id.startsWith("minecraft:")) {
            return BuiltInRegistries.ITEM.get(ResourceLocation.parse(id));
        }
        return BuiltInRegistries.ITEM.get(ReinhardtsHBM.id(id));
    }

    private static Entry e(String id, int count) {
        return new Entry(id, count);
    }

    private static Kit kitFor(String id) {
        return switch (id) {
            case "nuke_starter_kit" -> new Kit(List.of(
                    e("ingot_uranium", 32), e("powder_yellowcake", 32), e("machine_press", 1),
                    e("machine_blast_furnace", 1), e("machine_gascent", 1), e("machine_reactor_breeding", 1),
                    e("machine_assembly_machine", 1), e("machine_chemical_plant", 1), e("machine_reactor_small_new", 1),
                    e("machine_turbine", 2), e("radaway", 8), e("radx", 2), e("stamp_titanium_flat", 3),
                    e("ingot_steel", 64), e("ingot_lead", 64), e("ingot_copper", 64), e("gas_mask_m65", 1), e("geiger_counter", 1)
            ), 1, true, true);
            case "nuke_advanced_kit" -> new Kit(List.of(
                    e("powder_yellowcake", 64), e("powder_plutonium", 64), e("ingot_steel", 64), e("ingot_copper", 64),
                    e("ingot_tungsten", 64), e("ingot_lead", 64), e("ingot_polymer", 64), e("machine_blast_furnace", 3),
                    e("machine_gascent", 3), e("machine_centrifuge", 2), e("machine_uf6_tank", 2), e("machine_puf6_tank", 2),
                    e("machine_reactor_breeding", 2), e("machine_reactor_small_new", 4), e("machine_turbine", 4), e("machine_radgen", 1),
                    e("machine_rtg_grey", 1), e("machine_assembly_machine", 3), e("machine_chemical_plant", 2), e("machine_fluidtank", 1),
                    e("pellet_rtg", 3), e("pellet_rtg_weak", 3), e("cell_empty", 32), e("rod_empty", 32),
                    e("fluid_barrel_full@coolant", 4), e("radaway_strong", 4), e("radx", 4), e("pill_iodine", 1),
                    e("geiger_counter", 1), e("survey_scanner", 1), e("gas_mask_m65", 1)
            ), 2, true, true);
            case "nuke_commercially_kit" -> new Kit(List.of(
                    e("machine_reactor_small_new", 8), e("machine_reactor_breeding", 8), e("machine_fluidtank", 8),
                    e("billet_pu238be", 40), e("ingot_u233", 40), e("ingot_uranium_fuel", 32), e("ingot_plutonium_fuel", 16),
                    e("ingot_mox_fuel", 8), e("inf_water_mk2", 3), e("rod_empty", 64), e("rod_dual_empty", 64),
                    e("rod_quad_empty", 64), e("fluid_tank_lead_empty", 64), e("fluid_barrel_empty", 64), e("barrel_steel", 16),
                    e("plate_iron", 64), e("minecraft:white_dye", 64), e("radaway_flush", 8), e("iv_blood", 8),
                    e("pill_iodine", 8), e("gas_mask_filter_combo", 3)
            ), 2, true, true);
            case "gadget_kit" -> new Kit(List.of(e("nuke_gadget", 1), e("early_explosive_lenses", 4), e("gadget_wireing", 1), e("gadget_core", 1)), 0, true, true);
            case "boy_kit" -> new Kit(List.of(e("nuke_boy", 1), e("boy_shielding", 1), e("boy_target", 1), e("boy_bullet", 1), e("boy_propellant", 1), e("boy_igniter", 1)), 0, true, true);
            case "man_kit" -> new Kit(List.of(e("nuke_man", 1), e("early_explosive_lenses", 4), e("man_igniter", 1), e("man_core", 1)), 0, true, true);
            case "mike_kit" -> new Kit(List.of(e("nuke_mike", 1), e("explosive_lenses", 4), e("man_core", 1), e("mike_core", 1), e("mike_deut", 1), e("mike_cooling_unit", 1)), 0, true, true);
            case "tsar_kit" -> new Kit(List.of(e("nuke_tsar", 1), e("explosive_lenses", 4), e("man_core", 1), e("tsar_core", 1)), 0, true, true);
            case "multi_kit" -> new Kit(List.of(e("bomb_multi", 6), e("minecraft:tnt", 26), e("minecraft:gunpowder", 2), e("pellet_cluster", 2), e("powder_fire", 2), e("powder_poison", 2), e("pellet_gas", 2)), -1, true, false);
            case "custom_kit" -> new Kit(List.of(e("nuke_custom", 1), e("custom_tnt", 6), e("custom_nuke", 4), e("custom_hydro", 2), e("custom_amat", 2), e("custom_dirty", 3), e("custom_schrab", 1), e("custom_fall", 1)), -1, false, false);
            case "fleija_kit" -> new Kit(List.of(e("nuke_fleija", 1), e("fleija_igniter", 2), e("fleija_propellant", 3), e("fleija_core", 6)), 2, true, true);
            case "solinium_kit" -> new Kit(List.of(e("nuke_solinium", 1), e("solinium_igniter", 4), e("solinium_propellant", 4), e("solinium_core", 1)), 1, true, true);
            case "prototype_kit" -> new Kit(List.of(e("nuke_prototype", 1), e("igniter", 1), e("cell_sas3", 4), e("rod_quad@uranium", 4), e("rod_quad@lead", 4), e("rod_quad@np237", 2)), 2, true, true);
            case "missile_kit" -> new Kit(List.of(
                    e("launch_pad", 1), e("designator", 1), e("designator_range", 1), e("designator_manual", 1),
                    e("missile_generic", 1), e("missile_strong", 1), e("missile_burst", 1), e("missile_incendiary", 1),
                    e("missile_incendiary_strong", 1), e("missile_inferno", 1), e("missile_cluster", 1), e("missile_cluster_strong", 1),
                    e("missile_rain", 1), e("missile_buster", 1), e("missile_buster_strong", 1), e("missile_drill", 1),
                    e("missile_nuclear", 1), e("missile_nuclear_cluster", 1), e("missile_volcano", 1), e("missile_doomsday", 1),
                    e("missile_taint", 1), e("missile_micro", 1), e("missile_bhole", 1), e("missile_schrabidium", 1), e("missile_emp", 1)
            ), -1, true, false);
            case "stealth_boy" -> new Kit(List.of(), -1, false, false);
            case "euphemium_kit" -> new Kit(List.of(e("euphemium_helmet", 1), e("euphemium_plate", 1), e("euphemium_legs", 1), e("euphemium_boots", 1)), -1, false, false);
            case "hazmat_kit" -> new Kit(List.of(), 0, false, true);
            case "hazmat_red_kit" -> new Kit(List.of(), 1, false, false);
            case "hazmat_grey_kit" -> new Kit(List.of(), 2, false, false);
            default -> null;
        };
    }
}
