package com.slfftz.datahotload.fabric.network;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import com.slfftz.datahotload.core.common.network.NetworkHandler;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * Fabric implementation of {@link NetworkHandler} for sending
 * {@link DataHotloadPayload} to connected clients.
 * <p>
 * Uses Fabric's {@link ServerPlayNetworking} API to transmit the
 * {@link DataHotloadPayloadS2C} wrapper payload to players.
 * <p>
 * The payload type itself is registered in
 * {@link com.slfftz.datahotload.fabric.DataHotloadFabric#onInitialize()},
 * so {@link #register()} here is a no-op.
 */
public class FabricNetworkHandler implements NetworkHandler<ServerPlayerEntity> {

    private volatile MinecraftServer server;

    /**
     * Called by the server lifecycle when the MinecraftServer instance becomes available.
     *
     * @param server the running dedicated server
     */
    public void setServer(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public void sendToPlayer(ServerPlayerEntity player, DataHotloadPayload payload) {
        ServerPlayNetworking.send(player, new DataHotloadPayloadS2C(payload));
    }

    @Override
    public void sendToAllPlayers(DataHotloadPayload payload) {
        MinecraftServer current = this.server;
        if (current == null) {
            return;
        }
        DataHotloadPayloadS2C wrapper = new DataHotloadPayloadS2C(payload);
        for (ServerPlayerEntity player : current.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, wrapper);
        }
    }

    @Override
    public void register() {
        // Payload type registration is done in DataHotloadFabric.onInitialize()
        // via PayloadTypeRegistry.playS2C().register(ID, CODEC).
        // This method exists to satisfy the NetworkHandler interface contract
        // (called by ServerEntryPoint.start()).
    }
}
