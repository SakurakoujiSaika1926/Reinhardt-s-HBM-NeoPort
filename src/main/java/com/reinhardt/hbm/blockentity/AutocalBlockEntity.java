package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.autocal.AutocalScript;
import com.reinhardt.hbm.menu.AutocalMenu;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Stateful 1.7.10 TileEntityRadioAUTOCAL port, including its 100-op tick brake. */
public final class AutocalBlockEntity extends BlockEntity implements MenuProvider {
    private static final int HISTORY_LINES = 6;
    private boolean on;
    private boolean ignoreErrors;
    private boolean autoReboot;
    private String[] script = new String[0];
    private final String[] history = new String[HISTORY_LINES];
    private AutocalScript.Context context;

    public AutocalBlockEntity(BlockPos pos, BlockState state) {
        super(HbmBlockEntities.AUTOCAL.get(), pos, state);
        java.util.Arrays.fill(history, "");
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AutocalBlockEntity autocal) {
        if (!level.isClientSide) autocal.tickServer(level);
    }

    private void tickServer(Level level) {
        if (level.getGameTime() % 60L == 0L) setChanged();
        if (context == null) context = new AutocalScript.Context(level);
        context.level = level;
        if (!on && autoReboot) on = true;
        if (on) {
            int emergencyBrake = 100;
            for (int operation = 0; operation < context.clockSpeed && emergencyBrake-- > 0; operation++) {
                if (context.current == script.length) {
                    stop("Program has terminated");
                    break;
                }
                if (context.current < 0 || context.current >= script.length) {
                    stop("Program index is out of bounds");
                    break;
                }
                try {
                    int index = context.current++;
                    String line = script[index];
                    AutocalScript.Result result = AutocalScript.eval(context, line);
                    if (result != AutocalScript.Result.SKIP) pushMessage(index + ": " + line);
                    history[0] = "Buffer: " + context.buffer;
                    if (result == AutocalScript.Result.END_TICK) break;
                    if (result == AutocalScript.Result.SHUTDOWN) stop("Program requested shutdown");
                    if (!ignoreErrors) {
                        if (result == AutocalScript.Result.UNRECOGNIZED_COMMAND) stop("Unrecognized command");
                        if (result == AutocalScript.Result.PARAMETER_ERROR) stop("Parameter error");
                        if (result == AutocalScript.Result.UNDEFINED) stop("Undefined behavior");
                    }
                    if (result == AutocalScript.Result.SKIP) operation--;
                } catch (Exception ignored) {
                    stop("Evaluation unsuccessful");
                }
            }
        }
        sync();
    }

    public boolean isOn() {
        return on;
    }

    public boolean ignoresErrors() {
        return ignoreErrors;
    }

    public boolean autoReboots() {
        return autoReboot;
    }

    public String[] history() {
        return history.clone();
    }

    public void toggleOn() {
        if (on) stop("User requested shutdown");
        else on = true;
        sync();
    }

    public void toggleIgnoreErrors() {
        ignoreErrors = !ignoreErrors;
        sync();
    }

    public void toggleAutoReboot() {
        autoReboot = !autoReboot;
        sync();
    }

    public void setScript(String payload) {
        ensureContext();
        context.jumpPoints.clear();
        script = (payload == null ? "" : payload).split("\\r?\\n", -1);
        for (int index = 0; index < script.length; index++) {
            script[index] = script[index].trim();
            AutocalScript.generateJumpPoint(context, script[index], index);
        }
        if (on) stop("Script has changed");
        sync();
    }

    public void stop(String reason) {
        on = false;
        ensureContext();
        context.turnOff();
        pushMessage(reason);
    }

    private void pushMessage(String message) {
        for (int index = 2; index < history.length; index++) history[index - 1] = history[index];
        history[history.length - 1] = message;
    }

    private void ensureContext() {
        if (context == null) context = new AutocalScript.Context(level);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.reinhardtshbm.radio_autocal");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new AutocalMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ensureContext();
        tag.putBoolean("isOn", on);
        tag.putBoolean("ignoreError", ignoreErrors);
        tag.putBoolean("autoReboot", autoReboot);
        tag.putInt("current", context.current);
        tag.putInt("clockSpeed", context.clockSpeed);
        tag.putString("buffer", context.buffer);
        tag.put("variables", context.variables.copy());
        ListTag lines = new ListTag();
        for (String line : script) lines.add(StringTag.valueOf(line));
        tag.put("script", lines);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        on = tag.getBoolean("isOn");
        ignoreErrors = tag.getBoolean("ignoreError");
        autoReboot = tag.getBoolean("autoReboot");
        context = new AutocalScript.Context(level);
        context.current = tag.getInt("current");
        context.clockSpeed = Math.max(1, tag.getInt("clockSpeed"));
        context.buffer = tag.getString("buffer");
        context.variables = tag.contains("variables") ? tag.getCompound("variables") : new CompoundTag();
        ListTag lines = tag.getList("script", net.minecraft.nbt.Tag.TAG_STRING);
        script = new String[lines.size()];
        context.jumpPoints.clear();
        for (int index = 0; index < lines.size(); index++) {
            script[index] = lines.getString(index);
            AutocalScript.generateJumpPoint(context, script[index], index);
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        saveAdditional(tag, registries);
        return tag;
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void sync() {
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }
}
