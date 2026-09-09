package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import com.slfftz.datahotload.core.server.ReloadResult;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Fabric implementation of {@link DatapackReloader} that triggers a
 * vanilla datapack reload via {@link MinecraftServer#reloadResources}.
 * <p>
 * Calls the same reload mechanism as the vanilla {@code /reload} command,
 * ensuring consistent behaviour with vanilla datapack loading.
 */
public class FabricDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;
    private volatile String lastChangedDatapackName = "unknown";

    public FabricDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    /**
     * Update the name of the datapack that most recently changed.
     * Called by the watcher / server entry point before triggering a reload.
     *
     * @param name the datapack folder or zip name
     */
    public void setLastChangedDatapackName(String name) {
        this.lastChangedDatapackName = name != null ? name : "unknown";
    }

    @Override
    public ReloadResult reload() {
        long start = System.currentTimeMillis();

        try {
            // Trigger a full datapack reload using the vanilla mechanism.
            // Uses vanilla worker executor for preparation and the server thread for applying,
            // matching the behaviour of the /reload command.
            CompletableFuture<?> future = server.reloadResources(
                    Collections.emptyList(),   // dataPacksToLoad: load all
                    Collections.emptyList(),   // dataPacksToSkip: skip none
                    Util.getMainWorkerExecutor(), // prepareExecutor (background)
                    server                      // applyExecutor (main server thread)
            );

            // Block until the reload completes
            future.join();

            long duration = System.currentTimeMillis() - start;
            return ReloadResult.success(lastChangedDatapackName, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            System.err.println("[DataHotload] Datapack reload failed: " + e.getMessage());
            return ReloadResult.failure(lastChangedDatapackName, e, duration);
        }
    }

    @Override
    public String getLastChangedDatapackName() {
        return lastChangedDatapackName;
    }
}
