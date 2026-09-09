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
    CompletableFuture<Void> reloadAllDatapacks();
    boolean isAvailable();
}

