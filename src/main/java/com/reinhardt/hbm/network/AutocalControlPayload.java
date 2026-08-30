package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.AutocalBlockEntity;
import com.reinhardt.hbm.menu.AutocalMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Controls carried by the old NBTControlPacket for TileEntityRadioAUTOCAL. */
public record AutocalControlPayload(BlockPos pos, int action, String payload) implements CustomPacketPayload {
    public static final int TOGGLE_ON = 0;
    public static final int TOGGLE_IGNORE = 1;
    public static final int TOGGLE_REBOOT = 2;
    public static final int UPLOAD = 3;
    private static final int MAX_SCRIPT_LENGTH = 65_535;
    public static final Type<AutocalControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("autocal_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, AutocalControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public AutocalControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new AutocalControlPayload(buffer.readBlockPos(), buffer.readVarInt(), buffer.readUtf(MAX_SCRIPT_LENGTH));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, AutocalControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.action());
            buffer.writeUtf(payload.payload(), MAX_SCRIPT_LENGTH);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(AutocalControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof AutocalMenu menu)
                || !menu.blockPos().equals(payload.pos())) return;
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (!(entity instanceof AutocalBlockEntity autocal)) return;
        switch (payload.action()) {
            case TOGGLE_ON -> autocal.toggleOn();
            case TOGGLE_IGNORE -> autocal.toggleIgnoreErrors();
            case TOGGLE_REBOOT -> autocal.toggleAutoReboot();
            case UPLOAD -> autocal.setScript(payload.payload());
            default -> {
            }
        }
    }
}
