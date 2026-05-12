package brettdansmith.drugdiary.ui.profile;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.ui.auth.ProfileSelectorFragment;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.data.profile.EncryptedDrugCacheStore;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileJson;
import brettdansmith.drugdiary.databinding.FragmentProfileBinding;
import brettdansmith.drugdiary.domain.profile.ProfileValidator;
import brettdansmith.drugdiary.domain.units.UnitPreferences;
import brettdansmith.drugdiary.domain.validation.ProfileValidationResult;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.util.JsonUtils;
import brettdansmith.drugdiary.util.SecureQrShareController;
import brettdansmith.drugdiary.util.SecureQrShareController.ShareMode;
import brettdansmith.drugdiary.util.UnitConverter;

public class ProfileFragment extends Fragment {
    private FragmentProfileBinding binding;
    private boolean useMetricUnits = true;
    private UnitPreferences unitPreferences;
    private String profileName = "";
    private ActivityResultLauncher<String> avatarPicker;
    private SecureQrShareController qrShareController;
    private final ExecutorService diskExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private static final String[] SEX_OPTIONS = {"Male", "Female", "Other", "Prefer not to say"};
    private static final String[] BLOOD_OPTIONS = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-", "Unknown"};

    public ProfileFragment() {
        avatarPicker = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri == null || binding == null || profileName.isEmpty()) return;
            try {
                requireContext().getContentResolver().takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (SecurityException ignored) {
                // Some content providers grant transient read access only.
            }
            saveAvatarUri(uri);
        });
    }

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
        qrShareController = new SecureQrShareController(this);
        binding.layoutWeight.setHint(getString(R.string.weight_hint_format, unitPreferences.weightLabel()));
        binding.layoutHeight.setHint(getString(R.string.height_hint_format, unitPreferences.heightLabel()));
        setupDropdowns();
        loadProfileDataAsync();

        binding.imageProfileLarge.setOnClickListener(v -> showAvatarEditor());
        binding.btnMedications.setOnClickListener(v -> 
            NavHostFragment.findNavController(this).navigate(R.id.action_profileFragment_to_medicationsFragment));
        binding.btnShareProfileQr.setOnClickListener(v -> showShareDataTypeDialog());
    }

    private void showShareDataTypeDialog() {
        String[] options = {
                getString(R.string.share_type_whole_profile),
                getString(R.string.share_type_profile_details),
                getString(R.string.share_type_medications),
                getString(R.string.share_type_diary),
                getString(R.string.share_type_assistant),
                getString(R.string.share_type_reference_cache)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.share_data_type_title)
                .setItems(options, (dialog, which) -> showShareMethodDialog(which))
                .show();
    }

    private void showShareMethodDialog(int exportType) {
        String[] methods = {
                getString(R.string.share_method_qr),
                getString(R.string.share_method_encrypted_text)
        };
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.share_method_title)
                .setItems(methods, (dialog, which) -> shareSelectedData(exportType, which == 0 ? ShareMode.QR_CODE : ShareMode.ENCRYPTED_TEXT))
                .show();
    }

    private void shareSelectedData(int exportType, ShareMode mode) {
        try {
            saveProfileData();
            JSONObject data = EncryptedProfileStore.loadProfileData(requireContext());
            if (data == null) return;
            ExportSelection selection = buildExportSelection(data, exportType);
            qrShareController.shareJson(selection.type, selection.title, selection.payload, mode);
        } catch (Exception e) {
            Toast.makeText(requireContext(), getString(R.string.secure_qr_export_failed, e.getMessage()), Toast.LENGTH_LONG).show();
        }
    }

    private ExportSelection buildExportSelection(JSONObject data, int exportType) throws Exception {
        switch (exportType) {
            case 1:
                return new ExportSelection(
                        "profile_details",
                        getString(R.string.drugdiary_profile_details_export),
                        new JSONObject()
                                .put(ProfileJson.KEY_PROFILE, copyObject(data.optJSONObject(ProfileJson.KEY_PROFILE)))
                                .put(ProfileJson.KEY_MEDICAL, copyObject(data.optJSONObject(ProfileJson.KEY_MEDICAL)))
                                .put(ProfileJson.KEY_PRIVACY, copyObject(data.optJSONObject(ProfileJson.KEY_PRIVACY))));
            case 2:
                return new ExportSelection(
                        "medications",
                        getString(R.string.drugdiary_medications_export),
                        new JSONObject()
                                .put("schema", ProfileJson.KEY_MEDICATIONS)
                                .put("items", copyArray(trackers(data).optJSONArray(ProfileJson.KEY_MEDICATIONS))));
            case 3:
                return new ExportSelection(
                        "diary_logs",
                        getString(R.string.drugdiary_diary_export),
                        new JSONObject()
                                .put("schema", ProfileJson.KEY_LOGS)
                                .put("items", copyArray(trackers(data).optJSONArray(ProfileJson.KEY_LOGS))));
            case 4:
                return new ExportSelection(
                        "assistant_chats",
                        getString(R.string.drugdiary_assistant_export),
                        new JSONObject()
                                .put(ProfileJson.KEY_ASSISTANT_CHATS, copyArray(data.optJSONArray(ProfileJson.KEY_ASSISTANT_CHATS)))
                                .put(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, data.optString(ProfileJson.KEY_ASSISTANT_ACTIVE_CHAT_ID, "")));
            case 5:
                JSONObject drugData = EncryptedDrugCacheStore.loadDrugCache(requireContext());
                return new ExportSelection(
                        "reference_cache",
                        getString(R.string.drugdiary_reference_cache_export),
                        new JSONObject()
                                .put(ProfileJson.KEY_DRUG_DATABASE_CACHE, copyObject(drugData.optJSONObject(ProfileJson.KEY_DRUG_DATABASE_CACHE)))
                                .put(ProfileJson.KEY_PUBCHEM_CACHE, copyObject(drugData.optJSONObject(ProfileJson.KEY_PUBCHEM_CACHE))));
            case 0:
            default:
                JSONObject combined = new JSONObject(data.toString());
                JSONObject drugCache = EncryptedDrugCacheStore.loadDrugCache(requireContext());
                if (drugCache.has(ProfileJson.KEY_DRUG_DATABASE_CACHE)) {
                    combined.put(ProfileJson.KEY_DRUG_DATABASE_CACHE, drugCache.optJSONObject(ProfileJson.KEY_DRUG_DATABASE_CACHE));
                }
                if (drugCache.has(ProfileJson.KEY_PUBCHEM_CACHE)) {
                    combined.put(ProfileJson.KEY_PUBCHEM_CACHE, drugCache.optJSONObject(ProfileJson.KEY_PUBCHEM_CACHE));
                }
                return new ExportSelection(
                        "profile",
                        getString(R.string.drugdiary_profile_export),
                        combined);
        }
    }

    private JSONObject trackers(JSONObject data) {
        JSONObject trackers = data.optJSONObject(ProfileJson.KEY_TRACKERS);
        return trackers == null ? new JSONObject() : trackers;
    }

    private JSONObject copyObject(JSONObject object) throws Exception {
        return object == null ? new JSONObject() : new JSONObject(object.toString());
    }

    private JSONArray copyArray(JSONArray array) throws Exception {
        return array == null ? new JSONArray() : new JSONArray(array.toString());
    }

    private static final class ExportSelection {
        final String type;
        final String title;
        final JSONObject payload;

        ExportSelection(String type, String title, JSONObject payload) {
            this.type = type;
            this.title = title;
            this.payload = payload;
        }
    }

    private void setupDropdowns() {
        binding.editGender.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, SEX_OPTIONS));
        binding.editBloodType.setAdapter(new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, BLOOD_OPTIONS));
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

            String avatarUri = prefs.getString("avatar_uri_" + profileName, "");
            int avatarIcon = prefs.getInt("icon_" + profileName, 1);
            String age = profile.optString("age", "");
            String sex = profile.optString("sex", "");
            double weightKg = profile.optDouble("weight_kg", 0);
            double heightCm = profile.optDouble("height_cm", 0);
            String bloodType = profile.optString("blood_type", "");
            String location = profile.optString(ProfileJson.PROFILE_LOCATION, "");
            String bio = profile.optString("bio", "");
            String allergies = medical.optString("allergies", "");
            String conditions = medical.optString("conditions", "");
            String emergencyNote = medical.optString("emergency_note", "");

            mainHandler.post(() -> {
                if (binding == null) return;
                if (!avatarUri.isEmpty()) {
                    binding.imageProfileLarge.setImageURI(Uri.parse(avatarUri));
                } else {
                    binding.imageProfileLarge.setImageResource(avatarRes(avatarIcon));
                }
                binding.editAge.setText(age);
                binding.editGender.setText(sex);
                binding.editWeight.setText(formatEditableNumber(useMetricUnits ? weightKg : UnitConverter.kilogramsToPounds(weightKg)));
                binding.editHeight.setText(formatEditableNumber(useMetricUnits ? heightCm : UnitConverter.centimetersToInches(heightCm)));
                binding.editBloodType.setText(bloodType);
                binding.editLocation.setText(location);
                binding.editAboutMe.setText(bio);
                binding.editAllergies.setText(allergies);
                binding.editConditions.setText(conditions);
                binding.editEmergencyNote.setText(emergencyNote);
            });
        });
    }

    private void saveProfileData() {
        try {
            if (!validateFields()) {
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
            medical.put("emergency_note", binding.editEmergencyNote.getText().toString());

            EncryptedProfileStore.saveProfileData(requireContext(), data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveProfileDataAsync() {
        if (binding == null || !validateFields()) return;
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
        String emergencyNote = binding.editEmergencyNote.getText().toString();
        boolean metric = useMetricUnits;

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
                medical.put("emergency_note", emergencyNote);
                EncryptedProfileStore.saveProfileData(appContext, data);
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

    private int avatarRes(int avatar) {
        switch (avatar) {
            case 2: return R.drawable.avatar_placeholder_2;
            case 3: return R.drawable.avatar_placeholder_3;
            case 4: return R.drawable.avatar_placeholder_4;
            default: return R.drawable.avatar_placeholder_1;
        }
    }

    private void showAvatarEditor() {
        String[] choices = {"Use image from device", "Avatar 1", "Avatar 2", "Avatar 3", "Avatar 4"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Edit avatar")
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) {
                        avatarPicker.launch("image/*");
                    } else {
                        saveAvatarChoice(which);
                    }
                })
                .show();
    }

    private void saveAvatarChoice(int avatar) {
        SharedPreferences prefs = requireContext().getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
        prefs.edit().putInt("icon_" + profileName, avatar).remove("avatar_uri_" + profileName).apply();
        binding.imageProfileLarge.setImageResource(avatarRes(avatar));
    }

    private void saveAvatarUri(Uri uri) {
        SharedPreferences prefs = requireContext().getSharedPreferences(ProfileSelectorFragment.PREFS_PROFILES, Context.MODE_PRIVATE);
        prefs.edit().putString("avatar_uri_" + profileName, uri.toString()).apply();
        binding.imageProfileLarge.setImageURI(uri);
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
        if (qrShareController != null) qrShareController.shutdown();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        diskExecutor.shutdownNow();
    }
}
