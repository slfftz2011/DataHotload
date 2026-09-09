# DataHotload — NeoForge 1.21.1 Loader

NeoForge adapter layer for the DataHotload core module.

## Target

- Minecraft 1.21.1
- NeoForge 21.1.71+
- NeoGradle 7.0.150
- Java 21

## Structure

```
neoforge/V1_21_1/
├── build.gradle                          # NeoGradle userdev config
├── gradle.properties                    # Version numbers + mod metadata
└── src/main/
    ├── resources/
    │   ├── META-INF/neoforge.mods.toml  # Mod descriptor (Gradle-expandable)
    │   └── pack.mcmeta                  # Resource pack format declaration
    └── java/com/slfftz/datahotload/neoforge/
        ├── DataHotloadNeoForge.java     # @Mod main class — entry point
        ├── network/
        │   ├── NeoForgePayload.java     # CustomPayload wrapping core DataHotloadPayload
        │   └── NeoForgeNetworkHandler.java  # Implements NetworkHandler<ServerPlayer>
        ├── server/
        │   └── NeoForgeDatapackReloader.java # Implements DatapackReloader
        └── client/
            └── NeoForgeClient.java     # Client-side receiver + ClientEntryPoint
```

## Architecture

```
ServerStartingEvent → DataHotloadNeoForge.onServerStarting()
    ├── new NeoForgeDatapackReloader(server)     → DatapackReloader
    ├── new NeoForgeNetworkHandler(server)       → NetworkHandler<ServerPlayer>
    └── new ServerEntryPoint<>(worldDir, reloader, handler)
         ├── DatapackWatcher (WatchService on world/datapacks)
         └── onDatapackChanged → reloader.reload() → broadcast errors via network

RegisterPayloadHandlersEvent → event.playToClient(TYPE, STREAM_CODEC, handler)
    └── handler → NeoForgeClient.getInstance().onPayloadReceived(payload)
         └── ClientEntryPoint.onPayloadReceived() → GuiRenderer / ErrorAnalyzer
```

## Key Implementation Notes

- **Networking**: Uses NeoForge 1.21.1's `RegisterPayloadHandlersEvent` + `CustomPayload` pattern.
  S2C payload registered via `event.playToClient(...)`.
- **Datapack reload**: Calls `MinecraftServer.reloadResources(...)` (same as `/reload` command),
  blocking on the returned `CompletableFuture` from the watcher daemon thread.
- **Server lifecycle**: `ServerStartingEvent` starts the watcher; `ServerStoppingEvent` stops it.
- **Client init**: Client singleton created during mod construction when `FMLEnvironment.dist == Dist.CLIENT`.
  Payload handler uses `context.enqueueWork()` to hop to the client main thread.

## TODOs

- [ ] Implement full GUI rendering (HUD toast notifications, error screen)
- [ ] Wire `NeoForgeDatapackReloader.setLastChangedDatapackName()` from the watcher
- [ ] Add `/datahotload reload` command for manual reload triggering
- [ ] Verify `event.playToClient()` API signature matches NeoForge 21.1.x
- [ ] Verify `server.reloadResources()` method signature against exact NeoForge version
