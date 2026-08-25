package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.screen.LegacyBookLoreScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/** Direct ItemBookLore port. Its individual title, author and pages live on the stack. */
public final class LegacyBookLoreItem extends Item {
    public static final String KEY = "k";
    public static final String PAGES = "p";
    public static final String COVER_COLOR = "cov_col";
    public static final String TITLE_COLOR = "tit_col";

    public LegacyBookLoreItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            LegacyBookLoreScreen.open(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public Component getName(ItemStack stack) {
        String key = key(stack);
        return key.isEmpty() ? Component.translatable("item.reinhardtshbm.book_lore")
                : Component.translatable("book_lore." + key + ".name");
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String key = key(stack);
        if (!key.isEmpty()) {
            tooltip.add(Component.translatable("book_lore.author", Component.translatable("book_lore." + key + ".author"))
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    public static ItemStack create(ItemStack book, String key, int pages, int coverColor, int titleColor) {
        CompoundTag data = data(book);
        data.putString(KEY, key);
        data.putInt(PAGES, pages);
        data.putInt(COVER_COLOR, coverColor);
        data.putInt(TITLE_COLOR, titleColor);
        book.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return book;
    }

    public static String key(ItemStack stack) {
        return data(stack).getString(KEY);
    }

    public static int pages(ItemStack stack) {
        return Math.max(0, data(stack).getInt(PAGES));
    }

    public static int coverColor(ItemStack stack) {
        int color = data(stack).getInt(COVER_COLOR);
        return color > 0 ? color : 0x303030;
    }

    public static int titleColor(ItemStack stack) {
        int color = data(stack).getInt(TITLE_COLOR);
        return color > 0 ? color : 0xFFFFFF;
    }

    public static int tint(ItemStack stack, int tintIndex) {
        return switch (tintIndex) {
            case 1 -> coverColor(stack);
            case 2 -> titleColor(stack);
            default -> 0xFFFFFFFF;
        };
    }

    public static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }
}
