package brettdansmith.drugdiary.data.profile;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Stores profile names and PIN verification material outside the encrypted vault.
 *
 * The PIN itself is never stored. Legacy plaintext PINs are upgraded to salted
 * PBKDF2 hashes after the first successful verification.
 */
public final class ProfileAuthRegistry {
    public static final String PREFS_PROFILES = "ProfilesData";
    public static final String KEY_PROFILE_NAMES = "profile_names";
    public static final String KEY_LAST_PROFILE = "last_profile";

    private static final String PIN_HASH_PREFIX = "pin_hash_";
    private static final String PIN_SALT_PREFIX = "pin_salt_";
    private static final String PIN_LENGTH_PREFIX = "pin_length_";
    private static final String LEGACY_PIN_PREFIX = "pin_";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;

    private ProfileAuthRegistry() {
    }

    public static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS_PROFILES, Context.MODE_PRIVATE);
    }

    public static Set<String> getProfileNames(Context context) {
        return new HashSet<>(prefs(context).getStringSet(KEY_PROFILE_NAMES, new HashSet<>()));
    }

    public static void saveProfilePin(Context context, String profileName, String pin) throws Exception {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        prefs(context).edit()
                .putString(PIN_SALT_PREFIX + profileName, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putString(PIN_HASH_PREFIX + profileName, hashPin(profileName, pin, salt))
                .putInt(PIN_LENGTH_PREFIX + profileName, pin.length())
                .remove(LEGACY_PIN_PREFIX + profileName)
                .apply();
    }

    public static boolean verifyPin(Context context, String profileName, String pin) throws Exception {
        SharedPreferences prefs = prefs(context);
        String saltEncoded = prefs.getString(PIN_SALT_PREFIX + profileName, null);
        String expectedHash = prefs.getString(PIN_HASH_PREFIX + profileName, null);
        if (saltEncoded != null && expectedHash != null) {
            byte[] salt = Base64.decode(saltEncoded, Base64.NO_WRAP);
            return constantTimeEquals(expectedHash, hashPin(profileName, pin, salt));
        }

        String legacyPin = prefs.getString(LEGACY_PIN_PREFIX + profileName, "");
        boolean matched = legacyPin.equals(pin);
        if (matched) {
            saveProfilePin(context, profileName, pin);
        }
        return matched;
    }

    public static int getPinLength(Context context, String profileName) {
        return prefs(context).getInt(PIN_LENGTH_PREFIX + profileName, 4);
    }

    public static void removeProfile(Context context, String profileName) {
        Set<String> names = getProfileNames(context);
        names.remove(profileName);
        SharedPreferences.Editor editor = prefs(context).edit()
                .putStringSet(KEY_PROFILE_NAMES, names)
                .remove(PIN_HASH_PREFIX + profileName)
                .remove(PIN_SALT_PREFIX + profileName)
                .remove(PIN_LENGTH_PREFIX + profileName)
                .remove(LEGACY_PIN_PREFIX + profileName);
        ProfileAvatarPrefsStore.remove(editor, profileName);
        if (profileName.equals(prefs(context).getString(KEY_LAST_PROFILE, ""))) {
            editor.remove(KEY_LAST_PROFILE);
        }
        editor.apply();
    }

    private static String hashPin(String profileName, String pin, byte[] salt) throws Exception {
        PBEKeySpec spec = new PBEKeySpec((profileName + ":" + pin).toCharArray(), salt, ITERATIONS, KEY_LENGTH);
        byte[] hash = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        return Base64.encodeToString(hash, Base64.NO_WRAP);
    }

    private static boolean constantTimeEquals(String first, String second) {
        return MessageDigest.isEqual(first.getBytes(), second.getBytes());
    }
}
