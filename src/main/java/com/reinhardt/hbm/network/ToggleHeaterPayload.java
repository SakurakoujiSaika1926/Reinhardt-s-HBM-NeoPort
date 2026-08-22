package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.HeaterBlockEntity;
import com.reinhardt.hbm.menu.HeaterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ToggleHeaterPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<ToggleHeaterPayload> TYPE = new Type<>(ReinhardtsHBM.id("toggle_heater"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ToggleHeaterPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ToggleHeaterPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ToggleHeaterPayload(buffer.readBlockPos());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ToggleHeaterPayload payload) {
            buffer.writeBlockPos(payload.pos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ToggleHeaterPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof HeaterMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }

        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof HeaterBlockEntity heater && heater.kind() == HeaterBlockEntity.Kind.OILBURNER) {
            heater.toggleEnabled();
            player.containerMenu.broadcastChanges();
        }
    }
}
