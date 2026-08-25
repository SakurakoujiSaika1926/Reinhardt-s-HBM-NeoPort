package com.reinhardt.hbm.item;

import com.reinhardt.hbm.menu.LegacyHeldInventoryMenu;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Persistent inventories carried by the two original 1.7.10 handheld
 * containers. Contents intentionally stay on the item stack, exactly as the
 * old ItemPlasticBag and ItemLeadBox inventories did.
 */
public final class LegacyHeldInventoryItem extends Item {
    public enum Kind {
        PLASTIC_BAG("plastic_bag", 1, "container.reinhardtshbm.plastic_bag"),
        CONTAINMENT_BOX("containment_box", 20, "container.reinhardtshbm.containment_box");

        private final String id;
        private final int slots;
        private final String titleKey;

        Kind(String id, int slots, String titleKey) {
            this.id = id;
            this.slots = slots;
            this.titleKey = titleKey;
        }

        public String id() {
            return id;
        }

        public int slots() {
            return slots;
        }

        public String titleKey() {
            return titleKey;
        }
    }

    private static final String CONTENTS_KEY = "legacy_held_inventory";
    private final Kind kind;

    public LegacyHeldInventoryItem(Properties properties, Kind kind) {
        super(properties.stacksTo(1));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(
                    new SimpleMenuProvider(
                            (containerId, inventory, ignored) -> new LegacyHeldInventoryMenu(containerId, inventory, hand, kind),
                            Component.translatable(kind.titleKey())
                    ),
                    buffer -> {
                        buffer.writeBoolean(hand == InteractionHand.OFF_HAND);
                        buffer.writeByte(kind.ordinal());
                    }
            );
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    public static List<ItemStack> loadContents(ItemStack carrier, net.minecraft.core.RegistryAccess registries, Kind kind) {
        CompoundTag root = data(carrier);
        List<ItemStack> contents = new ArrayList<>(kind.slots());
        CompoundTag stored = root.getCompound(CONTENTS_KEY);
        for (int slot = 0; slot < kind.slots(); slot++) {
            contents.add(stored.contains("slot_" + slot)
                    ? ItemStack.parseOptional(registries, stored.getCompound("slot_" + slot))
                    : ItemStack.EMPTY);
        }
        return contents;
    }

    public static void saveContents(ItemStack carrier, List<ItemStack> contents, net.minecraft.core.RegistryAccess registries, Kind kind) {
        CompoundTag root = data(carrier);
        CompoundTag stored = new CompoundTag();
        for (int slot = 0; slot < kind.slots(); slot++) {
            ItemStack stack = slot < contents.size() ? contents.get(slot) : ItemStack.EMPTY;
            if (!stack.isEmpty()) {
                stored.put("slot_" + slot, stack.copyWithCount(1).saveOptional(registries));
            }
        }
        if (stored.isEmpty()) {
            root.remove(CONTENTS_KEY);
        } else {
            root.put(CONTENTS_KEY, stored);
        }
        if (root.isEmpty()) {
            carrier.remove(DataComponents.CUSTOM_DATA);
        } else {
            carrier.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
