package brettdansmith.drugdiary.model.interaction;

import org.json.JSONArray;

public final class InteractionCheckResult {
    public final String first;
    public final String second;
    public final String severity;
    public final String mechanism;
    public final String localGuidance;
    public final String uncertainty;
    public final JSONArray sources;
    public final long timestamp;

    public InteractionCheckResult(String first, String second, String severity, String mechanism, String localGuidance, String uncertainty, JSONArray sources, long timestamp) {
        this.first = first == null ? "" : first;
        this.second = second == null ? "" : second;
        this.severity = severity == null ? "UNKNOWN" : severity;
        this.mechanism = mechanism == null ? "" : mechanism;
        this.localGuidance = localGuidance == null ? "" : localGuidance;
        this.uncertainty = uncertainty == null ? "" : uncertainty;
        this.sources = sources == null ? new JSONArray() : sources;
        this.timestamp = timestamp <= 0 ? System.currentTimeMillis() : timestamp;
    }
}

