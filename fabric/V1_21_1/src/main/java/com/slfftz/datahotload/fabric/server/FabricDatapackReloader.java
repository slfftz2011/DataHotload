package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import net.minecraft.server.MinecraftServer;

import java.util.concurrent.CompletableFuture;

public class FabricDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;

    public FabricDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Void> reloadAllDatapacks() {
        return server.reloadResources().thenApply(r -> null);
    }

    @Override
    public boolean isAvailable() {
        return server != null && server.isRunning();
    }
}
