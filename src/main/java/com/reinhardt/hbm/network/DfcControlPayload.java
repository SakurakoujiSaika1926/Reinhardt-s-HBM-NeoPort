package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.DfcEmitterBlockEntity;
import com.reinhardt.hbm.blockentity.DfcStabilizerBlockEntity;
import com.reinhardt.hbm.menu.DfcEmitterMenu;
import com.reinhardt.hbm.menu.DfcStabilizerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record DfcControlPayload(BlockPos pos, int target, int value) implements CustomPacketPayload {
    public static final int TARGET_EMITTER_WATTS = 0;
    public static final int TARGET_EMITTER_TOGGLE = 1;
    public static final int TARGET_STABILIZER_WATTS = 2;

    public static final Type<DfcControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("dfc_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DfcControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public DfcControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new DfcControlPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, DfcControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.target());
            buffer.writeVarInt(payload.value());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(DfcControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        switch (payload.target()) {
            case TARGET_EMITTER_WATTS -> {
                if (player.containerMenu instanceof DfcEmitterMenu menu
                        && menu.blockPos().equals(payload.pos())
                        && blockEntity instanceof DfcEmitterBlockEntity emitter) {
                    emitter.setWatts(payload.value());
                    player.containerMenu.broadcastChanges();
                }
            }
            case TARGET_EMITTER_TOGGLE -> {
                if (player.containerMenu instanceof DfcEmitterMenu menu
                        && menu.blockPos().equals(payload.pos())
                        && blockEntity instanceof DfcEmitterBlockEntity emitter) {
                    emitter.toggle();
                    player.containerMenu.broadcastChanges();
                }
            }
            case TARGET_STABILIZER_WATTS -> {
                if (player.containerMenu instanceof DfcStabilizerMenu menu
                        && menu.blockPos().equals(payload.pos())
                        && blockEntity instanceof DfcStabilizerBlockEntity stabilizer) {
                    stabilizer.setWatts(payload.value());
                    player.containerMenu.broadcastChanges();
                }
            }
            default -> {
            }
        }
    }
}
