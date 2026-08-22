package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.PwrControllerBlockEntity;
import com.reinhardt.hbm.menu.PwrMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record PwrControlPayload(BlockPos pos, int rodTarget) implements CustomPacketPayload {
    public static final Type<PwrControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("pwr_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PwrControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PwrControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new PwrControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PwrControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.rodTarget());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PwrControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof PwrMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof PwrControllerBlockEntity controller) {
            controller.setRodTarget(payload.rodTarget());
            player.containerMenu.broadcastChanges();
        }
    }
}
