package com.reinhardt.hbm.item;

import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import com.reinhardt.hbm.registry.HbmBlocks;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StructureWandItem extends Item {
    private static final String X = "x";
    private static final String Y = "y";
    private static final String Z = "z";
    private static final String HAS_POS = "hasPos";
    private static final String BLOCKS = "blocks";

    public StructureWandItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return net.minecraft.world.InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        CompoundTag tag = data(stack);

        if (player.isShiftKeyDown()) {
            HbmStructureIO.LegacyBlockKey key = HbmStructureIO.keyFromState(level.getBlockState(pos));
            Set<HbmStructureIO.LegacyBlockKey> blacklist = blacklist(tag);
            if (!blacklist.remove(key)) {
                blacklist.add(key);
                message(player, level, "chat.reinhardtshbm.wand_s.blacklist_added", key.id());
            } else {
                message(player, level, "chat.reinhardtshbm.wand_s.blacklist_removed", key.id());
            }
            putBlacklist(tag, blacklist);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos first = firstPos(tag);
        if (first == null) {
            putPos(tag, pos);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            message(player, level, "chat.reinhardtshbm.wand_s.first");
            return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
        }

        clearPos(tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        BlockPos min = new BlockPos(
                Math.min(first.getX(), pos.getX()),
                Math.min(first.getY(), pos.getY()) - 1,
                Math.min(first.getZ(), pos.getZ())
        );
        if (!level.isClientSide) {
            int sizeX = Math.abs(pos.getX() - first.getX()) + 1;
            int sizeY = Math.abs(pos.getY() - first.getY()) + 1;
            int sizeZ = Math.abs(pos.getZ() - first.getZ()) + 1;
            BlockState state = HbmBlocks.WAND_STRUCTURE.get().defaultBlockState();
            level.setBlock(min, state, 3);
            if (level.getBlockEntity(min) instanceof WandStructureBlockEntity structure) {
                structure.sizeX = sizeX;
                structure.sizeY = sizeY;
                structure.sizeZ = sizeZ;
                structure.blacklist().clear();
                structure.blacklist().addAll(blacklist(tag));
                structure.sync();
            }
        }
        message(player, level, "chat.reinhardtshbm.wand_s.created", min.getX(), min.getY(), min.getZ());
        return net.minecraft.world.InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand usedHand) {
        ItemStack stack = player.getItemInHand(usedHand);
        if (player.isShiftKeyDown()) {
            CompoundTag tag = data(stack);
            tag.remove(BLOCKS);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            message(player, level, "chat.reinhardtshbm.wand_s.blacklist_cleared");
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.reinhardtshbm.wand_s.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("desc.reinhardtshbm.wand_s.2").withStyle(ChatFormatting.GRAY));
        CompoundTag tag = data(stack);
        BlockPos first = firstPos(tag);
        if (first == null) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.wand_s.no_start").withStyle(ChatFormatting.AQUA));
        } else {
            tooltip.add(Component.translatable("desc.reinhardtshbm.wand_s.from", first.getX(), first.getY(), first.getZ()).withStyle(ChatFormatting.AQUA));
        }
        Set<HbmStructureIO.LegacyBlockKey> blacklist = blacklist(tag);
        if (!blacklist.isEmpty()) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.wand_s.blacklist").withStyle(ChatFormatting.GRAY));
            for (HbmStructureIO.LegacyBlockKey key : blacklist) {
                tooltip.add(Component.literal("- " + key.id()).withStyle(ChatFormatting.RED));
            }
        }
    }

    private static void message(Player player, Level level, String key, Object... args) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(key, args), false);
        }
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void putPos(CompoundTag tag, BlockPos pos) {
        tag.putBoolean(HAS_POS, true);
        tag.putInt(X, pos.getX());
        tag.putInt(Y, pos.getY());
        tag.putInt(Z, pos.getZ());
    }

    private static void clearPos(CompoundTag tag) {
        tag.remove(HAS_POS);
        tag.remove(X);
        tag.remove(Y);
        tag.remove(Z);
    }

    private static BlockPos firstPos(CompoundTag tag) {
        if (!tag.getBoolean(HAS_POS) && !(tag.contains(X) || tag.contains(Y) || tag.contains(Z))) {
            return null;
        }
        int x = tag.getInt(X);
        int y = tag.getInt(Y);
        int z = tag.getInt(Z);
        return new BlockPos(x, y, z);
    }

    private static Set<HbmStructureIO.LegacyBlockKey> blacklist(CompoundTag tag) {
        Set<HbmStructureIO.LegacyBlockKey> result = new HashSet<>();
        ListTag list = tag.getList(BLOCKS, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            result.add(new HbmStructureIO.LegacyBlockKey(entry.getString("block"), entry.getInt("meta")));
        }
        return result;
    }

    private static void putBlacklist(CompoundTag tag, Set<HbmStructureIO.LegacyBlockKey> blacklist) {
        ListTag list = new ListTag();
        for (HbmStructureIO.LegacyBlockKey key : blacklist) {
            CompoundTag entry = new CompoundTag();
            entry.putString("block", key.id());
            entry.putInt("meta", key.meta());
            list.add(entry);
        }
        tag.put(BLOCKS, list);
    }
}
