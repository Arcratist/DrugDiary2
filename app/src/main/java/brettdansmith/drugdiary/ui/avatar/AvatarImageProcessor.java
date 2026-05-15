package brettdansmith.drugdiary.ui.avatar;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Locale;

public final class AvatarImageProcessor {
    private static final int AVATAR_SIZE = 512;

    private AvatarImageProcessor() {
    }

    @NonNull
    public static String processAndStoreSquareAvatar(
            @NonNull Context context,
            @NonNull Uri sourceUri,
            @NonNull String profileName) throws Exception {
        Bitmap decoded = decodeBitmap(context, sourceUri, 2048);
        if (decoded == null) {
            throw new IllegalStateException("Unable to decode avatar image");
        }
        Bitmap square = toSquare(decoded);
        Bitmap scaled = Bitmap.createScaledBitmap(square, AVATAR_SIZE, AVATAR_SIZE, true);
        if (square != decoded) {
            square.recycle();
        }
        decoded.recycle();

        File dir = getAvatarDirectory(context);
        if (!dir.exists() && !dir.mkdirs()) {
            scaled.recycle();
            throw new IllegalStateException("Unable to create avatar directory");
        }
        String safe = profileName.toLowerCase(Locale.US).replaceAll("[^a-z0-9._-]", "_");
        File outFile = new File(dir, "avatar_" + safe + "_" + System.currentTimeMillis() + ".jpg");
        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            if (!scaled.compress(Bitmap.CompressFormat.JPEG, 90, fos)) {
                throw new IllegalStateException("Unable to compress avatar image");
            }
        } finally {
            scaled.recycle();
        }
        return outFile.getAbsolutePath();
    }

    public static void deleteIfManaged(@NonNull Context context, @Nullable String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return;
        }
        try {
            File file = new File(imagePath);
            File dir = getAvatarDirectory(context);
            if (file.getAbsolutePath().startsWith(dir.getAbsolutePath())) {
                if (file.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    file.delete();
                }
            }
        } catch (Exception ignored) {
        }
    }

    @NonNull
    private static File getAvatarDirectory(@NonNull Context context) {
        return new File(context.getFilesDir(), "avatars");
    }

    @Nullable
    private static Bitmap decodeBitmap(@NonNull Context context, @NonNull Uri uri, int maxSize) throws Exception {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return null;
            BitmapFactory.decodeStream(stream, null, bounds);
        }

        int sample = 1;
        int width = Math.max(bounds.outWidth, 1);
        int height = Math.max(bounds.outHeight, 1);
        while (width / sample > maxSize || height / sample > maxSize) {
            sample *= 2;
        }

        BitmapFactory.Options decode = new BitmapFactory.Options();
        decode.inSampleSize = Math.max(sample, 1);
        decode.inPreferredConfig = Bitmap.Config.ARGB_8888;
        try (InputStream stream = context.getContentResolver().openInputStream(uri)) {
            if (stream == null) return null;
            return BitmapFactory.decodeStream(stream, null, decode);
        }
    }

    @NonNull
    private static Bitmap toSquare(@NonNull Bitmap source) {
        int side = Math.min(source.getWidth(), source.getHeight());
        int left = (source.getWidth() - side) / 2;
        int top = (source.getHeight() - side) / 2;
        Bitmap square = Bitmap.createBitmap(side, side, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(square);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        Matrix matrix = new Matrix();
        matrix.postTranslate(-left, -top);
        canvas.drawBitmap(source, matrix, paint);
        return square;
    }
}
