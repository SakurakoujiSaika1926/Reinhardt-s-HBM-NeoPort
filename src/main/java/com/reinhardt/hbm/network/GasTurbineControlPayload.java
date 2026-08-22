package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.GasTurbineBlockEntity;
import com.reinhardt.hbm.menu.GasTurbineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record GasTurbineControlPayload(BlockPos pos, int slider, boolean setSlider, boolean autoMode, boolean setAuto, int state, boolean setState) implements CustomPacketPayload {
    public static final Type<GasTurbineControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("gas_turbine_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, GasTurbineControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public GasTurbineControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new GasTurbineControlPayload(
                    buffer.readBlockPos(),
                    buffer.readVarInt(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readVarInt(),
                    buffer.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, GasTurbineControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.slider());
            buffer.writeBoolean(payload.setSlider());
            buffer.writeBoolean(payload.autoMode());
            buffer.writeBoolean(payload.setAuto());
            buffer.writeVarInt(payload.state());
            buffer.writeBoolean(payload.setState());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(GasTurbineControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof GasTurbineMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof GasTurbineBlockEntity turbine)) {
            return;
        }
        if (payload.setSlider()) {
            turbine.setSlider(payload.slider());
        }
        if (payload.setAuto()) {
            turbine.setAutoMode(payload.autoMode());
        }
        if (payload.setState()) {
            turbine.requestState(payload.state());
        }
        player.containerMenu.broadcastChanges();
    }
}
