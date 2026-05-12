package brettdansmith.drugdiary.util;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.text.InputType;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.security.SecureQrPayloadCodec;

/**
 * Small UI boundary for exporting sensitive app data as PIN-protected QR codes.
 *
 * Callers provide already-approved JSON. This helper only wraps, encrypts, and
 * displays/shares the payload; it does not decide what private fields are safe.
 */
public final class SecureQrShareController {
    private static final int QR_SIZE_PX = 1000;
    private final Fragment fragment;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public enum ShareMode {
        QR_CODE,
        ENCRYPTED_TEXT
    }

    public SecureQrShareController(Fragment fragment) {
        this.fragment = fragment;
    }

    public void shareJson(String exportType, String title, JSONObject payload) {
        shareJson(exportType, title, payload, ShareMode.QR_CODE);
    }

    public void shareJson(String exportType, String title, JSONObject payload, ShareMode mode) {
        EditText pinInput = new EditText(fragment.requireContext());
        pinInput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.secure_qr_pin_title)
                .setMessage(R.string.secure_qr_pin_body)
                .setView(pinInput)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.share, (dialog, which) -> {
                    String pin = pinInput.getText().toString().trim();
                    if (!pin.isEmpty()) {
                        encryptAndShow(exportType, title, payload, pin, mode);
                    }
                })
                .show();
    }

    private void encryptAndShow(String exportType, String title, JSONObject payload, String pin, ShareMode mode) {
        Context appContext = fragment.requireContext().getApplicationContext();
        executor.execute(() -> {
            try {
                JSONObject envelope = new JSONObject()
                        .put("export_type", exportType)
                        .put("version", SecureQrPayloadCodec.VERSION)
                        .put("title", title)
                        .put("created_at", System.currentTimeMillis())
                        .put("payload", payload);
                String encryptedPayload = SecureQrPayloadCodec.encode(envelope.toString(), pin);
                if (mode == ShareMode.ENCRYPTED_TEXT) {
                    fragment.requireActivity().runOnUiThread(() -> shareEncryptedText(title, encryptedPayload));
                    return;
                }
                Bitmap bitmap = null;
                try {
                    bitmap = SecureQrPayloadCodec.toQrBitmap(encryptedPayload, QR_SIZE_PX);
                } catch (Exception ignored) {
                    // Large encrypted exports can exceed QR capacity. They remain PIN-protected
                    // and are still shareable as encrypted text through the same flow.
                }
                Bitmap finalBitmap = bitmap;
                fragment.requireActivity().runOnUiThread(() -> showResult(title, encryptedPayload, finalBitmap));
            } catch (Exception e) {
                fragment.requireActivity().runOnUiThread(() ->
                        Toast.makeText(appContext, fragment.getString(R.string.secure_qr_export_failed, e.getMessage()), Toast.LENGTH_LONG).show());
            }
        });
    }

    private void showResult(String title, String encryptedPayload, Bitmap bitmap) {
        if (!fragment.isAdded()) return;
        if (bitmap == null) {
            new MaterialAlertDialogBuilder(fragment.requireContext())
                    .setTitle(R.string.secure_qr_too_large_title)
                    .setMessage(R.string.secure_qr_too_large_body)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.secure_qr_share_encrypted_text, (dialog, which) -> shareEncryptedText(title, encryptedPayload))
                    .show();
            return;
        }

        ImageView imageView = new ImageView(fragment.requireContext());
        imageView.setImageBitmap(bitmap);
        imageView.setAdjustViewBounds(true);
        int padding = (int) (16 * fragment.getResources().getDisplayMetrics().density);
        imageView.setPadding(padding, padding, padding, padding);
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(title)
                .setView(imageView)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.secure_qr_share_encrypted_text, (dialog, which) -> shareEncryptedText(title, encryptedPayload))
                .show();
    }

    private void shareEncryptedText(String title, String encryptedPayload) {
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, title)
                .putExtra(Intent.EXTRA_TEXT, encryptedPayload);
        fragment.startActivity(Intent.createChooser(intent, fragment.getString(R.string.share)));
    }

    public void shutdown() {
        executor.shutdownNow();
    }
}
