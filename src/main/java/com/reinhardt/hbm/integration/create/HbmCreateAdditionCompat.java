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
            boolean changed = setIntValue(commonConfig, "FE_RPM", HbmConfig.CREATE_ADDITION_FE_AT_MAX_RPM.get());

            int maxStress = HbmConfig.CREATE_ADDITION_MAX_STRESS.get();
            if (maxStress > 0) {
                changed |= setIntValue(commonConfig, "MAX_STRESS", maxStress);
            }

            if (changed) {
                ReinhardtsHBM.LOGGER.info(
                        "Applied HBM Create Crafts & Additions motor balance: fe_at_max_rpm={}, max_stress={}",
                        HbmConfig.CREATE_ADDITION_FE_AT_MAX_RPM.get(),
                        maxStress > 0 ? maxStress : "unchanged"
                );
            }
        } catch (ClassNotFoundException ignored) {
            // CCA is optional. Missing classes are expected when it is not installed.
        } catch (ReflectiveOperationException | RuntimeException ex) {
            ReinhardtsHBM.LOGGER.warn("Could not apply HBM Create Crafts & Additions motor balance", ex);
        }
    }

    private static boolean setIntValue(Class<?> commonConfig, String fieldName, int desired)
            throws ReflectiveOperationException {
        Field field = commonConfig.getField(fieldName);
        Object rawValue = field.get(null);
        if (!(rawValue instanceof ModConfigSpec.IntValue configValue)) {
            return false;
        }

        int current = configValue.get();
        if (current == desired) {
            return false;
        }

        configValue.set(desired);
        return true;
    }
}
