package brettdansmith.drugdiary.ui.assistant;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import brettdansmith.drugdiary.databinding.FragmentAttachmentViewerBinding;

public class AttachmentViewerFragment extends Fragment {
    public static final String ARG_BASE64 = "base64_data";
    public static final String ARG_NAME = "attachment_name";

    private FragmentAttachmentViewerBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentAttachmentViewerBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String base64 = getArguments() != null ? getArguments().getString(ARG_BASE64) : "";
        String name = getArguments() != null ? getArguments().getString(ARG_NAME, "Image") : "Image";

        binding.textAttachmentName.setText(name);
        binding.toolbar.setNavigationOnClickListener(v -> NavHostFragment.findNavController(this).navigateUp());

        if (!base64.isEmpty()) {
            try {
                byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                binding.imageViewAttachment.setImageBitmap(bitmap);
            } catch (Exception e) {
                Toast.makeText(requireContext(), "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }

        binding.buttonSaveAttachment.setOnClickListener(v -> {
            // Logic to save to device
            Toast.makeText(requireContext(), "Saving to device...", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
