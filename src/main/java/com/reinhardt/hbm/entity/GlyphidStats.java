package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.registry.HbmDamageTypes;
import net.minecraft.world.damagesource.DamageSource;

/** The default 1.7.10 Nuclear Tech glyphid stat table and damage handling. */
public final class GlyphidStats {
    public record StatBundle(double health, double speed, double damage,
                             float damageThreshold, float thresholdPerArmor,
                             float resistanceMultiplier) {
    }

    private static final StatBundle GRUNT = new StatBundle(20.0D, 1.0D, 2.0D, 0.0F, 1.0F, 0.1F);
    private static final StatBundle BOMBARDIER = new StatBundle(15.0D, 1.0D, 2.0D, 0.0F, 1.0F, 0.1F);
    private static final StatBundle BRAWLER = new StatBundle(35.0D, 1.0D, 10.0D, 0.5F, 2.0F, 0.15F);
    private static final StatBundle DIGGER = new StatBundle(50.0D, 1.0D, 10.0D, 0.5F, 3.0F, 0.20F);
    private static final StatBundle BLASTER = new StatBundle(35.0D, 1.0D, 10.0D, 0.5F, 2.0F, 0.15F);
    private static final StatBundle BEHEMOTH = new StatBundle(125.0D, 0.8D, 25.0D, 2.0F, 5.0F, 0.35F);
    private static final StatBundle BRENDA = new StatBundle(250.0D, 1.2D, 50.0D, 5.0F, 10.0F, 0.5F);
    private static final StatBundle NUCLEAR = new StatBundle(100.0D, 0.8D, 50.0D, 5.0F, 10.0F, 0.5F);
    private static final StatBundle SCOUT = new StatBundle(20.0D, 1.5D, 5.0D, 0.0F, 0.5F, 0.5F);

    private GlyphidStats() {
    }

    public static StatBundle forVariant(GlyphidEntity.Variant variant) {
        return switch (variant) {
            case BOMBARDIER -> BOMBARDIER;
            case BRAWLER -> BRAWLER;
            case DIGGER -> DIGGER;
            case BLASTER -> BLASTER;
            case BEHEMOTH -> BEHEMOTH;
            case BRENDA -> BRENDA;
            case NUCLEAR -> NUCLEAR;
            case SCOUT -> SCOUT;
            default -> GRUNT;
        };
    }

    public static boolean handleAttack(GlyphidEntity glyphid, DamageSource source, float amount) {
        if (source.is(HbmDamageTypes.ACID) && source.getEntity() instanceof GlyphidEntity) {
            return false;
        }
        return glyphid.attackSuperclass(source, amount);
    }

    public static float armorBreakMultiplier(GlyphidEntity.Variant variant) {
        return switch (variant) {
            case SCOUT -> 0.0F;
            case BRAWLER, DIGGER, BLASTER -> 0.25F;
            case BEHEMOTH -> 0.15F;
            case BRENDA, NUCLEAR -> 0.12F;
            default -> 0.6F;
        };
    }
}
