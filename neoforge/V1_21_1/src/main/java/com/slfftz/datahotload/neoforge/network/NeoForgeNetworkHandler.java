package com.slfftz.datahotload.neoforge.network;

import com.slfftz.datahotload.core.common.DataHotloadConstants;
import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record NeoForgePayload(DataHotloadPayload inner) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<NeoForgePayload> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.tryBuild(
                            DataHotloadConstants.CHANNEL_NAMESPACE,
                            DataHotloadConstants.CHANNEL_PATH
                    )
            );

    public static final StreamCodec<FriendlyByteBuf, NeoForgePayload> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE_ARRAY,
                    NeoForgePayload::innerBytes,
                    NeoForgePayload::new
            );

    private byte[] innerBytes() {
        return inner.encode();
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
