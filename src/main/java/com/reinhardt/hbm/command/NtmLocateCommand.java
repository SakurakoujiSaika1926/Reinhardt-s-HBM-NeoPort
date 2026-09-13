package com.reinhardt.hbm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyNbtStructure;
import com.reinhardt.hbm.worldgen.structure.HbmLegacyScatterStructure;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.concurrent.CompletableFuture;
import java.util.ArrayList;
import java.util.List;

public final class NtmLocateCommand {
    private static final int MAX_DISTANCE_CHUNKS = 256;

    private NtmLocateCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ntmlocate")
                .requires(source -> source.hasPermission(4) && source.getEntity() instanceof ServerPlayer)
                .then(Commands.literal("list")
                        .executes(NtmLocateCommand::list))
                .then(Commands.literal("structure")
                        .then(Commands.argument("name", StringArgumentType.word())
                                .suggests(NtmLocateCommand::suggestStructures)
                                .executes(NtmLocateCommand::locate))));
    }

    private static int list(CommandContext<CommandSourceStack> context) {
        List<String> names = allStructureNames();
        context.getSource().sendSuccess(
                () -> Component.literal(String.join(", ", names)),
                false
        );
        return names.size();
    }

    private static int locate(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "name");
        if (!allStructureNames().contains(name)) {
            context.getSource().sendFailure(Component.translatable("commands.locate.no_match"));
            return 0;
        }

        BlockPos sourcePos = BlockPos.containing(context.getSource().getPosition());
        ChunkPos start = new ChunkPos(sourcePos);
        ChunkPos found = nearest(context.getSource(), name, start);
        if (found == null) {
            context.getSource().sendFailure(Component.translatable("commands.locate.none_found"));
            return 0;
        }

        int x = found.getMinBlockX();
        int z = found.getMinBlockZ();
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.locate.success.coordinates", name, x, z)
                        .withStyle(ChatFormatting.GREEN),
                false
        );
        return 1;
    }

    private static ChunkPos nearest(CommandSourceStack source, String name, ChunkPos start) {
        if (name.equals(selectedNameAt(source, name, start))) {
            return start;
        }
        for (int radius = 1; radius < MAX_DISTANCE_CHUNKS; radius++) {
            for (int x = start.x - radius; x <= start.x + radius; x++) {
                ChunkPos north = new ChunkPos(x, start.z - radius);
                if (name.equals(selectedNameAt(source, name, north))) {
                    return north;
                }
                ChunkPos south = new ChunkPos(x, start.z + radius);
                if (name.equals(selectedNameAt(source, name, south))) {
                    return south;
                }
            }
            for (int z = start.z - radius; z <= start.z + radius; z++) {
                ChunkPos west = new ChunkPos(start.x - radius, z);
                if (name.equals(selectedNameAt(source, name, west))) {
                    return west;
                }
                ChunkPos east = new ChunkPos(start.x + radius, z);
                if (name.equals(selectedNameAt(source, name, east))) {
                    return east;
                }
            }
        }
        return null;
    }

    private static CompletableFuture<Suggestions> suggestStructures(
            CommandContext<CommandSourceStack> context,
            SuggestionsBuilder builder
    ) {
        return SharedSuggestionProvider.suggest(allStructureNames(), builder);
    }

    private static List<String> allStructureNames() {
        List<String> names = new ArrayList<>(HbmLegacyNbtStructure.listStructureNames());
        names.addAll(HbmLegacyScatterStructure.listStructureNames());
        return names;
    }

    private static String selectedNameAt(CommandSourceStack source, String name, ChunkPos chunk) {
        String nbt = HbmLegacyNbtStructure.selectedNameAt(source.getLevel(), chunk);
        if (name.equals(nbt)) {
            return nbt;
        }
        return HbmLegacyScatterStructure.selectedNameAt(source.getLevel(), chunk, name);
    }
}
