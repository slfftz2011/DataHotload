package com.slfftz.datahotload.neoforge.network;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import com.slfftz.datahotload.core.common.network.NetworkHandler;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;

public class NeoForgeNetworkHandler implements NetworkHandler<ServerPlayer> {

    private final MinecraftServer server;

    public NeoForgeNetworkHandler(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void sendToPlayer(ServerPlayer player, DataHotloadPayload payload) {
        player.connection.send(
                new ClientboundCustomPayloadPacket(new NeoForgePayload(payload))
        );
    }

    @Override
    public void sendToAllPlayers(DataHotloadPayload payload) {
        ClientboundCustomPayloadPacket packet =
                new ClientboundCustomPayloadPacket(new NeoForgePayload(payload));

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.connection.send(packet);
        }
    }

    @Override
    public void register() {
        // Payload type + codec + handler are registered in
        // DataHotloadNeoForge#registerPayloads during mod construction.
    }
}
