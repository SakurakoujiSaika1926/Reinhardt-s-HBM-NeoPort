package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.RadioRecBlockEntity;
import com.reinhardt.hbm.menu.RadioRecMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Saved channel (action 0) and toggle (action 1) mirror GUIRadioRec's two packet types. */
public record RadioRecControlPayload(BlockPos pos, int action, String channel) implements CustomPacketPayload {
    public static final Type<RadioRecControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("radio_rec_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RadioRecControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RadioRecControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RadioRecControlPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readUtf(10));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RadioRecControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
            buffer.writeUtf(payload.channel(), 10);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RadioRecControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof RadioRecMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof RadioRecBlockEntity radio) {
            if (payload.action() == 0) {
                radio.setChannel(payload.channel());
            } else if (payload.action() == 1) {
                radio.toggleOn();
            }
        }
    }
}
