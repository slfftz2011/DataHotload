package com.slfftz.datahotload.core.common;

/**
 * Global constants for DataHotload.
 */
public final class DataHotloadConstants {

    private DataHotloadConstants() {}

    public static final String MOD_ID = "datahotload";
    public static final String MOD_NAME = "DataHotload";
    public static final String VERSION = "1.0.0";

    /** Network channel namespace used by all loaders. */
    public static final String CHANNEL_NAMESPACE = "slfftz";
    public static final String CHANNEL_PATH = "datahotload";
    public static final String CHANNEL_ID = CHANNEL_NAMESPACE + ":" + CHANNEL_PATH;

    /** Protocol version for payload serialization. */
    public static final int PROTOCOL_VERSION = 1;

    /** Debounce interval (ms) between datapack change detection and reload. */
    public static final long RELOAD_DEBOUNCE_MS = 500;

    /** Subdirectory name for datapacks within a world folder. */
    public static final String DATAPACKS_DIR = "datapacks";
}
