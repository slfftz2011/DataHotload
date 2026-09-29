package com.slfftz.datahotload.core.server;

import java.util.concurrent.CompletableFuture;

/**
 * Loader-agnostic interface for triggering a datapack reload.
 *
 * Implementations may provide either the synchronous reload() -> ReloadResult
 * API or the asynchronous reloadAllDatapacks() -> CompletableFuture<Void>.
 * Default methods bridge the two so older/newer implementations interoperate.
 */
public interface DatapackReloader {

    /**
     * Asynchronous API: trigger a reload and return a CompletableFuture that
     * completes when the reload finishes. Implementations that prefer async
     * behaviour should override this.
     */
    default CompletableFuture<Void> reloadAllDatapacks() {
        // Default bridges to synchronous reload()
        ReloadResult result = reload();
        if (result.isSuccess()) {
            return CompletableFuture.completedFuture(null);
        } else {
            CompletableFuture<Void> f = new CompletableFuture<>();
            f.completeExceptionally(new RuntimeException(result.getErrorMessage()));
            return f;
        }
    }

    /**
     * Synchronous API returning a ReloadResult (used by ServerEntryPoint).
     * Implementations that prefer sync behaviour should override this.
     */
    default ReloadResult reload() {
        long start = System.currentTimeMillis();
        try {
            reloadAllDatapacks().join();
            long duration = System.currentTimeMillis() - start;
            return ReloadResult.success(null, duration); // datapack name unknown here
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            return ReloadResult.failure(null, cause, duration);
        }
    }

    /**
     * Whether the reloader is currently available on this server instance.
     * Implementations should override this.
     */
    boolean isAvailable();
}