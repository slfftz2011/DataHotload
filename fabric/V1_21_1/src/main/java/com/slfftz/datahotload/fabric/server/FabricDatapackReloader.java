package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.DataPackManager;
import net.minecraft.resource.ResourcePackProfile;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class FabricDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;

    public FabricDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Void> reloadAllDatapacks() {
        DataPackManager manager = server.getDataPackManager();

        Collection<String> enabledIds = manager.getEnabledIds().stream()
                .map(ResourcePackProfile::getId)
                .toList();

        return server.reloadResources(enabledIds);
    }

    @Override
    public boolean isAvailable() {
        return server != null && server.isRunning();
    }
}
