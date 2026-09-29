package com.slfftz.datahotload.neoforge.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import com.slfftz.datahotload.core.server.ReloadResult;
import net.minecraft.Util;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NeoForgeDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;
    private volatile String lastChangedDatapackName = "unknown";

    public NeoForgeDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Void> reloadAllDatapacks() {
        long start = System.currentTimeMillis();
        try {
            var packRepository = server.getPackRepository();
            List<String> selectedIds = new ArrayList<>(packRepository.getSelectedIds());

            CompletableFuture<?> future = server.reloadResources(
                    selectedIds,
                    selectedIds,
                    Util.backgroundExecutor(),
                    server
            );

            return future.thenAccept(v -> {
            }).exceptionally(ex -> {
                throw new RuntimeException(ex);
            });
        } catch (Exception e) {
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    @Override
    public boolean isAvailable() {
        return server != null && server.isRunning();
    }

    public void setLastChangedDatapackName(String name) {
        this.lastChangedDatapackName = name != null ? name : "unknown";
    }
}