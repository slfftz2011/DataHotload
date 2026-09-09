package com.slfftz.datahotload.core.common.network;

/**
 * Loader-agnostic interface for sending {@link DataHotloadPayload} to clients.
 * <p>
 * Each loader module provides its own implementation backed by its
 * networking API (Fabric PayloadRegistry, NeoForge CustomPayload, Bukkit PluginMessaging).
 *
 * @param <P> the loader-specific player type
 */
public interface NetworkHandler<P> {

    /**
     * Send a payload to a specific player.
     */
    void sendToPlayer(P player, DataHotloadPayload payload);

    /**
     * Broadcast a payload to all currently connected players.
     */
    void sendToAllPlayers(DataHotloadPayload payload);

    /**
     * Register the payload channel / handler with the loader's networking system.
     * Called once during mod initialization.
     */
    void register();
}
