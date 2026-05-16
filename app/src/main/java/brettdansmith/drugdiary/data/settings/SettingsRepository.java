package brettdansmith.drugdiary.data.settings;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.io.File;

/**
 * Single access point for global, non-vault settings.
 *
 * These preferences are intentionally stored outside the encrypted profile vault so
 * theme, language, and login preferences can be applied before a profile session exists.
 * Anything profile-sensitive must stay in the encrypted vault instead.
 */
public final class SettingsRepository {
    public static final String PREFS_NAME = "DrugDiaryGlobalPrefs";

    public static final String PREF_THEME = "app_theme";
    public static final String PREF_UNITS = "unit_system";
    public static final String PREF_TIME_FORMAT = "time_format";
    public static final String PREF_LANGUAGE = "language_tag";
    public static final String PREF_NOTIFICATIONS = "enable_notifs";
    public static final String PREF_BIOMETRIC = "use_biometric";
    public static final String PREF_STEALTH = "stealth_mode";
    public static final String PREF_AUTOLOCK = "auto_lock";
    public static final String PREF_AUTOLOCK_MINUTES = "auto_lock_minutes";
    public static final String PREF_DEFAULT_SIX_DIGIT_PIN = "default_six_digit_pin";
    public static final String PREF_SHOW_PROFILE_SETUP_GUIDANCE = "show_profile_setup_guidance";
    public static final String PREF_AI_MEMORY = "ai_memory";
    public static final String PREF_AI_RESPONSE_NOTIFICATIONS = "ai_response_notifications";
    public static final String PREF_ASSISTANT_ENTRY_SHARE = "assistant_entry_share_enabled";
    public static final String PREF_ASSISTANT_ENTRY_TEXT_SELECTION = "assistant_entry_text_selection_enabled";
    public static final String PREF_AI_PROVIDER = "ai_provider";
    public static final String PREF_REFERENCE_CACHE_DAYS = "reference_cache_days";
    public static final String PREF_AI_WEB_SEARCH = "ai_web_search";
    public static final String PREF_AI_REQUIRE_CITATIONS = "ai_require_citations";
    public static final String PREF_AI_FALLBACK_ENABLED = "ai_fallback_enabled";
    public static final String PREF_AI_FALLBACK_ORDER = "ai_fallback_order";
    public static final String PREF_PRIVATE_MODE = "private_mode";

    // New Global fields
    public static final String PREF_GLOBAL_PRIVACY_COLLECTION = "global_privacy_collection";

    private static final String KEY_API_SUFFIX = "_api_key";
    private static final String KEY_MODEL_SUFFIX = "_model";
    private static final String KEY_BASE_URL_SUFFIX = "_base_url";
    private static final String KEY_ENABLED_SUFFIX = "_enabled";
    private static final String KEY_WEB_SEARCH_SUFFIX = "_web_search";
    private static final String KEY_REQUIRE_CITATIONS_SUFFIX = "_require_citations";
    private static final String KEY_STREAMING_SUFFIX = "_streaming";
    private static final String KEY_TIMEOUT_SUFFIX = "_timeout_seconds";
    private static final String KEY_RETRIES_SUFFIX = "_max_retries";
    private static final String DEFAULT_GROQ_API_KEY = "";

    private final Context appContext;
    private final SharedPreferences prefs;

    public SettingsRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        migrateDefaults();
    }

    public SharedPreferences rawPrefs() {
        return prefs;
    }

    public SettingsState getState() {
        SettingsState defaults = SettingsState.defaults();
        return new SettingsState(
                prefs.getInt(PREF_THEME, defaults.themeMode),
                LanguageOption.fromTag(prefs.getString(PREF_LANGUAGE, defaults.language.languageTag())),
                UnitSystem.fromPreference(prefs.getString(PREF_UNITS, defaults.unitSystem.preferenceValue())),
                TimeFormat.fromPreference(prefs.getString(PREF_TIME_FORMAT, defaults.timeFormat.preferenceValue())),
                prefs.getBoolean(PREF_NOTIFICATIONS, defaults.notificationsEnabled),
                prefs.getBoolean(PREF_STEALTH, defaults.stealthNotifications),
                prefs.getBoolean(PREF_BIOMETRIC, defaults.biometricUnlock),
                prefs.getBoolean(PREF_AUTOLOCK, defaults.autoLock),
                prefs.getInt(PREF_AUTOLOCK_MINUTES, defaults.autoLockMinutes),
                prefs.getBoolean(PREF_DEFAULT_SIX_DIGIT_PIN, defaults.defaultSixDigitPin),
                prefs.getBoolean(PREF_SHOW_PROFILE_SETUP_GUIDANCE, defaults.showProfileSetupGuidance),
                prefs.getBoolean(PREF_AI_MEMORY, defaults.assistantMemory),
                prefs.getBoolean(PREF_AI_RESPONSE_NOTIFICATIONS, defaults.assistantResponseNotifications),
                prefs.getBoolean(PREF_ASSISTANT_ENTRY_SHARE, defaults.assistantEntryFromShareEnabled),
                prefs.getBoolean(PREF_ASSISTANT_ENTRY_TEXT_SELECTION, defaults.assistantEntryFromTextSelectionEnabled),
                AiProvider.fromPreference(prefs.getString(PREF_AI_PROVIDER, defaults.assistantProvider.preferenceValue())),
                prefs.getInt(PREF_REFERENCE_CACHE_DAYS, defaults.referenceCacheDays),
                prefs.getBoolean(PREF_PRIVATE_MODE, defaults.privateMode),
                prefs.getBoolean(PREF_GLOBAL_PRIVACY_COLLECTION, defaults.globalPrivacyCollection));
    }

    public void setThemeMode(int themeMode) {
        prefs.edit().putInt(PREF_THEME, themeMode).apply();
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    public void setLanguage(LanguageOption language) {
        LanguageOption value = language == null ? LanguageOption.SYSTEM : language;
        prefs.edit().putString(PREF_LANGUAGE, value.languageTag()).apply();
        AppCompatDelegate.setApplicationLocales(value == LanguageOption.SYSTEM
                ? LocaleListCompat.getEmptyLocaleList()
                : LocaleListCompat.forLanguageTags(value.languageTag()));
    }

    public void applySavedLocale() {
        setLanguage(getState().language);
    }

    public void setUnitSystem(UnitSystem unitSystem) {
        prefs.edit().putString(PREF_UNITS, unitSystem.preferenceValue()).apply();
    }

    public void setTimeFormat(TimeFormat timeFormat) {
        prefs.edit().putString(PREF_TIME_FORMAT, timeFormat.preferenceValue()).apply();
    }

    public void setNotificationsEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_NOTIFICATIONS, enabled).apply();
    }

    public void setStealthNotifications(boolean enabled) {
        prefs.edit().putBoolean(PREF_STEALTH, enabled).apply();
    }

    public void setBiometricUnlock(boolean enabled) {
        prefs.edit().putBoolean(PREF_BIOMETRIC, enabled).apply();
    }

    public void setAutoLock(boolean enabled) {
        prefs.edit().putBoolean(PREF_AUTOLOCK, enabled).apply();
    }

    public void setAutoLockMinutes(int minutes) {
        prefs.edit().putInt(PREF_AUTOLOCK_MINUTES, Math.max(1, minutes)).apply();
    }

    public void setDefaultSixDigitPin(boolean enabled) {
        prefs.edit().putBoolean(PREF_DEFAULT_SIX_DIGIT_PIN, enabled).apply();
    }

    public void setShowProfileSetupGuidance(boolean show) {
        prefs.edit().putBoolean(PREF_SHOW_PROFILE_SETUP_GUIDANCE, show).apply();
    }

    public void setAssistantMemory(boolean enabled) {
        prefs.edit().putBoolean(PREF_AI_MEMORY, enabled).apply();
    }

    public void setAssistantResponseNotifications(boolean enabled) {
        prefs.edit().putBoolean(PREF_AI_RESPONSE_NOTIFICATIONS, enabled).apply();
    }

    public void setAssistantEntryFromShareEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_ASSISTANT_ENTRY_SHARE, enabled).apply();
    }

    public void setAssistantEntryFromTextSelectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_ASSISTANT_ENTRY_TEXT_SELECTION, enabled).apply();
    }

    public void setAiProvider(AiProvider provider) {
        prefs.edit().putString(PREF_AI_PROVIDER, provider.preferenceValue()).apply();
    }

    public ProviderSettings getProviderSettings(AiProvider provider) {
        AiProvider value = provider == null ? getState().assistantProvider : provider;
        String apiKey = prefs.getString(providerPrefix(value) + KEY_API_SUFFIX, "");
        if ((apiKey == null || apiKey.trim().isEmpty()) && value == AiProvider.GROQ) {
            apiKey = DEFAULT_GROQ_API_KEY;
        }
        return new ProviderSettings(
                value,
                apiKey,
                prefs.getString(providerPrefix(value) + KEY_MODEL_SUFFIX, value.defaultModel()),
                prefs.getString(providerPrefix(value) + KEY_BASE_URL_SUFFIX, ""),
                prefs.getBoolean(providerPrefix(value) + KEY_ENABLED_SUFFIX, true),
                prefs.getBoolean(providerPrefix(value) + KEY_WEB_SEARCH_SUFFIX, false),
                prefs.getBoolean(providerPrefix(value) + KEY_REQUIRE_CITATIONS_SUFFIX, false),
                prefs.getBoolean(providerPrefix(value) + KEY_STREAMING_SUFFIX, true),
                prefs.getInt(providerPrefix(value) + KEY_TIMEOUT_SUFFIX, 45),
                prefs.getInt(providerPrefix(value) + KEY_RETRIES_SUFFIX, 1));
    }

    public void setProviderApiKey(AiProvider provider, String apiKey) {
        prefs.edit().putString(providerPrefix(provider) + KEY_API_SUFFIX, apiKey == null ? "" : apiKey.trim()).apply();
    }

    public void setProviderModel(AiProvider provider, String model) {
        String value = model == null || model.trim().isEmpty() ? provider.defaultModel() : model.trim();
        prefs.edit().putString(providerPrefix(provider) + KEY_MODEL_SUFFIX, value).apply();
    }

    public void setProviderBaseUrl(AiProvider provider, String baseUrl) {
        prefs.edit().putString(providerPrefix(provider) + KEY_BASE_URL_SUFFIX, baseUrl == null ? "" : baseUrl.trim()).apply();
    }

    public void setProviderEnabled(AiProvider provider, boolean enabled) {
        prefs.edit().putBoolean(providerPrefix(provider) + KEY_ENABLED_SUFFIX, enabled).apply();
    }

    public void setProviderWebSearchEnabled(AiProvider provider, boolean enabled) {
        prefs.edit().putBoolean(providerPrefix(provider) + KEY_WEB_SEARCH_SUFFIX, enabled).apply();
    }

    public void setProviderRequireCitations(AiProvider provider, boolean require) {
        prefs.edit().putBoolean(providerPrefix(provider) + KEY_REQUIRE_CITATIONS_SUFFIX, require).apply();
    }

    public void setProviderStreamingEnabled(AiProvider provider, boolean enabled) {
        prefs.edit().putBoolean(providerPrefix(provider) + KEY_STREAMING_SUFFIX, enabled).apply();
    }

    public void setProviderTimeoutSeconds(AiProvider provider, int timeoutSeconds) {
        prefs.edit().putInt(providerPrefix(provider) + KEY_TIMEOUT_SUFFIX, Math.max(5, timeoutSeconds)).apply();
    }

    public void setProviderMaxRetries(AiProvider provider, int maxRetries) {
        prefs.edit().putInt(providerPrefix(provider) + KEY_RETRIES_SUFFIX, Math.max(0, maxRetries)).apply();
    }

    public boolean isAiWebSearchEnabled() {
        return prefs.getBoolean(PREF_AI_WEB_SEARCH, false);
    }

    public void setAiWebSearchEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_AI_WEB_SEARCH, enabled).apply();
    }

    public boolean isAiCitationsRequired() {
        return prefs.getBoolean(PREF_AI_REQUIRE_CITATIONS, false);
    }

    public void setAiCitationsRequired(boolean required) {
        prefs.edit().putBoolean(PREF_AI_REQUIRE_CITATIONS, required).apply();
    }

    public boolean isAiFallbackEnabled() {
        return prefs.getBoolean(PREF_AI_FALLBACK_ENABLED, true);
    }

    public void setAiFallbackEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_AI_FALLBACK_ENABLED, enabled).apply();
    }

    public String getAiFallbackOrder() {
        return prefs.getString(PREF_AI_FALLBACK_ORDER, "");
    }

    public void setAiFallbackOrder(String order) {
        prefs.edit().putString(PREF_AI_FALLBACK_ORDER, order == null ? "" : order.trim()).apply();
    }

    public boolean isPrivateModeEnabled() {
        return prefs.getBoolean(PREF_PRIVATE_MODE, false);
    }

    public void setPrivateModeEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_PRIVATE_MODE, enabled).apply();
    }

    public void setReferenceCacheDays(int days) {
        prefs.edit().putInt(PREF_REFERENCE_CACHE_DAYS, Math.max(1, days)).apply();
    }

    public boolean isGlobalPrivacyCollectionEnabled() {
        return prefs.getBoolean(PREF_GLOBAL_PRIVACY_COLLECTION, true);
    }

    public void setGlobalPrivacyCollectionEnabled(boolean enabled) {
        prefs.edit().putBoolean(PREF_GLOBAL_PRIVACY_COLLECTION, enabled).apply();
    }

    public void resetAllAppData(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (am != null) {
            boolean success = am.clearApplicationUserData();
            if (success) {
                return; // App will be killed, no further action needed
            }
        }
        
        // Fallback if clearApplicationUserData fails or returns false
        prefs.edit().clear().apply();
        context.getSharedPreferences("DrugDiaryProfiles", Context.MODE_PRIVATE).edit().clear().apply();
        File[] files = context.getFilesDir().listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    public static String providerPrefix(AiProvider provider) {
        return provider.preferenceValue();
    }

    private void migrateDefaults() {
        SharedPreferences.Editor editor = prefs.edit();
        boolean changed = false;

        String geminiModel = prefs.getString("gemini_model", "");
        if (geminiModel == null || geminiModel.trim().isEmpty() || "gemini-2.5-flash".equals(geminiModel)) {
            editor.putString("gemini_model", AiProvider.GEMINI.defaultModel());
            changed = true;
        }
        String groqModel = prefs.getString("groq_model", "");
        if (groqModel == null || groqModel.trim().isEmpty() || "llama-3.3-70b-versatile".equals(groqModel)) {
            editor.putString("groq_model", AiProvider.GROQ.defaultModel());
            changed = true;
        }

        if (!prefs.contains(PREF_AI_RESPONSE_NOTIFICATIONS)) {
            editor.putBoolean(PREF_AI_RESPONSE_NOTIFICATIONS, SettingsState.defaults().assistantResponseNotifications);
            changed = true;
        }
        if (!prefs.contains(PREF_ASSISTANT_ENTRY_SHARE)) {
            editor.putBoolean(PREF_ASSISTANT_ENTRY_SHARE, SettingsState.defaults().assistantEntryFromShareEnabled);
            changed = true;
        }
        if (!prefs.contains(PREF_ASSISTANT_ENTRY_TEXT_SELECTION)) {
            editor.putBoolean(PREF_ASSISTANT_ENTRY_TEXT_SELECTION, SettingsState.defaults().assistantEntryFromTextSelectionEnabled);
            changed = true;
        }
        if (!prefs.contains(PREF_AUTOLOCK_MINUTES)) {
            editor.putInt(PREF_AUTOLOCK_MINUTES, SettingsState.defaults().autoLockMinutes);
            changed = true;
        }
        if (!prefs.contains(PREF_REFERENCE_CACHE_DAYS)) {
            editor.putInt(PREF_REFERENCE_CACHE_DAYS, SettingsState.defaults().referenceCacheDays);
            changed = true;
        }
        if (!prefs.contains(PREF_AI_WEB_SEARCH)) {
            editor.putBoolean(PREF_AI_WEB_SEARCH, false);
            changed = true;
        }
        if (!prefs.contains(PREF_AI_REQUIRE_CITATIONS)) {
            editor.putBoolean(PREF_AI_REQUIRE_CITATIONS, false);
            changed = true;
        }
        if (!prefs.contains(PREF_AI_FALLBACK_ENABLED)) {
            editor.putBoolean(PREF_AI_FALLBACK_ENABLED, true);
            changed = true;
        }
        if (!prefs.contains(PREF_AI_FALLBACK_ORDER)) {
            editor.putString(PREF_AI_FALLBACK_ORDER, "");
            changed = true;
        }
        if (!prefs.contains(PREF_PRIVATE_MODE)) {
            editor.putBoolean(PREF_PRIVATE_MODE, false);
            changed = true;
        }
        if (changed) {
            editor.apply();
        }
    }
}
