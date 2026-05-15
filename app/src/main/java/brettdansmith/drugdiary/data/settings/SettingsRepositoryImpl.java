package brettdansmith.drugdiary.data.settings;

import android.content.Context;

import brettdansmith.drugdiary.domain.repository.SettingsRepository;

/**
 * Implementation adapter for SettingsRepository interface.
 * Wraps the existing data-layer SettingsRepository to provide domain-level interface.
 */
public final class SettingsRepositoryImpl implements SettingsRepository {
    private final brettdansmith.drugdiary.data.settings.SettingsRepository delegate;

    public SettingsRepositoryImpl(Context context) {
        this.delegate = new brettdansmith.drugdiary.data.settings.SettingsRepository(context);
    }

    @Override
    public int getTheme() {
        return delegate.getState().themeMode;
    }

    @Override
    public void setTheme(int theme) {
        delegate.setThemeMode(theme);
    }

    @Override
    public ProviderSettings getProviderSettings(AiProvider provider) {
        return delegate.getProviderSettings(provider);
    }

    @Override
    public void setProviderApiKey(AiProvider provider, String apiKey) {
        delegate.setProviderApiKey(provider, apiKey);
    }

    @Override
    public AiProvider getAiProvider() {
        return delegate.getState().assistantProvider;
    }

    @Override
    public void setAiProvider(AiProvider provider) {
        delegate.setAiProvider(provider);
    }

    @Override
    public boolean isAiWebSearchEnabled() {
        return delegate.isAiWebSearchEnabled();
    }

    @Override
    public void setAiWebSearchEnabled(boolean enabled) {
        delegate.setAiWebSearchEnabled(enabled);
    }

    @Override
    public boolean isPrivateModeEnabled() {
        return delegate.isPrivateModeEnabled();
    }

    @Override
    public void setPrivateModeEnabled(boolean enabled) {
        delegate.setPrivateModeEnabled(enabled);
    }
}

