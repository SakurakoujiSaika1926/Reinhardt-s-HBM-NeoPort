package com.reinhardt.hbm.autocal;

import com.reinhardt.hbm.config.HbmConfig;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Exact MS-ES1 command surface from 1.7.10 ParseMSES1. */
public final class AutocalScript {
    private AutocalScript() {
    }

    public enum Result { OK, UNRECOGNIZED_COMMAND, PARAMETER_ERROR, END_TICK, SHUTDOWN, SKIP, UNDEFINED }

    public static final class Context {
        public Level level;
        public CompoundTag variables = new CompoundTag();
        public final Map<String, Integer> jumpPoints = new HashMap<>();
        public String buffer = "";
        public int clockSpeed = 1;
        public int current;

        public Context(Level level) {
            this.level = level;
        }

        public void turnOff() {
            clockSpeed = 1;
            current = 0;
            buffer = "";
            if (!variables.isEmpty()) variables = new CompoundTag();
        }
    }

    public static Result eval(Context context, String line) {
        String lower = line.toLowerCase(Locale.US);
        if (line.isEmpty() || lower.startsWith("dest ") || lower.startsWith("# ")) return Result.SKIP;
        if (lower.equals("nop")) return Result.OK;
        if (lower.startsWith("clockspeed ")) {
            if (line.length() <= 11) return Result.PARAMETER_ERROR;
            try {
                int speed = Integer.parseInt(line.substring(11));
                if (speed < 1 || speed > HbmConfig.AUTOCAL_MAX_CLOCK.get()) return Result.PARAMETER_ERROR;
                context.clockSpeed = speed;
                return Result.SKIP;
            } catch (Throwable ignored) {
                return Result.PARAMETER_ERROR;
            }
        }
        if (lower.startsWith("jmp ")) return jump(context, line, 4, true);
        if (lower.startsWith("jmpif ")) return context.buffer.equals("true") ? jump(context, line, 6, true) : Result.OK;
        if (lower.startsWith("jmpnot ")) return context.buffer.equals("true") ? Result.OK : jump(context, line, 7, true);
        if (lower.equals("endtick")) return Result.END_TICK;
        if (lower.equals("shutdown")) return Result.SHUTDOWN;
        if (lower.startsWith("load ")) {
            if (line.length() <= 5) return Result.PARAMETER_ERROR;
            context.buffer = context.variables.getString(line.substring(5));
            return Result.OK;
        }
        if (lower.startsWith("save ")) {
            if (line.length() <= 5 || context.buffer.isEmpty()) return Result.PARAMETER_ERROR;
            context.variables.putString(line.substring(5), context.buffer);
            return Result.OK;
        }
        if (lower.startsWith("buffer ")) {
            if (line.length() <= 7) return Result.PARAMETER_ERROR;
            context.buffer = line.substring(7);
            return Result.OK;
        }
        if (lower.startsWith("eval ")) return calculate(context, line.substring(5), false);
        if (lower.startsWith("evalr ")) return calculate(context, line.substring(6), true);
        if (lower.equals("evalr")) return context.buffer.isEmpty() ? Result.PARAMETER_ERROR : calculate(context, context.buffer, true);
        if (lower.equals("rounddown") || lower.equals("floor")) return round(context, 0);
        if (lower.equals("roundup") || lower.equals("ceil")) return round(context, 1);
        if (lower.equals("round") || lower.equals("nearest")) return round(context, 2);
        if (lower.startsWith("concat ")) {
            if (line.length() <= 7 || context.buffer.isEmpty()) return Result.PARAMETER_ERROR;
            context.buffer = substitute(context, line.substring(7), false);
            return Result.OK;
        }
        if (lower.startsWith("eq ")) {
            if (line.length() <= 3 || context.buffer.isEmpty()) return Result.PARAMETER_ERROR;
            context.buffer = context.buffer.equals(substitute(context, line.substring(3), false)) ? "true" : "false";
            return Result.OK;
        }
        if (lower.startsWith("gtb ")) return compare(context, line.substring(3), 0);
        if (lower.startsWith("ltb ")) return compare(context, line.substring(3), 1);
        if (lower.startsWith("geb ")) return compare(context, line.substring(3), 2);
        if (lower.startsWith("leb ")) return compare(context, line.substring(3), 3);
        if (lower.startsWith("send ")) {
            if (line.length() <= 5 || context.buffer.isEmpty()) return Result.PARAMETER_ERROR;
            HbmRttySystem.broadcast(context.level, substitute(context, line.substring(5), false), context.buffer);
            return Result.OK;
        }
        if (lower.startsWith("listen ")) {
            if (line.length() <= 7) return Result.PARAMETER_ERROR;
            HbmRttySystem.Channel channel = HbmRttySystem.listen(context.level, substitute(context, line.substring(7), false));
            if (channel != null) context.buffer = channel.signal();
            return Result.OK;
        }
        return Result.UNRECOGNIZED_COMMAND;
    }

    public static void generateJumpPoint(Context context, String line, int index) {
        if (line.startsWith("dest ") && line.length() > 5) context.jumpPoints.put(line.substring(5), index);
    }

    public static String substitute(Context context, String statement, boolean forceNumber) {
        if (!statement.contains("$")) return statement;
        StringBuilder joined = new StringBuilder();
        StringBuilder variable = new StringBuilder();
        boolean reading = false;
        for (char character : statement.toCharArray()) {
            if (character == '$') {
                if (!reading) {
                    reading = true;
                } else if (variable.toString().equals("buffer")) {
                    joined.append(context.buffer);
                    variable.setLength(0);
                    reading = false;
                } else {
                    String value = context.variables.getString(variable.toString());
                    joined.append(forceNumber && value.isEmpty() ? "0" : value);
                    variable.setLength(0);
                    reading = false;
                }
            } else if (reading) {
                variable.append(character);
            } else {
                joined.append(character);
            }
        }
        return joined.toString();
    }

    private static Result jump(Context context, String line, int start, boolean substitute) {
        if (line.length() <= start) return Result.PARAMETER_ERROR;
        String name = substitute ? substitute(context, line.substring(start), false) : line.substring(start);
        Integer target = context.jumpPoints.get(name);
        if (target == null) return Result.PARAMETER_ERROR;
        context.current = target;
        return Result.OK;
    }

    private static Result calculate(Context context, String expression, boolean rounded) {
        if (expression.isEmpty()) return Result.PARAMETER_ERROR;
        try {
            double result = AutocalMath.evaluateExpression(substitute(context, expression, true));
            context.buffer = rounded ? Integer.toString((int) Math.round(result)) : Double.toString(result);
            return Result.OK;
        } catch (Throwable ignored) {
            return Result.PARAMETER_ERROR;
        }
    }

    private static Result round(Context context, int mode) {
        if (context.buffer.isEmpty()) return Result.PARAMETER_ERROR;
        try {
            double value = Double.parseDouble(context.buffer);
            context.buffer = Integer.toString((int) switch (mode) {
                case 0 -> Math.floor(value);
                case 1 -> Math.ceil(value);
                default -> Math.round(value);
            });
            return Result.OK;
        } catch (Throwable ignored) {
            return Result.PARAMETER_ERROR;
        }
    }

    private static Result compare(Context context, String value, int mode) {
        if (value.isEmpty() || context.buffer.isEmpty()) return Result.PARAMETER_ERROR;
        try {
            double buffer = Double.parseDouble(context.buffer);
            double comparison = Double.parseDouble(value);
            boolean result = switch (mode) {
                case 0 -> comparison > buffer;
                case 1 -> comparison < buffer;
                case 2 -> comparison >= buffer;
                default -> comparison <= buffer;
            };
            context.buffer = result ? "true" : "false";
            return Result.OK;
        } catch (Throwable ignored) {
            return Result.PARAMETER_ERROR;
        }
    }
}
