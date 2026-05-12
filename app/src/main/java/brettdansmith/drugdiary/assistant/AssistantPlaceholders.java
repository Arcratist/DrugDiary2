package brettdansmith.drugdiary.assistant;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsState;

public final class AssistantPlaceholders {
    private static final Pattern PLACEHOLDER = Pattern.compile("!@([a-zA-Z0-9_]+):([^\\s|,;]+)");

    private AssistantPlaceholders() {}

    public static String expand(Context context, String input) {
        if (input == null || !input.contains("!@")) return input;
        JSONObject data = EncryptedProfileStore.loadProfileData(context);
        return expand(context, data, input);
    }

    public static String expand(Context context, JSONObject data, String input) {
        if (input == null || !input.contains("!@")) return input;
        JSONObject safeData = data == null ? new JSONObject() : data;
        Matcher matcher = PLACEHOLDER.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            String type = matcher.group(1).toLowerCase(Locale.US);
            String key = matcher.group(2);
            String replacement = resolve(context, safeData, type, key);
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(output);
        return output.toString();
    }

    public static String helpText() {
        return "=== Placeholders ===\n"
                + "- **!@medications:<name>**: Freshly inserts a saved/current/active medication entry by name\n"
                + "- **!@medications:all**: Inserts all profile medication entries\n"
                + "- **!@medications:saved**: Inserts saved medications\n"
                + "- **!@medications:active**: Inserts active prescribed medications\n"
                + "- **!@medications:current**: Inserts currently active/recently logged substances\n"
                + "- **!@medications:favorites**: Inserts favourite medications\n"
                + "- **!@profile:summary**: Inserts a compact profile and medical summary\n"
                + "- **!@logs:recent**: Inserts recent diary/log entries if log context is enabled\n"
                + "- **!@reminders:active**: Inserts active reminders\n"
                + "- **!@settings:summary**: Inserts app language and unit context\n";
    }

    private static String resolve(Context context, JSONObject data, String type, String key) {
        if ("medications".equals(type) || "meds".equals(type)) {
            return resolveMedication(data, key);
        }
        if ("profile".equals(type)) {
            return resolveProfile(data, key);
        }
        if ("logs".equals(type) || "diary".equals(type)) {
            return resolveLogs(data, key);
        }
        if ("reminders".equals(type)) {
            return resolveReminders(data, key);
        }
        if ("settings".equals(type)) {
            return resolveSettings(context);
        }
        return "[Unknown placeholder !" + type + ":" + key + "]";
    }

    private static String resolveMedication(JSONObject data, String key) {
        JSONArray medications = medications(data);
        if (medications.length() == 0) return "[No medications saved in this profile]";
        String normalized = normalize(key);
        StringBuilder output = new StringBuilder();
        boolean groupFilter = "all".equals(normalized) || "saved".equals(normalized) || "active".equals(normalized) || "current".equals(normalized) || "favorites".equals(normalized) || "favourites".equals(normalized);
        for (int i = 0; i < medications.length(); i++) {
            JSONObject item = medications.optJSONObject(i);
            if (item == null) continue;
            String group = item.optString("group", item.optBoolean("active", true) ? "Current" : "Saved");
            boolean match = groupFilter
                    ? ("all".equals(normalized)
                            || normalized.equals(normalize(group))
                            || (("favorites".equals(normalized) || "favourites".equals(normalized)) && item.optBoolean("favorite", false)))
                    : normalize(item.optString("name", "")).contains(normalized);
            if (match) appendMedication(output, item);
        }
        if (output.length() == 0) return "[No medication matched " + key + "]";
        return output.toString().trim();
    }

    private static String resolveLogs(JSONObject data, String key) {
        if (!"recent".equals(normalize(key)) && !"all".equals(normalize(key))) return "[Unknown logs placeholder " + key + "]";
        JSONArray logs = logs(data);
        if (logs.length() == 0) return "[No diary logs saved in this profile]";
        StringBuilder output = new StringBuilder("Recent diary logs\n");
        int start = Math.max(0, logs.length() - 8);
        for (int i = start; i < logs.length(); i++) {
            JSONObject item = logs.optJSONObject(i);
            if (item == null) continue;
            appendLog(output, item);
        }
        return output.toString().trim();
    }

    private static String resolveReminders(JSONObject data, String key) {
        if (!"active".equals(normalize(key)) && !"all".equals(normalize(key))) return "[Unknown reminders placeholder " + key + "]";
        JSONArray reminders = data.optJSONArray("reminders");
        if (reminders == null || reminders.length() == 0) return "[No active reminders saved in this profile]";
        StringBuilder output = new StringBuilder("Active reminders\n");
        for (int i = 0; i < reminders.length(); i++) {
            JSONObject item = reminders.optJSONObject(i);
            if (item == null || !item.optBoolean("enabled", true)) continue;
            append(output, "Reminder", item.optString("title", ""));
            append(output, "Notes", item.optString("notes", ""));
        }
        return output.toString().trim();
    }

    private static String resolveSettings(Context context) {
        if (context == null) return "[Assistant settings unavailable]";
        SettingsState state = new SettingsRepository(context).getState();
        return "Assistant settings\n"
                + "- Language: " + state.language.languageTag() + "\n"
                + "- Units: " + state.unitSystem.preferenceValue();
    }

    private static String resolveProfile(JSONObject data, String key) {
        if (!"summary".equals(normalize(key))) return "[Unknown profile placeholder " + key + "]";
        JSONObject profile = data.optJSONObject(ProfileJson.KEY_PROFILE);
        JSONObject medical = data.optJSONObject(ProfileJson.KEY_MEDICAL);
        StringBuilder output = new StringBuilder("Profile summary\n");
        append(output, "Name", profile == null ? "" : profile.optString("name", ""));
        append(output, "Age", profile == null ? "" : profile.optString("age", ""));
        append(output, "Sex", profile == null ? "" : profile.optString("sex", ""));
        append(output, "Location", profile == null ? "" : profile.optString(ProfileJson.PROFILE_LOCATION, ""));
        append(output, "Allergies", medical == null ? "" : medical.optString("allergies", ""));
        append(output, "Conditions", medical == null ? "" : medical.optString("conditions", ""));
        append(output, "Active medications", medical == null ? "" : medical.optString("active_medications", ""));
        return output.toString().trim();
    }

    private static JSONArray medications(JSONObject data) {
        JSONObject trackers = data.optJSONObject(ProfileJson.KEY_TRACKERS);
        JSONArray medications = trackers == null ? null : trackers.optJSONArray(ProfileJson.KEY_MEDICATIONS);
        return medications == null ? new JSONArray() : medications;
    }

    private static JSONArray logs(JSONObject data) {
        JSONObject trackers = data.optJSONObject(ProfileJson.KEY_TRACKERS);
        JSONArray logs = trackers == null ? null : trackers.optJSONArray(ProfileJson.KEY_LOGS);
        return logs == null ? new JSONArray() : logs;
    }

    private static void appendMedication(StringBuilder output, JSONObject item) {
        if (output.length() > 0) output.append("\n\n");
        output.append("Medication\n");
        append(output, "Name", item.optString("name", ""));
        append(output, "Group", item.optString("group", ""));
        append(output, "Dose", item.optString("dose", ""));
        append(output, "Schedule", item.optString("schedule", ""));
        append(output, "Notes", item.optString("notes", ""));
    }

    private static void appendLog(StringBuilder output, JSONObject item) {
        output.append("- ");
        String name = item.optString("substance", item.optString("name", item.optString("title", "Log")));
        output.append(name);
        String amount = item.optString("amount", item.optString("dose", ""));
        String mood = item.optString("mood", "");
        String notes = item.optString("notes", "");
        if (!amount.isEmpty()) output.append("; amount: ").append(amount);
        if (!mood.isEmpty()) output.append("; mood: ").append(mood);
        if (!notes.isEmpty()) output.append("; notes: ").append(notes);
        output.append("\n");
    }

    private static void append(StringBuilder output, String label, String value) {
        if (value == null || value.trim().isEmpty()) return;
        output.append("- ").append(label).append(": ").append(value.trim()).append("\n");
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.US);
    }
}


