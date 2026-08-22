package com.reinhardt.hbm.item;

import net.minecraft.world.item.Rarity;

import java.util.ArrayList;
import java.util.List;

public record HbmToolProfile(
        HbmToolTier tier,
        float attackDamage,
        float attackSpeed,
        double movementModifier,
        Rarity rarity,
        boolean miner,
        boolean depthRockBreaker,
        List<AbilityLevel<HbmToolBehavior.AreaAbility>> areaAbilities,
        List<AbilityLevel<HbmToolBehavior.HarvestAbility>> harvestAbilities,
        List<AbilityLevel<HbmToolBehavior.WeaponAbility>> weaponAbilities,
        HbmToolBehavior.SpecialBehavior specialBehavior
) {
    public HbmToolProfile {
        areaAbilities = List.copyOf(areaAbilities);
        harvestAbilities = List.copyOf(harvestAbilities);
        weaponAbilities = List.copyOf(weaponAbilities);
    }

    public static HbmToolProfile create(HbmToolTier tier, float attackDamage, float attackSpeed) {
        return new HbmToolProfile(
                tier,
                attackDamage,
                attackSpeed,
                0.0D,
                Rarity.COMMON,
                false,
                false,
                List.of(),
                List.of(),
                List.of(),
                HbmToolBehavior.SpecialBehavior.NONE
        );
    }

    public HbmToolProfile movement(double movementModifier) {
        return copy(movementModifier, this.rarity, this.miner, this.depthRockBreaker, this.areaAbilities, this.harvestAbilities, this.weaponAbilities, this.specialBehavior);
    }

    public HbmToolProfile rarity(Rarity rarity) {
        return copy(this.movementModifier, rarity, this.miner, this.depthRockBreaker, this.areaAbilities, this.harvestAbilities, this.weaponAbilities, this.specialBehavior);
    }

    public HbmToolProfile asMiner() {
        return copy(this.movementModifier, this.rarity, true, this.depthRockBreaker, this.areaAbilities, this.harvestAbilities, this.weaponAbilities, this.specialBehavior);
    }

    public HbmToolProfile canBreakDepthRock() {
        return copy(this.movementModifier, this.rarity, this.miner, true, this.areaAbilities, this.harvestAbilities, this.weaponAbilities, this.specialBehavior);
    }

    public HbmToolProfile area(HbmToolBehavior.AreaAbility ability, int level) {
        List<AbilityLevel<HbmToolBehavior.AreaAbility>> abilities = new ArrayList<>(this.areaAbilities);
        abilities.add(new AbilityLevel<>(ability, level));
        return copy(this.movementModifier, this.rarity, this.miner, this.depthRockBreaker, abilities, this.harvestAbilities, this.weaponAbilities, this.specialBehavior);
    }

    public HbmToolProfile harvest(HbmToolBehavior.HarvestAbility ability, int level) {
        List<AbilityLevel<HbmToolBehavior.HarvestAbility>> abilities = new ArrayList<>(this.harvestAbilities);
        abilities.add(new AbilityLevel<>(ability, level));
        return copy(this.movementModifier, this.rarity, this.miner, this.depthRockBreaker, this.areaAbilities, abilities, this.weaponAbilities, this.specialBehavior);
    }

    public HbmToolProfile weapon(HbmToolBehavior.WeaponAbility ability, int level) {
        List<AbilityLevel<HbmToolBehavior.WeaponAbility>> abilities = new ArrayList<>(this.weaponAbilities);
        abilities.add(new AbilityLevel<>(ability, level));
        return copy(this.movementModifier, this.rarity, this.miner, this.depthRockBreaker, this.areaAbilities, this.harvestAbilities, abilities, this.specialBehavior);
    }

    public HbmToolProfile special(HbmToolBehavior.SpecialBehavior specialBehavior) {
        return copy(this.movementModifier, this.rarity, this.miner, this.depthRockBreaker, this.areaAbilities, this.harvestAbilities, this.weaponAbilities, specialBehavior);
    }

    public boolean hasCyclableAbilities() {
        return !this.areaAbilities.isEmpty() || !this.harvestAbilities.isEmpty();
    }

    private HbmToolProfile copy(
            double movementModifier,
            Rarity rarity,
            boolean miner,
            boolean depthRockBreaker,
            List<AbilityLevel<HbmToolBehavior.AreaAbility>> areaAbilities,
            List<AbilityLevel<HbmToolBehavior.HarvestAbility>> harvestAbilities,
            List<AbilityLevel<HbmToolBehavior.WeaponAbility>> weaponAbilities,
            HbmToolBehavior.SpecialBehavior specialBehavior
    ) {
        return new HbmToolProfile(
                this.tier,
                this.attackDamage,
                this.attackSpeed,
                movementModifier,
                rarity,
                miner,
                depthRockBreaker,
                areaAbilities,
                harvestAbilities,
                weaponAbilities,
                specialBehavior
        );
    }

    public record AbilityLevel<T extends Enum<T>>(T ability, int level) {
    }
}
