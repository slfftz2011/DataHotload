package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resource.DataPackSettings;

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
            DataPackSettings settings = server.getDataPackManager().getSettings();
            Collection<String> enabled = settings.getEnabled();

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