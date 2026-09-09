package com.slfftz.datahotload.core.common.api;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;

/**
 * Extension point for client-side GUI rendering of error information.
 * <p>
 * TODO: Not yet implemented. This interface defines the contract for
 * future client GUI features:
 * <ul>
 *   <li>In-game HUD notification when a datapack error occurs</li>
 *   <li>Detailed error screen with stack trace viewer</li>
 *   <li>Copy-to-clipboard functionality</li>
 *   <li>Error history panel</li>
 * </ul>
 *
 * @param <S> the loader-specific screen / gui type
 */
public interface GuiRenderer<S> {

    /**
     * Show a brief notification (e.g., toast / action bar) for an error.
     */
    void showNotification(DataHotloadPayload payload);

    /**
     * Open a detailed error screen showing full stack trace and analysis.
     */
    void openErrorScreen(DataHotloadPayload payload);

    /**
     * Called when a payload is received on the client.
     * Implementations decide whether to show a notification, open a screen, or log.
     */
    void onPayloadReceived(DataHotloadPayload payload);
}
