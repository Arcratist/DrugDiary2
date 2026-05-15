package brettdansmith.drugdiary.data.profile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

public final class ProfileAvatar {
    private final AvatarType avatarType;
    @Nullable private final String avatarIconId;
    @Nullable private final String avatarImagePath;

    public ProfileAvatar(@NonNull AvatarType avatarType, @Nullable String avatarIconId, @Nullable String avatarImagePath) {
        this.avatarType = avatarType;
        this.avatarIconId = normalize(avatarIconId);
        this.avatarImagePath = normalize(avatarImagePath);
    }

    @NonNull
    public static ProfileAvatar initials() {
        return new ProfileAvatar(AvatarType.INITIALS, null, null);
    }

    @NonNull
    public static ProfileAvatar builtIn(@Nullable String iconId) {
        return new ProfileAvatar(AvatarType.BUILT_IN_ICON, iconId, null);
    }

    @NonNull
    public static ProfileAvatar customImage(@Nullable String imagePath) {
        return new ProfileAvatar(AvatarType.CUSTOM_IMAGE, null, imagePath);
    }

    @NonNull
    public static ProfileAvatar fromProfileJson(@Nullable JSONObject profile) {
        if (profile == null) {
            return initials();
        }
        AvatarType type = AvatarType.fromStorage(profile.optString(ProfileJson.PROFILE_AVATAR_TYPE, AvatarType.INITIALS.name()));
        String iconId = normalize(profile.optString(ProfileJson.PROFILE_AVATAR_ICON_ID, null));
        String imagePath = normalize(profile.optString(ProfileJson.PROFILE_AVATAR_IMAGE_PATH, null));
        if (type == AvatarType.BUILT_IN_ICON && iconId == null) {
            return initials();
        }
        if (type == AvatarType.CUSTOM_IMAGE && imagePath == null) {
            return initials();
        }
        return new ProfileAvatar(type, iconId, imagePath);
    }

    @NonNull
    public static ProfileAvatar fromPrefs(AvatarType type, @Nullable String iconId, @Nullable String imagePath) {
        if (type == AvatarType.BUILT_IN_ICON && normalize(iconId) == null) {
            return initials();
        }
        if (type == AvatarType.CUSTOM_IMAGE && normalize(imagePath) == null) {
            return initials();
        }
        return new ProfileAvatar(type, iconId, imagePath);
    }

    public void writeToProfileJson(@NonNull JSONObject profile) throws JSONException {
        profile.put(ProfileJson.PROFILE_AVATAR_TYPE, avatarType.name());
        profile.put(ProfileJson.PROFILE_AVATAR_ICON_ID, avatarIconId == null ? "" : avatarIconId);
        profile.put(ProfileJson.PROFILE_AVATAR_IMAGE_PATH, avatarImagePath == null ? "" : avatarImagePath);
    }

    @NonNull
    public AvatarType getAvatarType() {
        return avatarType;
    }

    @Nullable
    public String getAvatarIconId() {
        return avatarIconId;
    }

    @Nullable
    public String getAvatarImagePath() {
        return avatarImagePath;
    }

    @NonNull
    public ProfileAvatar withAvatarImagePath(@Nullable String newPath) {
        return new ProfileAvatar(avatarType, avatarIconId, newPath);
    }

    @Nullable
    private static String normalize(@Nullable String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
