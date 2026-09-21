package com.reinhardt.hbm.integration.tacz;

import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Optional TaCZ loot bridge for the weapon crate.
 *
 * <p>The bridge deliberately reads TaCZ's common runtime indexes instead of
 * hard-coding its built-in namespace. Consequently guns, ammunition and
 * attachments supplied by third-party gun packs enter the same flat pool.</p>
 */
public final class TaczWeaponCrateCompat {
    private static final String TACZ_MOD_ID = "tacz";
    private static final int MIN_AMMO = 20;
    private static final int MAX_AMMO = 120;

    private static volatile Api api;
    private static volatile boolean apiLookupFailed;

    private TaczWeaponCrateCompat() {
    }

    public static void addRandomDrops(
            HolderLookup.Provider registries,
            RandomSource random,
            List<ItemStack> hbmPool,
            int rolls,
            List<ItemStack> drops
    ) {
        PoolSnapshot taczPool = snapshotPool();
        int bucketCount = (hbmPool.isEmpty() ? 0 : 1)
                + (taczPool.guns().isEmpty() ? 0 : 1)
                + (taczPool.ammo().isEmpty() ? 0 : 1)
                + (taczPool.attachments().isEmpty() ? 0 : 1);
        if (bucketCount == 0) {
            return;
        }

        for (int roll = 0; roll < rolls; roll++) {
            // A gun pack can contribute hundreds of TaCZ definitions. Mixing
            // those definitions directly into the weighted HBM list makes the
            // original crate contents and scarce ammo definitions statistically
            // disappear. Select the independent content category first, then
            // select uniformly inside that category. Ammo is deliberately not
            // paired with a gun: other mods may provide several valid ammo
            // families for the same weapon.
            int bucket = random.nextInt(bucketCount);
            if (!hbmPool.isEmpty() && bucket-- == 0) {
                drops.add(hbmPool.get(random.nextInt(hbmPool.size())).copy());
                continue;
            }
            if (!taczPool.guns().isEmpty() && bucket-- == 0) {
                addRandomEntryDrops(registries, random, taczPool.guns(), drops);
                continue;
            }
            if (!taczPool.ammo().isEmpty() && bucket-- == 0) {
                addRandomEntryDrops(registries, random, taczPool.ammo(), drops);
                continue;
            }
            addRandomEntryDrops(registries, random, taczPool.attachments(), drops);
        }
    }

    private static void addRandomEntryDrops(
            HolderLookup.Provider registries,
            RandomSource random,
            List<Entry> pool,
            List<ItemStack> drops
    ) {
        addEntryDrops(registries, random, pool.get(random.nextInt(pool.size())), drops);
    }

    private static PoolSnapshot snapshotPool() {
        if (!ModList.get().isLoaded(TACZ_MOD_ID)) {
            return PoolSnapshot.EMPTY;
        }

        Api loadedApi = api();
        if (loadedApi == null) {
            return PoolSnapshot.EMPTY;
        }

        List<Entry> entries = new ArrayList<>();
        addIndexEntries(entries, loadedApi.allGuns(), Type.GUN);
        addIndexEntries(entries, loadedApi.allAmmo(), Type.AMMO);
        addIndexEntries(entries, loadedApi.allAttachments(), Type.ATTACHMENT);
        entries.sort(Comparator.comparing((Entry entry) -> entry.id().toString())
                .thenComparing(entry -> entry.type().ordinal()));
        return new PoolSnapshot(
                entries.stream().filter(entry -> entry.type() == Type.GUN).toList(),
                entries.stream().filter(entry -> entry.type() == Type.AMMO).toList(),
                entries.stream().filter(entry -> entry.type() == Type.ATTACHMENT).toList()
        );
    }

    private static void addIndexEntries(List<Entry> entries, Object value, Type type) {
        if (!(value instanceof Collection<?> collection)) {
            return;
        }
        for (Object element : collection) {
            if (element instanceof Map.Entry<?, ?> mapEntry && mapEntry.getKey() instanceof ResourceLocation id) {
                entries.add(new Entry(type, id));
            }
        }
    }

    private static void addEntryDrops(
            HolderLookup.Provider registries,
            RandomSource random,
            Entry entry,
            List<ItemStack> drops
    ) {
        Api loadedApi = api();
        if (loadedApi == null) {
            return;
        }

        ItemStack stack = loadedApi.build(entry, registries);
        if (stack.isEmpty()) {
            return;
        }

        if (entry.type() != Type.AMMO) {
            drops.add(stack);
            return;
        }

        int remaining = MIN_AMMO + random.nextInt(MAX_AMMO - MIN_AMMO + 1);
        int stackLimit = Math.max(1, stack.getMaxStackSize());
        while (remaining > 0) {
            ItemStack split = stack.copy();
            int amount = Math.min(remaining, stackLimit);
            split.setCount(amount);
            drops.add(split);
            remaining -= amount;
        }
    }

    private static Api api() {
        Api current = api;
        if (current != null || apiLookupFailed) {
            return current;
        }
        synchronized (TaczWeaponCrateCompat.class) {
            current = api;
            if (current != null || apiLookupFailed) {
                return current;
            }
            try {
                current = new Api();
                api = current;
                return current;
            } catch (ReflectiveOperationException | LinkageError exception) {
                apiLookupFailed = true;
                ReinhardtsHBM.LOGGER.warn("TaCZ is installed, but its weapon-crate API could not be initialized", exception);
                return null;
            }
        }
    }

    private enum Type {
        GUN,
        AMMO,
        ATTACHMENT
    }

    private record Entry(Type type, ResourceLocation id) {
    }

    private record PoolSnapshot(List<Entry> guns, List<Entry> ammo, List<Entry> attachments) {
        private static final PoolSnapshot EMPTY = new PoolSnapshot(List.of(), List.of(), List.of());
    }

    private static final class Api {
        private final Method allGuns;
        private final Method allAmmo;
        private final Method allAttachments;
        private final Method commonGunIndex;
        private final Method gunIndexData;
        private final Method gunDataFireModes;
        private final Builder gunBuilder;
        private final Builder ammoBuilder;
        private final Builder attachmentBuilder;

        private Api() throws ReflectiveOperationException {
            Class<?> timelessApi = Class.forName("com.tacz.guns.api.TimelessAPI");
            Class<?> commonGunIndexClass = Class.forName("com.tacz.guns.resource.index.CommonGunIndex");
            Class<?> gunDataClass = Class.forName("com.tacz.guns.resource.pojo.data.gun.GunData");
            this.allGuns = timelessApi.getMethod("getAllCommonGunIndex");
            this.allAmmo = timelessApi.getMethod("getAllCommonAmmoIndex");
            this.allAttachments = timelessApi.getMethod("getAllCommonAttachmentIndex");
            this.commonGunIndex = timelessApi.getMethod("getCommonGunIndex", ResourceLocation.class);
            this.gunIndexData = commonGunIndexClass.getMethod("getGunData");
            this.gunDataFireModes = gunDataClass.getMethod("getFireModeSet");
            this.gunBuilder = new Builder("com.tacz.guns.api.item.builder.GunItemBuilder", true);
            this.ammoBuilder = new Builder("com.tacz.guns.api.item.builder.AmmoItemBuilder", false);
            this.attachmentBuilder = new Builder("com.tacz.guns.api.item.builder.AttachmentItemBuilder", false);
        }

        private Object allGuns() {
            return invokeIndex(allGuns);
        }

        private Object allAmmo() {
            return invokeIndex(allAmmo);
        }

        private Object allAttachments() {
            return invokeIndex(allAttachments);
        }

        private Object invokeIndex(Method method) {
            try {
                return method.invoke(null);
            } catch (IllegalAccessException | InvocationTargetException | RuntimeException exception) {
                ReinhardtsHBM.LOGGER.debug("Could not read TaCZ weapon-crate index {}", method.getName(), exception);
                return List.of();
            }
        }

        private ItemStack build(Entry entry, HolderLookup.Provider registries) {
            try {
                return switch (entry.type()) {
                    case GUN -> gunBuilder.build(entry.id(), registries, defaultFireMode(entry.id()));
                    case AMMO -> ammoBuilder.build(entry.id(), registries);
                    case ATTACHMENT -> attachmentBuilder.build(entry.id(), registries);
                };
            } catch (ReflectiveOperationException | RuntimeException | LinkageError exception) {
                ReinhardtsHBM.LOGGER.debug("Could not create TaCZ weapon-crate drop {}", entry.id(), exception);
                return ItemStack.EMPTY;
            }
        }

        private Object defaultFireMode(ResourceLocation gunId)
                throws InvocationTargetException, IllegalAccessException {
            Object result = this.commonGunIndex.invoke(null, gunId);
            if (!(result instanceof java.util.Optional<?> optional) || optional.isEmpty()) {
                return null;
            }
            Object gunData = this.gunIndexData.invoke(optional.get());
            Object modes = this.gunDataFireModes.invoke(gunData);
            if (!(modes instanceof List<?> list) || list.isEmpty()) {
                return null;
            }
            return list.getFirst();
        }
    }

    private static final class Builder {
        private final Method create;
        private final Method setId;
        private final Method setFireMode;
        private final Method build;
        private final boolean needsRegistries;

        private Builder(String className, boolean needsRegistries) throws ReflectiveOperationException {
            Class<?> builderClass = Class.forName(className);
            this.create = builderClass.getMethod("create");
            this.setId = builderClass.getMethod("setId", ResourceLocation.class);
            this.setFireMode = needsRegistries
                    ? builderClass.getMethod("setFireMode", Class.forName("com.tacz.guns.api.item.gun.FireMode"))
                    : null;
            this.needsRegistries = needsRegistries;
            this.build = needsRegistries
                    ? builderClass.getMethod("build", HolderLookup.Provider.class)
                    : builderClass.getMethod("build");
        }

        private ItemStack build(ResourceLocation id, HolderLookup.Provider registries)
                throws ReflectiveOperationException {
            return build(id, registries, null);
        }

        private ItemStack build(ResourceLocation id, HolderLookup.Provider registries, Object fireMode)
                throws ReflectiveOperationException {
            Object builder = create.invoke(null);
            builder = setId.invoke(builder, id);
            if (this.setFireMode != null && fireMode != null) {
                builder = this.setFireMode.invoke(builder, fireMode);
            }
            Object result = needsRegistries ? build.invoke(builder, registries) : build.invoke(builder);
            return result instanceof ItemStack stack ? stack : ItemStack.EMPTY;
        }
    }
}
