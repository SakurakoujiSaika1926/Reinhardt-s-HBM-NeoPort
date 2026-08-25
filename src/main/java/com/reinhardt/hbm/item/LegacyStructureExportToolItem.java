package com.reinhardt.hbm.item;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.worldgen.structure.HbmStructureIO;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;

/** The five 1.7.10 structure code emitters, preserving their anchor/selection workflow. */
public final class LegacyStructureExportToolItem extends Item {
    public enum Mode { PATTERN, RANDOMIZED, RANDOMLY, SINGLE, SOLID, CUSTOM_MACHINE }

    private static final String ANCHOR = "anchor";
    private static final String FIRST = "first";
    private final Mode mode;

    public LegacyStructureExportToolItem(Properties properties, Mode mode) {
        super(properties.stacksTo(1));
        this.mode = mode;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        ItemStack stack = context.getItemInHand();
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        CompoundTag tag = data(stack);

        if (isAnchor(level, clicked)) {
            tag.putLong(ANCHOR, clicked.asLong());
            store(stack, tag);
            message(player, level, "chat.reinhardtshbm.structure_tool.anchor", clicked.getX(), clicked.getY(), clicked.getZ());
            return InteractionResult.sidedSuccess(level.isClientSide);
        }

        BlockPos anchor = pos(tag, ANCHOR);
        if (anchor == null) {
            message(player, level, "chat.reinhardtshbm.structure_tool.no_anchor");
            return InteractionResult.FAIL;
        }

        if (requiresSelection()) {
            BlockPos first = pos(tag, FIRST);
            if (first == null) {
                tag.putLong(FIRST, clicked.asLong());
                store(stack, tag);
                message(player, level, "chat.reinhardtshbm.structure_tool.first");
                return InteractionResult.sidedSuccess(level.isClientSide);
            }
            tag.remove(FIRST);
            store(stack, tag);
            if (!level.isClientSide) {
                write(level, anchor, first, clicked);
            }
        } else if (!level.isClientSide) {
            write(level, anchor, clicked, clicked);
        }
        message(player, level, "chat.reinhardtshbm.structure_tool.written");
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private boolean requiresSelection() {
        return this.mode != Mode.SINGLE && this.mode != Mode.CUSTOM_MACHINE;
    }

    private void write(Level level, BlockPos anchor, BlockPos first, BlockPos last) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        int minX = Math.min(first.getX(), last.getX()) - anchor.getX();
        int minY = Math.min(first.getY(), last.getY()) - anchor.getY();
        int minZ = Math.min(first.getZ(), last.getZ()) - anchor.getZ();
        int maxX = Math.max(first.getX(), last.getX()) - anchor.getX();
        int maxY = Math.max(first.getY(), last.getY()) - anchor.getY();
        int maxZ = Math.max(first.getZ(), last.getZ()) - anchor.getZ();
        String target = legacyBlock(serverLevel, last);
        String line = switch (this.mode) {
            case PATTERN -> pattern(serverLevel, anchor, minX, minY, minZ, maxX, maxY, maxZ);
            case RANDOMIZED -> "fillWithRandomizedBlocks(world, box, " + minX + ", " + minY + ", " + minZ + ", " + maxX + ", " + maxY + ", " + maxZ + ", rand, <block-selector>);";
            case RANDOMLY -> "randomlyFillWithBlocks(world, box, rand, <limit>, " + minX + ", " + minY + ", " + minZ + ", " + maxX + ", " + maxY + ", " + maxZ + ", " + target + ");";
            case SINGLE -> "placeBlockAtCurrentPosition(world, " + target + ", 0, " + minX + ", " + minY + ", " + minZ + ", box);";
            case SOLID -> "fillWithBlocks(world, box, " + minX + ", " + minY + ", " + minZ + ", " + maxX + ", " + maxY + ", " + maxZ + ", " + target + ");";
            case CUSTOM_MACHINE -> "{\"anchor\": [" + minX + ", " + minY + ", " + minZ + "], \"block\": \"" + target + "\"}";
        };
        Path output = FMLPaths.GAMEDIR.get().resolve(this.mode == Mode.CUSTOM_MACHINE ? "CMstructureOutput.txt" : "structureOutput.txt");
        try {
            Files.writeString(output, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException exception) {
            ReinhardtsHBM.LOGGER.error("Failed to write legacy structure tool output", exception);
        }
    }

    private static String pattern(ServerLevel level, BlockPos anchor, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        StringBuilder output = new StringBuilder();
        for (int x = minX; x <= maxX; x++) for (int y = minY; y <= maxY; y++) for (int z = minZ; z <= maxZ; z++) {
            BlockPos at = anchor.offset(x, y, z);
            if (!level.getBlockState(at).isAir()) {
                output.append("placeBlockAtCurrentPosition(world, ").append(legacyBlock(level, at))
                        .append(", 0, ").append(x).append(", ").append(y).append(", ").append(z).append(", box);").append(System.lineSeparator());
            }
        }
        return output.toString().stripTrailing();
    }

    private static String legacyBlock(ServerLevel level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString();
    }

    private static boolean isAnchor(Level level, BlockPos pos) {
        return BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath().equals("structure_anchor");
    }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
    }

    private static void store(ItemStack stack, CompoundTag tag) {
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private static BlockPos pos(CompoundTag tag, String key) {
        return tag.contains(key) ? BlockPos.of(tag.getLong(key)) : null;
    }

    private static void message(Player player, Level level, String key, Object... args) {
        if (!level.isClientSide) player.displayClientMessage(Component.translatable(key, args), false);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        BlockPos anchor = pos(data(stack), ANCHOR);
        tooltip.add(Component.translatable("tooltip.reinhardtshbm.structure_tool." + this.mode.name().toLowerCase()).withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable(anchor == null ? "chat.reinhardtshbm.structure_tool.no_anchor" : "chat.reinhardtshbm.structure_tool.anchor", anchor.getX(), anchor.getY(), anchor.getZ()).withStyle(anchor == null ? ChatFormatting.RED : ChatFormatting.GREEN));
    }
}
