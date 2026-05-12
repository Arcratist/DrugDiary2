package brettdansmith.drugdiary.ui.auth;

import brettdansmith.drugdiary.R;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.data.profile.ProfileAuthRegistry;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileRepository;
import brettdansmith.drugdiary.databinding.FragmentCreateProfileBinding;
import brettdansmith.drugdiary.domain.profile.ProfileValidator;
import brettdansmith.drugdiary.domain.units.UnitPreferences;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.domain.units.UnitConverter;
import brettdansmith.drugdiary.domain.validation.ProfileValidationResult;

public class CreateProfileFragment extends Fragment {

    private FragmentCreateProfileBinding binding;
    
    private static final String PREFIX_ICON = "icon_";
    private boolean useMetricUnits = true;
    private UnitPreferences unitPreferences;
    private Uri selectedAvatarUri;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private ActivityResultLauncher<String> avatarPicker;

    public CreateProfileFragment() {
        avatarPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null || binding == null) return;
            selectedAvatarUri = uri;
            binding.imageProfileIconPreview.setImageURI(uri);
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupDropdowns();
        setupUnits();
        binding.imageProfileIconPreview.setOnClickListener(v -> avatarPicker.launch("image/*"));
        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());

        // Show the info dialog when the fragment is first opened
        showAccuracyInfoDialog();
    }

    private void setupDropdowns() {
        String[] sexOptions = {"Male", "Female", "Other", "Prefer not to say"};
        ArrayAdapter<String> sexAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, sexOptions);
        binding.spinnerSex.setAdapter(sexAdapter);
    }

    private void setupUnits() {
        useMetricUnits = AppSettings.useMetric(requireContext());
        unitPreferences = UnitPreferences.from(requireContext());
        binding.layoutHeight.setHint(getString(R.string.height_hint_format, unitPreferences.heightLabel()));
        binding.layoutWeight.setHint(getString(R.string.weight_hint_format, unitPreferences.weightLabel()));
        binding.layoutSetPin.setHint(AppSettings.defaultSixDigitPin(requireContext())
                ? getString(R.string.security_pin_6)
                : getString(R.string.security_pin_4_or_6));
    }

    private void showAccuracyInfoDialog() {
        if (getContext() == null) return;

        SharedPreferences prefs = AppSettings.prefs(requireContext());
        if (!AppSettings.showProfileSetupGuidance(requireContext())) return;

        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_info, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                dialog.getWindow().getAttributes().setBlurBehindRadius(30);
            }
        }

        Button understandButton = dialogView.findViewById(R.id.button_understand);
        CheckBox dontShowCheckbox = dialogView.findViewById(R.id.checkbox_dont_show);

        understandButton.setOnClickListener(v -> {
            if (dontShowCheckbox.isChecked()) {
                prefs.edit().putBoolean(AppSettings.PREF_SHOW_PROFILE_SETUP_GUIDANCE, false).apply();
            }
            dialog.dismiss();
        });

        dialog.show();

        Window window = dialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.copyFrom(window.getAttributes());
            layoutParams.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            layoutParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
            window.setAttributes(layoutParams);
        }
    }

    private void saveProfile() {
        String name = binding.editProfileName.getText().toString().trim();
        String pin = binding.editSetPin.getText().toString().trim();
        String age = binding.editAge.getText().toString().trim();
        String sex = binding.spinnerSex.getText().toString();
        String height = binding.editHeight.getText().toString().trim();
        String weight = binding.editWeight.getText().toString().trim();
        String location = binding.editLocation.getText().toString().trim();
        String about = binding.editAboutMe.getText().toString().trim();
        
        ProfileValidationResult nameValidation = ProfileValidator.validateName(name);
        if (!nameValidation.valid) {
            binding.layoutProfileName.setError(nameValidation.message);
            return;
        } else {
            binding.layoutProfileName.setError(null);
        }

        ProfileValidationResult pinValidation = ProfileValidator.validatePin(pin, AppSettings.defaultSixDigitPin(requireContext()));
        if (!pinValidation.valid) {
            binding.layoutSetPin.setError(pinValidation.message);
            return;
        } else {
            binding.layoutSetPin.setError(null);
        }

        Set<String> profileSet = ProfileAuthRegistry.getProfileNames(requireContext());
        if (profileSet.contains(name)) {
            binding.layoutProfileName.setError(getString(R.string.duplicate_profile_name));
            return;
        }

        int iconToSave = 1;
        String avatarUriToSave = selectedAvatarUri == null ? "" : selectedAvatarUri.toString();

        binding.buttonSaveProfile.setEnabled(false);
        binding.buttonSaveProfile.setText(R.string.creating_profile);

        ProfileRepository profileRepository = new ProfileRepository(requireContext());
        executor.execute(() -> {
        try {
            org.json.JSONObject data = ProfileJson.emptyProfile(name);
            org.json.JSONObject profile = data.getJSONObject(ProfileJson.KEY_PROFILE);
            profile.put("age", age);
            profile.put("sex", sex);
            profile.put("height_cm", normalizeHeightToCm(height));
            profile.put("weight_kg", normalizeWeightToKg(weight));
            profile.put("blood_type", "");
            profile.put(ProfileJson.PROFILE_LOCATION, location);
            profile.put("bio", about);
            profile.put("avatar", iconToSave);
            profile.put("avatar_uri", avatarUriToSave);

            profileRepository.createProfile(name, pin, data, iconToSave, avatarUriToSave);

            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                Toast.makeText(getContext(), R.string.profile_created, Toast.LENGTH_SHORT).show();
                NavHostFragment.findNavController(this).navigate(R.id.action_CreateProfileFragment_to_ProfileSelectorFragment);
            });
        } catch (Exception e) {
            requireActivity().runOnUiThread(() -> {
                if (binding == null) return;
                binding.buttonSaveProfile.setEnabled(true);
                binding.buttonSaveProfile.setText(R.string.complete_profile);
                Toast.makeText(getContext(), R.string.profile_create_failed, Toast.LENGTH_LONG).show();
            });
        }
        });
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(value.trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private double normalizeHeightToCm(String value) {
        double parsed = parseDouble(value);
        return UnitConverter.normalizeDisplayHeight(parsed, unitPreferences);
    }

    private double normalizeWeightToKg(String value) {
        double parsed = parseDouble(value);
        return UnitConverter.normalizeDisplayWeight(parsed, unitPreferences);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}



