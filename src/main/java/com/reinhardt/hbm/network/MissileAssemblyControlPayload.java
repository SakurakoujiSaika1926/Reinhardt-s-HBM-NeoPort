package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.LegacyMachineBlockEntity;
import com.reinhardt.hbm.menu.MissileAssemblyMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server-authoritative construct button from GUIMachineMissileAssembly. */
public record MissileAssemblyControlPayload(BlockPos pos) implements CustomPacketPayload {
    public static final Type<MissileAssemblyControlPayload> TYPE = new Type<>(ReinhardtsHBM.id("missile_assembly_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MissileAssemblyControlPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override public MissileAssemblyControlPayload decode(RegistryFriendlyByteBuf buffer) { return new MissileAssemblyControlPayload(buffer.readBlockPos()); }
        @Override public void encode(RegistryFriendlyByteBuf buffer, MissileAssemblyControlPayload payload) { buffer.writeBlockPos(payload.pos()); }
    };

    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }

    public static void handle(MissileAssemblyControlPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)
                || !(player.containerMenu instanceof MissileAssemblyMenu menu)
                || !menu.blockPos().equals(payload.pos())) return;
        BlockEntity entity = player.level().getBlockEntity(payload.pos());
        if (entity instanceof LegacyMachineBlockEntity machine && machine.machineId().equals("machine_missile_assembly")) {
            machine.constructMissile();
            player.containerMenu.broadcastChanges();
        }
    }
}
