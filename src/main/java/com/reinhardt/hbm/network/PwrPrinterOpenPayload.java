package com.reinhardt.hbm.network;

import com.reinhardt.hbm.ReinhardtsHBM;
import com.reinhardt.hbm.block.PwrBlock;
import com.reinhardt.hbm.client.screen.PwrPrinterScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative copy of an assembled PWR used by the legacy slice
 * printer.  The old implementation synchronized every original PWR block to
 * the client before opening GUIScreenSlicePrinter; this sparse snapshot is the
 * 1.21 equivalent and avoids sending empty cells from the bounding box.
 */
public record PwrPrinterOpenPayload(
        int sizeX,
        int sizeY,
        int sizeZ,
        Direction inward,
        List<Part> parts
) implements CustomPacketPayload {
    private static final int MAX_PARTS = 4097; // 4096 assembled cells plus the controller.

    public static final Type<PwrPrinterOpenPayload> TYPE =
            new Type<>(ReinhardtsHBM.id("pwr_printer_open"));

    public static final StreamCodec<RegistryFriendlyByteBuf, PwrPrinterOpenPayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public PwrPrinterOpenPayload decode(RegistryFriendlyByteBuf buffer) {
            int sizeX = checkedSize(buffer.readVarInt(), "X");
            int sizeY = checkedSize(buffer.readVarInt(), "Y");
            int sizeZ = checkedSize(buffer.readVarInt(), "Z");
            Direction inward = Direction.from3DDataValue(buffer.readUnsignedByte());
            if (!inward.getAxis().isHorizontal()) {
                throw new IllegalArgumentException("PWR printer direction must be horizontal");
            }

            int count = buffer.readVarInt();
            if (count < 1 || count > MAX_PARTS) {
                throw new IllegalArgumentException("Invalid PWR printer part count: " + count);
            }
            List<Part> parts = new ArrayList<>(count);
            for (int index = 0; index < count; index++) {
                int x = buffer.readVarInt();
                int y = buffer.readVarInt();
                int z = buffer.readVarInt();
                int kindOrdinal = buffer.readUnsignedByte();
                if (x < 0 || x >= sizeX || y < 0 || y >= sizeY || z < 0 || z >= sizeZ) {
                    throw new IllegalArgumentException("PWR printer part lies outside its bounds");
                }
                PwrBlock.Kind[] kinds = PwrBlock.Kind.values();
                if (kindOrdinal >= kinds.length) {
                    throw new IllegalArgumentException("Invalid PWR printer part kind: " + kindOrdinal);
                }
                parts.add(new Part(x, y, z, kinds[kindOrdinal]));
            }
            return new PwrPrinterOpenPayload(sizeX, sizeY, sizeZ, inward, List.copyOf(parts));
        }

        @Override
        public void encode(RegistryFriendlyByteBuf buffer, PwrPrinterOpenPayload payload) {
            buffer.writeVarInt(payload.sizeX());
            buffer.writeVarInt(payload.sizeY());
            buffer.writeVarInt(payload.sizeZ());
            buffer.writeByte(payload.inward().get3DDataValue());
            buffer.writeVarInt(payload.parts().size());
            for (Part part : payload.parts()) {
                buffer.writeVarInt(part.x());
                buffer.writeVarInt(part.y());
                buffer.writeVarInt(part.z());
                buffer.writeByte(part.kind().ordinal());
            }
        }
    };

    public PwrPrinterOpenPayload {
        sizeX = checkedSize(sizeX, "X");
        sizeY = checkedSize(sizeY, "Y");
        sizeZ = checkedSize(sizeZ, "Z");
        if (inward == null || !inward.getAxis().isHorizontal()) {
            throw new IllegalArgumentException("PWR printer direction must be horizontal");
        }
        if (parts == null || parts.isEmpty() || parts.size() > MAX_PARTS) {
            throw new IllegalArgumentException("Invalid PWR printer part count");
        }
        for (Part part : parts) {
            if (part == null || part.kind() == null
                    || part.x() < 0 || part.x() >= sizeX
                    || part.y() < 0 || part.y() >= sizeY
                    || part.z() < 0 || part.z() >= sizeZ) {
                throw new IllegalArgumentException("PWR printer part lies outside its bounds");
            }
        }
        parts = List.copyOf(parts);
    }

    private static int checkedSize(int size, String axis) {
        if (size < 1 || size > MAX_PARTS) {
            throw new IllegalArgumentException("Invalid PWR printer " + axis + " size: " + size);
        }
        return size;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(PwrPrinterOpenPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> PwrPrinterScreen.open(payload));
    }

    public record Part(int x, int y, int z, PwrBlock.Kind kind) {
    }
}
