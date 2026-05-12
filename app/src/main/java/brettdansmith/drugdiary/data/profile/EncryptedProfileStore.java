package brettdansmith.drugdiary.data.profile;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import javax.crypto.spec.SecretKeySpec;

import brettdansmith.drugdiary.security.EncryptionManager;
import brettdansmith.drugdiary.security.UserSession;

/**
 * Reads and writes the encrypted per-profile vault.
 * Optimized for fresh-install-only environment.
 */
public final class EncryptedProfileStore {
    private static final String TAG = "EncryptedProfileStore";

    private EncryptedProfileStore() {
    }

    public static void saveProfileData(Context context, JSONObject data) {
        UserSession session = UserSession.getInstance();
        if (!session.isActive()) {
            Log.e(TAG, "Save blocked: no active secure session.");
            return;
        }

        try {
            String profileName = session.getProfileName();
            SecretKeySpec key = session.getSessionKey();
            File file = getProfileVaultFile(context, profileName);
            EncryptionManager.encryptToFile(data.toString(), key, file);
            session.setCachedData(data);
        } catch (Exception e) {
            Log.e(TAG, "Profile encryption error", e);
        }
    }

    public static JSONObject loadProfileData(Context context) {
        UserSession session = UserSession.getInstance();
        if (!session.isActive()) {
            Log.e(TAG, "Load blocked: no active secure session.");
            return new JSONObject();
        }

        try {
            if (session.getCachedData() != null) {
                return session.getCachedData();
            }

            String profileName = session.getProfileName();
            SecretKeySpec key = session.getSessionKey();
            File file = getProfileVaultFile(context, profileName);
            if (!file.exists()) {
                JSONObject created = ProfileJson.emptyProfile(profileName);
                saveProfileData(context, created);
                return created;
            }

            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int ignored = fis.read(bytes);
            }

            String decryptedJson = EncryptionManager.decrypt(new String(bytes, StandardCharsets.UTF_8), key);
            JSONObject data = new JSONObject(decryptedJson);
            session.setCachedData(data);
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Profile decryption failed.", e);
            return new JSONObject();
        }
    }

    public static void createProfileVault(Context context, String profileName, SecretKeySpec key, JSONObject data) throws Exception {
        File file = getProfileVaultFile(context, profileName);
        EncryptionManager.encryptToFile(data.toString(), key, file);
    }

    public static int deleteAllVaults(Context context) {
        File[] files = context.getFilesDir().listFiles((dir, name) -> (name.startsWith("vault_") || name.startsWith("drug_cache_")) && name.endsWith(".dat"));
        if (files == null) {
            return 0;
        }
        int deleted = 0;
        for (File file : files) {
            if (file.delete()) {
                deleted++;
            }
        }
        return deleted;
    }

    public static boolean deleteProfileVault(Context context, String profileName) {
        File vaultFile = getProfileVaultFile(context, profileName);
        boolean vaultDeleted = !vaultFile.exists() || vaultFile.delete();
        EncryptedDrugCacheStore.deleteCache(context, profileName);
        return vaultDeleted;
    }

    public static File getProfileVaultFile(Context context, String profileName) {
        return new File(context.getFilesDir(), "vault_" + safeFilePart(profileName) + ".dat");
    }

    private static String safeFilePart(String profileName) {
        return profileName == null ? "unknown" : profileName.toLowerCase(Locale.US).replaceAll("[^a-z0-9._-]", "_");
    }
}
