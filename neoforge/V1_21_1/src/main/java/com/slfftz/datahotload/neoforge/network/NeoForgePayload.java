package com.slfftz.datahotload.neoforge.network;

import com.slfftz.datahotload.core.common.DataHotloadConstants;
import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * NeoForge-wrapped {@link CustomPayload} that carries a core {@link DataHotloadPayload}.
 * <p>
 * Serialization delegates to the core payload's own binary format via
 * {@link DataHotloadPayload#encode()} and {@link DataHotloadPayload#decode(byte[])}.
 * The wire format is simply the raw byte array produced by the core encoder.
 * <p>
 * Channel ID: {@value DataHotloadConstants#CHANNEL_ID}
 */
public record NeoForgePayload(DataHotloadPayload inner) implements CustomPayload {

    /** Payload type identifier registered with NeoForge's networking system. */
    public static final Type<NeoForgePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    DataHotloadConstants.CHANNEL_NAMESPACE,
                    DataHotloadConstants.CHANNEL_PATH
            ),
            STREAM_CODEC
    );

    /**
     * Stream codec for encoding/decoding this payload on the network buffer.
     * <p>
     * Encoding: writes a varint length prefix followed by the raw bytes from
     * {@link DataHotloadPayload#encode()}.
     * Decoding: reads the varint length, then the bytes, and calls
     * {@link DataHotloadPayload#decode(byte[])}.
     */
    public static final StreamCodec<FriendlyByteBuf, NeoForgePayload> STREAM_CODEC =
            StreamCodec.of(
                    NeoForgePayload::encode,
                    NeoForgePayload::decode
            );

    private static void encode(FriendlyByteBuf buf, NeoForgePayload payload) {
        byte[] data = payload.inner().encode();
        buf.writeVarInt(data.length);
        buf.writeBytes(data);
    }

    private static NeoForgePayload decode(FriendlyByteBuf buf) {
        int length = buf.readVarInt();
        byte[] data = new byte[length];
        buf.readBytes(data);
        return new NeoForgePayload(DataHotloadPayload.decode(data));
    }

    @Override
    public Type<? extends CustomPayload> type() {
        return TYPE;
    }
}
