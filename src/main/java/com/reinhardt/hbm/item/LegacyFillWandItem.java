package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtTemplate;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/** Exact two-point fill wand used by the 1.7.10 creative tools tab. */
public final class LegacyFillWandItem extends Item {
    private static final String FIRST = "first";
    private static final String BLOCK = "block";
    private static final String META = "meta";

    public LegacyFillWandItem() {
        super(new Properties().stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        CompoundTag tag = data(stack);

        if (player.isShiftKeyDown()) {
            HbmStructureIO.LegacyBlockKey key = HbmStructureIO.keyFromState(level.getBlockState(clicked));
            tag.putString(BLOCK, key.id());
            tag.putInt(META, key.meta());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            message(player, level, "chat.reinhardtshbm.wand_k.block", key.id());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos first = readPos(tag);
        if (first == null) {
            tag.putLong(FIRST, clicked.asLong());
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
            message(player, level, "chat.reinhardtshbm.wand_k.first");
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        tag.remove(FIRST);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        String blockId = tag.getString(BLOCK);
        int meta = tag.getInt(META);
        BlockState fill = HbmLegacyNbtTemplate.stateFromLegacyId(blockId, meta);
        boolean airOnly = blockId.equals(ReinhardtsHBM.id("wand_air").toString());
        if (!level.isClientSide) {
            int minX = Math.min(first.getX(), clicked.getX());
            int maxX = Math.max(first.getX(), clicked.getX());
            int minY = Math.min(first.getY(), clicked.getY());
            int maxY = Math.max(first.getY(), clicked.getY());
            int minZ = Math.min(first.getZ(), clicked.getZ());
            int maxZ = Math.max(first.getZ(), clicked.getZ());
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (airOnly && !level.getBlockState(pos).isAir()) {
                            continue;
                        }
                        level.setBlock(pos, airOnly ? Blocks.AIR.defaultBlockState() : fill, 3);
                    }
                }
            }
        }
        message(player, level, "chat.reinhardtshbm.wand_k.filled");
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.reinhardtshbm.wand_k.1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("desc.reinhardtshbm.wand_k.2").withStyle(ChatFormatting.GRAY));
        CompoundTag tag = data(stack);
        tooltip.add(Component.translatable(
                tag.contains(FIRST) ? "desc.reinhardtshbm.wand_k.has_first" : "desc.reinhardtshbm.wand_k.no_first"
        ).withStyle(ChatFormatting.AQUA));
        if (tag.contains(BLOCK)) {
            tooltip.add(Component.translatable("desc.reinhardtshbm.wand_k.block", tag.getString(BLOCK)).withStyle(ChatFormatting.AQUA));
        }
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static BlockPos readPos(CompoundTag tag) {
        return tag.contains(FIRST) ? BlockPos.of(tag.getLong(FIRST)) : null;
    }

    private static void message(Player player, Level level, String key, Object... args) {
        if (!level.isClientSide) {
            player.displayClientMessage(Component.translatable(key, args), false);
        }
    }
}
