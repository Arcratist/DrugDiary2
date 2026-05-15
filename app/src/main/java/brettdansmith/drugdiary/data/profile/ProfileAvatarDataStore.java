package brettdansmith.drugdiary.data.profile;

import android.content.Context;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import brettdansmith.drugdiary.util.JsonUtils;

public final class ProfileAvatarDataStore {
    private ProfileAvatarDataStore() {
    }

    @NonNull
    public static ProfileAvatar readFromData(@NonNull JSONObject data) throws Exception {
        JSONObject profile = JsonUtils.object(data, ProfileJson.KEY_PROFILE);
        return ProfileAvatar.fromProfileJson(profile);
    }

    public static void writeToData(@NonNull JSONObject data, @NonNull ProfileAvatar avatar) throws Exception {
        JSONObject profile = JsonUtils.object(data, ProfileJson.KEY_PROFILE);
        avatar.writeToProfileJson(profile);
    }

    public static void writeToPrefs(@NonNull Context context, @NonNull String profileName, @NonNull ProfileAvatar avatar) {
        ProfileAvatarPrefsStore.save(context, profileName, avatar);
    }

    @NonNull
    public static ProfileAvatar readFromPrefs(@NonNull Context context, @NonNull String profileName) {
        return ProfileAvatarPrefsStore.load(context, profileName);
    }
}
