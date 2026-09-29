package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.DatapackReloader;
import net.minecraft.server.MinecraftServer;

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
            // Yarn: ResourcePackManager (原 PackRepository) 直接提供 getEnabledIds()
            Collection<String> enabled = server.getDataPackManager().getEnabledIds();

            // 重新扫描数据包目录，使新加入/修改的数据包生效
            server.getDataPackManager().scanPacks();

            // 恢复启用列表（scanPacks 后会重置状态，需要重新 apply）
            server.getDataPackManager().setEnabledProfiles(enabled);

            return CompletableFuture.completedFuture(null);
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
