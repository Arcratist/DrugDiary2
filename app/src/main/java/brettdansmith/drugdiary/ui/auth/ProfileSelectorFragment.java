package brettdansmith.drugdiary.ui.auth;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.security.EncryptionManager;
import brettdansmith.drugdiary.security.UserSession;

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
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.imageview.ShapeableImageView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.crypto.spec.SecretKeySpec;

import brettdansmith.drugdiary.data.profile.ProfileAuthRegistry;
import brettdansmith.drugdiary.data.reference.DrugReferencePrewarmer;
import brettdansmith.drugdiary.databinding.FragmentProfileSelectorBinding;

public class ProfileSelectorFragment extends Fragment {

    private FragmentProfileSelectorBinding binding;
    public static final String PREFS_PROFILES = ProfileAuthRegistry.PREFS_PROFILES;
    public static final String KEY_PROFILE_NAMES = ProfileAuthRegistry.KEY_PROFILE_NAMES;
    public static final String KEY_LAST_PROFILE = ProfileAuthRegistry.KEY_LAST_PROFILE;

    private String currentPin = "";
    private String selectedProfile = "";
    private int selectedProfilePinLength = 4;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private boolean isLoggingIn = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentProfileSelectorBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setupPinPad();
        checkProfilesAndLoad();

        binding.layoutSelectedProfile.setOnClickListener(v -> showProfilePickerDialog());
        
        binding.btnLoginSettings.setVisibility(View.GONE);
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

        for (int i = 0; i < binding.gridPinButtons.getChildCount(); i++) {
            View child = binding.gridPinButtons.getChildAt(i);
            if (child instanceof Button && child.getId() != R.id.btn_delete) {
                child.setOnClickListener(pinListener);
            }
        }

        binding.btnDelete.setOnClickListener(v -> {
            if (currentPin.length() > 0) {
                currentPin = currentPin.substring(0, currentPin.length() - 1);
                updatePinDots();
            }
        });
    }

    private void updatePinDots() {
        StringBuilder dots = new StringBuilder();
        for (int i = 0; i < selectedProfilePinLength; i++) {
            dots.append(i < currentPin.length() ? "* " : "- ");
        }
        binding.textPinDots.setText(dots.toString().trim());
    }

    private void checkProfilesAndLoad() {
        SharedPreferences prefs = ProfileAuthRegistry.prefs(requireContext());
        Set<String> profileSet = prefs.getStringSet(KEY_PROFILE_NAMES, new HashSet<>());
        List<String> profiles = new ArrayList<>(profileSet);

        if (profiles.isEmpty()) {
            NavHostFragment.findNavController(this).navigate(R.id.action_ProfileSelectorFragment_to_CreateProfileFragment);
        } else {
            binding.cardProfileSelector.setVisibility(View.VISIBLE);
            binding.layoutPinPad.setVisibility(View.VISIBLE);
            selectedProfile = prefs.getString(KEY_LAST_PROFILE, profiles.get(0));
            selectedProfilePinLength = ProfileAuthRegistry.getPinLength(requireContext(), selectedProfile);
            binding.textProfileName.setText(selectedProfile);
            setAvatar(prefs, selectedProfile);
        }
        currentPin = "";
        updatePinDots();
    }

    private void showProfilePickerDialog() {
        SharedPreferences prefs = ProfileAuthRegistry.prefs(requireContext());
        Set<String> profileSet = prefs.getStringSet(KEY_PROFILE_NAMES, new HashSet<>());
        List<String> profiles = new ArrayList<>(profileSet);
        
        List<String> displayList = new ArrayList<>(profiles);
        displayList.add("Add New Profile");

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(requireContext(), R.layout.item_profile_selection, R.id.text_item_name, displayList) {
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ShapeableImageView avatar = view.findViewById(R.id.image_item_avatar);
                String item = getItem(position);
                if ("Add New Profile".equals(item)) {
                    avatar.setImageResource(android.R.drawable.ic_input_add);
                } else {
                    avatar.setImageResource(avatarRes(prefs.getInt("icon_" + item, 1)));
                }
                return view;
            }
        };

        new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Select Profile")
                .setAdapter(adapter, (dialog, which) -> {
                    if (which == profiles.size()) {
                        NavHostFragment.findNavController(this).navigate(R.id.action_ProfileSelectorFragment_to_CreateProfileFragment);
                    } else {
                        selectedProfile = profiles.get(which);
                        selectedProfilePinLength = ProfileAuthRegistry.getPinLength(requireContext(), selectedProfile);
                        binding.textProfileName.setText(selectedProfile);
                        setAvatar(prefs, selectedProfile);
                        currentPin = "";
                        updatePinDots();
                    }
                })
                .show();
    }

    private void attemptLogin() {
        if (isLoggingIn) return;
        SharedPreferences prefs = ProfileAuthRegistry.prefs(requireContext());
        Context appContext = requireContext().getApplicationContext();
        isLoggingIn = true;
        setPinPadEnabled(false);
        binding.progressUnlocking.setVisibility(View.VISIBLE);
        binding.textUnlockStatus.setVisibility(View.VISIBLE);

        String pinToProcess = currentPin;
        String profileToProcess = selectedProfile;
        executor.execute(() -> {
            try {
                if (ProfileAuthRegistry.verifyPin(appContext, profileToProcess, pinToProcess)) {
                    SecretKeySpec key = EncryptionManager.deriveKey(profileToProcess + pinToProcess);
                    UserSession.getInstance().startSession(profileToProcess, key);
                    DrugReferencePrewarmer.prewarmAfterUnlock(appContext);
                    mainHandler.post(() -> {
                        if (binding == null) return;
                        prefs.edit().putString(KEY_LAST_PROFILE, profileToProcess).apply();
                        NavHostFragment.findNavController(this).navigate(R.id.action_ProfileSelectorFragment_to_dashboardFragment);
                    });
                } else {
                    mainHandler.post(() -> {
                        if (binding == null) return;
                        Toast.makeText(getContext(), "Incorrect PIN", Toast.LENGTH_SHORT).show();
                        resetPin();
                    });
                }
            } catch (Exception e) {
                UserSession.getInstance().endSession();
                mainHandler.post(() -> {
                    if (binding == null) return;
                    Toast.makeText(getContext(), "Auth Error", Toast.LENGTH_SHORT).show();
                    resetPin();
                });
            }
        });
    }

    private void resetPin() {
        isLoggingIn = false;
        currentPin = "";
        updatePinDots();
        setPinPadEnabled(true);
        binding.progressUnlocking.setVisibility(View.GONE);
        binding.textUnlockStatus.setVisibility(View.GONE);
    }

    private void setPinPadEnabled(boolean enabled) {
        binding.layoutPinPad.setAlpha(enabled ? 1.0f : 0.55f);
        for (int i = 0; i < binding.gridPinButtons.getChildCount(); i++) {
            binding.gridPinButtons.getChildAt(i).setEnabled(enabled);
        }
        binding.layoutSelectedProfile.setEnabled(enabled);
    }

    private int avatarRes(int avatar) {
        switch (avatar) {
            case 2: return R.drawable.avatar_placeholder_2;
            case 3: return R.drawable.avatar_placeholder_3;
            case 4: return R.drawable.avatar_placeholder_4;
            default: return R.drawable.avatar_placeholder_1;
        }
    }

    private void setAvatar(SharedPreferences prefs, String profile) {
        String uri = prefs.getString("avatar_uri_" + profile, "");
        if (!uri.isEmpty()) {
            binding.imageAvatar.setImageURI(Uri.parse(uri));
        } else {
            binding.imageAvatar.setImageResource(avatarRes(prefs.getInt("icon_" + profile, 1)));
        }
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
        executor.shutdown();
    }
}




