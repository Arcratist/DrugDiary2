package brettdansmith.drugdiary.ui.avatar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.BitmapShader;
import android.graphics.Shader;
import android.net.Uri;
import android.content.res.Resources;
import java.io.InputStream;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.Locale;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.AvatarType;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;

public final class AvatarNavIconFactory {
    private AvatarNavIconFactory() {
    }

    @NonNull
    public static Drawable create(@NonNull Context context,
                                  @Nullable String profileName,
                                  @NonNull ProfileAvatar avatar,
                                  int sizePx) {
        int safeSize = Math.max(sizePx, 24);
        int drawSize = safeSize * 2;
        Bitmap bitmap = Bitmap.createBitmap(drawSize, drawSize, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        float center = drawSize / 2f;
        float radius = drawSize / 2f;
        Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bgPaint.setColor(ContextCompat.getColor(context, R.color.brand_purple_alpha));
        canvas.drawCircle(center, center, radius, bgPaint);

        boolean drawn = false;
        if (avatar.getAvatarType() == AvatarType.CUSTOM_IMAGE) {
            drawn = drawCustomImage(canvas, context, avatar.getAvatarImagePath(), drawSize);
        } else if (avatar.getAvatarType() == AvatarType.BUILT_IN_ICON && avatar.getAvatarIconId() != null) {
            drawn = drawGlyph(canvas, avatar.getAvatarIconId(), drawSize);
        }

        if (!drawn) {
            drawInitials(canvas, profileName, drawSize);
        }

        return new BitmapDrawable(context.getResources(), bitmap);
    }

    private static boolean drawCustomImage(@NonNull Canvas canvas, @NonNull Context context, @Nullable String imagePath, int sizePx) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return false;
        }
        try {
            Bitmap source = null;
            File file = new File(imagePath);
            if (file.exists()) {
                source = BitmapFactory.decodeFile(imagePath);
            }
            
            if (source == null) {
                Uri uri;
                if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                    uri = Uri.parse(imagePath);
                } else {
                    uri = Uri.fromFile(new File(imagePath));
                }
                
                try (InputStream is = context.getContentResolver().openInputStream(uri)) {
                    if (is != null) {
                        source = BitmapFactory.decodeStream(is);
                    }
                }
            }
            
            if (source == null) return false;

            int side = Math.min(source.getWidth(), source.getHeight());
            int left = (source.getWidth() - side) / 2;
            int top = (source.getHeight() - side) / 2;
            Bitmap square = Bitmap.createBitmap(source, left, top, side, side);
            Bitmap scaled = Bitmap.createScaledBitmap(square, sizePx, sizePx, true);

            BitmapShader shader = new BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(shader);
            canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint);

            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean drawGlyph(@NonNull Canvas canvas, @NonNull String iconId, int sizePx) {
        AvatarGlyphDrawable glyph = new AvatarGlyphDrawable(iconId);
        glyph.setMonotoneColor(0xFFFFFFFF);
        int pad = Math.round(sizePx * 0.214f);
        glyph.setBounds(new Rect(pad, pad, sizePx - pad, sizePx - pad));
        glyph.draw(canvas);
        return true;
    }

    private static void drawInitials(@NonNull Canvas canvas, @Nullable String profileName, int sizePx) {
        String initials = AvatarInitials.fromName(profileName).toUpperCase(Locale.getDefault());
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setTextSize(sizePx * 0.42f);
        Paint.FontMetrics metrics = textPaint.getFontMetrics();
        float x = sizePx / 2f;
        float y = (sizePx / 2f) - ((metrics.ascent + metrics.descent) / 2f);
        canvas.drawText(initials, x, y, textPaint);
    }
}
