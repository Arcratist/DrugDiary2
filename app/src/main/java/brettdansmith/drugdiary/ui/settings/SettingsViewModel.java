package brettdansmith.drugdiary.ui.settings;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import android.content.Context;
import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.data.settings.UserSpecificSettings;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.domain.repository.SettingsRepository;
import brettdansmith.drugdiary.domain.service.ServiceLocator;

public class SettingsViewModel extends ViewModel {
    private final SettingsRepository settingsRepository;

    private final MutableLiveData<SettingsState> globalSettingsData = new MutableLiveData<>();
    private final MutableLiveData<UserSpecificSettings> userSettingsData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    public SettingsViewModel(ServiceLocator serviceLocator) {
        this.settingsRepository = serviceLocator.getSettingsRepository();
        loadSettings();
    }

    public LiveData<SettingsState> getGlobalSettings() {
        return globalSettingsData;
    }

    public LiveData<UserSpecificSettings> getUserSettings() {
        return userSettingsData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    public void loadSettings() {
        new Thread(() -> {
            try {
                SettingsState global = settingsRepository.getGlobalState();
                UserSpecificSettings user = settingsRepository.getUserSpecificSettings();
                globalSettingsData.postValue(global);
                userSettingsData.postValue(user);
                errorLiveData.postValue(null);
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setGlobalTheme(int themeMode) {
        new Thread(() -> {
            try {
                settingsRepository.setTheme(themeMode);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setLanguage(LanguageOption language) {
        new Thread(() -> {
            try {
                settingsRepository.setLanguage(language);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setUnitSystem(UnitSystem unitSystem) {
        new Thread(() -> {
            try {
                settingsRepository.setUnitSystem(unitSystem);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setAiProvider(AiProvider provider) {
        new Thread(() -> {
            try {
                settingsRepository.setAiProvider(provider);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setProviderApiKey(AiProvider provider, String apiKey) {
        new Thread(() -> {
            try {
                settingsRepository.setProviderApiKey(provider, apiKey);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setProviderModel(AiProvider provider, String model) {
        new Thread(() -> {
            try {
                settingsRepository.setProviderModel(provider, model);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setAiWebSearchEnabled(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setAiWebSearchEnabled(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setPrivateModeEnabled(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setPrivateModeEnabled(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void resetAllAppData(Context context) {
        new Thread(() -> {
            try {
                settingsRepository.resetAllAppData(context);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setUserThemeOverride(Integer theme) {
        new Thread(() -> {
            try {
                settingsRepository.setUserThemeOverride(theme);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setPreferredAiOverride(AiProvider provider) {
        new Thread(() -> {
            try {
                settingsRepository.setPreferredAiOverride(provider);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setPrivateModeOverride(Boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setPrivateModeOverride(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setAiCitationsRequired(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setAiCitationsRequired(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setAiFallbackEnabled(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setAiFallbackEnabled(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setAssistantMemory(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setAssistantMemory(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setAssistantEntryFromShareEnabled(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setAssistantEntryFromShareEnabled(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setAssistantEntryFromTextSelectionEnabled(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setAssistantEntryFromTextSelectionEnabled(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setLanguageOverride(LanguageOption language) {
        new Thread(() -> {
            try {
                settingsRepository.setLanguageOverride(language);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setUnitsOverride(UnitSystem units) {
        new Thread(() -> {
            try {
                settingsRepository.setUnitsOverride(units);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setUserAiProfileContext(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setUserAiProfileContext(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setUserAiMedicationContext(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setUserAiMedicationContext(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public void setUserAiLogContext(boolean enabled) {
        new Thread(() -> {
            try {
                settingsRepository.setUserAiLogContext(enabled);
                loadSettings();
            } catch (Exception e) {
                errorLiveData.postValue(e.getMessage());
            }
        }).start();
    }

    public ProviderSettings getProviderSettings(AiProvider provider) {
        return settingsRepository.getProviderSettings(provider);
    }
}
