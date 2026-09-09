package com.slfftz.datahotload.core.server;

import java.util.List;

/**
 * Result of a datapack reload operation.
 */
public class ReloadResult {

    private final boolean success;
    private final String datapackName;
    private final String errorMessage;
    private final List<String> stackTrace;
    private final long durationMs;

    public ReloadResult(boolean success, String datapackName, String errorMessage,
                        List<String> stackTrace, long durationMs) {
        this.success = success;
        this.datapackName = datapackName;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace;
        this.durationMs = durationMs;
    }

    public static ReloadResult success(String datapackName, long durationMs) {
        return new ReloadResult(true, datapackName, null, null, durationMs);
    }

    public static ReloadResult failure(String datapackName, String errorMessage,
                                       List<String> stackTrace, long durationMs) {
        return new ReloadResult(false, datapackName, errorMessage, stackTrace, durationMs);
    }

    public static ReloadResult failure(String datapackName, Throwable throwable, long durationMs) {
        List<String> trace = new java.util.ArrayList<>();
        for (StackTraceElement el : throwable.getStackTrace()) {
            trace.add(el.toString());
        }
        String message = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
        return new ReloadResult(false, datapackName, message, trace, durationMs);
    }

    public boolean isSuccess() { return success; }
    public String getDatapackName() { return datapackName; }
    public String getErrorMessage() { return errorMessage; }
    public List<String> getStackTrace() { return stackTrace; }
    public long getDurationMs() { return durationMs; }
}
