package com.reinhardt.hbm.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class TurretAmmoItem extends LegacyVariantItem {
    public TurretAmmoItem(Properties properties) {
        super(properties, "ammo_shell", variants(
                "stock",
                "explosive",
                "apfsds_t",
                "apfsds_du",
                "w9"
        ));
    }

    public static AmmoType ammoType(ItemStack stack) {
        if (stack.getItem() instanceof TurretAmmoItem ammoItem) {
            String id = ammoItem.variant(stack).id();
            return AmmoType.byId(id);
        }
        return AmmoType.NONE;
    }

    public static ItemStack stackFor(Item item, AmmoType type) {
        return LegacyVariantItem.stackFor(item, type.id());
    }

    public enum AmmoType {
        NONE("none", 0.0F, 0.0F, false, true),
        STOCK("stock", 50.0F, 10.0F, false, true),
        EXPLOSIVE("explosive", 75.0F, 10.0F, false, true),
        APFSDS_T("apfsds_t", 100.0F, 0.0F, true, true),
        APFSDS_DU("apfsds_du", 125.0F, 0.0F, true, false),
        W9("w9", 125.0F, 0.0F, false, true);

        private final String id;
        private final float damage;
        private final float explosionRadius;
        private final boolean penetrates;
        private final boolean damageFalloffByPenetration;

        AmmoType(String id, float damage, float explosionRadius, boolean penetrates, boolean damageFalloffByPenetration) {
            this.id = id;
            this.damage = damage;
            this.explosionRadius = explosionRadius;
            this.penetrates = penetrates;
            this.damageFalloffByPenetration = damageFalloffByPenetration;
        }

        public String id() {
            return this.id;
        }

        public float damage() {
            return this.damage;
        }

        public float explosionRadius() {
            return this.explosionRadius;
        }

        public boolean penetrates() {
            return this.penetrates;
        }

        public boolean damageFalloffByPenetration() {
            return this.damageFalloffByPenetration;
        }

        public static AmmoType byId(String id) {
            for (AmmoType type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return NONE;
        }

        public static AmmoType byOrdinal(int ordinal) {
            AmmoType[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : NONE;
        }
    }
}
