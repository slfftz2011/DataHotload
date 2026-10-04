package com.slfftz.datahotload.fabric;

import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.fabric.network.DataHotloadPayloadS2C;
import com.slfftz.datahotload.fabric.network.FabricNetworkHandler;
import com.slfftz.datahotload.fabric.server.FabricDatapackReloader;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.IOException;
import java.nio.file.*;

/**
 * Main Fabric mod entry point (runs on both client and server).
 * <p>
 * Registers the S2C payload type codec with the Fabric networking system.
 * Client-side and server-side logic are handled in their respective entry points.
 */
public class DataHotloadFabric implements ModInitializer {

    // Keep references so we can stop the entry point on shutdown
    private ServerEntryPoint<ServerPlayerEntity> serverEntryPoint;
    private FabricNetworkHandler networkHandler;
    private FabricDatapackReloader reloader;

    @Override
    public void onInitialize() {

        // Register the S2C payload type so the client can decode incoming packets.
        try {
            PayloadTypeRegistry.playS2C().register(DataHotloadPayloadS2C.ID, DataHotloadPayloadS2C.CODEC);
            System.out.println("[DataHotload] Registered S2C payload codec");
        } catch (Throwable t) {
            System.err.println("[DataHotload] Failed to register S2C payload codec: " + t);
            t.printStackTrace();
        }

        // 注册服务器启动/停止回调（适用于 dedicated 与 integrated）
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            // Create and inject network handler
            this.networkHandler = new FabricNetworkHandler();
            this.networkHandler.setServer(server);

            // Resolve world directory intelligently (saves/<world> preferred for integrated)
            Path worldDir = resolveWorldDir(server.getRunDirectory());

            // Create reloader and server entry point
            this.reloader = new FabricDatapackReloader(server);
            this.serverEntryPoint = new ServerEntryPoint<>(worldDir, reloader, networkHandler);
            this.serverEntryPoint.start();

            System.out.println("[DataHotload] Fabric server entry point started, watching: "
                    + worldDir.resolve("datapacks"));
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            if (this.serverEntryPoint != null) {
                try {
                    this.serverEntryPoint.stop();
                } catch (Throwable t) {
                    System.err.println("[DataHotload] Error stopping server entry point: " + t);
                    t.printStackTrace();
                } finally {
                    this.serverEntryPoint = null;
                }
            }
            System.out.println("[DataHotload] Fabric server entry point stopped");
        });

        System.out.println("[DataHotload] Fabric main entry point initialized");
    }

    /**
     * Try to resolve the actual world directory for the running server.
     * Preference order:
     * 1) runDir/saves/<dir-containing-level.dat> (integrated singleplayer typical)
     * 2) runDir/world (dedicated server typical)
     * 3) runDir (fallback)
     */
    private Path resolveWorldDir(Path runDir) {
        Path saves = runDir.resolve("saves");
        if (Files.isDirectory(saves)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(saves)) {
                for (Path candidate : ds) {
                    if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("level.dat"))) {
                        System.out.println("[DataHotload] Resolved world dir from saves: " + candidate);
                        return candidate;
                    }
                }
            } catch (IOException e) {
                System.err.println("[DataHotload] Error scanning saves directory: " + e.getMessage());
            }
        }

        Path world = runDir.resolve("world");
        if (Files.isDirectory(world)) {
            System.out.println("[DataHotload] Resolved world dir: " + world);
            return world;
        }

        System.out.println("[DataHotload] Falling back to run directory as world dir: " + runDir);
        return runDir;
    }
}