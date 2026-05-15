package brettdansmith.drugdiary.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.domain.repository.SettingsRepository;
import brettdansmith.drugdiary.domain.service.ServiceLocator;

/**
 * ViewModel for managing application settings.
 */
public class SettingsViewModel extends ViewModel {
    private final SettingsRepository settingsRepository;

    private final MutableLiveData<Integer> themeModeData = new MutableLiveData<>();
    private final MutableLiveData<AiProvider> aiProviderData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> privateModeData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> webSearchData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public SettingsViewModel(ServiceLocator serviceLocator) {
        this.settingsRepository = serviceLocator.getSettingsRepository();
        loadSettings();
    }

    /**
     * Gets the theme mode live data.
     */
    public LiveData<Integer> getThemeMode() {
        return themeModeData;
    }

    /**
     * Gets the AI provider live data.
     */
    public LiveData<AiProvider> getAiProvider() {
        return aiProviderData;
    }

    /**
     * Gets the private mode live data.
     */
    public LiveData<Boolean> getPrivateMode() {
        return privateModeData;
    }

    /**
     * Gets the web search enabled live data.
     */
    public LiveData<Boolean> getWebSearchEnabled() {
        return webSearchData;
    }

    /**
     * Gets the error live data.
     */
    public LiveData<String> getError() {
        return errorLiveData;
    }

    /**
     * Loads all settings.
     */
    private void loadSettings() {
        new Thread(() -> {
            try {
                int themeMode = settingsRepository.getTheme();
                AiProvider provider = settingsRepository.getAiProvider();
                boolean privateMode = settingsRepository.isPrivateModeEnabled();
                boolean webSearch = settingsRepository.isAiWebSearchEnabled();

                themeModeData.postValue(themeMode);
                aiProviderData.postValue(provider);
                privateModeData.postValue(privateMode);
                webSearchData.postValue(webSearch);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    /**
     * Sets the theme mode.
     */
    public void setThemeMode(int themeMode) {
        new Thread(() -> {
            try {
                settingsRepository.setTheme(themeMode);
                themeModeData.postValue(themeMode);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    /**
     * Sets the AI provider.
     */
    public void setAiProvider(AiProvider provider) {
        new Thread(() -> {
            try {
                settingsRepository.setAiProvider(provider);
                aiProviderData.postValue(provider);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    /**
     * Sets private mode.
     */
    public void setPrivateMode(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setPrivateModeEnabled(enabled);
                privateModeData.postValue(enabled);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    /**
     * Sets web search enabled.
     */
    public void setWebSearchEnabled(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setAiWebSearchEnabled(enabled);
                webSearchData.postValue(enabled);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }
}

