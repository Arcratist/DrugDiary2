package brettdansmith.drugdiary.ui.settings;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.databinding.FragmentGlobalSettingsBinding;

public class GlobalSettingsFragment extends Fragment {
    private FragmentGlobalSettingsBinding binding;
    private SettingsRepository settings;
    private ActivityResultLauncher<Intent> authenticationLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        authenticationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                        showFinalResetConfirmation();
                    } else {
                        Toast.makeText(getContext(), R.string.settings_reset_auth_failed, Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentGlobalSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = new SettingsRepository(requireContext());
        
        populateUI();

        binding.btnAppReset.setOnClickListener(v -> requestResetAuthentication());
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void populateUI() {
        SettingsState state = settings.getState();

        // --- Language Dropdown ---
        String[] langLabels = { getString(R.string.language_system), getString(R.string.language_english), getString(R.string.language_spanish) };
        LanguageOption[] langOptions = { LanguageOption.SYSTEM, LanguageOption.ENGLISH, LanguageOption.SPANISH };
        NoFilterArrayAdapter<String> langAdapter = new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, langLabels);
        binding.inputGlobalLanguage.setAdapter(langAdapter);
        final String langText = langLabels[indexOf(langOptions, state.language)];
        binding.inputGlobalLanguage.post(() -> binding.inputGlobalLanguage.setText(langText, false));
        binding.inputGlobalLanguage.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputGlobalLanguage.post(() -> {
                binding.inputGlobalLanguage.dismissDropDown();
                binding.inputGlobalLanguage.clearFocus();
                settings.setLanguage(langOptions[position]);
            });
        });

        // --- Units Dropdown ---
        String[] unitLabels = { getString(R.string.unit_system_system), getString(R.string.unit_metric), getString(R.string.unit_imperial) };
        UnitSystem[] unitOptions = { UnitSystem.SYSTEM, UnitSystem.METRIC, UnitSystem.IMPERIAL };
        NoFilterArrayAdapter<String> unitAdapter = new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, unitLabels);
        binding.inputGlobalUnits.setAdapter(unitAdapter);
        int unitSelected = 0;
        if (state.unitSystem == UnitSystem.METRIC) unitSelected = 1;
        else if (state.unitSystem == UnitSystem.IMPERIAL) unitSelected = 2;
        final String unitText = unitLabels[unitSelected];
        binding.inputGlobalUnits.post(() -> binding.inputGlobalUnits.setText(unitText, false));
        binding.inputGlobalUnits.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputGlobalUnits.post(() -> {
                binding.inputGlobalUnits.dismissDropDown();
                binding.inputGlobalUnits.clearFocus();
                settings.setUnitSystem(unitOptions[position]);
            });
        });
        
        // --- Theme Dropdown ---
        String[] themeLabels = { getString(R.string.theme_system), getString(R.string.theme_light), getString(R.string.theme_dark) };
        int[] themeOptions = { AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES };
        NoFilterArrayAdapter<String> themeAdapter = new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, themeLabels);
        binding.inputGlobalTheme.setAdapter(themeAdapter);
        int themeIndex = 0;
        if (state.themeMode == AppCompatDelegate.MODE_NIGHT_NO) themeIndex = 1;
        else if (state.themeMode == AppCompatDelegate.MODE_NIGHT_YES) themeIndex = 2;
        final String themeText = themeLabels[themeIndex];
        binding.inputGlobalTheme.post(() -> binding.inputGlobalTheme.setText(themeText, false));
        binding.inputGlobalTheme.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputGlobalTheme.post(() -> {
                binding.inputGlobalTheme.dismissDropDown();
                binding.inputGlobalTheme.clearFocus();
                settings.setThemeMode(themeOptions[position]);
            });
        });

        // --- AI Configuration ---
        bindAiConfiguration(state);

        // --- Notifications ---
        binding.switchNotificationsEnabled.setChecked(state.notificationsEnabled);
        binding.switchStealthNotifications.setChecked(state.stealthNotifications);
        binding.switchAssistantResponseNotifications.setChecked(state.assistantResponseNotifications);
        binding.switchNotificationsEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setNotificationsEnabled(isChecked));
        binding.switchStealthNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setStealthNotifications(isChecked));
        binding.switchAssistantResponseNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> settings.setAssistantResponseNotifications(isChecked));

        // --- Popup Visibility ---
        boolean suppressWelcome = settings.rawPrefs().getBoolean("dont_show_welcome", false);
        binding.switchShowWelcomeDialog.setChecked(!suppressWelcome);
        binding.switchShowWelcomeDialog.setOnCheckedChangeListener((buttonView, show) -> {
            long version = getVersionCode();
            settings.rawPrefs().edit()
                    .putBoolean("dont_show_welcome", !show)
                    .putLong("welcome_version", version)
                    .apply();
        });

        binding.switchShowProfileSetupGuidance.setChecked(state.showProfileSetupGuidance);
        binding.switchShowProfileSetupGuidance.setOnCheckedChangeListener((buttonView, show) ->
                settings.setShowProfileSetupGuidance(show));
    }

    private void bindAiConfiguration(SettingsState state) {
        AiProvider[] providers = AiProvider.values();
        String[] providerLabels = new String[providers.length];
        for (int i = 0; i < providers.length; i++) {
            providerLabels[i] = providers[i].displayName();
        }

        NoFilterArrayAdapter<String> providerAdapter = new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, providerLabels);
        binding.inputAiProvider.setAdapter(providerAdapter);

        // Set initial selection
        int providerIndex = indexOf(providers, state.assistantProvider);
        binding.inputAiProvider.post(() -> binding.inputAiProvider.setText(providerLabels[providerIndex], false));

        // Load current provider's model and API key
        loadProviderFields(state.assistantProvider);

        binding.inputAiProvider.setOnItemClickListener((parent, view, position, id) -> {
            binding.inputAiProvider.post(() -> {
                binding.inputAiProvider.dismissDropDown();
                binding.inputAiProvider.clearFocus();
                AiProvider selected = providers[position];
                settings.setAiProvider(selected);
                loadProviderFields(selected);
            });
        });

        // Save model on focus loss or editor action
        binding.inputAiModel.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveCurrentModel();
        });
        binding.inputAiModel.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCurrentModel();
                binding.inputAiModel.clearFocus();
                return true;
            }
            return false;
        });

        // Save API key on focus loss or editor action
        binding.inputAiApiKey.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveCurrentApiKey();
        });
        binding.inputAiApiKey.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCurrentApiKey();
                binding.inputAiApiKey.clearFocus();
                return true;
            }
            return false;
        });

        // AI Switches
        binding.switchAiMemory.setChecked(state.assistantMemory);
        binding.switchAiWebSearch.setChecked(settings.isAiWebSearchEnabled());
        binding.switchAiRequireCitations.setChecked(settings.isAiCitationsRequired());
        binding.switchAiFallback.setChecked(settings.isAiFallbackEnabled());

        binding.switchAiMemory.setOnCheckedChangeListener((v, checked) -> settings.setAssistantMemory(checked));
        binding.switchAiWebSearch.setOnCheckedChangeListener((v, checked) -> settings.setAiWebSearchEnabled(checked));
        binding.switchAiRequireCitations.setOnCheckedChangeListener((v, checked) -> settings.setAiCitationsRequired(checked));
        binding.switchAiFallback.setOnCheckedChangeListener((v, checked) -> settings.setAiFallbackEnabled(checked));
        
        // Hide global context switches from view since they are removed from logic
        binding.switchAiProfileContext.setVisibility(View.GONE);
        binding.switchAiMedicationContext.setVisibility(View.GONE);
        binding.switchAiLogContext.setVisibility(View.GONE);
    }

    private void loadProviderFields(AiProvider provider) {
        ProviderSettings ps = settings.getProviderSettings(provider);
        binding.inputAiModel.setText(ps.model);
        binding.layoutAiModel.setHelperText("Default: " + provider.defaultModel());

        // Show masked API key if one exists, otherwise leave empty
        String apiKey = ps.apiKey;
        if (apiKey != null && !apiKey.trim().isEmpty()) {
            binding.inputAiApiKey.setText(apiKey);
        } else {
            binding.inputAiApiKey.setText("");
        }
    }

    private void saveCurrentModel() {
        if (binding == null) return;
        AiProvider currentProvider = settings.getState().assistantProvider;
        String model = binding.inputAiModel.getText() != null ? binding.inputAiModel.getText().toString().trim() : "";
        if (model.isEmpty()) {
            model = currentProvider.defaultModel();
            binding.inputAiModel.setText(model);
            Toast.makeText(getContext(), R.string.ai_model_reset_to_default, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.ai_model_saved, Toast.LENGTH_SHORT).show();
        }
        settings.setProviderModel(currentProvider, model);
    }

    private void saveCurrentApiKey() {
        if (binding == null) return;
        AiProvider currentProvider = settings.getState().assistantProvider;
        String apiKey = binding.inputAiApiKey.getText() != null ? binding.inputAiApiKey.getText().toString().trim() : "";
        settings.setProviderApiKey(currentProvider, apiKey);
        if (apiKey.isEmpty()) {
            Toast.makeText(getContext(), R.string.ai_api_key_cleared, Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(getContext(), R.string.ai_api_key_saved, Toast.LENGTH_SHORT).show();
        }
    }
    
    private void requestResetAuthentication() {
        KeyguardManager km = (KeyguardManager) requireContext().getSystemService(Context.KEYGUARD_SERVICE);
        if (km != null && km.isDeviceSecure()) {
            Intent authIntent = km.createConfirmDeviceCredentialIntent(getString(R.string.settings_reset_auth_title), getString(R.string.settings_reset_auth_body));
            if (authIntent != null) {
                authenticationLauncher.launch(authIntent);
            } else {
                showFinalResetConfirmation();
            }
        } else {
            showFinalResetConfirmation();
        }
    }

    private void showFinalResetConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.settings_reset_confirm_title)
                .setMessage(R.string.settings_reset_confirm_body)
                .setPositiveButton(R.string.settings_reset_confirm_btn, (dialog, which) -> {
                    settings.resetAllAppData(requireContext());
                    Toast.makeText(getContext(), R.string.settings_reset_success, Toast.LENGTH_LONG).show();
                    if (getActivity() != null) {
                        getActivity().finishAffinity();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    private <T> int indexOf(T[] values, T target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target || (values[i] != null && values[i].equals(target))) return i;
        }
        return 0;
    }

    private long getVersionCode() {
        try {
            PackageInfo pInfo = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P ? pInfo.getLongVersionCode() : pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1L;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
