package brettdansmith.drugdiary.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.MainActivity;
import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileAuthRegistry;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileRepository;
import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsResolver;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.data.settings.UserSpecificSettings;
import brettdansmith.drugdiary.data.settings.EffectiveSettings;
import brettdansmith.drugdiary.databinding.FragmentUserSpecificSettingsBinding;
import brettdansmith.drugdiary.ui.assistant.AssistantViewModel;
import brettdansmith.drugdiary.ui.auth.ProfileSelectorFragment;
import brettdansmith.drugdiary.util.JsonUtils;

public class UserSpecificSettingsFragment extends Fragment {
    private FragmentUserSpecificSettingsBinding binding;
    private SettingsRepository globalSettings;
    private String profileName = "";
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isBinding = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserSpecificSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        globalSettings = new SettingsRepository(requireContext());
        
        SharedPreferences prefs = requireContext().getApplicationContext()
                .getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
        profileName = prefs.getString(ProfileSelectorFragment.KEY_LAST_PROFILE, "");

        loadAndBindSettings();

        binding.btnChangePin.setOnClickListener(v -> showChangePinDialog());
        binding.btnDeleteProfile.setOnClickListener(v -> showDeleteProfileDialog());
        binding.btnClearAssistantHistory.setOnClickListener(v -> clearAssistantHistoryAsync());
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void loadAndBindSettings() {
        isBinding = true;
        UserSpecificSettings userSettings = SettingsResolver.getUserSpecificSettings(requireContext());
        SettingsState global = globalSettings.getState();
        EffectiveSettings effective = EffectiveSettings.resolve(global, userSettings);

        bindTheme(userSettings, global);
        bindPreferredAi(userSettings, global);
        bindLanguage(userSettings, global);
        bindUnits(userSettings, global);
        bindSwitches(userSettings, effective);
        isBinding = false;
    }

    private void bindTheme(UserSpecificSettings user, SettingsState global) {
        String globalLabel = getString(R.string.language_system);
        if (global.themeMode == AppCompatDelegate.MODE_NIGHT_NO) globalLabel = getString(R.string.theme_light);
        else if (global.themeMode == AppCompatDelegate.MODE_NIGHT_YES) globalLabel = getString(R.string.theme_dark);

        String[] labels = {
                getString(R.string.default_with_value, globalLabel),
                getString(R.string.theme_light),
                getString(R.string.theme_dark),
                getString(R.string.language_system)
        };
        Integer[] values = {null, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM};
        
        binding.inputThemeOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        int selected = 0;
        if (user.themeOverride != null) {
            for (int i = 1; i < values.length; i++) {
                if (values[i].equals(user.themeOverride)) {
                    selected = i;
                    break;
                }
            }
        }
        binding.inputThemeOverride.setText(labels[selected], false);
        binding.inputThemeOverride.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputThemeOverride.post(() -> {
                binding.inputThemeOverride.dismissDropDown();
                binding.inputThemeOverride.clearFocus();
                saveTheme(values[position]);
            });
        });
    }

    private void bindPreferredAi(UserSpecificSettings user, SettingsState global) {
        String globalLabel = global.assistantProvider.displayName();
        AiProvider[] allProviders = AiProvider.values();

        // Build labels: "Default (current global)" + each provider
        String[] labels = new String[allProviders.length + 1];
        labels[0] = getString(R.string.default_with_value, globalLabel);
        AiProvider[] values = new AiProvider[allProviders.length + 1];
        values[0] = null;
        for (int i = 0; i < allProviders.length; i++) {
            labels[i + 1] = allProviders[i].displayName();
            values[i + 1] = allProviders[i];
        }

        binding.inputPreferredAiOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        int selected = 0;
        if (user.preferredAiOverride != null) {
            for (int i = 1; i < values.length; i++) {
                if (values[i] == user.preferredAiOverride) {
                    selected = i;
                    break;
                }
            }
        }
        binding.inputPreferredAiOverride.setText(labels[selected], false);
        binding.inputPreferredAiOverride.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputPreferredAiOverride.post(() -> {
                binding.inputPreferredAiOverride.dismissDropDown();
                binding.inputPreferredAiOverride.clearFocus();
                savePreferredAi(values[position]);
            });
        });
    }

    private void bindLanguage(UserSpecificSettings user, SettingsState global) {
        String globalLabel = global.language.displayName();
        String[] labels = {
                getString(R.string.default_with_value, globalLabel),
                getString(R.string.language_english),
                getString(R.string.language_spanish)
        };
        LanguageOption[] values = {null, LanguageOption.ENGLISH, LanguageOption.SPANISH};

        binding.inputLanguageOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        int selected = 0;
        if (user.languageOverride != null) {
            for (int i = 1; i < values.length; i++) {
                if (values[i] == user.languageOverride) {
                    selected = i;
                    break;
                }
            }
        }
        binding.inputLanguageOverride.setText(labels[selected], false);
        binding.inputLanguageOverride.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputLanguageOverride.post(() -> {
                binding.inputLanguageOverride.dismissDropDown();
                binding.inputLanguageOverride.clearFocus();
                saveLanguage(values[position]);
            });
        });
    }

    private void bindUnits(UserSpecificSettings user, SettingsState global) {
        String globalLabel = (global.unitSystem == UnitSystem.METRIC) ? getString(R.string.unit_metric) : getString(R.string.unit_imperial);
        String[] labels = {
                getString(R.string.default_with_value, globalLabel),
                getString(R.string.unit_metric),
                getString(R.string.unit_imperial)
        };
        UnitSystem[] values = {null, UnitSystem.METRIC, UnitSystem.IMPERIAL};

        binding.inputUnitsOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        int selected = 0;
        if (user.unitsOverride != null) {
            for (int i = 1; i < values.length; i++) {
                if (values[i] == user.unitsOverride) {
                    selected = i;
                    break;
                }
            }
        }
        binding.inputUnitsOverride.setText(labels[selected], false);
        binding.inputUnitsOverride.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputUnitsOverride.post(() -> {
                binding.inputUnitsOverride.dismissDropDown();
                binding.inputUnitsOverride.clearFocus();
                saveUnits(values[position]);
            });
        });
    }

    private void bindSwitches(UserSpecificSettings user, EffectiveSettings effective) {
        binding.switchPrivateMode.setChecked(effective.privateMode);
        binding.switchAiProfileContext.setChecked(user.aiProfileContext);
        binding.switchAiMedicationContext.setChecked(user.aiMedicationContext);
        binding.switchAiLogContext.setChecked(user.aiLogContext);

        binding.switchPrivateMode.setOnCheckedChangeListener((v, checked) -> {
            if (!isBinding) savePrivateMode(checked);
        });
        binding.switchAiProfileContext.setOnCheckedChangeListener((v, checked) -> {
            if (!isBinding) saveAiProfileContext(checked);
        });
        binding.switchAiMedicationContext.setOnCheckedChangeListener((v, checked) -> {
            if (!isBinding) saveAiMedicationContext(checked);
        });
        binding.switchAiLogContext.setOnCheckedChangeListener((v, checked) -> {
            if (!isBinding) saveAiLogContext(checked);
        });

        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(requireContext().getApplicationContext());
                JSONObject privacy = data.optJSONObject(ProfileJson.KEY_PRIVACY);
                if (privacy == null) privacy = new JSONObject();
                boolean showDashboard = privacy.optBoolean("show_on_dashboard", true);
                
                mainHandler.post(() -> {
                    if (binding == null) return;
                    isBinding = true;
                    binding.switchShowOnDashboard.setChecked(showDashboard);
                    binding.switchHideDashboardSensitive.setChecked(effective.hideDashboardSensitive);
                    
                    binding.switchShowOnDashboard.setOnCheckedChangeListener((v, checked) -> {
                        if (!isBinding) saveProfilePrivacyAsync(checked);
                    });
                    binding.switchHideDashboardSensitive.setOnCheckedChangeListener((v, checked) -> {
                        if (!isBinding) saveHideDashboardSensitive(checked);
                    });
                    isBinding = false;
                });
            } catch (Exception ignored) {}
        });
    }

    private void saveTheme(Integer theme) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(theme, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.hideDashboardSensitiveOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
        if (theme != null) {
            AppCompatDelegate.setDefaultNightMode(theme);
        } else {
            AppCompatDelegate.setDefaultNightMode(globalSettings.getState().themeMode);
        }
    }

    private void savePreferredAi(AiProvider provider) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.hideDashboardSensitiveOverride, provider, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }

    private void saveLanguage(LanguageOption lang) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, lang, current.unitsOverride, current.privateModeOverride, current.hideDashboardSensitiveOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }

    private void saveUnits(UnitSystem units) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, units, current.privateModeOverride, current.hideDashboardSensitiveOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }
    
    private void savePrivateMode(boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, enabled, current.hideDashboardSensitiveOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }

    private void saveHideDashboardSensitive(boolean hide) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, hide, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }

    private void saveAiProfileContext(boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.hideDashboardSensitiveOverride, current.preferredAiOverride, enabled, current.aiMedicationContext, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }

    private void saveAiMedicationContext(boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.hideDashboardSensitiveOverride, current.preferredAiOverride, current.aiProfileContext, enabled, current.aiLogContext);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }

    private void saveAiLogContext(boolean enabled) {
        UserSpecificSettings current = SettingsResolver.getUserSpecificSettings(requireContext());
        UserSpecificSettings updated = new UserSpecificSettings(current.themeOverride, current.languageOverride, current.unitsOverride, current.privateModeOverride, current.hideDashboardSensitiveOverride, current.preferredAiOverride, current.aiProfileContext, current.aiMedicationContext, enabled);
        SettingsResolver.saveUserSpecificSettings(requireContext(), updated);
    }

    private void saveProfilePrivacyAsync(boolean showDashboard) {
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                JSONObject privacy = JsonUtils.object(data, ProfileJson.KEY_PRIVACY);
                privacy.put("show_on_dashboard", showDashboard);
                EncryptedProfileStore.saveProfileData(appContext, data);
            } catch (Exception ignored) {}
        });
    }

    private void showChangePinDialog() {
        LinearLayout layout = pinDialogLayout(true);
        EditText current = (EditText) layout.findViewWithTag("current");
        EditText next = (EditText) layout.findViewWithTag("next");
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.change_pin)
                .setView(layout)
                .setPositiveButton(R.string.change_pin, (dialog, which) -> {
                    try {
                        String currentPin = current.getText().toString();
                        String nextPin = next.getText().toString();
                        if (!ProfileAuthRegistry.verifyPin(requireContext(), profileName, currentPin)) {
                            Toast.makeText(getContext(), "Current PIN is incorrect", Toast.LENGTH_LONG).show();
                            return;
                        }
                        if (!(nextPin.matches("\\d{4}") || nextPin.matches("\\d{6}"))) {
                            Toast.makeText(getContext(), "New PIN must be 4 or 6 digits", Toast.LENGTH_LONG).show();
                            return;
                        }
                        ProfileAuthRegistry.saveProfilePin(requireContext(), profileName, nextPin);
                        Toast.makeText(getContext(), "PIN changed. Please log in again.", Toast.LENGTH_LONG).show();
                        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).logout();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Could not change PIN", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void showDeleteProfileDialog() {
        LinearLayout layout = pinDialogLayout(false);
        EditText current = (EditText) layout.findViewWithTag("current");
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.delete_profile)
                .setMessage("This permanently deletes this profile vault from local storage.")
                .setView(layout)
                .setPositiveButton(R.string.delete_profile, (dialog, which) -> {
                    try {
                        if (!ProfileAuthRegistry.verifyPin(requireContext(), profileName, current.getText().toString())) {
                            Toast.makeText(getContext(), "PIN is incorrect", Toast.LENGTH_LONG).show();
                            return;
                        }
                        new ProfileRepository(requireContext()).deleteProfile(profileName, current.getText().toString());
                        Toast.makeText(getContext(), "Profile deleted", Toast.LENGTH_LONG).show();
                        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).logout();
                    } catch (Exception e) {
                        Toast.makeText(getContext(), "Could not delete profile", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private LinearLayout pinDialogLayout(boolean includeNewPin) {
        LinearLayout layout = new LinearLayout(requireContext());
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        layout.setPadding(padding, padding, padding, 0);
        EditText current = new EditText(requireContext());
        current.setTag("current");
        current.setHint("Current PIN");
        current.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        layout.addView(current);
        if (includeNewPin) {
            EditText next = new EditText(requireContext());
            next.setTag("next");
            next.setHint("New 4 or 6 digit PIN");
            next.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            layout.addView(next);
        }
        return layout;
    }

    private void clearAssistantHistoryAsync() {
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                data.put(ProfileJson.KEY_ASSISTANT_CHATS, new org.json.JSONArray());
                data.put(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, "");
                EncryptedProfileStore.saveProfileData(appContext, data);
                mainHandler.post(() -> {
                    if (binding == null) return;
                    new ViewModelProvider(requireActivity()).get(AssistantViewModel.class).clear();
                    Toast.makeText(getContext(), R.string.assistant_history_cleared, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(getContext(), "Failed to clear history", Toast.LENGTH_SHORT).show());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        diskExecutor.shutdownNow();
    }
}
