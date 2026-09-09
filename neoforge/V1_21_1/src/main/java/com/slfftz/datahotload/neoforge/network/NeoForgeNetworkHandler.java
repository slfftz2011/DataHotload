package com.slfftz.datahotload.neoforge.network;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import com.slfftz.datahotload.core.common.network.NetworkHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

/**
 * NeoForge implementation of {@link NetworkHandler} that sends
 * {@link DataHotloadPayload}s to connected players via the vanilla
 * {@link ClientboundCustomPayloadPacket} mechanism.
 * <p>
 * The payload type and handler are registered separately in
 * {@link com.slfftz.datahotload.neoforge.DataHotloadNeoForge#registerPayloads},
 * so {@link #register()} here is a no-op.
 */
public class NeoForgeNetworkHandler implements NetworkHandler<ServerPlayer> {

    private final MinecraftServer server;

    public NeoForgeNetworkHandler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void sendToPlayer(ServerPlayer player, DataHotloadPayload payload) {
        player.connection.send(new ClientboundCustomPayloadPacket(new NeoForgePayload(payload)));
    }

    @Override
    public void sendToAllPlayers(DataHotloadPayload payload) {
        NeoForgePayload wrapped = new NeoForgePayload(payload);
        ClientboundCustomPayloadPacket packet = new ClientboundCustomPayloadPacket(wrapped);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }

    /**
     * No-op — payload registration is handled in the mod's
     * {@code RegisterPayloadHandlersEvent} listener.
     */
    @Override
    public void register() {
        // Payload type + codec + handler are registered in
        // DataHotloadNeoForge#registerPayloads during mod construction.
    }
}
