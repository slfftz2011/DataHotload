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
        // 如果 MinecraftServer 有 submit/callable 提交方法，使用它；否则使用 execute 并手动完成 future。
        CompletableFuture<Void> future = new CompletableFuture<>();
        try {
            server.execute(() -> {
                try {
                    Collection<String> enabled = server.getDataPackManager().getEnabledIds();
                    server.getDataPackManager().scanPacks();
                    server.getDataPackManager().setEnabledProfiles(enabled);

                    server.reloadResources(enabled);
                    future.complete(null);
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
}
