package com.slfftz.datahotload.fabric;

import com.slfftz.datahotload.fabric.network.DataHotloadPayloadS2C;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

/**
 * Main Fabric mod entry point (runs on both client and server).
 * <p>
 * Registers the S2C payload type codec with the Fabric networking system.
 * Client-side and server-side logic are handled in their respective entry points.
 */
public class DataHotloadFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        // Register the S2C payload type so the client can decode incoming packets.
        // The client receiver is registered separately in DataHotloadFabricClient.
        PayloadTypeRegistry.playS2C().register(DataHotloadPayloadS2C.ID, DataHotloadPayloadS2C.CODEC);

        System.out.println("[DataHotload] Fabric main entry point initialized");
    }
}
