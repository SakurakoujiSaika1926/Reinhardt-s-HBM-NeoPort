package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.ItemLike;

public enum HbmToolTier implements Tier {
    WOOD_COMPAT(0, 59, 2.0F, 0.0F, 15, "minecraft:stick"),
    PIPE_LEAD(1, 250, 1.5F, 3.0F, 25, "pipe_lead"),
    BOTTLE_OPENER(1, 250, 1.5F, 0.5F, 200, "plate_steel"),
    SCHRABIDIUM_HAMMER(3, 0, 50.0F, 999_999_996.0F, 200, "block_schrabidium"),
    SCHRABIDIUM(4, 10000, 50.0F, 100.0F, 200, "ingot_schrabidium"),
    CHAINSAW(3, 1500, 50.0F, 22.0F, 0, "ingot_steel"),
    STEEL(2, 500, 7.5F, 2.0F, 10, "ingot_steel"),
    TITANIUM(2, 750, 9.0F, 2.5F, 15, "ingot_titanium"),
    ALLOY(3, 2000, 15.0F, 5.0F, 5, "ingot_advanced_alloy"),
    CMB(4, 8500, 40.0F, 55.0F, 100, "ingot_combine_steel"),
    ELEC(2, 500000, 30.0F, 12.0F, 2, "battery_advanced"),
    DESH(2, 0, 7.5F, 2.0F, 10, "ingot_desh"),
    COBALT(4, 750, 9.0F, 2.5F, 15, "ingot_cobalt"),
    COBALT_DECORATED(4, 1000, 15.0F, 2.5F, 25, "ingot_cobalt"),
    STARMETAL(3, 1000, 20.0F, 2.5F, 30, "ingot_starmetal"),
    BISMUTH(4, 0, 50.0F, 0.0F, 200, "ingot_bismuth"),
    VOLCANIC(4, 0, 50.0F, 0.0F, 200, "ingot_bismuth"),
    CHLOROPHYTE(5, 0, 50.0F, 0.0F, 200, "powder_chlorophyte"),
    MESE(6, 0, 50.0F, 0.0F, 200, "plate_paa"),
    DWARVEN(2, 250, 4.0F, 0.0F, 10, "ingot_copper"),
    METEORITE(4, 0, 50.0F, 0.0F, 200, "plate_paa"),
    SHIMMER(1, 0, 25.0F, 26.0F, 200, "shimmer_axe_head"),
    STONE_COMPAT(1, 132, 4.0F, 1.0F, 5, "dust"),
    DIAMOND_COMPAT(3, 1561, 8.0F, 3.0F, 10, "minecraft:diamond");

    private final int level;
    private final int uses;
    private final float speed;
    private final float damageBonus;
    private final int enchantmentValue;
    private final String repairItemId;
    private Ingredient repairIngredient;

    HbmToolTier(int level, int uses, float speed, float damageBonus, int enchantmentValue, String repairItemId) {
        this.level = level;
        this.uses = uses;
        this.speed = speed;
        this.damageBonus = damageBonus;
        this.enchantmentValue = enchantmentValue;
        this.repairItemId = repairItemId;
    }

    @Override
    public int getUses() {
        return this.uses;
    }

    @Override
    public float getSpeed() {
        return this.speed;
    }

    @Override
    public float getAttackDamageBonus() {
        return this.damageBonus;
    }

    @Override
    public TagKey<Block> getIncorrectBlocksForDrops() {
        if (this.level <= 0) {
            return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
        }
        if (this.level == 1) {
            return BlockTags.INCORRECT_FOR_STONE_TOOL;
        }
        if (this.level == 2) {
            return BlockTags.INCORRECT_FOR_IRON_TOOL;
        }
        if (this.level == 3) {
            return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
        }
        return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public Ingredient getRepairIngredient() {
        if (this.repairIngredient == null) {
            ResourceLocation repairId = this.repairItemId.indexOf(':') >= 0
                    ? ResourceLocation.parse(this.repairItemId)
                    : ReinhardtsHBM.id(this.repairItemId);
            this.repairIngredient = BuiltInRegistries.ITEM
                    .getOptional(repairId)
                    .map(item -> Ingredient.of((ItemLike) item))
                    .orElse(Ingredient.EMPTY);
        }
        return this.repairIngredient;
    }
}
