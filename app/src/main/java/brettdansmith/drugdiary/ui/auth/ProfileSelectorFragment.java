package brettdansmith.drugdiary.ui.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.EncryptedProfileStore;
import brettdansmith.drugdiary.data.profile.ProfileAuthRegistry;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;
import brettdansmith.drugdiary.data.profile.ProfileAvatarDataStore;
import brettdansmith.drugdiary.databinding.FragmentProfileSelectorBinding;
import brettdansmith.drugdiary.security.EncryptionManager;
import brettdansmith.drugdiary.security.UserSession;
import brettdansmith.drugdiary.settings.AppSettings;
import brettdansmith.drugdiary.data.reference.DrugReferencePrewarmer;
import brettdansmith.drugdiary.ui.avatar.ProfileAvatarView;

public class ProfileSelectorFragment extends Fragment {
    private FragmentProfileSelectorBinding binding;
    public static final String PREFS_PROFILES = ProfileAuthRegistry.PREFS_PROFILES;
    public static final String KEY_PROFILE_NAMES = ProfileAuthRegistry.KEY_PROFILE_NAMES;
    public static final String KEY_LAST_PROFILE = ProfileAuthRegistry.KEY_LAST_PROFILE;

    private String currentPin = "";
    private String selectedProfile = "";
    private int selectedProfilePinLength = 6;
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isLoggingIn = false;
    private boolean hasProfiles = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileSelectorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        if (savedInstanceState != null) {
            currentPin = savedInstanceState.getString("currentPin", "");
            selectedProfile = savedInstanceState.getString("selectedProfile", "");
        }
        
        setupPinPad();
        checkProfilesAndLoad();

        binding.layoutSelectedProfile.setOnClickListener(v -> {
            if (hasProfiles) {
                toggleProfileExpansion(true);
            } else {
                showCreateImportDialog();
            }
        });
        binding.layoutHeaderExpanded.setOnClickListener(v -> {
            if (hasProfiles) {
                toggleProfileExpansion(false);
            }
        });

        binding.btnGlobalSettings.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_ProfileSelectorFragment_to_globalSettingsFragment));
    }

    private void setupPinPad() {
        View.OnClickListener pinListener = v -> {
            if (currentPin.length() < selectedProfilePinLength) {
                currentPin += ((Button) v).getText().toString();
                updatePinDots();
                if (currentPin.length() == selectedProfilePinLength) {
                    attemptLogin();
                }
            }
        };

        int[] ids = {R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4, R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9};
        for (int id : ids) {
            View btn = binding.getRoot().findViewById(id);
            if (btn != null) btn.setOnClickListener(pinListener);
        }

        binding.btnDelete.setOnClickListener(v -> {
            if (currentPin.length() > 0) {
                currentPin = currentPin.substring(0, currentPin.length() - 1);
                updatePinDots();
            }
        });
    }

    private void updatePinDots() {
        binding.layoutPinDots.removeAllViews();
        int dotSize = (int) (12 * getResources().getDisplayMetrics().density);
        int margin = (int) (8 * getResources().getDisplayMetrics().density);

        for (int i = 0; i < selectedProfilePinLength; i++) {
            ImageView dot = new ImageView(requireContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dotSize, dotSize);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);

            if (i < currentPin.length()) {
                dot.setImageResource(R.drawable.pin_dot_filled);
            } else {
                dot.setImageResource(R.drawable.pin_dot_empty);
            }
            binding.layoutPinDots.addView(dot);
        }
    }

    private void checkProfilesAndLoad() {
        SharedPreferences prefs = ProfileAuthRegistry.prefs(requireContext());
        Set<String> profileSet = prefs.getStringSet(KEY_PROFILE_NAMES, new HashSet<>());
        List<String> profiles = new ArrayList<>(profileSet);

        if (profiles.isEmpty()) {
            hasProfiles = false;
            showEmptySelectorState();
        } else {
            hasProfiles = true;
            showProfilesSelectorState();
            binding.cardProfileSelector.setVisibility(View.VISIBLE);
            if (selectedProfile.isEmpty()) {
                selectedProfile = prefs.getString(KEY_LAST_PROFILE, profiles.get(0));
            }
            updateSelectedProfileDisplay();
        }
        updatePinDots();
    }

    private void showEmptySelectorState() {
        binding.cardProfileSelector.setVisibility(View.VISIBLE);
        binding.cardProfileExpanded.setVisibility(View.GONE);
        binding.textProfileName.setText(R.string.create_or_import_profile);
        binding.viewAvatarSelected.showAddAvatar();
        binding.imgArrow.setVisibility(View.VISIBLE);
        binding.labelEnterPin.setVisibility(View.GONE);
        binding.layoutPinDots.setVisibility(View.GONE);
        binding.gridPinButtons.setVisibility(View.GONE);
        binding.layoutStatusFeedback.setVisibility(View.GONE);
        selectedProfile = "";
        setPinPadEnabled(false);
    }

    private void showProfilesSelectorState() {
        binding.imgArrow.setVisibility(View.VISIBLE);
        binding.labelEnterPin.setVisibility(View.VISIBLE);
        binding.layoutPinDots.setVisibility(View.VISIBLE);
        binding.gridPinButtons.setVisibility(View.VISIBLE);
        binding.layoutStatusFeedback.setVisibility(View.VISIBLE);
        setPinPadEnabled(true);
    }

    private void updateSelectedProfileDisplay() {
        SharedPreferences prefs = ProfileAuthRegistry.prefs(requireContext());
        selectedProfilePinLength = ProfileAuthRegistry.getPinLength(requireContext(), selectedProfile);

        binding.textProfileName.setText(selectedProfile);
        bindAvatar(selectedProfile, binding.viewAvatarSelected);
        binding.imgArrow.setVisibility(View.VISIBLE);

        binding.textProfileNameExp.setText(selectedProfile);
        bindAvatar(selectedProfile, binding.viewAvatarSelectedExp);
    }

    private void bindAvatar(@NonNull String profileName, @NonNull ProfileAvatarView avatarView) {
        ProfileAvatar avatar = ProfileAvatarDataStore.readFromPrefs(requireContext(), profileName);
        avatarView.bind(profileName, avatar);
    }

    private void toggleProfileExpansion(boolean expand) {
        if (expand) {
            populateExpandedList();

            TransitionSet transition = new TransitionSet()
                    .addTransition(new AutoTransition())
                    .setDuration(250)
                    .setOrdering(TransitionSet.ORDERING_TOGETHER);

            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), transition);

            binding.cardProfileExpanded.setVisibility(View.VISIBLE);
            binding.cardProfileSelector.setAlpha(0f);
            binding.dividerExpanded.setVisibility(View.VISIBLE);
            binding.scrollOtherProfiles.setVisibility(View.VISIBLE);
            binding.scrollOtherProfiles.setAlpha(1f);
        } else {
            TransitionSet transition = new TransitionSet()
                    .addTransition(new AutoTransition())
                    .setDuration(200);

            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot(), transition);

            binding.scrollOtherProfiles.setAlpha(0f);
            binding.scrollOtherProfiles.setVisibility(View.GONE);
            binding.dividerExpanded.setVisibility(View.GONE);
            binding.cardProfileExpanded.setVisibility(View.GONE);
            binding.cardProfileSelector.setAlpha(1f);
        }
    }

    private void populateExpandedList() {
        SharedPreferences prefs = ProfileAuthRegistry.prefs(requireContext());
        Set<String> profileSet = prefs.getStringSet(KEY_PROFILE_NAMES, new HashSet<>());
        List<String> profiles = new ArrayList<>(profileSet);
        profiles.remove(selectedProfile);

        binding.containerOtherProfiles.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        for (String profile : profiles) {
            View itemView = inflater.inflate(R.layout.item_profile_selection, binding.containerOtherProfiles, false);
            TextView nameText = itemView.findViewById(R.id.text_item_name);
            ProfileAvatarView avatarView = itemView.findViewById(R.id.view_item_avatar);

            nameText.setText(profile);
            bindAvatar(profile, avatarView);

            itemView.setOnClickListener(v -> {
                selectedProfile = profile;
                updateSelectedProfileDisplay();
                toggleProfileExpansion(false);
                currentPin = "";
                updatePinDots();
            });

            binding.containerOtherProfiles.addView(itemView);
        }

        View addView = inflater.inflate(R.layout.item_profile_selection, binding.containerOtherProfiles, false);
        TextView addText = addView.findViewById(R.id.text_item_name);
        ProfileAvatarView addAvatar = addView.findViewById(R.id.view_item_avatar);

        addText.setText(R.string.create_or_import_profile);
        addAvatar.showAddAvatar();

        addView.setOnClickListener(v -> showCreateImportDialog());

        binding.containerOtherProfiles.addView(addView);
    }

    private void showCreateImportDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_profile_entry_action, null);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
        dialogView.findViewById(R.id.button_create_profile).setOnClickListener(v -> {
            dialog.dismiss();
            NavHostFragment.findNavController(this).navigate(R.id.action_ProfileSelectorFragment_to_CreateProfileFragment);
        });
        dialogView.findViewById(R.id.button_import_profile).setOnClickListener(v ->
        {
            Toast.makeText(requireContext(), R.string.import_profile_nyi, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        dialogView.findViewById(R.id.button_cancel_profile_entry).setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void attemptLogin() {
        if (isLoggingIn) return;
        SharedPreferences prefs = ProfileAuthRegistry.prefs(requireContext());
        Context appContext = requireContext().getApplicationContext();
        isLoggingIn = true;
        setPinPadEnabled(false);
        binding.progressUnlocking.setVisibility(View.VISIBLE);
        binding.imgUnlockError.setVisibility(View.GONE);
        binding.textUnlockStatus.setText(R.string.unlocking_profile);
        binding.textUnlockStatus.setVisibility(View.VISIBLE);

        String pinToProcess = currentPin;
        String profileToProcess = selectedProfile;
        executor.execute(() -> {
            try {
                if (ProfileAuthRegistry.verifyPin(appContext, profileToProcess, pinToProcess)) {
                    SecretKeySpec key = EncryptionManager.deriveKey(profileToProcess + pinToProcess);
                    UserSession.getInstance().startSession(profileToProcess, key);
                    syncAvatarPrefsToVault(appContext, profileToProcess);
                    DrugReferencePrewarmer.prewarmAfterUnlock(appContext);
                    mainHandler.post(() -> {
                        if (binding == null) return;

                        AppCompatDelegate.setDefaultNightMode(AppSettings.getTheme(appContext));

                        prefs.edit().putString(KEY_LAST_PROFILE, profileToProcess).apply();
                        NavHostFragment.findNavController(this).navigate(R.id.action_ProfileSelectorFragment_to_profileFragment);
                    });
                } else {
                    mainHandler.post(() -> showAuthError(getString(R.string.incorrect_pin)));
                }
            } catch (Exception e) {
                UserSession.getInstance().endSession();
                mainHandler.post(() -> showAuthError("Authentication Error"));
            }
        });
    }

    private void syncAvatarPrefsToVault(@NonNull Context appContext, @NonNull String profileName) {
        try {
            ProfileAvatar avatar = ProfileAvatarDataStore.readFromPrefs(appContext, profileName);
            JSONObject data = EncryptedProfileStore.loadProfileData(appContext);
            ProfileAvatarDataStore.writeToData(data, avatar);
            EncryptedProfileStore.saveProfileData(appContext, data);
        } catch (Exception ignored) {
        }
    }

    private void showAuthError(String message) {
        if (binding == null) return;
        binding.progressUnlocking.setVisibility(View.GONE);
        binding.imgUnlockError.setVisibility(View.VISIBLE);
        binding.textUnlockStatus.setText(message);
        binding.textUnlockStatus.setVisibility(View.VISIBLE);

        mainHandler.postDelayed(() -> {
            if (binding != null) {
                resetPin();
            }
        }, 1500);
    }

    private void resetPin() {
        isLoggingIn = false;
        currentPin = "";
        updatePinDots();
        setPinPadEnabled(true);
        binding.progressUnlocking.setVisibility(View.GONE);
        binding.imgUnlockError.setVisibility(View.GONE);
        binding.textUnlockStatus.setVisibility(View.GONE);
    }

    private void setPinPadEnabled(boolean enabled) {
        if (binding == null) return;
        binding.gridPinButtons.setAlpha(enabled ? 1.0f : 0.55f);
        for (int i = 0; i < binding.gridPinButtons.getChildCount(); i++) {
            binding.gridPinButtons.getChildAt(i).setEnabled(enabled);
        }
        if (hasProfiles) {
            binding.layoutSelectedProfile.setEnabled(enabled);
        } else {
            binding.layoutSelectedProfile.setEnabled(true);
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("currentPin", currentPin);
        outState.putString("selectedProfile", selectedProfile);
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
