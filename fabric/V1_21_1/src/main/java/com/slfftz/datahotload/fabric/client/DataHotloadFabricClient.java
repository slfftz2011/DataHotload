package com.slfftz.datahotload.fabric.client;

import com.slfftz.datahotload.core.client.ClientEntryPoint;
import com.slfftz.datahotload.core.common.api.ErrorAnalyzer;
import com.slfftz.datahotload.fabric.network.DataHotloadPayloadS2C;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side Fabric mod entry point.
 * <p>
 * Registers the global payload receiver for S2C packets and delegates
 * received data to the core {@link ClientEntryPoint}.
 * <p>
 * TODO: Implement a real {@link com.slfftz.datahotload.core.common.api.GuiRenderer}
 * to display error notifications and detailed screens in-game.
 * Currently GuiRenderer is null and errors are only logged to console.
 */
public class DataHotloadFabricClient implements ClientModInitializer {

    private static ClientEntryPoint clientEntryPoint;

    @Override
    public void onInitializeClient() {
        // TODO: Replace null with a concrete GuiRenderer implementation
        // (HUD toast notifications + detailed error screen with stack trace viewer)
        clientEntryPoint = new ClientEntryPoint(null, ErrorAnalyzer.NOOP);

        // Register the receiver for S2C payloads from the server
        ClientPlayNetworking.registerGlobalReceiver(
                DataHotloadPayloadS2C.ID,
                (payload, context) -> {
                    // Ensure we run on the client render thread
                    context.client().execute(() ->
                            clientEntryPoint.onPayloadReceived(payload.payload())
                    );
                }
        );

        System.out.println("[DataHotload] Fabric client entry point initialized");
    }

    /**
     * @return the client-side lifecycle manager (useful for GUI hooks)
     */
    public static ClientEntryPoint getClientEntryPoint() {
        return clientEntryPoint;
    }
}
