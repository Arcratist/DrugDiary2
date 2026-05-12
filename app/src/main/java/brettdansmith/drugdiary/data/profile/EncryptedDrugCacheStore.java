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
 * Manages an independent encrypted storage for drug database lookups.
 * Moving this out of the primary profile vault prevents OOM errors caused by 
 * large JSON string allocations and improves overall performance.
 */
public final class EncryptedDrugCacheStore {
    private static final String TAG = "EncryptedDrugCacheStore";

    private EncryptedDrugCacheStore() {
    }

    public static void saveDrugCache(Context context, JSONObject data) {
        UserSession session = UserSession.getInstance();
        if (!session.isActive()) {
            return;
        }

        try {
            String profileName = session.getProfileName();
            SecretKeySpec key = session.getSessionKey();
            File file = getCacheFile(context, profileName);
            EncryptionManager.encryptToFile(data.toString(), key, file);
            session.setCachedDrugData(data);
        } catch (Exception e) {
            Log.e(TAG, "Drug cache encryption error", e);
        }
    }

    public static JSONObject loadDrugCache(Context context) {
        UserSession session = UserSession.getInstance();
        if (!session.isActive()) {
            return new JSONObject();
        }

        try {
            if (session.getCachedDrugData() != null) {
                return session.getCachedDrugData();
            }

            String profileName = session.getProfileName();
            SecretKeySpec key = session.getSessionKey();
            File file = getCacheFile(context, profileName);
            if (!file.exists()) {
                JSONObject created = new JSONObject();
                session.setCachedDrugData(created);
                return created;
            }

            byte[] bytes = new byte[(int) file.length()];
            try (FileInputStream fis = new FileInputStream(file)) {
                int ignored = fis.read(bytes);
            }

            String decryptedJson = EncryptionManager.decrypt(new String(bytes, StandardCharsets.UTF_8), key);
            JSONObject data = new JSONObject(decryptedJson);
            session.setCachedDrugData(data);
            return data;
        } catch (Exception e) {
            Log.e(TAG, "Drug cache decryption failed", e);
            return new JSONObject();
        }
    }

    public static boolean deleteCache(Context context, String profileName) {
        File file = getCacheFile(context, profileName);
        return !file.exists() || file.delete();
    }

    private static File getCacheFile(Context context, String profileName) {
        return new File(context.getFilesDir(), "drug_cache_" + safeFilePart(profileName) + ".dat");
    }

    private static String safeFilePart(String profileName) {
        return profileName == null ? "unknown" : profileName.toLowerCase(Locale.US).replaceAll("[^a-z0-9._-]", "_");
    }
}
