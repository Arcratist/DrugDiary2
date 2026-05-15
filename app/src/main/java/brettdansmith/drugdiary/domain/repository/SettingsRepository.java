package brettdansmith.drugdiary.domain.repository;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.ProviderSettings;

/**
 * Repository interface for managing settings.
 * Defines the contract for all settings data operations.
 */
public interface SettingsRepository {
    /**
     * Gets the current theme setting.
     *
     * @return theme identifier
     */
    int getTheme();

    /**
     * Sets the application theme.
     *
     * @param theme the theme to set
     */
    void setTheme(int theme);

    /**
     * Gets provider settings for a specific AI provider.
     *
     * @param provider the AI provider
     * @return provider settings
     */
    ProviderSettings getProviderSettings(AiProvider provider);

    /**
     * Updates API key for a provider.
     *
     * @param provider the AI provider
     * @param apiKey the API key
     */
    void setProviderApiKey(AiProvider provider, String apiKey);

    /**
     * Gets the current AI provider.
     *
     * @return the AI provider
     */
    AiProvider getAiProvider();

    /**
     * Sets the current AI provider.
     *
     * @param provider the AI provider to set
     */
    void setAiProvider(AiProvider provider);

    /**
     * Checks if AI web search is enabled.
     *
     * @return true if enabled
     */
    boolean isAiWebSearchEnabled();

    /**
     * Sets AI web search enabled state.
     *
     * @param enabled the enabled state
     */
    void setAiWebSearchEnabled(boolean enabled);

    /**
     * Checks if private mode is enabled.
     *
     * @return true if enabled
     */
    boolean isPrivateModeEnabled();

    /**
     * Sets private mode enabled state.
     *
     * @param enabled the enabled state
     */
    void setPrivateModeEnabled(boolean enabled);
}

