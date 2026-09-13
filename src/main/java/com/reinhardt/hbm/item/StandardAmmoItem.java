package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class StandardAmmoItem extends LegacyVariantItem {
    public StandardAmmoItem(Properties properties) {
        super(properties, "ammo_standard", variants(
                "bmg50_sp",
                "bmg50_fmj",
                "bmg50_jhp",
                "bmg50_ap",
                "bmg50_du",
                "r556_sp",
                "r556_fmj",
                "r556_jhp",
                "r556_ap",
                "p9_sp",
                "p9_fmj",
                "p9_jhp",
                "p9_ap",
                "tau_uranium",
                "rocket_he",
                "rocket_heat",
                "rocket_demo",
                "rocket_inc",
                "rocket_phosphorus",
                "flame_diesel",
                "nuke_demo",
                "nuke_standard",
                "nuke_high",
                "nuke_tots",
                "nuke_hive",
                "nuke_balefire"
        ));
    }

    public static Bmg50Type bmg50Type(ItemStack stack) {
        StandardAmmoType type = standardType(stack);
        return type.family == AmmoFamily.BMG50 ? Bmg50Type.byId(type.id()) : Bmg50Type.NONE;
    }

    public static StandardAmmoType standardType(ItemStack stack) {
        if (stack.getItem() instanceof StandardAmmoItem ammoItem) {
            return StandardAmmoType.byId(ammoItem.variant(stack).id());
        }
        return StandardAmmoType.NONE;
    }

    public static ItemStack stackFor(Item item, Bmg50Type type) {
        return LegacyVariantItem.stackFor(item, type.id());
    }

    public static ItemStack stackFor(Item item, StandardAmmoType type) {
        return LegacyVariantItem.stackFor(item, type.id());
    }

    public enum AmmoFamily {
        NONE,
        BMG50,
        R556,
        P9,
        TAU,
        ROCKET_ML,
        FLAME,
        NUKE,
        LEGACY_BULLET,
        FLARE
    }

    public enum StandardAmmoType {
        NONE("none", AmmoFamily.NONE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        BMG50_SP("bmg50_sp", AmmoFamily.BMG50, 10.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        BMG50_FMJ("bmg50_fmj", AmmoFamily.BMG50, 8.0F, 0.8F, false, true, 7.0F, 0.10F, 1.25F),
        BMG50_JHP("bmg50_jhp", AmmoFamily.BMG50, 15.0F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        BMG50_AP("bmg50_ap", AmmoFamily.BMG50, 12.5F, 1.25F, true, false, 17.5F, 0.15F, 1.25F),
        BMG50_DU("bmg50_du", AmmoFamily.BMG50, 15.0F, 1.5F, true, false, 21.0F, 0.25F, 1.25F),
        R556_SP("r556_sp", AmmoFamily.R556, 10.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        R556_FMJ("r556_fmj", AmmoFamily.R556, 8.0F, 0.8F, false, true, 4.0F, 0.10F, 1.25F),
        R556_JHP("r556_jhp", AmmoFamily.R556, 15.0F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        R556_AP("r556_ap", AmmoFamily.R556, 12.5F, 1.25F, true, false, 10.0F, 0.15F, 1.25F),
        P9_SP("p9_sp", AmmoFamily.P9, 5.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        P9_FMJ("p9_fmj", AmmoFamily.P9, 4.0F, 0.8F, false, true, 2.0F, 0.10F, 1.25F),
        P9_JHP("p9_jhp", AmmoFamily.P9, 7.5F, 1.5F, false, true, 0.0F, -0.25F, 1.50F),
        P9_AP("p9_ap", AmmoFamily.P9, 6.25F, 1.25F, true, false, 5.0F, 0.15F, 1.25F),
        TAU_URANIUM("tau_uranium", AmmoFamily.TAU, 35.0F, 1.0F, true, false, 0.0F, 0.0F, 1.25F),
        ROCKET_HE("rocket_he", AmmoFamily.ROCKET_ML, 30.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        ROCKET_HEAT("rocket_heat", AmmoFamily.ROCKET_ML, 15.0F, 0.5F, false, true, 5.0F, 0.20F, 1.0F),
        ROCKET_DEMO("rocket_demo", AmmoFamily.ROCKET_ML, 22.5F, 0.75F, false, true, 0.0F, 0.0F, 1.0F),
        ROCKET_INC("rocket_inc", AmmoFamily.ROCKET_ML, 22.5F, 0.75F, false, true, 0.0F, 0.0F, 1.0F),
        ROCKET_PHOSPHORUS("rocket_phosphorus", AmmoFamily.ROCKET_ML, 22.5F, 0.75F, false, true, 0.0F, 0.0F, 1.0F),
        FLAME_DIESEL("flame_diesel", AmmoFamily.FLAME, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        NUKE_DEMO("nuke_demo", AmmoFamily.NUKE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        NUKE_STANDARD("nuke_standard", AmmoFamily.NUKE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        NUKE_HIGH("nuke_high", AmmoFamily.NUKE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        NUKE_TOTS("nuke_tots", AmmoFamily.NUKE, 0.35F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        NUKE_HIVE("nuke_hive", AmmoFamily.NUKE, 0.25F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        NUKE_BALEFIRE("nuke_balefire", AmmoFamily.NUKE, 2.5F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        // Appended so the ids already stored by legacy_bullet entities retain
        // their 1.21.1 ordinals.  These are precisely the first accepted
        // magazine configurations used by the 1.7.10 skeleton gun pools.
        STONE("stone", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.0F),
        M357_BP("m357_bp", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        M44_BP("m44_bp", AmmoFamily.LEGACY_BULLET, 0.0F, 0.75F, false, true, 0.0F, 0.0F, 1.25F),
        G12_BP("g12_bp", AmmoFamily.LEGACY_BULLET, 0.0F, 0.09375F, false, true, 0.0F, 0.0F, 1.25F),
        G26_FLARE("g26_flare", AmmoFamily.FLARE, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        P22_SP("p22_sp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        R762_SP("r762_sp", AmmoFamily.LEGACY_BULLET, 0.0F, 1.0F, false, true, 0.0F, 0.0F, 1.25F),
        R762_FMJ("r762_fmj", AmmoFamily.LEGACY_BULLET, 0.0F, 0.8F, false, true, 5.0F, 0.10F, 1.25F);

        private final String id;
        private final AmmoFamily family;
        private final float damage;
        private final float damageMultiplier;
        private final boolean penetrates;
        private final boolean damageFalloffByPenetration;
        private final float armorThresholdNegation;
        private final float armorPiercingPercent;
        private final float headshotMultiplier;

        StandardAmmoType(
                String id,
                AmmoFamily family,
                float damage,
                float damageMultiplier,
                boolean penetrates,
                boolean damageFalloffByPenetration,
                float armorThresholdNegation,
                float armorPiercingPercent,
                float headshotMultiplier
        ) {
            this.id = id;
            this.family = family;
            this.damage = damage;
            this.damageMultiplier = damageMultiplier;
            this.penetrates = penetrates;
            this.damageFalloffByPenetration = damageFalloffByPenetration;
            this.armorThresholdNegation = armorThresholdNegation;
            this.armorPiercingPercent = armorPiercingPercent;
            this.headshotMultiplier = headshotMultiplier;
        }

        public String id() {
            return id;
        }

        public AmmoFamily family() {
            return family;
        }

        public float damage() {
            return damage;
        }

        public float damageMultiplier() {
            return damageMultiplier;
        }

        public boolean penetrates() {
            return penetrates;
        }

        public boolean damageFalloffByPenetration() {
            return damageFalloffByPenetration;
        }

        public float armorThresholdNegation() {
            return armorThresholdNegation;
        }

        public float armorPiercingPercent() {
            return armorPiercingPercent;
        }

        public float headshotMultiplier() {
            return headshotMultiplier;
        }

        public float ricochetAngle() {
            return 5.0F;
        }

        public int maxRicochetCount() {
            return 2;
        }

        /** BulletConfig projectile count from the 1.7.10 first magazine entry. */
        public int projectileCount() {
            return this == G12_BP ? 8 : 1;
        }

        /** BulletConfig spread before the receiver's own spread terms. */
        public float projectileSpread() {
            return switch (this) {
                case STONE -> 0.025F;
                case G12_BP -> 0.035F;
                default -> 0.0F;
            };
        }

        public double projectileSpeed() {
            return switch (family) {
                case ROCKET_ML -> 0.0D;
                case FLAME -> 1.0D;
                case FLARE -> 2.0D;
                default -> 10.0D;
            };
        }

        public double projectileGravity() {
            return switch (family) {
                case FLAME -> 0.02D;
                case FLARE -> 0.015D;
                default -> 0.0D;
            };
        }

        public int projectileLifetime() {
            return switch (family) {
                case FLAME, FLARE -> 100;
                case ROCKET_ML -> 300;
                default -> 30;
            };
        }

        public int selfDamageDelay() {
            return family == AmmoFamily.FLAME ? 20 : 2;
        }

        public static StandardAmmoType byId(String id) {
            for (StandardAmmoType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return NONE;
        }
    }

    public enum Bmg50Type {
        NONE(StandardAmmoType.NONE),
        SP(StandardAmmoType.BMG50_SP),
        FMJ(StandardAmmoType.BMG50_FMJ),
        JHP(StandardAmmoType.BMG50_JHP),
        AP(StandardAmmoType.BMG50_AP),
        DU(StandardAmmoType.BMG50_DU);

        private final StandardAmmoType standardType;

        Bmg50Type(StandardAmmoType standardType) {
            this.standardType = standardType;
        }

        public String id() {
            return this.standardType.id();
        }

        public float damage() {
            return this.standardType.damage();
        }

        public boolean penetrates() {
            return this.standardType.penetrates();
        }

        public StandardAmmoType standardType() {
            return this.standardType;
        }

        public static Bmg50Type byId(String id) {
            for (Bmg50Type type : values()) {
                if (type.id().equals(id)) {
                    return type;
                }
            }
            return NONE;
        }

        public static Bmg50Type byOrdinal(int ordinal) {
            Bmg50Type[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
        }
    }
}
