package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.RbmkComponentBlock;
import com.reinhardt.hbm.blockentity.MachineDummyBlockEntity;
import com.reinhardt.hbm.blockentity.RbmkComponentBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record RbmkCraneControlPayload(BlockPos pos, boolean up, boolean down, boolean left, boolean right, boolean load) implements CustomPacketPayload {
    public static final Type<RbmkCraneControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("rbmk_crane_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RbmkCraneControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public RbmkCraneControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new RbmkCraneControlPayload(
                    buffer.readBlockPos(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean(),
                    buffer.readBoolean()
            );
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, RbmkCraneControlPayload payload) {
            buffer.writeBlockPos(payload.pos);
            buffer.writeBoolean(payload.up);
            buffer.writeBoolean(payload.down);
            buffer.writeBoolean(payload.left);
            buffer.writeBoolean(payload.right);
            buffer.writeBoolean(payload.load);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RbmkCraneControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos);
        if (blockEntity instanceof MachineDummyBlockEntity dummy) {
            blockEntity = player.level().getBlockEntity(dummy.getCorePos());
        }
        if (blockEntity instanceof RbmkComponentBlockEntity rbmk
                && rbmk.kind() == RbmkComponentBlock.Kind.CRANE_CONSOLE) {
            rbmk.applyCraneInput(player, payload.up, payload.down, payload.left, payload.right, payload.load);
        }
    }
}
