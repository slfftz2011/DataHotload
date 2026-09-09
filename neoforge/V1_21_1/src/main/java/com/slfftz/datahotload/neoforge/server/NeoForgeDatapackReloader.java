package com.slfftz.datahotload.neoforge.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import com.slfftz.datahotload.core.server.ReloadResult;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * NeoForge implementation of {@link DatapackReloader} that triggers a
 * vanilla datapack resource reload via {@link MinecraftServer#reloadResources(...)}.
 * <p>
 * This mirrors what the vanilla {@code /reload} command does:
 * <ol>
 *   <li>Gather currently selected pack IDs from the pack repository</li>
 *   <li>Call {@code reloadResources(...)} on the server</li>
 *   <li>Block on the returned {@link CompletableFuture} and capture any errors</li>
 * </ol>
 * <p>
 * Note: reload runs synchronously on the calling thread. The caller
 * (e.g. {@code ServerEntryPoint.onDatapackChanged}) runs on a daemon watcher
 * thread, so blocking here does not freeze the main game thread — but the
 * reload itself internally hops to the server thread as needed.
 */
public class NeoForgeDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;
    private volatile String lastChangedDatapackName = "unknown";

    public NeoForgeDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public ReloadResult reload() {
        long start = System.currentTimeMillis();
        String datapackName = this.lastChangedDatapackName;

        try {
            // Gather currently selected datapack IDs
            var packRepository = server.getPackRepository();
            List<String> selectedIds = new ArrayList<>(packRepository.getSelectedIds());

            // Trigger vanilla resource reload (same as /reload command)
            // backgroundExecutor = common pool, gameExecutor = server itself (implements Executor)
            CompletableFuture<?> future = server.reloadResources(
                    selectedIds,
                    selectedIds,
                    Util.backgroundExecutor(),
                    server
            );

            // Block until reload completes
            future.join();

            long duration = System.currentTimeMillis() - start;
            return ReloadResult.success(datapackName, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            // Unwrap CompletionException if present
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return ReloadResult.failure(datapackName, cause, duration);
        }
    }

    @Override
    public String getLastChangedDatapackName() {
        return lastChangedDatapackName;
    }

    /**
     * Called by the watcher (via ServerEntryPoint) to record which datapack
     * triggered the most recent reload, so error reports can name it.
     *
     * @param name the datapack directory / zip file name
     */
    public void setLastChangedDatapackName(String name) {
        this.lastChangedDatapackName = name != null ? name : "unknown";
    }
}
