package com.reinhardt.hbm.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.reinhardt.hbm.pollution.HbmPollutionData;
import com.reinhardt.hbm.pollution.HbmPollutionType;
import com.reinhardt.hbm.radiation.ChunkRadiationData;
import com.reinhardt.hbm.radiation.HbmLivingRadiation;
import net.minecraft.core.BlockPos;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RhbmCommand {
    private static final SimpleCommandExceptionType ERROR_NOT_LIVING = new SimpleCommandExceptionType(
            Component.translatable("commands.reinhardtshbm.not_living")
    );

    private RhbmCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        NtmLocateCommand.register(dispatcher);
        dispatcher.register(
                Commands.literal("rhbm")
                        .then(Commands.literal("radiation")
                                .then(Commands.literal("get")
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(RhbmCommand::getRadiation)))
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, HbmLivingRadiation.MAX_RADIATION))
                                                        .executes(RhbmCommand::setRadiation))))
                                .then(Commands.literal("add")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                        .executes(RhbmCommand::addRadiation))))
                                .then(Commands.literal("clear")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("target", EntityArgument.entity())
                                                .executes(RhbmCommand::clearRadiation)))
                                .then(Commands.literal("chunk")
                                        .then(Commands.literal("get")
                                                .executes(RhbmCommand::getChunkRadiationHere)
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(RhbmCommand::getChunkRadiation)))
                                        .then(Commands.literal("set")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, HbmLivingRadiation.MAX_RADIATION))
                                                                .executes(RhbmCommand::setChunkRadiation))))
                                        .then(Commands.literal("add")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                                .executes(RhbmCommand::addChunkRadiation))))
                                        .then(Commands.literal("clear")
                                                .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .executes(RhbmCommand::clearChunkRadiation)))))
        );
        dispatcher.register(
                Commands.literal("rhbm")
                        .then(Commands.literal("pollution")
                                .then(Commands.literal("get")
                                        .executes(RhbmCommand::getPollutionHere)
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(RhbmCommand::getPollution)))
                                .then(Commands.literal("set")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("value", FloatArgumentType.floatArg(0.0F, 10_000.0F))
                                                                .executes(RhbmCommand::setPollution)))))
                                .then(Commands.literal("add")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("type", StringArgumentType.word())
                                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                        .then(Commands.argument("value", FloatArgumentType.floatArg())
                                                                .executes(RhbmCommand::addPollution)))))
                                .then(Commands.literal("clear")
                                        .requires(source -> source.hasPermission(2))
                                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                                .executes(RhbmCommand::clearPollution))))
        );
    }

    private static int getPollutionHere(CommandContext<CommandSourceStack> context) {
        BlockPos pos = BlockPos.containing(context.getSource().getPosition());
        return sendPollution(context, pos);
    }

    private static int getPollution(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return sendPollution(context, BlockPosArgument.getLoadedBlockPos(context, "pos"));
    }

    private static int sendPollution(CommandContext<CommandSourceStack> context, BlockPos pos) {
        HbmPollutionData.PollutionValues values = HbmPollutionData.get(context.getSource().getLevel()).get(pos);
        context.getSource().sendSuccess(
                () -> Component.literal(String.format(
                        java.util.Locale.ROOT,
                        "Pollution at %d %d %d: soot=%.3f poison=%.3f heavyMetal=%.3f fallout=%.3f",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        values.get(HbmPollutionType.SOOT),
                        values.get(HbmPollutionType.POISON),
                        values.get(HbmPollutionType.HEAVYMETAL),
                        values.get(HbmPollutionType.FALLOUT)
                )),
                false
        );
        return 1;
    }

    private static int setPollution(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        HbmPollutionType type = pollutionType(context);
        float value = FloatArgumentType.getFloat(context, "value");
        HbmPollutionData.get(context.getSource().getLevel()).set(pos, type, value);
        context.getSource().sendSuccess(
                () -> Component.literal("Set " + type.name().toLowerCase(java.util.Locale.ROOT) + " pollution to " + format(value)),
                true
        );
        return 1;
    }

    private static int addPollution(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        HbmPollutionType type = pollutionType(context);
        float value = FloatArgumentType.getFloat(context, "value");
        HbmPollutionData data = HbmPollutionData.get(context.getSource().getLevel());
        data.increment(pos, type, value, 1.0D);
        context.getSource().sendSuccess(
                () -> Component.literal("Added " + format(value) + " " + type.name().toLowerCase(java.util.Locale.ROOT) + " pollution"),
                true
        );
        return 1;
    }

    private static int clearPollution(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        HbmPollutionData.get(context.getSource().getLevel()).clear(pos);
        context.getSource().sendSuccess(
                () -> Component.literal("Cleared pollution at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()),
                true
        );
        return 1;
    }

    private static HbmPollutionType pollutionType(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, "type");
        return HbmPollutionType.valueOf(name.toUpperCase(java.util.Locale.ROOT));
    }

    private static int getRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        LivingEntity target = getLiving(context);
        HbmLivingRadiation data = HbmLivingRadiation.get(target);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.reinhardtshbm.radiation.get",
                        target.getName(),
                        format(data.getRadiation()),
                        format(data.getEnvironmentRadiation()),
                        format(data.getDigamma())
                ),
                false
        );
        return 1;
    }

    private static int getChunkRadiationHere(CommandContext<CommandSourceStack> context) {
        BlockPos pos = BlockPos.containing(context.getSource().getPosition());
        return sendChunkRadiation(context, pos);
    }

    private static int getChunkRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return sendChunkRadiation(context, BlockPosArgument.getLoadedBlockPos(context, "pos"));
    }

    private static int sendChunkRadiation(CommandContext<CommandSourceStack> context, BlockPos pos) {
        double value = ChunkRadiationData.get(context.getSource().getLevel()).getRadiation(pos);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.reinhardtshbm.radiation.chunk.get",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        format(value)
                ),
                false
        );
        return 1;
    }

    private static int setChunkRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        float value = FloatArgumentType.getFloat(context, "value");
        ChunkRadiationData.get(context.getSource().getLevel()).setRadiation(pos, value);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.reinhardtshbm.radiation.chunk.set",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        format(value)
                ),
                true
        );
        return 1;
    }

    private static int addChunkRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        float value = FloatArgumentType.getFloat(context, "value");
        ChunkRadiationData data = ChunkRadiationData.get(context.getSource().getLevel());
        data.incrementRadiation(pos, value);
        double current = data.getRadiation(pos);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.reinhardtshbm.radiation.chunk.add",
                        format(value),
                        pos.getX(),
                        pos.getY(),
                        pos.getZ(),
                        format(current)
                ),
                true
        );
        return 1;
    }

    private static int clearChunkRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(context, "pos");
        ChunkRadiationData.get(context.getSource().getLevel()).clearRadiation(pos);
        context.getSource().sendSuccess(
                () -> Component.translatable(
                        "commands.reinhardtshbm.radiation.chunk.clear",
                        pos.getX(),
                        pos.getY(),
                        pos.getZ()
                ),
                true
        );
        return 1;
    }

    private static int setRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        LivingEntity target = getLiving(context);
        float value = FloatArgumentType.getFloat(context, "value");
        HbmLivingRadiation data = HbmLivingRadiation.get(target);
        data.setRadiation(value);
        HbmLivingRadiation.set(target, data);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.reinhardtshbm.radiation.set", target.getName(), format(value)),
                true
        );
        return 1;
    }

    private static int addRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        LivingEntity target = getLiving(context);
        float value = FloatArgumentType.getFloat(context, "value");
        HbmLivingRadiation data = HbmLivingRadiation.get(target);
        data.addRadiation(value);
        HbmLivingRadiation.set(target, data);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.reinhardtshbm.radiation.add", target.getName(), format(value), format(data.getRadiation())),
                true
        );
        return 1;
    }

    private static int clearRadiation(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        LivingEntity target = getLiving(context);
        HbmLivingRadiation data = HbmLivingRadiation.get(target);
        data.setRadiation(0.0F);
        data.setEnvironmentRadiation(0.0F);
        data.setDigamma(0.0F);
        HbmLivingRadiation.set(target, data);
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.reinhardtshbm.radiation.clear", target.getName()),
                true
        );
        return 1;
    }

    private static LivingEntity getLiving(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        Entity entity = EntityArgument.getEntity(context, "target");
        if (entity instanceof LivingEntity living) {
            return living;
        }
        throw ERROR_NOT_LIVING.create();
    }

    private static String format(float value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }

    private static String format(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value);
    }
}
