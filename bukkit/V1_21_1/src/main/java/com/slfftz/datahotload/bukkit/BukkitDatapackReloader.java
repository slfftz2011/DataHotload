package com.slfftz.datahotload.bukkit;

import com.slfftz.datahotload.core.server.DatapackReloader;
import com.slfftz.datahotload.core.server.ReloadResult;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

/**
 * Bukkit/Paper implementation of {@link DatapackReloader}.
 * <p>
 * Reload strategy, in order:
 * <ol>
 *   <li>Try the Bukkit/Paper static API {@code Bukkit.reloadDataPack()}
 *       (available since Bukkit 1.20.5 / Paper 1.20.5). This is the preferred
 *       path on a Paper 1.21.1 server.</li>
 *   <li>If that method does not exist (e.g. vanilla Spigot without the
 *       Paper patch), fall back to NMS reflection: resolve the
 *       {@code MinecraftServer} instance via {@code CraftServer#getServer()}
 *       and invoke {@code reloadResources(...)}.</li>
 * </ol>
 * All exceptions are caught and wrapped into {@link ReloadResult#failure}.
 */
public class BukkitDatapackReloader implements DatapackReloader {

    private final JavaPlugin plugin;

    /** Name of the datapack that most recently changed (updated by the main class via command/watcher context). */
    private volatile String lastChangedDatapackName = "unknown";

    public BukkitDatapackReloader(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void setLastChangedDatapackName(String name) {
        this.lastChangedDatapackName = (name != null && !name.isBlank()) ? name : "unknown";
    }

    @Override
    public ReloadResult reload() {
        long start = System.currentTimeMillis();
        String name = lastChangedDatapackName;
        try {
            tryBukkitApiReload();
            return ReloadResult.success(name, System.currentTimeMillis() - start);
        } catch (BukkitApiUnavailableException unavailable) {
            // Primary API missing -> try NMS reflection fallback
            try {
                tryNmsReload();
                return ReloadResult.success(name, System.currentTimeMillis() - start);
            } catch (Throwable t2) {
                plugin.getLogger().severe("[DataHotload] NMS reload fallback also failed: " + t2.getMessage());
                return ReloadResult.failure(name, t2, System.currentTimeMillis() - start);
            }
        } catch (Throwable t) {
            // reloadDataPack() threw -> reload genuinely failed
            plugin.getLogger().severe("[DataHotload] Datapack reload failed: " + t.getMessage());
            return ReloadResult.failure(name, t, System.currentTimeMillis() - start);
        }
    }

    /**
     * Invoke {@code Bukkit.reloadDataPack()} via reflection so this class
     * compiles even against API versions that do not declare the method.
     *
     * @throws BukkitApiUnavailableException if the static method is absent
     * @throws ReflectiveOperationException    if invocation fails
     */
    private void tryBukkitApiReload() throws ReflectiveOperationException, BukkitApiUnavailableException {
        Method m;
        try {
            m = Bukkit.class.getMethod("reloadDataPack");
        } catch (NoSuchMethodException e) {
            throw new BukkitApiUnavailableException(e);
        }
        m.invoke(null);
    }

    /**
     * Fallback: reflectively call {@code MinecraftServer#reloadResources(...)}.
     * <p>
     * NOTE: NMS internals changed across 1.20.5+ (Mojang-mapped/modern mappings).
     * This fallback is best-effort; on a genuine Paper 1.21.1 server the
     * {@link #tryBukkitApiReload()} path should always succeed, so this code
     * is only exercised on Spigot forks.
     * <p>
     * TODO: Verify the exact {@code reloadResources} signature against the
     * runtime version. On 1.21.1 the expected shape is:
     * <pre>
     *   CompletableFuture&lt;ReloadableServerResources&gt; reloadResources(
     *       Collection&lt;PackSelectionConfig&gt;, Collection&lt;PackSelectionConfig&gt;,
     *       Executor, Executor)
     * </pre>
     */
    private void tryNmsReload() throws Exception {
        Object craftServer = Bukkit.getServer();
        Method getServer = craftServer.getClass().getMethod("getServer");
        Object minecraftServer = getServer.invoke(craftServer);

        Method reload = minecraftServer.getClass().getMethod(
                "reloadResources",
                Collection.class, Collection.class,
                java.util.concurrent.Executor.class,
                java.util.concurrent.Executor.class);

        @SuppressWarnings("unchecked")
        CompletableFuture<Object> future = (CompletableFuture<Object>) reload.invoke(
                minecraftServer,
                Collections.emptyList(), Collections.emptyList(),
                (java.util.concurrent.Executor) CompletableFuture::runAsync,
                minecraftServer);

        // Block until the (async) reload completes so timing/error reporting
        // in ReloadResult is accurate.
        future.join();
    }

    @Override
    public String getLastChangedDatapackName() {
        return lastChangedDatapackName;
    }

    /** Thrown internally when the Bukkit static reload API is not present on this server. */
    private static final class BukkitApiUnavailableException extends Exception {
        BukkitApiUnavailableException(Throwable cause) {
            super(cause);
        }
    }
}
