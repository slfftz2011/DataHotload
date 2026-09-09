package com.slfftz.datahotload.core.common.api;

import com.slfftz.datahotload.core.common.network.DataHotloadPayload;

/**
 * Extension point for error analysis and translation.
 * <p>
 * TODO: Not yet implemented. This interface defines the contract for
 * future error analysis features:
 * <ul>
 *   <li>Parse stack traces to identify root cause</li>
 *   <li>Translate technical errors to user-friendly messages</li>
 *   <li>Support i18n for translated messages</li>
 *   <li>Suggest fixes for common datapack errors</li>
 * </ul>
 */
public interface ErrorAnalyzer {

    /**
     * Analyze a payload and produce a human-readable summary.
     *
     * @param payload the error payload to analyze
     * @return an analyzed result with translated message and suggestions
     */
    AnalysisResult analyze(DataHotloadPayload payload);

    /**
     * Result of error analysis.
     */
    record AnalysisResult(
            String translatedMessage,
            String rootCause,
            String suggestedFix,
            String languageCode
    ) {}

    /**
     * Default no-op implementation.
     */
    ErrorAnalyzer NOOP = payload -> new AnalysisResult(
            payload.getErrorMessage(),
            payload.getErrorMessage(),
            "",
            "en"
    );
}
