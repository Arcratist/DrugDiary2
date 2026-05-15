package brettdansmith.drugdiary.domain.model.diary;

import org.json.JSONException;
import org.json.JSONObject;

public final class SymptomLog {
    public final String symptom;
    public final int severity;

    public SymptomLog(String symptom, int severity) {
        this.symptom = symptom == null ? "" : symptom.trim();
        this.severity = Math.max(0, Math.min(10, severity));
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("symptom", symptom)
                .put("severity", severity);
    }

    public static SymptomLog fromJson(JSONObject json) {
        if (json == null) return new SymptomLog("", 0);
        return new SymptomLog(json.optString("symptom", ""), json.optInt("severity", 0));
    }
}
