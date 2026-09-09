package com.slfftz.datahotload.core.common.network;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Serializable payload carrying datapack reload error information.
 * <p>
 * Wire format (all strings length-prefixed as big-endian int + UTF-8 bytes):
 * <pre>
 *   int  protocolVersion
 *   long timestamp (epoch millis)
 *   int  severity ordinal
 *   String datapackName
 *   String errorMessage
 *   int  stackTraceLineCount
 *   String[] stackTraceLines
 * </pre>
 */
public class DataHotloadPayload {

    public enum Severity {
        INFO, WARNING, ERROR, FATAL
    }

    private final int protocolVersion;
    private final long timestamp;
    private final Severity severity;
    private final String datapackName;
    private final String errorMessage;
    private final List<String> stackTrace;

    public DataHotloadPayload(Severity severity, String datapackName, String errorMessage, List<String> stackTrace) {
        this.protocolVersion = 1;
        this.timestamp = System.currentTimeMillis();
        this.severity = severity;
        this.datapackName = datapackName != null ? datapackName : "unknown";
        this.errorMessage = errorMessage != null ? errorMessage : "";
        this.stackTrace = stackTrace != null ? new ArrayList<>(stackTrace) : new ArrayList<>();
    }

    public DataHotloadPayload(int protocolVersion, long timestamp, Severity severity,
                              String datapackName, String errorMessage, List<String> stackTrace) {
        this.protocolVersion = protocolVersion;
        this.timestamp = timestamp;
        this.severity = severity;
        this.datapackName = datapackName;
        this.errorMessage = errorMessage;
        this.stackTrace = stackTrace != null ? new ArrayList<>(stackTrace) : new ArrayList<>();
    }

    // ---- Factory helpers ----

    public static DataHotloadPayload fromException(String datapackName, Throwable throwable) {
        List<String> trace = new ArrayList<>();
        for (StackTraceElement el : throwable.getStackTrace()) {
            trace.add(el.toString());
        }
        String message = throwable.getMessage() != null ? throwable.getMessage() : throwable.getClass().getSimpleName();
        return new DataHotloadPayload(Severity.ERROR, datapackName, message, trace);
    }

    // ---- Serialization ----

    public byte[] encode() {
        List<byte[]> parts = new ArrayList<>();
        int total = 0;

        // protocolVersion (int)
        byte[] pv = intToBytes(protocolVersion);
        parts.add(pv); total += pv.length;

        // timestamp (long)
        byte[] ts = longToBytes(timestamp);
        parts.add(ts); total += ts.length;

        // severity (int)
        byte[] sv = intToBytes(severity.ordinal());
        parts.add(sv); total += sv.length;

        // datapackName
        byte[] dn = encodeString(datapackName);
        parts.add(dn); total += dn.length;

        // errorMessage
        byte[] em = encodeString(errorMessage);
        parts.add(em); total += em.length;

        // stackTrace count + lines
        byte[] sc = intToBytes(stackTrace.size());
        parts.add(sc); total += sc.length;
        for (String line : stackTrace) {
            byte[] ln = encodeString(line);
            parts.add(ln); total += ln.length;
        }

        byte[] result = new byte[total];
        int offset = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, result, offset, part.length);
            offset += part.length;
        }
        return result;
    }

    public static DataHotloadPayload decode(byte[] data) {
        int offset = 0;

        int protocolVersion = bytesToInt(data, offset); offset += 4;
        long timestamp = bytesToLong(data, offset); offset += 8;
        int severityOrdinal = bytesToInt(data, offset); offset += 4;
        Severity severity = Severity.values()[severityOrdinal];
        String datapackName = decodeString(data, offset); offset += 4 + datapackName.getBytes(StandardCharsets.UTF_8).length;
        String errorMessage = decodeString(data, offset); offset += 4 + errorMessage.getBytes(StandardCharsets.UTF_8).length;
        int traceCount = bytesToInt(data, offset); offset += 4;
        List<String> stackTrace = new ArrayList<>(traceCount);
        for (int i = 0; i < traceCount; i++) {
            String line = decodeString(data, offset);
            offset += 4 + line.getBytes(StandardCharsets.UTF_8).length;
            stackTrace.add(line);
        }

        return new DataHotloadPayload(protocolVersion, timestamp, severity, datapackName, errorMessage, stackTrace);
    }

    // ---- Getters ----

    public int getProtocolVersion() { return protocolVersion; }
    public long getTimestamp() { return timestamp; }
    public Severity getSeverity() { return severity; }
    public String getDatapackName() { return datapackName; }
    public String getErrorMessage() { return errorMessage; }
    public List<String> getStackTrace() { return new ArrayList<>(stackTrace); }

    @Override
    public String toString() {
        return "DataHotloadPayload{" +
                "severity=" + severity +
                ", datapack='" + datapackName + '\'' +
                ", message='" + errorMessage + '\'' +
                ", traceLines=" + stackTrace.size() +
                '}';
    }

    // ---- Utility ----

    private static byte[] encodeString(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[4 + bytes.length];
        System.arraycopy(intToBytes(bytes.length), 0, result, 0, 4);
        System.arraycopy(bytes, 0, result, 4, bytes.length);
        return result;
    }

    private static String decodeString(byte[] data, int offset) {
        int len = bytesToInt(data, offset);
        return new String(data, offset + 4, len, StandardCharsets.UTF_8);
    }

    private static byte[] intToBytes(int v) {
        return new byte[] {
                (byte) (v >>> 24), (byte) (v >>> 16), (byte) (v >>> 8), (byte) v
        };
    }

    private static int bytesToInt(byte[] data, int offset) {
        return ((data[offset] & 0xFF) << 24) |
               ((data[offset + 1] & 0xFF) << 16) |
               ((data[offset + 2] & 0xFF) << 8) |
               (data[offset + 3] & 0xFF);
    }

    private static byte[] longToBytes(long v) {
        byte[] result = new byte[8];
        for (int i = 7; i >= 0; i--) {
            result[i] = (byte) (v & 0xFF);
            v >>>= 8;
        }
        return result;
    }

    private static long bytesToLong(byte[] data, int offset) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result = (result << 8) | (data[offset + i] & 0xFF);
        }
        return result;
    }
}
