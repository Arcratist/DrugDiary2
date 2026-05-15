package brettdansmith.drugdiary.domain.model.medication;

import org.json.JSONException;
import org.json.JSONObject;

public final class MedicationSchedule {
    public final String type;
    public final String pattern;
    public final long nextDoseAt;

    public MedicationSchedule(String type, String pattern, long nextDoseAt) {
        this.type = type == null || type.trim().isEmpty() ? "scheduled" : type.trim();
        this.pattern = pattern == null ? "" : pattern.trim();
        this.nextDoseAt = Math.max(0, nextDoseAt);
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("type", type)
                .put("pattern", pattern)
                .put("next_dose_at", nextDoseAt);
    }

    public static MedicationSchedule fromJson(JSONObject json) {
        if (json == null) return new MedicationSchedule("scheduled", "", 0);
        return new MedicationSchedule(
                json.optString("type", "scheduled"),
                json.optString("pattern", ""),
                json.optLong("next_dose_at", 0));
    }
}
