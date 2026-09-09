package com.slfftz.datahotload.neoforge.client;

import com.slfftz.datahotload.core.client.ClientEntryPoint;
import com.slfftz.datahotload.core.common.api.ErrorAnalyzer;
import com.slfftz.datahotload.core.common.api.GuiRenderer;
import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import com.slfftz.datahotload.neoforge.network.NeoForgePayload;

/**
 * Client-side entry point for DataHotload on NeoForge.
 * <p>
 * Instantiated on physical client only. Receives
 * {@link NeoForgePayload}s from the server and forwards them to the
 * loader-agnostic {@link ClientEntryPoint}.
 * <p>
 * Currently uses the NOOP {@link ErrorAnalyzer} and a basic logging
 * {@link GuiRenderer} — TODO: implement full HUD notifications and
 * error screen as described in the core API.
 */
public class NeoForgeClient {

    private static NeoForgeClient INSTANCE;

    private final ClientEntryPoint clientEntryPoint;

    private NeoForgeClient() {
        // TODO: Replace with a real GuiRenderer implementation (HUD toast / error screen)
        GuiRenderer<?> guiRenderer = new GuiRenderer<Object>() {
            @Override
            public void showNotification(DataHotloadPayload payload) {
                System.out.println("[DataHotload-Client] Notification: " + payload.getErrorMessage());
            }

            @Override
            public void openErrorScreen(DataHotloadPayload payload) {
                System.out.println("[DataHotload-Client] Error screen (TODO): " + payload.getErrorMessage());
            }

            @Override
            public void onPayloadReceived(DataHotloadPayload payload) {
                showNotification(payload);
            }
        };
        this.clientEntryPoint = new ClientEntryPoint(guiRenderer, ErrorAnalyzer.NOOP);
    }

    /**
     * Get or create the singleton client instance.
     * Should only be called on the physical client.
     */
    public static synchronized NeoForgeClient getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new NeoForgeClient();
        }
        return INSTANCE;
    }

    /**
     * Called by the network handler when a {@link NeoForgePayload}
     * arrives from the server on the client side.
     *
     * @param wrapped the received payload wrapper
     */
    public void onPayloadReceived(NeoForgePayload wrapped) {
        DataHotloadPayload inner = wrapped.inner();
        clientEntryPoint.onPayloadReceived(inner);
    }

    public ClientEntryPoint getClientEntryPoint() {
        return clientEntryPoint;
    }
}
