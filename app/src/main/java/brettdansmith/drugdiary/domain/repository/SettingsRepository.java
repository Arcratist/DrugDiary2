package brettdansmith.drugdiary.domain.repository;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.data.settings.UserSpecificSettings;
import android.content.Context;

/**
 * Repository interface for managing settings.
 * Defines the contract for all settings data operations.
 */
public interface SettingsRepository {
    /**
     * Gets the current global settings state.
     */
    SettingsState getGlobalState();

    /**
     * Gets the user-specific settings.
     */
    UserSpecificSettings getUserSpecificSettings();

    /**
     * Sets the application theme.
     */
    void setTheme(int theme);

    /**
     * Sets the application language.
     */
    void setLanguage(LanguageOption language);

    /**
     * Sets the unit system.
     */
    void setUnitSystem(UnitSystem unitSystem);

    /**
     * Sets AI provider.
     */
    void setAiProvider(AiProvider provider);

    /**
     * Sets AI web search enabled.
     */
    void setAiWebSearchEnabled(boolean enabled);

    /**
     * Sets private mode enabled.
     */
    void setPrivateModeEnabled(boolean enabled);

    /**
     * Sets provider API key.
     */
    void setProviderApiKey(AiProvider provider, String apiKey);

    /**
     * Sets provider model.
     */
    void setProviderModel(AiProvider provider, String model);

    /**
     * Sets notifications enabled.
     */
    void setNotificationsEnabled(boolean enabled);

    /**
     * Sets stealth notifications.
     */
    void setStealthNotifications(boolean enabled);

    /**
     * Sets assistant response notifications.
     */
    void setAssistantResponseNotifications(boolean enabled);
    void setAssistantEntryFromShareEnabled(boolean enabled);
    void setAssistantEntryFromTextSelectionEnabled(boolean enabled);

    /**
     * Sets assistant memory.
     */
    void setAssistantMemory(boolean enabled);

    /**
     * Sets citations required.
     */
    void setAiCitationsRequired(boolean enabled);

    /**
     * Sets fallback enabled.
     */
    void setAiFallbackEnabled(boolean enabled);

    /**
     * Resets all app data.
     */
    void resetAllAppData(Context context);

    /**
     * Sets user theme override.
     */
    void setUserThemeOverride(Integer theme);

    /**
     * Sets user preferred AI override.
     */
    void setPreferredAiOverride(AiProvider provider);

    /**
     * Sets user language override.
     */
    void setLanguageOverride(LanguageOption language);

    /**
     * Sets user units override.
     */
    void setUnitsOverride(UnitSystem units);

    /**
     * Sets user private mode override.
     */
    void setPrivateModeOverride(Boolean enabled);

    /**
     * Sets user AI profile context inclusion.
     */
    void setUserAiProfileContext(boolean enabled);

    /**
     * Sets user AI medication context inclusion.
     */
    void setUserAiMedicationContext(boolean enabled);

    /**
     * Sets user AI log context inclusion.
     */
    void setUserAiLogContext(boolean enabled);

    /**
     * Gets provider settings.
     */
    ProviderSettings getProviderSettings(AiProvider provider);

    /**
     * Gets application context.
     */
    Context getContext();
}

