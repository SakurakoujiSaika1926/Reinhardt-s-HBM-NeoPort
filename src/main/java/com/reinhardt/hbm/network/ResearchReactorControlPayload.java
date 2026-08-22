package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.ResearchReactorBlockEntity;
import com.reinhardt.hbm.menu.ResearchReactorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record ResearchReactorControlPayload(BlockPos pos, int percent) implements CustomPacketPayload {
    public static final Type<ResearchReactorControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("research_reactor_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ResearchReactorControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public ResearchReactorControlPayload decode(RegistryFriendlyByteBuf buffer) {
            return new ResearchReactorControlPayload(buffer.readBlockPos(), buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, ResearchReactorControlPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeVarInt(payload.percent());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ResearchReactorControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof ResearchReactorMenu menu)
                || !menu.blockPos().equals(payload.pos())) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof ResearchReactorBlockEntity reactor) {
            reactor.setTargetPercent(payload.percent());
            player.containerMenu.broadcastChanges();
        }
    }
}
