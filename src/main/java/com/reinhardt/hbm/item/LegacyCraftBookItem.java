package com.reinhardt.hbm.item;

import com.reinhardt.hbm.menu.LegacyCraftBookMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/** The two portable crafting books from 1.7.10. */
public final class LegacyCraftBookItem extends Item {
    public enum Kind {
        BOXCARS("book_of_", "container.reinhardtshbm.book_of_"),
        LEMEGETON("book_lemegeton", "container.reinhardtshbm.book_lemegeton");

        private final String itemId;
        private final String titleKey;

        Kind(String itemId, String titleKey) {
            this.itemId = itemId;
            this.titleKey = titleKey;
        }

        public String itemId() {
            return itemId;
        }

        public String titleKey() {
            return titleKey;
        }
    }

    private final Kind kind;

    public LegacyCraftBookItem(Properties properties, Kind kind) {
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
                            (containerId, inventory, ignored) -> new LegacyCraftBookMenu(containerId, inventory, hand, kind),
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

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (kind == Kind.BOXCARS) {
            tooltip.add(Component.literal("Edition 4, gold lined pages").withStyle(ChatFormatting.GRAY));
        }
    }
}
