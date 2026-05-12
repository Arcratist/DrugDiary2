package brettdansmith.drugdiary.ui.assistant;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.OutputStream;
import java.util.UUID;

import brettdansmith.drugdiary.R;

public class FullscreenImageDialog extends Dialog {
    private final Bitmap bitmap;

    public FullscreenImageDialog(@NonNull Context context, Bitmap bitmap) {
        super(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.bitmap = bitmap;
    }

    public static void show(Context context, Bitmap bitmap) {
        new FullscreenImageDialog(context, bitmap).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.dialog_fullscreen_image);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.BLACK));
        getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        ImageView imageView = findViewById(R.id.fullscreen_image);
        imageView.setImageBitmap(bitmap);

        findViewById(R.id.button_close).setOnClickListener(v -> dismiss());
        findViewById(R.id.button_save).setOnClickListener(v -> saveImage());
    }

    private void saveImage() {
        Context context = getContext();
        String filename = "DrugDiary_" + UUID.randomUUID().toString().substring(0, 8) + ".jpg";
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, filename);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES);
                values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 1);
            }

            android.net.Uri uri = context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            if (uri != null) {
                try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
                    if (out != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    values.clear();
                    values.put(android.provider.MediaStore.Images.Media.IS_PENDING, 0);
                    context.getContentResolver().update(uri, values, null, null);
                }
                Toast.makeText(context, "Image saved to Gallery", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Failed to create image file", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show();
        }
    }
}
