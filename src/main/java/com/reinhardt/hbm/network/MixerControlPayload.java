package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.MixerBlockEntity;
import com.reinhardt.hbm.menu.MixerMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MixerControlPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<MixerControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("mixer_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MixerControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public MixerControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new MixerControlPayload(buffer.readBlockPos());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, MixerControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(MixerControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof MixerMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof MixerBlockEntity mixer) {
            mixer.cycleRecipe();
            player.containerMenu.broadcastChanges();
        }
    }
}
