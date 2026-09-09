package com.slfftz.datahotload.neoforge;

import com.slfftz.datahotload.core.common.DataHotloadConstants;
import com.slfftz.datahotload.core.common.network.NetworkHandler;
import com.slfftz.datahotload.core.server.DatapackReloader;
import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.neoforge.client.NeoForgeClient;
import com.slfftz.datahotload.neoforge.network.NeoForgeNetworkHandler;
import com.slfftz.datahotload.neoforge.network.NeoForgePayload;
import com.slfftz.datahotload.neoforge.server.NeoForgeDatapackReloader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.nio.file.Path;

/**
 * Main entry point for DataHotload on NeoForge 1.21.1.
 * <p>
 * Responsibilities:
 * <ul>
 *   <li>Register the {@link NeoForgePayload} network channel and handler</li>
 *   <li>Start the server-side {@link ServerEntryPoint} when the server boots</li>
 *   <li>Stop it cleanly when the server shuts down</li>
 *   <li>Initialize the client-side receiver on physical clients</li>
 * </ul>
 * <p>
 * Architecture mirrors the loader-agnostic core: this class is a thin adapter
 * that wires NeoForge events and APIs to the core's {@link ServerEntryPoint}.
 */
@Mod(DataHotloadConstants.MOD_ID)
public class DataHotloadNeoForge {

    private ServerEntryPoint<ServerPlayer> serverEntryPoint;
    private MinecraftServer minecraftServer;

    public DataHotloadNeoForge(IEventBus modEventBus, ModContainer modContainer) {
        // --- Register payload handler on the mod event bus ---
        modEventBus.addListener(this::registerPayloads);

        // --- Register server lifecycle on the NeoForge (game) event bus ---
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        // --- Client-side initialization (only on physical client) ---
        if (FMLEnvironment.dist == Dist.CLIENT) {
            initializeClient();
        }

        System.out.println("[DataHotload] NeoForge mod constructed");
    }

    // ========================================================================
    // Networking
    // ========================================================================

    /**
     * Register the DataHotload payload type with NeoForge's networking system.
     * <p>
     * This listens on the mod event bus for {@link RegisterPayloadHandlersEvent}.
     * We register an S2C (server-to-client) handler for {@link NeoForgePayload}.
     * <p>
     * TODO: If NeoForge 21.1.x API differs slightly (e.g. method names),
     * adjust the call below. The conceptual approach is:
     * "register this payload type + codec + client-side handler".
     */
    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        // Register as a play-to-client payload: server sends, client receives.
        // The handler receives the payload and forwards it to NeoForgeClient.
        event.playToClient(
                NeoForgePayload.TYPE,
                NeoForgePayload.STREAM_CODEC,
                (payload, context) -> {
                    // context.enqueueWork ensures we run on the client main thread
                    context.enqueueWork(() -> {
                        if (FMLEnvironment.dist == Dist.CLIENT) {
                            NeoForgeClient.getInstance().onPayloadReceived(payload);
                        }
                    });
                }
        );

        System.out.println("[DataHotload] Payload handler registered: " +
                DataHotloadConstants.CHANNEL_ID);
    }

    // ========================================================================
    // Server Lifecycle
    // ========================================================================

    /**
     * Called when the server is starting. Creates the {@link ServerEntryPoint}
     * with NeoForge-specific adapter implementations.
     */
    private void onServerStarting(ServerStartingEvent event) {
        this.minecraftServer = event.getServer();

        Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);

        DatapackReloader reloader = new NeoForgeDatapackReloader(minecraftServer);
        NetworkHandler<ServerPlayer> networkHandler =
                new NeoForgeNetworkHandler(minecraftServer);

        serverEntryPoint = new ServerEntryPoint<>(worldDir, reloader, networkHandler);
        serverEntryPoint.start();

        System.out.println("[DataHotload] NeoForge server entry point initialized for world: " + worldDir);
    }

    /**
     * Called when the server is stopping. Stops the {@link ServerEntryPoint}
     * and closes the datapack watcher.
     */
    private void onServerStopping(ServerStoppingEvent event) {
        if (serverEntryPoint != null) {
            serverEntryPoint.stop();
            serverEntryPoint = null;
        }
        minecraftServer = null;
        System.out.println("[DataHotload] NeoForge server entry point stopped");
    }

    // ========================================================================
    // Client Initialization
    // ========================================================================

    /**
     * Client-only initialization. Called from the constructor when
     * running on the physical client.
     * <p>
     * Instantiates the client singleton so that payload handlers can
     * immediately forward received packets.
     */
    private void initializeClient() {
        // Touch the singleton to ensure ClientEntryPoint is constructed
        NeoForgeClient.getInstance();
        System.out.println("[DataHotload] NeoForge client initialized");
    }

    // ========================================================================
    // Accessors
    // ========================================================================

    public ServerEntryPoint<ServerPlayer> getServerEntryPoint() {
        return serverEntryPoint;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }
}
