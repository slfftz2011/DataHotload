package com.slfftz.datahotload.neoforge.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import net.minecraft.server.MinecraftServer;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class NeoForgeDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;
    private volatile String lastChangedDatapackName = "unknown";

    public NeoForgeDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Void> reloadAllDatapacks() {
        // Submit the reload to the server thread, refresh the pack repository
        // so newly added datapack folders are picked up, and keep the currently
        // enabled packs selected (mirrors Fabric's scanPacks + setEnabledProfiles).
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            server.execute(() -> {
                try {
                    var packRepository = server.getPackRepository();
                    Collection<String> enabled = packRepository.getSelectedIds();

                    packRepository.reload();
                    packRepository.setSelected(enabled);

                    server.reloadResources(enabled).whenComplete((v, t) -> {
                        if (t == null) {
                            future.complete(null);
                        } else {
                            future.completeExceptionally(t);
                        }
                    });
                } catch (Throwable t) {
                    future.completeExceptionally(t);
                }
            });
        } catch (Throwable t) {
            future.completeExceptionally(t);
        }
        return future;
    }

    @Override
    public boolean isAvailable() {
        return server != null && server.isRunning();
    }

    public void setLastChangedDatapackName(String name) {
        this.lastChangedDatapackName = name != null ? name : "unknown";
    }
}
