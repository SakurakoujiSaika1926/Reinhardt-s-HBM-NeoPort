package com.reinhardt.hbm.integration.createbigcannons;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.entity.LegacyMistEntity;
import com.reinhardt.hbm.pollution.HbmPollution;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.registry.HbmFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Consumer;

/**
 * Soft integration for Create: Big Cannons.
 *
 * <p>HBM does not register a second cannon shell item here. CBC already owns
 * the cannon shell crafting and filling workflow; this class only teaches CBC
 * what HBM mustard gas does once a CBC fluid shell releases it.</p>
 */
public final class HbmCreateBigCannonsCompat {
    private static final String CBC_MOD_ID = "createbigcannons";
    private static final ResourceLocation CBC_FLUID_SHELL =
            ResourceLocation.fromNamespaceAndPath(CBC_MOD_ID, "fluid_shell");
    private static final String FLUID_BLOB_EFFECT_REGISTRY =
            "rbasamoyai.createbigcannons.munitions.big_cannon.fluid_shell.FluidBlobEffectRegistry";
    private static final String ON_HIT_BLOCK = FLUID_BLOB_EFFECT_REGISTRY + "$OnHitBlock";
    private static final String ON_HIT_ENTITY = FLUID_BLOB_EFFECT_REGISTRY + "$OnHitEntity";
    private static final String CBC_DATA_COMPONENTS =
            "rbasamoyai.createbigcannons.index.CBCDataComponents";

    private static final int CBC_FLUID_SHELL_CAPACITY_MB = 2_000;
    private static final double LEGACY_ARTILLERY_MUSTARD_SHELL_FLUID_MB = 4_000.0D;
    private static final double LEGACY_ARTILLERY_MUSTARD_POISON_POLLUTION = 30.0D;
    private static final double LEGACY_ARTILLERY_MUSTARD_HEAVYMETAL_POLLUTION = 15.0D;
    private static final float LEGACY_MUSTARD_MIST_WIDTH = 20.0F;
    private static final float LEGACY_MUSTARD_MIST_HEIGHT = 10.0F;
    private static final double LEGACY_MUSTARD_MIST_Y_OFFSET = -5.0D;

    private static boolean registered;
    private static boolean callbackWarningLogged;

    private HbmCreateBigCannonsCompat() {
    }

    public static void registerMustardGasFluidShellEffects() {
        if (registered || !ModList.get().isLoaded(CBC_MOD_ID)) {
            return;
        }
        registered = true;

        try {
            Class<?> registry = Class.forName(FLUID_BLOB_EFFECT_REGISTRY);
            Class<?> onHitBlock = Class.forName(ON_HIT_BLOCK);
            Class<?> onHitEntity = Class.forName(ON_HIT_ENTITY);
            Fluid mustardGas = HbmFluids.source("mustardgas");

            registry.getMethod("registerHitBlock", Fluid.class, onHitBlock)
                    .invoke(null, mustardGas, consumerProxy(onHitBlock, HbmCreateBigCannonsCompat::mustardGasHitBlock));
            registry.getMethod("registerHitEntity", Fluid.class, onHitEntity)
                    .invoke(null, mustardGas, consumerProxy(onHitEntity, HbmCreateBigCannonsCompat::mustardGasHitEntity));

            ReinhardtsHBM.LOGGER.info("Registered HBM mustard gas effects for Create: Big Cannons fluid shells");
        } catch (ClassNotFoundException ignored) {
            // CBC is optional and may disappear between ModList detection and setup on unusual launchers.
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            ReinhardtsHBM.LOGGER.warn("Could not register HBM mustard gas effects for Create: Big Cannons", ex);
        }
    }

    public static ItemStack mustardGasFluidShellStack() {
        return mustardGasFluidShellStack(null);
    }

    public static ItemStack mustardGasFluidShellStack(HolderLookup.Provider registries) {
        if (!ModList.get().isLoaded(CBC_MOD_ID) || !BuiltInRegistries.ITEM.containsKey(CBC_FLUID_SHELL)) {
            return ItemStack.EMPTY;
        }

        Item item = BuiltInRegistries.ITEM.get(CBC_FLUID_SHELL);
        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        ItemStack shell = new ItemStack(item);
        ItemStack capabilityFilled = FluidUtil.getFluidHandler(shell.copy())
                .map(handler -> {
                    int accepted = handler.fill(
                            new FluidStack(HbmFluids.source("mustardgas"), CBC_FLUID_SHELL_CAPACITY_MB),
                            IFluidHandler.FluidAction.EXECUTE
                    );
                    return accepted > 0 ? handler.getContainer() : ItemStack.EMPTY;
                })
                .orElse(ItemStack.EMPTY);
        if (!capabilityFilled.isEmpty() || registries == null) {
            return capabilityFilled;
        }

        return fillShellComponent(shell, registries);
    }

    @SuppressWarnings("unchecked")
    private static ItemStack fillShellComponent(ItemStack shell, HolderLookup.Provider registries) {
        try {
            Field fluidContentField = Class.forName(CBC_DATA_COMPONENTS).getField("FLUID_CONTENT");
            Object rawComponentType = fluidContentField.get(null);
            if (!(rawComponentType instanceof DataComponentType<?> componentType)) {
                return ItemStack.EMPTY;
            }

            FluidTank tank = new FluidTank(CBC_FLUID_SHELL_CAPACITY_MB);
            tank.setFluid(new FluidStack(HbmFluids.source("mustardgas"), CBC_FLUID_SHELL_CAPACITY_MB));
            CompoundTag fluidTag = new CompoundTag();
            tank.writeToNBT(registries, fluidTag);
            shell.set((DataComponentType<CustomData>) componentType, CustomData.of(fluidTag));
            return shell;
        } catch (ClassNotFoundException ignored) {
            return ItemStack.EMPTY;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            ReinhardtsHBM.LOGGER.warn("Could not create a pre-filled CBC mustard gas fluid shell stack", ex);
            return ItemStack.EMPTY;
        }
    }

    private static Object consumerProxy(Class<?> interfaceClass, Consumer<Object> callback) {
        InvocationHandler handler = (proxy, method, args) -> {
            String methodName = method.getName();
            if ("accept".equals(methodName) && args != null && args.length == 1) {
                callback.accept(args[0]);
                return null;
            }
            if ("toString".equals(methodName)) {
                return "HBM " + interfaceClass.getSimpleName() + " mustard gas callback";
            }
            if ("hashCode".equals(methodName)) {
                return System.identityHashCode(proxy);
            }
            if ("equals".equals(methodName) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            return null;
        };
        return Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class<?>[]{interfaceClass}, handler);
    }

    private static void mustardGasHitBlock(Object context) {
        try {
            Level level = (Level) invoke(context, "level");
            if (level.isClientSide) {
                return;
            }
            BlockHitResult result = (BlockHitResult) invoke(context, "result");
            spawnMustardGasMist(level, result.getLocation(), blobFluidAmount(context));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warnCallbackFailure(ex);
        }
    }

    private static void mustardGasHitEntity(Object context) {
        try {
            Level level = (Level) invoke(context, "level");
            if (level.isClientSide) {
                return;
            }
            EntityHitResult result = (EntityHitResult) invoke(context, "result");
            Entity entity = result.getEntity();
            Vec3 location = result.getLocation();
            if (location == null) {
                location = entity.position();
            }
            spawnMustardGasMist(level, location, blobFluidAmount(context));
        } catch (ReflectiveOperationException | LinkageError | RuntimeException ex) {
            warnCallbackFailure(ex);
        }
    }

    private static void spawnMustardGasMist(Level level, Vec3 hitLocation, int fluidAmountMb) {
        Vec3 cloudBase = hitLocation.add(0.0D, LEGACY_MUSTARD_MIST_Y_OFFSET, 0.0D);
        level.addFreshEntity(new LegacyMistEntity(
                level,
                cloudBase.x,
                cloudBase.y,
                cloudBase.z,
                LegacyMistEntity.MistType.MUSTARD,
                LEGACY_MUSTARD_MIST_WIDTH,
                LEGACY_MUSTARD_MIST_HEIGHT
        ));

        double ratio = Math.max(0, fluidAmountMb) / LEGACY_ARTILLERY_MUSTARD_SHELL_FLUID_MB;
        if (ratio <= 0.0D) {
            return;
        }
        BlockPos pollutionPos = BlockPos.containing(hitLocation);
        HbmPollution.increment(
                level,
                pollutionPos,
                HbmPollutionType.POISON,
                LEGACY_ARTILLERY_MUSTARD_POISON_POLLUTION * ratio
        );
        HbmPollution.increment(
                level,
                pollutionPos,
                HbmPollutionType.HEAVYMETAL,
                LEGACY_ARTILLERY_MUSTARD_HEAVYMETAL_POLLUTION * ratio
        );
    }

    private static int blobFluidAmount(Object context) throws ReflectiveOperationException {
        Object fstack = invoke(context, "fstack");
        Object amount = invoke(fstack, "amount");
        if (!(amount instanceof Number number)) {
            throw new IllegalStateException("CBC fluid blob stack amount is not numeric");
        }
        return number.intValue();
    }

    private static Object invoke(Object target, String methodName) throws ReflectiveOperationException {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    private static void warnCallbackFailure(Throwable throwable) {
        if (callbackWarningLogged) {
            return;
        }
        callbackWarningLogged = true;
        ReinhardtsHBM.LOGGER.warn("Create: Big Cannons mustard gas callback failed", throwable);
    }
}
