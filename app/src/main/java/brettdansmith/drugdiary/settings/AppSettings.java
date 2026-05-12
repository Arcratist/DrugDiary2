package brettdansmith.drugdiary.settings;

import android.content.Context;
import android.content.SharedPreferences;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.TimeFormat;
import brettdansmith.drugdiary.data.settings.UnitSystem;

/**
 * Backward-compatible facade for older screens while new code moves to SettingsRepository.
 * Keep this class thin so settings behavior stays centralized.
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
    public static final String PREF_AI_PROFILE_CONTEXT = SettingsRepository.PREF_AI_PROFILE_CONTEXT;
    public static final String PREF_AI_MEDICATION_CONTEXT = SettingsRepository.PREF_AI_MEDICATION_CONTEXT;
    public static final String PREF_AI_LOG_CONTEXT = SettingsRepository.PREF_AI_LOG_CONTEXT;
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

    public static final String PROVIDER_OPENAI = AiProvider.OPENAI.preferenceValue();
    public static final String PROVIDER_ANTHROPIC = AiProvider.ANTHROPIC.preferenceValue();
    public static final String PROVIDER_GEMINI = AiProvider.GEMINI.preferenceValue();
    public static final String PROVIDER_OPENROUTER = AiProvider.OPENROUTER.preferenceValue();
    public static final String PROVIDER_DEEPSEEK = AiProvider.DEEPSEEK.preferenceValue();
    public static final String PROVIDER_XAI = AiProvider.XAI.preferenceValue();
    public static final String PROVIDER_GROQ = AiProvider.GROQ.preferenceValue();
    public static final String PROVIDER_MISTRAL = AiProvider.MISTRAL.preferenceValue();
    public static final String PROVIDER_PERPLEXITY = AiProvider.PERPLEXITY.preferenceValue();

    public static final String PREF_OPENAI_API_KEY = "openai_api_key";
    public static final String PREF_OPENAI_MODEL = "openai_model";
    public static final String PREF_ANTHROPIC_API_KEY = "anthropic_api_key";
    public static final String PREF_ANTHROPIC_MODEL = "anthropic_model";
    public static final String PREF_GEMINI_API_KEY = "gemini_api_key";
    public static final String PREF_GEMINI_MODEL = "gemini_model";
    public static final String PREF_OPENROUTER_API_KEY = "openrouter_api_key";
    public static final String PREF_OPENROUTER_MODEL = "openrouter_model";
    public static final String PREF_DEEPSEEK_API_KEY = "deepseek_api_key";
    public static final String PREF_DEEPSEEK_MODEL = "deepseek_model";
    public static final String PREF_XAI_API_KEY = "xai_api_key";
    public static final String PREF_XAI_MODEL = "xai_model";
    public static final String PREF_GROQ_API_KEY = "groq_api_key";
    public static final String PREF_GROQ_MODEL = "groq_model";
    public static final String PREF_MISTRAL_API_KEY = "mistral_api_key";
    public static final String PREF_MISTRAL_MODEL = "mistral_model";
    public static final String PREF_PERPLEXITY_API_KEY = "perplexity_api_key";
    public static final String PREF_PERPLEXITY_MODEL = "perplexity_model";

    public static final String UNITS_METRIC = UnitSystem.METRIC.preferenceValue();
    public static final String UNITS_IMPERIAL = UnitSystem.IMPERIAL.preferenceValue();
    public static final String TIME_24H = TimeFormat.TWENTY_FOUR_HOUR.preferenceValue();
    public static final String TIME_12H = TimeFormat.TWELVE_HOUR.preferenceValue();

    private AppSettings() {
    }

    public static SettingsRepository repository(Context context) {
        return new SettingsRepository(context);
    }

    public static SettingsState state(Context context) {
        return repository(context).getState();
    }

    public static SharedPreferences prefs(Context context) {
        return repository(context).rawPrefs();
    }

    public static int getTheme(Context context) {
        return state(context).themeMode;
    }

    public static void setTheme(Context context, int theme) {
        repository(context).setThemeMode(theme);
    }

    public static boolean useMetric(Context context) {
        return state(context).unitSystem == UnitSystem.METRIC;
    }

    public static void setUseMetric(Context context, boolean useMetric) {
        repository(context).setUnitSystem(useMetric ? UnitSystem.METRIC : UnitSystem.IMPERIAL);
    }

    public static boolean use24HourTime(Context context) {
        return state(context).timeFormat == TimeFormat.TWENTY_FOUR_HOUR;
    }

    public static void setUse24HourTime(Context context, boolean use24HourTime) {
        repository(context).setTimeFormat(use24HourTime ? TimeFormat.TWENTY_FOUR_HOUR : TimeFormat.TWELVE_HOUR);
    }

    public static String getLanguageTag(Context context) {
        return state(context).language.languageTag();
    }

    public static void setLanguageTag(Context context, String languageTag) {
        repository(context).setLanguage(brettdansmith.drugdiary.data.settings.LanguageOption.fromTag(languageTag));
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

    public static void setAiProvider(Context context, String provider) {
        repository(context).setAiProvider(AiProvider.fromPreference(provider));
    }

    public static boolean assistantProfileContextEnabled(Context context) {
        return state(context).assistantProfileContext;
    }

    public static boolean assistantMedicationContextEnabled(Context context) {
        return state(context).assistantMedicationContext;
    }

    public static boolean assistantLogContextEnabled(Context context) {
        return state(context).assistantLogContext;
    }

    public static boolean assistantResponseNotificationsEnabled(Context context) {
        return state(context).assistantResponseNotifications;
    }

    public static String getOpenAiApiKey(Context context) {
        return provider(context, AiProvider.OPENAI).apiKey;
    }

    public static void setOpenAiApiKey(Context context, String apiKey) {
        repository(context).setProviderApiKey(AiProvider.OPENAI, apiKey);
    }

    public static String getOpenAiModel(Context context) {
        return provider(context, AiProvider.OPENAI).model;
    }

    public static void setOpenAiModel(Context context, String model) {
        repository(context).setProviderModel(AiProvider.OPENAI, model);
    }

    public static String getAnthropicApiKey(Context context) {
        return provider(context, AiProvider.ANTHROPIC).apiKey;
    }

    public static void setAnthropicApiKey(Context context, String apiKey) {
        repository(context).setProviderApiKey(AiProvider.ANTHROPIC, apiKey);
    }

    public static String getAnthropicModel(Context context) {
        return provider(context, AiProvider.ANTHROPIC).model;
    }

    public static void setAnthropicModel(Context context, String model) {
        repository(context).setProviderModel(AiProvider.ANTHROPIC, model);
    }

    public static String getGeminiApiKey(Context context) {
        return provider(context, AiProvider.GEMINI).apiKey;
    }

    public static void setGeminiApiKey(Context context, String apiKey) {
        repository(context).setProviderApiKey(AiProvider.GEMINI, apiKey);
    }

    public static String getGeminiModel(Context context) {
        return provider(context, AiProvider.GEMINI).model;
    }

    public static void setGeminiModel(Context context, String model) {
        repository(context).setProviderModel(AiProvider.GEMINI, model);
    }

    public static String getOpenRouterApiKey(Context context) {
        return provider(context, AiProvider.OPENROUTER).apiKey;
    }

    public static void setOpenRouterApiKey(Context context, String apiKey) {
        repository(context).setProviderApiKey(AiProvider.OPENROUTER, apiKey);
    }

    public static String getOpenRouterModel(Context context) {
        return provider(context, AiProvider.OPENROUTER).model;
    }

    public static void setOpenRouterModel(Context context, String model) {
        repository(context).setProviderModel(AiProvider.OPENROUTER, model);
    }

    public static String getDeepseekApiKey(Context context) {
        return provider(context, AiProvider.DEEPSEEK).apiKey;
    }

    public static void setDeepseekApiKey(Context context, String apiKey) {
        repository(context).setProviderApiKey(AiProvider.DEEPSEEK, apiKey);
    }

    public static String getDeepseekModel(Context context) {
        return provider(context, AiProvider.DEEPSEEK).model;
    }

    public static void setDeepseekModel(Context context, String model) {
        repository(context).setProviderModel(AiProvider.DEEPSEEK, model);
    }

    public static String getXaiApiKey(Context context) {
        return provider(context, AiProvider.XAI).apiKey;
    }

    public static String getXaiModel(Context context) {
        return provider(context, AiProvider.XAI).model;
    }

    public static String getGroqApiKey(Context context) {
        return provider(context, AiProvider.GROQ).apiKey;
    }

    public static String getGroqModel(Context context) {
        return provider(context, AiProvider.GROQ).model;
    }

    public static String getMistralApiKey(Context context) {
        return provider(context, AiProvider.MISTRAL).apiKey;
    }

    public static String getMistralModel(Context context) {
        return provider(context, AiProvider.MISTRAL).model;
    }

    public static String getPerplexityApiKey(Context context) {
        return provider(context, AiProvider.PERPLEXITY).apiKey;
    }

    public static String getPerplexityModel(Context context) {
        return provider(context, AiProvider.PERPLEXITY).model;
    }

    public static boolean aiWebSearchEnabled(Context context) {
        return repository(context).isAiWebSearchEnabled();
    }

    public static boolean aiCitationsRequired(Context context) {
        return repository(context).isAiCitationsRequired();
    }

    public static boolean aiFallbackEnabled(Context context) {
        return repository(context).isAiFallbackEnabled();
    }

    public static String aiFallbackOrder(Context context) {
        return repository(context).getAiFallbackOrder();
    }

    public static boolean privateModeEnabled(Context context) {
        return repository(context).isPrivateModeEnabled();
    }

    public static boolean hideDashboardSensitive(Context context) {
        return repository(context).hideDashboardSensitive();
    }

    private static ProviderSettings provider(Context context, AiProvider provider) {
        return repository(context).getProviderSettings(provider);
    }
}

