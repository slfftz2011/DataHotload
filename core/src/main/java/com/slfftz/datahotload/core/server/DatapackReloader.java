package com.slfftz.datahotload.core.server;

/**
 * Loader-agnostic interface for triggering a datapack reload.
 * <p>
 * Each loader implements this using its own access to the Minecraft server
 * internals:
 * <ul>
 *   <li>Fabric/NeoForge: {@code MinecraftServer#reloadResources(...)}</li>
 *   <li>Bukkit/Paper: Paper API {@code Server#reloadDataPacks()} or reflection</li>
 * </ul>
 */
public interface DatapackReloader {

    /**
     * Trigger a full datapack reload using the vanilla mechanism.
     *
     * @return the result of the reload operation
     */
    ReloadResult reload();

    /**
     * Get the name of the datapack that most recently changed (for error reporting).
     * May return "unknown" if multiple packs changed or the name cannot be determined.
     */
    String getLastChangedDatapackName();
}
