package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.util.FluidCopiable;
import com.reinhardt.hbm.util.SettingsCopiable;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SettingsToolItem extends Item {
    private static final String TILE_NAME = "tileName";
    private static final String COPY_INDEX = "copyIndex";
    private static final String INPUT_DELAY = "inputDelay";
    private static final String DISPLAY_INFO = "displayInfo";
    private static final Map<UUID, KeyState> KEY_STATES = new ConcurrentHashMap<>();

    public SettingsToolItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static void setKeyState(Player player, boolean ctrl, boolean alt) {
        KEY_STATES.put(player.getUUID(), new KeyState(ctrl, alt));
    }

    public static boolean isCtrlDown(Player player) {
        return KEY_STATES.getOrDefault(player.getUUID(), KeyState.NONE).ctrl();
    }

    public static boolean isAltDown(Player player) {
        return KEY_STATES.getOrDefault(player.getUUID(), KeyState.NONE).alt();
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOnCopiable(stack, context.getLevel(), context.getPlayer(), context.getClickedPos());
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useOnCopiable(context.getItemInHand(), context.getLevel(), context.getPlayer(), context.getClickedPos());
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (level.isClientSide || !isSelected || !(entity instanceof ServerPlayer player)) {
            return;
        }
        if (player.getMainHandItem() != stack || !hasCopiedSettings(stack)) {
            return;
        }

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int delay = tag.getInt(INPUT_DELAY) + 1;
        List<Component> info = displayInfo(tag);
        int selectableCount = copiedSelectionCount(tag, info);
        if (isAltDown(player) && selectableCount > 1 && delay > 4) {
            int index = tag.getInt(COPY_INDEX) + 1;
            if (index >= selectableCount) {
                index = 0;
            }
            tag.putInt(COPY_INDEX, index);
            delay = 0;
        }
        tag.putInt(INPUT_DELAY, delay);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));

        if (!info.isEmpty() && level.getGameTime() % 20L == 0L) {
            int index = Math.max(0, Math.min(tag.getInt(COPY_INDEX), info.size() - 1));
            player.displayClientMessage(Component.translatable(
                    "chat.reinhardtshbm.settings_tool.selected",
                    index + 1,
                    info.size(),
                    info.get(index)
            ), true);
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.reinhardtshbm.settings_tool.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("desc.reinhardtshbm.settings_tool.2").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("desc.reinhardtshbm.settings_tool.3").withStyle(ChatFormatting.GRAY));

        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!hasCopiedSettings(tag)) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.settings_tool.none").withStyle(ChatFormatting.RED));
            return;
        }

        tooltip.add(Component.translatable("desc.reinhardtshbm.settings_tool.source",
                Component.translatable(tag.getString(TILE_NAME))).withStyle(ChatFormatting.AQUA));
        List<Component> info = displayInfo(tag);
        int selected = tag.getInt(COPY_INDEX);
        for (int i = 0; i < info.size(); i++) {
            ChatFormatting color = i == selected ? ChatFormatting.AQUA : ChatFormatting.YELLOW;
            tooltip.add(Component.literal("  ").append(info.get(i)).withStyle(color));
        }
    }

    private static InteractionResult useOnCopiable(ItemStack stack, Level level, @Nullable Player player, BlockPos clickedPos) {
        if (player == null) {
            return InteractionResult.PASS;
        }

        CopyTarget target = copyTarget(level, clickedPos);
        if (target == null) {
            return InteractionResult.PASS;
        }

        if (player.isShiftKeyDown()) {
            if (!level.isClientSide) {
                copySettings(stack, level, player, target);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!hasCopiedSettings(stack)) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.settings_tool.no_settings"), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        if (!level.isClientSide) {
            pasteSettings(stack, level, player, target);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static void copySettings(ItemStack stack, Level level, Player player, CopyTarget target) {
        CompoundTag settings = target.copiable().getSettings(level, target.pos());
        if (settings.isEmpty()) {
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.settings_tool.no_target"), true);
            return;
        }

        CompoundTag tag = settings.copy();
        tag.putString(TILE_NAME, target.copiable().getSettingsSourceId(level, target.pos()));
        tag.putInt(COPY_INDEX, 0);
        tag.putInt(INPUT_DELAY, 0);
        putDisplayInfo(tag, target.copiable().settingsInfo(level, target.pos(), settings));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        level.playSound(null, target.pos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
        player.displayClientMessage(Component.translatable(
                "chat.reinhardtshbm.settings_tool.copied",
                Component.translatable(tag.getString(TILE_NAME))
        ), true);
    }

    private static void pasteSettings(ItemStack stack, Level level, Player player, CopyTarget target) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int index = tag.getInt(COPY_INDEX);
        target.copiable().pasteSettings(tag, index, level, player, target.pos());
        level.playSound(null, target.pos(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.2F, 0.9F);
        player.displayClientMessage(Component.translatable(
                "chat.reinhardtshbm.settings_tool.pasted",
                Component.translatable(target.copiable().getSettingsSourceId(level, target.pos()))
        ), true);
    }

    @Nullable
    private static CopyTarget copyTarget(Level level, BlockPos clickedPos) {
        BlockPos pos = resolveCore(level, clickedPos);
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof SettingsCopiable copiable) {
            return new CopyTarget(pos, copiable);
        }
        return null;
    }

    private static BlockPos resolveCore(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineDummyBlockEntity dummy) {
            return dummy.getCorePos();
        }
        return pos;
    }

    private static boolean hasCopiedSettings(ItemStack stack) {
        return hasCopiedSettings(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag());
    }

    private static boolean hasCopiedSettings(CompoundTag tag) {
        return tag.contains(TILE_NAME) && hasPayload(tag);
    }

    private static boolean hasPayload(CompoundTag tag) {
        for (String key : tag.getAllKeys()) {
            if (!key.equals(TILE_NAME) && !key.equals(COPY_INDEX) && !key.equals(INPUT_DELAY) && !key.equals(DISPLAY_INFO)) {
                return true;
            }
        }
        return false;
    }

    private static int copiedSelectionCount(CompoundTag tag, List<Component> info) {
        if (tag.contains(FluidCopiable.FLUID_ID)) {
            return tag.getIntArray(FluidCopiable.FLUID_ID).length;
        }
        return Math.max(1, info.size());
    }

    private static List<Component> displayInfo(CompoundTag tag) {
        if (tag.contains(FluidCopiable.FLUID_ID)) {
            return FluidCopiable.fluidInfo(tag);
        }
        ListTag list = tag.getList(DISPLAY_INFO, Tag.TAG_STRING);
        return list.stream()
                .map(Tag::getAsString)
                .filter(value -> !value.isBlank())
                .map(value -> (Component) Component.literal(value))
                .toList();
    }

    private static void putDisplayInfo(CompoundTag tag, List<Component> info) {
        if (info.isEmpty()) {
            return;
        }
        ListTag list = new ListTag();
        for (Component component : info) {
            list.add(StringTag.valueOf(component.getString()));
        }
        tag.put(DISPLAY_INFO, list);
    }

    private record CopyTarget(BlockPos pos, SettingsCopiable copiable) {
    }

    private record KeyState(boolean ctrl, boolean alt) {
        private static final KeyState NONE = new KeyState(false, false);
    }
}
