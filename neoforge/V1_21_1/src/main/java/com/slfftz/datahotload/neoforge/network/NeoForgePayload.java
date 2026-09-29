package com.slfftz.datahotload.neoforge.network;

import com.slfftz.datahotload.core.common.DataHotloadConstants;
import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * NeoForge-wrapped {@link CustomPacketPayload} that carries a core {@link DataHotloadPayload}.
 * <p>
 * Serialization delegates to the core payload's own binary format via
 * {@link DataHotloadPayload#encode()} and {@link DataHotloadPayload#decode(byte[])}.
 * The wire format is simply the raw byte array produced by the core encoder.
 * <p>
 * Channel ID: {@value DataHotloadConstants#CHANNEL_ID}
 */
public record NeoForgePayload(DataHotloadPayload inner) implements CustomPacketPayload {

    /** Payload type identifier registered with NeoForge's networking system. */
    public static final CustomPacketPayload.Type<NeoForgePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.tryBuild(
                            DataHotloadConstants.CHANNEL_NAMESPACE,
                            DataHotloadConstants.CHANNEL_PATH
                    )
            );

    /**
     * Stream codec for encoding/decoding this payload on the network buffer.
     * <p>
     * Encoding: writes a varint length prefix followed by the raw bytes from
     * {@link DataHotloadPayload#encode()}.
     * Decoding: reads the varint length, then the bytes, and calls
     * {@link DataHotloadPayload#decode(byte[])}.
     */
    public static final StreamCodec<FriendlyByteBuf, NeoForgePayload> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(FriendlyByteBuf buf, NeoForgePayload payload) {
            ByteBufCodecs.BYTE_ARRAY.encode(buf, payload.inner.encode());
        }

        @Override
        public NeoForgePayload decode(FriendlyByteBuf buf) {
            byte[] data = ByteBufCodecs.BYTE_ARRAY.decode(buf);
            return new NeoForgePayload(DataHotloadPayload.decode(data));
        }
    };

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
