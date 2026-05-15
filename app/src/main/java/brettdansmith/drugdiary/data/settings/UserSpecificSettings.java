package brettdansmith.drugdiary.data.settings;

import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Per-profile overrides for global settings.
 * AI context settings are stored exclusively here and default to true.
 */
public final class UserSpecificSettings {
    @Nullable public final Integer themeOverride;
    @Nullable public final LanguageOption languageOverride;
    @Nullable public final UnitSystem unitsOverride;
    @Nullable public final Boolean privateModeOverride;
    @Nullable public final Boolean hideDashboardSensitiveOverride;
    @Nullable public final AiProvider preferredAiOverride;
    
    // AI context settings are profile-only
    public final boolean aiProfileContext;
    public final boolean aiMedicationContext;
    public final boolean aiLogContext;

    public UserSpecificSettings(
            @Nullable Integer themeOverride,
            @Nullable LanguageOption languageOverride,
            @Nullable UnitSystem unitsOverride,
            @Nullable Boolean privateModeOverride,
            @Nullable Boolean hideDashboardSensitiveOverride) {
        this(themeOverride, languageOverride, unitsOverride, privateModeOverride, hideDashboardSensitiveOverride, null, true, true, true);
    }

    public UserSpecificSettings(
            @Nullable Integer themeOverride,
            @Nullable LanguageOption languageOverride,
            @Nullable UnitSystem unitsOverride,
            @Nullable Boolean privateModeOverride,
            @Nullable Boolean hideDashboardSensitiveOverride,
            @Nullable AiProvider preferredAiOverride) {
        this(themeOverride, languageOverride, unitsOverride, privateModeOverride, hideDashboardSensitiveOverride, preferredAiOverride, true, true, true);
    }

    public UserSpecificSettings(
            @Nullable Integer themeOverride,
            @Nullable LanguageOption languageOverride,
            @Nullable UnitSystem unitsOverride,
            @Nullable Boolean privateModeOverride,
            @Nullable Boolean hideDashboardSensitiveOverride,
            @Nullable AiProvider preferredAiOverride,
            boolean aiProfileContext,
            boolean aiMedicationContext,
            boolean aiLogContext) {
        this.themeOverride = themeOverride;
        this.languageOverride = languageOverride;
        this.unitsOverride = unitsOverride;
        this.privateModeOverride = privateModeOverride;
        this.hideDashboardSensitiveOverride = hideDashboardSensitiveOverride;
        this.preferredAiOverride = preferredAiOverride;
        this.aiProfileContext = aiProfileContext;
        this.aiMedicationContext = aiMedicationContext;
        this.aiLogContext = aiLogContext;
    }

    public static UserSpecificSettings empty() {
        return new UserSpecificSettings(null, null, null, null, null, null, true, true, true);
    }

    public JSONObject toJson() throws JSONException {
        JSONObject json = new JSONObject();
        if (themeOverride != null) json.put("theme", (int) themeOverride);
        if (languageOverride != null) json.put("language", languageOverride.languageTag());
        if (unitsOverride != null) json.put("units", unitsOverride.preferenceValue());
        if (privateModeOverride != null) json.put("private_mode", (boolean) privateModeOverride);
        if (hideDashboardSensitiveOverride != null) json.put("hide_sensitive", (boolean) hideDashboardSensitiveOverride);
        if (preferredAiOverride != null) json.put("preferred_ai", preferredAiOverride.preferenceValue());
        
        json.put("ai_profile_context", aiProfileContext);
        json.put("ai_medication_context", aiMedicationContext);
        json.put("ai_log_context", aiLogContext);
        return json;
    }

    public static UserSpecificSettings fromJson(JSONObject json) {
        if (json == null) return empty();
        
        Integer theme = json.has("theme") ? json.optInt("theme") : null;
        String langTag = json.optString("language", null);
        LanguageOption lang = (langTag != null) ? LanguageOption.fromTag(langTag) : null;
        String unitPref = json.optString("units", null);
        UnitSystem units = (unitPref != null) ? UnitSystem.fromPreference(unitPref) : null;
        Boolean privateMode = json.has("private_mode") ? json.optBoolean("private_mode") : null;
        Boolean hideSensitive = json.has("hide_sensitive") ? json.optBoolean("hide_sensitive") : null;
        String aiPref = json.optString("preferred_ai", null);
        AiProvider preferredAi = (aiPref != null) ? AiProvider.fromPreference(aiPref) : null;
        if (aiPref == null) preferredAi = null;

        boolean aiProfile = json.optBoolean("ai_profile_context", true);
        boolean aiMedication = json.optBoolean("ai_medication_context", true);
        boolean aiLog = json.optBoolean("ai_log_context", true);
        
        return new UserSpecificSettings(theme, lang, units, privateMode, hideSensitive, preferredAi, aiProfile, aiMedication, aiLog);
    }
}
