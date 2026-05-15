package brettdansmith.drugdiary.domain.model.diary;

import org.json.JSONException;
import org.json.JSONObject;

public final class SleepLog {
    public final double hours;
    public final int quality;

    public SleepLog(double hours, int quality) {
        this.hours = Math.max(0, hours);
        this.quality = Math.max(0, Math.min(10, quality));
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("hours", hours)
                .put("quality", quality);
    }

    public static SleepLog fromJson(JSONObject json) {
        if (json == null) return new SleepLog(0, 0);
        return new SleepLog(json.optDouble("hours", 0), json.optInt("quality", 0));
    }
}
