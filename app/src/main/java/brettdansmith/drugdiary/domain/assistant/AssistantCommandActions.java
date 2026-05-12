package brettdansmith.drugdiary.domain.assistant;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.util.JsonUtils;

final class AssistantCommandActions {
    private AssistantCommandActions() {}

    static boolean addMedication(Context context, String name, String group, String notes) {
        try {
            JSONObject data = EncryptedProfileStore.loadProfileData(context);
            JSONObject trackers = JsonUtils.object(data, ProfileJson.KEY_TRACKERS);
            JSONArray medications = JsonUtils.array(trackers, ProfileJson.KEY_MEDICATIONS);
            JSONObject medication = new JSONObject();
            medication.put("name", name);
            medication.put("group", group == null || group.isEmpty() ? "Saved" : group);
            medication.put("notes", notes == null ? "" : notes);
            medication.put("created_by", "assistant_command");
            medication.put("created_at", System.currentTimeMillis());
            medications.put(medication);
            EncryptedProfileStore.saveProfileData(context, data);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    static boolean addReminder(Context context, String title, String notes) {
        try {
            JSONObject data = EncryptedProfileStore.loadProfileData(context);
            JSONArray reminders = JsonUtils.array(data, "reminders");
            JSONObject reminder = new JSONObject();
            reminder.put("title", title);
            reminder.put("notes", notes == null ? "" : notes);
            reminder.put("created_by", "assistant_command");
            reminder.put("created_at", System.currentTimeMillis());
            reminder.put("enabled", true);
            reminders.put(reminder);
            EncryptedProfileStore.saveProfileData(context, data);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
