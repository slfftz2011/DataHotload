package com.slfftz.datahotload.fabric.server;

import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.fabric.DataHotloadFabric;
import com.slfftz.datahotload.fabric.network.FabricNetworkHandler;
import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.Files;
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
        // 只展示关键修改片段 —— 放到 fabric/V1_21_1/src/main/java/.../DataHotloadFabricServer.java

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Inject the server reference into the network handler
            networkHandler.setServer(server);

            // Resolve the world directory intelligently:
            Path worldDir = resolveWorldDir(server.getRunDirectory());

            // Create the datapack reloader backed by vanilla reloadResources
            reloader = new FabricDatapackReloader(server);

            // Create and start the core server entry point
            serverEntryPoint = new ServerEntryPoint<>(worldDir, reloader, networkHandler);
            serverEntryPoint.start();

            DataHotloadFabric.LOGGER.info("Fabric server entry point started, watching: {}", worldDir.resolve("datapacks"));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (serverEntryPoint != null) {
                serverEntryPoint.stop();
                DataHotloadFabric.LOGGER.info("Fabric server entry point stopped");
            }
        });

        DataHotloadFabric.LOGGER.info("Fabric server entry point initialized");
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

    private Path resolveWorldDir(Path runDir) {
        // 1) Check for run/saves/<dir-with-level.dat> (common for integrated singleplayer)
        Path saves = runDir.resolve("saves");
        if (Files.exists(saves) && Files.isDirectory(saves)) {
            try (java.nio.file.DirectoryStream<Path> ds = Files.newDirectoryStream(saves)) {
                for (Path candidate : ds) {
                    if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("level.dat"))) {
                        DataHotloadFabric.LOGGER.info("Resolved world dir from saves: {}", candidate);
                        return candidate;
                    }
                }
            } catch (IOException e) {
                // ignore and fall back
            }
        }

        // 2) Fallback to run/world (dedicated server default)
        Path world = runDir.resolve("world");
        if (Files.exists(world) && Files.isDirectory(world)) {
            DataHotloadFabric.LOGGER.info("Resolved world dir: {}", world);
            return world;
        }

        // 3) Final fallback: runDir itself
        DataHotloadFabric.LOGGER.info("Falling back to run directory as world dir: {}", runDir);
        return runDir;
    }
}
