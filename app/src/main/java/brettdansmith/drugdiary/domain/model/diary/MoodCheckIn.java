package brettdansmith.drugdiary.domain.model.diary;

import org.json.JSONException;
import org.json.JSONObject;

public final class MoodCheckIn {
    public final int mood;
    public final int anxiety;
    public final int stress;
    public final int energy;

    public MoodCheckIn(int mood, int anxiety, int stress, int energy) {
        this.mood = clamp(mood);
        this.anxiety = clamp(anxiety);
        this.stress = clamp(stress);
        this.energy = clamp(energy);
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("mood", mood)
                .put("anxiety", anxiety)
                .put("stress", stress)
                .put("energy", energy);
    }

    public static MoodCheckIn fromJson(JSONObject json) {
        if (json == null) return new MoodCheckIn(0, 0, 0, 0);
        return new MoodCheckIn(
                json.optInt("mood", 0),
                json.optInt("anxiety", 0),
                json.optInt("stress", 0),
                json.optInt("energy", 0));
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(10, value));
    }
}
