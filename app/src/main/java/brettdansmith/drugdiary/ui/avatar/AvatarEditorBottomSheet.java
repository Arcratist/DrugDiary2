package brettdansmith.drugdiary.ui.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.canhub.cropper.CropImageContract;
import com.canhub.cropper.CropImageContractOptions;
import com.canhub.cropper.CropImageOptions;
import com.canhub.cropper.CropImageView;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.AvatarType;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;

public final class AvatarEditorBottomSheet extends BottomSheetDialogFragment {
    public static final String RESULT_TYPE = "avatar_type";
    public static final String RESULT_ICON_ID = "avatar_icon_id";
    public static final String RESULT_IMAGE_PATH = "avatar_image_path";

    private static final String ARG_REQUEST_KEY = "request_key";
    private static final String ARG_PROFILE_NAME = "profile_name";
    private static final String ARG_INITIAL_TYPE = "initial_type";
    private static final String ARG_INITIAL_ICON_ID = "initial_icon_id";
    private static final String ARG_INITIAL_IMAGE_PATH = "initial_image_path";

    private final ExecutorService imageExecutor = Executors.newSingleThreadExecutor();
    private ProfileAvatarView previewView;
    private AvatarIconAdapter iconAdapter;
    private String requestKey;
    private String profileName;
    private ProfileAvatar initialAvatar = ProfileAvatar.initials();
    private ProfileAvatar pendingAvatar = ProfileAvatar.initials();
    private String stagedCustomImagePath;
    private boolean resultCommitted;

    private final ActivityResultLauncher<PickVisualMediaRequest> photoPicker =
            registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> {
                if (uri == null) {
                    return;
                }
                launchCrop(uri);
            });

    private final ActivityResultLauncher<CropImageContractOptions> cropLauncher =
            registerForActivityResult(new CropImageContract(), result -> {
                if (result == null) {
                    return;
                }
                if (!result.isSuccessful()) {
                    if (result.getError() != null && getContext() != null) {
                        Toast.makeText(getContext(), R.string.avatar_crop_failed, Toast.LENGTH_SHORT).show();
                    }
                    return;
                }
                Uri croppedUri = result.getUriContent();
                if (croppedUri == null) {
                    return;
                }
                processPickedImage(croppedUri);
            });

    @NonNull
    public static AvatarEditorBottomSheet newInstance(
            @NonNull String requestKey,
            @NonNull String profileName,
            @NonNull ProfileAvatar currentAvatar) {
        AvatarEditorBottomSheet sheet = new AvatarEditorBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_REQUEST_KEY, requestKey);
        args.putString(ARG_PROFILE_NAME, profileName);
        args.putString(ARG_INITIAL_TYPE, currentAvatar.getAvatarType().name());
        args.putString(ARG_INITIAL_ICON_ID, currentAvatar.getAvatarIconId());
        args.putString(ARG_INITIAL_IMAGE_PATH, currentAvatar.getAvatarImagePath());
        sheet.setArguments(args);
        return sheet;
    }

    public static void show(
            @NonNull FragmentManager fragmentManager,
            @NonNull String requestKey,
            @NonNull String profileName,
            @NonNull ProfileAvatar currentAvatar) {
        AvatarEditorBottomSheet.newInstance(requestKey, profileName, currentAvatar)
                .show(fragmentManager, "avatar_editor");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_avatar_editor, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = requireArguments();
        requestKey = args.getString(ARG_REQUEST_KEY, "");
        profileName = args.getString(ARG_PROFILE_NAME, "");
        initialAvatar = new ProfileAvatar(
                AvatarType.fromStorage(args.getString(ARG_INITIAL_TYPE, AvatarType.INITIALS.name())),
                args.getString(ARG_INITIAL_ICON_ID, ""),
                args.getString(ARG_INITIAL_IMAGE_PATH, ""));
        pendingAvatar = initialAvatar;

        previewView = view.findViewById(R.id.view_avatar_preview);
        RecyclerView iconRecycler = view.findViewById(R.id.recycler_avatar_icons);
        iconRecycler.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        iconAdapter = new AvatarIconAdapter(AvatarIconRegistry.all(), profileName, this::onOptionSelected);
        iconRecycler.setAdapter(iconAdapter);

        view.findViewById(R.id.button_choose_device).setOnClickListener(v ->
                photoPicker.launch(new PickVisualMediaRequest.Builder()
                        .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                        .build()));
        view.findViewById(R.id.button_cancel_avatar).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.button_save_avatar).setOnClickListener(v -> commitResult());

        refreshUi();
    }

    private void onOptionSelected(@NonNull String iconId) {
        clearStagedPathIfNeeded(null);
        if ("initials".equals(iconId)) {
            pendingAvatar = ProfileAvatar.initials();
        } else {
            pendingAvatar = ProfileAvatar.builtIn(iconId);
        }
        refreshUi();
    }

    private void launchCrop(@NonNull Uri sourceUri) {
        CropImageOptions options = new CropImageOptions();
        options.cropShape = CropImageView.CropShape.RECTANGLE;
        options.guidelines = CropImageView.Guidelines.ON;
        options.fixAspectRatio = true;
        options.aspectRatioX = 1;
        options.aspectRatioY = 1;
        options.maxCropResultWidth = 2048;
        options.maxCropResultHeight = 2048;
        options.outputCompressQuality = 92;
        options.outputCompressFormat = Bitmap.CompressFormat.JPEG;
        cropLauncher.launch(new CropImageContractOptions(sourceUri, options));
    }

    private void processPickedImage(@NonNull Uri croppedUri) {
        if (getContext() == null) {
            return;
        }
        Context appContext = requireContext().getApplicationContext();
        String effectiveName = profileName == null || profileName.trim().isEmpty() ? "profile" : profileName;
        imageExecutor.execute(() -> {
            try {
                String processedPath = AvatarImageProcessor.processAndStoreSquareAvatar(appContext, croppedUri, effectiveName);
                if (!isAdded()) {
                    AvatarImageProcessor.deleteIfManaged(appContext, processedPath);
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    clearStagedPathIfNeeded(processedPath);
                    stagedCustomImagePath = processedPath;
                    pendingAvatar = ProfileAvatar.customImage(processedPath);
                    refreshUi();
                });
            } catch (Exception e) {
                if (!isAdded() || getContext() == null) return;
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.avatar_process_failed, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void refreshUi() {
        if (previewView != null) {
            previewView.bind(profileName, pendingAvatar);
        }
        if (iconAdapter != null) {
            String selectedId;
            if (pendingAvatar.getAvatarType() == AvatarType.BUILT_IN_ICON) {
                selectedId = pendingAvatar.getAvatarIconId();
            } else if (pendingAvatar.getAvatarType() == AvatarType.INITIALS) {
                selectedId = "initials";
            } else {
                selectedId = null;
            }
            iconAdapter.setSelectedIconId(selectedId);
        }
    }

    private void commitResult() {
        resultCommitted = true;
        Bundle result = new Bundle();
        result.putString(RESULT_TYPE, pendingAvatar.getAvatarType().name());
        result.putString(RESULT_ICON_ID, pendingAvatar.getAvatarIconId());
        result.putString(RESULT_IMAGE_PATH, pendingAvatar.getAvatarImagePath());
        getParentFragmentManager().setFragmentResult(requestKey, result);
        dismiss();
    }

    private void clearStagedPathIfNeeded(@Nullable String nextPath) {
        if (stagedCustomImagePath == null || stagedCustomImagePath.equals(nextPath)) {
            return;
        }
        if (getContext() != null) {
            AvatarImageProcessor.deleteIfManaged(requireContext(), stagedCustomImagePath);
        }
        stagedCustomImagePath = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (!resultCommitted) {
            if (stagedCustomImagePath != null && getContext() != null) {
                AvatarImageProcessor.deleteIfManaged(requireContext(), stagedCustomImagePath);
            }
        } else if (initialAvatar.getAvatarType() == AvatarType.CUSTOM_IMAGE
                && pendingAvatar.getAvatarType() == AvatarType.CUSTOM_IMAGE
                && stagedCustomImagePath != null
                && !stagedCustomImagePath.equals(initialAvatar.getAvatarImagePath())) {
            if (initialAvatar.getAvatarImagePath() != null && getContext() != null) {
                AvatarImageProcessor.deleteIfManaged(requireContext(), initialAvatar.getAvatarImagePath());
            }
        } else if (resultCommitted
                && initialAvatar.getAvatarType() == AvatarType.CUSTOM_IMAGE
                && pendingAvatar.getAvatarType() != AvatarType.CUSTOM_IMAGE
                && initialAvatar.getAvatarImagePath() != null
                && getContext() != null) {
            AvatarImageProcessor.deleteIfManaged(requireContext(), initialAvatar.getAvatarImagePath());
        }
        imageExecutor.shutdownNow();
    }
}
