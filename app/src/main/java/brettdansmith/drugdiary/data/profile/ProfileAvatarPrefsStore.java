package brettdansmith.drugdiary.data.profile;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

public final class ProfileAvatarPrefsStore {
    private static final String AVATAR_TYPE_PREFIX = "avatar_type_";
    private static final String AVATAR_ICON_ID_PREFIX = "avatar_icon_id_";
    private static final String AVATAR_IMAGE_PATH_PREFIX = "avatar_image_path_";

    private ProfileAvatarPrefsStore() {
    }

    @NonNull
    public static ProfileAvatar load(@NonNull Context context, @NonNull String profileName) {
        SharedPreferences prefs = ProfileAuthRegistry.prefs(context);
        AvatarType type = AvatarType.fromStorage(
                prefs.getString(AVATAR_TYPE_PREFIX + profileName, AvatarType.INITIALS.name()));
        String iconId = prefs.getString(AVATAR_ICON_ID_PREFIX + profileName, "");
        String imagePath = prefs.getString(AVATAR_IMAGE_PATH_PREFIX + profileName, "");
        return ProfileAvatar.fromPrefs(type, iconId, imagePath);
    }

    public static void save(@NonNull Context context, @NonNull String profileName, @NonNull ProfileAvatar avatar) {
        SharedPreferences prefs = ProfileAuthRegistry.prefs(context);
        prefs.edit()
                .putString(AVATAR_TYPE_PREFIX + profileName, avatar.getAvatarType().name())
                .putString(AVATAR_ICON_ID_PREFIX + profileName, avatar.getAvatarIconId() == null ? "" : avatar.getAvatarIconId())
                .putString(AVATAR_IMAGE_PATH_PREFIX + profileName, avatar.getAvatarImagePath() == null ? "" : avatar.getAvatarImagePath())
                .apply();
    }

    public static void remove(@NonNull SharedPreferences.Editor editor, @NonNull String profileName) {
        editor.remove(AVATAR_TYPE_PREFIX + profileName)
                .remove(AVATAR_ICON_ID_PREFIX + profileName)
                .remove(AVATAR_IMAGE_PATH_PREFIX + profileName);
    }
}
