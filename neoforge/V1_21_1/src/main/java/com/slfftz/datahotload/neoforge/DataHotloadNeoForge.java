package com.slfftz.datahotload.neoforge;

import com.slfftz.datahotload.core.common.DataHotloadConstants;
import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.neoforge.server.NeoForgeDatapackReloader;
import com.slfftz.datahotload.neoforge.network.NeoForgeNetworkHandler;
import com.slfftz.datahotload.neoforge.network.NeoForgePayload;
import com.slfftz.datahotload.neoforge.client.NeoForgeClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * NeoForge mod main class. Registers the S2C payload with the NeoForge
 * networking system on the mod event bus, and wires the core
 * {@link ServerEntryPoint} on the game event bus.
 */
@Mod("datahotload")
public class DataHotloadNeoForge {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataHotloadNeoForge.class);

    private MinecraftServer minecraftServer;
    private ServerEntryPoint<ServerPlayer> serverEntryPoint;
    private NeoForgeNetworkHandler networkHandler;
    private NeoForgeDatapackReloader reloader;

    public DataHotloadNeoForge(IEventBus modEventBus) {
        // Register the S2C payload type + codec + client handler on the mod event bus
        modEventBus.addListener(this::registerPayloads);

        // Register lifecycle listeners on the NeoForge game event bus
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(this::onServerStopping);

        // Client-only initialization
        if (FMLEnvironment.dist == Dist.CLIENT) {
            initializeClient();
        }

        LOGGER.info("[DataHotload] NeoForge mod constructed");
    }

    private void onServerStarting(ServerStartingEvent event) {
        this.minecraftServer = event.getServer();

        // Use the server-provided world path (works for dedicated and integrated)
        Path worldDir = minecraftServer.getWorldPath(LevelResource.ROOT);

        this.reloader = new NeoForgeDatapackReloader(minecraftServer);
        this.networkHandler = new NeoForgeNetworkHandler(minecraftServer);

        serverEntryPoint = new ServerEntryPoint<>(worldDir, reloader, networkHandler);
        serverEntryPoint.start();

        LOGGER.info("[DataHotload] NeoForge server entry point initialized for world: " + worldDir);
    }

    /**
     * Registers the {@link NeoForgePayload} with NeoForge's networking system
     * (equivalent to Fabric's {@code PayloadTypeRegistry.playS2C().register}).
     * The handler is only invoked on the physical client for S2C packets.
     */
    private void registerPayloads(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar =
                event.registrar(String.valueOf(DataHotloadConstants.PROTOCOL_VERSION));
        registrar.playToClient(
                NeoForgePayload.TYPE,
                NeoForgePayload.STREAM_CODEC,
                this::handleClientPayload
        );
        LOGGER.info("[DataHotload] Registered NeoForge S2C payload");
    }

    /**
     * Client-side payload handler: forwards the received payload to
     * {@link NeoForgeClient} on the client (render) thread.
     */
    private void handleClientPayload(NeoForgePayload payload, IPayloadContext context) {
        context.enqueueWork(() ->
                NeoForgeClient.getInstance().onPayloadReceived(payload)
        );
    }

    private void onServerStopping(ServerStoppingEvent event) {
        if (serverEntryPoint != null) {
            serverEntryPoint.stop();
            serverEntryPoint = null;
        }
        minecraftServer = null;
        LOGGER.info("[DataHotload] NeoForge server entry point stopped");
    }

    private void initializeClient() {
        NeoForgeClient.getInstance();
        LOGGER.info("[DataHotload] NeoForge client initialized");
    }

    public ServerEntryPoint<ServerPlayer> getServerEntryPoint() {
        return serverEntryPoint;
    }

    public MinecraftServer getMinecraftServer() {
        return minecraftServer;
    }

    public NeoForgeDatapackReloader getReloader() {
        return reloader;
    }
}
