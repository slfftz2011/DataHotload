package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.WorldDataConfiguration;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class FabricDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;

    public FabricDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Void> reloadAllDatapacks() {
        try {
            WorldDataConfiguration config = server.getSaveProperties().getDataConfiguration();
            Collection<String> enabled = config.getEnabled();

            // MinecraftServer#reloadResources(Collection<String>) -> returns CompletableFuture<?>
            return server.reloadResources(enabled).thenApply(r -> null);
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
}