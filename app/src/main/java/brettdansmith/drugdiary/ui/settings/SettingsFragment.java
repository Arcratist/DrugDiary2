package brettdansmith.drugdiary.ui.settings;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.MainActivity;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileAuthRegistry;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileRepository;
import brettdansmith.drugdiary.data.settings.AiProvider;
import brettdansmith.drugdiary.data.settings.LanguageOption;
import brettdansmith.drugdiary.data.settings.ProviderSettings;
import brettdansmith.drugdiary.data.settings.SettingsRepository;
import brettdansmith.drugdiary.data.settings.SettingsState;
import brettdansmith.drugdiary.data.settings.TimeFormat;
import brettdansmith.drugdiary.data.settings.UnitSystem;
import brettdansmith.drugdiary.databinding.FragmentSettingsBinding;
import brettdansmith.drugdiary.ui.assistant.AssistantViewModel;
import brettdansmith.drugdiary.ui.auth.ProfileSelectorFragment;
import brettdansmith.drugdiary.util.JsonUtils;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SettingsRepository settings;
    private AiProvider currentProvider = AiProvider.OPENAI;
    private boolean bindingProviderFields;
    private boolean bindingProfilePrivacy;
    private String profileName = "";
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        settings = new SettingsRepository(requireContext());
        SettingsState state = settings.getState();
        currentProvider = state.assistantProvider;

        bindTheme(state);
        bindLanguage(state);
        bindSwitches(state);
        bindNumberFields(state);
        bindProviderPicker();
        bindProviderTextFields();
        refreshProviderFields();
        bindProfilePrivacyAndSecurity();

        binding.btnAboutLegal.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).showWelcomeDialog(false);
            }
        });
        binding.btnResetProviderModel.setOnClickListener(v -> resetProviderModel());
        binding.btnClearAssistantHistory.setOnClickListener(v -> clearAssistantHistoryAsync());
    }

    private void bindTheme(SettingsState state) {
        if (state.themeMode == AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) binding.radioSystem.setChecked(true);
        else if (state.themeMode == AppCompatDelegate.MODE_NIGHT_NO) binding.radioLight.setChecked(true);
        else if (state.themeMode == AppCompatDelegate.MODE_NIGHT_YES) binding.radioDark.setChecked(true);

        binding.radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            int newTheme = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
            if (checkedId == R.id.radio_light) newTheme = AppCompatDelegate.MODE_NIGHT_NO;
            else if (checkedId == R.id.radio_dark) newTheme = AppCompatDelegate.MODE_NIGHT_YES;
            settings.setThemeMode(newTheme);
        });
    }

    private void bindLanguage(SettingsState state) {
        String[] labels = {
                getString(R.string.language_system),
                getString(R.string.language_english),
                getString(R.string.language_spanish)
        };
        LanguageOption[] options = {LanguageOption.SYSTEM, LanguageOption.ENGLISH, LanguageOption.SPANISH};
        binding.inputLanguage.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        int selected = indexOf(options, state.language);
        binding.inputLanguage.setText(labels[selected], false);
        binding.inputLanguage.setOnItemClickListener((parent, view, position, id) -> settings.setLanguage(options[position]));
    }

    private void bindSwitches(SettingsState state) {
        binding.switchMetric.setChecked(state.unitSystem == UnitSystem.METRIC);
        binding.switchMetric.setOnCheckedChangeListener((v, checked) -> settings.setUnitSystem(checked ? UnitSystem.METRIC : UnitSystem.IMPERIAL));

        binding.switch24hr.setChecked(state.timeFormat == TimeFormat.TWENTY_FOUR_HOUR);
        binding.switch24hr.setOnCheckedChangeListener((v, checked) -> settings.setTimeFormat(checked ? TimeFormat.TWENTY_FOUR_HOUR : TimeFormat.TWELVE_HOUR));

        binding.switchNotifications.setChecked(state.notificationsEnabled);
        binding.switchNotifications.setOnCheckedChangeListener((v, checked) -> {
            settings.setNotificationsEnabled(checked);
            if (checked) requestNotificationPermissionIfNeeded();
        });

        binding.switchStealthMode.setChecked(state.stealthNotifications);
        binding.switchStealthMode.setOnCheckedChangeListener((v, checked) -> settings.setStealthNotifications(checked));

        binding.switchBiometric.setChecked(state.biometricUnlock);
        binding.switchBiometric.setOnCheckedChangeListener((v, checked) -> settings.setBiometricUnlock(checked));

        binding.switchAutoLock.setChecked(state.autoLock);
        binding.switchAutoLock.setOnCheckedChangeListener((v, checked) -> settings.setAutoLock(checked));

        binding.switchDefaultSixDigitPin.setChecked(state.defaultSixDigitPin);
        binding.switchDefaultSixDigitPin.setOnCheckedChangeListener((v, checked) -> settings.setDefaultSixDigitPin(checked));

        binding.switchProfileSetupGuidance.setChecked(state.showProfileSetupGuidance);
        binding.switchProfileSetupGuidance.setOnCheckedChangeListener((v, checked) -> settings.setShowProfileSetupGuidance(checked));

        binding.switchAiMemory.setChecked(state.assistantMemory);
        binding.switchAiMemory.setOnCheckedChangeListener((v, checked) -> settings.setAssistantMemory(checked));

        binding.switchAiProfileContext.setChecked(state.assistantProfileContext);
        binding.switchAiProfileContext.setOnCheckedChangeListener((v, checked) -> settings.setAssistantProfileContext(checked));

        binding.switchAiMedicationContext.setChecked(state.assistantMedicationContext);
        binding.switchAiMedicationContext.setOnCheckedChangeListener((v, checked) -> settings.setAssistantMedicationContext(checked));

        binding.switchAiLogContext.setChecked(state.assistantLogContext);
        binding.switchAiLogContext.setOnCheckedChangeListener((v, checked) -> settings.setAssistantLogContext(checked));

        binding.switchAiResponseNotifications.setChecked(state.assistantResponseNotifications);
        binding.switchAiResponseNotifications.setOnCheckedChangeListener((v, checked) -> {
            settings.setAssistantResponseNotifications(checked);
            if (checked) requestNotificationPermissionIfNeeded();
        });

        binding.switchAiWebSearch.setChecked(settings.isAiWebSearchEnabled());
        binding.switchAiWebSearch.setOnCheckedChangeListener((v, checked) -> settings.setAiWebSearchEnabled(checked));

        binding.switchAiRequireCitations.setChecked(settings.isAiCitationsRequired());
        binding.switchAiRequireCitations.setOnCheckedChangeListener((v, checked) -> settings.setAiCitationsRequired(checked));

        binding.switchAiFallback.setChecked(settings.isAiFallbackEnabled());
        binding.switchAiFallback.setOnCheckedChangeListener((v, checked) -> settings.setAiFallbackEnabled(checked));
    }

    private void requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return;
        if (requireContext().checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) return;
        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 42);
    }

    private void bindProfilePrivacyAndSecurity() {
        SharedPreferences prefs = requireContext().getApplicationContext()
                .getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
        profileName = prefs.getString(ProfileSelectorFragment.KEY_LAST_PROFILE, "");

        loadProfilePrivacy();
        binding.switchShowOnDashboard.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingProfilePrivacy) saveProfilePrivacyAsync(checked);
        });
        binding.switchPrivateMode.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingProfilePrivacy) settings.setPrivateModeEnabled(checked);
        });
        binding.switchHideDashboardSensitive.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingProfilePrivacy) settings.setHideDashboardSensitive(checked);
        });

        binding.btnChangePin.setOnClickListener(v -> showChangePinDialog());
        binding.btnDeleteProfile.setOnClickListener(v -> showDeleteProfileDialog());
    }

    private void loadProfilePrivacy() {
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                JSONObject privacy = data == null ? new JSONObject() : data.optJSONObject(ProfileJson.KEY_PRIVACY);
                if (privacy == null) privacy = new JSONObject();
                boolean showDashboard = privacy.optBoolean("show_on_dashboard", true);
                mainHandler.post(() -> {
                    if (binding == null) return;
                    bindingProfilePrivacy = true;
                    binding.switchShowOnDashboard.setChecked(showDashboard);
                    binding.switchPrivateMode.setChecked(settings.isPrivateModeEnabled());
                    binding.switchHideDashboardSensitive.setChecked(settings.hideDashboardSensitive());
                    bindingProfilePrivacy = false;
                });
            } catch (Exception ignored) {
            }
        });
    }

    private void saveProfilePrivacyAsync(boolean showDashboard) {
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                if (data == null) return;
                JSONObject privacy = JsonUtils.object(data, ProfileJson.KEY_PRIVACY);
                privacy.put("show_on_dashboard", showDashboard);
                EncryptedProfileStore.saveProfileData(appContext, data);
            } catch (Exception ignored) {
            }
        });
    }

    private void bindNumberFields(SettingsState state) {
        binding.inputAutoLockTimeout.setText(String.valueOf(state.autoLockMinutes));
        binding.inputReferenceCacheDays.setText(String.valueOf(state.referenceCacheDays));
        binding.inputAutoLockTimeout.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                settings.setAutoLockMinutes(parsePositiveInt(s.toString(), state.autoLockMinutes));
            }
        });
        binding.inputReferenceCacheDays.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                settings.setReferenceCacheDays(parsePositiveInt(s.toString(), state.referenceCacheDays));
            }
        });
    }

    private void bindProviderPicker() {
        AiProvider[] providers = AiProvider.values();
        String[] labels = new String[providers.length];
        for (int i = 0; i < providers.length; i++) labels[i] = providers[i].displayName();
        binding.inputAiProvider.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, labels));
        binding.inputAiProvider.setText(labels[indexOf(providers, currentProvider)], false);
        binding.inputAiProvider.setOnItemClickListener((parent, view, position, id) -> {
            currentProvider = providers[position];
            settings.setAiProvider(currentProvider);
            refreshProviderFields();
        });
    }

    private void bindProviderTextFields() {
        binding.inputActiveApiKey.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (!bindingProviderFields) settings.setProviderApiKey(currentProvider, s.toString());
            }
        });
        binding.inputActiveModel.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (!bindingProviderFields) settings.setProviderModel(currentProvider, s.toString());
            }
        });
        binding.inputActiveBaseUrl.addTextChangedListener(new SimpleTextWatcher() {
            @Override public void afterTextChanged(Editable s) {
                if (!bindingProviderFields) settings.setProviderBaseUrl(currentProvider, s.toString());
            }
        });
        binding.switchProviderEnabled.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingProviderFields) settings.setProviderEnabled(currentProvider, checked);
        });
        binding.switchProviderStreaming.setOnCheckedChangeListener((v, checked) -> {
            if (!bindingProviderFields) settings.setProviderStreamingEnabled(currentProvider, checked);
        });
    }

    private void refreshProviderFields() {
        ProviderSettings providerSettings = settings.getProviderSettings(currentProvider);
        binding.textProviderSettingsTitle.setText(getString(R.string.provider_configuration, currentProvider.displayName()));
        bindingProviderFields = true;
        binding.inputActiveApiKey.setText(providerSettings.apiKey);
        binding.inputActiveModel.setText(providerSettings.model);
        binding.inputActiveBaseUrl.setText(providerSettings.baseUrl);
        binding.switchProviderEnabled.setChecked(providerSettings.enabled);
        binding.switchProviderStreaming.setChecked(providerSettings.allowStreaming);
        bindingProviderFields = false;
    }

    private void resetProviderModel() {
        settings.setProviderModel(currentProvider, currentProvider.defaultModel());
        refreshProviderFields();
    }

    private void clearAssistantHistoryAsync() {
        Context appContext = requireContext().getApplicationContext();
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                if (data == null) throw new IllegalStateException();
                data.put(ProfileJson.KEY_ASSISTANT_CHATS, new org.json.JSONArray());
                data.put(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, "");
                EncryptedProfileStore.saveProfileData(appContext, data);
                mainHandler.post(() -> {
                    if (binding == null) return;
                    new ViewModelProvider(requireActivity()).get(AssistantViewModel.class).clear();
                    Toast.makeText(getContext(), R.string.assistant_history_cleared, Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(), R.string.assistant_history_clear_failed, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private int parsePositiveInt(String value, int fallback) {
        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private <T> int indexOf(T[] values, T target) {
        for (int i = 0; i < values.length; i++) {
            if (values[i] == target || values[i].equals(target)) return i;
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

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mainHandler.removeCallbacksAndMessages(null);
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        diskExecutor.shutdownNow();
    }
}




