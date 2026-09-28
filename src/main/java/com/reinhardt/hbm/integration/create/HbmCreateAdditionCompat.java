package com.reinhardt.hbm.integration.create;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;

/**
 * Runtime balance glue for Create Crafts & Additions.
 *
 * <p>HBM's FE bridge deliberately makes HBM power infrastructure feed modern
 * FE consumers directly. CCA's stock electric motor is balanced around smaller
 * FE sources, so the default 480 FE/t full-speed motor makes a single HBM
 * generator cover too much Create stress. This soft integration applies HBM's
 * pack-level motor default without requiring server owners to edit
 * {@code createaddition-common.toml} by hand.</p>
 */
public final class HbmCreateAdditionCompat {
    private static final String COMMON_CONFIG_CLASS = "com.mrh0.createaddition.config.CommonConfig";
    private static final int HBM_MOTOR_CAPACITY_FLOOR = 16_384;
    private static boolean applied;

    private HbmCreateAdditionCompat() {
    }

    public static void applyMotorBalanceDefaults() {
        if (applied) {
            return;
        }
        applied = true;

        if (!HbmConfig.ENABLE_CREATE_ADDITION_MOTOR_DEFAULTS.get()) {
            return;
        }

        try {
            Class<?> commonConfig = Class.forName(COMMON_CONFIG_CLASS);
            ModConfigSpec.IntValue feAtMaxRpmConfig = intValue(commonConfig, "FE_RPM");
            ModConfigSpec.IntValue motorMaxInputConfig = intValue(commonConfig, "ELECTRIC_MOTOR_MAX_INPUT");
            ModConfigSpec.IntValue motorCapacityConfig = intValue(commonConfig, "ELECTRIC_MOTOR_CAPACITY");

            int feAtMaxRpm = HbmConfig.CREATE_ADDITION_FE_AT_MAX_RPM.get();
            long requiredCapacityLong = 2L * feAtMaxRpm + 1L;
            if (requiredCapacityLong > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "CCA motor rate cannot satisfy its 2x restart threshold: " + feAtMaxRpm
                );
            }

            /*
             * CCA 1.5.11 restarts an inactive motor only when stored FE is
             * strictly greater than twice its current FE/t consumption. Its
             * stock 5,000 FE capacity is therefore incompatible with HBM's
             * 4,096 FE/t full-speed setting. Keep the input at CCA's required
             * minimum and apply HBM's 16,384 FE capacity floor, while still
             * enforcing the restart threshold for higher configured rates.
             * Never lower a larger pack-defined value.
             */
            int requiredCapacity = Math.max(HBM_MOTOR_CAPACITY_FLOOR, (int) requiredCapacityLong);
            boolean changed = setIntValue(feAtMaxRpmConfig, feAtMaxRpm);
            changed |= ensureIntValueAtLeast(motorMaxInputConfig, feAtMaxRpm);
            changed |= ensureIntValueAtLeast(motorCapacityConfig, requiredCapacity);

            int maxStress = HbmConfig.CREATE_ADDITION_MAX_STRESS.get();
            if (maxStress > 0) {
                changed |= setIntValue(intValue(commonConfig, "MAX_STRESS"), maxStress);
            }

            if (changed) {
                ReinhardtsHBM.LOGGER.info(
                        "Applied HBM Create Crafts & Additions motor balance: "
                                + "fe_at_max_rpm={}, motor_max_input={}, motor_capacity={}, max_stress={}",
                        feAtMaxRpmConfig.get(),
                        motorMaxInputConfig.get(),
                        motorCapacityConfig.get(),
                        maxStress > 0 ? maxStress : "unchanged"
                );
            }
        } catch (ClassNotFoundException ignored) {
            // CCA is optional. Missing classes are expected when it is not installed.
        } catch (ReflectiveOperationException | RuntimeException | LinkageError ex) {
            ReinhardtsHBM.LOGGER.warn("Could not apply HBM Create Crafts & Additions motor balance", ex);
        }
    }

    private static ModConfigSpec.IntValue intValue(Class<?> commonConfig, String fieldName)
            throws ReflectiveOperationException {
        Field field = commonConfig.getField(fieldName);
        Object rawValue = field.get(null);
        if (!(rawValue instanceof ModConfigSpec.IntValue configValue)) {
            throw new NoSuchFieldException(COMMON_CONFIG_CLASS + "." + fieldName + " is not an IntValue");
        }
        return configValue;
    }

    private static boolean setIntValue(ModConfigSpec.IntValue configValue, int desired) {
        int current = configValue.get();
        if (current == desired) {
            return false;
        }

        configValue.set(desired);
        return true;
    }

    private static boolean ensureIntValueAtLeast(ModConfigSpec.IntValue configValue, int minimum) {
        int current = configValue.get();
        if (current >= minimum) {
            return false;
        }
        configValue.set(minimum);
        return true;
    }
}
