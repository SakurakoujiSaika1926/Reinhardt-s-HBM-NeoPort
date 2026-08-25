package com.reinhardt.hbm.item;

import com.reinhardt.hbm.entity.LegacyBomberEntity;
import com.reinhardt.hbm.registry.HbmSoundEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.List;

/** 1.7.10 ItemBombCaller: consumable long-range designator for bomber entities. */
public final class LegacyBombCallerItem extends Item {
    private static final String MODE = "airstrike_mode";

    public LegacyBombCallerItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        HitResult hit = player.pick(500.0D, 1.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }

        if (!level.isClientSide) {
            LegacyBomberEntity bomber = LegacyBomberEntity.create(level, blockHit.getBlockPos(), type(stack));
            level.addFreshEntity(bomber);
            level.playSound(null, player.blockPosition(), HbmSoundEvents.TECH_BLEEP.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.airstrike.called"), true);
            // 1.7.10 consumed the designator even for creative-mode players.
            stack.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.bomb_caller.usage").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(type(stack).translationKey()).withStyle(ChatFormatting.GRAY));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        // ItemBombCaller#hasEffect: types 4 through 7 are enchanted-looking.
        return type(stack).id() >= Type.ATOMIC.id();
    }

    public void addCreativeVariants(CreativeModeTab.Output output) {
        // The legacy creative menu exposed types 0 through 4; the remaining three
        // still retain their saved-item variants for commands and old content.
        for (int id = 0; id <= 4; id++) {
            output.accept(stackFor(Type.byId(id)));
        }
    }

    public static ItemStack stackFor(Type type) {
        ItemStack stack = new ItemStack(com.reinhardt.hbm.registry.HbmItems.BOMB_CALLER.get());
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(MODE, type.id());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(type.id()));
        return stack;
    }

    public static Type type(ItemStack stack) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(MODE)) {
            return Type.byId(tag.getInt(MODE));
        }
        CustomModelData model = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return Type.byId(model == null ? 0 : model.value());
    }

    public enum Type {
        CARPET(0, "tooltip.reinhardtshbm.bomb_caller.carpet"),
        NAPALM(1, "tooltip.reinhardtshbm.bomb_caller.napalm"),
        CHLORINE(2, "tooltip.reinhardtshbm.bomb_caller.chlorine"),
        AGENT_ORANGE(3, "tooltip.reinhardtshbm.bomb_caller.agent_orange"),
        ATOMIC(4, "tooltip.reinhardtshbm.bomb_caller.atomic"),
        STINGER(5, "tooltip.reinhardtshbm.bomb_caller.stinger"),
        BOXCARS(6, "tooltip.reinhardtshbm.bomb_caller.boxcars"),
        CLOUD(7, "tooltip.reinhardtshbm.bomb_caller.cloud");

        private final int id;
        private final String translationKey;

        Type(int id, String translationKey) {
            this.id = id;
            this.translationKey = translationKey;
        }

        public int id() {
            return id;
        }

        public String translationKey() {
            return translationKey;
        }

        public static Type byId(int id) {
            for (Type type : values()) {
                if (type.id == id) {
                    return type;
                }
            }
            return CARPET;
        }
    }
}
