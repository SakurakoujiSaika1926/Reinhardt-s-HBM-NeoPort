package com.reinhardt.hbm.block;

/** Raw ICF laser parts. Variants retain the six 1.7.10 EnumICFPart ordinals. */
public final class IcfLaserComponentBlock extends LegacyVariantBlock {
    public IcfLaserComponentBlock(Properties properties) {
        super(properties, Part.values().length - 1);
    }

    public enum Part {
        CASING,
        PORT,
        CELL,
        EMITTER,
        CAPACITOR,
        TURBO;

        public static Part byVariant(int variant) {
            Part[] values = values();
            return variant >= 0 && variant < values.length ? values[variant] : CASING;
        }
    }
}
