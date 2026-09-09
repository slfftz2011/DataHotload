package com.slfftz.datahotload.fabric.network;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/**
 * Fabric network payload that wraps the loader-agnostic
 * {@link DataHotloadPayload} for S2C (server-to-client) transmission.
 * <p>
 * Implements the 1.21.1 {@link CustomPayload} API with a registered
 * {@link CustomPayload.Id} and {@link PacketCodec} for serialization.
 * <p>
 * The wire format delegates to {@link DataHotloadPayload#encode()} /
 * {@link DataHotloadPayload#decode(byte[])} for cross-loader compatibility.
 */
public record DataHotloadPayloadS2C(DataHotloadPayload payload) implements CustomPayload {

    /**
     * Unique channel identifier for this payload type.
     * Matches {@link com.slfftz.datahotload.core.common.DataHotloadConstants#CHANNEL_ID}.
     */
    public static final CustomPayload.Id<DataHotloadPayloadS2C> ID =
            new CustomPayload.Id<>(Identifier.of("slfftz", "datahotload"));

    /**
     * Stream codec for encoding/decoding this payload on the network buffer.
     * Uses a VarInt length prefix followed by the raw encoded bytes from
     * {@link DataHotloadPayload#encode()}.
     */
    public static final PacketCodec<PacketByteBuf, DataHotloadPayloadS2C> CODEC = PacketCodec.of(
            (value, buf) -> {
                byte[] data = value.payload.encode();
                buf.writeVarInt(data.length);
                buf.writeBytes(data);
            },
            buf -> {
                int length = buf.readVarInt();
                byte[] data = new byte[length];
                buf.readBytes(data);
                return new DataHotloadPayloadS2C(DataHotloadPayload.decode(data));
            }
    );

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
