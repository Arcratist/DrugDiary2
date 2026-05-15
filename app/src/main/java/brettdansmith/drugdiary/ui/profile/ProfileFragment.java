package brettdansmith.drugdiary.ui.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.MainActivity;
import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.EncryptedDrugCacheStore;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;
import brettdansmith.drugdiary.data.profile.ProfileAvatarDataStore;
import brettdansmith.drugdiary.databinding.FragmentProfileBinding;
import brettdansmith.drugdiary.domain.profile.ProfileValidator;
import brettdansmith.drugdiary.domain.units.UnitPreferences;
import brettdansmith.drugdiary.domain.validation.ProfileValidationResult;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.ui.auth.ProfileSelectorFragment;
import brettdansmith.drugdiary.ui.avatar.AvatarEditorBottomSheet;
import brettdansmith.drugdiary.util.JsonUtils;
import brettdansmith.drugdiary.util.UnitConverter;
import brettdansmith.drugdiary.ui.profile.LocationAutoCompleteAdapter;

public class ProfileFragment extends Fragment {
    private static final String AVATAR_EDITOR_REQUEST = "profile_avatar_editor";

    private FragmentProfileBinding binding;
    private boolean useMetricUnits = true;
    private UnitPreferences unitPreferences;
    private String profileName = "";
    private ProfileAvatar currentAvatar = ProfileAvatar.initials();
    private static final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isDataLoaded = false;

    private static final String[] SEX_OPTIONS = {"Male", "Female", "Other", "Prefer not to say"};
    private static final String[] BLOOD_OPTIONS = {"Unknown", "O+", "A+", "B+", "O-", "A-", "AB+", "B-", "AB-"};

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentProfileBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        useMetricUnits = AppSettings.useMetric(requireContext());
        unitPreferences = UnitPreferences.from(requireContext());
        binding.layoutWeight.setHint(getString(R.string.weight_hint_format, unitPreferences.weightLabel()));
        binding.layoutHeight.setHint(getString(R.string.height_hint_format, unitPreferences.heightLabel()));
        setupDropdowns();
        setupAvatarEditor();

        binding.editLocation.setAdapter(new LocationAutoCompleteAdapter(requireContext()));

        loadProfileDataAsync();

        binding.buttonEditAvatar.setOnClickListener(v -> openAvatarEditor());
        binding.btnMedications.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_profileFragment_to_medicationsFragment));

        binding.btnProfileSettings.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_profileFragment_to_userSpecificSettingsFragment));
        binding.btnLogout.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).logout();
            }
        });
    }

    private void setupAvatarEditor() {
        getParentFragmentManager().setFragmentResultListener(
                AVATAR_EDITOR_REQUEST,
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    ProfileAvatar newAvatar = ProfileAvatar.fromPrefs(
                            brettdansmith.drugdiary.data.profile.AvatarType.fromStorage(
                                    bundle.getString(AvatarEditorBottomSheet.RESULT_TYPE,
                                            brettdansmith.drugdiary.data.profile.AvatarType.INITIALS.name())),
                            bundle.getString(AvatarEditorBottomSheet.RESULT_ICON_ID, ""),
                            bundle.getString(AvatarEditorBottomSheet.RESULT_IMAGE_PATH, ""));
                    saveAvatarAsync(newAvatar);
                });
    }

    private void openAvatarEditor() {
        AvatarEditorBottomSheet.show(getParentFragmentManager(), AVATAR_EDITOR_REQUEST, profileName, currentAvatar);
    }

    private void saveAvatarAsync(@NonNull ProfileAvatar avatar) {
        currentAvatar = avatar;
        binding.imageProfileLarge.bind(profileName, currentAvatar);
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshProfileNavIconNow(profileName, currentAvatar);
        }
        Context appContext = requireContext().getApplicationContext();
        String name = profileName;
        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                ProfileAvatarDataStore.writeToData(data, avatar);
                EncryptedProfileStore.saveProfileData(appContext, data);
                ProfileAvatarDataStore.writeToPrefs(appContext, name, avatar);
            } catch (Exception e) {
                mainHandler.post(() -> Toast.makeText(requireContext(), R.string.avatar_save_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }



    private void setupDropdowns() {
        ArrayAdapter<String> genderAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, SEX_OPTIONS);
        binding.editGender.setAdapter(genderAdapter);
        binding.editGender.setOnClickListener(v -> binding.editGender.showDropDown());

        ArrayAdapter<String> bloodAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, BLOOD_OPTIONS);
        binding.editBloodType.setAdapter(bloodAdapter);
        binding.editBloodType.setOnClickListener(v -> binding.editBloodType.showDropDown());
    }



    private void loadProfileDataAsync() {
        Context appContext = requireContext().getApplicationContext();
        SharedPreferences prefs = appContext.getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
        profileName = prefs.getString(ProfileSelectorFragment.KEY_LAST_PROFILE, "User");
        binding.textProfileNameHeader.setText(profileName);

        diskExecutor.execute(() -> {
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            if (data == null) return;
            JSONObject profile = data.optJSONObject(ProfileJson.KEY_PROFILE);
            JSONObject medical = data.optJSONObject(ProfileJson.KEY_MEDICAL);
            if (profile == null) profile = new JSONObject();
            if (medical == null) medical = new JSONObject();

            ProfileAvatar avatar = ProfileAvatar.fromProfileJson(profile);
            String age = profile.optString("age", "");
            String sex = profile.optString("sex", "");
            double weightKg = profile.optDouble("weight_kg", 0);
            double heightCm = profile.optDouble("height_cm", 0);
            String rawBloodType = profile.optString("blood_type", "Unknown");
            final String bloodType = rawBloodType.isEmpty() ? "Unknown" : rawBloodType;
            String location = profile.optString(ProfileJson.PROFILE_LOCATION, "");
            String bio = profile.optString("bio", "");
            String allergies = medical.optString("allergies", "");
            String conditions = medical.optString("conditions", "");
            String emergencyNote = medical.optString("emergency_note", "");

            mainHandler.post(() -> {
                if (binding == null) return;
                currentAvatar = avatar;
                binding.imageProfileLarge.bind(profileName, currentAvatar);
                binding.editAge.setText(age);
                binding.editGender.setText(sex, false);
                binding.editWeight.setText(formatEditableNumber(useMetricUnits ? weightKg : UnitConverter.kilogramsToPounds(weightKg)));
                binding.editHeight.setText(formatEditableNumber(useMetricUnits ? heightCm : UnitConverter.centimetersToInches(heightCm)));
                binding.editBloodType.setText(bloodType, false);
                binding.editLocation.setText(location);
                binding.editAboutMe.setText(bio);
                binding.editAllergies.setText(allergies);
                binding.editConditions.setText(conditions);
                isDataLoaded = true;
            });
        });
    }

    private void saveProfileData() {
        try {
            if (!isDataLoaded || !validateFields()) {
                return;
            }
            JSONObject data = EncryptedProfileStore.loadProfileData(requireContext());
            JSONObject profile = JsonUtils.object(data, ProfileJson.KEY_PROFILE);
            JSONObject medical = JsonUtils.object(data, ProfileJson.KEY_MEDICAL);

            profile.put("age", binding.editAge.getText().toString());
            profile.put("sex", binding.editGender.getText().toString());
            double displayedWeight = parseDouble(binding.editWeight.getText().toString());
            double displayedHeight = parseDouble(binding.editHeight.getText().toString());
            profile.put("weight_kg", useMetricUnits ? displayedWeight : UnitConverter.poundsToKilograms(displayedWeight));
            profile.put("height_cm", useMetricUnits ? displayedHeight : UnitConverter.inchesToCentimeters(displayedHeight));
            profile.put("blood_type", binding.editBloodType.getText().toString());
            profile.put(ProfileJson.PROFILE_LOCATION, binding.editLocation.getText().toString());
            profile.put("bio", binding.editAboutMe.getText().toString());
            medical.put("allergies", binding.editAllergies.getText().toString());
            medical.put("conditions", binding.editConditions.getText().toString());
            ProfileAvatarDataStore.writeToData(data, currentAvatar);

            EncryptedProfileStore.saveProfileData(requireContext(), data);
            ProfileAvatarDataStore.writeToPrefs(requireContext(), profileName, currentAvatar);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProfileDataAsync() {
        if (binding == null || !isDataLoaded || !validateFields()) return;
        Context appContext = requireContext().getApplicationContext();
        String age = binding.editAge.getText().toString();
        String sex = binding.editGender.getText().toString();
        double displayedWeight = parseDouble(binding.editWeight.getText().toString());
        double displayedHeight = parseDouble(binding.editHeight.getText().toString());
        String bloodType = binding.editBloodType.getText().toString();
        String location = binding.editLocation.getText().toString();
        String bio = binding.editAboutMe.getText().toString();
        String allergies = binding.editAllergies.getText().toString();
        String conditions = binding.editConditions.getText().toString();
        boolean metric = useMetricUnits;
        ProfileAvatar avatar = currentAvatar;
        String name = profileName;

        diskExecutor.execute(() -> {
            try {
                JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
                JSONObject profile = JsonUtils.object(data, ProfileJson.KEY_PROFILE);
                JSONObject medical = JsonUtils.object(data, ProfileJson.KEY_MEDICAL);
                profile.put("age", age);
                profile.put("sex", sex);
                profile.put("weight_kg", metric ? displayedWeight : UnitConverter.poundsToKilograms(displayedWeight));
                profile.put("height_cm", metric ? displayedHeight : UnitConverter.inchesToCentimeters(displayedHeight));
                profile.put("blood_type", bloodType);
                profile.put(ProfileJson.PROFILE_LOCATION, location);
                profile.put("bio", bio);
                medical.put("allergies", allergies);
                medical.put("conditions", conditions);
                ProfileAvatarDataStore.writeToData(data, avatar);
                EncryptedProfileStore.saveProfileData(appContext, data);
                ProfileAvatarDataStore.writeToPrefs(appContext, name, avatar);
            } catch (Exception ignored) {}
        });
    }

    private double parseDouble(CharSequence value) {
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (Exception ignored) {
            return 0;
        }
    }

    private boolean validateFields() {
        boolean valid = true;
        binding.editAge.setError(null);
        binding.editWeight.setError(null);
        binding.editHeight.setError(null);

        String ageText = binding.editAge.getText() == null ? "" : binding.editAge.getText().toString().trim();
        if (!ageText.isEmpty()) {
            ProfileValidationResult result = ProfileValidator.validateAge(ageText);
            if (!result.valid) {
                binding.editAge.setError(result.message);
                valid = false;
            }
        }

        double weight = parseDouble(binding.editWeight.getText() == null ? "" : binding.editWeight.getText().toString());
        if (weight > 0) {
            ProfileValidationResult result = ProfileValidator.validateDisplayWeight(weight, unitPreferences);
            if (!result.valid) {
                binding.editWeight.setError(result.message);
                valid = false;
            }
        }

        double height = parseDouble(binding.editHeight.getText() == null ? "" : binding.editHeight.getText().toString());
        if (height > 0) {
            ProfileValidationResult result = ProfileValidator.validateDisplayHeight(height, unitPreferences);
            if (!result.valid) {
                binding.editHeight.setError(result.message);
                valid = false;
            }
        }
        return valid;
    }

    private String formatEditableNumber(double value) {
        if (value <= 0) {
            return "";
        }
        if (Math.abs(value - Math.round(value)) < 0.05) {
            return String.valueOf((long) Math.round(value));
        }
        return String.format(java.util.Locale.getDefault(), "%.1f", value);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveProfileDataAsync();
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
    }
}
