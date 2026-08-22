package com.reinhardt.hbm.item;

import com.reinhardt.hbm.block.PwrBlock;
import com.reinhardt.hbm.blockentity.PwrBlockEntity;
import com.reinhardt.hbm.blockentity.PwrControllerBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PwrPrinterItem extends Item {
    private static final String X1 = "x1";
    private static final String Y1 = "y1";
    private static final String Z1 = "z1";
    private static final String X2 = "x2";
    private static final String Y2 = "y2";
    private static final String Z2 = "z2";
    private static final String PARTS = "parts";
    private static final String RODS = "rods";
    private static final String PORTS = "ports";

    public PwrPrinterItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return useOnPwr(stack, context);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return useOnPwr(context.getItemInHand(), context);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_printer.1").withStyle(ChatFormatting.YELLOW));
        tooltip.add(Component.translatable("desc.reinhardtshbm.pwr_printer.2").withStyle(ChatFormatting.GRAY));
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains(PARTS)) {
            tooltip.add(Component.translatable(
                    "desc.reinhardtshbm.pwr_printer.bounds",
                    tag.getInt(X1), tag.getInt(Y1), tag.getInt(Z1),
                    tag.getInt(X2), tag.getInt(Y2), tag.getInt(Z2)
            ).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.translatable(
                    "desc.reinhardtshbm.pwr_printer.parts",
                    tag.getInt(PARTS), tag.getInt(RODS), tag.getInt(PORTS)
            ).withStyle(ChatFormatting.GREEN));
        }
    }

    private static InteractionResult useOnPwr(ItemStack stack, UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(clicked);
        if (!(blockEntity instanceof PwrControllerBlockEntity controller)) {
            return InteractionResult.PASS;
        }
        if (!controller.assembled()) {
            if (!level.isClientSide) {
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.pwr_printer.not_assembled").withStyle(ChatFormatting.RED), true);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!level.isClientSide) {
            Scan scan = scan(level, clicked, level.getBlockState(clicked).getValue(PwrBlock.FACING));
            if (scan.parts() <= 0) {
                player.displayClientMessage(Component.translatable("chat.reinhardtshbm.pwr_printer.no_parts").withStyle(ChatFormatting.RED), true);
                return InteractionResult.CONSUME;
            }
            writeScan(stack, scan);
            player.displayClientMessage(Component.translatable("chat.reinhardtshbm.pwr_printer.scanned", scan.parts()).withStyle(ChatFormatting.YELLOW), true);
            level.playSound(null, clicked, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.25F, 1.25F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static Scan scan(Level level, BlockPos controllerPos, Direction controllerFacing) {
        Direction dir = controllerFacing.getOpposite();
        BlockPos start = controllerPos.relative(dir);
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        queue.add(start);
        int rods = 0;
        int ports = 0;
        BlockPos min = start;
        BlockPos max = start;
        int parts = 0;
        while (!queue.isEmpty() && visited.size() < 4096) {
            BlockPos pos = queue.removeFirst();
            if (!visited.add(pos)) {
                continue;
            }
            BlockStateInfo info = blockInfo(level, pos);
            if (!info.isPwr()) {
                continue;
            }
            parts++;
            min = new BlockPos(Math.min(min.getX(), pos.getX()), Math.min(min.getY(), pos.getY()), Math.min(min.getZ(), pos.getZ()));
            max = new BlockPos(Math.max(max.getX(), pos.getX()), Math.max(max.getY(), pos.getY()), Math.max(max.getZ(), pos.getZ()));
            if (info.kind() == PwrBlock.Kind.FUEL) {
                rods++;
            }
            if (info.kind() == PwrBlock.Kind.PORT) {
                ports++;
            }
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!visited.contains(next) && blockInfo(level, next).isPwr()) {
                    queue.add(next);
                }
            }
        }
        return new Scan(min, max, parts, rods, ports);
    }

    private static BlockStateInfo blockInfo(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof PwrBlockEntity part) {
            return new BlockStateInfo(true, part.originalKind());
        }
        Block block = level.getBlockState(pos).getBlock();
        if (block instanceof PwrBlock pwr) {
            return new BlockStateInfo(true, pwr.kind());
        }
        return new BlockStateInfo(false, PwrBlock.Kind.BLOCK);
    }

    private static void writeScan(ItemStack stack, Scan scan) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putInt(X1, scan.min().getX());
        tag.putInt(Y1, scan.min().getY());
        tag.putInt(Z1, scan.min().getZ());
        tag.putInt(X2, scan.max().getX());
        tag.putInt(Y2, scan.max().getY());
        tag.putInt(Z2, scan.max().getZ());
        tag.putInt(PARTS, scan.parts());
        tag.putInt(RODS, scan.rods());
        tag.putInt(PORTS, scan.ports());
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    private record BlockStateInfo(boolean isPwr, PwrBlock.Kind kind) {
    }

    private record Scan(BlockPos min, BlockPos max, int parts, int rods, int ports) {
    }
}
