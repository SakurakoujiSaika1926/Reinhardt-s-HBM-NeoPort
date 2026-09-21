package com.reinhardt.hbm.integration.touhoulittlemaid;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.items.IItemHandler;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class HbmTouhouLittleMaidCompat {
    public static final String MOD_ID = "touhou_little_maid";
    private static final String ENTITY_MAID_CLASS =
            "com.github.tartaricacid.touhoulittlemaid.entity.passive.EntityMaid";
    private static final String ALL_INVENTORY_METHOD = "getAllInv";
    private static final Map<Class<?>, Optional<Method>> ALL_INVENTORY_METHODS = new ConcurrentHashMap<>();
    private static volatile Boolean loaded;

    private HbmTouhouLittleMaidCompat() {
    }

    public static boolean isLoaded() {
        Boolean cached = loaded;
        if (cached == null) {
            cached = ModList.get().isLoaded(MOD_ID);
            loaded = cached;
        }
        return cached;
    }

    public static boolean isMaid(LivingEntity entity) {
        if (!isLoaded() || entity == null) {
            return false;
        }
        if (ENTITY_MAID_CLASS.equals(entity.getClass().getName())) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
        return key != null
                && MOD_ID.equals(key.getNamespace())
                && key.getPath().contains("maid")
                && entity.getClass().getName().contains("EntityMaid");
    }

    public static IItemHandler inventory(LivingEntity entity) {
        if (!isMaid(entity)) {
            return null;
        }
        Optional<Method> method = ALL_INVENTORY_METHODS.computeIfAbsent(
                entity.getClass(),
                HbmTouhouLittleMaidCompat::resolveAllInventoryMethod);
        if (method.isEmpty()) {
            return null;
        }
        try {
            Object inventory = method.get().invoke(entity);
            return inventory instanceof IItemHandler handler ? handler : null;
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            return null;
        }
    }

    private static Optional<Method> resolveAllInventoryMethod(Class<?> type) {
        try {
            Method method = type.getMethod(ALL_INVENTORY_METHOD);
            if (!IItemHandler.class.isAssignableFrom(method.getReturnType())) {
                return Optional.empty();
            }
            return Optional.of(method);
        } catch (NoSuchMethodException ignored) {
            return Optional.empty();
        }
    }
}
