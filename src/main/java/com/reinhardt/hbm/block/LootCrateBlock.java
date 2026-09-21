package com.reinhardt.hbm.block;

import com.mojang.serialization.MapCodec;
import com.reinhardt.hbm.integration.tacz.TaczWeaponCrateCompat;
import com.reinhardt.hbm.item.LegacyVariantItem;
import com.reinhardt.hbm.item.UniversalGrenadeItem;
import com.reinhardt.hbm.registry.HbmItems;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.ArrayList;
import java.util.List;

/** The five weighted, crowbar-opened falling crates from BlockCrate. */
public final class LootCrateBlock extends FallingBlock {
    private final MapCodec<LootCrateBlock> codec;
    private final Kind kind;

    public LootCrateBlock(Properties properties, Kind kind) {
        super(properties);
        this.kind = kind;
        this.codec = simpleCodec(nextProperties -> new LootCrateBlock(nextProperties, kind));
    }

    @Override
    protected MapCodec<? extends FallingBlock> codec() {
        return this.codec;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (!CrateBlockSupport.isCrowbar(stack)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide) {
            CrateBlockSupport.open(level, pos, createDrops(level, level.random));
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }

    private List<ItemStack> createDrops(Level level, RandomSource random) {
        List<ItemStack> pool = createPool();
        if (this.kind == Kind.RED) {
            return pool.stream().map(ItemStack::copy).toList();
        }
        if (pool.isEmpty()) {
            return List.of();
        }

        int count = 3 + random.nextInt(3);
        if (this.kind == Kind.WEAPON) {
            count = 1 + random.nextInt(2);
            if (random.nextInt(100) == 34) {
                count = 25;
            }
        }
        List<ItemStack> drops = new ArrayList<>(count);
        if (this.kind == Kind.WEAPON) {
            TaczWeaponCrateCompat.addRandomDrops(level.registryAccess(), random, pool, count, drops);
            return drops;
        }
        for (int i = 0; i < count; i++) {
            drops.add(pool.get(random.nextInt(pool.size())).copy());
        }
        return drops;
    }

    private List<ItemStack> createPool() {
        List<ItemStack> pool = new ArrayList<>();
        switch (this.kind) {
            case SUPPLY -> addSupplyPool(pool);
            case WEAPON -> {
                // The 1.7.10 pool contained firearms, which are retired in 1.21.1 in
                // favour of TACZ. Keep the crate useful with the modern HBM weapons
                // that are still registered locally; an empty pool would make the
                // crowbar interaction silently remove the crate without any drops.
                add(pool, "weapon_pipe_lead", 10);
                add(pool, "reer_graar", 7);
                add(pool, "boltgun", 5);
                add(pool, "chainsaw", 5);
                add(pool, "crucible", 2);
            }
            case LEAD -> addLeadPool(pool);
            case METAL -> addMetalPool(pool);
            case RED -> addRedPool(pool);
        }
        return pool;
    }

    private static void addSupplyPool(List<ItemStack> pool) {
        add(pool, CrateBlockSupport.stack("syringe_metal_stimpak"), 10);
        add(pool, CrateBlockSupport.stack("syringe_antidote"), 5);
        add(pool, UniversalGrenadeItem.make(
                UniversalGrenadeItem.Shell.FRAG,
                UniversalGrenadeItem.Filling.HE,
                UniversalGrenadeItem.Fuze.S3,
                UniversalGrenadeItem.Extra.FRAG_SLEEVE
        ), 8);
        add(pool, UniversalGrenadeItem.make(
                UniversalGrenadeItem.Shell.STICK,
                UniversalGrenadeItem.Filling.HE,
                UniversalGrenadeItem.Fuze.IMPACT,
                null
        ), 6);
        add(pool, UniversalGrenadeItem.make(
                UniversalGrenadeItem.Shell.FRAG,
                UniversalGrenadeItem.Filling.INC,
                UniversalGrenadeItem.Fuze.S7,
                null
        ), 4);
    }

    private static void addLeadPool(List<ItemStack> pool) {
        add(pool, "ingot_uranium", 10);
        add(pool, "ingot_u238", 8);
        add(pool, "ingot_plutonium", 7);
        add(pool, "ingot_pu240", 6);
        add(pool, "ingot_neptunium", 7);
        add(pool, "ingot_uranium_fuel", 8);
        add(pool, "ingot_plutonium_fuel", 7);
        add(pool, "ingot_mox_fuel", 6);
        add(pool, "nugget_uranium", 10);
        add(pool, "nugget_u238", 8);
        add(pool, "nugget_plutonium", 7);
        add(pool, "nugget_pu240", 6);
        add(pool, "nugget_neptunium", 7);
        add(pool, "nugget_uranium_fuel", 8);
        add(pool, "nugget_plutonium_fuel", 7);
        add(pool, "nugget_mox_fuel", 6);
        add(pool, "cell_deuterium", 8);
        add(pool, "cell_tritium", 8);
        add(pool, "cell_uf6", 8);
        add(pool, "cell_puf6", 8);
        add(pool, "pellet_rtg", 6);
        add(pool, "pellet_rtg_weak", 7);
        add(pool, "powder_yellowcake", 10);
    }

    private static void addMetalPool(List<ItemStack> pool) {
        add(pool, "machine_press", 10);
        add(pool, "machine_reactor_breeding", 6);
        add(pool, "machine_wood_burner", 10);
        add(pool, "machine_diesel", 8);
        add(pool, "machine_rtg_grey", 4);
        add(pool, "red_pylon", 9);
        add(pool, LegacyVariantItem.stackFor(HbmItems.BATTERY_PACK.get(), "battery_lead"), 10);
        add(pool, "machine_electric_furnace_off", 8);
        add(pool, "machine_assembly_machine", 10);
        add(pool, "machine_fluidtank", 7);
        add(pool, "centrifuge_element", 6);
        add(pool, "motor", 8);
        add(pool, "coil_tungsten", 7);
        add(pool, "photo_panel", 3);
        add(pool, "coil_copper", 10);
        add(pool, "blade_titanium", 3);
        add(pool, "piston_selenium", 6);
    }

    private static void addRedPool(List<ItemStack> pool) {
        add(pool, "mysteryshovel", 1);
        add(pool, "battery_spark", 1);
        add(pool, "bottle_sparkle", 1);
        add(pool, "bottle_rad", 1);
        add(pool, "ring_starmetal", 1);
        add(pool, "flame_pony", 1);
        add(pool, "ntm_dirt", 1);
        add(pool, "broadcaster_pc", 1);
    }

    private static void add(List<ItemStack> pool, String id, int weight) {
        add(pool, CrateBlockSupport.stack(id), weight);
    }

    private static void add(List<ItemStack> pool, ItemStack stack, int weight) {
        if (stack.isEmpty()) {
            return;
        }
        for (int i = 0; i < weight; i++) {
            pool.add(stack.copy());
        }
    }

    public enum Kind {
        SUPPLY,
        WEAPON,
        LEAD,
        METAL,
        RED
    }
}
