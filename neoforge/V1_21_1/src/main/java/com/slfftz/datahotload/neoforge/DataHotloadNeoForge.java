package com.slfftz.datahotload.neoforge;

import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.neoforge.server.NeoForgeDatapackReloader;
import com.slfftz.datahotload.neoforge.network.NeoForgeNetworkHandler;
import com.slfftz.datahotload.neoforge.client.NeoForgeClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;

/**
 * NeoForge mod main class (slimmed). Listeners are registered on the event bus.
 */
@Mod("datahotload")
public class DataHotloadNeoForge {

    private MinecraftServer minecraftServer;
    private ServerEntryPoint<ServerPlayer> serverEntryPoint;

    public DataHotloadNeoForge() {
        // Register lifecycle listeners on the NeoForge game event bus
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        // Client-only initialization
        if (FMLEnvironment.dist == Dist.CLIENT) {
            initializeClient();
        }

        System.out.println("[DataHotload] NeoForge mod constructed");
    }

    private void onServerStarting(ServerStartingEvent event) {
        this.minecraftServer = event.getServer();

        // Use the server-provided world path (works for dedicated and integrated)
        Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);

        var reloader = new NeoForgeDatapackReloader(minecraftServer);
        var networkHandler = new NeoForgeNetworkHandler(minecraftServer);

        serverEntryPoint = new ServerEntryPoint<>(worldDir, reloader, networkHandler);
        serverEntryPoint.start();

        System.out.println("[DataHotload] NeoForge server entry point initialized for world: " + worldDir);
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (serverEntryPoint != null) {
            serverEntryPoint.stop();
            serverEntryPoint = null;
        }
        minecraftServer = null;
        System.out.println("[DataHotload] NeoForge server entry point stopped");
    }

    private void initializeClient() {
        NeoForgeClient.getInstance();
        System.out.println("[DataHotload] NeoForge client initialized");
    }

    public ServerEntryPoint<ServerPlayer> getServerEntryPoint() {
        return serverEntryPoint;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}
