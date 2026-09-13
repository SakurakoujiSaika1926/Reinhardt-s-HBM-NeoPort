package com.reinhardt.hbm.item;

import com.reinhardt.hbm.registry.HbmSoundEvents;
import com.reinhardt.hbm.util.SavedItemStackPreview;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/** 1.7.10 ItemKitCustom and ItemKitNBT, with the old stack-backed payload. */
public final class LegacyCustomKitItem extends Item {
    private static final String CONTENTS = "items";
    private static final String COLOR_ONE = "color1";
    private static final String COLOR_TWO = "color2";

    public LegacyCustomKitItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack kit = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(kit);
        }

        HolderLookup.Provider registries = level.registryAccess();
        for (ItemStack content : contents(kit, registries)) {
            if (!content.isEmpty()) {
                player.getInventory().add(content.copy());
            }
        }
        level.playSound(null, player.blockPosition(), HbmSoundEvents.ITEM_UNPACK.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
        if (!player.getAbilities().instabuild) {
            kit.shrink(1);
        }
        return InteractionResultHolder.consume(kit);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        List<ItemStack> contents = contentsForTooltip(stack);
        if (contents.isEmpty()) {
            return;
        }
        tooltip.add(Component.literal("Contains:"));
        for (ItemStack content : contents) {
            tooltip.add(Component.literal("-" + content.getHoverName().getString()
                    + (content.getCount() > 1 ? " x" + content.getCount() : "")));
        }
    }

    public static ItemStack create(ItemStack kit, HolderLookup.Provider registries, int colorOne, int colorTwo, ItemStack... contents) {
        setColors(kit, colorOne, colorTwo);
        setContents(kit, registries, List.of(contents));
        return kit;
    }

    public static void setContents(ItemStack kit, HolderLookup.Provider registries, List<ItemStack> contents) {
        CompoundTag root = data(kit);
        ListTag serialized = new ListTag();
        for (int slot = 0; slot < contents.size(); slot++) {
            ItemStack content = contents.get(slot);
            if (!content.isEmpty()) {
                Tag encoded = content.saveOptional(registries);
                if (!(encoded instanceof CompoundTag saved)) {
                    continue;
                }
                saved.putByte("slot", (byte) slot);
                serialized.add(saved);
            }
        }
        if (serialized.isEmpty()) {
            root.remove(CONTENTS);
        } else {
            root.put(CONTENTS, serialized);
        }
        save(kit, root);
    }

    public static List<ItemStack> contents(ItemStack kit, HolderLookup.Provider registries) {
        CompoundTag root = data(kit);
        List<ItemStack> contents = new ArrayList<>();
        ListTag serialized = root.getList(CONTENTS, Tag.TAG_COMPOUND);
        for (int index = 0; index < serialized.size(); index++) {
            CompoundTag saved = serialized.getCompound(index);
            ItemStack content = ItemStack.parseOptional(registries, saved);
            if (!content.isEmpty()) {
                contents.add(content);
            }
        }
        return contents;
    }

    public static List<ItemStack> contentsForTooltip(ItemStack kit) {
        return SavedItemStackPreview.listFromCustomData(kit, CONTENTS);
    }

    public static void setColors(ItemStack kit, int colorOne, int colorTwo) {
        CompoundTag root = data(kit);
        root.putInt(COLOR_ONE, colorOne);
        root.putInt(COLOR_TWO, colorTwo);
        save(kit, root);
    }

    public static int tint(ItemStack stack, int tintIndex) {
        CompoundTag root = data(stack);
        return switch (tintIndex) {
            case 1 -> root.getInt(COLOR_ONE);
            case 2 -> root.getInt(COLOR_TWO);
            default -> 0xFFFFFFFF;
        };
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void save(ItemStack stack, CompoundTag root) {
        if (root.isEmpty()) {
            stack.remove(DataComponents.CUSTOM_DATA);
        } else {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        }
    }
}
