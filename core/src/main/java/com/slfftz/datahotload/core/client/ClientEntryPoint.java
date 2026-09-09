package com.slfftz.datahotload.core.client;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;
import com.slfftz.datahotload.core.common.api.GuiRenderer;
import com.slfftz.datahotload.core.common.api.ErrorAnalyzer;

/**
 * Client-side lifecycle manager.
 * <p>
 * Handles received payloads and delegates to the (currently TODO) GUI renderer
 * and error analyzer. Loader-specific client initializers create an instance
 * and call {@link #onPayloadReceived(DataHotloadPayload)} when a packet arrives.
 */
public class ClientEntryPoint {

    private final GuiRenderer<?> guiRenderer;
    private final ErrorAnalyzer errorAnalyzer;

    public ClientEntryPoint(GuiRenderer<?> guiRenderer, ErrorAnalyzer errorAnalyzer) {
        this.guiRenderer = guiRenderer;
        this.errorAnalyzer = errorAnalyzer != null ? errorAnalyzer : ErrorAnalyzer.NOOP;
    }

    /**
     * Called when a DataHotload payload is received from the server.
     */
    public void onPayloadReceived(DataHotloadPayload payload) {
        // TODO: Implement full GUI rendering
        // For now, log the received error
        System.out.println("[DataHotload] Received datapack error from server:");
        System.out.println("  Datapack: " + payload.getDatapackName());
        System.out.println("  Severity: " + payload.getSeverity());
        System.out.println("  Message:  " + payload.getErrorMessage());

        if (guiRenderer != null) {
            guiRenderer.onPayloadReceived(payload);
        }
    }

    public GuiRenderer<?> getGuiRenderer() {
        return guiRenderer;
    }

    public ErrorAnalyzer getErrorAnalyzer() {
        return errorAnalyzer;
    }
}
