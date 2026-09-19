package com.reinhardt.hbm.integration.immersiveengineering;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.config.HbmConfig;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private static final String IE_SERVER_CONFIG_FILE = "immersiveengineering-server.toml";
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
            int dieselGeneratorOutput = HbmConfig.IMMERSIVE_ENGINEERING_DIESEL_GENERATOR_OUTPUT.get();
            changed |= setIntValue(machines, "dieselGen_output", dieselGeneratorOutput);

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
            changed |= patchServerConfigFile(dieselGeneratorOutput, hvWireTransferRate, hvConnectorRate);

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

    private static boolean patchServerConfigFile(int dieselGeneratorOutput, int hvWireTransferRate, int hvConnectorRate) {
        Path configFile = FMLPaths.CONFIGDIR.get().resolve(IE_SERVER_CONFIG_FILE);
        if (!Files.isRegularFile(configFile)) {
            return false;
        }

        try {
            String original = Files.readString(configFile, StandardCharsets.UTF_8);
            String patched = replaceIntInSection(original, "machines", "dieselGen_output", dieselGeneratorOutput);
            if (hvWireTransferRate > 0) {
                patched = replaceIntInSection(patched, "wires.hv", "transferRate", hvWireTransferRate);
            }
            if (hvConnectorRate > 0) {
                patched = replaceIntInSection(patched, "wires.hv", "wireConnectorInput", hvConnectorRate);
            }

            if (original.equals(patched)) {
                return false;
            }

            Files.writeString(configFile, patched, StandardCharsets.UTF_8);
            return true;
        } catch (IOException | RuntimeException ex) {
            ReinhardtsHBM.LOGGER.warn("Could not persist HBM Immersive Engineering diesel balance to {}", configFile, ex);
            return false;
        }
    }

    private static String replaceIntInSection(String config, String section, String key, int value) {
        Matcher sectionMatcher = Pattern.compile("(?m)^\\s*\\[" + Pattern.quote(section) + "\\]\\s*$").matcher(config);
        if (!sectionMatcher.find()) {
            return appendSection(config, section, key, value);
        }

        int sectionStart = sectionMatcher.end();
        int sectionEnd = config.length();
        Matcher nextSectionMatcher = Pattern.compile("(?m)^\\s*\\[[^\\]]+\\]\\s*$").matcher(config);
        if (nextSectionMatcher.find(sectionStart)) {
            sectionEnd = nextSectionMatcher.start();
        }

        String body = config.substring(sectionStart, sectionEnd);
        Matcher keyMatcher = Pattern.compile(
                "(?m)^(\\s*" + Pattern.quote(key) + "\\s*=\\s*)-?\\d+(\\s*(?:#.*)?(?:\\r?\\n|$))"
        ).matcher(body);
        if (!keyMatcher.find()) {
            String lineSeparator = lineSeparator(config);
            String insertion = (body.endsWith("\n") || body.endsWith("\r") ? "" : lineSeparator)
                    + "\t\t" + key + " = " + value + lineSeparator;
            return config.substring(0, sectionEnd) + insertion + config.substring(sectionEnd);
        }

        String replacement = Matcher.quoteReplacement(keyMatcher.group(1) + value + keyMatcher.group(2));
        String patchedBody = keyMatcher.replaceFirst(replacement);
        return config.substring(0, sectionStart) + patchedBody + config.substring(sectionEnd);
    }

    private static String appendSection(String config, String section, String key, int value) {
        String lineSeparator = lineSeparator(config);
        String prefix = config.endsWith("\n") || config.endsWith("\r") ? "" : lineSeparator;
        return config
                + prefix
                + lineSeparator
                + "[" + section + "]"
                + lineSeparator
                + "\t\t" + key + " = " + value
                + lineSeparator;
    }

    private static String lineSeparator(String config) {
        return config.contains("\r\n") ? "\r\n" : "\n";
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
