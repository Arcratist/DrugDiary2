package brettdansmith.drugdiary.data.diary;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.model.diary.DiaryEntry;

public final class DiaryRepository {
    private final Context appContext;

    public DiaryRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public List<DiaryEntry> listEntries() {
        List<DiaryEntry> entries = new ArrayList<>();
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONObject trackers = data.optJSONObject(ProfileJson.KEY_TRACKERS);
        JSONArray array = trackers == null ? null : trackers.optJSONArray(ProfileJson.KEY_DIARY_ENTRIES);
        if (array == null) {
            array = trackers == null ? null : trackers.optJSONArray(ProfileJson.KEY_LOGS);
        }
        if (array == null) return entries;
        for (int i = 0; i < array.length(); i++) {
            DiaryEntry entry = DiaryEntry.fromJson(array.optJSONObject(i));
            if (entry.title.isEmpty() && entry.notes.isEmpty()) continue;
            entries.add(entry);
        }
        entries.sort((a, b) -> Long.compare(b.createdAt, a.createdAt));
        return entries;
    }

    public void addEntry(DiaryEntry entry) throws JSONException {
        JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
        JSONObject trackers = ProfileJson.object(data, ProfileJson.KEY_TRACKERS);
        JSONArray entries = ProfileJson.array(trackers, ProfileJson.KEY_DIARY_ENTRIES);
        entries.put(entry.toJson());
        // Keep legacy log summary in sync for screens still reading KEY_LOGS.
        JSONArray legacy = ProfileJson.array(trackers, ProfileJson.KEY_LOGS);
        legacy.put(new JSONObject()
                .put("type", "diary")
                .put("title", entry.title)
                .put("notes", entry.notes)
                .put("created_at", entry.createdAt));
        EncryptedProfileStore.saveProfileData(appContext, data);
    }
}

