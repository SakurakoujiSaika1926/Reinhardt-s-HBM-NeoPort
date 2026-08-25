package com.reinhardt.hbm.item;

import com.reinhardt.hbm.client.screen.GuideBookScreen;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLEnvironment;

import java.util.List;

/** Direct 1.7.10 ItemGuideBook port. The old metadata variants are data-component variants now. */
public final class GuideBookItem extends Item {
    private static final String TYPE_KEY = "guide_book_type";

    public GuideBookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide && FMLEnvironment.dist.isClient()) {
            GuideBookScreen.open(stack);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        for (String line : Component.translatable(type(stack).coverKey()).getString().split("\\$")) {
            if (!line.isBlank()) {
                tooltip.add(Component.literal(line).withStyle(ChatFormatting.GOLD));
            }
        }
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        // 1.7.10 exposes only RBMK and Starter in the creative inventory.
        output.accept(stackFor(Type.RBMK));
        output.accept(stackFor(Type.STARTER));
    }

    public static ItemStack stackFor(Type type) {
        ItemStack stack = new ItemStack(com.reinhardt.hbm.registry.HbmItems.BOOK_GUIDE.get());
        setType(stack, type);
        return stack;
    }

    public static Type type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return Type.byId(tag.getString(TYPE_KEY));
    }

    public static void setType(ItemStack stack, Type type) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putString(TYPE_KEY, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    public enum Type {
        TEST("test", "book.test.cover", 2.0F),
        RBMK("rbmk", "book.rbmk.cover", 1.5F),
        HADRON("hadron", "book.error.cover", 1.5F),
        STARTER("starter", "book.starter.cover", 1.5F);

        private final String id;
        private final String coverKey;
        private final float titleScale;

        Type(String id, String coverKey, float titleScale) {
            this.id = id;
            this.coverKey = coverKey;
            this.titleScale = titleScale;
        }

        public String id() {
            return id;
        }

        public String coverKey() {
            return coverKey;
        }

        public float titleScale() {
            return titleScale;
        }

        private static Type byId(String id) {
            for (Type type : values()) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return TEST;
        }
    }
}
