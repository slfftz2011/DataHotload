package com.slfftz.datahotload.core.server;

import com.slfftz.datahotload.core.common.DataHotloadConstants;

import java.io.IOException;
import java.nio.file.*;
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
        datapacksDir.register(watchService,
                StandardWatchEventKinds.ENTRY_CREATE,
                StandardWatchEventKinds.ENTRY_MODIFY,
                StandardWatchEventKinds.ENTRY_DELETE);

        running.set(true);
        watchThread = new Thread(this::watchLoop, "datahotload-watcher");
        watchThread.setDaemon(true);
        watchThread.start();

        System.out.println("[DataHotload] Watching datapacks directory: " + datapacksDir);
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

            for (WatchEvent<?> event : key.pollEvents()) {
                WatchEvent.Kind<?> kind = event.kind();
                if (kind == StandardWatchEventKinds.OVERFLOW) {
                    continue;
                }

                @SuppressWarnings("unchecked")
                WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                Path changed = pathEvent.context();
                String name = changed.getFileName().toString();

                // Only react to .zip files and directories (datapacks)
                boolean isZip = name.endsWith(".zip");
                boolean isDir = Files.isDirectory(datapacksDir.resolve(changed));
                if (!isZip && !isDir && kind != StandardWatchEventKinds.ENTRY_DELETE) {
                    continue;
                }

                lastChangedName = name;
                lastEventTime.set(System.currentTimeMillis());

                // Schedule debounced reload
                scheduleDebouncedReload(name);
            }

            boolean valid = key.reset();
            if (!valid) {
                break;
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
    }

    public String getLastChangedName() {
        return lastChangedName;
    }

    public boolean isRunning() {
        return running.get();
    }
}
