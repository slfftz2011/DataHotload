# Architecture Design

## Overview

DataHotload uses a **client-server separated, loader-agnostic core** architecture. The `core` module contains all business logic independent of any mod loader, while each loader module provides thin adapter layers that bridge core APIs to loader-specific mechanisms.

## Module Dependency Graph

```
                    ┌─────────────┐
                    │    core     │  (loader-agnostic)
                    │  (Java lib) │
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌────────────┐  ┌────────────┐  ┌────────────┐
    │   Fabric   │  │  NeoForge  │  │   Bukkit   │
    │  V1_21_1   │  │  V1_21_1   │  │  V1_21_1   │
    └────────────┘  └────────────┘  └────────────┘
```

## Core Module Design

### Package Structure

```
com.slfftz.datahotload.core
├── common/
│   ├── DataHotloadConstants.java    # MOD_ID, channel names, version
│   ├── network/
│   │   ├── DataHotloadPayload.java  # Serializable error payload
│   │   └── NetworkHandler.java      # Interface for sending payloads
│   └── api/
│       ├── ErrorAnalyzer.java       # [TODO] Error analysis & translation
│       └── GuiRenderer.java         # [TODO] Client GUI rendering
├── client/
│   └── ClientEntryPoint.java        # Client-side lifecycle interface
└── server/
    ├── DatapackWatcher.java         # WatchService-based file monitor
    ├── DatapackReloader.java        # Interface for reload triggering
    └── ServerEntryPoint.java        # Server-side lifecycle interface
```

### Key Components

#### 1. DatapackWatcher
- Uses `java.nio.file.WatchService` to monitor `world/datapacks` directory
- Detects CREATE, MODIFY, DELETE events on `.zip` files and directories
- Debounces rapid changes (500ms cooldown) to avoid reload spamming
- Notifies `ServerEntryPoint` when a reload should be triggered

#### 2. DatapackReloader (Interface)
- Loader-specific implementations call the vanilla reload API:
  - Fabric/NeoForge: `MinecraftServer#reloadResources(...)` 
  - Bukkit/Paper: `Bukkit.reloadDataPack()` or Paper's `Server#reloadDataPacks()`
- Returns a `ReloadResult` containing success status and any errors

#### 3. DataHotloadPayload
- Contains: error message, stack trace, datapack name, timestamp, severity
- Serialized as a simple byte buffer (length-prefixed strings) for cross-loader compatibility
- Channel ID: `slfftz:datahotload`

#### 4. NetworkHandler (Interface)
- `sendToPlayer(player, payload)` - send to specific player
- `sendToAllPlayers(payload)` - broadcast to all connected players
- Loader-specific implementations handle the actual network transport

## Loader Adapter Pattern

Each loader module implements three adapter layers:

1. **Main Class** - Loader entry point (ModInitializer / @Mod / JavaPlugin)
2. **Network Adapter** - Implements `NetworkHandler` using loader's networking API
3. **Reload Adapter** - Implements `DatapackReloader` using loader's server API

### Fabric Implementation
- `DataHotloadFabric` implements `ModInitializer` (common)
- `DataHotloadFabricClient` implements `ClientModInitializer`
- `DataHotloadFabricServer` implements `DedicatedServerModInitializer`
- Networking: `ServerPlayNetworking` + `PayloadTypeRegistry` (1.20.2+ style)
- Reload: `MinecraftServer#reloadResources(Collection, Collection, Executor, Executor)`

### NeoForge Implementation
- `DataHotloadNeoForge` with `@Mod` annotation
- Client setup via `RegisterClientExtensionsEvent` or `FMLClientSetupEvent`
- Networking: `RegisterPayloadHandlersEvent` + `CustomPayload`
- Reload: `MinecraftServer#reloadResources(...)` via `ServerLifecycleHooks`

### Bukkit Implementation
- `DataHotloadBukkit` extends `JavaPlugin`
- Networking: Plugin Messaging Channel (`slfftz:datahotload`) via `Messenger`
- Reload: Paper API `Server#reloadDataPacks()` (with fallback to reflection for Spigot)
- Note: Bukkit clients need the corresponding mod installed to receive payloads

## Data Flow

```
File Change → DatapackWatcher → ServerEntryPoint → DatapackReloader
                                                        │
                                                        ▼
                                                 ReloadResult (error?)
                                                        │
                                           ┌────────────┴────────────┐
                                           ▼                         ▼
                                      Success                  Error captured
                                           │                         │
                                      (log only)          NetworkHandler.sendToAll
                                                               │
                                                               ▼
                                                      DataHotloadPayload
                                                               │
                    ┌──────────────────────────────────────────┼──────────────────┐
                    ▼                                          ▼                  ▼
             Fabric Client                              NeoForge Client    Bukkit Client
          (GUI TODO: render)                          (GUI TODO: render)  (mod required)
```

## Future Extension Points

### Error Analysis & Translation (`api/ErrorAnalyzer`)
- Parse stack traces to identify root cause
- Translate technical errors to user-friendly messages
- Support i18n

### Client GUI (`api/GuiRenderer`)
- In-game HUD notification
- Detailed error screen with stack trace viewer
- Copy-to-clipboard functionality

### Forge Support
- Directory structure already in place (`forge/V1_21_1/`)
- Will use `@Mod` + `SimpleChannel` / `EventNetty` networking
- Target: Forge 1.21.1 (or latest compatible)
