package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.blockentity.WandJigsawBlockEntity;
import com.reinhardt.hbm.blockentity.WandStructureBlockEntity;
import com.reinhardt.hbm.blockentity.WandTandemBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record WandConfigPayload(BlockPos pos, CompoundTag data, int action) implements CustomPacketPayload {
    public static final int ACTION_NONE = 0;
    public static final int ACTION_SAVE = 1;
    public static final int ACTION_LOAD = 2;

    public static final Type<WandConfigPayload> TYPE = new Type<>(ReinhardtsHBM.id("wand_config"));
    public static final StreamCodec<RegistryFriendlyByteBuf, WandConfigPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public WandConfigPayload decode(RegistryFriendlyByteBuf buffer) {
            BlockPos pos = buffer.readBlockPos();
            Tag tag = buffer.readNbt(NbtAccounter.unlimitedHeap());
            CompoundTag data = tag instanceof CompoundTag compound ? compound : new CompoundTag();
            return new WandConfigPayload(pos, data, buffer.readVarInt());
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, WandConfigPayload payload) {
            buffer.writeBlockPos(payload.pos());
            buffer.writeNbt(payload.data());
            buffer.writeVarInt(payload.action());
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(WandConfigPayload payload, IPayloadContext context) {
        if (!(context.player() instanceof ServerPlayer player)) {
            return;
        }
        BlockEntity blockEntity = player.level().getBlockEntity(payload.pos());
        if (blockEntity instanceof WandStructureBlockEntity structure) {
            structure.applyConfig(payload.data());
            if (payload.action() == ACTION_SAVE) {
                structure.saveStructure(player);
            } else if (payload.action() == ACTION_LOAD) {
                structure.loadStructure(player);
            }
        } else if (blockEntity instanceof WandTandemBlockEntity tandem) {
            tandem.applyConfig(payload.data());
        } else if (blockEntity instanceof WandJigsawBlockEntity jigsaw) {
            jigsaw.applyConfig(payload.data());
        }
    }
}
