package brettdansmith.drugdiary.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
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

import brettdansmith.drugdiary.app.MainActivity;
import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.ProfileAuthRegistry;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileRepository;
import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.data.settings.UserSpecificSettings;
import brettdansmith.drugdiary.data.settings.EffectiveSettings;
import brettdansmith.drugdiary.databinding.FragmentUserSpecificSettingsBinding;
import brettdansmith.drugdiary.ui.common.ViewModelFactory;
import brettdansmith.drugdiary.ui.auth.ProfileSelectorFragment;

public class UserSpecificSettingsFragment extends Fragment {
    private FragmentUserSpecificSettingsBinding binding;
    private SettingsViewModel viewModel;
    private String profileName = "";
    private boolean isBinding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUserSpecificSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        viewModel = new ViewModelProvider(this, new ViewModelFactory(requireContext()))
                .get(SettingsViewModel.class);

        SharedPreferences prefs = requireContext().getApplicationContext()
                .getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
        profileName = prefs.getString(ProfileSelectorFragment.KEY_LAST_PROFILE, "");

        observeViewModel();

        binding.btnChangePin.setOnClickListener(v -> showChangePinDialog());
        binding.btnDeleteProfile.setOnClickListener(v -> showDeleteProfileDialog());
        binding.btnClearAssistantHistory.setOnClickListener(v -> clearAssistantHistory());
        binding.btnBack.setOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());
    }

    private void observeViewModel() {
        viewModel.getUserSettings().observe(getViewLifecycleOwner(), userSettings -> {
            SettingsState global = viewModel.getGlobalSettings().getValue();
            if (global != null && userSettings != null) {
                bindUI(userSettings, global);
            }
        });
        
        viewModel.getGlobalSettings().observe(getViewLifecycleOwner(), global -> {
            UserSpecificSettings user = viewModel.getUserSettings().getValue();
            if (global != null && user != null) {
                bindUI(user, global);
            }
        });
    }

    private void bindUI(UserSpecificSettings user, SettingsState global) {
        isBinding = true;
        EffectiveSettings effective = EffectiveSettings.resolve(global, user);
        
        // Theme
        String[] themeLabels = { "Default", "Light", "Dark", "System" };
        Integer[] themeValues = { null, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM };
        binding.inputThemeOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, themeLabels));
        binding.inputThemeOverride.setText(themeLabels[indexOf(themeValues, user.themeOverride)], false);
        binding.inputThemeOverride.setOnItemClickListener((p, v, pos, id) -> viewModel.setUserThemeOverride(themeValues[pos]));

        // AI provider override
        AiProvider[] providers = AiProvider.values();
        String[] providerLabels = new String[providers.length + 1];
        providerLabels[0] = "Default";
        for (int i = 0; i < providers.length; i++) {
            providerLabels[i + 1] = providers[i].displayName();
        }
        binding.inputPreferredAiOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, providerLabels));
        int providerIndex = user.preferredAiOverride == null ? 0 : (indexOf(providers, user.preferredAiOverride) + 1);
        binding.inputPreferredAiOverride.setText(providerLabels[Math.max(0, providerIndex)], false);
        binding.inputPreferredAiOverride.setOnItemClickListener((p, v, pos, id) -> {
            AiProvider selected = pos == 0 ? null : providers[pos - 1];
            viewModel.setPreferredAiOverride(selected);
        });

        // Language override
        LanguageOption[] languageOptions = { null, LanguageOption.SYSTEM, LanguageOption.ENGLISH, LanguageOption.SPANISH };
        String[] languageLabels = { "Default", getString(R.string.language_system), getString(R.string.language_english), getString(R.string.language_spanish) };
        binding.inputLanguageOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, languageLabels));
        binding.inputLanguageOverride.setText(languageLabels[indexOf(languageOptions, user.languageOverride)], false);
        binding.inputLanguageOverride.setOnItemClickListener((p, v, pos, id) -> viewModel.setLanguageOverride(languageOptions[pos]));

        // Units override
        UnitSystem[] unitOptions = { null, UnitSystem.SYSTEM, UnitSystem.METRIC, UnitSystem.IMPERIAL };
        String[] unitLabels = { "Default", getString(R.string.unit_system_system), getString(R.string.unit_metric), getString(R.string.unit_imperial) };
        binding.inputUnitsOverride.setAdapter(new NoFilterArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, unitLabels));
        binding.inputUnitsOverride.setText(unitLabels[indexOf(unitOptions, user.unitsOverride)], false);
        binding.inputUnitsOverride.setOnItemClickListener((p, v, pos, id) -> viewModel.setUnitsOverride(unitOptions[pos]));

        // Switches
        binding.switchPrivateMode.setChecked(effective.privateMode);
        binding.switchPrivateMode.setOnCheckedChangeListener((v, checked) -> {
            if (isBinding) return;
            viewModel.setPrivateModeOverride(checked);
        });
        binding.switchAiProfileContext.setChecked(user.aiProfileContext);
        binding.switchAiMedicationContext.setChecked(user.aiMedicationContext);
        binding.switchAiLogContext.setChecked(user.aiLogContext);
        binding.switchAiProfileContext.setOnCheckedChangeListener((v, checked) -> {
            if (isBinding) return;
            viewModel.setUserAiProfileContext(checked);
        });
        binding.switchAiMedicationContext.setOnCheckedChangeListener((v, checked) -> {
            if (isBinding) return;
            viewModel.setUserAiMedicationContext(checked);
        });
        binding.switchAiLogContext.setOnCheckedChangeListener((v, checked) -> {
            if (isBinding) return;
            viewModel.setUserAiLogContext(checked);
        });
        isBinding = false;
    }

    private <T> int indexOf(T[] values, T target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target || (values[i] != null && values[i].equals(target))) return i;
        }
        return 0;
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
                        ProfileAuthRegistry.saveProfilePin(requireContext(), profileName, nextPin);
                        Toast.makeText(getContext(), "PIN changed.", Toast.LENGTH_LONG).show();
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
                .setMessage("This permanently deletes this profile.")
                .setView(layout)
                .setPositiveButton(R.string.delete, (dialog, which) -> {
                    try {
                        if (!ProfileAuthRegistry.verifyPin(requireContext(), profileName, current.getText().toString())) {
                            Toast.makeText(getContext(), "PIN is incorrect", Toast.LENGTH_LONG).show();
                            return;
                        }
                        new ProfileRepository(requireContext()).deleteProfile(profileName, current.getText().toString());
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
            next.setHint("New PIN");
            next.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            layout.addView(next);
        }
        return layout;
    }

    private void clearAssistantHistory() {
        try {
            ProfileRepository repository = new ProfileRepository(requireContext());
            org.json.JSONObject data = repository.load();
            data.put(ProfileJson.KEY_ASSISTANT_CHATS, new org.json.JSONArray());
            data.put(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, "");
            repository.save(data);
            Toast.makeText(requireContext(), R.string.assistant_history_cleared, Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(requireContext(), R.string.assistant_history_clear_failed, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
