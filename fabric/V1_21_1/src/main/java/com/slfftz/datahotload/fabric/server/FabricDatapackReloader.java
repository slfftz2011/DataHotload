package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.DataPackSettings;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class FabricDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;

    public FabricDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Void> reloadAllDatapacks() {
        DataPackSettings settings = server.getSaveProperties().getDataPackSettings();
        Collection<String> enabled = settings.enabled();

        return server.reloadResources(enabled).thenApply(r -> null);
    }

    @Override
    public boolean isAvailable() {
        return server != null && server.isRunning();
    }
}
