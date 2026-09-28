package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.SoyuzLaunchpadBlockEntity;
import com.reinhardt.hbm.menu.SoyuzLaunchpadMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record SoyuzLaunchpadControlPayload(BlockPos pos, int action)
        implements CustomPacketPayload {
    public static final int MODE_CARGO = 0;
    public static final int MODE_SATELLITE = 1;
    public static final int LAUNCH = 2;
    public static final Type<SoyuzLaunchpadControlPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("soyuz_launchpad_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SoyuzLaunchpadControlPayload> STREAM_CODEC =
            new StreamCodec<>() {
                @Override
                public SoyuzLaunchpadControlPayload decode(RegistryFriendlyByteBuf buffer) {
                    return new SoyuzLaunchpadControlPayload(buffer.readBlockPos(), buffer.readVarInt());
                }

                @Override
                public void encode(RegistryFriendlyByteBuf buffer,
                                   SoyuzLaunchpadControlPayload payload) {
                    buffer.writeBlockPos(payload.pos());
                    buffer.writeVarInt(payload.action());
                }
            };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SoyuzLaunchpadControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof SoyuzLaunchpadMenu menu)
                || !menu.blockPos().equals(payload.pos()) || !menu.stillValid(player)) {
            return;
        }
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (!(entity instanceof SoyuzLaunchpadBlockEntity launchpad)) {
            return;
        }
        switch (payload.action()) {
            case MODE_CARGO -> launchpad.setCargoMode(true);
            case MODE_SATELLITE -> launchpad.setCargoMode(false);
            case LAUNCH -> launchpad.startLaunch();
            default -> {
                return;
            }
        }
        player.containerMenu.broadcastChanges();
    }
}
