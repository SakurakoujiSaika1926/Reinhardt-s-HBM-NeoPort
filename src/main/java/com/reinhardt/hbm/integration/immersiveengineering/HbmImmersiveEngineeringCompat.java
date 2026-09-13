package com.reinhardt.hbm.integration.immersiveengineering;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Runtime balance glue for Immersive Engineering.
 *
 * <p>HBM's modern FE bridge makes liquid-fueled generators part of the same
 * power economy as Create Crafts & Additions motors. IE's diesel generator is
 * much larger than HBM's small diesel generator, so HBM applies a pack-level
 * default that raises IE diesel throughput and the matching HV connector cap
 * as one coherent balance set.</p>
 */
public final class HbmImmersiveEngineeringCompat {
    private static final String IE_SERVER_CONFIG_CLASS = "blusunrize.immersiveengineering.common.config.IEServerConfig";
    private static final String IE_WIRE_TYPE_CLASS = "blusunrize.immersiveengineering.common.wires.IEWireTypes$IEWireType";
    private static boolean applied;

    private HbmImmersiveEngineeringCompat() {
    }

    public static void applyDieselGeneratorBalanceDefaults() {
        if (applied) {
            return;
        }
        applied = true;

        if (!HbmConfig.ENABLE_IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_DEFAULTS.get()) {
            return;
        }

        try {
            Class<?> serverConfig = Class.forName(IE_SERVER_CONFIG_CLASS);
            boolean changed = false;

            Object machines = staticField(serverConfig, "MACHINES");
            changed |= setIntValue(machines, "dieselGen_output", HbmConfig.IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_OUTPUT.get());

            Object wires = staticField(serverConfig, "WIRES");
            Object hvWireConfig = hvEnergyWireConfig(wires);
            int hvWireTransferRate = HbmConfig.IMMERSIVE_ENGINEERING_HV_WIRE_TRANSFER_RATE.get();
            if (hvWireTransferRate > 0) {
                changed |= setIntValue(hvWireConfig, "transferRate", hvWireTransferRate);
            }
            int hvConnectorRate = HbmConfig.IMMERSIVE_ENGINEERING_HV_CONNECTOR_RATE.get();
            if (hvConnectorRate > 0) {
                changed |= setIntValue(hvWireConfig, "connectorRate", hvConnectorRate);
            }

            if (changed) {
                refresh(serverConfig);
                ReinhardtsHBM.LOGGER.info(
                        "Applied HBM Immersive Engineering diesel balance: dieselGen_output={}, hv_wire_transfer={}, hv_connector={}",
                        HbmConfig.IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_OUTPUT.get(),
                        hvWireTransferRate > 0 ? hvWireTransferRate : "unchanged",
                        hvConnectorRate > 0 ? hvConnectorRate : "unchanged"
                );
            }
        } catch (ClassNotFoundException ignored) {
            // IE is optional. Missing classes are expected when it is not installed.
        } catch (ReflectiveOperationException | RuntimeException ex) {
            ReinhardtsHBM.LOGGER.warn("Could not apply HBM Immersive Engineering diesel generator balance", ex);
        }
    }

    private static Object staticField(Class<?> owner, String fieldName) throws ReflectiveOperationException {
        Field field = owner.getField(fieldName);
        return field.get(null);
    }

    private static boolean setIntValue(Object owner, String fieldName, int desired)
            throws ReflectiveOperationException {
        Field field = owner.getClass().getField(fieldName);
        Object rawValue = field.get(owner);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object hvEnergyWireConfig(Object wires) throws ReflectiveOperationException {
        Field field = wires.getClass().getField("energyWireConfigs");
        Object rawConfigs = field.get(wires);
        if (!(rawConfigs instanceof Map<?, ?> configs)) {
            throw new IllegalStateException("IE WIRES.energyWireConfigs is not a Map");
        }

        Class<? extends Enum> wireType = (Class<? extends Enum>) Class.forName(IE_WIRE_TYPE_CLASS).asSubclass(Enum.class);
        Object steel = Enum.valueOf(wireType, "STEEL");
        Object direct = configs.get(steel);
        if (direct != null) {
            return direct;
        }

        for (Map.Entry<?, ?> entry : configs.entrySet()) {
            Object key = entry.getKey();
            if (key instanceof Enum<?> enumKey && "STEEL".equals(enumKey.name())) {
                return entry.getValue();
            }
        }

        throw new IllegalStateException("Could not find IE HV/steel energy wire config");
    }

    private static void refresh(Class<?> serverConfig) {
        try {
            Method refresh = serverConfig.getMethod("refresh");
            refresh.invoke(null);
        } catch (ReflectiveOperationException ex) {
            ReinhardtsHBM.LOGGER.debug("IE server config refresh was not available after HBM balance override", ex);
        }
    }
}
