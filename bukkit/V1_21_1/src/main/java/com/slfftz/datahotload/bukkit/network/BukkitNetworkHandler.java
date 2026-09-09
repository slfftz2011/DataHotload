package com.slfftz.datahotload.bukkit.network;

import com.slfftz.datahotload.core.common.DataHotloadConstants;
import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import com.slfftz.datahotload.core.common.network.NetworkHandler;

import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

/**
 * Bukkit/Paper implementation of {@link NetworkHandler} backed by the
 * Plugin Messaging Channel API.
 * <p>
 * The server registers an <em>outgoing</em> channel named
 * {@value DataHotloadConstants#CHANNEL_ID} and pushes serialized
 * {@link DataHotloadPayload} bytes onto it. Only players whose client has
 * installed the matching Fabric/NeoForge mod (which registers the same
 * channel on the client side) can decode these messages; players without
 * the mod simply ignore the bytes.
 * <p>
 * Bukkit itself never processes the client-side payload logic — there is
 * no "client entry point" in a Bukkit plugin.
 */
public class BukkitNetworkHandler implements NetworkHandler<Player> {

    private final JavaPlugin plugin;

    public BukkitNetworkHandler(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void register() {
        // 1.13+ channels use the namespaced "namespace:path" form.
        // Bukkit accepts the channel name verbatim; the client mod must
        // register the identical identifier.
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(
                plugin, DataHotloadConstants.CHANNEL_ID);
        plugin.getLogger().info("[DataHotload] Registered outgoing plugin channel: "
                + DataHotloadConstants.CHANNEL_ID);
    }

    @Override
    public void sendToPlayer(Player player, DataHotloadPayload payload) {
        byte[] data = payload.encode();
        try {
            player.sendPluginMessage(plugin, DataHotloadConstants.CHANNEL_ID, data);
        } catch (Exception e) {
            // Sending can fail if the player disconnected mid-send or the
            // client does not support the channel. Log and continue.
            plugin.getLogger().log(Level.WARNING,
                    "[DataHotload] Failed to send payload to " + player.getName(), e);
        }
    }

    @Override
    public void sendToAllPlayers(DataHotloadPayload payload) {
        byte[] data = payload.encode();
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            try {
                player.sendPluginMessage(plugin, DataHotloadConstants.CHANNEL_ID, data);
            } catch (Exception e) {
                plugin.getLogger().log(Level.FINE,
                        "[DataHotload] Failed to send payload to " + player.getName(), e);
            }
        }
    }

    /**
     * Unregister the outgoing channel. Called from {@code onDisable}.
     */
    public void unregister() {
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(
                plugin, DataHotloadConstants.CHANNEL_ID);
    }
}
