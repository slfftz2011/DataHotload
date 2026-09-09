package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.fabric.network.FabricNetworkHandler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.nio.file.Path;

/**
 * Dedicated server-side Fabric mod entry point.
 * <p>
 * Wires together the core {@link ServerEntryPoint} with Fabric's server
 * lifecycle events. Starts the datapack watcher when the server starts
 * and stops it when the server begins shutting down.
 */
public class DataHotloadFabricServer implements DedicatedServerModInitializer {

    private ServerEntryPoint<ServerPlayerEntity> serverEntryPoint;
    private FabricNetworkHandler networkHandler;
    private FabricDatapackReloader reloader;

    @Override
    public void onInitializeServer() {
        // Create the network handler early (server reference will be injected on SERVER_STARTED)
        this.networkHandler = new FabricNetworkHandler();

        // Register server lifecycle events
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Inject the server reference into the network handler
            networkHandler.setServer(server);

            // Resolve the world directory: <runDir>/world
            // TODO: Use server.getSavePath() or level storage API to support custom world names
            Path worldDir = server.getRunDirectory().resolve("world");

            // Create the datapack reloader backed by vanilla reloadResources
            reloader = new FabricDatapackReloader(server);

            // Create and start the core server entry point
            serverEntryPoint = new ServerEntryPoint<>(worldDir, reloader, networkHandler);
            serverEntryPoint.start();

            System.out.println("[DataHotload] Fabric server entry point started, watching: "
                    + worldDir.resolve("datapacks"));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (serverEntryPoint != null) {
                serverEntryPoint.stop();
                System.out.println("[DataHotload] Fabric server entry point stopped");
            }
        });

        System.out.println("[DataHotload] Fabric server entry point initialized");
    }

    /**
     * @return the core server entry point (available after SERVER_STARTED)
     */
    public ServerEntryPoint<ServerPlayerEntity> getServerEntryPoint() {
        return serverEntryPoint;
    }

    /**
     * @return the datapack reloader (available after SERVER_STARTED)
     */
    public FabricDatapackReloader getReloader() {
        return reloader;
    }
}
