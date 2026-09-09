# DataHotload

A Minecraft multi-loader mod/plugin that automatically detects datapack changes and reloads them, with error reporting to clients.

## Features

- 🔄 **Automatic Datapack Reload**: Watches the `world/datapacks` directory for changes and triggers a vanilla-style reload
- 📡 **Error Reporting**: Sends detailed error information to connected clients via custom network payloads
- 🖥️ **Client-Server Separation**: Clean architecture with distinct common/client/server modules
- 🔌 **Multi-Loader Support**: Fabric, NeoForge, and Bukkit/Paper (Forge placeholder for future)

## Supported Loaders (Current)

| Loader   | Version | Status     |
|----------|---------|------------|
| Fabric   | 1.21.1  | ✅ Implemented |
| NeoForge | 1.21.1  | ✅ Implemented |
| Bukkit/Paper | 1.21 | ✅ Implemented |
| Forge    | 1.21.1  | 📋 Placeholder |

## Project Structure

```
DataHotload/
├── core/                    # Loader-agnostic core business logic
│   └── src/main/java/com/slfftz/datahotload/core/
│       ├── common/          # Shared code (both client & server)
│       │   ├── network/     # Network payload definitions
│       │   └── api/         # Extension interfaces (error analysis, GUI)
│       ├── client/          # Client-side interfaces
│       └── server/          # Server-side logic (datapack watcher, reloader)
├── fabric/V1_21_1/          # Fabric 1.21.1 implementation
├── neoforge/V1_21_1/        # NeoForge 1.21.1 implementation
├── bukkit/V1_21_1/          # Bukkit/Paper 1.21 implementation
├── forge/V1_21_1/           # Forge placeholder (future)
└── .github/workflows/       # CI/CD workflows
```

## Architecture

See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for detailed design.

## Building

Each loader module can be built independently:

```bash
# Build all
./gradlew build

# Build specific loader
./gradlew :fabric:V1_21_1:build
./gradlew :neoforge:V1_21_1:build
./gradlew :bukkit:V1_21_1:build
```

## Network Channel

All loaders use the channel namespace `slfftz:datahotload` for custom payloads.

## License

MIT
