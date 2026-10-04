package com.slfftz.datahotload.neoforge;

import com.slfftz.datahotload.core.server.ServerEntryPoint;
import com.slfftz.datahotload.core.server.WorldLocator;
import com.slfftz.datahotload.neoforge.server.NeoForgeDatapackReloader;
import com.slfftz.datahotload.neoforge.network.NeoForgeNetworkHandler;
import com.slfftz.datahotload.neoforge.network.NeoForgePayload;
import com.slfftz.datahotload.neoforge.client.NeoForgeClient;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

import java.nio.file.Path;

/**
 * NeoForge mod main class (slimmed). Listeners are registered on the event bus.
 */
@Mod("datahotload")
public class DataHotloadNeoForge {

    private MinecraftServer minecraftServer;
    private ServerEntryPoint<ServerPlayer> serverEntryPoint;

    public DataHotloadNeoForge() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // --- Register payload handler on the mod event bus ---
        modEventBus.addListener(this::registerPayloads);

        // --- Register server lifecycle on the NeoForge (game) event bus ---
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::onServerStarting);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(this::onServerStopping);

        // --- Client-side initialization (only on physical client) ---
        if (FMLEnvironment.dist == FMLEnvironment.Dist.CLIENT) {
            initializeClient();
        }

        System.out.println("[DataHotload] NeoForge mod constructed");
    }

    private void registerPayloads(final net.minecraftforge.event.server.ServerStartingEvent event) {
        // placeholder - actual registration handled elsewhere in original code
    }

    private void onServerStarting(ServerStartingEvent event) {
        this.minecraftServer = event.getServer();

        // Prefer run/saves/<world> when running integrated singleplayer
        Path worldDir = WorldLocator.resolveWorldDir(minecraftServer.getRunDirectory());

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
