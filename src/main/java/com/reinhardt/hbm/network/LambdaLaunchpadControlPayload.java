package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LambdaLaunchpadBlockEntity;
import com.reinhardt.hbm.menu.LambdaLaunchpadMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record LambdaLaunchpadControlPayload(BlockPos pos, int action) implements CustomPacketPayload {
    public static final int MANUAL = 0;
    public static final int AUTO = 1;
    public static final int LAUNCH = 2;
    public static final Type<LambdaLaunchpadControlPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("lambda_launchpad_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, LambdaLaunchpadControlPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public LambdaLaunchpadControlPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new LambdaLaunchpadControlPayload(buffer.readBlockPos(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer, LambdaLaunchpadControlPayload payload) {
                    buffer.writeBlockPos(payload.pos());
                    buffer.writeVarInt(payload.action());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LambdaLaunchpadControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof LambdaLaunchpadMenu menu)
                || !menu.blockPos().equals(payload.pos())
                || !menu.stillValid(player)) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (!(blockEntity instanceof LambdaLaunchpadBlockEntity launchpad)) {
            return;
        }
        switch (payload.action()) {
            case MANUAL -> launchpad.setAutolaunch(false);
            case AUTO -> launchpad.setAutolaunch(true);
            case LAUNCH -> launchpad.startCountdown();
            default -> {
                return;
            }
        }
        player.containerMenu.broadcastChanges();
    }
}
