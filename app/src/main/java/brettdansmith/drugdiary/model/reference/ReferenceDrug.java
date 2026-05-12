package brettdansmith.drugdiary.model.reference;

import org.json.JSONObject;

public final class ReferenceDrug {
    public final String query;
    public final JSONObject payload;
    public final long cachedAt;

    public ReferenceDrug(String query, JSONObject payload, long cachedAt) {
        this.query = query == null ? "" : query;
        this.payload = payload == null ? new JSONObject() : payload;
        this.cachedAt = Math.max(0, cachedAt);
    }
}

