package brettdansmith.drugdiary.ui.auth;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
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
import android.text.Editable;
import android.text.TextWatcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.AvatarType;
import brettdansmith.drugdiary.data.profile.ProfileAuthRegistry;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;
import brettdansmith.drugdiary.data.profile.ProfileAvatarDataStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileRepository;
import brettdansmith.drugdiary.databinding.FragmentCreateProfileBinding;
import brettdansmith.drugdiary.domain.profile.ProfileValidator;
import brettdansmith.drugdiary.domain.units.UnitConverter;
import brettdansmith.drugdiary.domain.units.UnitPreferences;
import brettdansmith.drugdiary.domain.validation.ProfileValidationResult;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.ui.avatar.AvatarEditorBottomSheet;
import brettdansmith.drugdiary.ui.profile.LocationAutoCompleteAdapter;

public class CreateProfileFragment extends Fragment {
    private static final String AVATAR_EDITOR_REQUEST = "create_profile_avatar_editor";

    private FragmentCreateProfileBinding binding;
    private boolean useMetricUnits = true;
    private UnitPreferences unitPreferences;
    private ProfileAvatar selectedAvatar = ProfileAvatar.initials();
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentCreateProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (savedInstanceState != null) {
            String typeStr = savedInstanceState.getString("avatar_type", AvatarType.INITIALS.name());
            String iconId = savedInstanceState.getString("avatar_icon_id", "");
            String imagePath = savedInstanceState.getString("avatar_image_path", "");
            selectedAvatar = ProfileAvatar.fromPrefs(AvatarType.fromStorage(typeStr), iconId, imagePath);
        }

        setupDropdowns();
        setupUnits();
        setupAvatarEditor();

        binding.editLocation.setAdapter(new LocationAutoCompleteAdapter(requireContext()));
        binding.buttonSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void setupAvatarEditor() {
        getParentFragmentManager().setFragmentResultListener(
                AVATAR_EDITOR_REQUEST,
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    AvatarType type = AvatarType.fromStorage(bundle.getString(AvatarEditorBottomSheet.RESULT_TYPE, AvatarType.INITIALS.name()));
                    String iconId = bundle.getString(AvatarEditorBottomSheet.RESULT_ICON_ID, "");
                    String imagePath = bundle.getString(AvatarEditorBottomSheet.RESULT_IMAGE_PATH, "");
                    selectedAvatar = ProfileAvatar.fromPrefs(type, iconId, imagePath);
                    refreshAvatarPreview();
                });

        binding.buttonEditAvatar.setOnClickListener(v -> {
            String name = currentDraftName();
            AvatarEditorBottomSheet.show(getParentFragmentManager(), AVATAR_EDITOR_REQUEST, name, selectedAvatar);
        });

        binding.editProfileName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (selectedAvatar.getAvatarType() == AvatarType.INITIALS) {
                    refreshAvatarPreview();
                }
            }
        });

        refreshAvatarPreview();
    }

    private void refreshAvatarPreview() {
        if (binding == null) return;
        binding.imageProfileIconPreview.bind(currentDraftName(), selectedAvatar);
    }

    @NonNull
    private String currentDraftName() {
        if (binding == null || binding.editProfileName.getText() == null) {
            return "";
        }
        return binding.editProfileName.getText().toString().trim();
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

        binding.buttonSaveProfile.setEnabled(false);
        binding.buttonSaveProfile.setText(R.string.creating_profile);

        ProfileRepository profileRepository = new ProfileRepository(requireContext());
        ProfileAvatar avatarToSave = selectedAvatar;
        executor.execute(() -> {
            try {
                org.json.JSONObject data = ProfileJson.emptyProfile(name);
                org.json.JSONObject profile = data.getJSONObject(ProfileJson.KEY_PROFILE);
                profile.put("age", age);
                profile.put("sex", sex);
                profile.put("height_cm", normalizeHeightToCm(height));
                profile.put("weight_kg", normalizeWeightToKg(weight));
                profile.put("blood_type", "Unknown");
                profile.put(ProfileJson.PROFILE_LOCATION, location);
                profile.put("bio", about);
                ProfileAvatarDataStore.writeToData(data, avatarToSave);

                profileRepository.createProfile(name, pin, data, avatarToSave);

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
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (selectedAvatar != null) {
            outState.putString("avatar_type", selectedAvatar.getAvatarType().name());
            outState.putString("avatar_icon_id", selectedAvatar.getAvatarIconId());
            outState.putString("avatar_image_path", selectedAvatar.getAvatarImagePath());
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
