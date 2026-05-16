package brettdansmith.drugdiary.data.settings;

import android.content.Context;
import brettdansmith.drugdiary.domain.repository.SettingsRepository;

public final class SettingsRepositoryImpl implements SettingsRepository {
    private final brettdansmith.drugdiary.data.settings.SettingsRepository delegate;
    private final Context context;

    public SettingsRepositoryImpl(Context context) {
        this.context = context.getApplicationContext();
        this.delegate = new brettdansmith.drugdiary.data.settings.SettingsRepository(this.context);
    }

    @Override
    public SettingsState getGlobalState() {
        return delegate.getState();
    }

    @Override
    public UserSpecificSettings getUserSpecificSettings() {
        return SettingsResolver.getUserSpecificSettings(context);
    }

    @Override
    public void setTheme(int theme) {
        delegate.setThemeMode(theme);
    }

    @Override
    public void setLanguage(LanguageOption language) {
        delegate.setLanguage(language);
    }

    @Override
    public void setUnitSystem(UnitSystem unitSystem) {
        delegate.setUnitSystem(unitSystem);
    }

    @Override
    public void setAiProvider(AiProvider provider) {
        delegate.setAiProvider(provider);
    }

    @Override
    public void setAiWebSearchEnabled(boolean enabled) {
        delegate.setAiWebSearchEnabled(enabled);
    }

    @Override
    public void setPrivateModeEnabled(boolean enabled) {
        delegate.setPrivateModeEnabled(enabled);
    }

    @Override
    public void setProviderApiKey(AiProvider provider, String apiKey) {
        delegate.setProviderApiKey(provider, apiKey);
    }

    @Override
    public void setProviderModel(AiProvider provider, String model) {
        delegate.setProviderModel(provider, model);
    }

    @Override
    public void setNotificationsEnabled(boolean enabled) {
        delegate.setNotificationsEnabled(enabled);
    }

    @Override
    public void setStealthNotifications(boolean enabled) {
        delegate.setStealthNotifications(enabled);
    }

    @Override
    public void setAssistantResponseNotifications(boolean enabled) {
        delegate.setAssistantResponseNotifications(enabled);
    }

    @Override
    public void setAssistantEntryFromShareEnabled(boolean enabled) {
        delegate.setAssistantEntryFromShareEnabled(enabled);
    }

    @Override
    public void setAssistantEntryFromTextSelectionEnabled(boolean enabled) {
        delegate.setAssistantEntryFromTextSelectionEnabled(enabled);
    }

    @Override
    public void setAssistantMemory(boolean enabled) {
        delegate.setAssistantMemory(enabled);
    }

    @Override
    public void setAiCitationsRequired(boolean enabled) {
        delegate.setAiCitationsRequired(enabled);
    }

    @Override
    public void setAiFallbackEnabled(boolean enabled) {
        delegate.setAiFallbackEnabled(enabled);
    }

    @Override
    public void resetAllAppData(Context context) {
        delegate.resetAllAppData(context);
    }

    @Override
    public void setUserThemeOverride(Integer theme) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(theme, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public void setPreferredAiOverride(AiProvider provider) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, provider, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public void setLanguageOverride(LanguageOption language) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, language, current.unitsOverride, current.privateModeOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public void setUnitsOverride(UnitSystem units) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, units, current.privateModeOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public void setPrivateModeOverride(Boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, enabled, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public void setUserAiProfileContext(boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.preferredAiOverride, enabled, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public void setUserAiMedicationContext(boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.preferredAiOverride, current.aiProfileContext, enabled, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public void setUserAiLogContext(boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(context);
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, enabled);
        SettingsResolver.saveUserSpecificSettings(context, updated);
    }

    @Override
    public ProviderSettings getProviderSettings(AiProvider provider) {
        return delegate.getProviderSettings(provider);
    }

    @Override
    public Context getContext() {
        return context;
    }
}
