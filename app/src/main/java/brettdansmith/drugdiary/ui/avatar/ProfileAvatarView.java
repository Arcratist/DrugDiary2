package brettdansmith.drugdiary.ui.avatar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;

import brettdansmith.drugdiary.R;
import brettdansmith.drugdiary.data.profile.AvatarType;
import brettdansmith.drugdiary.data.profile.ProfileAvatar;

public class ProfileAvatarView extends FrameLayout {
    private static final float ICON_PADDING_RATIO = 12f / 56f;
    private static final float INITIALS_TEXT_RATIO = 18f / 56f;

    private TextView initialsView;
    private ImageView iconView;
    private ImageView customImageView;
    @Nullable private String boundProfileName;
    @Nullable private ProfileAvatar boundAvatar;
    private boolean forceAddAvatarMode = false;

    public ProfileAvatarView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public ProfileAvatarView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public ProfileAvatarView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_profile_avatar, this, true);
        initialsView = findViewById(R.id.text_avatar_initials);
        iconView = findViewById(R.id.image_avatar_icon);
        customImageView = findViewById(R.id.image_avatar_custom);
    }

    public void bind(@Nullable String profileName, @Nullable ProfileAvatar avatar) {
        forceAddAvatarMode = false;
        boundProfileName = profileName;
        boundAvatar = avatar;
        renderBoundAvatar();
    }

    private void renderBoundAvatar() {
        if (forceAddAvatarMode) {
            renderAddAvatar();
            return;
        }
        ProfileAvatar avatar = boundAvatar;
        String profileName = boundProfileName;
        if (avatar == null) {
            showInitials(profileName);
            return;
        }

        if (avatar.getAvatarType() == AvatarType.CUSTOM_IMAGE && loadCustomImage(avatar.getAvatarImagePath())) {
            setContentDescription(getContext().getString(R.string.avatar_custom_image));
            return;
        }

        if (avatar.getAvatarType() == AvatarType.BUILT_IN_ICON) {
            AvatarIconRegistry.AvatarIconOption option = AvatarIconRegistry.findById(avatar.getAvatarIconId());
            if (option != null) {
                showIcon(new AvatarGlyphDrawable(option.id));
                setContentDescription(getContext().getString(option.contentDescriptionRes));
                return;
            }
        }

        showInitials(profileName);
    }

    public void showAddAvatar() {
        forceAddAvatarMode = true;
        renderAddAvatar();
    }

    private void renderAddAvatar() {
        hideAll();
        initialsView.setVisibility(View.VISIBLE);
        initialsView.setText("+");
        int side = Math.min(getWidth(), getHeight());
        if (side > 0) {
            initialsView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, side * 0.54f);
            initialsView.setTranslationY(0f);
        } else {
            initialsView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 27f);
            initialsView.setTranslationY(0f);
        }
        setContentDescription(getContext().getString(R.string.create_profile));
    }

    private void showInitials(@Nullable String profileName) {
        hideAll();
        initialsView.setVisibility(View.VISIBLE);
        applyInitialsTextSize();
        initialsView.setTranslationY(0f);
        initialsView.setText(AvatarInitials.fromName(profileName));
        setContentDescription(getContext().getString(R.string.avatar_initials));
    }

    private void showIcon(@NonNull Drawable drawable) {
        hideAll();
        iconView.setVisibility(View.VISIBLE);
        applyIconPadding();
        if (drawable instanceof AvatarGlyphDrawable) {
            ((AvatarGlyphDrawable) drawable).setMonotoneColor(initialsView.getCurrentTextColor());
            iconView.setImageTintList(null);
        } else {
            iconView.setImageTintList(ColorStateList.valueOf(initialsView.getCurrentTextColor()));
        }
        iconView.setImageDrawable(drawable);
    }

    private boolean loadCustomImage(@Nullable String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return false;
        }
        hideAll();
        try {
            File file = new File(imagePath);
            if (file.exists()) {
                if (BitmapFactory.decodeFile(imagePath) == null) {
                    return false;
                }
                customImageView.setVisibility(View.VISIBLE);
                customImageView.setImageURI(Uri.fromFile(file));
                return true;
            }
            Uri uri = Uri.parse(imagePath);
            customImageView.setVisibility(View.VISIBLE);
            customImageView.setImageURI(uri);
            return customImageView.getDrawable() != null;
        } catch (Exception ignored) {
            return false;
        }
    }

    private void hideAll() {
        initialsView.setVisibility(View.GONE);
        iconView.setVisibility(View.GONE);
        customImageView.setVisibility(View.GONE);
        iconView.setImageDrawable(null);
        customImageView.setImageDrawable(null);
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w != oldw || h != oldh) {
            renderBoundAvatar();
        }
    }

    private void applyIconPadding() {
        int side = Math.min(getWidth(), getHeight());
        int padding = side > 0 ? Math.round(side * ICON_PADDING_RATIO) : dp(12);
        iconView.setPadding(padding, padding, padding, padding);
    }

    private void applyInitialsTextSize() {
        int side = Math.min(getWidth(), getHeight());
        if (side > 0) {
            initialsView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, side * INITIALS_TEXT_RATIO);
        } else {
            initialsView.setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 18f);
        }
    }
}
