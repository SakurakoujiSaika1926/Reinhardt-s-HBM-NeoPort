package com.reinhardt.hbm.integration.createdieselgenerators;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.reinhardt.hbm.ReinhardtsHBM;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.util.Locale;

@EventBusSubscriber(modid = ReinhardtsHBM.MOD_ID)
public final class HbmCreateDieselGeneratorsCommandCompat {
    private static final String MOD_ID = "createdieselgenerators";
    private static final int LOCATE_RADIUS_CHUNKS = 10;

    private HbmCreateDieselGeneratorsCommandCompat() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void registerCommands(RegisterCommandsEvent event) {
        if (!ModList.get().isLoaded(MOD_ID)) {
            return;
        }
        register(event.getDispatcher());
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("cdg")
                .then(Commands.literal("oil")
                        .then(Commands.literal("get")
                                .executes(HbmCreateDieselGeneratorsCommandCompat::getOilChunk))
                        .then(Commands.literal("locate")
                                .executes(HbmCreateDieselGeneratorsCommandCompat::locateOilChunk))
                        .then(Commands.literal("regenerate")
                                .executes(HbmCreateDieselGeneratorsCommandCompat::regenerateOilChunk))
                        .then(Commands.literal("set")
                                .then(Commands.literal("infinity")
                                        .executes(HbmCreateDieselGeneratorsCommandCompat::setOilChunkInfinite))
                                .then(Commands.argument("amount", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                        .executes(HbmCreateDieselGeneratorsCommandCompat::setOilChunk)))));
    }

    private static int getOilChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.hasPermission(2)) {
            return 0;
        }
        int amount = HbmCreateDieselGeneratorsOilCompat.getChunkOilAmount(source.getLevel(), chunkAt(source));
        if (amount == Integer.MAX_VALUE) {
            source.sendSuccess(() -> message("command.reinhardtshbm.cdg.oil.get_infinite"), false);
        } else {
            source.sendSuccess(() -> message("command.reinhardtshbm.cdg.oil.get", amount(amount)), false);
        }
        return 1;
    }

    private static int setOilChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.hasPermission(2)) {
            return 0;
        }
        int amount = IntegerArgumentType.getInteger(context, "amount");
        HbmCreateDieselGeneratorsOilCompat.setChunkOilAmount(source.getLevel(), chunkAt(source), amount);
        source.sendSuccess(() -> message("command.reinhardtshbm.cdg.oil.set", amount(amount)), false);
        return 1;
    }

    private static int setOilChunkInfinite(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.hasPermission(2)) {
            return 0;
        }
        HbmCreateDieselGeneratorsOilCompat.setChunkOilAmount(source.getLevel(), chunkAt(source), Integer.MAX_VALUE);
        source.sendSuccess(() -> message("command.reinhardtshbm.cdg.oil.set_infinite"), false);
        return 1;
    }

    private static int regenerateOilChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.hasPermission(2)) {
            return 0;
        }
        HbmCreateDieselGeneratorsOilCompat.removeChunk(source.getLevel(), chunkAt(source));
        source.sendSuccess(() -> message("command.reinhardtshbm.cdg.oil.regenerate"), false);
        return 1;
    }

    private static int locateOilChunk(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        if (!source.hasPermission(2)) {
            return 0;
        }
        ServerLevel level = source.getLevel();
        ChunkPos origin = chunkAt(source);
        for (int radius = 0; radius <= LOCATE_RADIUS_CHUNKS; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != radius) {
                        continue;
                    }
                    ChunkPos candidate = new ChunkPos(origin.x + dx, origin.z + dz);
                    int amount = HbmCreateDieselGeneratorsOilCompat.getChunkOilAmount(level, candidate);
                    if (amount > 0) {
                        source.sendSuccess(() -> message(
                                "command.reinhardtshbm.cdg.oil.locate",
                                number(candidate.x),
                                number(candidate.z),
                                number(candidate.getMinBlockX() + 8),
                                number(candidate.getMinBlockZ() + 8),
                                amount(amount)
                        ), false);
                        return 1;
                    }
                }
            }
        }
        source.sendFailure(message("command.reinhardtshbm.cdg.oil.locate_none").withStyle(ChatFormatting.RED));
        return 1;
    }

    private static ChunkPos chunkAt(CommandSourceStack source) {
        return new ChunkPos(BlockPos.containing(source.getPosition()));
    }

    private static MutableComponent message(String key, Object... args) {
        return Component.translatable(key, args).withStyle(ChatFormatting.GRAY);
    }

    private static Component amount(int amount) {
        if (amount == Integer.MAX_VALUE) {
            return Component.literal("∞").withStyle(ChatFormatting.GOLD);
        }
        return number(amount).copy().append(" mB");
    }

    private static Component number(int value) {
        return Component.literal(String.format(Locale.ROOT, "%,d", value)).withStyle(ChatFormatting.GOLD);
    }
}
