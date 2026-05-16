package brettdansmith.drugdiary.ui.settings;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
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
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.databinding.FragmentGlobalSettingsBinding;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;

public class GlobalSettingsFragment extends Fragment {
    private FragmentGlobalSettingsBinding binding;
    private SettingsViewModel viewModel;
    private ActivityResultLauncher<Intent> authenticationLauncher;
    private AiProvider selectedProvider = AiProvider.OPENAI;
    private boolean isBinding;

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
        
        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(SettingsViewModel.class);

        observeViewModel();

        binding.btnAppReset.setOnClickListener(v -> requestResetAuthentication());
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void observeViewModel() {
        viewModel.getGlobalSettings().observe(getViewLifecycleOwner(), state -> {
            if (state == null) return;
            populateUI(state);
        });

        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            if (error != null && !error.isEmpty()) {
                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateUI(SettingsState state) {
        isBinding = true;
        // --- Language Dropdown ---
        String[] langLabels = { getString(R.string.language_system), getString(R.string.language_english), getString(R.string.language_spanish) };
        LanguageOption[] langOptions = { LanguageOption.SYSTEM, LanguageOption.ENGLISH, LanguageOption.SPANISH };
        NoFilterArrayAdapter<String> langAdapter = new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, langLabels);
        binding.inputGlobalLanguage.setAdapter(langAdapter);
        final String langText = langLabels[indexOf(langOptions, state.language)];
        binding.inputGlobalLanguage.post(() -> binding.inputGlobalLanguage.setText(langText, false));
        binding.inputGlobalLanguage.setOnItemClickListener((parent, view, position, id) -> {
            viewModel.setLanguage(langOptions[position]);
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
            viewModel.setUnitSystem(unitOptions[position]);
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
            viewModel.setGlobalTheme(themeOptions[position]);
        });

        // --- AI Configuration ---
        bindAiConfiguration(state);

        // --- Notifications ---
        binding.switchNotificationsEnabled.setChecked(state.notificationsEnabled);
        binding.switchStealthNotifications.setChecked(state.stealthNotifications);
        binding.switchAssistantResponseNotifications.setChecked(state.assistantResponseNotifications);
        binding.switchNotificationsEnabled.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            new brettdansmith.drugdiary.data.settings.SettingsRepository(requireContext()).setNotificationsEnabled(checked);
        });
        binding.switchStealthNotifications.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            new brettdansmith.drugdiary.data.settings.SettingsRepository(requireContext()).setStealthNotifications(checked);
        });
        binding.switchAssistantResponseNotifications.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            new brettdansmith.drugdiary.data.settings.SettingsRepository(requireContext()).setAssistantResponseNotifications(checked);
        });
        isBinding = false;
    }

    private void bindAiConfiguration(SettingsState state) {
        AiProvider[] providers = AiProvider.values();
        String[] providerLabels = new String[providers.length];
        for (int i = 0; i < providers.length; i++) {
            providerLabels[i] = providers[i].displayName();
        }

        NoFilterArrayAdapter<String> providerAdapter = new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, providerLabels);
        binding.inputAiProvider.setAdapter(providerAdapter);

        int providerIndex = indexOf(providers, state.assistantProvider);
        selectedProvider = state.assistantProvider;
        binding.inputAiProvider.post(() -> binding.inputAiProvider.setText(providerLabels[providerIndex], false));

        loadProviderFields(state.assistantProvider);

        binding.inputAiProvider.setOnItemClickListener((parent, view, position, id) -> {
            selectedProvider = providers[position];
            viewModel.setAiProvider(selectedProvider);
            loadProviderFields(selectedProvider);
        });

        binding.inputAiModel.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCurrentModel();
                return true;
            }
            return false;
        });
        binding.inputAiModel.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveCurrentModel();
        });

        binding.inputAiApiKey.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                saveCurrentApiKey();
                return true;
            }
            return false;
        });
        binding.inputAiApiKey.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) saveCurrentApiKey();
        });

        binding.switchAiMemory.setChecked(state.assistantMemory);
        binding.switchAssistantEntryShare.setChecked(state.assistantEntryFromShareEnabled);
        binding.switchAssistantEntryTextSelection.setChecked(state.assistantEntryFromTextSelectionEnabled);
        binding.switchAiWebSearch.setChecked(new brettdansmith.drugdiary.data.settings.SettingsRepository(requireContext()).isAiWebSearchEnabled());
        binding.switchAiRequireCitations.setChecked(new brettdansmith.drugdiary.data.settings.SettingsRepository(requireContext()).isAiCitationsRequired());
        binding.switchAiFallback.setChecked(new brettdansmith.drugdiary.data.settings.SettingsRepository(requireContext()).isAiFallbackEnabled());

        binding.switchAiMemory.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            viewModel.setAssistantMemory(checked);
        });
        binding.switchAiWebSearch.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            viewModel.setAiWebSearchEnabled(checked);
        });
        binding.switchAiRequireCitations.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            viewModel.setAiCitationsRequired(checked);
        });
        binding.switchAiFallback.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            viewModel.setAiFallbackEnabled(checked);
        });
        binding.switchAssistantEntryShare.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            viewModel.setAssistantEntryFromShareEnabled(checked);
        });
        binding.switchAssistantEntryTextSelection.setOnCheckedChangeListener((buttonView, checked) -> {
            if (isBinding) return;
            viewModel.setAssistantEntryFromTextSelectionEnabled(checked);
        });
    }

    private void loadProviderFields(AiProvider provider) {
        ProviderSettings ps = viewModel.getProviderSettings(provider);
        binding.inputAiModel.setText(ps.model);
        binding.inputAiApiKey.setText(ps.apiKey);
    }

    private void saveCurrentModel() {
        String model = binding.inputAiModel.getText().toString().trim();
        viewModel.setProviderModel(selectedProvider, model);
    }

    private void saveCurrentApiKey() {
        String apiKey = binding.inputAiApiKey.getText().toString().trim();
        viewModel.setProviderApiKey(selectedProvider, apiKey);
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
                    viewModel.resetAllAppData(requireContext());
                    getActivity().finishAffinity();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private <T> int indexOf(T[] values, T target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target || (values[i] != null && values[i].equals(target))) return i;
        }
        return 0;
    }

    @Override
    public void onDestroyView() {
        if (binding != null) {
            saveCurrentModel();
            saveCurrentApiKey();
        }
        super.onDestroyView();
        binding = null;
    }
}
