package com.slfftz.datahotload.fabric;

import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.fabric.network.DataHotloadPayloadS2C;
import com.slfftz.datahotload.fabric.network.FabricNetworkHandler;
import com.slfftz.datahotload.fabric.server.FabricDatapackReloader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;

/**
 * Main Fabric mod entry point (runs on both client and server).
 * <p>
 * Registers the S2C payload type codec with the Fabric networking system.
 * Client-side and server-side logic are handled in their respective entry points.
 */
public class DataHotloadFabric implements ModInitializer {

    @Override
    public void onInitialize() {

        // Register the S2C payload type so the client can decode incoming packets.
        // The client receiver is registered separately in DataHotloadFabricClient.
        try {
            PayloadTypeRegistry.playS2C().register(DataHotloadPayloadS2C.ID, DataHotloadPayloadS2C.CODEC);
            System.out.println("[DataHotload] Registered S2C payload codec");
        } catch (Throwable t) {
            System.err.println("[DataHotload] Failed to register S2C payload codec: " + t);
            t.printStackTrace();
        }

        // 注册服务器启动/停止回调（适用于 dedicated 与 integrated）
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // 这是与你原来 DataHotloadFabricServer 中基本相同的逻辑
            FabricNetworkHandler networkHandler = new FabricNetworkHandler();
            networkHandler.setServer(server); // 如果需要注入 server

            Path worldDir = server.getRunDirectory().resolve("world");
            FabricDatapackReloader reloader = new FabricDatapackReloader(server);

            ServerEntryPoint<ServerPlayerEntity> serverEntryPoint =
                    new ServerEntryPoint<>(worldDir, reloader, networkHandler);
            serverEntryPoint.start();

            System.out.println("[DataHotload] Fabric server entry point started, watching: "
                    + worldDir.resolve("datapacks"));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            // 这里你需要保存引用以便 stop()
            // 如果你把 serverEntryPoint 存为字段，调用 serverEntryPoint.stop()
            System.out.println("[DataHotload] Fabric server entry point stopped");
        });
        System.out.println("[DataHotload] Fabric main entry point initialized");
    }
}
