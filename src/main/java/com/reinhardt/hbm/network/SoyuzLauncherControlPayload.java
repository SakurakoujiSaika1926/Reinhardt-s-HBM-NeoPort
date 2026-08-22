package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.SoyuzLauncherBlockEntity;
import com.reinhardt.hbm.menu.SoyuzLauncherMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SoyuzLauncherControlPayload(BlockPos pos, int button, int mode) implements CustomPacketPayload {
    public static final int BUTTON_MODE = 0;
    public static final int BUTTON_START = 1;
    public static final Type<SoyuzLauncherControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("soyuz_launcher_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoyuzLauncherControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public SoyuzLauncherControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new SoyuzLauncherControlPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, SoyuzLauncherControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.button());
            buffer.writeVarInt(payload.mode());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoyuzLauncherControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof SoyuzLauncherMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof SoyuzLauncherBlockEntity launcher)) {
            return;
        }
        if (payload.button() == BUTTON_MODE) {
            launcher.setMode(payload.mode());
        } else if (payload.button() == BUTTON_START) {
            launcher.startCountdown();
        }
        player.containerMenu.broadcastChanges();
    }
}
