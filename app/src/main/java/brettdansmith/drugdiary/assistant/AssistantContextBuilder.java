package brettdansmith.drugdiary.assistant;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Context;

import java.util.List;

import brettdansmith.drugdiary.data.diary.DiaryRepository;
import brettdansmith.drugdiary.data.medication.MedicationRepository;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.settings.EffectiveSettings;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.domain.units.UnitConverter;
import brettdansmith.drugdiary.domain.units.UnitPreferences;
import brettdansmith.drugdiary.domain.assistant.AssistantCommandRegistry;
import brettdansmith.drugdiary.domain.model.diary.DiaryEntry;
import brettdansmith.drugdiary.domain.model.medication.MedicationRecord;
import brettdansmith.drugdiary.ui.assistant.ChatMessage;
import brettdansmith.drugdiary.settings.AppSettings;

public final class AssistantContextBuilder {
    private static final int MAX_CONTEXT_CHARS = 18_000;
    private AssistantContextBuilder() {}

    public static boolean isProfileContextEnabled(JSONObject vaultData) {
        if (vaultData == null) return false;
        JSONObject privacy = vaultData.optJSONObject(ProfileJson.KEY_PRIVACY);
        if (privacy == null) return false;
        return privacy.optBoolean("share_profile",
                privacy.optBoolean("share_biometrics", false)
                        || privacy.optBoolean("share_meds", false)
                        || privacy.optBoolean("share_logs", false));
    }

    public static String buildPlainText(JSONObject vaultData) {
        return buildPlainText(null, vaultData);
    }

    public static String buildPlainText(Context context, JSONObject vaultData) {
        return buildPlainText(context, vaultData, null);
    }

    public static String buildPlainText(Context context, JSONObject vaultData, List<ChatMessage> requestMessages) {
        EffectiveSettings effective = context == null 
            ? new EffectiveSettings(0, LanguageOption.SYSTEM, brettdansmith.drugdiary.data.settings.UnitSystem.METRIC, false)
            : AppSettings.effective(context);
            
        boolean includeProfile = effective.aiProfileContext;
        boolean includeMedications = effective.aiMedicationContext;
        boolean includeLogs = effective.aiLogContext;

        JSONObject safeVaultData = vaultData == null ? new JSONObject() : vaultData;
        JSONObject profile = safeVaultData.optJSONObject(ProfileJson.KEY_PROFILE);
        JSONObject medical = safeVaultData.optJSONObject(ProfileJson.KEY_MEDICAL);
        JSONObject trackers = safeVaultData.optJSONObject(ProfileJson.KEY_TRACKERS);
        JSONArray reminders = safeVaultData.optJSONArray("reminders");
        
        if (profile == null) profile = new JSONObject();
        if (medical == null) medical = new JSONObject();
        if (trackers == null) trackers = new JSONObject();

        UnitPreferences units = context == null ? new UnitPreferences(brettdansmith.drugdiary.data.settings.UnitSystem.METRIC) : UnitPreferences.from(context);
        boolean privateMode = effective.privateMode;
        StringBuilder text = new StringBuilder();
        text.append("DD_CTX v5\n");
        text.append("settings: lang=").append(languageLabel(effective.language))
                .append("; units=").append(effective.unitSystem.preferenceValue())
                .append("; private_mode=").append(privateMode)
                .append("\n");

        if (includeProfile) {
            text.append("profile: ");
            appendInlineContext(text, "name", profile.optString("name", ""));
            appendInlineContext(text, "age", profile.optString("age", ""));
            appendInlineContext(text, "sex", profile.optString("sex", ""));
            appendInlineContext(text, "location", profile.optString(ProfileJson.PROFILE_LOCATION, ""));
            appendInlineContext(text, "weight", UnitConverter.formatWeight(profile.optDouble("weight_kg", 0), units));
            appendInlineContext(text, "height", UnitConverter.formatHeight(profile.optDouble("height_cm", 0), units));
            appendInlineContext(text, "blood", profile.optString("blood_type", ""));
            appendInlineContext(text, "about", profile.optString("bio", ""));
            trimContextLine(text);
            text.append("\nmedical: ");
            appendInlineContext(text, "allergies", medical.optString("allergies", ""));
            appendInlineContext(text, "conditions", medical.optString("conditions", ""));
            appendInlineContext(text, "emergency_note", medical.optString("emergency_note", ""));
            trimContextLine(text);
            text.append("\n");
        }

        if (includeMedications) {
            text.append("med_notes: ");
            appendInlineContext(text, "active", medical.optString("active_medications", ""));
            trimContextLine(text);
            text.append("\nmeds:\n");
            if (context != null) {
                appendMedicationRecords(text, new MedicationRepository(context).listRecords());
            } else {
                appendArray(text, trackers.optJSONArray(ProfileJson.KEY_MEDICATIONS));
            }
        }

        if (includeLogs) {
            text.append("logs_recent:\n");
            if (context != null) {
                appendDiaryEntries(text, new DiaryRepository(context).listEntries());
            } else {
                appendArray(text, trackers.optJSONArray(ProfileJson.KEY_LOGS));
            }
        }

        if (includeMedications || includeLogs) {
            text.append("reminders:\n");
            appendArray(text, reminders);
        }

        appendRecentCommandContext(text, requestMessages);
        appendToolCatalog(text);
        return trimContext(text.toString().trim());
    }

    private static void appendToolCatalog(StringBuilder sb) {
        sb.append("\n").append(AssistantCommandRegistry.contextToolCatalogText());
    }

    private static void appendRecentCommandContext(StringBuilder text, List<ChatMessage> requestMessages) {
        text.append("recent_command_context:\n");
        if (requestMessages == null || requestMessages.isEmpty()) {
            text.append("- None\n");
            return;
        }
        int appended = 0;
        for (int i = requestMessages.size() - 1; i >= 0 && appended < 6; i--) {
            ChatMessage message = requestMessages.get(i);
            if (message == null) continue;
            String content = message.getContent();
            if (content == null) continue;
            String trimmed = content.trim();
            if (trimmed.isEmpty()) continue;

            if (message.isSent() && trimmed.startsWith("/")) {
                text.append("- command: ").append(oneLine(trimmed)).append("\n");
                appended++;
                continue;
            }

            if (!message.isSent() && trimmed.startsWith("[[command-response]]")) {
                String output = trimmed.substring("[[command-response]]".length()).trim();
                text.append("- command_output: ").append(oneLine(output)).append("\n");
                appended++;
            }
        }
        if (appended == 0) {
            text.append("- None\n");
        }
    }

    private static void appendField(StringBuilder text, String label, String value) {
        appendField(text, label, value, null);
    }

    private static void appendField(StringBuilder text, String label, String value, String instruction) {
        String clean = value == null ? "" : value.trim();
        text.append("- ").append(label).append(": ").append(clean.isEmpty() ? "Not provided" : clean).append("\n");
        if (instruction != null && !instruction.isEmpty()) {
            text.append("  Guidance: ").append(instruction).append("\n");
        }
    }

    private static void appendArray(StringBuilder text, JSONArray array) {
        if (array == null || array.length() == 0) {
            text.append("- None recorded\n");
            return;
        }
        int limit = Math.min(array.length(), 8);
        int start = Math.max(0, array.length() - limit);
        for (int i = start; i < array.length(); i++) {
            JSONObject item = array.optJSONObject(i);
            if (item == null) continue;
            text.append("- ");
            String name = item.optString("name", item.optString("title", "Untitled"));
            text.append(name);
            String group = item.optString("group", "");
            if (!group.isEmpty()) text.append(" [").append(group).append("]");
            String dose = item.optString("dose", "");
            if (!dose.isEmpty()) text.append("; dose: ").append(dose);
            String schedule = item.optString("schedule", "");
            if (!schedule.isEmpty()) text.append("; schedule: ").append(schedule);
            String notes = item.optString("notes", "");
            if (!notes.isEmpty()) text.append("; notes: ").append(notes);
            
            // For reminders
            boolean enabled = item.optBoolean("enabled", true);
            if (item.has("enabled")) text.append("; enabled: ").append(enabled);
            
            text.append("\n");
        }
        if (array.length() > limit) text.append("- total=").append(array.length()).append("; shown=latest ").append(limit).append("\n");
    }

    private static void appendMedicationRecords(StringBuilder text, List<MedicationRecord> records) {
        if (records == null || records.isEmpty()) {
            text.append("- None recorded\n");
            return;
        }
        int limit = Math.min(records.size(), 10);
        for (int i = 0; i < limit; i++) {
            MedicationRecord item = records.get(i);
            text.append("- ").append(item.name);
            text.append("; groups: ").append(item.categoryLabels());
            if (!item.strength.isEmpty()) text.append("; strength: ").append(item.strength);
            if (!item.form.isEmpty()) text.append("; form: ").append(item.form);
            text.append("; active: ").append(item.active);
            text.append("; prn: ").append(item.prn);
            if (item.favorite) text.append("; favourite: true");
            if (!item.notes.isEmpty()) text.append("; notes: ").append(item.notes);
            text.append("\n");
        }
        if (records.size() > limit) text.append("- total=").append(records.size()).append("; shown=").append(limit).append("\n");
    }

    private static void appendDiaryEntries(StringBuilder text, List<DiaryEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            text.append("- None recorded\n");
            return;
        }
        int limit = Math.min(entries.size(), 8);
        for (int i = 0; i < limit; i++) {
            DiaryEntry item = entries.get(i);
            text.append("- ").append(item.title);
            if (!item.notes.isEmpty()) text.append("; notes: ").append(item.notes);
            text.append("; mood=").append(item.mood.mood);
            text.append("; anxiety=").append(item.mood.anxiety);
            text.append("; stress=").append(item.mood.stress);
            text.append("; energy=").append(item.mood.energy);
            text.append("\n");
        }
        if (entries.size() > limit) text.append("- total=").append(entries.size()).append("; shown=").append(limit).append("\n");
    }

    private static void appendInlineContext(StringBuilder text, String label, String value) {
        String clean = value == null ? "" : value.trim();
        if (clean.isEmpty() || "Not provided".equalsIgnoreCase(clean)) return;
        if (clean.length() > 240) clean = clean.substring(0, 240) + "...";
        text.append(label).append("=").append(clean.replace('\n', ' ')).append("; ");
    }

    private static void trimContextLine(StringBuilder text) {
        int length = text.length();
        if (length >= 2 && text.substring(length - 2).equals("; ")) {
            text.delete(length - 2, length);
        } else if (length > 0 && text.charAt(length - 1) == ' ') {
            text.deleteCharAt(length - 1);
        }
    }

    private static String languageLabel(LanguageOption language) {
        if (language == LanguageOption.ENGLISH) return "English";
        if (language == LanguageOption.SPANISH) return "Spanish";
        return "System default";
    }

    private static String oneLine(String value) {
        String safe = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ').trim();
        if (safe.length() > 220) safe = safe.substring(0, 220) + "...";
        return safe;
    }

    private static String trimContext(String context) {
        if (context == null) return "";
        if (context.length() <= MAX_CONTEXT_CHARS) return context;
        String prefix = context.substring(0, Math.min(context.length(), MAX_CONTEXT_CHARS));
        return prefix + "\n[context truncated for efficiency]";
    }

}
