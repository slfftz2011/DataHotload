package com.slfftz.datahotload.bukkit;

import com.slfftz.datahotload.bukkit.network.BukkitNetworkHandler;
import com.slfftz.datahotload.core.common.DataHotloadConstants;
import com.slfftz.datahotload.core.server.ReloadResult;
import com.slfftz.datahotload.core.server.ServerEntryPoint;

import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.nio.file.Path;

/**
 * Bukkit/Paper 1.21 entry point for DataHotload.
 * <p>
 * Wires the loader-agnostic {@link ServerEntryPoint} to Bukkit services:
 * <ul>
 *   <li>{@link BukkitNetworkHandler} for plugin-messaging broadcast</li>
 *   <li>{@link BukkitDatapackReloader} for triggering datapack reloads</li>
 *   <li>The main world's folder as the watched {@code datapacks} root</li>
 * </ul>
 * Bukkit has no client entry point: players must install the corresponding
 * Fabric/NeoForge mod client-side to decode the pushed payloads.
 */
public final class DataHotloadBukkit extends JavaPlugin {

    private BukkitNetworkHandler networkHandler;
    private BukkitDatapackReloader reloader;
    private ServerEntryPoint<Player> serverEntryPoint;

    @Override
    public void onEnable() {
        // 1. Networking
        this.networkHandler = new BukkitNetworkHandler(this);

        // 2. Reloader
        this.reloader = new BukkitDatapackReloader(this);

        // 3. Resolve the overworld folder. ServerEntryPoint resolves
        //    worldDir.resolve("datapacks") internally, so we pass the
        //    world root, not the datapacks subdir.
        World primaryWorld = getServer().getWorlds().get(0);
        Path worldDir = primaryWorld.getWorldFolder().toPath();
        getLogger().info("[DataHotload] World directory: " + worldDir);

        // 4. Start the loader-agnostic server entry point.
        //    Its start() registers the network channel and launches the
        //    WatchService thread.
        this.serverEntryPoint = new ServerEntryPoint<>(worldDir, reloader, networkHandler);
        this.serverEntryPoint.start();

        getLogger().info("[DataHotload] Enabled on Bukkit/Paper "
                + getServer().getMinecraftVersion()
                + " (channel: " + DataHotloadConstants.CHANNEL_ID + ")");
    }

    @Override
    public void onDisable() {
        if (serverEntryPoint != null) {
            serverEntryPoint.stop();
        }
        if (networkHandler != null) {
            networkHandler.unregister();
        }
        getLogger().info("[DataHotload] Disabled");
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equalsIgnoreCase("datahotload")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage("[DataHotload] Usage: /datahotload reload");
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload": {
                sender.sendMessage("[DataHotload] Triggering manual datapack reload...");
                ReloadResult result = serverEntryPoint.triggerReload();
                if (result.isSuccess()) {
                    sender.sendMessage("[DataHotload] Reload succeeded in "
                            + result.getDurationMs() + "ms");
                } else {
                    sender.sendMessage("[DataHotload] Reload FAILED: " + result.getErrorMessage());
                    // Broadcast the captured failure (with real stack trace) to all players
                    com.slfftz.datahotload.core.common.network.DataHotloadPayload payload =
                            new com.slfftz.datahotload.core.common.network.DataHotloadPayload(
                                    com.slfftz.datahotload.core.common.network.DataHotloadPayload.Severity.ERROR,
                                    result.getDatapackName(),
                                    result.getErrorMessage(),
                                    result.getStackTrace() != null
                                            ? result.getStackTrace()
                                            : new java.util.ArrayList<>());
                    networkHandler.sendToAllPlayers(payload);
                }
                return true;
            }
            case "status": {
                boolean watching = serverEntryPoint != null
                        && serverEntryPoint.getWatcher().isRunning();
                sender.sendMessage("[DataHotload] Watching: " + watching
                        + " | Last changed: " + serverEntryPoint.getWatcher().getLastChangedName()
                        + " | Errors: " + serverEntryPoint.getErrorHistory().size());
                return true;
            }
            default:
                sender.sendMessage("[DataHotload] Unknown subcommand: " + args[0]
                        + ". Usage: /datahotload reload|status");
                return true;
        }
    }

    public ServerEntryPoint<Player> getServerEntryPoint() {
        return serverEntryPoint;
    }
}
