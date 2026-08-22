package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.BatteryReddBlockEntity;
import com.reinhardt.hbm.menu.BatteryReddMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record BatteryReddControlPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int ACTION_LOW = 0;
    public static final int ACTION_HIGH = 1;
    public static final int ACTION_PRIORITY = 2;
    public static final Type<BatteryReddControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("battery_redd_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, BatteryReddControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public BatteryReddControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new BatteryReddControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, BatteryReddControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(BatteryReddControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof BatteryReddMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof BatteryReddBlockEntity battery)) {
            return;
        }
        switch (payload.action()) {
            case ACTION_LOW -> battery.cycleLowMode();
            case ACTION_HIGH -> battery.cycleHighMode();
            case ACTION_PRIORITY -> battery.cyclePriority();
            default -> {
            }
        }
        menu.sendSync();
        player.containerMenu.broadcastChanges();
    }
}
