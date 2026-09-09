# Forge Module (Placeholder)

This directory is reserved for future Forge support.

## Target
- Forge 1.21.1 (or latest compatible version)
- Uses `@Mod` annotation for main class
- Networking via `SimpleChannel` or `EventNetty` (depending on Forge version)
- Reload via `MinecraftServer#reloadResources(...)`

## Status
📋 Not yet implemented. Directory structure reserved for future expansion.

## Implementation Notes (when ready)
- Main class: `com.slfftz.datahotload.forge.DataHotloadForge`
- Use `@Mod("datahotload")` annotation
- Register payload via `RegisterPayloadHandlersEvent` (Forge 1.20.2+)
- Client setup via `FMLClientSetupEvent`
- Server tick event for watcher lifecycle management
