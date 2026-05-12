package brettdansmith.drugdiary.data.profile;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;

import javax.crypto.spec.SecretKeySpec;

import brettdansmith.drugdiary.security.EncryptionManager;

/**
 * High-level profile operations used by UI and assistant commands.
 * Storage remains split between encrypted vault data and non-sensitive auth index metadata.
 */
public final class ProfileRepository {
    private final Context appContext;

    public ProfileRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public JSONObject load() {
        return EncryptedProfileStore.loadProfileData(appContext);
    }

    public void save(JSONObject data) {
        EncryptedProfileStore.saveProfileData(appContext, data);
    }

    public void createProfile(String name, String pin, JSONObject data, int avatar, String avatarUri) throws Exception {
        SecretKeySpec key = EncryptionManager.deriveKey(name + pin);
        EncryptedProfileStore.createProfileVault(appContext, name, key, data);
        ProfileAuthRegistry.saveProfilePin(appContext, name, pin);

        SharedPreferences prefs = ProfileAuthRegistry.prefs(appContext);
        Set<String> profileSet = new HashSet<>(prefs.getStringSet(ProfileAuthRegistry.KEY_PROFILE_NAMES, new HashSet<>()));
        profileSet.add(name);
        prefs.edit()
                .putStringSet(ProfileAuthRegistry.KEY_PROFILE_NAMES, profileSet)
                .putInt("icon_" + name, avatar)
                .putString("avatar_uri_" + name, avatarUri == null ? "" : avatarUri)
                .putString(ProfileAuthRegistry.KEY_LAST_PROFILE, name)
                .apply();
    }

    public boolean changePin(String profileName, String currentPin, String nextPin) throws Exception {
        if (!ProfileAuthRegistry.verifyPin(appContext, profileName, currentPin)) {
            return false;
        }
        ProfileAuthRegistry.saveProfilePin(appContext, profileName, nextPin);
        return true;
    }

    public boolean deleteProfile(String profileName, String pin) throws Exception {
        if (!ProfileAuthRegistry.verifyPin(appContext, profileName, pin)) {
            return false;
        }
        EncryptedProfileStore.deleteProfileVault(appContext, profileName);
        ProfileAuthRegistry.removeProfile(appContext, profileName);
        return true;
    }
}

