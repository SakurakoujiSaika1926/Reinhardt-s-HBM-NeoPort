package com.reinhardt.hbm.blockentity;

import com.reinhardt.hbm.block.RadioTorchBlock;
import com.reinhardt.hbm.item.LegacyRttyPagerItem;
import com.reinhardt.hbm.registry.HbmBlockEntities;
import com.reinhardt.hbm.rtty.HbmRttySystem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Server-side state and ticking logic for the 1.7.10 redstone-over-radio blocks. */
public final class RadioTorchBlockEntity extends BlockEntity {
    private static final int CHANNEL_LIMIT = 64;
    private final RadioTorchBlock.Kind kind;
    private String channel = "";
    private int lastState;
    private long lastUpdate = -1L;
    private boolean polling;
    private boolean customMap;
    private boolean descending;
    private final String[] mapping = new String[16];
    private final int[] conditions = new int[16];
    private final String[] channels = new String[8];
    private final String[] names = new String[8];
    private final String[] previous = new String[8];
    private final ItemStack[] filters = new ItemStack[3];
    private final int[] lastCount = new int[3];
    private String previousCommand = "";

    public RadioTorchBlockEntity(BlockPos pos, BlockState state, RadioTorchBlock.Kind kind) {
        super(HbmBlockEntities.RADIO_TORCH.get(), pos, state);
        this.kind = kind;
        for (int i = 0; i < mapping.length; i++) mapping[i] = "";
        for (int i = 0; i < channels.length; i++) {
            channels[i] = "";
            names[i] = "";
            previous[i] = "";
        }
        for (int i = 0; i < filters.length; i++) filters[i] = ItemStack.EMPTY;
    }

    public RadioTorchBlock.Kind kind() {
        return kind;
    }

    public int signal() {
        return Math.max(0, Math.min(15, lastState));
    }

    public String channel() {
        return channel;
    }

    public void setChannel(String channel) {
        String normalized = channel == null ? "" : channel.trim();
        this.channel = normalized.length() > CHANNEL_LIMIT ? normalized.substring(0, CHANNEL_LIMIT) : normalized;
        setChanged();
    }

    /** Empty-hand use toggles polling while sneaking and reports state otherwise. */
    public void interact(Player player) {
        if (level == null || level.isClientSide) return;
        ItemStack held = player.getMainHandItem();
        if (held.getItem() instanceof LegacyRttyPagerItem) {
            String pagerChannel = LegacyRttyPagerItem.channel(held);
            if (!pagerChannel.isEmpty()) {
                setChannel(pagerChannel);
                player.displayClientMessage(Component.translatable("message.reinhardtshbm.radio_torch.channel_set", pagerChannel), true);
                sync();
                return;
            }
        }
        if (player.isCrouching()) {
            polling = !polling;
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.radio_torch.polling", polling), true);
        } else {
            player.displayClientMessage(Component.translatable("message.reinhardtshbm.radio_torch.status", channel, signal()), true);
        }
        setChanged();
        sync();
    }

    public static void tick(Level level, BlockPos pos, BlockState state, RadioTorchBlockEntity entity) {
        if (level.isClientSide) return;
        switch (entity.kind) {
            case SENDER -> entity.tickSender(level, pos, state);
            case RECEIVER -> entity.tickReceiver(level, pos, state, false);
            case LOGIC -> entity.tickReceiver(level, pos, state, true);
            case READER -> entity.tickReader(level, pos, state);
            case COUNTER -> entity.tickCounter(level, pos, state);
            case CONTROLLER -> entity.tickController(level, pos, state);
        }
    }

    private void tickSender(Level level, BlockPos pos, BlockState state) {
        BlockPos support = support(pos, state);
        BlockState inputState = level.getBlockState(support);
        int input = Math.max(0, Math.min(15, level.getBestNeighborSignal(support)));
        if (inputState.hasAnalogOutputSignal()) {
            input = Math.max(0, Math.min(15, inputState.getAnalogOutputSignal(level, support)));
        }
        boolean shouldSend = polling || input != lastState;
        if (input != lastState) {
            lastState = input;
            updatePoweredState(level, pos, state, input > 0);
        }
        if (shouldSend && !channel.isEmpty()) {
            String value = customMap ? mapping[input] : Integer.toString(input);
            if (value != null && !value.isEmpty()) HbmRttySystem.broadcast(level, channel, value);
        }
        setChanged();
    }

    private void tickReceiver(Level level, BlockPos pos, BlockState state, boolean logic) {
        if (channel.isEmpty()) return;
        HbmRttySystem.Channel received = HbmRttySystem.listen(level, channel);
        if (received == null || (!polling && received.timestamp() <= lastUpdate)) return;
        lastUpdate = received.timestamp();
        String message = received.signal();
        if ("selfdestruct".equalsIgnoreCase(message)) {
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5.0F,
                    Level.ExplosionInteraction.BLOCK);
            return;
        }
        int next;
        if (polling && received.timestamp() < level.getGameTime() - 2L) {
            next = 0;
        } else if (logic) {
            next = evaluateLogic(message);
        } else if (customMap) {
            next = 0;
            for (int i = 15; i >= 0; i--) {
                if (message.equals(mapping[i])) {
                    next = i;
                    break;
                }
            }
        } else {
            next = parseInt(message, 0);
        }
        next = Math.max(0, Math.min(15, next));
        if (next != lastState) {
            lastState = next;
            updatePoweredState(level, pos, state, next > 0);
            level.updateNeighborsAt(pos, state.getBlock());
            setChanged();
        }
    }

    private int evaluateLogic(String message) {
        if (polling && lastUpdate < (level == null ? 0L : level.getGameTime()) - 2L) message = "0";
        if (descending) {
            for (int i = 15; i >= 0; i--) if (!mapping[i].isEmpty() && compare(message, i)) return i;
        } else {
            for (int i = 0; i < 16; i++) if (!mapping[i].isEmpty() && compare(message, i)) return i;
        }
        return 0;
    }

    private boolean compare(String message, int index) {
        int op = Math.max(0, Math.min(9, conditions[index]));
        if (op <= 5) {
            try {
                long signal = Long.parseLong(message);
                long mapped = Long.parseLong(mapping[index]);
                return switch (op) {
                    case 0 -> signal < mapped;
                    case 1 -> signal <= mapped;
                    case 2 -> signal >= mapped;
                    case 3 -> signal > mapped;
                    case 4 -> signal == mapped;
                    default -> signal != mapped;
                };
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return switch (op) {
            case 6 -> message.equals(mapping[index]);
            case 7 -> !message.equals(mapping[index]);
            case 8 -> message.contains(mapping[index]);
            default -> !message.contains(mapping[index]);
        };
    }

    private void tickReader(Level level, BlockPos pos, BlockState state) {
        BlockState supportState = level.getBlockState(support(pos, state));
        int value = Math.max(0, Math.min(15, supportState.hasAnalogOutputSignal()
                ? supportState.getAnalogOutputSignal(level, support(pos, state))
                : level.getBestNeighborSignal(support(pos, state))));
        for (int i = 0; i < channels.length; i++) {
            if (channels[i].isEmpty()) continue;
            String valueText = Integer.toString(value);
            if (polling || !valueText.equals(previous[i])) {
                HbmRttySystem.broadcast(level, channels[i], valueText);
                previous[i] = valueText;
            }
        }
        setChanged();
    }

    private void tickCounter(Level level, BlockPos pos, BlockState state) {
        BlockEntity adjacent = level.getBlockEntity(support(pos, state));
        if (!(adjacent instanceof Container container)) return;
        for (int i = 0; i < filters.length; i++) {
            if (channels[i].isEmpty() || filters[i].isEmpty()) continue;
            int count = 0;
            for (int slot = 0; slot < container.getContainerSize(); slot++) {
                ItemStack stack = container.getItem(slot);
                if (ItemStack.isSameItemSameComponents(filters[i], stack)) count += stack.getCount();
            }
            if (polling || count != lastCount[i]) HbmRttySystem.broadcast(level, channels[i], count);
            lastCount[i] = count;
        }
        setChanged();
    }

    private void tickController(Level level, BlockPos pos, BlockState state) {
        if (channel.isEmpty()) return;
        HbmRttySystem.Channel received = HbmRttySystem.listen(level, channel);
        if (received == null || (!polling && received.timestamp() <= lastUpdate)) return;
        lastUpdate = received.timestamp();
        String command = received.signal();
        if ("selfdestruct".equalsIgnoreCase(command)) {
            level.explode(null, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, 5.0F,
                    Level.ExplosionInteraction.BLOCK);
            return;
        }
        // The old IRORInteractive API is optional. Preserve command edge detection
        // here so adjacent modern machines can consume the command via redstone.
        if (!command.equals(previousCommand)) {
            previousCommand = command;
            boolean powered = !command.isEmpty() && !"off".equalsIgnoreCase(command) && !"0".equals(command);
            updatePoweredState(level, pos, state, powered);
            level.updateNeighborsAt(pos, state.getBlock());
            setChanged();
        }
    }

    private BlockPos support(BlockPos pos, BlockState state) {
        return pos.relative(state.getValue(RadioTorchBlock.FACING).getOpposite());
    }

    private void updatePoweredState(Level level, BlockPos pos, BlockState state, boolean powered) {
        if (state.hasProperty(RadioTorchBlock.POWERED) && state.getValue(RadioTorchBlock.POWERED) != powered) {
            level.setBlock(pos, state.setValue(RadioTorchBlock.POWERED, powered), Block.UPDATE_ALL);
        }
    }

    private void sync() {
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("Channel", channel);
        tag.putInt("LastState", lastState);
        tag.putLong("LastUpdate", lastUpdate);
        tag.putBoolean("Polling", polling);
        tag.putBoolean("CustomMap", customMap);
        tag.putBoolean("Descending", descending);
        tag.putString("PreviousCommand", previousCommand);
        for (int i = 0; i < mapping.length; i++) {
            if (!mapping[i].isEmpty()) tag.putString("Map" + i, mapping[i]);
            if (conditions[i] != 0) tag.putInt("Condition" + i, conditions[i]);
        }
        for (int i = 0; i < channels.length; i++) {
            tag.putString("Channel" + i, channels[i]);
            tag.putString("Name" + i, names[i]);
            tag.putString("Previous" + i, previous[i]);
        }
        for (int i = 0; i < filters.length; i++) if (!filters[i].isEmpty()) {
            CompoundTag filter = new CompoundTag();
            filters[i].save(registries, filter);
            tag.put("Filter" + i, filter);
            tag.putInt("Count" + i, lastCount[i]);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        channel = tag.getString("Channel");
        lastState = Math.max(0, Math.min(15, tag.getInt("LastState")));
        lastUpdate = tag.getLong("LastUpdate");
        polling = tag.getBoolean("Polling");
        customMap = tag.getBoolean("CustomMap");
        descending = tag.getBoolean("Descending");
        previousCommand = tag.getString("PreviousCommand");
        for (int i = 0; i < mapping.length; i++) {
            mapping[i] = tag.getString("Map" + i);
            conditions[i] = tag.getInt("Condition" + i);
        }
        for (int i = 0; i < channels.length; i++) {
            channels[i] = tag.getString("Channel" + i);
            names[i] = tag.getString("Name" + i);
            previous[i] = tag.getString("Previous" + i);
        }
        for (int i = 0; i < filters.length; i++) {
            if (tag.contains("Filter" + i)) filters[i] = ItemStack.parse(registries, tag.getCompound("Filter" + i)).orElse(ItemStack.EMPTY);
            lastCount[i] = tag.getInt("Count" + i);
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
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }
}
