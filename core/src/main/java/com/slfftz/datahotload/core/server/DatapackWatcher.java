package com.slfftz.datahotload.core.server;

import com.slfftz.datahotload.core.common.DataHotloadConstants;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

/**
 * Watches a world's {@code datapacks} directory for file changes using
 * {@link java.nio.file.WatchService}.
 * <p>
 * Detects CREATE, MODIFY, and DELETE events on {@code .zip} files and
 * subdirectories. Events are debounced to avoid triggering multiple reloads
 * during rapid file operations (e.g., extracting a zip).
 */
public class DatapackWatcher implements AutoCloseable {

    private final Path datapacksDir;
    private final Consumer<String> changeCallback;
    private WatchService watchService;
    private Thread watchThread;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastEventTime = new AtomicLong(0);
    private volatile String lastChangedName = "unknown";

    // Map watch keys back to the registered directory path so we can resolve events
    private final Map<WatchKey, Path> keyToDir = new ConcurrentHashMap<>();

    /**
     * @param datapacksDir   the path to the world's datapacks directory
     * @param changeCallback called with the changed datapack name when a debounced change occurs
     */
    public DatapackWatcher(Path datapacksDir, Consumer<String> changeCallback) {
        this.datapacksDir = datapacksDir;
        this.changeCallback = changeCallback;
    }

    /**
     * Start watching the directory. Creates the directory if it does not exist.
     */
    public void start() throws IOException {
        if (!Files.exists(datapacksDir)) {
            Files.createDirectories(datapacksDir);
        }

        this.watchService = FileSystems.getDefault().newWatchService();

        // Register the datapacks dir and all existing subdirectories so we observe
        // changes inside unpacked datapacks as well.
        registerAll(datapacksDir);

        running.set(true);
        watchThread = new Thread(this::watchLoop, "datahotload-watcher");
        watchThread.setDaemon(true);
        watchThread.start();

        System.out.println("[DataHotload] Watching datapacks directory: " + datapacksDir);
    }

    private void registerAll(final Path start) throws IOException {
        Files.walkFileTree(start, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                registerDir(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private void registerDir(Path dir) throws IOException {
        if (watchService == null) return;
        try {
            WatchKey key = dir.register(watchService,
                    StandardWatchEventKinds.ENTRY_CREATE,
                    StandardWatchEventKinds.ENTRY_MODIFY,
                    StandardWatchEventKinds.ENTRY_DELETE);
            keyToDir.put(key, dir);
        } catch (IOException e) {
            // bubble up
            throw e;
        }
    }

    private void watchLoop() {
        while (running.get()) {
            WatchKey key;
            try {
                key = watchService.take();
            } catch (InterruptedException | ClosedWatchServiceException e) {
                Thread.currentThread().interrupt();
                break;
            }

            Path dir = keyToDir.get(key);
            if (dir == null) {
                // Unknown key, skip
                key.reset();
                continue;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path changedRel = pathEvent.context();
                Path changed = dir.resolve(changedRel);
                String name = changed.getFileName().toString();

                // Only react to .zip files and directories (datapacks)
                boolean isZip = name.endsWith(".zip");
                boolean isDir = Files.isDirectory(changed);
                if (!isZip && !isDir && kind != StandardWatchEventKinds.ENTRY_DELETE) {
                    continue;
                }

                // If a new directory was created, register it so we watch its children
                if (kind == StandardWatchEventKinds.ENTRY_CREATE && isDir) {
                    try {
                        registerAll(changed);
                    } catch (IOException ignored) {
                        // non-fatal: continue watching other events
                    }
                }

                lastChangedName = name;
                lastEventTime.set(System.currentTimeMillis());

                // Schedule debounced reload
                scheduleDebouncedReload(name);
            }

            boolean valid = key.reset();
            if (!valid) {
                keyToDir.remove(key);
                // If no more keys, possibly exit
                if (keyToDir.isEmpty()) break;
            }
        }
    }

    private void scheduleDebouncedReload(String name) {
        // Use a separate short-lived thread for debounce
        new Thread(() -> {
            try {
                Thread.sleep(DataHotloadConstants.RELOAD_DEBOUNCE_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            // Only fire if no newer event occurred during the wait
            long now = System.currentTimeMillis();
            if (now - lastEventTime.get() >= DataHotloadConstants.RELOAD_DEBOUNCE_MS - 50) {
                changeCallback.accept(name);
            }
        }, "datahotload-debounce").start();
    }

    /**
     * Stop watching and release resources.
     */
    @Override
    public void close() {
        running.set(false);
        if (watchService != null) {
            try {
                watchService.close();
            } catch (IOException ignored) {}
        }
        if (watchThread != null) {
            watchThread.interrupt();
        }
        keyToDir.clear();
    }
}
