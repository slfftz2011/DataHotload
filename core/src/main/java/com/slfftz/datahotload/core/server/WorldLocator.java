package com.slfftz.datahotload.core.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Helper to resolve the actual world directory for different server runtimes.
 *
 * Preference order:
 * 1) runDir/saves/<dir-containing-level.dat> (integrated singleplayer typical)
 * 2) runDir/world (dedicated server typical)
 * 3) runDir (fallback)
 */
public final class WorldLocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorldLocator.class);

    private WorldLocator() {}

    public static Path resolveWorldDir(Path runDir) {
        if (runDir == null) {
            return null;
        }

        Path saves = runDir.resolve("saves");
        if (Files.isDirectory(saves)) {
            try (DirectoryStream<Path> ds = Files.newDirectoryStream(saves)) {
                for (Path candidate : ds) {
                    if (Files.isDirectory(candidate) && Files.exists(candidate.resolve("level.dat"))) {
                        LOGGER.info("[DataHotload] Resolved world dir from saves: " + candidate);
                        return candidate;
                    }
                }
            } catch (IOException e) {
                LOGGER.error("[DataHotload] Error scanning saves directory: " + e.getMessage(), e);
            }
        }

        Path world = runDir.resolve("world");
        if (Files.isDirectory(world)) {
            LOGGER.info("[DataHotload] Resolved world dir: " + world);
            return world;
        }

        LOGGER.info("[DataHotload] Falling back to run directory as world dir: " + runDir);
        return runDir;
    }
}
