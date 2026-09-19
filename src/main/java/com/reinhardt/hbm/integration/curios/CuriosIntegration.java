package com.reinhardt.hbm.integration.curios;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Soft Curios bridge.  Keep this class free of Curios compile-time references:
 * the modpack can equip HBM masks in Curios when Curios is present, while a
 * plain HBM install keeps the original helmet-slot behavior.
 */
public final class CuriosIntegration {
    private static final String CURIOS_API = "top.theillusivec4.curios.api.CuriosApi";
    private static final String CURIOS_HANDLER = "top.theillusivec4.curios.api.type.capability.ICuriosItemHandler";
    private static final String SLOT_RESULT = "top.theillusivec4.curios.api.SlotResult";

    private static volatile boolean initialized;
    private static volatile boolean available;
    private static Method getCuriosInventory;
    private static Method findCurios;
    private static Method slotResultStack;

    private CuriosIntegration() {
    }

    public static ItemStack findFirstEquipped(LivingEntity entity, Predicate<ItemStack> predicate) {
        for (ItemStack stack : findEquipped(entity, predicate)) {
            return stack;
        }
        return ItemStack.EMPTY;
    }

    @SuppressWarnings("unchecked")
    public static List<ItemStack> findEquipped(LivingEntity entity, Predicate<ItemStack> predicate) {
        if (entity == null || !init()) {
            return List.of();
        }
        try {
            Optional<?> optional = (Optional<?>) getCuriosInventory.invoke(null, entity);
            if (optional.isEmpty()) {
                return List.of();
            }
            Object curios = optional.get();
            Object found = findCurios.invoke(curios, predicate);
            if (!(found instanceof Iterable<?> results)) {
                return List.of();
            }

            List<ItemStack> stacks = new ArrayList<>();
            for (Object result : results) {
                Object stackObject = slotResultStack.invoke(result);
                if (stackObject instanceof ItemStack stack && !stack.isEmpty() && predicate.test(stack)) {
                    stacks.add(stack);
                }
            }
            return stacks;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ignored) {
            available = false;
            return List.of();
        }
    }

    private static boolean init() {
        if (initialized) {
            return available;
        }
        synchronized (CuriosIntegration.class) {
            if (initialized) {
                return available;
            }
            try {
                Class<?> apiClass = Class.forName(CURIOS_API);
                Class<?> handlerClass = Class.forName(CURIOS_HANDLER);
                Class<?> slotResultClass = Class.forName(SLOT_RESULT);
                getCuriosInventory = apiClass.getMethod("getCuriosInventory", LivingEntity.class);
                findCurios = handlerClass.getMethod("findCurios", Predicate.class);
                slotResultStack = slotResultClass.getMethod("stack");
                available = true;
            } catch (ReflectiveOperationException | LinkageError ignored) {
                available = false;
            }
            initialized = true;
            return available;
        }
    }
}
