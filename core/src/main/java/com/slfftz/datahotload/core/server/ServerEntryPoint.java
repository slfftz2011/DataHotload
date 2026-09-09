package com.slfftz.datahotload.core.server;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import com.slfftz.datahotload.core.common.network.NetworkHandler;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Server-side lifecycle manager that ties together the datapack watcher,
 * reloader, and network handler.
 * <p>
 * Loader-specific server initializers create an instance, start the watcher,
 * and the entry point handles reload triggering and error broadcasting.
 *
 * @param <P> the loader-specific player type
 */
public class ServerEntryPoint<P> {

    private final DatapackWatcher watcher;
    private final DatapackReloader reloader;
    private final NetworkHandler<P> networkHandler;
    private final List<DataHotloadPayload> errorHistory = new ArrayList<>();

    public ServerEntryPoint(Path worldDir, DatapackReloader reloader, NetworkHandler<P> networkHandler) {
        Path datapacksDir = worldDir.resolve("datapacks");
        this.reloader = reloader;
        this.networkHandler = networkHandler;
        this.watcher = new DatapackWatcher(datapacksDir, this::onDatapackChanged);
    }

    /**
     * Start the server-side components (watcher + network registration).
     */
    public void start() {
        try {
            networkHandler.register();
            watcher.start();
            System.out.println("[DataHotload] Server entry point started");
        } catch (Exception e) {
            System.err.println("[DataHotload] Failed to start server entry point: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Stop the server-side components.
     */
    public void stop() {
        watcher.close();
        System.out.println("[DataHotload] Server entry point stopped");
    }

    /**
     * Called by the watcher when a datapack change is detected (after debounce).
     */
    private void onDatapackChanged(String datapackName) {
        System.out.println("[DataHotload] Datapack change detected: " + datapackName + ", triggering reload...");

        ReloadResult result = reloader.reload();

        if (result.isSuccess()) {
            System.out.println("[DataHotload] Reload succeeded in " + result.getDurationMs() + "ms");
        } else {
            System.err.println("[DataHotload] Reload failed: " + result.getErrorMessage());
            broadcastError(result);
        }
    }

    /**
     * Manually trigger a reload (e.g., via command).
     */
    public ReloadResult triggerReload() {
        return reloader.reload();
    }

    private void broadcastError(ReloadResult result) {
        DataHotloadPayload payload = new DataHotloadPayload(
                DataHotloadPayload.Severity.ERROR,
                result.getDatapackName() != null ? result.getDatapackName() : watcher.getLastChangedName(),
                result.getErrorMessage() != null ? result.getErrorMessage() : "Unknown reload error",
                result.getStackTrace() != null ? result.getStackTrace() : new ArrayList<>()
        );

        errorHistory.add(payload);
        networkHandler.sendToAllPlayers(payload);
    }

    public List<DataHotloadPayload> getErrorHistory() {
        return new ArrayList<>(errorHistory);
    }

    public DatapackWatcher getWatcher() {
        return watcher;
    }

    public DatapackReloader getReloader() {
        return reloader;
    }
}
