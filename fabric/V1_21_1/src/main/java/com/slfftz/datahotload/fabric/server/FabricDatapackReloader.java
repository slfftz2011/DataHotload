package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.DatapackReloader;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.world.level.storage.WorldData;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

public class FabricDatapackReloader implements DatapackReloader {

    private final MinecraftServer server;

    public FabricDatapackReloader(MinecraftServer server) {
        this.server = server;
    }

    @Override
    public CompletableFuture<Void> reloadAllDatapacks() {
        // 获取当前已启用的数据包ID集合，等价于 /reload
        PackRepository repo = server.getPackRepository();
        Collection<String> enabledPacks = repo.getSelectedIds();
        return server.reloadResources(enabledPacks);
    }

    @Override
    public boolean isAvailable() {
        return server.isRunning();
    }
}
