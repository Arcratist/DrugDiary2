package brettdansmith.drugdiary.settings;

import android.content.Context;
import android.content.SharedPreferences;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.EffectiveSettings;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsResolver;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.TimeFormat;
import brettdansmith.drugdiary.data.settings.UnitSystem;

/**
 * Backward-compatible facade for older screens while new code moves to SettingsRepository.
 * Updated to use SettingsResolver for multi-layer settings.
 */
public final class AppSettings {
    public static final String PREFS_NAME = SettingsRepository.PREFS_NAME;

    public static final String PREF_THEME = SettingsRepository.PREF_THEME;
    public static final String PREF_UNITS = SettingsRepository.PREF_UNITS;
    public static final String PREF_TIME_FORMAT = SettingsRepository.PREF_TIME_FORMAT;
    public static final String PREF_LANGUAGE = SettingsRepository.PREF_LANGUAGE;
    public static final String PREF_NOTIFICATIONS = SettingsRepository.PREF_NOTIFICATIONS;
    public static final String PREF_BIOMETRIC = SettingsRepository.PREF_BIOMETRIC;
    public static final String PREF_STEALTH = SettingsRepository.PREF_STEALTH;
    public static final String PREF_AUTOLOCK = SettingsRepository.PREF_AUTOLOCK;
    public static final String PREF_AUTOLOCK_MINUTES = SettingsRepository.PREF_AUTOLOCK_MINUTES;
    public static final String PREF_AI_MEMORY = SettingsRepository.PREF_AI_MEMORY;
    public static final String PREF_AI_RESPONSE_NOTIFICATIONS = SettingsRepository.PREF_AI_RESPONSE_NOTIFICATIONS;
    public static final String PREF_AI_PROVIDER = SettingsRepository.PREF_AI_PROVIDER;
    public static final String PREF_DEFAULT_SIX_DIGIT_PIN = SettingsRepository.PREF_DEFAULT_SIX_DIGIT_PIN;
    public static final String PREF_SHOW_PROFILE_SETUP_GUIDANCE = SettingsRepository.PREF_SHOW_PROFILE_SETUP_GUIDANCE;
    public static final String PREF_REFERENCE_CACHE_DAYS = SettingsRepository.PREF_REFERENCE_CACHE_DAYS;
    public static final String PREF_AI_WEB_SEARCH = SettingsRepository.PREF_AI_WEB_SEARCH;
    public static final String PREF_AI_REQUIRE_CITATIONS = SettingsRepository.PREF_AI_REQUIRE_CITATIONS;
    public static final String PREF_AI_FALLBACK_ENABLED = SettingsRepository.PREF_AI_FALLBACK_ENABLED;
    public static final String PREF_AI_FALLBACK_ORDER = SettingsRepository.PREF_AI_FALLBACK_ORDER;
    public static final String PREF_PRIVATE_MODE = SettingsRepository.PREF_PRIVATE_MODE;
    public static final String PREF_HIDE_DASHBOARD_SENSITIVE = SettingsRepository.PREF_HIDE_DASHBOARD_SENSITIVE;

    private AppSettings() {
    }

    public static SettingsRepository repository(Context context) {
        return new SettingsRepository(context);
    }

    public static SettingsState state(Context context) {
        return repository(context).getState();
    }

    public static EffectiveSettings effective(Context context) {
        return SettingsResolver.getEffectiveSettings(context);
    }

    public static SharedPreferences prefs(Context context) {
        return repository(context).rawPrefs();
    }

    public static int getTheme(Context context) {
        return effective(context).themeMode;
    }

    public static boolean useMetric(Context context) {
        return effective(context).unitSystem == UnitSystem.METRIC;
    }

    public static boolean use24HourTime(Context context) {
        return state(context).timeFormat == TimeFormat.TWENTY_FOUR_HOUR;
    }

    public static String getLanguageTag(Context context) {
        return effective(context).language.languageTag();
    }

    public static boolean privateModeEnabled(Context context) {
        return effective(context).privateMode;
    }

    public static boolean hideDashboardSensitive(Context context) {
        return effective(context).hideDashboardSensitive;
    }

    public static boolean defaultSixDigitPin(Context context) {
        return state(context).defaultSixDigitPin;
    }

    public static boolean showProfileSetupGuidance(Context context) {
        return state(context).showProfileSetupGuidance;
    }

    public static String getAiProvider(Context context) {
        return state(context).assistantProvider.preferenceValue();
    }

    public static String getAnthropicApiKey(Context context) {
        return provider(context, AiProvider.ANTHROPIC).apiKey;
    }

    public static String getAnthropicModel(Context context) {
        return provider(context, AiProvider.ANTHROPIC).model;
    }

    public static String getGeminiApiKey(Context context) {
        return provider(context, AiProvider.GEMINI).apiKey;
    }

    public static String getGeminiModel(Context context) {
        return provider(context, AiProvider.GEMINI).model;
    }

    public static void setGlobalTheme(Context context, int theme) {
        repository(context).setThemeMode(theme);
    }

    public static ProviderSettings provider(Context context, AiProvider provider) {
        return repository(context).getProviderSettings(provider);
    }
}
