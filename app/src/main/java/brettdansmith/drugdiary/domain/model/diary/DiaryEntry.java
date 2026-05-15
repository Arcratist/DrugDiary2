package brettdansmith.drugdiary.domain.model.diary;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class DiaryEntry {
    public final String id;
    public final String title;
    public final String notes;
    public final MoodCheckIn mood;
    public final SleepLog sleep;
    public final SymptomLog symptom;
    public final String linkedMedicationId;
    public final JSONArray tags;
    public final long createdAt;

    public DiaryEntry(String id, String title, String notes, MoodCheckIn mood, SleepLog sleep, SymptomLog symptom, String linkedMedicationId, JSONArray tags, long createdAt) {
        this.id = id == null || id.trim().isEmpty() ? "diary_" + System.currentTimeMillis() : id.trim();
        this.title = title == null ? "" : title.trim();
        this.notes = notes == null ? "" : notes.trim();
        this.mood = mood == null ? new MoodCheckIn(0, 0, 0, 0) : mood;
        this.sleep = sleep == null ? new SleepLog(0, 0) : sleep;
        this.symptom = symptom == null ? new SymptomLog("", 0) : symptom;
        this.linkedMedicationId = linkedMedicationId == null ? "" : linkedMedicationId.trim();
        this.tags = tags == null ? new JSONArray() : tags;
        this.createdAt = createdAt <= 0 ? System.currentTimeMillis() : createdAt;
    }

    public JSONObject toJson() throws JSONException {
        return new JSONObject()
                .put("id", id)
                .put("title", title)
                .put("notes", notes)
                .put("mood", mood.toJson())
                .put("sleep", sleep.toJson())
                .put("symptom", symptom.toJson())
                .put("linked_medication_id", linkedMedicationId)
                .put("tags", tags)
                .put("created_at", createdAt);
    }

    public static DiaryEntry fromJson(JSONObject json) {
        if (json == null) {
            return new DiaryEntry("", "", "", null, null, null, "", null, 0);
        }
        return new DiaryEntry(
                json.optString("id", ""),
                json.optString("title", ""),
                json.optString("notes", ""),
                MoodCheckIn.fromJson(json.optJSONObject("mood")),
                SleepLog.fromJson(json.optJSONObject("sleep")),
                SymptomLog.fromJson(json.optJSONObject("symptom")),
                json.optString("linked_medication_id", ""),
                json.optJSONArray("tags"),
                json.optLong("created_at", 0));
    }
}
