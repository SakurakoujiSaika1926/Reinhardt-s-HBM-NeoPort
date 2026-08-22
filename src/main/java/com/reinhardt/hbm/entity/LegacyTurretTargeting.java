package com.reinhardt.hbm.entity;

import com.reinhardt.hbm.api.entity.LegacyRadarDetectable;
import com.reinhardt.hbm.api.entity.LegacyTurretMachineTarget;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.AbstractMinecart;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.HashSet;
import java.util.function.BiFunction;

public final class LegacyTurretTargeting {
    public static final Set<Class<? extends Entity>> TARGET_PLAYERS = new HashSet<>();
    public static final Set<Class<? extends Entity>> TARGET_FRIENDLY = new HashSet<>();
    public static final Set<Class<? extends Entity>> TARGET_HOSTILE = new HashSet<>();
    public static final Set<Class<? extends Entity>> TARGET_MACHINES = new HashSet<>();
    public static final Set<Class<? extends Entity>> TARGET_BLACKLIST = new HashSet<>();
    public static final Map<Class<? extends Entity>, BiFunction<Entity, Object, Integer>> TARGET_CONDITIONS = new HashMap<>();

    // Preserve the 1.7.10 API names for addons ported against CompatExternal.
    public static final Set<Class<? extends Entity>> turretTargetPlayer = TARGET_PLAYERS;
    public static final Set<Class<? extends Entity>> turretTargetFriendly = TARGET_FRIENDLY;
    public static final Set<Class<? extends Entity>> turretTargetHostile = TARGET_HOSTILE;
    public static final Set<Class<? extends Entity>> turretTargetMachine = TARGET_MACHINES;
    public static final Set<Class<? extends Entity>> turretTargetBlacklist = TARGET_BLACKLIST;
    public static final Map<Class<? extends Entity>, BiFunction<Entity, Object, Integer>> turretTargetCondition = TARGET_CONDITIONS;

    private LegacyTurretTargeting() {
    }

    /**
     * Legacy-compatible target category registration: 0 player, 1 friendly, 2 hostile, 3 machine.
     */
    public static void registerSimple(Class<? extends Entity> entityClass, int type) {
        switch (type) {
            case 0 -> TARGET_PLAYERS.add(entityClass);
            case 1 -> TARGET_FRIENDLY.add(entityClass);
            case 2 -> TARGET_HOSTILE.add(entityClass);
            case 3 -> TARGET_MACHINES.add(entityClass);
            default -> {
            }
        }
    }

    public static void registerTurretTargetSimple(Class<? extends Entity> entityClass, int type) {
        registerSimple(entityClass, type);
    }

    public static void registerBlacklist(Class<? extends Entity> entityClass) {
        TARGET_BLACKLIST.add(entityClass);
    }

    public static void registerTurretTargetBlacklist(Class<? extends Entity> entityClass) {
        registerBlacklist(entityClass);
    }

    /**
     * The condition result follows 1.7.10: -1 rejects, 0 continues normal checks, 1 accepts.
     */
    public static void registerCondition(Class<? extends Entity> entityClass,
                                         BiFunction<Entity, Object, Integer> condition) {
        TARGET_CONDITIONS.put(entityClass, condition);
    }

    public static void registerTurretTargetingCondition(Class<? extends Entity> entityClass,
                                                        BiFunction<Entity, Object, Integer> condition) {
        registerCondition(entityClass, condition);
    }

    public static boolean blacklisted(Entity entity) {
        return matches(TARGET_BLACKLIST, entity);
    }

    public static int conditionalResult(Entity entity, Object turret) {
        for (Map.Entry<Class<? extends Entity>, BiFunction<Entity, Object, Integer>> entry : TARGET_CONDITIONS.entrySet()) {
            if (!entry.getKey().isAssignableFrom(entity.getClass())) {
                continue;
            }
            BiFunction<Entity, Object, Integer> condition = entry.getValue();
            if (condition == null) {
                continue;
            }
            Integer result = condition.apply(entity, turret);
            if (result != null && (result == -1 || result == 1)) {
                return result;
            }
        }
        return 0;
    }

    public static boolean friendlyTarget(Entity entity) {
        return matches(TARGET_FRIENDLY, entity);
    }

    public static boolean hostileTarget(Entity entity) {
        return matches(TARGET_HOSTILE, entity);
    }

    public static boolean playerTarget(Entity entity) {
        return matches(TARGET_PLAYERS, entity);
    }

    public static boolean radarVisible(Entity entity, Object turret) {
        return !(entity instanceof LegacyRadarDetectable detectable) || detectable.canBeSeenBy(turret);
    }

    public static boolean machineTarget(Entity entity, Object turret) {
        if (!radarVisible(entity, turret)) {
            return false;
        }
        if (entity instanceof AbstractMinecart) {
            return true;
        }
        if (entity instanceof LegacyTurretMachineTarget target) {
            return target.turretTargetKind() != LegacyTurretMachineTarget.TargetKind.MISSILE
                    || entity.getDeltaMovement().y < 0.0D;
        }
        return matches(TARGET_MACHINES, entity);
    }

    private static boolean matches(Set<Class<? extends Entity>> classes, Entity entity) {
        for (Class<? extends Entity> entityClass : classes) {
            if (entityClass.isAssignableFrom(entity.getClass())) {
                return true;
            }
        }
        return false;
    }
}
